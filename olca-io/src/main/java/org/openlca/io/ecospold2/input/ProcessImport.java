package org.openlca.io.ecospold2.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.core.database.BaseDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.ecospold2.Activity;
import org.openlca.ecospold2.Classification;
import org.openlca.ecospold2.DataSet;
import org.openlca.ecospold2.ElementaryExchange;
import org.openlca.ecospold2.IntermediateExchange;
import org.openlca.ecospold2.PedigreeMatrix;
import org.openlca.io.ecospold2.UncertaintyConverter;
import org.openlca.util.KeyGen;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

class ProcessImport {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final IDatabase db;
	private final RefDataIndex index;
	private final ProcessDao dao;
	private final PriceMapper prices;
	private final ImportConfig config;
	private final PedigreeDQSystem pedigree;

	/** Exchanges that wait for a default provider: provider-id -> exchanges. */
	private final HashMap<String, List<Exchange>> linkQueue = new HashMap<>();

	public ProcessImport(IDatabase db, RefDataIndex index, ImportConfig config) {
		this.db = db;
		this.index = index;
		this.config = config;
		dao = new ProcessDao(db);
		prices = new PriceMapper(db);
		pedigree = new PedigreeDQSystem(db);
	}

	public void importDataSet(DataSet dataSet) {
		try {
			if (dataSet == null) {
				log.warn("not an EcoSpold data set");
				return;
			}
			checkImport(dataSet);
		} catch (Exception e) {
			log.error("Failed to import EcoSpold 2 process", e);
		}
	}

	private void checkImport(DataSet dataSet) {
		if (!valid(dataSet)) {
			log.warn("invalid data set -> not imported");
			return;
		}
		Activity activity = dataSet.getActivity();
		try {
			String refId = RefId.forProcess(dataSet);
			boolean contains = dao.contains(refId);
			if (contains) {
				log.trace("process {} is already in the database",
						activity.getId());
				return;
			}
			log.trace("import process {}", activity.getName());
			runImport(dataSet, refId);
		} catch (Exception e) {
			log.error("Failed to import process", e);
		}
	}

	private boolean valid(DataSet dataSet) {
		Activity activity = dataSet.getActivity();
		if (activity.getId() == null || activity.getName() == null)
			return false;
		IntermediateExchange refFlow = null;
		for (IntermediateExchange techFlow : dataSet
				.getIntermediateExchanges()) {
			if (techFlow.outputGroup == null)
				continue;
			if (techFlow.outputGroup != 0)
				continue;
			if (techFlow.amount == 0)
				continue;
			refFlow = techFlow;
			break;
		}
		return refFlow != null;
	}

	private void runImport(DataSet dataSet, String refId) {
		Activity activity = dataSet.getActivity();
		Process process = new Process();
		process.exchangeDqSystem = pedigree.get();
		process.setRefId(refId);
		setMetaData(activity, process);
		setCategory(dataSet, process);
		if (config.withParameters)
			handleParameters(dataSet, process);
		createProductExchanges(dataSet, process);
		if (process.getQuantitativeReference() == null)
			log.warn("could not set a quantitative reference for process {}",
					refId);
		createElementaryExchanges(dataSet, process);
		new DocImportMapper(db).map(dataSet, process);
		db.createDao(Process.class).insert(process);
		index.putProcessId(refId, process.getId());
		flushLinkQueue(process);
	}

	private void handleParameters(DataSet dataSet, Process process) {
		List<Parameter> list = Parameters.fetch(dataSet, config);
		List<Parameter> newGlobals = new ArrayList<>();
		for (Parameter p : list) {
			if (p.getScope() == ParameterScope.PROCESS)
				process.getParameters().add(p);
			else if (p.getScope() == ParameterScope.GLOBAL)
				newGlobals.add(p);
		}
		ParameterDao dao = new ParameterDao(db);
		Map<String, Boolean> map = new HashMap<>();
		for (Parameter p : dao.getGlobalParameters())
			map.put(p.getName(), Boolean.TRUE);
		for (Parameter newGlobal : newGlobals) {
			Boolean exists = map.get(newGlobal.getName());
			if (exists == null) {
				dao.insert(newGlobal);
				map.put(newGlobal.getName(), Boolean.TRUE);
			}
		}
	}

	private void setMetaData(Activity activity, Process process) {
		process.setName(activity.getName());
		ProcessType type = activity.getType() == 2 ? ProcessType.LCI_RESULT
				: ProcessType.UNIT_PROCESS;
		process.setProcessType(type);
		String description = Joiner
				.on(" ")
				.skipNulls()
				.join(activity.getGeneralComment(),
						activity.getIncludedActivitiesStart(),
						activity.getIncludedActivitiesEnd(),
						activity.getAllocationComment());
		process.setDescription(description);
	}

	private void flushLinkQueue(Process process) {
		List<Exchange> exchanges = linkQueue.remove(process.getRefId());
		if (exchanges == null || process.getId() == 0)
			return;
		try {
			BaseDao<Exchange> dao = db.createDao(Exchange.class);
			for (Exchange exchange : exchanges) {
				exchange.setDefaultProviderId(process.getId());
				dao.update(exchange);
			}
		} catch (Exception e) {
			log.error("failed to update default provider", e);
		}
	}

