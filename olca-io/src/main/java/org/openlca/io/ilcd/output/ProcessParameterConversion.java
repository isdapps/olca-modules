package org.openlca.io.ilcd.output;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.Process;
import org.openlca.ilcd.util.LangString;
import org.openlca.ilcd.util.ParameterExtension;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProcessParameterConversion {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ExportConfig config;

	public ProcessParameterConversion(ExportConfig config) {
		this.config = config;
	}

	public List<org.openlca.ilcd.processes.Parameter> run(Process process) {
		log.trace("Create process parameters.");
		List<org.openlca.ilcd.processes.Parameter> params = processParams(process);
		try {
			addDatabaseParams(params);
		} catch (Exception e) {
			log.error("Failed to add database parameters", e);
		}
		return params;
	}

	private void addDatabaseParams(
			List<org.openlca.ilcd.processes.Parameter> params) {
		ParameterDao dao = new ParameterDao(config.db);
		for (Parameter param : dao.getGlobalParameters()) {
			if (!valid(param))
				continue;
			org.openlca.ilcd.processes.Parameter iParam = convertParam(param);
			params.add(iParam);
			addScope(iParam, ParameterScope.GLOBAL);
		}
	}

	private List<org.openlca.ilcd.processes.Parameter> processParams(
			Process process) {
		List<org.openlca.ilcd.processes.Parameter> iParameters = new ArrayList<>();	
		ParameterDao dao = new ParameterDao(config.db);
		//Hibernate.initialize(process.getParameters());
		//for (Parameter oParam : process.getParameters()) {
		for (Parameter oParam : dao.getPrametersForProcess(process.getId())) {
			if (!valid(oParam))
				continue;
			org.openlca.ilcd.processes.Parameter iParam = convertParam(oParam);
			iParameters.add(iParam);
			addScope(iParam, ParameterScope.PROCESS);
		}
		return iParameters;
	}

	private org.openlca.ilcd.processes.Parameter convertParam(Parameter oParam) {
		org.openlca.ilcd.processes.Parameter iParameter = new org.openlca.ilcd.processes.Parameter();
		iParameter.setName(oParam.getName());
		iParameter.setFormula(oParam.getFormula());
		iParameter.setMeanValue(oParam.getValue());
		new UncertaintyConverter().map(oParam, iParameter);
		if (Strings.notEmpty(oParam.getDescription())) {
			iParameter.getComment()
					.add(LangString.label(oParam.getDescription(),
							config.ilcdConfig));
		}
		return iParameter;
	}

	private boolean valid(Parameter param) {
		if (param == null || Strings.nullOrEmpty(param.getName()))
			return false;
		return true;
	}

	private void addScope(org.openlca.ilcd.processes.Parameter param,
			ParameterScope type) {
		String scope = getScope(type);
		new ParameterExtension(param).setScope(scope);
	}

	private String getScope(ParameterScope type) {
		if (type == null)
			return "unspecified";
		switch (type) {
		case GLOBAL:
			return "global";
		case PROCESS:
			return "process";
		default:
			return "unspecified";
		}
	}
}
