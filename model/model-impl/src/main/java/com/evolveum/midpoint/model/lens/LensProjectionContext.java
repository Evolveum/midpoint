/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.model.model_context_2.LensProjectionContextType;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.Cloner;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProjectionPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectTypeDependencyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

/**
 * @author semancik
 *
 */
public class LensProjectionContext<O extends ObjectType> extends LensElementContext<O> implements ModelProjectionContext<O> {
	
	private ObjectDelta<O> syncDelta;
	
	/**
	 * The wave in which this resource should be processed. Initial value of -1 means "undetermined".
	 */
	private int wave = -1;

    /**
     * Definition of account type.
     */
    private ResourceShadowDiscriminator resourceShadowDiscriminator;
	
	private boolean fullShadow = false;
	    
    /**
     * True if the account is "legal" (assigned to the user). It may be false for accounts that are either
     * found to be illegal by live sync, were unassigned from user, etc.
     * If set to null the situation is not yet known. Null is a typical value when the context is constructed.
     */
    private boolean isAssigned;
    
    /**
     * True if the account should be part of the synchronization. E.g. outbound expression should be applied to it.
     */
    private boolean isActive;
    
    /**
     * True if there is a valid assignment for this projection and/or the policy allows such project to exist.
     */
    private Boolean isLegal = null;
    private Boolean isLegalOld = null;
    
    private boolean isExists;

    /**
     * Initial intent regarding the account. It indicated what the initiator of the operation WANTS TO DO with the
     * context. 
     * If set to null then the decision is left to "the engine". Null is also a typical value
     * when the context is created. It may be pre-set under some circumstances, e.g. if an account is being unlinked.
     */
    private SynchronizationIntent synchronizationIntent;
    
    /**
     * Decision regarding the account. It indicated what the engine has DECIDED TO DO with the context.
     * If set to null no decision was made yet. Null is also a typical value when the context is created.
     */
    private SynchronizationPolicyDecision synchronizationPolicyDecision;

    /**
     * True if we want to reconcile account in this context.
     */
    private boolean doReconciliation;
    
    private int iteration;
    private String iterationToken;
    
    /**
     * Delta set triple for accounts. Specifies which accounts should be added, removed or stay as they are.
     * It tells almost nothing about attributes directly although the information about attributes are inside
     * each account construction (in a form of ValueConstruction that contains attribute delta triples).
     * 
     * Intermediary computation result. It is stored to allow re-computing of account constructions during
     * iterative computations.
     */
    private transient PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountConstructionDeltaSetTriple;
    
    private transient AccountConstruction outboundAccountConstruction;
    
    private transient Collection<ResourceObjectTypeDependencyType> dependencies = null;
    
    private transient Map<QName, DeltaSetTriple<ItemValueWithOrigin<? extends PrismPropertyValue<?>>>> squeezedAttributes;
    
    private ValuePolicyType accountPasswordPolicy;

    
	/**
     * Resource that hosts this projection.
     */
    transient private ResourceType resource;
    
    LensProjectionContext(Class<O> objectTypeClass, LensContext<? extends ObjectType, O> lensContext, ResourceShadowDiscriminator resourceAccountType) {
    	super(objectTypeClass, lensContext);
        this.resourceShadowDiscriminator = resourceAccountType;
        this.isAssigned = false;
    }

	public ObjectDelta<O> getSyncDelta() {
		return syncDelta;
	}

	public void setSyncDelta(ObjectDelta<O> syncDelta) {
		this.syncDelta = syncDelta;
	}

    public int getWave() {
		return wave;
	}

    public void setWave(int wave) {
		this.wave = wave;
	}

    public boolean isDoReconciliation() {
        return doReconciliation;
    }

    public void setDoReconciliation(boolean doReconciliation) {
        this.doReconciliation = doReconciliation;
    }

    public ResourceShadowDiscriminator getResourceShadowDiscriminator() {
        return resourceShadowDiscriminator;
    }
    
    public void setResourceShadowDiscriminator(ResourceShadowDiscriminator resourceShadowDiscriminator) {
		this.resourceShadowDiscriminator = resourceShadowDiscriminator;
	}
    
	public boolean isThombstone() {
		if (resourceShadowDiscriminator == null) {
			return false;
		}
		return resourceShadowDiscriminator.isThombstone();
	}

