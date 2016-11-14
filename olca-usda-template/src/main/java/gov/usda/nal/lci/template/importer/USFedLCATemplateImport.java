package gov.usda.nal.lci.template.importer;
/** ===========================================================================
*
*                            PUBLIC DOMAIN NOTICE
*               		National Agriculture Library
*
*  This software/database is a "United States Government Work" under the
*  terms of the United States Copyright Act.  It was written as part of
*  the author's official duties as a United States Government employee and
*  thus cannot be copyrighted.  This software/database is freely available
*  to the public for use. The National Agriculture Library and the U.S.
*  Government have not placed any restriction on its use or reproduction.
*
*  Although all reasonable efforts have been taken to ensure the accuracy
*  and reliability of the software and data, the NAL and the U.S.
*  Government do not and cannot warrant the performance or results that
*  may be obtained by using this software or data. The NAL and the U.S.
*  Government disclaim all warranties, express or implied, including
*  warranties of performance, merchantability or fitness for any particular
*  purpose.
*
*  Please cite the author in any work or product based on this material.
*
*===========================================================================
*/
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.openlca.util.Strings;
import org.openlca.io.FileImport;
import org.openlca.io.ImportEvent;
import org.openlca.io.ImportProcessEvent;
import org.openlca.util.KeyGen;
import org.openlca.io.UnitMapping;

import gov.usda.nal.lci.template.excel.MappingCells;
import gov.usda.nal.lci.template.keys.UsdaKeyGen;

import org.openlca.io.maps.FlowMap;
import org.openlca.io.maps.MapType;

import gov.usda.nal.lci.template.support.IDataSet;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Actor;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Category;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.Source;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.UncertaintyType;
import org.openlca.ecospold.IAllocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
/**
 * Parses a USDA Excel Template and creates OpenLCA objects and inserts them into a database.  For better or
 * worse, the template import process is modeled after the OpenLCA Ecospold-1 import process.  Understanding the classes in
 * the olca-ecospold-1 module and the org.openlca.io.ecospold1.importer package will help explain how the template
 * importer functions. 
 * 
 * @author Y Radchenko
 *
 */
public class USFedLCATemplateImport implements FileImport {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private Category processCategory;
	private HashMap<Integer, Exchange> localExchangeCache = new HashMap<Integer, Exchange>();
	private DB db;
	private FlowImport flowImport;
	private IDataSet dataSet;
	private EventBus eventBus;
	private boolean canceled = false;
	private InputStream template;
	private String source;
	private ProcessDao processDao;
	private IDatabase iDatabase;
	
			
	public String getSource() {
		return this.source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}
	public void cancel() {
		canceled = true;
	}
	public USFedLCATemplateImport()
	{
		
	}
	public USFedLCATemplateImport(IDatabase iDatabase, InputStream template,UnitMapping unitMapping) {
			this.template=template;
			this.db = new DB(iDatabase);
			this.iDatabase = iDatabase;
			FlowMap flowMap = new FlowMap(MapType.ES1_FLOW);
			this.processDao = new ProcessDao(iDatabase);
			this.flowImport = new FlowImport(db, unitMapping, flowMap);
	}

	public USFedLCATemplateImport(IDatabase iDatabase, UnitMapping unitMapping,
			IDataSet dataSet) {
		this.db = new DB(iDatabase);
		this.dataSet = dataSet;
		FlowMap flowMap = new FlowMap(MapType.ES1_FLOW);
		this.flowImport = new FlowImport(db, unitMapping, flowMap);
	}

	/** Set an optional root category for the new processes. */
	public void setProcessCategory(Category processCategory) {
		this.processCategory = processCategory;
	}
	
	private void fireEvent(String dataSetName) {
		log.trace("import data set {}", dataSetName);
		if (this.eventBus == null)
			return;
		this.eventBus.post(new ImportEvent(dataSetName));
	}
	
