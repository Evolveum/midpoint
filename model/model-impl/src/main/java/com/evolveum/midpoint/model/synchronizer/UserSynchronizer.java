/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.model.synchronizer;

import java.util.Collection;
import java.util.Iterator;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.util.Utils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author semancik
 */
@Component
public class UserSynchronizer {

    private static final Trace LOGGER = TraceManager.getTrace(UserSynchronizer.class);

    @Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @Autowired(required = true)
    private ProvisioningService provisioningService;

    @Autowired(required = true)
    private UserPolicyProcessor userPolicyProcessor;

    @Autowired(required = true)
    private AssignmentProcessor assignmentProcessor;

    @Autowired(required = true)
    private InboundProcessor inboundProcessor;
    
    @Autowired(required = true)
    private AccountValuesProcessor accountValuesProcessor;

    @Autowired(required = true)
    private ReconciliationProcessor reconciliationProcessor;

    @Autowired(required = true)
    private CredentialsProcessor credentialsProcessor;

    @Autowired(required = true)
    private ActivationProcessor activationProcessor;

    @Autowired(required = true)
    private PrismContext prismContext;
    
    private boolean consistenceChecks = true;
    private SyncContextListener syncContextListener;
    
    public SyncContextListener getSyncContextListener() {
		return syncContextListener;
	}

	public void setSyncContextListener(SyncContextListener syncContextListener) {
		this.syncContextListener = syncContextListener;
	}

	public void synchronizeUser(SyncContext context, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, 
            ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {

		if (syncContextListener != null) {
        	syncContextListener.beforeSync(context);
        }
		
    	if (consistenceChecks) context.checkConsistence();
    	
        loadUser(context, result);
        loadFromSystemConfig(context, result);
        context.recomputeUserNew();

        if (consistenceChecks) context.checkConsistence();
        
        loadAccountRefs(context, result);
        context.recomputeUserNew();
        
        if (consistenceChecks) context.checkConsistence();

        // Check reconcile flag in account sync context and set accountOld
        // variable if it's not set (from provisioning)
        checkAccountContextReconciliation(context, result);

        SynchronizerUtil.traceContext("load", context, false);
        if (consistenceChecks) context.checkConsistence();
                
        // Loop through the account changes, apply inbound expressions
        inboundProcessor.processInbound(context, result);
        context.recomputeUserNew();
        SynchronizerUtil.traceContext("inbound", context, false);
        if (consistenceChecks) context.checkConsistence();

        userPolicyProcessor.processUserPolicy(context, result);
        context.recomputeUserNew();
        SynchronizerUtil.traceContext("user policy", context, false);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("User delta:\n{}", context.getUserDelta() == null ? "null" : context.getUserDelta().dump());
        }
        if (consistenceChecks) context.checkConsistence();

        assignmentProcessor.processAssignmentsAccounts(context, result);
        context.recomputeNew();
        SynchronizerUtil.traceContext("assignments", context, true);
        if (consistenceChecks) context.checkConsistence();

        accountValuesProcessor.process(context, result);
        
        credentialsProcessor.processCredentials(context, result);
        context.recomputeNew();
        SynchronizerUtil.traceContext("credentials", context, false);
        if (consistenceChecks) context.checkConsistence();

        activationProcessor.processActivation(context, result);
        context.recomputeNew();
        SynchronizerUtil.traceContext("activation", context, false);
        if (consistenceChecks) context.checkConsistence();

        reconciliationProcessor.processReconciliation(context, result);
        context.recomputeNew();
        SynchronizerUtil.traceContext("reconciliation", context, false);
        if (consistenceChecks) context.checkConsistence();
        
        if (syncContextListener != null) {
        	syncContextListener.afterSync(context);
        }

    }

