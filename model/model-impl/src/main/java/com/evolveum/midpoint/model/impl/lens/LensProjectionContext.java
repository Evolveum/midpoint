/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.*;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.apache.commons.lang.StringUtils;
import org.jvnet.jaxb2_commons.lang.Validate;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.Cloner;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 *
 */
public class LensProjectionContext extends LensElementContext<ShadowType> implements ModelProjectionContext {
	
	private ObjectDelta<ShadowType> syncDelta;

	private ObjectDelta<ShadowType> secondaryDelta;
	
	/**
	 * If set to true: absolute state of this projection was detected by the synchronization.
	 * This is mostly for debugging and visibility. It is not used by projection logic.
	 */
	private boolean syncAbsoluteTrigger = false;
	
	/**
	 * The wave in which this resource should be processed. Initial value of -1 means "undetermined".
	 */
	private int wave = -1;
	
	/**
	 * Indicates that the wave computation is still in progress.
	 */
	private transient boolean waveIncomplete = false;

    /**
     * Definition of account type.
     */
    private ResourceShadowDiscriminator resourceShadowDiscriminator;
	
	private boolean fullShadow = false;
	    
    /**
     * True if the account is assigned to the user by a valid assignment. It may be false for accounts that are either
     * found to be illegal by live sync, were unassigned from user, etc.
     * If set to null the situation is not yet known. Null is a typical value when the context is constructed.
     */
    private boolean isAssigned;
    private boolean isAssignedOld;
    
    /**
     * True if the account should be part of the synchronization. E.g. outbound expression should be applied to it.
     */
    private boolean isActive;
    
    /**
     * True if there is a valid assignment for this projection and/or the policy allows such projection to exist.
     */
    private Boolean isLegal = null;
    private Boolean isLegalOld = null;
    
    private boolean isExists;

    /**
     * Decision regarding the account. It indicated what the engine has DECIDED TO DO with the context.
     * If set to null no decision was made yet. Null is also a typical value when the context is created.
     */
    private SynchronizationPolicyDecision synchronizationPolicyDecision;

    /**
     * True if we want to reconcile account in this context.
     */
    private boolean doReconciliation;
    
    /**
     * false if the context should be not taken into the account while synchronizing changes from other resource
     */
    private boolean canProject = true;
    
    

    /**
     * Synchronization situation as it was originally detected by the synchronization code (SynchronizationService).
     * This is mostly for debug purposes. Projector and clockwork do not need to care about this.
     * The synchronization intent is used instead.
     */
    private SynchronizationSituationType synchronizationSituationDetected = null;
    /**
     * Synchronization situation which was the result of synchronization reaction (projector and clockwork run).
     * This is mostly for debug purposes. Projector and clockwork do not care about this (except for setting it).
     * The synchronization decision is used instead.
     */
    private SynchronizationSituationType synchronizationSituationResolved = null;
    
    /**
     * Delta set triple for accounts. Specifies which accounts should be added, removed or stay as they are.
     * It tells almost nothing about attributes directly although the information about attributes are inside
     * each account construction (in a form of ValueConstruction that contains attribute delta triples).
     * 
     * Intermediary computation result. It is stored to allow re-computing of account constructions during
     * iterative computations.
	 *
	 * Source: AssignmentProcessor
	 * Target: ConsolidationProcessor / ReconciliationProcessor (via squeezed structures)
     */
    private transient PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> constructionDeltaSetTriple;

	/**
	 * Triples for outbound mappings; similar to the above.
	 * Source: OutboundProcessor
	 * Target: ConsolidationProcessor / ReconciliationProcessor (via squeezed structures)
	 */
	private transient Construction outboundConstruction;
    
	/**
	 * Postprocessed triples from the above two properties.
	 * Source: ConsolidationProcessor
	 * Target: ReconciliationProcessor
	 */
	private transient Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>>>> squeezedAttributes;
    private transient Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>>> squeezedAssociations;
    private transient Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<QName>,PrismPropertyDefinition<QName>>>> squeezedAuxiliaryObjectClasses;

	private transient Collection<ResourceObjectTypeDependencyType> dependencies = null;

	// Cached copy, to avoid constructing it over and over again
    private transient PrismObjectDefinition<ShadowType> shadowDefinition = null;
    
    private transient RefinedObjectClassDefinition structuralObjectClassDefinition;
    private transient Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions;
    private transient CompositeRefinedObjectClassDefinition compositeObjectClassDefinition;
    
    private ValuePolicyType accountPasswordPolicy;

    
	/**
     * Resource that hosts this projection.
     */
    transient private ResourceType resource;

	/**
	 * EXPERIMENTAL. A flag that this projection context has to be put into 'history archive'.
	 * Necessary to evaluate old state of hasLinkedAccount.
	 *
	 * TODO implement as non-transient.
	 */
	transient private boolean toBeArchived;
	
	transient private String humanReadableName;

	LensProjectionContext(LensContext<? extends ObjectType> lensContext, ResourceShadowDiscriminator resourceAccountType) {
    	super(ShadowType.class, lensContext);
        this.resourceShadowDiscriminator = resourceAccountType;
        this.isAssigned = false;
        this.isAssignedOld = false;
    }

	public ObjectDelta<ShadowType> getSyncDelta() {
		return syncDelta;
	}

	public void setSyncDelta(ObjectDelta<ShadowType> syncDelta) {
		this.syncDelta = syncDelta;
	}

	@Override
	public ObjectDelta<ShadowType> getSecondaryDelta() {
		return secondaryDelta;
	}

	@Override
	public ObjectDeltaObject<ShadowType> getObjectDeltaObject() throws SchemaException {
		return new ObjectDeltaObject<>(getObjectCurrent(), getDelta(), getObjectNew());
	}


	@Override
	public void setSecondaryDelta(ObjectDelta<ShadowType> secondaryDelta) {
		this.secondaryDelta = secondaryDelta;
	}

	public void addSecondaryDelta(ObjectDelta<ShadowType> delta) throws SchemaException {
		if (secondaryDelta == null) {
			secondaryDelta = delta;
		} else {
			secondaryDelta.merge(delta);
		}
	}