	public void addAccountSyncDelta(ObjectDelta<O> delta) throws SchemaException {
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
		if (ObjectDelta.isAdd(getPrimaryDelta())) {
			return true;
		}
		if (ObjectDelta.isAdd(getSecondaryDelta())) {
			return true;
		}
		return false;
	}
    
    public boolean isModify() {
		if (synchronizationPolicyDecision == SynchronizationPolicyDecision.KEEP) {
			return true;
		} else if (synchronizationPolicyDecision != null){
			return false;
		}
		if (ObjectDelta.isModify(getPrimaryDelta())) {
			return true;
		}
		if (ObjectDelta.isModify(getSecondaryDelta())) {
			return true;
		}
		return false;
	}

	public boolean isDelete() {
		if (synchronizationPolicyDecision == SynchronizationPolicyDecision.DELETE) {
			return true;
		} else if (synchronizationPolicyDecision != null){
			return false;
		}
		if (ObjectDelta.isDelete(getPrimaryDelta())) {
			return true;
		}
		if (ObjectDelta.isDelete(getSecondaryDelta())) {
			return true;
		}
		return false;
	}
	
	public ResourceType getResource() {
        return resource;
    }

    public void setResource(ResourceType resource) {
        this.resource = resource;
    }
    
    public boolean isAssigned() {
        return isAssigned;
    }

