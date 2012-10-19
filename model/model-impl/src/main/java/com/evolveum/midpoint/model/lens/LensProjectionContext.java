/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.bcel.generic.GETSTATIC;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.Cloner;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ValuePolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceAccountTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceShadowDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;

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
     * Decision regarding the account. If set to null no decision was made yet. Null is also a typical value
     * when the context is created. It may be pre-set under some circumstances, e.g. if an account is being unlinked.
     */
    private SynchronizationPolicyDecision policyDecision;

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
    
    private transient Collection<ResourceShadowDiscriminatorType> dependencies = null;
    
    private transient Map<QName, DeltaSetTriple<PropertyValueWithOrigin>> squeezedAttributes;
    
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

	public void addAccountSyncDelta(ObjectDelta<O> delta) throws SchemaException {
        if (syncDelta == null) {
        	syncDelta = delta;
        } else {
        	syncDelta.merge(delta);
        }
    }
    
    public boolean isAdd() {
		if (policyDecision == SynchronizationPolicyDecision.ADD) {
			return true;
		}
		if (ObjectDelta.isAdd(getPrimaryDelta())) {
			return true;
		}
		if (ObjectDelta.isAdd(getSecondaryDelta())) {
			return true;
		}
		return false;
	}

	public boolean isDelete() {
		if (policyDecision == SynchronizationPolicyDecision.DELETE) {
			return true;
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

	public SynchronizationPolicyDecision getPolicyDecision() {
        return policyDecision;
    }

    public void setPolicyDecision(SynchronizationPolicyDecision policyDecision) {
        this.policyDecision = policyDecision;
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

	public void setFullShadow(boolean fullShadow) {
		this.fullShadow = fullShadow;
	}
	
	public boolean isShadow() {
		return ResourceObjectShadowType.class.isAssignableFrom(getObjectTypeClass());
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

    public Map<QName, DeltaSetTriple<PropertyValueWithOrigin>> getSqueezedAttributes() {
		return squeezedAttributes;
	}

	public void setSqueezedAttributes(Map<QName, DeltaSetTriple<PropertyValueWithOrigin>> squeezedAttributes) {
		this.squeezedAttributes = squeezedAttributes;
	}
	
	public ResourceAccountTypeDefinitionType getResourceAccountTypeDefinitionType() {
		if (isShadow()) {
	        ResourceAccountTypeDefinitionType def = ResourceTypeUtil.getResourceAccountTypeDefinitionType(
	        		resource, resourceShadowDiscriminator.getIntent());
	        return def;
		} else {
			return null;
		}
    }
	
	private ResourceSchema getResourceSchema() throws SchemaException {
		return RefinedResourceSchema.getResourceSchema(resource, getNotNullPrismContext());
	}
	
    public RefinedResourceSchema getRefinedResourceSchema() throws SchemaException {
    	return RefinedResourceSchema.getRefinedSchema(resource, getNotNullPrismContext());
    }
    
    public RefinedAccountDefinition getRefinedAccountDefinition() throws SchemaException {
		RefinedResourceSchema refinedSchema = getRefinedResourceSchema();
		return refinedSchema.getAccountDefinition(getResourceShadowDiscriminator().getIntent());
	}
	
	public Collection<ResourceShadowDiscriminatorType> getDependencies() {
		if (dependencies == null) {
			ResourceAccountTypeDefinitionType resourceAccountTypeDefinitionType = getResourceAccountTypeDefinitionType();
			if (resourceAccountTypeDefinitionType == null) {
				// No dependencies. But we cannot set null as that means "unknown". So let's set empty collection instead.
				dependencies = new ArrayList<ResourceShadowDiscriminatorType>();
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
		PrismReference resourceRef = object.findReference(ResourceObjectShadowType.F_RESOURCE_REF);
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
			ReferenceDelta referenceDelta = delta.findReferenceModification(ResourceObjectShadowType.F_RESOURCE_REF);
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

	public void checkConsistence() {
		checkConsistence(null, true);
	}
	
	public void checkConsistence(String contextDesc, boolean fresh) {
		super.checkConsistence(contextDesc);
    	if (fresh) {
    		if (isShadow()) {
	    		if (resource == null) {
		    		throw new IllegalStateException("Null resource in "+this + (contextDesc == null ? "" : " in " +contextDesc));
		    	}
		    	if (AccountShadowType.class.isAssignableFrom(getObjectTypeClass())) {
			    	if (resourceShadowDiscriminator == null) {
			    		throw new IllegalStateException("Null resource account type in "+this + (contextDesc == null ? "" : " in " +contextDesc));
			    	}
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
    
	@Override
	public void cleanup() {
		super.cleanup();
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
		if (this.accountConstructionDeltaSetTriple != null) {
			clone.accountConstructionDeltaSetTriple = this.accountConstructionDeltaSetTriple.clone();
		}
		clone.dependencies = this.dependencies;
		clone.doReconciliation = this.doReconciliation;
		clone.fullShadow = this.fullShadow;
		clone.isAssigned = this.isAssigned;
		clone.iteration = this.iteration;
		clone.iterationToken = this.iterationToken;
		clone.outboundAccountConstruction = this.outboundAccountConstruction;
		clone.policyDecision = this.policyDecision;
		clone.resource = this.resource;
		clone.resourceShadowDiscriminator = this.resourceShadowDiscriminator;
		clone.squeezedAttributes = cloneSqueezedAttributes();
		if (this.syncDelta != null) {
			clone.syncDelta = this.syncDelta.clone();
		}
		clone.wave = this.wave;
	}

	private Map<QName, DeltaSetTriple<PropertyValueWithOrigin>> cloneSqueezedAttributes() {
		if (squeezedAttributes == null) {
			return null;
		}
		Map<QName, DeltaSetTriple<PropertyValueWithOrigin>> clonedMap = new HashMap<QName, DeltaSetTriple<PropertyValueWithOrigin>>();
		Cloner<PropertyValueWithOrigin> cloner = new Cloner<PropertyValueWithOrigin>() {
			@Override
			public PropertyValueWithOrigin clone(PropertyValueWithOrigin original) {
				return original.clone();
			}
		};
		for (Entry<QName, DeltaSetTriple<PropertyValueWithOrigin>> entry: squeezedAttributes.entrySet()) {
			clonedMap.put(entry.getKey(), entry.getValue().clone(cloner));
		}
		return clonedMap;
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
		if (object.canRepresent(ResourceObjectShadowType.class)) {
			PrismObject<ResourceObjectShadowType> shadow = (PrismObject<ResourceObjectShadowType>)object;
			Collection<ResourceAttribute<?>> identifiers = ResourceObjectShadowUtil.getIdentifiers(shadow);
			if (identifiers == null) {
				return null;
			}
			StringBuilder sb = new StringBuilder();
			Iterator<ResourceAttribute<?>> iterator = identifiers.iterator();
			while (iterator.hasNext()) {
				ResourceAttribute<?> id = iterator.next();
				sb.append(id.getHumanReadableDump());
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
        sb.append("OID: ").append(getOid());
        sb.append(", wave ").append(wave);
        if (fullShadow) {
        	sb.append(", full");
        } else {
        	sb.append(", shadow");
        }
        sb.append(", assigned=").append(isAssigned);
        sb.append(", active=").append(isActive);
        sb.append(", recon=").append(doReconciliation);
        sb.append(", decision=").append(policyDecision);
        if (resourceShadowDiscriminator != null && resourceShadowDiscriminator.isThombstone()) {
        	sb.append(", THOMBSTONE");
        }
        if (iteration != 0) {
        	sb.append(", iteration=").append(iteration);
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("old"), getObjectOld(), indent);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("new"), getObjectNew(), indent);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("primary delta"), getPrimaryDelta(), indent);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("secondary delta"), getSecondaryDelta(), indent);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("sync delta"), getSyncDelta(), indent);

        if (showTriples) {
        	
        	sb.append("\n");
        	DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("accountConstructionDeltaSetTriple"), accountConstructionDeltaSetTriple, indent);
        	
	        sb.append("\n");
	        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("outbound account construction"), outboundAccountConstruction, indent);
	        
	        sb.append("\n");
	        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("squeezed attributes"), squeezedAttributes, indent);

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


}