	@Override
	public void swallowToSecondaryDelta(ItemDelta<?,?> itemDelta) throws SchemaException {
		if (secondaryDelta == null) {
			secondaryDelta = new ObjectDelta<ShadowType>(getObjectTypeClass(), ChangeType.MODIFY, getPrismContext());
			secondaryDelta.setOid(getOid());
		}
		secondaryDelta.swallow(itemDelta);
	}

	@Override
	public void setOid(String oid) {
		super.setOid(oid);
		if (secondaryDelta != null) {
			secondaryDelta.setOid(oid);
		}
	}

	public boolean isSyncAbsoluteTrigger() {
		return syncAbsoluteTrigger;
	}

	public void setSyncAbsoluteTrigger(boolean syncAbsoluteTrigger) {
		this.syncAbsoluteTrigger = syncAbsoluteTrigger;
	}

	public int getWave() {
		return wave;
	}

    public void setWave(int wave) {
		this.wave = wave;
	}

    public boolean isWaveIncomplete() {
		return waveIncomplete;
	}

	public void setWaveIncomplete(boolean waveIncomplete) {
		this.waveIncomplete = waveIncomplete;
	}

	public boolean isDoReconciliation() {
        return doReconciliation;
    }

    public void setDoReconciliation(boolean doReconciliation) {
        this.doReconciliation = doReconciliation;
    }

    @Override
    public ResourceShadowDiscriminator getResourceShadowDiscriminator() {
        return resourceShadowDiscriminator;
    }
    
    public void setResourceShadowDiscriminator(ResourceShadowDiscriminator resourceShadowDiscriminator) {
		this.resourceShadowDiscriminator = resourceShadowDiscriminator;
	}
    
    public boolean compareResourceShadowDiscriminator(ResourceShadowDiscriminator rsd, boolean compareOrder) {
    	Validate.notNull(rsd.getResourceOid());
    	if (resourceShadowDiscriminator == null) {
    		// This may be valid case e.g. in case of broken contexts or if a context is just loading
    		return false;
    	}
    	if (!rsd.getResourceOid().equals(resourceShadowDiscriminator.getResourceOid())) {
    		return false;
    	}
    	if (!rsd.getKind().equals(resourceShadowDiscriminator.getKind())) {
    		return false;
    	}
    	if (rsd.isThombstone() != resourceShadowDiscriminator.isThombstone()) {
    		return false;
    	}
    	if (rsd.getIntent() == null) {
			try {
				if (!getStructuralObjectClassDefinition().isDefaultInAKind()) {
					return false;
				}
			} catch (SchemaException e) {
				throw new SystemException("Internal error: "+e.getMessage(), e);
			}
		} else if (!rsd.getIntent().equals(resourceShadowDiscriminator.getIntent())) {
			return false;
		}

    	if (compareOrder && rsd.getOrder() != resourceShadowDiscriminator.getOrder()) {
    		return false;
    	}

		return true;
	}
    
	public boolean isThombstone() {
		if (resourceShadowDiscriminator == null) {
			return false;
		}
		return resourceShadowDiscriminator.isThombstone();
	}

	public void addAccountSyncDelta(ObjectDelta<ShadowType> delta) throws SchemaException {
        if (syncDelta == null) {
        	syncDelta = delta;
        } else {
        	syncDelta.merge(delta);
        }
    }
    
    public boolean isAdd() {
		if (synchronizationPolicyDecision == SynchronizationPolicyDecision.ADD) {
			return true;
		} else if (synchronizationPolicyDecision != null){
			return false;
		}
		return super.isAdd();
	}
    
    public boolean isModify() {
		if (synchronizationPolicyDecision == SynchronizationPolicyDecision.KEEP) {
			return true;
		} else if (synchronizationPolicyDecision != null){
			return false;
		}
		return super.isModify();
	}

	public boolean isDelete() {
		if (synchronizationPolicyDecision == SynchronizationPolicyDecision.DELETE) {
			return true;
		} else if (synchronizationPolicyDecision != null){
			return false;
		}
		if (syncDelta != null && syncDelta.isDelete()) {
			return true;
		}
		return super.isDelete();
	}
	
	public ResourceType getResource() {
        return resource;
    }

    public void setResource(ResourceType resource) {
        this.resource = resource;
    }
    
    @Override
	public PrismObjectDefinition<ShadowType> getObjectDefinition() {
		if (shadowDefinition == null) {
			try {
				shadowDefinition = ShadowUtil.applyObjectClass(super.getObjectDefinition(), getCompositeObjectClassDefinition());
			} catch (SchemaException e) {
				// This should not happen
				throw new SystemException(e.getMessage(), e);
			}
		}
		return shadowDefinition;
	}

	public boolean isAssigned() {
        return isAssigned;
    }

    public void setAssigned(boolean isAssigned) {
        this.isAssigned = isAssigned;
    }

    public boolean isAssignedOld() {
        return isAssignedOld;
    }

    public void setAssignedOld(boolean isAssignedOld) {
        this.isAssignedOld = isAssignedOld;
    }

    public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
	
	public Boolean isLegal() {
		return isLegal;
	}

	public void setLegal(Boolean isLegal) {
		this.isLegal = isLegal;
	}

	public Boolean isLegalOld() {
		return isLegalOld;
	}

	public void setLegalOld(Boolean isLegalOld) {
		this.isLegalOld = isLegalOld;
	}

	public boolean isExists() {
		return isExists;
	}

	public void setExists(boolean exists) {
		this.isExists = exists;
	}

	public SynchronizationPolicyDecision getSynchronizationPolicyDecision() {
        return synchronizationPolicyDecision;
    }

    public void setSynchronizationPolicyDecision(SynchronizationPolicyDecision policyDecision) {
        this.synchronizationPolicyDecision = policyDecision;
    }

	public SynchronizationSituationType getSynchronizationSituationDetected() {
		return synchronizationSituationDetected;
	}

	public void setSynchronizationSituationDetected(
			SynchronizationSituationType synchronizationSituationDetected) {
		this.synchronizationSituationDetected = synchronizationSituationDetected;
	}

	public SynchronizationSituationType getSynchronizationSituationResolved() {
		return synchronizationSituationResolved;
	}

	public void setSynchronizationSituationResolved(
			SynchronizationSituationType synchronizationSituationResolved) {
		this.synchronizationSituationResolved = synchronizationSituationResolved;
	}

