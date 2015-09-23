/**
 */
package xtext.objectsModel.impl;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import xtext.objectsModel.Impact;
import xtext.objectsModel.ObjectsModelPackage;
import xtext.objectsModel.WorkItem;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Impact</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link xtext.objectsModel.impl.ImpactImpl#getImpactWI <em>Impact WI</em>}</li>
 *   <li>{@link xtext.objectsModel.impl.ImpactImpl#getLikelihood <em>Likelihood</em>}</li>
 *   <li>{@link xtext.objectsModel.impl.ImpactImpl#getImpact <em>Impact</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ImpactImpl extends MinimalEObjectImpl.Container implements Impact
{
  /**
   * The cached value of the '{@link #getImpactWI() <em>Impact WI</em>}' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getImpactWI()
   * @generated
   * @ordered
   */
  protected WorkItem impactWI;

  /**
   * The default value of the '{@link #getLikelihood() <em>Likelihood</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getLikelihood()
   * @generated
   * @ordered
   */
  protected static final double LIKELIHOOD_EDEFAULT = 0.0;

  /**
   * The cached value of the '{@link #getLikelihood() <em>Likelihood</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getLikelihood()
   * @generated
   * @ordered
   */
  protected double likelihood = LIKELIHOOD_EDEFAULT;

  /**
   * The default value of the '{@link #getImpact() <em>Impact</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getImpact()
   * @generated
   * @ordered
   */
  protected static final double IMPACT_EDEFAULT = 0.0;

  /**
   * The cached value of the '{@link #getImpact() <em>Impact</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getImpact()
   * @generated
   * @ordered
   */
  protected double impact = IMPACT_EDEFAULT;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected ImpactImpl()
  {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  protected EClass eStaticClass()
  {
    return ObjectsModelPackage.Literals.IMPACT;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public WorkItem getImpactWI()
  {
    if (impactWI != null && impactWI.eIsProxy())
    {
      InternalEObject oldImpactWI = (InternalEObject)impactWI;
      impactWI = (WorkItem)eResolveProxy(oldImpactWI);
      if (impactWI != oldImpactWI)
      {
        if (eNotificationRequired())
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, ObjectsModelPackage.IMPACT__IMPACT_WI, oldImpactWI, impactWI));
      }
    }
    return impactWI;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public WorkItem basicGetImpactWI()
  {
    return impactWI;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setImpactWI(WorkItem newImpactWI)
  {
    WorkItem oldImpactWI = impactWI;
    impactWI = newImpactWI;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, ObjectsModelPackage.IMPACT__IMPACT_WI, oldImpactWI, impactWI));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public double getLikelihood()
  {
    return likelihood;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setLikelihood(double newLikelihood)
  {
    double oldLikelihood = likelihood;
    likelihood = newLikelihood;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, ObjectsModelPackage.IMPACT__LIKELIHOOD, oldLikelihood, likelihood));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public double getImpact()
  {
    return impact;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setImpact(double newImpact)
  {
    double oldImpact = impact;
    impact = newImpact;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, ObjectsModelPackage.IMPACT__IMPACT, oldImpact, impact));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType)
  {
    switch (featureID)
    {
      case ObjectsModelPackage.IMPACT__IMPACT_WI:
        if (resolve) return getImpactWI();
        return basicGetImpactWI();
      case ObjectsModelPackage.IMPACT__LIKELIHOOD:
        return getLikelihood();
      case ObjectsModelPackage.IMPACT__IMPACT:
        return getImpact();
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public void eSet(int featureID, Object newValue)
  {
    switch (featureID)
    {
      case ObjectsModelPackage.IMPACT__IMPACT_WI:
        setImpactWI((WorkItem)newValue);
        return;
      case ObjectsModelPackage.IMPACT__LIKELIHOOD:
        setLikelihood((Double)newValue);
        return;
      case ObjectsModelPackage.IMPACT__IMPACT:
        setImpact((Double)newValue);
        return;
    }
    super.eSet(featureID, newValue);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public void eUnset(int featureID)
  {
    switch (featureID)
    {
      case ObjectsModelPackage.IMPACT__IMPACT_WI:
        setImpactWI((WorkItem)null);
        return;
      case ObjectsModelPackage.IMPACT__LIKELIHOOD:
        setLikelihood(LIKELIHOOD_EDEFAULT);
        return;
      case ObjectsModelPackage.IMPACT__IMPACT:
        setImpact(IMPACT_EDEFAULT);
        return;
    }
    super.eUnset(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public boolean eIsSet(int featureID)
  {
    switch (featureID)
    {
      case ObjectsModelPackage.IMPACT__IMPACT_WI:
        return impactWI != null;
      case ObjectsModelPackage.IMPACT__LIKELIHOOD:
        return likelihood != LIKELIHOOD_EDEFAULT;
      case ObjectsModelPackage.IMPACT__IMPACT:
        return impact != IMPACT_EDEFAULT;
    }
    return super.eIsSet(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public String toString()
  {
    if (eIsProxy()) return super.toString();

    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (likelihood: ");
    result.append(likelihood);
    result.append(", impact: ");
    result.append(impact);
    result.append(')');
    return result.toString();
  }

} //ImpactImpl
