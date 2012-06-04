/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.model.synchronizer.AccountConstruction;
import com.evolveum.midpoint.model.synchronizer.PropertyValueWithOrigin;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceAccountTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Account synchronization context that is part of SyncContext. Synchronization context that is passed inside the model
 * as the change is processed through several stages.
 * <p/>
 * This part of the context describes an account. The account belonged, belongs, should belong or will belong to the user
 * specified in SyncContext. The specific state of the account is defined by the fields of this context, especially the
 * policyDecision field.
 *
 * @author Radovan Semancik
 * @see SyncContext
 */
public class AccountSyncContext implements Dumpable, DebugDumpable {

    /**
     * Definition of account type. This is the same value as the key in SyncContext. It is duplicated
     * here, therefore the AccountSyncContext may be used as stand-alone object.
     */
    private ResourceAccountType resourceAccountType;

    /**
     * OID of the account shadow that this context describes. It is copied to the deltas if necessary.
     * It may be null if the account is just being created.
     */
    private String oid;

    /**
     * Old state of the account (state before the change). May be null if the account haven't existed before.
     */
    private PrismObject<AccountShadowType> accountOld;

    /**
     * New state of the account (after the change). It is not created automatically, it has to be manually recomputed.
     */
    private PrismObject<AccountShadowType> accountNew;

    /**
     * Synchronization account delta. This describe changes that already happened.
     */
    private ObjectDelta<AccountShadowType> accountSyncDelta;

    /**
     * Primary account delta. This describe the change that the user explicitly requested (e.g. from GUI).
     */
    private ObjectDelta<AccountShadowType> accountPrimaryDelta;

    /**
     * Secondary account delta. This describes the changes that are an effect of primary account delta or user deltas.
     */
    private ObjectDelta<AccountShadowType> accountSecondaryDelta;
    
    /**
     * Set to true if the account loaded into accountOld is the full account with all the attributes (as opposed to repository shadow).
     */
    private boolean fullAccount = false;
    
    /**
     * Delta set triple for accounts. Specifies which accounts should be added, removed or stay as they are.
     * It tells almost nothing about attributes directly although the information about attributes are inside
     * each account construction (in a form of ValueConstruction that contains attribute delta triples).
     * 
     * Intermediary computation result. It is stored to allow re-computing of account constructions during
     * iterative computations.
     */
    private PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountConstructionDeltaSetTriple;
    
    private AccountConstruction outboundAccountConstruction;
    
    private Map<QName, DeltaSetTriple<PropertyValueWithOrigin>> squeezedAttributes;

    /**
     * Resource that hosts this account.
     */
    private ResourceType resource;

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
    
    private PrismContext prismContext;

    AccountSyncContext(ResourceAccountType resourceAccountType, PrismContext prismContext) {
    	if (resourceAccountType == null) {
    		throw new IllegalArgumentException("No resourceAccountType");
    	}
    	if (prismContext == null) {
    		throw new IllegalArgumentException("No prismContext");
    	}
        this.resourceAccountType = resourceAccountType;
        this.isAssigned = false;
        this.prismContext = prismContext;
    }

    public ObjectDelta<AccountShadowType> getAccountSyncDelta() {
        return accountSyncDelta;
    }

