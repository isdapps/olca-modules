
package org.openlca.ilcd.flows;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.openlca.ilcd.commons.DataSetReference;
import org.openlca.ilcd.commons.FreeText;
import org.openlca.ilcd.commons.Other;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TechnologyType", propOrder = {
		"technologicalApplicability",
		"technicalSpecifications",
		"other"
})
public class Technology implements Serializable {

	private final static long serialVersionUID = 1L;

	public List<FreeText> technologicalApplicability;

	@XmlElement(name = "referenceToTechnicalSpecification")
	public List<DataSetReference> technicalSpecifications;

	@XmlElement(namespace = "http://lca.jrc.it/ILCD/Common")
	public Other other;

}
