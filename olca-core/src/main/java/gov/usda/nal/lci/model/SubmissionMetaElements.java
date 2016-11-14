package gov.usda.nal.lci.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.model.AbstractEntity;
import org.openlca.core.model.ImpactMethod;

@Entity
@Table(name="tbl_submission_docs")
public class SubmissionMetaElements
  extends AbstractEntity
  implements Serializable, Cloneable
{
  private String baseName;
  private String treatment;
  private String mixType;
  private String quantProduct;
  private String locationDescription;
  private String reliability;
  private String geographicalCoverage;
  private String flowDataCompleteness;
  private String temporalCoverage;
  private String precession;
  private String completeness;
  private String uncertainty;
  private String massBalance;


  @Transient
  private IDatabase database;
  @ManyToMany
  @JoinTable(name="tbl_submission_impact_methods", joinColumns={@javax.persistence.JoinColumn(name="submission_id", referencedColumnName="id")}, inverseJoinColumns={@javax.persistence.JoinColumn(name="impact_methods_id", referencedColumnName="id")})
  private List<ImpactMethod> impactMethods = new ArrayList();
  
  public String getBaseName()
  {
    return this.baseName;
  }
  
  public void setBaseName(String baseName)
  {
    this.baseName = baseName;
  }
  
  public String getTreatment()
  {
    return this.treatment;
  }
  
  public void setTreatment(String treatment)
  {
    this.treatment = treatment;
  }
  
  public String getMixType()
  {
    return this.mixType;
  }
  
  public void setMixType(String mixType)
  {
    this.mixType = mixType;
  }
  
  public String getQuantProduct()
  {
    return this.quantProduct;
  }
  
  public void setQuantProduct(String quantProduct)
  {
    this.quantProduct = quantProduct;
  }
  
  public String getReliability()
  {
    return this.reliability;
  }
  
  public void setReliability(String reliability)
  {
    this.reliability = reliability;
  }
  
  public String getGeographicalCoverage()
  {
    return this.geographicalCoverage;
  }
  
  public void setGeographicalCoverage(String geographicalCoverage)
  {
    this.geographicalCoverage = geographicalCoverage;
  }
  
  public String getFlowDataCompleteness()
  {
    return this.flowDataCompleteness;
  }
  
  public void setFlowDataCompleteness(String flowDataCompleteness)
  {
    this.flowDataCompleteness = flowDataCompleteness;
  }
  
  public String getTemporalCoverage()
  {
    return this.temporalCoverage;
  }
  
  public void setTemporalCoverage(String temporalCoverage)
  {
    this.temporalCoverage = temporalCoverage;
  }
  
  public String getPrecession()
  {
    return this.precession;
  }
  
  public void setPrecession(String precession)
  {
    this.precession = precession;
  }
  
  public String getCompleteness()
  {
    return this.completeness;
  }
  
  public void setCompleteness(String completeness)
  {
    this.completeness = completeness;
  }
  
  public String getUncertainty()
  {
    return this.uncertainty;
  }
  
  public void setUncertainty(String uncertainty)
  {
    this.uncertainty = uncertainty;
  }
 
  public String getMassBalance() {
	return massBalance;
  }

  public void setMassBalance(String massBalance) {
	this.massBalance = massBalance;
  }
  
  public List<ImpactMethod> getImpactmethods()
  {
    return this.impactMethods;
  }
  
  public void setImpactmethods(List<ImpactMethod> impactMethods)
  {
    this.impactMethods = impactMethods;
  }
  
  public void setDatabase(IDatabase database)
  {
    this.database = database;
  }
  
  public void setAndRetriveImpactmethods(String[] methods)
  {
    ImpactMethodDao imDao = new ImpactMethodDao(this.database);
    List<ImpactMethod> imList = new ArrayList();
    for (String s : methods)
    {
	  System.out.println("Adding Impact method :" + s);
	  if(s != null && !s.equals(""))
	  {
		ImpactMethod im = (ImpactMethod)imDao.getForId(Long.parseLong(s));
		this.impactMethods.add(im);
	  }
    }
  }
  
  public String getLocationDescription()
  {
    return this.locationDescription;
  }
  
  public void setLocationDescription(String locationDescription)
  {
    this.locationDescription = locationDescription;
  }
}