	public boolean isFullShadow() {
		return fullShadow;
	}
	
	/**
	 * Returns true if full shadow is available, either loaded or in a create delta.
	 */
	public boolean hasFullShadow() {
		if (synchronizationPolicyDecision == SynchronizationPolicyDecision.ADD) {
			return true;
		}
		return isFullShadow();
	}

	public void setFullShadow(boolean fullShadow) {
		this.fullShadow = fullShadow;
	}
	
	public ShadowKindType getKind() {
		ResourceShadowDiscriminator discr = getResourceShadowDiscriminator();
		if (discr != null) {
			return discr.getKind();
		}
		if (getObjectOld()!=null) {
			return getObjectOld().asObjectable().getKind();
		}
		if (getObjectCurrent()!=null) {
			return getObjectCurrent().asObjectable().getKind();
		}
		if (getObjectNew()!=null) {
			return getObjectNew().asObjectable().getKind();
		}
		return ShadowKindType.ACCOUNT;
	}
	
	public PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> getConstructionDeltaSetTriple() {
		return constructionDeltaSetTriple;
	}

	public void setConstructionDeltaSetTriple(
			PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> constructionDeltaSetTriple) {
		this.constructionDeltaSetTriple = constructionDeltaSetTriple;
	}
	
	public Construction getOutboundConstruction() {
		return outboundConstruction;
	}

	public void setOutboundConstruction(Construction outboundConstruction) {
		this.outboundConstruction = outboundConstruction;
	}

    public Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>>>> getSqueezedAttributes() {
		return squeezedAttributes;
	}

	public void setSqueezedAttributes(Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>>>> squeezedAttributes) {
		this.squeezedAttributes = squeezedAttributes;
	}
	
	public Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>>> getSqueezedAssociations() {
		return squeezedAssociations;
	}

	public void setSqueezedAssociations(
			Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>>> squeezedAssociations) {
		this.squeezedAssociations = squeezedAssociations;
	}

	public Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>>>> getSqueezedAuxiliaryObjectClasses() {
		return squeezedAuxiliaryObjectClasses;
	}

	public void setSqueezedAuxiliaryObjectClasses(
			Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<QName>, PrismPropertyDefinition<QName>>>> squeezedAuxiliaryObjectClasses) {
		this.squeezedAuxiliaryObjectClasses = squeezedAuxiliaryObjectClasses;
	}

	public ResourceObjectTypeDefinitionType getResourceObjectTypeDefinitionType() {
		if (synchronizationPolicyDecision == SynchronizationPolicyDecision.BROKEN) {
			return null;
		}
		ResourceShadowDiscriminator discr = getResourceShadowDiscriminator();
		if (discr == null) {
			return null;			// maybe when an account is deleted
		}
		if (resource == null) {
			return null;
		}
		ResourceObjectTypeDefinitionType def = ResourceTypeUtil.getResourceObjectTypeDefinitionType(resource, discr.getKind(), discr.getIntent());
        return def;
    }
	
	private ResourceSchema getResourceSchema() throws SchemaException {
		return RefinedResourceSchemaImpl.getResourceSchema(resource, getNotNullPrismContext());
	}
	
    public RefinedResourceSchema getRefinedResourceSchema() throws SchemaException {
    	if (resource == null) {
    		return null;
    	}
    	return RefinedResourceSchemaImpl.getRefinedSchema(resource, LayerType.MODEL, getNotNullPrismContext());
    }
    
    public RefinedObjectClassDefinition getStructuralObjectClassDefinition() throws SchemaException {
    	if (structuralObjectClassDefinition == null) {
			RefinedResourceSchema refinedSchema = getRefinedResourceSchema();
			if (refinedSchema == null) {
				return null;
			}
			structuralObjectClassDefinition = refinedSchema.getRefinedDefinition(getResourceShadowDiscriminator().getKind(), getResourceShadowDiscriminator().getIntent());
    	}
    	return structuralObjectClassDefinition;
	}

    public Collection<RefinedObjectClassDefinition> getAuxiliaryObjectClassDefinitions() throws SchemaException {
    	if (auxiliaryObjectClassDefinitions == null) {
    		refreshAuxiliaryObjectClassDefinitions();
    	}
    	return auxiliaryObjectClassDefinitions;
	}
    
    public void refreshAuxiliaryObjectClassDefinitions() throws SchemaException {
    	RefinedResourceSchema refinedSchema = getRefinedResourceSchema();
		if (refinedSchema == null) {
			return;
		}
		List<QName> auxiliaryObjectClassQNames = new ArrayList<>();
		addAuxiliaryObjectClassNames(auxiliaryObjectClassQNames, getObjectOld());
		addAuxiliaryObjectClassNames(auxiliaryObjectClassQNames, getObjectNew());
		auxiliaryObjectClassDefinitions = new ArrayList<>(auxiliaryObjectClassQNames.size());
		for (QName auxiliaryObjectClassQName: auxiliaryObjectClassQNames) {
			RefinedObjectClassDefinition auxiliaryObjectClassDef = refinedSchema.getRefinedDefinition(auxiliaryObjectClassQName);
			if (auxiliaryObjectClassDef == null) {
				throw new SchemaException("Auxiliary object class "+auxiliaryObjectClassQName+" specified in "+this+" does not exist");
			}
			auxiliaryObjectClassDefinitions.add(auxiliaryObjectClassDef);
		}
		compositeObjectClassDefinition = null;
    }
    
    public CompositeRefinedObjectClassDefinition getCompositeObjectClassDefinition() throws SchemaException {
    	if (compositeObjectClassDefinition == null) {
    		RefinedObjectClassDefinition structuralObjectClassDefinition = getStructuralObjectClassDefinition();
    		if (structuralObjectClassDefinition != null) {
    			compositeObjectClassDefinition = new CompositeRefinedObjectClassDefinitionImpl(
    					structuralObjectClassDefinition, getAuxiliaryObjectClassDefinitions());
    		}
    	}
    	return compositeObjectClassDefinition;
    }

