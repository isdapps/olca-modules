package org.openlca.io.ilcd.output;

import java.math.BigInteger;

import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.math.ReferenceAmount;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.ilcd.commons.ClassificationInformation;
import org.openlca.ilcd.commons.DataSetReference;
import org.openlca.ilcd.commons.ExchangeDirection;
import org.openlca.ilcd.commons.Other;
import org.openlca.ilcd.commons.QuantitativeReferenceType;
import org.openlca.ilcd.io.DataStoreException;
import org.openlca.ilcd.processes.DataSetInformation;
import org.openlca.ilcd.processes.Exchange;
import org.openlca.ilcd.processes.ExchangeList;
import org.openlca.ilcd.processes.Process;
import org.openlca.ilcd.processes.ProcessInformation;
import org.openlca.ilcd.processes.ProcessName;
import org.openlca.ilcd.processes.QuantitativeReference;
import org.openlca.ilcd.productmodel.Connector;
import org.openlca.ilcd.productmodel.ConsumedBy;
import org.openlca.ilcd.productmodel.ProcessNode;
import org.openlca.ilcd.productmodel.Product;
import org.openlca.ilcd.productmodel.ProductModel;
import org.openlca.ilcd.util.LangString;
import org.openlca.ilcd.util.ProcessInfoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemExport {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ExportConfig config;
	private ProductSystem system;

	public SystemExport(ExportConfig config) {
		this.config = config;
	}

	public Process run(ProductSystem system) throws DataStoreException {
		if (config.store.contains(Process.class, system.getRefId()))
			return config.store.get(Process.class, system.getRefId());
		this.system = system;
		log.trace("Run product system export with {}", system);
		if (!canRun()) {
			log.error("System {} is not valid and cannot exported", system);
			return null;
		}
		Process process = createProcess();
		config.store.put(process, system.getRefId());
		this.system = null;
		return process;
	}

	private Process createProcess() {
		Process process = new Process();
		ProcessInformation info = new ProcessInformation();
		process.setProcessInformation(info);
		info.setDataSetInformation(makeDataSetInfo());
		info.setQuantitativeReference(makeQuantitativeReference());
		addRefProcessInfo(info);
		addExchange(process);
		addProductModel(info);
		return process;
	}

	private void addProductModel(ProcessInformation info) {
		ProductModel model = new ProductModel();
		model.setName(system.getName());
		Other other = new Other();
		other.getAny().add(model);
		info.setOther(other);
		exportProcesses(model);
		exportLinks(model);
		// addParamaters(model);
	}

	private void exportProcesses(ProductModel model) {
		ProcessDao processDao = new ProcessDao(config.db);
		for (Long processId : system.getProcesses()) {
			org.openlca.core.model.Process proc = processDao
					.getForId(processId);
			DataSetReference ref = ExportDispatch.forwardExportCheck(proc,
					config);
			ProcessNode node = new ProcessNode();
			node.setId(proc.getRefId());
			node.setName(proc.getName());
			node.setUri(ref.getUri());
			node.setUuid(proc.getRefId());
			model.getNodes().add(node);
		}
	}

	private void exportLinks(ProductModel model) {
		int c = 0;
		ProcessDao processDao = new ProcessDao(config.db);
		FlowDao flowDao = new FlowDao(config.db);
		for (ProcessLink link : this.system.getProcessLinks()) {
			Connector connector = new Connector();
			model.getConnections().add(connector);
			connector.setId(Integer.toString(++c));

			// provider process
			//BaseDescriptor provider = processDao.getDescriptor(link.providerId);
			org.openlca.core.model.Process provider = processDao.getForId(link.providerId);				
			if (provider == null)
				continue;
			connector.setOrigin(provider.getRefId());

			// product flow
			//BaseDescriptor flow = flowDao.getDescriptor(link.flowId);
			org.openlca.core.model.Flow flow = flowDao.getForId(link.flowId);			
			if (flow == null)
				continue;
			Product product = new Product();
			connector.getProducts().add(product);
			product.setName(flow.getName());
			product.setUuid(flow.getRefId());

			// recipient process
			ConsumedBy consumedBy = new ConsumedBy();
			product.setConsumedBy(consumedBy);
			consumedBy.setFlowUUID(flow.getRefId());
			//BaseDescriptor recipient = processDao.getDescriptor(link.processId);
			org.openlca.core.model.Process recipient = processDao.getForId(link.processId);				
			if (recipient == null)
				continue;
			consumedBy.setProcessId(recipient.getRefId());

			// we do not set the amount field currently because it is
			// the value in the respective process
			// consumedBy.setAmount(link.getRecipientInput().getConvertedResult());
		}
	}

	private void addExchange(Process process) {
		org.openlca.core.model.Exchange refExchange = system
				.getReferenceExchange();
		ExchangeList list = new ExchangeList();
		process.setExchanges(list);
		Exchange exchange = new Exchange();
		list.getExchanges().add(exchange);
		exchange.setDataSetInternalID(new BigInteger("1"));
		exchange.setExchangeDirection(ExchangeDirection.OUTPUT);
		DataSetReference flowRef = ExportDispatch.forwardExportCheck(
				refExchange.getFlow(), config);
		exchange.setFlow(flowRef);
		double refAmount = ReferenceAmount.get(system);
		exchange.setMeanAmount(refAmount);
		exchange.setResultingAmount(refAmount);
	}

	private boolean canRun() {
		if (system == null)
			return false;
		return system.getReferenceExchange() != null
				&& system.getReferenceProcess() != null;
	}

	private DataSetInformation makeDataSetInfo() {
		DataSetInformation info = new DataSetInformation();
		info.setUUID(system.getRefId());
		ProcessName processName = new ProcessName();
		info.setName(processName);
		String name = system.getName() + " (product system)";
		LangString.addLabel(processName.getBaseName(), name, config.ilcdConfig);
		if (system.getDescription() != null) {
			LangString.addFreeText(info.getGeneralComment(),
					system.getDescription(), config.ilcdConfig);
		}
		addClassification(info);
		return info;
	}

	private void addClassification(DataSetInformation info) {
		CategoryConverter conv = new CategoryConverter();
		ClassificationInformation classInfo = conv
				.getClassificationInformation(system.getCategory());
		if (classInfo != null)
			info.setClassificationInformation(classInfo);
	}

	private QuantitativeReference makeQuantitativeReference() {
		QuantitativeReference qRef = new QuantitativeReference();
		qRef.setType(QuantitativeReferenceType.REFERENCE_FLOW_S);
		qRef.getReferenceToReferenceFlow().add(new BigInteger("1"));
		return qRef;
	}

	private void addRefProcessInfo(ProcessInformation info) {
		org.openlca.core.model.Process refProcess = system
				.getReferenceProcess();
		ProcessInfoExtension ext = new ProcessInfoExtension(info);
		ext.setModelRefProcess(refProcess.getRefId());
	}

	// TODO: map system parameters
	// private void addParamaters(ProductModel model) {
	// for (Parameter parameter : system.getParameters())
	// addParameter(parameter, model, ParameterScopeValues.PRODUCTMODEL);
	// ParameterDao dao = new ParameterDao(database);
	// try {
	// for (Parameter param : dao.getAllForType(ParameterScope.DATABASE))
	// addParameter(param, model, ParameterScopeValues.GLOBAL);
	// } catch (Exception e) {
	// log.error("Failed to export database paramaters", e);
	// }
	// }
	//
	// private void addParameter(Parameter parameter, ProductModel model,
	// ParameterScopeValues scope) {
	// Expression exp = parameter.getExpression();
	// if (exp == null || parameter.getName() == null)
	// return;
	// org.openlca.ilcd.productmodel.Parameter iParameter = new
	// org.openlca.ilcd.productmodel.Parameter();
	// iParameter.setFormula(exp.getFormula());
	// iParameter.setName(parameter.getName());
	// iParameter.setScope(scope);
	// iParameter.setValue(exp.getValue());
	// model.getParameters().add(iParameter);
	// }

}