	private void createElementaryExchanges(DataSet dataSet, Process process) {
		for (ElementaryExchange e : dataSet.getElementaryExchanges()) {
			if (e.amount == 0 && config.skipNullExchanges)
				continue;
			String refId = e.elementaryExchangeId;
			Flow flow = index.getFlow(refId);
			if (flow == null) {
				log.warn("could not create flow for {}",
						e.elementaryExchangeId);
			}
			createExchange(e, refId, flow, process);
		}
	}

	private void createProductExchanges(DataSet dataSet, Process process) {
		for (IntermediateExchange ie : dataSet.getIntermediateExchanges()) {
			boolean isRefFlow = ie.outputGroup != null
					&& ie.outputGroup == 0;
			if (ie.amount == 0 && config.skipNullExchanges)
				continue;
			String refId = ie.intermediateExchangeId;
			Flow flow = index.getFlow(refId);
			if (flow == null) {
				log.warn("could not get flow for {}", refId);
				continue;
			}
			Exchange e = createExchange(ie, refId, flow, process);
			if (e == null)
				continue;
			if (isAvoidedProduct(refId, e))
				e.setAvoidedProduct(true);
			if (ie.activityLinkId != null)
				addActivityLink(ie, e);
			if (isRefFlow)
				process.setQuantitativeReference(e);
			prices.map(ie, e);
		}
	}

	private boolean isAvoidedProduct(String refId, Exchange exchange) {
		return false;
		// If the sign of an product/waste input is different from the sign of
		// the product/waste output of the linked activity it could be an
		// avoided product. Not sure, if this is true for ecoinvent 3
		// boolean isNeg = exchange.getAmountValue() < 0;
		// return isNeg != index.isNegativeFlow(refId) && exchange.isInput();
	}

	private Exchange createExchange(org.openlca.ecospold2.Exchange es2,
			String flowRefId, Flow flow, Process process) {
		if (flow == null || flow.getReferenceFlowProperty() == null)
			return null;
		Exchange e = new Exchange();
		e.setFlow(flow);
		e.setFlowPropertyFactor(flow.getReferenceFactor());
		e.description = es2.comment;
		Unit unit = getFlowUnit(es2, flowRefId, flow);
		if (unit == null)
			return null;
		e.setUnit(unit);
		e.setInput(es2.inputGroup != null);
		double amount = es2.amount;
		if (index.isMappedFlow(flowRefId))
			amount = amount * index.getMappedFlowFactor(flowRefId);
		e.setAmountValue(amount);
		if (config.withParameters && config.withParameterFormulas)
			mapFormula(es2, process, e);
		e.setUncertainty(UncertaintyConverter.toOpenLCA(es2.uncertainty));
		e.setDqEntry(getPedigreeMatrix(es2));
		process.getExchanges().add(e);
		return e;
	}

	private String getPedigreeMatrix(org.openlca.ecospold2.Exchange es2) {
		if (es2 == null || es2.uncertainty == null)
			return null;
		PedigreeMatrix pm = es2.uncertainty.pedigreeMatrix;
		if (pm == null)
			return null;
		DQSystem system = pedigree.get();
		return system.toString(pm.reliability, pm.completeness, pm.temporalCorrelation, pm.geographicalCorrelation,
				pm.technologyCorrelation);
	}

	private Unit getFlowUnit(org.openlca.ecospold2.Exchange original,
			String flowRefId, Flow flow) {
		if (!index.isMappedFlow(flowRefId))
			return index.getUnit(original.unitId);
		FlowProperty refProp = flow.getReferenceFlowProperty();
		if (refProp == null)
			return null;
		UnitGroup ug = refProp.getUnitGroup();
		if (ug == null)
			return null;
		return ug.getReferenceUnit();
	}

	private void mapFormula(org.openlca.ecospold2.Exchange original,
			Process process, Exchange exchange) {
		String var = original.variableName;
		if (Strings.notEmpty(var)) {
			if (Parameters.contains(var, process.getParameters()))
				exchange.setAmountFormula(var);
		} else if (Parameters.isValid(original.mathematicalRelation, config)) {
			exchange.setAmountFormula(original.mathematicalRelation.trim());
		}

	}

	private void addActivityLink(IntermediateExchange e, Exchange exchange) {
		String providerId = e.activityLinkId;
		String flowId = e.intermediateExchangeId;
		String refId = KeyGen.get(providerId, flowId);
		Long processId = index.getProcessId(refId);
		if (processId != null) {
			exchange.setDefaultProviderId(processId);
			return;
		}
		List<Exchange> exchanges = linkQueue.get(refId);
		if (exchanges == null) {
			exchanges = new ArrayList<>();
			linkQueue.put(refId, exchanges);
		}
		exchanges.add(exchange);
	}

	private void setCategory(DataSet dataSet, Process process) {
		Category category = null;
		for (Classification clazz : dataSet.getClassifications()) {
			category = index.getProcessCategory(clazz.getClassificationId());
			if (category != null)
				break;
		}
		process.setCategory(category);
	}

}