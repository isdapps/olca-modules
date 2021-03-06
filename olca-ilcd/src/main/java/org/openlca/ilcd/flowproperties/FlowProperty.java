
package org.openlca.ilcd.flowproperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.openlca.ilcd.commons.Other;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FlowPropertyDataSetType", propOrder = {
		"flowPropertyInformation",
		"modellingAndValidation",
		"administrativeInformation",
		"other"
})
public class FlowProperty implements Serializable {

	private final static long serialVersionUID = 1L;

	@XmlElement(name = "flowPropertiesInformation", required = true)
	public FlowPropertyInfo flowPropertyInformation;

	public ModellingAndValidation modellingAndValidation;

	public AdminInfo administrativeInformation;

	@XmlElement(namespace = "http://lca.jrc.it/ILCD/Common")
	public Other other;

	@XmlAttribute(name = "version", required = true)
	public String version;

	@XmlAnyAttribute
	public final Map<QName, String> otherAttributes = new HashMap<>();

}
