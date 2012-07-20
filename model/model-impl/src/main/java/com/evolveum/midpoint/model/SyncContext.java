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
import com.evolveum.midpoint.common.valueconstruction.ObjectDeltaObject;
import com.evolveum.midpoint.model.synchronizer.ObjectDeltaWaves;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Synchronization context that is passed inside the model as the change is processed through several stages.
 * <p/>
 * The context contains complete information about the change that relates to a user. It container user, accounts
 * that belongs, belonged or should belong to the user, deltas, old a new values for all the objects. This information
 * is assembled and transformed as the context is passed through individual stages of model processing.
 * <p/>
 * It also contains policies (e.g. user template), channel and similar information. This can be used to customize
 * processing per-request.
 *
 * @author Radovan Semancik
 */
public class SyncContext implements Dumpable, DebugDumpable, Serializable {
	
	/**
	 * Current wave of computation and execution.
	 */
	int wave = 0;

    /**
     * User as midPointObject before any change (at the time when context was created)
     * This value is considered immutable.
     */
    private PrismObject<UserType> userOld;

    /**
     * User as midPointObject after the changes were applied. This reflects the expected state of
     * the user at the current stage of the processing. It is computed from userOld and user deltas.
     * This value is not computed automatically. It needs to be explicitly recomputed.
     */
    private PrismObject<UserType> userNew;

    /**
     * Primary change of the user object - the original change done by the user.
     */
    private ObjectDelta<UserType> userPrimaryDelta;

    /**
     * Secondary change of the user object - the change caused as a side-effect of the primary change.
     */
    private ObjectDeltaWaves<UserType> userSecondaryDeltas = new ObjectDeltaWaves<UserType>();

    /**
     * User template that should be applied during the processing of user policy. If it is null,
     * no template will be applied.
     */
    private UserTemplateType userTemplate;

    /**
     * Channel that is the source of primary change (GUI, live sync, import, ...)
     */
    private String channel;

    /**
     * Global synchronization settings that should be applied during processing of this context.
     */
    private AccountSynchronizationSettingsType accountSynchronizationSettings;
    
    private boolean noExecute = false;

    /**
     * Map of account synchronizations contexts. All the accounts in the map somehow "belong" to the user specified in this context.
     * The accounts are "distributed" in the map by account type - more exactly a composite key of resource OID and account type.
     *
     * @see ResourceAccountType
     */
    private Map<ResourceAccountType, AccountSyncContext> accountContextMap;

    /**
     * Cache of resource instances. It is used to reduce the number of read (getObject) calls for ResourceType objects.
     */
    private Map<String, ResourceType> resourceCache;

    /**
     * True if we want to reconcile all accounts in this context.
     */
    private boolean doReconciliationForAllAccounts = false;
    
    private PrismObjectDefinition<UserType> userDefinition = null;
    private PrismContext prismContext;

    public SyncContext(PrismContext prismContext) {
        accountContextMap = new HashMap<ResourceAccountType, AccountSyncContext>();
        resourceCache = new HashMap<String, ResourceType>();
        this.prismContext = prismContext;
    }

    public int getWave() {
		return wave;
	}

	public void setWave(int wave) {
		this.wave = wave;
	}
	
	public void incrementWave() {
		wave++;
	}

	public PrismObject<UserType> getUserOld() {
        return userOld;
    }

    public void setUserOld(PrismObject<UserType> userOld) {
        this.userOld = userOld;
    }

    public PrismObject<UserType> getUserNew() {
        return userNew;
    }

    public void setUserNew(PrismObject<UserType> userNew) {
        this.userNew = userNew;
    }

    public ObjectDelta<UserType> getUserPrimaryDelta() {
        return userPrimaryDelta;
    }
    
    public ObjectDelta<UserType> getWaveUserPrimaryDelta() throws SchemaException {
    	if (wave == 0) {
    		return userPrimaryDelta;
    	} else {
    		return userSecondaryDeltas.getMergedDeltas(userPrimaryDelta, wave);
    	}
    }