	private void addAuxiliaryObjectClassNames(List<QName> auxiliaryObjectClassQNames,
			PrismObject<ShadowType> shadow) {
		if (shadow == null) {
			return;
		}
		for (QName aux: shadow.asObjectable().getAuxiliaryObjectClass()) {
			if (!auxiliaryObjectClassQNames.contains(aux)) {
				auxiliaryObjectClassQNames.add(aux);
			}
		}
	}
	
	public <T> RefinedAttributeDefinition<T> findAttributeDefinition(QName attrName) throws SchemaException {
		RefinedAttributeDefinition<T> attrDef = getStructuralObjectClassDefinition().findAttributeDefinition(attrName);
		if (attrDef != null) {
			return attrDef;
		}
		for (RefinedObjectClassDefinition auxOcDef: getAuxiliaryObjectClassDefinitions()) {
			attrDef = auxOcDef.findAttributeDefinition(attrName);
			if (attrDef != null) {
				return attrDef;
			}
		}
		return null;
	}

	public Collection<ResourceObjectTypeDependencyType> getDependencies() {
		if (dependencies == null) {
			ResourceObjectTypeDefinitionType resourceAccountTypeDefinitionType = getResourceObjectTypeDefinitionType();
			if (resourceAccountTypeDefinitionType == null) {
				// No dependencies. But we cannot set null as that means "unknown". So let's set empty collection instead.
				dependencies = new ArrayList<ResourceObjectTypeDependencyType>();
			} else {
				dependencies = resourceAccountTypeDefinitionType.getDependency();
			}
		}
		return dependencies;
	}
	
	public ValuePolicyType getAccountPasswordPolicy() {
		return accountPasswordPolicy;
	}
	
	public void setCanProject(boolean canProject) {
		this.canProject = canProject;
	}
	
	public boolean isCanProject() {
		return canProject;
	}
	
	public void setAccountPasswordPolicy(ValuePolicyType accountPasswordPolicy) {
		this.accountPasswordPolicy = accountPasswordPolicy;
	}
	
	public ValuePolicyType getEffectivePasswordPolicy() {
		if (accountPasswordPolicy != null) {
			return accountPasswordPolicy;
		}
		
		if (getLensContext().getFocusContext().getOrgPasswordPolicy() != null){
			return getLensContext().getFocusContext().getOrgPasswordPolicy();
		}
		
		return getLensContext().getGlobalPasswordPolicy();
	}
	
	public AssignmentPolicyEnforcementType getAssignmentPolicyEnforcementType() {
		// TODO: per-resource assignment enforcement
		ResourceType resource = getResource();
		ProjectionPolicyType globalAccountSynchronizationSettings = null;
		if (resource != null){
			globalAccountSynchronizationSettings = resource.getProjection();
		} 
		
		if (globalAccountSynchronizationSettings == null) {
			globalAccountSynchronizationSettings = getLensContext().getAccountSynchronizationSettings();
		}
		AssignmentPolicyEnforcementType globalAssignmentPolicyEnforcement = MiscSchemaUtil.getAssignmentPolicyEnforcementType(globalAccountSynchronizationSettings);
		return globalAssignmentPolicyEnforcement;
	}
	
	public boolean isLegalize(){
		ResourceType resource = getResource();
		ProjectionPolicyType globalAccountSynchronizationSettings = null;
		if (resource != null){
			globalAccountSynchronizationSettings = resource.getProjection();
		} 
		
		if (globalAccountSynchronizationSettings == null) {
			globalAccountSynchronizationSettings = getLensContext().getAccountSynchronizationSettings();
		}
		
		if (globalAccountSynchronizationSettings == null){
			return false;
		}
		
		if (globalAccountSynchronizationSettings.isLegalize() == null){
			return false;
		}
		
		return globalAccountSynchronizationSettings.isLegalize();
	}

	/**
     * Recomputes the new state of account (accountNew). It is computed by applying deltas to the old state (accountOld).
     * Assuming that oldAccount is already set (or is null if it does not exist)
     */
    public void recompute() throws SchemaException {
        ObjectDelta<ShadowType> accDelta = getDelta();

        PrismObject<ShadowType> base = getObjectCurrent();
        if (base == null) {
        	base = getObjectOld();
        }
        ObjectDelta<ShadowType> syncDelta = getSyncDelta();
        if (base == null && syncDelta != null
                && ChangeType.ADD.equals(syncDelta.getChangeType())) {
            PrismObject<ShadowType> objectToAdd = syncDelta.getObjectToAdd();
            if (objectToAdd != null) {
                PrismObjectDefinition<ShadowType> objectDefinition = objectToAdd.getDefinition();
                // TODO: remove constructor, use some factory method instead
                base = new PrismObject<ShadowType>(objectToAdd.getElementName(), objectDefinition, getNotNullPrismContext());
                base = syncDelta.computeChangedObject(base);
            }
        }

        if (accDelta == null) {
            // No change
            setObjectNew(base);
            return;
        }
        
        if (base == null && accDelta.isModify()) {
        	RefinedObjectClassDefinition rOCD = getCompositeObjectClassDefinition();
        	if (rOCD != null) {
        		base = rOCD.createBlankShadow();
        	}
        }

        setObjectNew(accDelta.computeChangedObject(base));
    }
    
	public void clearIntermediateResults() {
		//constructionDeltaSetTriple = null;
		outboundConstruction = null;
		squeezedAttributes = null;
	}
	
	/**
	 * Distribute the resource that's in the context into all the prism objects (old, new) and deltas.
	 * The resourceRef will not just contain the OID but also full resource object. This may optimize handling
	 * of the objects in upper layers (e.g. GUI).
	 */
	public void distributeResource() {
		ResourceType resourceType = getResource();
		if (resourceType == null) {
			return;
		}
		PrismObject<ResourceType> resource = resourceType.asPrismObject();
		distributeResourceObject(getObjectOld(), resource);
		distributeResourceObject(getObjectCurrent(), resource);
		distributeResourceObject(getObjectNew(), resource);
		distributeResourceDelta(getPrimaryDelta(), resource);
		distributeResourceDelta(getSecondaryDelta(), resource);
	}
	
	private void distributeResourceObject(PrismObject<ShadowType> object, PrismObject<ResourceType> resource) {
		if (object == null) {
			return;
		}
		PrismReference resourceRef = object.findReference(ShadowType.F_RESOURCE_REF);
		if (resourceRef != null) {
			distributeResourceValues(resourceRef.getValues(), resource);
		}
	}

