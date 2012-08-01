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
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceAccountReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceAccountTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
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
    private ResourceAccountType resourceAccountType;
	
	private boolean fullShadow = false;
	    
    /**
     * True if the account is "legal" (assigned to the user). It may be false for accounts that are either
     * found to be illegal by live sync, were unassigned from user, etc.
     * If set to null the situation is not yet known. Null is a typical value when the context is constructed.
     */
    private boolean isAssigned;

    /**
     * Decision regarding the account. If set to null no decision was made yet. Null is also a typical value
     * when the context is created. It may be pre-set under some circumstances, e.g. if an account is being unlinked.
     */
    private PolicyDecision policyDecision;

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
    
    private transient Collection<ResourceAccountReferenceType> dependencies = null;
    
    private transient Map<QName, DeltaSetTriple<PropertyValueWithOrigin>> squeezedAttributes;

    
	/**
     * Resource that hosts this projection.
     */
    transient private ResourceType resource;
    
    LensProjectionContext(Class<O> objectTypeClass, LensContext<? extends ObjectType, O> lensContext, ResourceAccountType resourceAccountType) {
    	super(objectTypeClass, lensContext);
        this.resourceAccountType = resourceAccountType;
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

    public ResourceAccountType getResourceAccountType() {
        return resourceAccountType;
    }
    
    public void addAccountSyncDelta(ObjectDelta<O> delta) throws SchemaException {
        if (syncDelta == null) {
        	syncDelta = delta;
        } else {
        	syncDelta.merge(delta);
        }
    }
    
    public boolean isAdd() {
		if (policyDecision == PolicyDecision.ADD) {
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
		if (policyDecision == PolicyDecision.DELETE) {
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

    public PolicyDecision getPolicyDecision() {
        return policyDecision;
    }

    public void setPolicyDecision(PolicyDecision policyDecision) {
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
        ResourceAccountTypeDefinitionType def = ResourceTypeUtil.getResourceAccountTypeDefinitionType(
        		resource, resourceAccountType.getAccountType());
        return def;
    }
	
	private ResourceSchema getResourceSchema() throws SchemaException {
		return RefinedResourceSchema.getResourceSchema(resource, getPrismContext());
	}
	
    public RefinedResourceSchema getRefinedResourceSchema() throws SchemaException {
    	return RefinedResourceSchema.getRefinedSchema(resource, getPrismContext());
    }
    
    public RefinedAccountDefinition getRefinedAccountDefinition() throws SchemaException {
		RefinedResourceSchema refinedSchema = getRefinedResourceSchema();
		return refinedSchema.getAccountDefinition(getResourceAccountType().getAccountType());
	}
	
	public Collection<ResourceAccountReferenceType> getDependencies() {
		if (dependencies == null) {
			ResourceAccountTypeDefinitionType resourceAccountTypeDefinitionType = getResourceAccountTypeDefinitionType();
			if (resourceAccountTypeDefinitionType == null) {
				// No dependencies. But we cannot set null as that means "unknown". So let's set empty collection instead.
				dependencies = new ArrayList<ResourceAccountReferenceType>();
			} else {
				dependencies = resourceAccountTypeDefinitionType.getDependency();
			}
		}
		return dependencies;
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
                oldAccount = new PrismObject<O>(objectToAdd.getName(), objectDefinition, getPrismContext());
                oldAccount = syncDelta.computeChangedObject(oldAccount);
            }
        }

        if (accDelta == null) {
            // No change
            setObjectNew(oldAccount);
            return;
        }

        setObjectNew(accDelta.computeChangedObject(oldAccount));
        fixShadow(getObjectNew());
    }
    
    public void fixShadows() throws SchemaException {
		fixShadow(getObjectOld());
		fixShadow(getObjectNew());
    }
    
    private void fixShadow(PrismObject<O> object) throws SchemaException {
    	if (object == null) {
    		return;
    	}
    	if (object.canRepresent(ResourceObjectShadowType.class)) {
    		PrismObject<ResourceObjectShadowType> shadow = (PrismObject<ResourceObjectShadowType>)object;
    		ResourceObjectShadowUtil.fixShadow(shadow, getResourceSchema());
    	}
    }

	public void clearIntermediateResults() {
		accountConstructionDeltaSetTriple = null;
		outboundAccountConstruction = null;
		squeezedAttributes = null;
	}
	
	public void checkConsistence() {
		checkConsistence(null, true);
	}
	
	public void checkConsistence(String contextDesc, boolean fresh) {
		super.checkConsistence(contextDesc);
    	if (fresh && resource == null) {
    		throw new IllegalStateException("Null resource in "+this + (contextDesc == null ? "" : " in " +contextDesc));
    	}
    	if (resourceAccountType == null) {
    		throw new IllegalStateException("Null resource "+getElementDesc()+" type in "+this + (contextDesc == null ? "" : " in " +contextDesc));
    	}
    	if (syncDelta != null) {
    		try {
    			syncDelta.checkConsistence();
    		} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e.getMessage()+"; in "+getElementDesc()+" sync delta in "+this + (contextDesc == null ? "" : " in " +contextDesc), e);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage()+"; in "+getElementDesc()+" sync delta in "+this + (contextDesc == null ? "" : " in " +contextDesc), e);
			}
    	}
    }
    
	@Override
	public LensProjectionContext<O> clone(LensContext lensContext) {
		LensProjectionContext<O> clone = new LensProjectionContext<O>(getObjectTypeClass(), lensContext, resourceAccountType);
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
		clone.resourceAccountType = this.resourceAccountType;
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
		sb.append(getResourceAccountType().getAccountType());
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
        sb.append(", recon=").append(doReconciliation);
        sb.append(", decision=").append(policyDecision);
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
		return "LensProjectionContext(" + getObjectTypeClass().getSimpleName() + ":" + getOid() + ")";
	}


}
