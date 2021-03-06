
package org.openlca.ilcd.processes;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AllocationType")
public class AllocationFactor implements Serializable {

	private final static long serialVersionUID = 1L;

	@XmlAttribute(name = "internalReferenceToCoProduct")
	public int referenceToCoProduct;

	@XmlAttribute(name = "allocatedFraction")
	public double allocatedFraction;

}
