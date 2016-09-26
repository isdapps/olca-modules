
package org.openlca.ilcd.processes;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.openlca.ilcd.commons.Compliance;
import org.openlca.ilcd.commons.DataSetReference;
import org.openlca.ilcd.commons.Other;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ComplianceType", propOrder = {
		"referenceToComplianceSystem",
		"approvalOfOverallCompliance",
		"nomenclatureCompliance",
		"methodologicalCompliance",
		"reviewCompliance",
		"documentationCompliance",
		"qualityCompliance",
		"other"
})
public class ComplianceDeclaration implements Serializable {

	private final static long serialVersionUID = 1L;

	@XmlElement(namespace = "http://lca.jrc.it/ILCD/Common", required = true)
	public DataSetReference referenceToComplianceSystem;

	@XmlElement(namespace = "http://lca.jrc.it/ILCD/Common")
	public Compliance approvalOfOverallCompliance;

	@XmlElement(namespace = "http://lca.jrc.it/ILCD/Common")
	public Compliance nomenclatureCompliance;

	@XmlElement(namespace = "http://lca.jrc.it/ILCD/Common")
	public Compliance methodologicalCompliance;

	@XmlElement(namespace = "http://lca.jrc.it/ILCD/Common")
	public Compliance reviewCompliance;

	@XmlElement(namespace = "http://lca.jrc.it/ILCD/Common")
	public Compliance documentationCompliance;

	@XmlElement(namespace = "http://lca.jrc.it/ILCD/Common")
	public Compliance qualityCompliance;

	@XmlElement(namespace = "http://lca.jrc.it/ILCD/Common")
	public Other other;

	@XmlAnyAttribute
	public final Map<QName, String> otherAttributes = new HashMap<>();

}
