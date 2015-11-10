package org.openlca.jsonld.output;

import java.util.Objects;
import java.util.function.Consumer;

import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessDocumentation;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.Source;
import org.openlca.jsonld.Dates;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

class ProcessWriter extends Writer<Process> {

	private Process process;
	private Consumer<RootEntity> refFn;

	@Override
	JsonObject write(Process process, Consumer<RootEntity> refFn) {
		JsonObject obj = super.write(process, refFn);
		if (obj == null)
			return null;
		this.process = process;
		this.refFn = refFn;
		ProcessType type = process.getProcessType();
		if (type != null)
			obj.addProperty("processTyp", type.name());
		obj.addProperty("defaultAllocationMethod",
				getAllocationType(process.getDefaultAllocationMethod()));
		obj.add("location", References.create(process.getLocation(), refFn));
		obj.add("processDocumentation", createDoc());
		mapParameters(obj);
		mapExchanges(obj);
		return obj;
	}

	private void mapParameters(JsonObject obj) {
		JsonArray parameters = new JsonArray();
		for (Parameter p : process.getParameters()) {
			JsonObject pObj = new ParameterWriter().write(p, ref -> {
			});
			parameters.add(pObj);
		}
		obj.add("parameters", parameters);
	}

	private void mapExchanges(JsonObject obj) {
		JsonArray exchanges = new JsonArray();
		for (Exchange e : process.getExchanges()) {
			JsonObject eObj = new JsonObject();
			Exchanges.map(e, eObj, refFn);
			if (Objects.equals(process.getQuantitativeReference(), e))
				eObj.addProperty("quantitativeReference", true);
			exchanges.add(eObj);
		}
		obj.add("exchanges", exchanges);
	}

	private JsonObject createDoc() {
		ProcessDocumentation d = process.getDocumentation();
		if (d == null)
			return null;
		JsonObject o = new JsonObject();
		o.addProperty("@type", "ProcessDocumentation");
		mapSimpleDocFields(d, o);
		o.add("reviewer", References.create(d.getReviewer(), refFn));
		o.add("dataDocumentor", References.create(d.getDataDocumentor(), refFn));
		o.add("dataGenerator", References.create(d.getDataGenerator(), refFn));
		o.add("dataSetOwner", References.create(d.getDataSetOwner(), refFn));
		o.add("publication", References.create(d.getPublication(), refFn));
		JsonArray sources = new JsonArray();
		for (Source source : d.getSources())
			sources.add(References.create(source, refFn));
		o.add("sources", sources);
		return o;
	}

	private void mapSimpleDocFields(ProcessDocumentation d, JsonObject o) {
		o.addProperty("timeDescription", d.getTime());
		o.addProperty("technologyDescription", d.getTechnology());
		o.addProperty("dataCollectionDescription", d.getDataCollectionPeriod());
		o.addProperty("completenessDescription", d.getCompleteness());
		o.addProperty("dataSelectionDescription", d.getDataSelection());
		o.addProperty("reviewDetails", d.getReviewDetails());
		o.addProperty("dataTreatmentDescription", d.getDataTreatment());
		o.addProperty("inventoryMethodDescription", d.getInventoryMethod());
		o.addProperty("modelingConstantsDescription", d.getModelingConstants());
		o.addProperty("samplingDescription", d.getSampling());
		o.addProperty("restrictionsDescription", d.getRestrictions());
		o.addProperty("copyright", d.isCopyright());
		o.addProperty("validFrom", Dates.toString(d.getValidFrom()));
		o.addProperty("validUntil", Dates.toString(d.getValidUntil()));
		o.addProperty("creationDate", Dates.toString(d.getCreationDate()));
		o.addProperty("intendedApplication", d.getIntendedApplication());
		o.addProperty("projectDescription", d.getProject());
		o.addProperty("geographyDescription", d.getGeography());
	}

	private String getAllocationType(AllocationMethod method) {
		if (method == null)
			return null;
		switch (method) {
		case CAUSAL:
			return "CAUSAL_ALLOCATION";
		case ECONOMIC:
			return "ECONOMIC_ALLOCATION";
		case PHYSICAL:
			return "PHYSICAL_ALLOCATION";
		default:
			return null;
		}
	}

}
