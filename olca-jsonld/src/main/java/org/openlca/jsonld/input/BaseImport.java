package org.openlca.jsonld.input;

import org.openlca.core.model.ModelType;
import org.openlca.core.model.RootEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

abstract class BaseImport<T extends RootEntity> {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ModelType modelType;
	String refId;
	ImportConfig conf;

	BaseImport(ModelType modelType, String refId, ImportConfig conf) {
		this.refId = refId;
		this.conf = conf;
		this.modelType = modelType;
	}

	final T run() {
		if (refId == null || conf == null)
			return null;
		try {
			T model = get(refId);
			JsonObject json = conf.store.get(modelType, refId);
			if (!doImport(model, json))
				return model;
			long id = model != null ? model.getId() : 0l;
			return map(json, id);
		} catch (Exception e) {
			log.error("failed to import actor " + refId, e);
			return null;
		}
	}

	private boolean doImport(T model, JsonObject json) {
		if (model == null)
			return true;
		long jsonVersion = In.getVersion(json);
		long jsonDate = In.getLastChange(json);
		if (jsonVersion < model.getVersion())
			return false;
		if (jsonVersion == model.getVersion()
				&& jsonDate <= model.getLastChange())
			return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	private T get(String refId) {
		switch (modelType) {
		case ACTOR:
			return (T) conf.db.getActor(refId);
		case CATEGORY:
			return (T) conf.db.getCategory(refId);
		case CURRENCY:
			return (T) conf.db.getCurrency(refId);
		case FLOW:
			return (T) conf.db.getFlow(refId);
		case FLOW_PROPERTY:
			return (T) conf.db.getFlowProperty(refId);
		case IMPACT_METHOD:
			return (T) conf.db.getMethod(refId);
		case LOCATION:
			return (T) conf.db.getLocation(refId);
		case PROCESS:
			return (T) conf.db.getProcess(refId);
		case SOCIAL_INDICATOR:
			return (T) conf.db.getSocialIndicator(refId);
		case SOURCE:
			return (T) conf.db.getSource(refId);
		case UNIT_GROUP:
			return (T) conf.db.getUnitGroup(refId);
		default:
			throw new RuntimeException(modelType.name() + " not supported");
		}
	}

	abstract T map(JsonObject json, long id);

}