    public void setUserPrimaryDelta(ObjectDelta<UserType> userPrimaryDelta) {
        this.userPrimaryDelta = userPrimaryDelta;
    }

    public ObjectDelta<UserType> getUserSecondaryDelta() throws SchemaException {
        return userSecondaryDeltas.getMergedDeltas();
    }
    
    public ObjectDelta<UserType> getUserSecondaryDelta(int wave) {
    	return userSecondaryDeltas.get(wave);
    }
    
    public ObjectDelta<UserType> getWaveUserSecondaryDelta() throws SchemaException {
        return userSecondaryDeltas.get(wave);
    }

    public void setUserSecondaryDelta(ObjectDelta<UserType> userSecondaryDelta, int wave) {
        this.userSecondaryDeltas.set(wave, userSecondaryDelta);
    }
    
    public void setWaveUserSecondaryDelta(ObjectDelta<UserType> userSecondaryDelta) {
        this.userSecondaryDeltas.set(wave, userSecondaryDelta);
    }
    
	public ObjectDeltaObject<UserType> getUserOdo() throws SchemaException {
		return new ObjectDeltaObject<UserType>(userOld, getUserDelta(), userNew);
	}


    public UserTemplateType getUserTemplate() {
        return userTemplate;
    }

    public void setUserTemplate(UserTemplateType userTemplate) {
        this.userTemplate = userTemplate;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public AccountSynchronizationSettingsType getAccountSynchronizationSettings() {
        return accountSynchronizationSettings;
    }

    public void setAccountSynchronizationSettings(
            AccountSynchronizationSettingsType accountSynchronizationSettings) {
        this.accountSynchronizationSettings = accountSynchronizationSettings;
    }

    public boolean isDoReconciliationForAllAccounts() {
        return doReconciliationForAllAccounts;
    }

    public void setDoReconciliationForAllAccounts(boolean doReconciliationForAllAccounts) {
        this.doReconciliationForAllAccounts = doReconciliationForAllAccounts;
    }

    /**
     * Returns one aspect from the synchronization settings (with respect to default value).
     * TODO: maybe this is redundant?
     */
    public AssignmentPolicyEnforcementType getAssignmentPolicyEnforcementType() {
        if (accountSynchronizationSettings.getAssignmentPolicyEnforcement() == null) {
            return AssignmentPolicyEnforcementType.FULL;
        }
        return accountSynchronizationSettings.getAssignmentPolicyEnforcement();
    }

    public boolean isNoExecute() {
		return noExecute;
	}

	public void setNoExecute(boolean noExecute) {
		this.noExecute = noExecute;
	}

	public Collection<AccountSyncContext> getAccountContexts() {
        return accountContextMap.values();
    }

    public void addAccountSyncContext(ResourceAccountType rat, AccountSyncContext accountSyncContext) {
        if (accountContextMap.containsKey(rat)) {
            throw new IllegalArgumentException("Addition of duplicate account context for " + rat);
        }
        if (accountSyncContext.getResource() == null) {
            accountSyncContext.setResource(getResource(rat));
        }
        accountContextMap.put(rat, accountSyncContext);
    }
    
	public AccountSyncContext findAccountSyncContextByOid(String oid) {
		for (AccountSyncContext accCtx: getAccountContexts()) {
			if (oid.equals(accCtx.getOid())) {
				return accCtx;
			}
		}
		return null;
	}


    public void setAccountPrimaryDelta(ResourceAccountType rat, ObjectDelta<AccountShadowType> accountDelta) {
        if (!accountContextMap.containsKey(rat)) {
            accountContextMap.put(rat, new AccountSyncContext(rat, prismContext));
        }
        accountContextMap.get(rat).setAccountPrimaryDelta(accountDelta);
    }

    public void setAccountSecondaryDelta(ResourceAccountType rat, ObjectDelta<AccountShadowType> accountDelta) {
        if (!accountContextMap.containsKey(rat)) {
            accountContextMap.put(rat, new AccountSyncContext(rat, prismContext));
        }
        accountContextMap.get(rat).setAccountSecondaryDelta(accountDelta);
    }

    /**
     * Returns user delta, both primary and secondary (merged together).
     * The returned object is (kind of) immutable. Changing it may do strange things (but most likely the changes will be lost).
     */
    public ObjectDelta<UserType> getUserDelta() throws SchemaException {
        return ObjectDelta.union(userPrimaryDelta, getUserSecondaryDelta());
    }
    
    /**
     * Returns user delta, both primary and secondary (merged together) for a current wave.
     * The returned object is (kind of) immutable. Changing it may do strange things (but most likely the changes will be lost).
     */
    public ObjectDelta<UserType> getWaveUserDelta() throws SchemaException {
    	if (wave == 0) {
    		// Primary delta is executed only in the first wave (wave 0)
    		return ObjectDelta.union(userPrimaryDelta, getWaveUserSecondaryDelta());
    	} else {
    		return getWaveUserSecondaryDelta();
    	}
    }
    
	public ObjectDeltaObject<UserType> getUserObjectDeltaObject() {
		ObjectDeltaObject<UserType> userOdo;
		try {
			userOdo = new ObjectDeltaObject<UserType>(userOld, getUserDelta(), userNew);
		} catch (SchemaException e) {
			throw new SystemException("Unexpected schema error: "+e.getMessage(),e);
		}
		return userOdo;
	}

    public void setUserOid(String oid) {
        if (getUserPrimaryDelta() != null) {
            getUserPrimaryDelta().setOid(oid);
        }
        userSecondaryDeltas.setOid(oid);
        if (userNew != null) {
            userNew.setOid(oid);
        }
    }

    /**
     * Recompute new user state and new account states. It applies the deltas (both secondary and primary)
     * to the old states (userOld, accountOld), creating a new state (userNew, accountNew).
     */
    public void recomputeNew() throws SchemaException {
        recomputeUserNew();
        recomputeAccountsNew();
    }

    /**
     * Recompute new user state.
     * Assuming that oldUser is already set (or is null if it does not exist)
     */
    public void recomputeUserNew() throws SchemaException {
        ObjectDelta<UserType> userDelta = getUserDelta();
        if (userDelta == null) {
            // No change
            userNew = userOld;
            return;
        }
        userNew = userDelta.computeChangedObject(userOld);
    }

    /**
     * Recompute new account state.
     */
    public void recomputeAccountsNew() throws SchemaException {
        for (AccountSyncContext accCtx : getAccountContexts()) {
            accCtx.recomputeAccountNew();
        }
    }
    
    /**
     * Returns delta of user assignments, both primary and secondary (merged together).
     * The returned object is (kind of) immutable. Changing it may do strange things (but most likely the changes will be lost).
     */
    public ContainerDelta<AssignmentType> getAssignmentDelta() throws SchemaException {
        ObjectDelta<UserType> userDelta = getUserDelta();
        if (userDelta == null) {
            return createEmptyAssignmentDelta();
        }
        ContainerDelta<AssignmentType> assignmentDelta = userDelta.findContainerDelta(new PropertyPath(SchemaConstants.C_ASSIGNMENT));
        if (assignmentDelta == null) { 
            return createEmptyAssignmentDelta();
        }
        return assignmentDelta;
    }

    private ContainerDelta<AssignmentType> createEmptyAssignmentDelta() {
        return new ContainerDelta<AssignmentType>(getAssignmentContainerDefinition());
    }
    
    private PrismContainerDefinition<AssignmentType> getAssignmentContainerDefinition() {
		return getUserDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
	}

	private PrismObjectDefinition<UserType> getUserDefinition() {
		if (userDefinition == null) {
			userDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
		}
		return userDefinition;
	}
	

	public void addPrimaryUserDelta(ObjectDelta<UserType> userDelta) throws SchemaException {
        if (userPrimaryDelta == null) {
            userPrimaryDelta = userDelta;
        } else {
            userPrimaryDelta.merge(userDelta);
        }
    }

    /**
     * Returns refined resource schema for specified account type.
     * This is supposed to be efficient, taking the schema from the cache if possible.
     * It assumes the resource is in the cache.
     *
     * @see SyncContext#rememberResource(ResourceType)
     */
    public RefinedResourceSchema getRefinedResourceSchema(ResourceAccountType rat) throws
            SchemaException {
    	ResourceType resourceType = getResource(rat);
    	if (resourceType == null) {
    		throw new IllegalStateException("No resource for "+rat);
    	}
        return RefinedResourceSchema.getRefinedSchema(resourceType, prismContext);
    }

    /**
     * Returns refined account definition for specified account type.
     * This is supposed to be efficient, taking the schema from the cache if possible.
     * It assumes the resource is in the cache.
     *
     * @see SyncContext#rememberResource(ResourceType)
     */
    public RefinedAccountDefinition getRefinedAccountDefinition(ResourceAccountType rat) throws SchemaException {
        // TODO: check for null
        return getRefinedResourceSchema(rat).getAccountDefinition(rat.getAccountType());
    }

    /**
     * Returns a resource for specified account type.
     * This is supposed to be efficient, taking the resource from the cache. It assumes the resource is in the cache.
     *
     * @see SyncContext#rememberResource(ResourceType)
     */
    public ResourceType getResource(ResourceAccountType rat) {
        return resourceCache.get(rat.getResourceOid());
    }

    public AccountSyncContext getAccountSyncContext(ResourceAccountType rat) {
        return accountContextMap.get(rat);
    }

    /**
     * Puts resources in the cache for later use. The resources should be fetched from provisioning
     * and have pre-parsed schemas. So the next time just reuse them without the other overhead.
     */
    public void rememberResources(Collection<ResourceType> resources) {
        for (ResourceType resourceType : resources) {
            rememberResource(resourceType);
        }
    }

    /**
     * Puts resource in the cache for later use. The resource should be fetched from provisioning
     * and have pre-parsed schemas. So the next time just reuse it without the other overhead.
     */
    public void rememberResource(ResourceType resourceType) {
        resourceCache.put(resourceType.getOid(), resourceType);
    }

    /**
     * Returns all changes, user and all accounts. Both primary and secondary changes are returned, but
     * these are not merged.
     * TODO: maybe it would be better to merge them.
     */
    public Collection<ObjectDelta<? extends ObjectType>> getAllChanges() throws SchemaException {
        Collection<ObjectDelta<? extends ObjectType>> allChanges = new ArrayList<ObjectDelta<? extends ObjectType>>();

        addChangeIfNotNull(allChanges, userPrimaryDelta);
        addChangeIfNotNull(allChanges, userSecondaryDeltas.getMergedDeltas());

        for (AccountSyncContext accSyncCtx : accountContextMap.values()) {
            addChangeIfNotNull(allChanges, accSyncCtx.getAccountPrimaryDelta());
            addChangeIfNotNull(allChanges, accSyncCtx.getAccountSecondaryDelta());
        }

        return allChanges;
    }

    private <T extends ObjectType> void addChangeIfNotNull(Collection<ObjectDelta<? extends ObjectType>> changes,
            ObjectDelta<T> change) {
        if (change != null) {
            changes.add(change);
        }
    }

    /**
     * Creates new empty account sync context and adds it to this context.
     */
    public AccountSyncContext createAccountSyncContext(ResourceAccountType rat) {
        AccountSyncContext accountSyncContext = new AccountSyncContext(rat, prismContext);
        addAccountSyncContext(rat, accountSyncContext);
        return accountSyncContext;
    }
    
    public void checkConsistence() {
    	if (userOld != null) {
    		try {
    			userOld.checkConsistence();
    		} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e.getMessage()+"; in old user in SyncContext", e);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage()+"; in old user in SyncContext", e);
			}
    		if (userOld.getDefinition() == null) {
    			throw new IllegalStateException("No old user definition in "+this);
    		}
    	}
    	if (userPrimaryDelta != null) {
    		try {
    			userPrimaryDelta.checkConsistence();
    		} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e.getMessage()+"; in user primary delta in SyncContext", e);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage()+"; in user primary delta in SyncContext", e);
			}
    	}
    	
    	// Secondary delta may not have OID yet (as it may relate to ADD primary delta that doesn't have OID yet)
		boolean requireOid = userPrimaryDelta == null;
    	userSecondaryDeltas.checkConsistence(requireOid, "user secondary delta in SyncContext");
    	
    	if (userNew != null) {
    		try {
    			userNew.checkConsistence();
    		} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e.getMessage()+"; in new user in SyncContext", e);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage()+"; in new user in SyncContext", e);
			}
    		if (userNew.getDefinition() == null) {
    			throw new IllegalStateException("No new user definition in "+this);
    		}
    	}
    	for (AccountSyncContext accCtx: getAccountContexts()) {
    		accCtx.checkConsistence();
    	}
    }


    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String dump() {
        return debugDump(0);
    }
    
    public String dump(boolean showTriples) {
        return debugDump(0, showTriples);
    }

    @Override
    public String debugDump(int indent) {
    	return debugDump(indent, true);
    }
    
    public String debugDump(int indent, boolean showTriples) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("SyncContext: wave ").append(wave).append("\n");

        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("Settings: ");
        if (accountSynchronizationSettings != null) {
            sb.append("assignments:");
            sb.append(accountSynchronizationSettings.getAssignmentPolicyEnforcement());
        } else {
            sb.append("null");
        }
        sb.append("\n");

        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("USER old:");
        if (userOld == null) {
            sb.append(" null");
        } else {
            sb.append("\n");
            sb.append(userOld.debugDump(indent + 2));
        }

        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("USER new:");
        if (userNew == null) {
            sb.append(" null");
        } else {
            sb.append("\n");
            sb.append(userNew.debugDump(indent + 2));
        }

        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("USER primary delta:");
        if (userPrimaryDelta == null) {
            sb.append(" null");
        } else {
            sb.append("\n");
            sb.append(userPrimaryDelta.debugDump(indent + 2));
        }

        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("USER secondary delta:");
        if (userSecondaryDeltas.isEmpty()) {
            sb.append(" empty");
        } else {
            sb.append("\n");
            sb.append(userSecondaryDeltas.debugDump(indent + 2));
        }

        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("ACCOUNTS:");
        if (accountContextMap.isEmpty()) {
            sb.append(" none");
        } else {
        	sb.append(" (").append(accountContextMap.size()).append(")");
            for (Entry<ResourceAccountType, AccountSyncContext> entry : accountContextMap.entrySet()) {
                sb.append("\n");
                DebugUtil.indentDebugDump(sb, indent + 2);
                sb.append("ACCOUNT ");
                sb.append(entry.getKey()).append(":\n");
                sb.append(entry.getValue().debugDump(indent + 3, showTriples));
            }
        }

        // TODO

        return sb.toString();
    }

	public void swallowToWaveUserSecondaryDelta(PropertyDelta<?> propDelta) throws SchemaException {
		ObjectDelta<UserType> userSecondaryDelta = getWaveUserSecondaryDelta();
		if (userSecondaryDelta == null) {
            userSecondaryDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.MODIFY);
            if (getUserOld() != null) {
                userSecondaryDelta.setOid(getUserOld().getOid());
            }
            setWaveUserSecondaryDelta(userSecondaryDelta);
        }
        userSecondaryDelta.swallow(propDelta);
	}

}