	private void fireProcessEvent(String id) {
		log.trace("Process Event ID{}", id);
		if (eventBus == null)
			return;
		eventBus.post(new ImportProcessEvent(id));
	}
	
	public void run() {
//		try {
			fireEvent(this.source);
			// parse the Excel workbook
			MappingCells cells = null;
			try {
				cells = getExcelCells(this.template);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// create a processing data set from the parsed workbook
			if ( cells.getExchangeData() != null && cells.getParametersData() != null
					 && cells.getProcessData() != null) {
					ProcessVOData process = new ProcessVOData(cells);
					process.run();
					// import the data set
					parseProcessDataSet(process.getDataSet());
				}
/*			
		}
		catch (FileNotFoundException fe) {
			log.error("File Not Found " + fe.getMessage());
		} catch (IOException ioe) {
			log.error("IO Exception " + ioe.getMessage(), ioe.getCause());
		} catch (Exception e) {
			log.error("Error " + e.getMessage(), e.getCause());
		}
		*/
	}
	public MappingCells getExcelCells(InputStream template) throws FileNotFoundException, IOException,Exception {
		MappingCells cells = new MappingCells(template);
		cells.run();
		return cells;
	}
	private void parseProcessDataSet(IDataSet dataSet) {
		if (dataSet.getReferenceFunction() == null)
			return;
		insertPersons(dataSet);
		insertSources(dataSet);
		insertLocations(dataSet);
		insertProcess(dataSet);
	}

	private void insertPersons(IDataSet dataSet) {
		for (gov.usda.nal.lci.template.domain.Person person : dataSet
				.getPersons()) {
			String genKey = UsdaKeyGen.forPerson(person);
			if (db.findActor(person, genKey) != null) {
				log.info("Actor {} already exists", genKey);
			} else {
				try {
					Actor actor = new Actor();
					actor.setRefId(genKey);
					Mapper.mapPerson(person, actor);
					db.put(actor, genKey);
					log.info("Person {} has been saved ", genKey);
				} catch (Exception e) {
					log.info("Error " + e);
				}
			}
		}
	}

	private void insertSources(IDataSet dataSet) {
		for (gov.usda.nal.lci.template.domain.Source isource : dataSet
				.getSources()) {
			String genKey = UsdaKeyGen.forSource(isource);
			if (db.findSource(isource, genKey) != null) {
				log.info("Source {} already exists", genKey);
				continue;
			} else {
				try {
					Source oSource = new Source();
					oSource.setRefId(genKey);
					Mapper.mapSource(isource, oSource);
					db.put(oSource, genKey);
					log.info("Source {} has been saved ", genKey);
				} catch (Exception e) {
					log.info("Error " + e);
				}
			}
		}
	}

	private void insertLocations(IDataSet dataSet) {
		if (dataSet.getProcessInformation().getGeography() != null)
			insertLocation(dataSet.getProcessInformation().getGeography()
					.getLocation(),dataSet.getProcessInformation().getGeography()
					.getLatitude(),dataSet.getProcessInformation().getGeography()
					.getLongitude());

		for (gov.usda.nal.lci.template.domain.Exchange exchange : dataSet
				.getExchanges()) {
			if (exchange.getLocation() != null)
				insertLocation(exchange.getLocation());
		}

	}

	private void insertLocation(String locationCode) {
		if (locationCode == null)
			return;
		String genKey = KeyGen.get(locationCode);
		if (db.findLocation(locationCode, genKey) != null) {
			log.info("Location {} already exists", genKey);
		} else {
			try {
				Location location = new Location();
				location.setRefId(genKey);
				location.setName(locationCode);
				location.setCode(locationCode);
				db.put(location, genKey);
				log.info("Location {} has been saved ", genKey);
			} catch (Exception e) {
				log.info("Error " + e);
			}
		}
	}

	private void insertLocation(String locationCode,String lat, String longitude) {
		if (locationCode == null)
			return;
		String genKey = KeyGen.get(locationCode);
		if (db.findLocation(locationCode, genKey) != null) {
			log.info("Location {} already exists", genKey);
		} else {
			try {
				Location location = new Location();
				location.setRefId(genKey);
				location.setName(locationCode);
				if(lat != null)
					location.setLatitude(Double.parseDouble(lat));
				if(longitude != null)
					location.setLongitude(Double.parseDouble(longitude));
				location.setCode(locationCode);
				db.put(location, genKey);
				log.info("Location {} has been saved ", genKey);
			} catch (Exception e) {
				log.info("Error " + e);
			}
		}
	}	
	
	private void insertProcess(IDataSet dataSet) {
		String processname=buildProcessName(dataSet);
		String processId = UsdaKeyGen.forProcess(dataSet,processname);
		fireProcessEvent(processId);
		Process process = db.get(Process.class, processId);
		if (process != null) {
			log.info("Process {} already exists ", processId);
			return;
		}
		try {
			process = new Process();
			process.setRefId(processId);			
			ProcessDocumentation documentation = Mapper
					.mapProcessDocumentationEntities(dataSet);
			process.setDocumentation(documentation);

			if (dataSet.getReferenceFunction() != null) {
				process.setDescription(dataSet.getReferenceFunction()
						.getGeneralComment());
				process.setInfrastructureProcess(dataSet.getReferenceFunction()
						.isInfrastructureProcess());
			}
			process.setProcessType(Mapper.getProcessType());

		//	SubmissionMetaElements submission = Mapper
		//			.mapSubmissionDocumentationEntities(dataSet);
		//??	process.setSubmissionMetaElements(submission);

			mapTimeAndGeographyEntities(dataSet, process, documentation);

			if (dataSet.getProcessInformation().getTechnology() != null
					&& dataSet.getProcessInformation().getTechnology()
							.getText() != null)
				documentation.setTechnology(Strings.cut((dataSet
						.getProcessInformation().getTechnology().getText()),
						65500));

			mapExchangesEntities(dataSet.getExchanges(), process);

			Mapper.mapParametersEntities(dataSet.getParameters(), process);
			process.setName(processname);
			if (dataSet.getReferenceFunction() != null)
				mapProcessEntities(dataSet, process, documentation);

			/*
			  if (dataSet.getAllocations() != null &&
					  dataSet.getAllocations().size() > 0) { mapAllocations(process,
							  dataSet.getAllocations());
					  process.setDefaultAllocationMethod(AllocationMethod.CAUSAL); 
			  }
			
			Mapper.mapCostCategoryEntities(dataSet.getProcessInformation()
					.getCosts(), process);
*/
			mapActors(documentation, dataSet);
			mapSources(documentation, dataSet);
			mapAllocations(process,dataSet);
			db.put(process, processId);
			log.info("Process {} has been saved ", processId);
		} catch (Exception e) {
			log.info("Error " + e);
		}
		localExchangeCache.clear();
	}

	private void mapTimeAndGeographyEntities(IDataSet dataSet, Process process,
			ProcessDocumentation documentation) {
		// ask ProcessTime processTime = new
		// ProcessTime(dataSet.getTimePeriod());
		// processTime.map(documentation);
		if (dataSet.getProcessInformation().getGeography() != null) {
			String locationCode = dataSet.getProcessInformation()
					.getGeography().getLocation();
			if (locationCode != null) {
				String genKey = KeyGen.get(locationCode);
				process.setLocation(db.findLocation(locationCode, genKey));
			}
			documentation.setGeography(dataSet.getProcessInformation()
					.getGeography().getText());
		}
	}
	/**
	*	Transfers Exchange entities to a Process.  Exchanges with null amounts are ignored
	*/
	private void mapExchangesEntities(
			List<gov.usda.nal.lci.template.domain.Exchange> inExchanges,
			Process ioProcess) {
		int exchangecnt=0;
		for (gov.usda.nal.lci.template.domain.Exchange inExchange : inExchanges) {
			try {
				if ( inExchange.getAmountValue() == null ) {
					log.info("Blank amount for "+inExchange.getFlowName()+" will be ignored.");
					continue;
				}
				
				FlowBucket flow = flowImport.handleProcessExchange(inExchange);
				if (flow == null || !flow.isValid()) {
					log.error("Could not import flow {}", inExchange);
					continue;
				}
			
				Exchange outExchange = new Exchange();
				outExchange.setPedigreeUncertainty(inExchange
						.getDataQualityComment());
				outExchange.description=inExchange.getExchangeDescription();				
				outExchange.setFlow(flow.flow);
				outExchange.setUnit(flow.unit);
				outExchange.setFlowPropertyFactor(flow.flowProperty);
				outExchange.setInput(inExchange.getInputGroup() != null);
				outExchange.setAmountFormula(inExchange.getParameterName());
				
				ExchangeAmount exchangeAmount = new ExchangeAmount(outExchange,
						inExchange);
				exchangeAmount.map(flow.conversionFactor);
				ioProcess.getExchanges().add(outExchange);
				if (ioProcess.getQuantitativeReference() == null
						&& inExchange.getOutputGroup() != null
						&& (inExchange.getOutputGroup() == 0 || inExchange
								.getOutputGroup() == 2)) {
					ioProcess.setQuantitativeReference(outExchange);
				}
				//
				if(inExchange.getDefaultProvider() != null)
				{
					long processId = searchProcessByRefStr(inExchange.getDefaultProvider());
					if(processId > 0)
						outExchange.setDefaultProviderId(processId);
				}
					
				// tbl_parameters.distribution_type
				if (inExchange.getUncertaintyType() != null) {
					Uncertainty uncertainty = new Uncertainty();
					uncertainty.setDistributionType(inExchange
							.getUncertaintyType());

					if (inExchange.getUncertaintyType().equals(
							UncertaintyType.LOG_NORMAL)) {
						double gmean = inExchange.getExpectedValue();
						double gsd = inExchange.getDispersion();
						uncertainty.setParameter1Value(gmean);
						uncertainty.setParameter2Value(gsd);
						outExchange.setUncertainty(uncertainty);
					}
					else if (inExchange.getUncertaintyType().equals(
							UncertaintyType.NORMAL)) {
						double mean = inExchange.getExpectedValue();
						double sd = inExchange.getDispersion();
						uncertainty.setParameter1Value(mean);
						uncertainty.setParameter2Value(sd);

						outExchange.setUncertainty(uncertainty);
					}
					else if (inExchange.getUncertaintyType().equals(
							UncertaintyType.TRIANGLE)) {
						double min = inExchange.getMinValue();
						double mode = inExchange.getExpectedValue();
						double max = inExchange.getMaxValue();
						uncertainty.setParameter1Value(min);
						uncertainty.setParameter2Value(mode);
						uncertainty.setParameter3Value(max);
						outExchange.setUncertainty(uncertainty);
					}
					else if (inExchange.getUncertaintyType().equals(
							UncertaintyType.UNIFORM)) {
						uncertainty.setParameter1Value(inExchange
								.getMinValue());
						uncertainty.setParameter3Value(inExchange
								.getMaxValue());
						outExchange.setUncertainty(uncertainty);
					}

				}
				localExchangeCache.put(exchangecnt++, outExchange);
			} catch (Exception e) {

			}
		}
	}

	private long searchProcessByRefStr(String defaultProvider) {
		String[] processIdVals =  defaultProvider.split("/");	
		Process existingProc = processDao.getForRefId(KeyGen.get(processIdVals));
		if (existingProc != null)
			return existingProc.getId();
		
		Set<Long> ids = queryForIds(getProcessSearchQuery(processIdVals));
		if(ids.isEmpty())
			return 0;
		else if (ids.size() > 1)
			return 0;
		else
			return ids.iterator().next();
	
	}
	
	Set<Long> queryForIds(String query) {
		Set<Long> ids = new HashSet<>();
		try {
			NativeSql.on(iDatabase).query(query, (result) -> {
				ids.add(result.getLong(1));
				return result.next();
			});
		} catch (SQLException e) {
			log.error("Error executing native query '" + query + "'");
		}
		return ids;
	}

	private String getProcessSearchQuery(String[] metaVals) {
		StringBuilder query = new StringBuilder();
		query.append("SELECT DISTINCT p.id  ");
		query.append ("FROM ");
		query.append ("tbl_processes p left outer join tbl_locations l on  p.f_location=l.id, tbl_categories c,tbl_categories cp,tbl_exchanges e,tbl_units u ");
		query.append("WHERE c.id=p.f_category AND c.f_category = cp.id AND p.f_quantitative_reference = e.id AND e.f_unit = u.id");
		query.append(" AND cp.name = '" + metaVals[0]);
		query.append("' AND c.name = '" + metaVals[1]);
		query.append("' AND p.name = '" + metaVals[2]);
		query.append("' AND u.name = '" + metaVals[3]);
		if (StringUtils.isNotBlank(metaVals[4])) 
			query.append("' AND l.code = '" + metaVals[4]);
		int intVal = (Boolean.parseBoolean(metaVals[5]))? 1 : 0;
		query.append("' AND p.infrastructure_process = " + intVal);
		query.append(" AND p.process_type = '" + metaVals[6] + "'");
		return query.toString();
	}
	
	private void mapActors(ProcessDocumentation doc, IDataSet dataSet) {

		Map<Long, Actor> actors = new HashMap<Long, Actor>();
		gov.usda.nal.lci.template.domain.AdministrativeInformation adm = dataSet
				.getProcessInformation().getAdministrativeInformation();
		gov.usda.nal.lci.template.domain.ModellingAndValidation mod = dataSet
				.getProcessInformation().getModelingAndValidation();

		for (gov.usda.nal.lci.template.domain.Person person : dataSet
				.getPersons()) {
			Actor actor = db.findActor(person, UsdaKeyGen.forPerson(person));
			if (actor != null) {
				actors.put(person.getId(), actor);

				if (person.getName().equalsIgnoreCase(adm.getDataOwner().getName()))
					doc.setDataSetOwner(actors.get(person.getId()));
				if (person.getName().equalsIgnoreCase(adm.getDataGenerator().getName()))
					doc.setDataGenerator(actors.get(person.getId()));
				if (person.getName().equalsIgnoreCase(adm.getDataDocumentor().getName()))
					doc.setDataDocumentor(actors.get(person.getId()));
				if (person.getName().equalsIgnoreCase(mod.getPrimaryReviewer().getName()))
					doc.setReviewer(actors.get(person.getId()));

			}

		}

	}


	private void mapProcessEntities(IDataSet dataSet, Process ioProcess,
			ProcessDocumentation doc) {
		if (dataSet.getReferenceFunction() == null)
			return;

		//String processName = buildProcessName(dataSet);
		//if (processName != null)
		//	ioProcess.setName(processName);
		ioProcess.setDescription(dataSet.getReferenceFunction()
				.getGeneralComment());
		ioProcess.setInfrastructureProcess(dataSet.getReferenceFunction()
				.isInfrastructureProcess());
		String topCategoryName = dataSet.getReferenceFunction().getCategory();
		String subCategoryName = dataSet.getReferenceFunction()
				.getSubcategory();
		Category cat = null;
		if (processCategory != null)
			cat = db.getPutCategory(processCategory, topCategoryName,
					subCategoryName);
		else
			cat = db.getPutCategory(ModelType.PROCESS, null,topCategoryName,
					true);
			//cat = db.getPutCategory(ModelType.PROCESS, null,topCategoryName,
				//	subCategoryName);
		ioProcess.setCategory(cat);

	}

	private String buildProcessName(IDataSet dataSet) {

		StringBuilder builder = new StringBuilder();
		builder.append(dataSet.getProcessInformation().getProcessName()
				.getBaseName());

		if (StringUtils.isNotBlank(dataSet.getProcessInformation()
				.getProcessName().getTreatmentStandardsRoutes())) {
			builder.append("; "
					+ dataSet.getProcessInformation().getProcessName()
							.getTreatmentStandardsRoutes());
		}
		if (StringUtils.isNotBlank(dataSet.getProcessInformation()
				.getProcessName().getLocationType())) {
			builder.append("; "
					+ dataSet.getProcessInformation().getProcessName()
							.getLocationType());
		}
		if (StringUtils.isNotBlank(dataSet.getProcessInformation()
				.getProcessName().getMixType())) {
			builder.append("; "
					+ dataSet.getProcessInformation().getProcessName()
							.getMixType());
		}
		if (StringUtils.isNotBlank(dataSet.getProcessInformation()
				.getProcessName().getQuantitativeFlowProperties())) {
			builder.append("; "
					+ dataSet.getProcessInformation().getProcessName()
							.getQuantitativeFlowProperties());
		}

		return builder.toString();

	}
	

	private void mapSources(ProcessDocumentation doc, IDataSet dataSet) {
		gov.usda.nal.lci.template.domain.AdministrativeInformation adm = dataSet
				.getProcessInformation().getAdministrativeInformation();
		
		Map<Long, Source> sources = new HashMap<Long, Source>();
		List<Source> sourceList = new ArrayList<Source>();
		for (gov.usda.nal.lci.template.domain.Source isource : dataSet
				.getSources()) {
			Source s_data = db.findSource(isource,
					UsdaKeyGen.forSource(isource));
			if (s_data != null) {
				sources.put(isource.getId(), s_data);
				doc.getSources().add(s_data);
				if (isource.getFirstAuthor().equalsIgnoreCase(adm.getPublication().getFirstAuthor()))
					doc.setPublication(sources.get(isource.getId()));
				sourceList.add(s_data);

			}
		}

	}
	/**
	 * <code>mapAllocations</code> iterates over the list of Allocation objects we've gathered
	 * and attempts to link each to localExchangeCache
	 * @param process
	 * @param dataset
	 */
	private void mapAllocations(Process process, IDataSet dataset) {
		for ( gov.usda.nal.lci.template.domain.Allocation ialloc : dataset.getAllocations() )
		{
			//iterate our localExchangeCache and match up to allocation objects
			for ( int i=0;i<localExchangeCache.size();i++ )
			{
				Exchange product=localExchangeCache.get(i);
				if ( product.getFlow().getName().equals(ialloc.getReferenceToCoProduct()))
				{
					
					AllocationFactor allocationFactor = new AllocationFactor();
					allocationFactor.setProductId(product.getFlow().getId());
					//force our float into the double preserving the decimal places
					double factor = Math.round(ialloc.getValue() * 10000000d) / 10000000d;
					allocationFactor.setValue(factor);
					allocationFactor.setAllocationType(ialloc.getAllocationMethod());
					// get flow exchange for Causal 
					if ( ialloc.getAllocationMethod() == AllocationMethod.CAUSAL)
					{
						//find exchange to make 
						int n=findAllocationExchangeFromLocalExchangeCache(ialloc);
						if ( n >= 0 )
							allocationFactor.setExchange(localExchangeCache.get(n));
						else {
							log.error("Cannot find exchange for "+ialloc.getReferenceToInputOutput());
						}
							
					}
					process.getAllocationFactors().add(allocationFactor);
					break;
				}
			}
		}
	}
	private int findAllocationExchangeFromLocalExchangeCache(gov.usda.nal.lci.template.domain.Allocation a)
	{
		int j=-1;
		for ( int i=0;i<localExchangeCache.size();i++) {
			if ( localExchangeCache.get(i).getFlow().getName().equals(a.getReferenceToInputOutput()))
			{
				j=i;
				break;
			}
		}
		return j;
	}
}