	private void distributeResourceValue(PrismReferenceValue resourceRefVal, PrismObject<ResourceType> resource) {
		if (resourceRefVal != null) {
			resourceRefVal.setObject(resource);
		}
	}

	private void distributeResourceDelta(ObjectDelta<ShadowType> delta, PrismObject<ResourceType> resource) {
		if (delta == null) {
			return;
		}
		if (delta.isAdd()) {
			distributeResourceObject(delta.getObjectToAdd(), resource);
		} else if (delta.isModify()) {
			ReferenceDelta referenceDelta = delta.findReferenceModification(ShadowType.F_RESOURCE_REF);
			if (referenceDelta != null) {
				distributeResourceValues(referenceDelta.getValuesToAdd(), resource);
				distributeResourceValues(referenceDelta.getValuesToDelete(), resource);
				distributeResourceValues(referenceDelta.getValuesToReplace(), resource);
			}
		} // Nothing to do for DELETE delta
	}

	private void distributeResourceValues(Collection<PrismReferenceValue> values, PrismObject<ResourceType> resource) {
		if (values == null) {
			return;
		}
		for(PrismReferenceValue pval: values) {
			distributeResourceValue(pval, resource);
		}
	}
	
	/**
	 * Returns delta suitable for execution. The primary and secondary deltas may not make complete sense all by themselves.
	 * E.g. they may both be MODIFY deltas even in case that the account should be created. The deltas begin to make sense
	 * only if combined with sync decision. This method provides the deltas all combined and ready for execution.
	 */
	@Override
	public ObjectDelta<ShadowType> getExecutableDelta() throws SchemaException {
		SynchronizationPolicyDecision policyDecision = getSynchronizationPolicyDecision();
		ObjectDelta<ShadowType> origDelta = getFixedDelta();
		if (policyDecision == SynchronizationPolicyDecision.ADD) {
			// let's try to retrieve original (non-fixed) delta. Maybe it's ADD delta so we spare fixing it.
			origDelta = getDelta();
            if (origDelta == null || origDelta.isModify()) {
            	// We need to convert modify delta to ADD
            	ObjectDelta<ShadowType> addDelta = new ObjectDelta<ShadowType>(getObjectTypeClass(),
                		ChangeType.ADD, getPrismContext());
                RefinedObjectClassDefinition rObjectClassDef = getCompositeObjectClassDefinition();

                if (rObjectClassDef == null) {
                    throw new IllegalStateException("Definition for account type " + getResourceShadowDiscriminator() 
                    		+ " not found in the context, but it should be there");
                }
                PrismObject<ShadowType> newAccount = (PrismObject<ShadowType>) rObjectClassDef.createBlankShadow();
                addDelta.setObjectToAdd(newAccount);
                
                if (origDelta != null) {
                	addDelta.merge(origDelta);
                }
                return addDelta;
            }
        } else if (policyDecision == SynchronizationPolicyDecision.KEEP) {
            // Any delta is OK
        } else if (policyDecision == SynchronizationPolicyDecision.DELETE) {
        	ObjectDelta<ShadowType> deleteDelta = new ObjectDelta<ShadowType>(getObjectTypeClass(),
            		ChangeType.DELETE, getPrismContext());
            String oid = getOid();
            if (oid == null) {
            	throw new IllegalStateException(
            			"Internal error: account context OID is null during attempt to create delete secondary delta; context="
            					+this);
            }
            deleteDelta.setOid(oid);
            return deleteDelta;
        } else {
            // This is either UNLINK or null, both are in fact the same as KEEP
        	// Any delta is OK
        }
		return origDelta;
	}

	public void checkConsistence() {
		checkConsistence(null, true, false);
	}

	@Override
	public void checkConsistence(String contextDesc) {
		super.checkConsistence(contextDesc);
		if (secondaryDelta != null) {
			boolean requireOid = isRequireSecondardyDeltaOid();
			// Secondary delta may not have OID yet (as it may relate to ADD primary delta that doesn't have OID yet)
			checkConsistence(secondaryDelta, requireOid, getElementDesc() + " secondary delta in " + this + (contextDesc == null ? "" : " in " + contextDesc));
		}
	}

	public void checkConsistence(String contextDesc, boolean fresh, boolean force) {
		if (synchronizationPolicyDecision == SynchronizationPolicyDecision.IGNORE) {
			// No not check these. they may be quite wild.
			return;
		}
		super.checkConsistence(contextDesc);
		if (synchronizationPolicyDecision == SynchronizationPolicyDecision.BROKEN) {
			return;
		}
    	if (fresh && !force && resourceShadowDiscriminator != null && !resourceShadowDiscriminator.isThombstone()) {
    		if (resource == null) {
	    		throw new IllegalStateException("Null resource in "+this + (contextDesc == null ? "" : " in " +contextDesc));
	    	}
	    	if (resourceShadowDiscriminator == null) {
	    		throw new IllegalStateException("Null resource account type in "+this + (contextDesc == null ? "" : " in " +contextDesc));
	    	}
    	}
    	if (syncDelta != null) {
    		try {
    			syncDelta.checkConsistence(true, true, true, ConsistencyCheckScope.THOROUGH);
    		} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e.getMessage()+"; in "+getElementDesc()+" sync delta in "+this + (contextDesc == null ? "" : " in " +contextDesc), e);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage()+"; in "+getElementDesc()+" sync delta in "+this + (contextDesc == null ? "" : " in " +contextDesc), e);
			}
    	}
    }
	
	@Override
	protected void checkConsistence(PrismObject<ShadowType> object, String elementDesc, String contextDesc) {
		super.checkConsistence(object, elementDesc, contextDesc);
		ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(object);
    	if (attributesContainer != null) {
    		ResourceType resource = getResource();
    		if (resource != null) {
	    		String resourceNamespace = ResourceTypeUtil.getResourceNamespace(resource);
	    		for(ResourceAttribute<?> attribute: attributesContainer.getAttributes()) {
	    			QName attrName = attribute.getElementName();
	    			if (SchemaConstants.NS_ICF_SCHEMA.equals(attrName.getNamespaceURI())) {
	    				continue;
	    			}
	    			if (resourceNamespace.equals(attrName.getNamespaceURI())) {
	    				continue;
	    			}
	    			String desc = elementDesc+" in "+this + (contextDesc == null ? "" : " in " +contextDesc);
	    			throw new IllegalStateException("Invalid namespace for attribute "+attrName+" in "+desc);
	    		}
    		}
    	}
	}
	
	protected boolean isRequireSecondardyDeltaOid() {
		if (synchronizationPolicyDecision == SynchronizationPolicyDecision.ADD ||
				synchronizationPolicyDecision == SynchronizationPolicyDecision.BROKEN ||
				synchronizationPolicyDecision == SynchronizationPolicyDecision.IGNORE) {
			return false;
		}
		if (getResourceShadowDiscriminator() != null && getResourceShadowDiscriminator().getOrder() > 0) {
			// These may not have the OID yet
			return false;
		}
		return super.isRequireSecondardyDeltaOid();
	}
    
	@Override
	public void cleanup() {
		secondaryDelta = null;
		resetSynchronizationPolicyDecision();
//		isLegal = null;
//		isLegalOld = null;
		isAssigned = false;
        isAssignedOld = false;  // ??? [med]
		isActive = false;
	}
	
	@Override
	public void normalize() {
		super.normalize();
		if (secondaryDelta != null) {
			secondaryDelta.normalize();
		}
		if (syncDelta != null) {
			syncDelta.normalize();
		}
	}