    public void setAccountSyncDelta(ObjectDelta<AccountShadowType> accountSyncDelta) {
        this.accountSyncDelta = accountSyncDelta;
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

    public PrismObject<AccountShadowType> getAccountOld() {
        return accountOld;
    }

    public void setAccountOld(PrismObject<AccountShadowType> accountOld) {
        this.accountOld = accountOld;
    }

    public PrismObject<AccountShadowType> getAccountNew() {
        return accountNew;
    }

    public void setAccountNew(PrismObject<AccountShadowType> accountNew) {
        this.accountNew = accountNew;
    }

    public ObjectDelta<AccountShadowType> getAccountPrimaryDelta() {
        return accountPrimaryDelta;
    }

    public void setAccountPrimaryDelta(ObjectDelta<AccountShadowType> accountPrimaryDelta) {
        this.accountPrimaryDelta = accountPrimaryDelta;
    }
    
    public void addAccountPrimaryDelta(ObjectDelta<AccountShadowType> accountDelta) throws SchemaException {
        if (accountPrimaryDelta == null) {
        	accountPrimaryDelta = accountDelta;
        } else {
        	accountPrimaryDelta.merge(accountDelta);
        }
    }
    
    public void addAccountSecondaryDelta(ObjectDelta<AccountShadowType> accountDelta) throws SchemaException {
        if (accountSecondaryDelta == null) {
        	accountSecondaryDelta = accountDelta;
        } else {
        	accountSecondaryDelta.merge(accountDelta);
        }
    }
    
    public void addAccountSyncDelta(ObjectDelta<AccountShadowType> accountDelta) throws SchemaException {
        if (accountSyncDelta == null) {
        	accountSyncDelta = accountDelta;
        } else {
        	accountSyncDelta.merge(accountDelta);
        }
    }

    public ObjectDelta<AccountShadowType> getAccountSecondaryDelta() {
        return accountSecondaryDelta;
    }

    public void setAccountSecondaryDelta(ObjectDelta<AccountShadowType> accountSecondaryDelta) {
        this.accountSecondaryDelta = accountSecondaryDelta;
    }

    public void addToSecondaryDelta(PropertyDelta accountPasswordDelta) throws SchemaException {
        if (accountSecondaryDelta == null) {
            accountSecondaryDelta = new ObjectDelta<AccountShadowType>(AccountShadowType.class, ChangeType.MODIFY);
            accountSecondaryDelta.setOid(oid);
        }
        accountSecondaryDelta.swallow(accountPasswordDelta);
    }

    public ObjectDelta<AccountShadowType> getAccountDelta() throws SchemaException {
        return ObjectDelta.union(accountPrimaryDelta, accountSecondaryDelta);
    }
    
	public boolean isAdd() {
		if (policyDecision == PolicyDecision.ADD) {
			return true;
		}
		if (ObjectDelta.isAdd(accountPrimaryDelta)) {
			return true;
		}
		if (ObjectDelta.isAdd(accountSecondaryDelta)) {
			return true;
		}
		return false;
	}

	public boolean isDelete() {
		if (policyDecision == PolicyDecision.DELETE) {
			return true;
		}
		if (ObjectDelta.isDelete(accountPrimaryDelta)) {
			return true;
		}
		if (ObjectDelta.isDelete(accountSecondaryDelta)) {
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

    public String getOid() {
        return oid;
    }

    /**
     * Sets oid to the field but also to the deltas (if applicable).
     */
    public void setOid(String oid) {
        this.oid = oid;
        if (accountPrimaryDelta != null) {
            accountPrimaryDelta.setOid(oid);
        }
        if (accountSecondaryDelta != null) {
            accountSecondaryDelta.setOid(oid);
        }
        if (accountNew != null) {
            accountNew.setOid(oid);
        }
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
	
	public boolean isFullAccount() {
		return fullAccount;
	}

	public void setFullAccount(boolean fullAccount) {
		this.fullAccount = fullAccount;
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

    /**
     * Recomputes the new state of account (accountNew). It is computed by applying deltas to the old state (accountOld).
     * Assuming that oldAccount is already set (or is null if it does not exist)
     */
    public void recomputeAccountNew() throws SchemaException {
        ObjectDelta<AccountShadowType> accDelta = getAccountDelta();

        PrismObject<AccountShadowType> oldAccount = accountOld;
        if (oldAccount == null && accountSyncDelta != null
                && ChangeType.ADD.equals(accountSyncDelta.getChangeType())) {
            PrismObject<AccountShadowType> accountToAdd = accountSyncDelta.getObjectToAdd();
            if (accountToAdd != null) {
                PrismObjectDefinition<AccountShadowType> objectDefinition = (PrismObjectDefinition<AccountShadowType>)
                        accountToAdd.getDefinition();
                // TODO: remove constructor, use some factory method instead
                oldAccount = new PrismObject<AccountShadowType>(accountToAdd.getName(), objectDefinition, prismContext);
                oldAccount = accountSyncDelta.computeChangedObject(oldAccount);
            }
        }

        if (accDelta == null) {
            // No change
            accountNew = oldAccount;
            return;
        }

        accountNew = accDelta.computeChangedObject(oldAccount);
    }
    
	public RefinedAccountDefinition getRefinedAccountDefinition() throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, prismContext);
		return refinedSchema.getAccountDefinition(getResourceAccountType().getAccountType());
	}

	public void clearIntermediateResults() {
		accountConstructionDeltaSetTriple = null;
		outboundAccountConstruction = null;
		squeezedAttributes = null;
	}
    
    public void checkConsistence() {
    	if (resource == null) {
    		throw new IllegalStateException("Null resource in "+this);
    	}
    	if (resourceAccountType == null) {
    		throw new IllegalStateException("Null resource account type in "+this);
    	}
    	if (prismContext == null) {
    		throw new IllegalStateException("Null prism context in "+this);
    	}
    	if (accountOld != null) {
    		checkConsistence(accountOld, "old account");
    	}
    	if (accountPrimaryDelta != null) {
    		try {
    			accountPrimaryDelta.checkConsistence();
    		} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e.getMessage()+"; in account primary delta in "+this, e);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage()+"; in account primary delta in "+this, e);
			}
    	}
    	if (accountSecondaryDelta != null) {
    		try {
	    		// Secondary delta may not have OID yet (as it may relate to ADD primary delta that doesn't have OID yet)
	    		boolean requireOid = accountPrimaryDelta == null;
	    		accountSecondaryDelta.checkConsistence(requireOid);
    		} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e.getMessage()+"; in account secondary delta in "+this, e);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage()+"; in account secondary delta in "+this, e);
			}

    	}
    	if (accountSyncDelta != null) {
    		try {
    			accountSyncDelta.checkConsistence();
    		} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e.getMessage()+"; in account sync delta in "+this, e);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage()+"; in account sync delta in "+this, e);
			}
    	}
    	if (accountNew != null) {
    		checkConsistence(accountNew, "new account");
    	}
    }
    
    private void checkConsistence(PrismObject<AccountShadowType> account, String desc) {
    	try {
    		account.checkConsistence();
    	} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage()+"; in "+desc+" in "+this, e);
		} catch (IllegalStateException e) {
			throw new IllegalStateException(e.getMessage()+"; in "+desc+" in "+this, e);
		}
		if (account.getDefinition() == null) {
			throw new IllegalStateException("No new account definition "+desc+" in "+this);
		}
    	PrismReference resourceRef = account.findReference(AccountShadowType.F_RESOURCE_REF);
    	if (resourceRef == null) {
    		throw new IllegalStateException("No resourceRef in "+desc+" in "+this);
    	}
    	if (StringUtils.isBlank(resourceRef.getOid())) {
    		throw new IllegalStateException("Null or empty OID in resourceRef in "+desc+" in "+this);
    	}
    }

    @Override
	public String toString() {
    	StringBuilder sb = new StringBuilder("AccountSyncContext(");
    	sb.append("OID: ").append(oid);
    	sb.append(", RAT: ").append(resourceAccountType);
    	sb.append(")");
    	return sb.toString();
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
        sb.append("OID: ").append(oid);
        if (fullAccount) {
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
        DebugUtil.debugDumpWithLabel(sb, "ACCOUNT old", accountOld, indent);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "ACCOUNT new", accountNew, indent);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "ACCOUNT primary delta", accountPrimaryDelta, indent);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "ACCOUNT secondary delta", accountSecondaryDelta, indent);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "ACCOUNT sync delta", accountSyncDelta, indent);

        if (showTriples) {
        	
        	sb.append("\n");
        	DebugUtil.debugDumpWithLabel(sb, "ACCOUNT accountConstructionDeltaSetTriple", accountConstructionDeltaSetTriple, indent);
        	
	        sb.append("\n");
	        DebugUtil.debugDumpWithLabel(sb, "ACCOUNT outbound account construction", outboundAccountConstruction, indent);
	        
	        sb.append("\n");
	        DebugUtil.debugDumpWithLabel(sb, "ACCOUNT squeezed attributes", squeezedAttributes, indent);
        }

        return sb.toString();
    }

    @Override
    public String dump() {
        return debugDump();
    }

}