    private void checkAccountContextReconciliation(SyncContext context, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {

        OperationResult subResult = result.createSubresult(UserSynchronizer.class + ".checkAccountContextReconciliation");
        try {
            for (AccountSyncContext accContext : context.getAccountContexts()) {
                if (!accContext.isDoReconciliation() || accContext.getAccountOld() != null) {
                    continue;
                }

                AccountShadowType account = provisioningService.getObject(AccountShadowType.class, accContext.getOid(),
                        null, subResult).asObjectable();
                ResourceType resource = Utils.getResource(account, provisioningService, result);
                PrismObjectDefinition<AccountShadowType> definition = RefinedResourceSchema.getRefinedSchema(
                        resource, prismContext).getObjectDefinition(account);

                PrismObject<AccountShadowType> object = definition.instantiate(SchemaConstants.I_ACCOUNT);
                object.setOid(account.getOid());
                accContext.setAccountOld(object);
            }
        } finally {
            subResult.computeStatus();
        }
    }

    private void loadUser(SyncContext context, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (context.getUserOld() != null) {
            // already loaded
            return;
        }
        if (context.getUserDelta().getObjectToAdd() != null) {
            //we're adding user
            //todo it's only fast fix - how to check that we're adding user
            return;
        }

        ObjectDelta<UserType> userPrimaryDelta = context.getUserPrimaryDelta();
        if (userPrimaryDelta == null) {
            // no change to user
            // TODO: where to get OID from????
            throw new UnsupportedOperationException("TODO");
        }
        if (userPrimaryDelta.getChangeType() == ChangeType.ADD) {
            // null oldUser is OK
            return;
        }
        String userOid = userPrimaryDelta.getOid();
        if (StringUtils.isBlank(userOid)) {
        	throw new IllegalArgumentException("No OID in primary user delta");
        }

        PrismObject<UserType> user = cacheRepositoryService.getObject(UserType.class, userOid, null, result);
        context.setUserOld(user);
    }

    private void loadAccountRefs(SyncContext context, OperationResult result) throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        PolicyDecision policyDecision = null;
        if (context.getUserPrimaryDelta() != null && context.getUserPrimaryDelta().getChangeType() == ChangeType.DELETE) {
            // If user is deleted, all accounts should also be deleted
            policyDecision = PolicyDecision.DELETE;
        }

        PrismObject<UserType> userOld = context.getUserOld();
        if (userOld != null) {
            loadAccountRefsFromUser(context, userOld, policyDecision, result);
        }

       loadAccountRefsFromDelta(context, userOld, context.getUserPrimaryDelta(), result);
       
       loadAccountContextsSync(context, result);
    }