//	@Override
//	public void reset() {
//		super.reset();
//		wave = -1;
//		fullShadow = false;
//		isAssigned = false;
//        isAssignedOld = false;
//		isActive = false;
//		resetSynchronizationPolicyDecision();
//		constructionDeltaSetTriple = null;
//		outboundConstruction = null;
//		dependencies = null;
//		squeezedAttributes = null;
//		accountPasswordPolicy = null;
//	}

	protected void resetSynchronizationPolicyDecision() {
		if (synchronizationPolicyDecision == SynchronizationPolicyDecision.DELETE || synchronizationPolicyDecision == SynchronizationPolicyDecision.UNLINK) {
			toBeArchived = true;
		} else if (synchronizationPolicyDecision != null) {
			toBeArchived = false;
		}
		synchronizationPolicyDecision = null;
	}

	@Override
	public void adopt(PrismContext prismContext) throws SchemaException {
		super.adopt(prismContext);
		if (syncDelta != null) {
			prismContext.adopt(syncDelta);
		}
		if (secondaryDelta != null) {
			prismContext.adopt(secondaryDelta);
		}
	}

	@Override
	public LensProjectionContext clone(LensContext<? extends ObjectType> lensContext) {
		LensProjectionContext clone = new LensProjectionContext(lensContext, resourceShadowDiscriminator);
		copyValues(clone, lensContext);
		return clone;
	}
	
	protected void copyValues(LensProjectionContext clone, LensContext<? extends ObjectType> lensContext) {
		super.copyValues(clone, lensContext);
		// do NOT clone transient values such as accountConstructionDeltaSetTriple
		// these are not meant to be cloned and they are also not directly clonnable
		clone.dependencies = this.dependencies;
		clone.doReconciliation = this.doReconciliation;
		clone.fullShadow = this.fullShadow;
		clone.isAssigned = this.isAssigned;
        clone.isAssignedOld = this.isAssignedOld;
		clone.outboundConstruction = this.outboundConstruction;
		clone.synchronizationPolicyDecision = this.synchronizationPolicyDecision;
		clone.resource = this.resource;
		clone.resourceShadowDiscriminator = this.resourceShadowDiscriminator;
		clone.squeezedAttributes = cloneSqueezedAttributes();
		if (this.syncDelta != null) {
			clone.syncDelta = this.syncDelta.clone();
		}
		clone.secondaryDelta = cloneDelta(this.secondaryDelta);
		clone.wave = this.wave;
	}

	private Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>>>> cloneSqueezedAttributes() {
		if (squeezedAttributes == null) {
			return null;
		}
		Map<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>>>> clonedMap 
		= new HashMap<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>>>>();
		Cloner<ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>>> cloner = new Cloner<ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>>>() {
			@Override
			public ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>> clone(ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>> original) {
				return original.clone();
			}
		};
		for (Entry<QName, DeltaSetTriple<ItemValueWithOrigin<PrismPropertyValue<?>,PrismPropertyDefinition<?>>>> entry: squeezedAttributes.entrySet()) {
			clonedMap.put(entry.getKey(), entry.getValue().clone(cloner));
		}
		return clonedMap;
	}
	
	/**
	 * Returns true if the projection has any value for specified attribute.
	 */
	public boolean hasValueForAttribute(QName attributeName) throws SchemaException {
		ItemPath attrPath = new ItemPath(ShadowType.F_ATTRIBUTES, attributeName);
		if (getObjectNew() != null) {
			PrismProperty<?> attrNew = getObjectNew().findProperty(attrPath);
			if (attrNew != null && !attrNew.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private boolean hasValueForAttribute(QName attributeName, Collection<PrismPropertyValue<Construction>> acPpvSet) {
		if (acPpvSet == null) {
			return false;
		}
		for (PrismPropertyValue<Construction> acPpv: acPpvSet) {
			Construction ac = acPpv.getValue();
			if (ac.hasValueForAttribute(attributeName)) {
				return true;
			}
		}
		return false;
	}

    @Override
	public void checkEncrypted() {
		super.checkEncrypted();
		if (syncDelta != null) {
			CryptoUtil.checkEncrypted(syncDelta);
		}
		if (secondaryDelta != null) {
			CryptoUtil.checkEncrypted(secondaryDelta);
		}
	}

	public String getHumanReadableName() {
		if (humanReadableName == null) {
			StringBuilder sb = new StringBuilder();
			sb.append("account(");
			String humanReadableAccountIdentifier = getHumanReadableIdentifier();
			if (StringUtils.isEmpty(humanReadableAccountIdentifier)) {
				sb.append("no ID");
			} else {
				sb.append("ID ");
				sb.append(humanReadableAccountIdentifier);
			}
			ResourceShadowDiscriminator discr = getResourceShadowDiscriminator();
			if (discr != null) {
				sb.append(", type '");
				sb.append(discr.getIntent());
				sb.append("', ");
				if (discr.getOrder() != 0) {
					sb.append("order ").append(discr.getOrder()).append(", ");
				}
			} else {
				sb.append(" (no discriminator) ");
			}
			sb.append(getResource());
			sb.append(")");
			humanReadableName = sb.toString();
		}
		return humanReadableName;
	}

	private String getHumanReadableIdentifier() {
		PrismObject<ShadowType> object = getObjectNew();
		if (object == null) {
			object = getObjectOld();
		}
		if (object == null) {
			object = getObjectCurrent();
		}
		if (object == null) {
			return null;
		}
		if (object.canRepresent(ShadowType.class)) {
			PrismObject<ShadowType> shadow = (PrismObject<ShadowType>)object;
			Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
			if (identifiers == null) {
				return null;
			}
			StringBuilder sb = new StringBuilder();
			Iterator<ResourceAttribute<?>> iterator = identifiers.iterator();
			while (iterator.hasNext()) {
				ResourceAttribute<?> id = iterator.next();
				sb.append(id.toHumanReadableString());
				if (iterator.hasNext()) {
					sb.append(",");
				}
			}
			return sb.toString();
		} else {
			return object.toString();
		}
	}

	@Override
    public String debugDump() {
        return debugDump(0);
    }
    
    @Override
    public String debugDump(int indent) {
    	return debugDump(indent, true);
    }
    
    public String debugDump(int indent, boolean showTriples) {
        StringBuilder sb = new StringBuilder();
        SchemaDebugUtil.indentDebugDump(sb, indent);
        sb.append("PROJECTION ");
        sb.append(getObjectTypeClass() == null ? "null" : getObjectTypeClass().getSimpleName());
        sb.append(" ");
        sb.append(getResourceShadowDiscriminator());
        if (resource != null) {
        	sb.append(" : ");
        	sb.append(resource.getName().getOrig());
        }
        sb.append("\n");
        SchemaDebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("OID: ").append(getOid());
        sb.append(", wave ").append(wave);
        if (fullShadow) {
        	sb.append(", full");
        } else {
        	sb.append(", shadow");
        }
        sb.append(", exists=").append(isExists);
        sb.append(", assigned=").append(isAssignedOld).append("->").append(isAssigned);
        sb.append(", active=").append(isActive);
        sb.append(", legal=").append(isLegalOld).append("->").append(isLegal);
        sb.append(", recon=").append(doReconciliation);
        sb.append(", syncIntent=").append(getSynchronizationIntent());
        sb.append(", decision=").append(synchronizationPolicyDecision);
        if (!isFresh()) {
        	sb.append(", NOT FRESH");
        }
        if (resourceShadowDiscriminator != null && resourceShadowDiscriminator.isThombstone()) {
        	sb.append(", THOMBSTONE");
        }
        if (syncAbsoluteTrigger) {
        	sb.append(", SYNC TRIGGER");
        }
        if (getIteration() != 0) {
        	sb.append(", iteration=").append(getIteration()).append(" (").append(getIterationToken()).append(")");
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("old"), getObjectOld(), indent + 1);
        
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("current"), getObjectCurrent(), indent + 1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("new"), getObjectNew(), indent + 1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("primary delta"), getPrimaryDelta(), indent + 1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("secondary delta"), getSecondaryDelta(), indent + 1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("sync delta"), getSyncDelta(), indent + 1);
        
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("executed deltas"), getExecutedDeltas(), indent+1);

        if (showTriples) {
        	
        	sb.append("\n");
        	DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("constructionDeltaSetTriple"), constructionDeltaSetTriple, indent + 1);
        	
	        sb.append("\n");
	        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("outbound account construction"), outboundConstruction, indent + 1);
	        
	        sb.append("\n");
	        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("squeezed attributes"), squeezedAttributes, indent + 1);
	        
	        sb.append("\n");
	        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("squeezed associations"), squeezedAssociations, indent + 1);
	        
	        sb.append("\n");
	        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("squeezed auxiliary object classes"), squeezedAuxiliaryObjectClasses, indent + 1);

	        // This is just a debug thing