    public void setAssigned(boolean isAssigned) {
        this.isAssigned = isAssigned;
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

	public SynchronizationIntent getSynchronizationIntent() {
		return synchronizationIntent;
	}

	public void setSynchronizationIntent(SynchronizationIntent synchronizationIntent) {
		this.synchronizationIntent = synchronizationIntent;
	}

	public SynchronizationPolicyDecision getSynchronizationPolicyDecision() {
        return synchronizationPolicyDecision;
    }

    public void setSynchronizationPolicyDecision(SynchronizationPolicyDecision policyDecision) {
        this.synchronizationPolicyDecision = policyDecision;
    }
    
    public int getIteration() {
		return iteration;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

	public String getIterationToken() {
		return iterationToken;
	}

	public void setIterationToken(String iterationToken) {
		this.iterationToken = iterationToken;
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
	
	public boolean isShadow() {
		return ShadowType.class.isAssignableFrom(getObjectTypeClass());
	}

	public ShadowKindType getKind() {
		if (!isShadow()) {
			return null;
		}
		if (getObjectOld()!=null) {
			return ((PrismObject<ShadowType>)getObjectOld()).asObjectable().getKind();
		}
		if (getObjectNew()!=null) {
			return ((PrismObject<ShadowType>)getObjectNew()).asObjectable().getKind();
		}
		return null;
	}
	
	public PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> getAccountConstructionDeltaSetTriple() {
		return accountConstructionDeltaSetTriple;
	}

	public void setAccountConstructionDeltaSetTriple(
			PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountConstructionDeltaSetTriple) {
		this.accountConstructionDeltaSetTriple = accountConstructionDeltaSetTriple;
	}
	
	public AccountConstruction getOutboundAccountConstruction() {
		return outboundAccountConstruction;
	}

	public void setOutboundAccountConstruction(AccountConstruction outboundAccountConstruction) {
		this.outboundAccountConstruction = outboundAccountConstruction;
	}

    public Map<QName, DeltaSetTriple<ItemValueWithOrigin<? extends PrismPropertyValue<?>>>> getSqueezedAttributes() {
		return squeezedAttributes;
	}

	public void setSqueezedAttributes(Map<QName, DeltaSetTriple<ItemValueWithOrigin<? extends PrismPropertyValue<?>>>> squeezedAttributes) {
		this.squeezedAttributes = squeezedAttributes;
	}
	
	public ResourceObjectTypeDefinitionType getResourceAccountTypeDefinitionType() {
		if (synchronizationPolicyDecision == SynchronizationPolicyDecision.BROKEN) {
			return null;
		}
		if (isShadow()) {
			ResourceObjectTypeDefinitionType def = ResourceTypeUtil.getResourceObjectTypeDefinitionType(
	        		resource, ShadowKindType.ACCOUNT, resourceShadowDiscriminator.getIntent());
	        return def;
		} else {
			return null;
		}
    }
	
	private ResourceSchema getResourceSchema() throws SchemaException {
		return RefinedResourceSchema.getResourceSchema(resource, getNotNullPrismContext());
	}
	
    public RefinedResourceSchema getRefinedResourceSchema() throws SchemaException {
    	if (resource == null) {
    		return null;
    	}
    	return RefinedResourceSchema.getRefinedSchema(resource, LayerType.MODEL, getNotNullPrismContext());
    }
    
    public RefinedObjectClassDefinition getRefinedAccountDefinition() throws SchemaException {
		RefinedResourceSchema refinedSchema = getRefinedResourceSchema();
		if (refinedSchema == null) {
			return null;
		}
		return refinedSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, getResourceShadowDiscriminator().getIntent());
	}
	
	public Collection<ResourceObjectTypeDependencyType> getDependencies() {
		if (dependencies == null) {
			ResourceObjectTypeDefinitionType resourceAccountTypeDefinitionType = getResourceAccountTypeDefinitionType();
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
	
	public void setAccountPasswordPolicy(ValuePolicyType accountPasswordPolicy) {
		this.accountPasswordPolicy = accountPasswordPolicy;
	}
	
	public ValuePolicyType getEffectivePasswordPolicy() {
		if (accountPasswordPolicy != null) {
			return accountPasswordPolicy;
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
        ObjectDelta<O> accDelta = getDelta();

        PrismObject<O> oldAccount = getObjectOld();
        ObjectDelta<O> syncDelta = getSyncDelta();
        if (oldAccount == null && syncDelta != null
                && ChangeType.ADD.equals(syncDelta.getChangeType())) {
            PrismObject<O> objectToAdd = syncDelta.getObjectToAdd();
            if (objectToAdd != null) {
                PrismObjectDefinition<O> objectDefinition = objectToAdd.getDefinition();
                // TODO: remove constructor, use some factory method instead
                oldAccount = new PrismObject<O>(objectToAdd.getName(), objectDefinition, getNotNullPrismContext());
                oldAccount = syncDelta.computeChangedObject(oldAccount);
            }
        }

        if (accDelta == null) {
            // No change
            setObjectNew(oldAccount);
            return;
        }
        
        if (oldAccount == null && accDelta.isModify()) {
        	RefinedObjectClassDefinition rAccountDef = getRefinedAccountDefinition();
        	if (rAccountDef != null) {
        		oldAccount = (PrismObject<O>) rAccountDef.createBlankShadow();
        	}
        }

        setObjectNew(accDelta.computeChangedObject(oldAccount));
    }
    
	public void clearIntermediateResults() {
		accountConstructionDeltaSetTriple = null;
		outboundAccountConstruction = null;
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
		distributeResourceObject(getObjectNew(), resource);
		distributeResourceDelta(getPrimaryDelta(), resource);
		distributeResourceDelta(getSecondaryDelta(), resource);
	}
	
	private void distributeResourceObject(PrismObject<O> object, PrismObject<ResourceType> resource) {
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

	private void distributeResourceDelta(ObjectDelta<O> delta, PrismObject<ResourceType> resource) {
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
	public ObjectDelta<O> getExecutableDelta() throws SchemaException {
		SynchronizationPolicyDecision policyDecision = getSynchronizationPolicyDecision();
		ObjectDelta<O> origDelta = getDelta();
		if (policyDecision == SynchronizationPolicyDecision.ADD) {
            if (origDelta == null || origDelta.isModify()) {
            	// We need to convert modify delta to ADD
            	ObjectDelta<O> addDelta = new ObjectDelta<O>(getObjectTypeClass(),
                		ChangeType.ADD, getPrismContext());
                RefinedObjectClassDefinition rAccount = getRefinedAccountDefinition();

                if (rAccount == null) {
                    throw new IllegalStateException("Definition for account type " + getResourceShadowDiscriminator() 
                    		+ " not found in the context, but it should be there");
                }
                PrismObject<O> newAccount = (PrismObject<O>) rAccount.createBlankShadow();
                addDelta.setObjectToAdd(newAccount);
                
                if (origDelta != null) {
                	addDelta.merge(origDelta);
                }
                return addDelta;
            }
        } else if (policyDecision == SynchronizationPolicyDecision.KEEP) {
            // Any delta is OK
        } else if (policyDecision == SynchronizationPolicyDecision.DELETE) {
        	ObjectDelta<O> deleteDelta = new ObjectDelta<O>(getObjectTypeClass(),
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
	
	
	public void checkConsistence(String contextDesc, boolean fresh, boolean force) {
		if (synchronizationPolicyDecision == SynchronizationPolicyDecision.IGNORE) {
			// No not check these. they may be quite wild.
			return;
		}
		super.checkConsistence(contextDesc);
		if (synchronizationPolicyDecision == SynchronizationPolicyDecision.BROKEN) {
			return;
		}
    	if (fresh && !force) {
    		if (isShadow()) {
	    		if (resource == null) {
		    		throw new IllegalStateException("Null resource in "+this + (contextDesc == null ? "" : " in " +contextDesc));
		    	}
		    	if (resourceShadowDiscriminator == null) {
		    		throw new IllegalStateException("Null resource account type in "+this + (contextDesc == null ? "" : " in " +contextDesc));
		    	}
    		}
    	}
    	if (syncDelta != null) {
    		try {
    			syncDelta.checkConsistence(true, true, true);
    		} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e.getMessage()+"; in "+getElementDesc()+" sync delta in "+this + (contextDesc == null ? "" : " in " +contextDesc), e);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage()+"; in "+getElementDesc()+" sync delta in "+this + (contextDesc == null ? "" : " in " +contextDesc), e);
			}
    	}
    }
	
	protected boolean isRequireSecondardyDeltaOid() {
		if (synchronizationPolicyDecision == SynchronizationPolicyDecision.ADD) {
			return false;
		}
		return super.isRequireSecondardyDeltaOid();
	}
    
	@Override
	public void cleanup() {
		super.cleanup();
		synchronizationPolicyDecision = null;
//		isLegal = null;
//		isLegalOld = null;
		isAssigned = false;
		isActive = false;
	}
	
	@Override
	public void normalize() {
		super.normalize();
		if (syncDelta != null) {
			syncDelta.normalize();
		}
	}

	@Override
	public void reset() {
		super.reset();
		wave = -1;
		fullShadow = false;
		isAssigned = false;
		isActive = false;
		synchronizationPolicyDecision = null;
		accountConstructionDeltaSetTriple = null;
		outboundAccountConstruction = null;
		dependencies = null;
		squeezedAttributes = null;
		accountPasswordPolicy = null;
	}

	@Override
	public void adopt(PrismContext prismContext) throws SchemaException {
		super.adopt(prismContext);
		if (syncDelta != null) {
			prismContext.adopt(syncDelta);
		}
	}

	@Override
	public LensProjectionContext<O> clone(LensContext lensContext) {
		LensProjectionContext<O> clone = new LensProjectionContext<O>(getObjectTypeClass(), lensContext, resourceShadowDiscriminator);
		copyValues(clone, lensContext);
		return clone;
	}
	
	protected void copyValues(LensProjectionContext<O> clone, LensContext lensContext) {
		super.copyValues(clone, lensContext);
		// do NOT clone transient values such as accountConstructionDeltaSetTriple
		// these are not meant to be cloned and they are also not directly clonnable
		clone.dependencies = this.dependencies;
		clone.doReconciliation = this.doReconciliation;
		clone.fullShadow = this.fullShadow;
		clone.isAssigned = this.isAssigned;
		clone.iteration = this.iteration;
		clone.iterationToken = this.iterationToken;
		clone.outboundAccountConstruction = this.outboundAccountConstruction;
		clone.synchronizationPolicyDecision = this.synchronizationPolicyDecision;
		clone.resource = this.resource;
		clone.resourceShadowDiscriminator = this.resourceShadowDiscriminator;
		clone.squeezedAttributes = cloneSqueezedAttributes();
		if (this.syncDelta != null) {
			clone.syncDelta = this.syncDelta.clone();
		}
		clone.wave = this.wave;
	}

	private Map<QName, DeltaSetTriple<ItemValueWithOrigin<? extends PrismPropertyValue<?>>>> cloneSqueezedAttributes() {
		if (squeezedAttributes == null) {
			return null;
		}
		Map<QName, DeltaSetTriple<ItemValueWithOrigin<? extends PrismPropertyValue<?>>>> clonedMap 
		= new HashMap<QName, DeltaSetTriple<ItemValueWithOrigin<? extends PrismPropertyValue<?>>>>();
		Cloner<ItemValueWithOrigin<? extends PrismPropertyValue<?>>> cloner = new Cloner<ItemValueWithOrigin<? extends PrismPropertyValue<?>>>() {
			@Override
			public ItemValueWithOrigin<? extends PrismPropertyValue<?>> clone(ItemValueWithOrigin<? extends PrismPropertyValue<?>> original) {
				return original.clone();
			}
		};
		for (Entry<QName, DeltaSetTriple<ItemValueWithOrigin<? extends PrismPropertyValue<?>>>> entry: squeezedAttributes.entrySet()) {
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

	private boolean hasValueForAttribute(QName attributeName, Collection<PrismPropertyValue<AccountConstruction>> acPpvSet) {
		if (acPpvSet == null) {
			return false;
		}
		for (PrismPropertyValue<AccountConstruction> acPpv: acPpvSet) {
			AccountConstruction ac = acPpv.getValue();
			if (ac.hasValueForAttribute(attributeName)) {
				return true;
			}
		}
		return false;
	}
	
	public AccountOperation getOperation() {
		if (isAdd()) {
			return AccountOperation.ADD;
		}
		if (isDelete()) {
			return AccountOperation.DELETE;
		}
		return AccountOperation.MODIFY;
	}

    @Override
	public void checkEncrypted() {
		super.checkEncrypted();
		if (syncDelta != null) {
			CryptoUtil.checkEncrypted(syncDelta);
		}
	}

	public String getHumanReadableName() {
		StringBuilder sb = new StringBuilder();
		sb.append("account(");
		String humanReadableAccountIdentifier = getHumanReadableIdentifier();
		if (StringUtils.isEmpty(humanReadableAccountIdentifier)) {
			sb.append("no ID");
		} else {
			sb.append("ID ");
			sb.append(humanReadableAccountIdentifier);
		}
		sb.append(", type '");
		sb.append(getResourceShadowDiscriminator().getIntent());
		sb.append("', ");
		sb.append(getResource());
		sb.append(")");
		return sb.toString();
	}

	private String getHumanReadableIdentifier() {
		PrismObject<O> object = getObjectNew();
		if (object == null) {
			object = getObjectOld();
		}
		if (object == null) {
			return null;
		}
		if (object.canRepresent(ShadowType.class)) {
			PrismObject<ShadowType> shadow = (PrismObject<ShadowType>)object;
			Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getIdentifiers(shadow);
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
        sb.append(", assigned=").append(isAssigned);
        sb.append(", active=").append(isActive);
        sb.append(", legal=").append(isLegalOld).append("->").append(isLegal);
        sb.append(", recon=").append(doReconciliation);
        sb.append(", syncIntent=").append(synchronizationIntent);
        sb.append(", decision=").append(synchronizationPolicyDecision);
        if (!isFresh()) {
        	sb.append(", NOT FRESH");
        }
        if (resourceShadowDiscriminator != null && resourceShadowDiscriminator.isThombstone()) {
        	sb.append(", THOMBSTONE");
        }
        if (iteration != 0) {
        	sb.append(", iteration=").append(iteration);
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("old"), getObjectOld(), indent + 1);

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
        	DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("accountConstructionDeltaSetTriple"), accountConstructionDeltaSetTriple, indent + 1);
        	
	        sb.append("\n");
	        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("outbound account construction"), outboundAccountConstruction, indent + 1);
	        
	        sb.append("\n");
	        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("squeezed attributes"), squeezedAttributes, indent + 1);

	        // This is just a debug thing
//	        sb.append("\n");
//	        DebugUtil.indentDebugDump(sb, indent);
//	        sb.append("ACCOUNT dependencies\n");
//	        sb.append(DebugUtil.debugDump(dependencies, indent + 1));
        }

        return sb.toString();
    }

    @Override
    public String dump() {
        return debugDump();
    }
    
    @Override
	protected String getElementDefaultDesc() {
		return "projection";
	}
    
	@Override
	public String toString() {
		return "LensProjectionContext(" + (getObjectTypeClass() == null ? "null" : getObjectTypeClass().getSimpleName()) + ":" + getOid() + ")";
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

	private String getKindValue(ShadowKindType kind) {
		if (kind == null) {
			return "null";
		}
		return kind.value();
	}

	public void addToPrismContainer(PrismContainer<LensProjectionContextType> lensProjectionContextTypeContainer) throws SchemaException {

        LensProjectionContextType lensProjectionContextType = lensProjectionContextTypeContainer.createNewValue().asContainerable();

        super.storeIntoLensElementContextType(lensProjectionContextType);
        lensProjectionContextType.setSyncDelta(syncDelta != null ? DeltaConvertor.toObjectDeltaType(syncDelta) : null);
        lensProjectionContextType.setWave(wave);
        lensProjectionContextType.setResourceShadowDiscriminator(resourceShadowDiscriminator != null ?
                resourceShadowDiscriminator.toResourceShadowDiscriminatorType() : null);
        lensProjectionContextType.setFullShadow(fullShadow);
        lensProjectionContextType.setIsAssigned(isAssigned);
        lensProjectionContextType.setIsActive(isActive);
        lensProjectionContextType.setIsLegal(isLegal);
        lensProjectionContextType.setIsLegalOld(isLegalOld);
        lensProjectionContextType.setIsExists(isExists);
        lensProjectionContextType.setSynchronizationIntent(synchronizationIntent != null ? synchronizationIntent.toSynchronizationIntentType() : null);
        lensProjectionContextType.setSynchronizationPolicyDecision(synchronizationPolicyDecision != null ? synchronizationPolicyDecision.toSynchronizationPolicyDecisionType() : null);
        lensProjectionContextType.setDoReconciliation(doReconciliation);
        lensProjectionContextType.setIteration(iteration);
        lensProjectionContextType.setIterationToken(iterationToken);
        lensProjectionContextType.setAccountPasswordPolicy(accountPasswordPolicy);
    }

    public static LensProjectionContext fromLensProjectionContextType(LensProjectionContextType projectionContextType, LensContext lensContext, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {

        String objectTypeClassString = projectionContextType.getObjectTypeClass();
        if (StringUtils.isEmpty(objectTypeClassString)) {
            throw new SystemException("Object type class is undefined in LensProjectionContextType");
        }
        ResourceShadowDiscriminator resourceShadowDiscriminator = ResourceShadowDiscriminator.fromResourceShadowDiscriminatorType(projectionContextType.getResourceShadowDiscriminator());

        LensProjectionContext projectionContext;
        try {
            projectionContext = new LensProjectionContext(Class.forName(objectTypeClassString), lensContext, resourceShadowDiscriminator);
        } catch (ClassNotFoundException e) {
            throw new SystemException("Couldn't instantiate LensProjectionContext because object type class couldn't be found", e);
        }

        projectionContext.retrieveFromLensElementContextType(projectionContextType, result);
        projectionContext.syncDelta = projectionContextType.getSyncDelta() != null ? DeltaConvertor.createObjectDelta(projectionContextType.getSyncDelta(), lensContext.getPrismContext()) : null;
        projectionContext.wave = projectionContextType.getWave() != null ? projectionContextType.getWave() : 0;
        projectionContext.fullShadow = projectionContextType.isFullShadow() != null ? projectionContextType.isFullShadow() : false;
        projectionContext.isAssigned = projectionContextType.isIsAssigned() != null ? projectionContextType.isIsAssigned() : false;
        projectionContext.isActive = projectionContextType.isIsActive() != null ? projectionContextType.isIsActive() : false;
        projectionContext.isLegal = projectionContextType.isIsLegal();
        projectionContext.isExists = projectionContextType.isIsExists() != null ? projectionContextType.isIsExists() : false;
        projectionContext.synchronizationIntent = SynchronizationIntent.fromSynchronizationIntentType(projectionContextType.getSynchronizationIntent());
        projectionContext.synchronizationPolicyDecision = SynchronizationPolicyDecision.fromSynchronizationPolicyDecisionType(projectionContextType.getSynchronizationPolicyDecision());
        projectionContext.doReconciliation = projectionContextType.isDoReconciliation() != null ? projectionContextType.isDoReconciliation() : false;
        projectionContext.iteration = projectionContextType.getIteration() != null ? projectionContextType.getIteration() : 0;
        projectionContext.iterationToken = projectionContextType.getIterationToken();
        projectionContext.accountPasswordPolicy = projectionContextType.getAccountPasswordPolicy();

        return projectionContext;
    }

}