	/**
     * Does not overwrite existing account contexts, just adds new ones.
     */
    private void loadAccountRefsFromUser(SyncContext context, PrismObject<UserType> user, PolicyDecision policyDecision,
            OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, 
            SecurityViolationException {
    	PrismReference accountRef = user.findReference(UserType.F_ACCOUNT_REF);
    	if (accountRef == null) {
    		return;
    	}
    	for (PrismReferenceValue accountRefVal: accountRef.getValues()) {
        	String oid = accountRefVal.getOid();
        	if (StringUtils.isBlank(oid)) {
            	LOGGER.trace("Null or empty OID in account reference {} in user:\n{}", accountRef, user.dump());
            	throw new SchemaException("Null or empty OID in account reference in "+user);
            }
            if (accountContextAlreadyExists(oid, context)) {
                continue;
            }
        	PrismObject<AccountShadowType> account = accountRefVal.getObject();
        	if (account == null) {
	            // Fetching from repository instead of provisioning so we avoid reading in a full account
	            account = cacheRepositoryService.getObject(AccountShadowType.class, oid, null, result);
        	}
        	AccountSyncContext accountSyncContext = getOrCreateAccountContext(context, account, result);
        	if (accountSyncContext.getPolicyDecision() == null) {
                accountSyncContext.setPolicyDecision(policyDecision);
            }
        	if (accountSyncContext.isDoReconciliation()) {
				// Do not load old account now. It will get loaded later in the reconciliation step.
				continue;
			}
        	accountSyncContext.setAccountOld(account);
            if (context.isDoReconciliationForAllAccounts()) {
                accountSyncContext.setDoReconciliation(true);
            }
        }
    }

	private void loadAccountRefsFromDelta(SyncContext context, PrismObject<UserType> user, 
			ObjectDelta<UserType> userPrimaryDelta, OperationResult result) throws SchemaException, 
			ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (userPrimaryDelta == null) {
			return;
		}

		ReferenceDelta accountRefDelta;
		if (userPrimaryDelta.getChangeType() == ChangeType.ADD) {
			PrismReference accountRef = userPrimaryDelta.getObjectToAdd().findReference(UserType.F_ACCOUNT_REF);
			if (accountRef == null) {
				// Adding new user with no accountRef -> nothing to do
				return;
			}
			accountRefDelta = accountRef.createDelta(new PropertyPath(UserType.F_ACCOUNT_REF));
			accountRefDelta.addValuesToAdd(PrismValue.cloneValues(accountRef.getValues()));
		} else if (userPrimaryDelta.getChangeType() == ChangeType.MODIFY) {
			accountRefDelta = userPrimaryDelta.findReferenceModification(UserType.F_ACCOUNT_REF);
			if (accountRefDelta == null) {
				return;
			}
		} else {
			// delete, all existing account are already marked for delete
			return;
		}
		
		if (accountRefDelta.isReplace()) {
			// process "replace" by distributing values to delete and add
			accountRefDelta = (ReferenceDelta)accountRefDelta.clone();
			PrismReference accountRef = user.findReference(UserType.F_ACCOUNT_REF);
			accountRefDelta.distributeReplace(accountRef == null? null : accountRef.getValues());
		}
		
		if (accountRefDelta.getValuesToAdd() != null) {
			for (PrismReferenceValue refVal: accountRefDelta.getValuesToAdd()) {
				String oid = refVal.getOid();
				AccountSyncContext accountSyncContext = null;
				PrismObject<AccountShadowType> account = null;
				if (oid == null) {
					// Adding new account
					account = refVal.getObject();
					if (account == null) {
		            	throw new SchemaException("Null or empty OID in account reference "+refVal+" in " + user);
					}
					// Create account context from embedded object
					accountSyncContext = getOrCreateAccountContext(context, account, result);
					// This is a new account that is to be added. So it should go to account primary delta
					ObjectDelta<AccountShadowType> accountPrimaryDelta = account.createAddDelta();
					accountSyncContext.setAccountPrimaryDelta(accountPrimaryDelta);
				} else {
					// We have OID. This is either linking of exising account or add of new account
					// therefore check for account existence to decide
					try {
						account = cacheRepositoryService.getObject(AccountShadowType.class, oid, null, result);
						// Create account context from retrieved object
						accountSyncContext = getOrCreateAccountContext(context, account, result);
						accountSyncContext.setAccountOld(account);
					} catch (ObjectNotFoundException e) {
						if (refVal.getObject() == null) {
							// account does not exist, no compisite account in ref -> this is really an error
							throw e;
						} else {
							// New account (with OID)
							account = refVal.getObject();
							// Create account context from embedded object
							accountSyncContext = getOrCreateAccountContext(context, account, result);
							ObjectDelta<AccountShadowType> accountPrimaryDelta = account.createAddDelta();
							accountSyncContext.setAccountPrimaryDelta(accountPrimaryDelta);
						}
					}				
				}
				if (context.isDoReconciliationForAllAccounts()) {
	                accountSyncContext.setDoReconciliation(true);
	            }
			}			
		}
		
		if (accountRefDelta.getValuesToDelete() != null) {
			for (PrismReferenceValue refVal: accountRefDelta.getValuesToDelete()) {
				String oid = refVal.getOid();
				AccountSyncContext accountSyncContext = null;
				PrismObject<AccountShadowType> account = null;
				if (oid == null) {
					throw new SchemaException("Cannot delete account ref withot an oid in " + user);
				} else {
					try {
						account = cacheRepositoryService.getObject(AccountShadowType.class, oid, null, result);
						// Create account context from retrieved object
						accountSyncContext = getOrCreateAccountContext(context, account, result);
						accountSyncContext.setAccountOld(account);
					} catch (ObjectNotFoundException e) {
						// This is still OK. It means deleting an accountRef that points to non-existing object
						// just log a warning
						LOGGER.warn("Deleting accountRef of " + user + " that points to non-existing OID " + oid);
					}				
				}
				if (accountSyncContext != null) {
					accountSyncContext.setPolicyDecision(PolicyDecision.UNLINK);
					if (context.isDoReconciliationForAllAccounts()) {
						accountSyncContext.setDoReconciliation(true);
					}
	            }
			}
		}
		
		// remove the accountRefs without oid. These will get into the way now. The accounts 
		// are in the context now and will be linked at the end of the process (it they survive the policy)
		// We need to make sure this happens on the real primary user delta
		
		if (userPrimaryDelta.getChangeType() == ChangeType.ADD) {
			PrismReference accountRef = userPrimaryDelta.getObjectToAdd().findReference(UserType.F_ACCOUNT_REF);
			pruneOidlessReferences(accountRef.getValues());
		} else if (userPrimaryDelta.getChangeType() == ChangeType.MODIFY) {
			accountRefDelta = userPrimaryDelta.findReferenceModification(UserType.F_ACCOUNT_REF);
			pruneOidlessReferences(accountRefDelta.getValuesToAdd());
			pruneOidlessReferences(accountRefDelta.getValuesToReplace());
			pruneOidlessReferences(accountRefDelta.getValuesToDelete());
		}
		
	}