//	        sb.append("\n");
//	        DebugUtil.indentDebugDump(sb, indent);
//	        sb.append("ACCOUNT dependencies\n");
//	        sb.append(DebugUtil.debugDump(dependencies, indent + 1));
        }

        return sb.toString();
    }

    @Override
	protected String getElementDefaultDesc() {
		return "projection";
	}
    
	@Override
	public String toString() {
		return "LensProjectionContext(" + (getObjectTypeClass() == null ? "null" : getObjectTypeClass().getSimpleName()) + ":" + getOid() +
				( resource == null ? "" : " on " + resource ) + ")";
	}

	/**
	 * Return a human readable name of the projection object suitable for logs.
	 */
	public String toHumanReadableString() {
		if (resourceShadowDiscriminator == null) {
			return "(null" + resource + ")";
		}
		if (resource != null) {
			return "("+getKindValue(resourceShadowDiscriminator.getKind()) + " ("+resourceShadowDiscriminator.getIntent()+") on " + resource + ")";
		} else {
			return "("+getKindValue(resourceShadowDiscriminator.getKind()) + " ("+resourceShadowDiscriminator.getIntent()+") on " + resourceShadowDiscriminator.getResourceOid() + ")";
		}
	}

    public String getHumanReadableKind() {
        if (resourceShadowDiscriminator == null) {
            return "resource object";
        }
        return getKindValue(resourceShadowDiscriminator.getKind());
    }

	private String getKindValue(ShadowKindType kind) {
		if (kind == null) {
			return "null";
		}
		return kind.value();
	}

	@Override
	protected String getElementDesc() {
		if (resourceShadowDiscriminator == null) {
            return "shadow";
        }
        return getKindValue(resourceShadowDiscriminator.getKind());
	}

	public void addToPrismContainer(PrismContainer<LensProjectionContextType> lensProjectionContextTypeContainer) throws SchemaException {

        LensProjectionContextType lensProjectionContextType = lensProjectionContextTypeContainer.createNewValue().asContainerable();

        super.storeIntoLensElementContextType(lensProjectionContextType);
        lensProjectionContextType.setSyncDelta(syncDelta != null ? DeltaConvertor.toObjectDeltaType(syncDelta) : null);
		lensProjectionContextType.setSecondaryDelta(secondaryDelta != null ? DeltaConvertor.toObjectDeltaType(secondaryDelta) : null);
        lensProjectionContextType.setWave(wave);
        lensProjectionContextType.setResourceShadowDiscriminator(resourceShadowDiscriminator != null ?
                resourceShadowDiscriminator.toResourceShadowDiscriminatorType() : null);
        lensProjectionContextType.setFullShadow(fullShadow);
        lensProjectionContextType.setIsAssigned(isAssigned);
        lensProjectionContextType.setIsAssignedOld(isAssignedOld);
        lensProjectionContextType.setIsActive(isActive);
        lensProjectionContextType.setIsLegal(isLegal);
        lensProjectionContextType.setIsLegalOld(isLegalOld);
        lensProjectionContextType.setIsExists(isExists);
        lensProjectionContextType.setSynchronizationPolicyDecision(synchronizationPolicyDecision != null ? synchronizationPolicyDecision.toSynchronizationPolicyDecisionType() : null);
        lensProjectionContextType.setDoReconciliation(doReconciliation);
        lensProjectionContextType.setSynchronizationSituationDetected(synchronizationSituationDetected);
        lensProjectionContextType.setSynchronizationSituationResolved(synchronizationSituationResolved);
        lensProjectionContextType.setAccountPasswordPolicy(accountPasswordPolicy);
        lensProjectionContextType.setSyncAbsoluteTrigger(syncAbsoluteTrigger);
    }

    public static LensProjectionContext fromLensProjectionContextType(LensProjectionContextType projectionContextType, LensContext lensContext, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {

        String objectTypeClassString = projectionContextType.getObjectTypeClass();
        if (StringUtils.isEmpty(objectTypeClassString)) {
            throw new SystemException("Object type class is undefined in LensProjectionContextType");
        }
        ResourceShadowDiscriminator resourceShadowDiscriminator = ResourceShadowDiscriminator.fromResourceShadowDiscriminatorType(projectionContextType.getResourceShadowDiscriminator());

        LensProjectionContext projectionContext = new LensProjectionContext(lensContext, resourceShadowDiscriminator);

        projectionContext.retrieveFromLensElementContextType(projectionContextType, result);
        if (projectionContextType.getSyncDelta() != null) {
			projectionContext.syncDelta = DeltaConvertor.createObjectDelta(projectionContextType.getSyncDelta(), lensContext.getPrismContext());
        } else {
			projectionContext.syncDelta = null;
		}
		ObjectDeltaType secondaryDeltaType = projectionContextType.getSecondaryDelta();
		projectionContext.secondaryDelta = secondaryDeltaType != null ? (ObjectDelta) DeltaConvertor.createObjectDelta(secondaryDeltaType, lensContext.getPrismContext()) : null;
		ObjectType object = projectionContextType.getObjectNew() != null ? projectionContextType.getObjectNew() : projectionContextType.getObjectOld();
		projectionContext.fixProvisioningTypeInDelta(projectionContext.secondaryDelta, object, result);

		projectionContext.wave = projectionContextType.getWave() != null ? projectionContextType.getWave() : 0;
        projectionContext.fullShadow = projectionContextType.isFullShadow() != null ? projectionContextType.isFullShadow() : false;
        projectionContext.isAssigned = projectionContextType.isIsAssigned() != null ? projectionContextType.isIsAssigned() : false;
        projectionContext.isAssignedOld = projectionContextType.isIsAssignedOld() != null ? projectionContextType.isIsAssignedOld() : false;
        projectionContext.isActive = projectionContextType.isIsActive() != null ? projectionContextType.isIsActive() : false;
        projectionContext.isLegal = projectionContextType.isIsLegal();
        projectionContext.isLegalOld = projectionContextType.isIsLegalOld();
        projectionContext.isExists = projectionContextType.isIsExists() != null ? projectionContextType.isIsExists() : false;
        projectionContext.synchronizationPolicyDecision = SynchronizationPolicyDecision.fromSynchronizationPolicyDecisionType(projectionContextType.getSynchronizationPolicyDecision());
        projectionContext.doReconciliation = projectionContextType.isDoReconciliation() != null ? projectionContextType.isDoReconciliation() : false;
        projectionContext.synchronizationSituationDetected = projectionContextType.getSynchronizationSituationDetected();
        projectionContext.synchronizationSituationResolved = projectionContextType.getSynchronizationSituationResolved();
        projectionContext.accountPasswordPolicy = projectionContextType.getAccountPasswordPolicy();
        projectionContext.syncAbsoluteTrigger = projectionContextType.isSyncAbsoluteTrigger();

        return projectionContext;
    }

    // determines whether full shadow is present, based on operation result got from provisioning
    public void determineFullShadowFlag(OperationResultType fetchResult) {
        if (fetchResult != null
                && (fetchResult.getStatus() == OperationResultStatusType.PARTIAL_ERROR
                    || fetchResult.getStatus() == OperationResultStatusType.FATAL_ERROR)
                    && (getObjectAny().asObjectable().getFailedOperationType() == null || getObjectAny().asObjectable().getFailedOperationType() != FailedOperationTypeType.ADD)) {                 // todo what about other kinds of status? [e.g. in-progress]
           	setFullShadow(false);
        } else {
            setFullShadow(true);
        }
    }

	public boolean isToBeArchived() {
		return toBeArchived;
	}

	public void setToBeArchived(boolean toBeArchived) {
		this.toBeArchived = toBeArchived;
	}

	public String getResourceOid() {
		if (resource != null) {
			return resource.getOid();
		} else if (resourceShadowDiscriminator != null) {
			return resourceShadowDiscriminator.getResourceOid();
		} else {
			return null;
		}
	}
	
	public ResourceObjectVolatilityType getVolatility() throws SchemaException {
		RefinedObjectClassDefinition structuralObjectClassDefinition = getStructuralObjectClassDefinition();
		if (structuralObjectClassDefinition == null) {
			return null;
		}
		return structuralObjectClassDefinition.getVolatility();
	}
}
