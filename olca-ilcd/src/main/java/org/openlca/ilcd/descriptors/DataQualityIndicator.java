
package org.openlca.ilcd.descriptors;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "dataQualityIndicator")
public class DataQualityIndicator implements Serializable {

	private final static long serialVersionUID = 1L;

	@XmlAttribute(name = "name", required = true)
	public DataQualityIndicatorValues name;

	@XmlAttribute(name = "value", required = true)
	public QualityValues value;

}