	private void loadAccountContextsSync(SyncContext context, OperationResult result) throws SchemaException, ObjectNotFoundException,
			CommunicationException, ConfigurationException, SecurityViolationException {
		for (AccountSyncContext accountCtx: context.getAccountContexts()) {
			if (accountCtx.getAccountOld() != null) {
				// already loaded
				continue;
			}
			if (accountCtx.isDoReconciliation()) {
				// Do not load old account now. It will get loaded later in the reconciliation step.
				continue;
			}
			ObjectDelta<AccountShadowType> syncDelta = accountCtx.getAccountSyncDelta();
			if (syncDelta != null) {
				String oid = syncDelta.getOid();
				PrismObject<AccountShadowType> account = null;
				if (syncDelta.getChangeType() == ChangeType.ADD) {
					account = syncDelta.getObjectToAdd().clone();
					accountCtx.setAccountOld(account);
				} else {
					if (oid == null) {
						throw new IllegalArgumentException("No OID in sync delta in "+accountCtx);
					}
					account = cacheRepositoryService.getObject(AccountShadowType.class, oid, null, result);
					// We will not set old account if the delta is delete. The account does not really exists now.
					// (but the OID and resource will be set from the repo shadow)
					if (syncDelta.getChangeType() != ChangeType.DELETE) {
						syncDelta.applyTo(account);
						accountCtx.setAccountOld(account);
					}
				}
				// Make sure OID is set correctly
				accountCtx.setOid(oid);
				// Make sure that resource is also resolved
				if (accountCtx.getResource() == null) {
					String resourceOid = ResourceObjectShadowUtil.getResourceOid(account.asObjectable());
					if (resourceOid == null) {
						throw new IllegalArgumentException("No resource OID in "+account);
					}
					ResourceType resourceType = provisioningService.getObject(ResourceType.class, resourceOid, null, result).asObjectable();
	                context.rememberResource(resourceType);
	                accountCtx.setResource(resourceType);
				}

			}
		}
	}

	
	private void pruneOidlessReferences(Collection<PrismReferenceValue> refVals) {
		if (refVals == null) {
			return;
		}
		Iterator<PrismReferenceValue> iterator = refVals.iterator();
		while (iterator.hasNext()) {
			PrismReferenceValue referenceValue = iterator.next();
			if (referenceValue.getOid() == null) {
				iterator.remove();
			}
		}
	}
	
	private AccountSyncContext getOrCreateAccountContext(SyncContext context, PrismObject<AccountShadowType> account, 
			OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, 
			SecurityViolationException {
		AccountShadowType accountType = account.asObjectable();
        String resourceOid = ResourceObjectShadowUtil.getResourceOid(accountType);
        ResourceAccountType rat = new ResourceAccountType(resourceOid, accountType.getAccountType());
        AccountSyncContext accountSyncContext = context.getAccountSyncContext(rat);
        if (accountSyncContext == null) {
            ResourceType resource = context.getResource(rat);
            if (resource == null) {
                // Fetching from provisioning to take advantage of caching and pre-parsed schema
                resource = provisioningService.getObject(ResourceType.class, resourceOid, null, result).asObjectable();
                context.rememberResource(resource);
            }
            accountSyncContext = context.createAccountSyncContext(rat);
        }
        accountSyncContext.setOid(account.getOid());
        return accountSyncContext;
	}

	private boolean accountContextAlreadyExists(String oid, SyncContext context) {
        for (AccountSyncContext accContext : context.getAccountContexts()) {
            if (oid.equals(accContext.getOid())) {
                return true;
            }
        }

        return false;
    }

    private void loadFromSystemConfig(SyncContext context, OperationResult result) throws ObjectNotFoundException,
            SchemaException {
        PrismObject<SystemConfigurationType> systemConfiguration = 
        	cacheRepositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), null,
        			result);
        if (systemConfiguration == null) {
            // throw new SystemException("System configuration object is null (should not happen!)");
            // This should not happen, but it happens in tests. And it is a convenient short cut. Tolerate it for now.
            LOGGER.warn("System configuration object is null (should not happen!)");
            return;
        }
        
        SystemConfigurationType systemConfigurationType = systemConfiguration.asObjectable();

        if (context.getUserTemplate() == null) {
            UserTemplateType defaultUserTemplate = systemConfigurationType.getDefaultUserTemplate();
            context.setUserTemplate(defaultUserTemplate);
        }

        if (context.getAccountSynchronizationSettings() == null) {
            AccountSynchronizationSettingsType globalAccountSynchronizationSettings = systemConfigurationType.getGlobalAccountSynchronizationSettings();
            LOGGER.trace("Applying globalAccountSynchronizationSettings to context: {}", globalAccountSynchronizationSettings);
            context.setAccountSynchronizationSettings(globalAccountSynchronizationSettings);
        }
    }

}
