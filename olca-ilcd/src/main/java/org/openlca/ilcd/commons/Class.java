
package org.openlca.ilcd.commons;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.namespace.QName;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ClassType", propOrder = {
		"value"
})
public class Class implements Serializable {

	private final static long serialVersionUID = 1L;

	@XmlValue
	public String value;

	@XmlAttribute(name = "level", required = true)
	public BigInteger level;

	@XmlAttribute(name = "classId")
	public String classId;

	@XmlAnyAttribute
	public final Map<QName, String> otherAttributes = new HashMap<>();

}
