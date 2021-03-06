package org.openlca.io.ilcd.output;

import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.Version;
import org.openlca.ilcd.commons.ClassificationInfo;
import org.openlca.ilcd.commons.DataSetReference;
import org.openlca.ilcd.commons.LangString;
import org.openlca.ilcd.flowproperties.AdminInfo;
import org.openlca.ilcd.flowproperties.DataEntry;
import org.openlca.ilcd.flowproperties.DataSetInfo;
import org.openlca.ilcd.flowproperties.FlowProperty;
import org.openlca.ilcd.flowproperties.FlowPropertyInfo;
import org.openlca.ilcd.flowproperties.Publication;
import org.openlca.ilcd.flowproperties.QuantitativeReference;
import org.openlca.ilcd.io.DataStoreException;
import org.openlca.ilcd.util.Reference;

public class FlowPropertyExport {

	private final ExportConfig config;
	private org.openlca.core.model.FlowProperty flowProperty;
	private String baseUri;

	public FlowPropertyExport(ExportConfig config) {
		this.config = config;
	}

	public void setBaseUri(String baseUri) {
		this.baseUri = baseUri;
	}

	public FlowProperty run(org.openlca.core.model.FlowProperty property)
			throws DataStoreException {
		if (config.store.contains(FlowProperty.class, property.getRefId()))
			return config.store.get(FlowProperty.class, property.getRefId());
		this.flowProperty = property;
		FlowProperty iProperty = new FlowProperty();
		iProperty.version = "1.1";
		FlowPropertyInfo info = new FlowPropertyInfo();
		iProperty.flowPropertyInformation = info;
		info.dataSetInformation = makeDataSetInfo();
		info.quantitativeReference = makeUnitGroupRef();
		iProperty.administrativeInformation = makeAdminInfo();
		config.store.put(iProperty, property.getRefId());
		this.flowProperty = null;
		return iProperty;
	}

	private DataSetInfo makeDataSetInfo() {
		DataSetInfo dataSetInfo = new DataSetInfo();
		dataSetInfo.uuid = flowProperty.getRefId();
		LangString.set(dataSetInfo.name, flowProperty.getName(),
				config.lang);
		if (flowProperty.getDescription() != null) {
			LangString.set(dataSetInfo.generalComment,
					flowProperty.getDescription(), config.lang);
		}
		CategoryConverter converter = new CategoryConverter();
		ClassificationInfo classInfo = converter
				.getClassificationInformation(flowProperty.getCategory());
		dataSetInfo.classificationInformation = classInfo;
		return dataSetInfo;
	}

	private QuantitativeReference makeUnitGroupRef() {
		QuantitativeReference qRef = new QuantitativeReference();
		UnitGroup unitGroup = flowProperty.getUnitGroup();
		DataSetReference ref = ExportDispatch.forwardExportCheck(unitGroup,
				config);
		qRef.unitGroup = ref;
		return qRef;
	}

	private AdminInfo makeAdminInfo() {
		AdminInfo info = new AdminInfo();
		DataEntry entry = new DataEntry();
		info.dataEntry = entry;
		entry.timeStamp = Out.getTimestamp(flowProperty);
		entry.referenceToDataSetFormat.add(
				Reference.forIlcdFormat());
		addPublication(info);
		return info;
	}

	private void addPublication(AdminInfo info) {
		Publication pub = new Publication();
		info.publication = pub;
		pub.dataSetVersion = Version.asString(flowProperty.getVersion());
		if (baseUri == null)
			baseUri = "http://openlca.org/ilcd/resource/";
		if (!baseUri.endsWith("/"))
			baseUri += "/";
		pub.permanentDataSetURI = baseUri + "flowproperties/" + flowProperty.getRefId();
	}

}
