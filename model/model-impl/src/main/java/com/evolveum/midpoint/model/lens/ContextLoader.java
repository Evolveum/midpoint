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

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author semancik
 *
 */
@Component
public class ContextLoader {

	@Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;
	
	@Autowired(required = true)
    private ProvisioningService provisioningService;
	
	private static final Trace LOGGER = TraceManager.getTrace(ContextLoader.class);
	
	public <F extends ObjectType, P extends ObjectType> void load(LensContext<F,P> context, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
		loadObjectOld(context, result);
		
		if (focusContext.getObjectTypeClass() != UserType.class) {
    		// We can do this only for user.
    		return;
    	}
		LensContext<UserType,AccountShadowType> ucContext = (LensContext<UserType,AccountShadowType>) context;
		
        loadFromSystemConfig(ucContext, result);
        context.recomputeFocus();
        
        loadAccountRefs(ucContext, result);
        context.recomputeFocus();

        LensUtil.traceContext("load", context, false);

        // Check reconcile flag in account sync context and set accountOld
        // variable if it's not set (from provisioning)
        checkAccountContextReconciliation(ucContext, result);		
	}
	
	private <F extends ObjectType, P extends ObjectType> void loadObjectOld(LensContext<F,P> context, OperationResult result) throws SchemaException, ObjectNotFoundException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			// Nothing to load
			return;
		}
        if (focusContext.getObjectOld() != null) {
            // already loaded
            return;
        }
        ObjectDelta<F> objectDelta = focusContext.getDelta();
        if (objectDelta != null && objectDelta.isAdd()) {
            //we're adding the focal object. No need to load it, it is in the delta
            return;
        }

        ObjectDelta<F> primaryDelta = focusContext.getPrimaryDelta();
        String userOid = primaryDelta.getOid();
        if (StringUtils.isBlank(userOid)) {
        	throw new IllegalArgumentException("No OID in primary focus delta");
        }

        PrismObject<F> object = cacheRepositoryService.getObject(focusContext.getObjectTypeClass(), userOid, result);
        focusContext.setObjectOld(object);
    }
	
	private <F extends ObjectType, P extends ObjectType> void loadFromSystemConfig(LensContext<F,P> context, OperationResult result)
			throws ObjectNotFoundException, SchemaException {
		PrismObject<SystemConfigurationType> systemConfiguration = 
			cacheRepositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
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
	
	private void loadAccountRefs(LensContext<UserType,AccountShadowType> context, OperationResult result) throws ObjectNotFoundException,
			SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
		if (focusContext == null) {
			// Nothing to load
			return;
		}
		PolicyDecision policyDecision = null;
		if (focusContext.getPrimaryDelta() != null && focusContext.getPrimaryDelta().isDelete()) {
			// If user is deleted, all accounts should also be deleted
			policyDecision = PolicyDecision.DELETE;
		}

		PrismObject<UserType> userOld = focusContext.getObjectOld();
		if (userOld != null) {
			loadAccountRefsFromUser(context, userOld, policyDecision, result);
		}

		loadAccountRefsFromDelta(context, userOld, focusContext.getPrimaryDelta(), result);

		loadAccountContextsSync(context, result);
	}

	/**
	 * Does not overwrite existing account contexts, just adds new ones.
	 */
	private void loadAccountRefsFromUser(LensContext<UserType,AccountShadowType> context, PrismObject<UserType> user,
			PolicyDecision policyDecision, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		PrismReference accountRef = user.findReference(UserType.F_ACCOUNT_REF);
		if (accountRef == null) {
			return;
		}
		for (PrismReferenceValue accountRefVal : accountRef.getValues()) {
			String oid = accountRefVal.getOid();
			if (StringUtils.isBlank(oid)) {
				LOGGER.trace("Null or empty OID in account reference {} in user:\n{}", accountRef,
						user.dump());
				throw new SchemaException("Null or empty OID in account reference in " + user);
			}
			if (accountContextAlreadyExists(oid, context)) {
				continue;
			}
			PrismObject<AccountShadowType> account = accountRefVal.getObject();
			if (account == null) {
				// Fetching from repository instead of provisioning so we avoid
				// reading in a full account
				account = cacheRepositoryService.getObject(AccountShadowType.class, oid, result);
			}
			LensProjectionContext<AccountShadowType> accountSyncContext = getOrCreateAccountContext(context, account, result);
			if (accountSyncContext.getPolicyDecision() == null) {
				accountSyncContext.setPolicyDecision(policyDecision);
			}
			if (accountSyncContext.isDoReconciliation()) {
				// Do not load old account now. It will get loaded later in the
				// reconciliation step.
				continue;
			}
			accountSyncContext.setObjectOld(account);
			accountSyncContext.fixShadows();
			if (context.isDoReconciliationForAllProjections()) {
				accountSyncContext.setDoReconciliation(true);
			}
		}
	}

	private void loadAccountRefsFromDelta(LensContext<UserType,AccountShadowType> context, PrismObject<UserType> user,
			ObjectDelta<UserType> userPrimaryDelta, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException {
		if (userPrimaryDelta == null) {
			return;
		}

		ReferenceDelta accountRefDelta;
		if (userPrimaryDelta.getChangeType() == ChangeType.ADD) {
			PrismReference accountRef = userPrimaryDelta.getObjectToAdd().findReference(
					UserType.F_ACCOUNT_REF);
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
			accountRefDelta = (ReferenceDelta) accountRefDelta.clone();
			PrismReference accountRef = user.findReference(UserType.F_ACCOUNT_REF);
			accountRefDelta.distributeReplace(accountRef == null ? null : accountRef.getValues());
		}

		if (accountRefDelta.getValuesToAdd() != null) {
			for (PrismReferenceValue refVal : accountRefDelta.getValuesToAdd()) {
				String oid = refVal.getOid();
				LensProjectionContext<AccountShadowType> accountSyncContext = null;
				PrismObject<AccountShadowType> account = null;
				boolean isCombinedAdd = false;
				if (oid == null) {
					// Adding new account
					account = refVal.getObject();
					if (account == null) {
						throw new SchemaException("Null or empty OID in account reference " + refVal + " in "
								+ user);
					}
					// Create account context from embedded object
					accountSyncContext = getOrCreateAccountContext(context, account, result);
					// This is a new account that is to be added. So it should
					// go to account primary delta
					ObjectDelta<AccountShadowType> accountPrimaryDelta = account.createAddDelta();
					accountSyncContext.setPrimaryDelta(accountPrimaryDelta);
					accountSyncContext.setFullShadow(true);
					isCombinedAdd = true;
				} else {
					// We have OID. This is either linking of exising account or
					// add of new account
					// therefore check for account existence to decide
					try {
						account = cacheRepositoryService.getObject(AccountShadowType.class, oid, result);
						// Create account context from retrieved object
						accountSyncContext = getOrCreateAccountContext(context, account, result);
						accountSyncContext.setObjectOld(account);
						accountSyncContext.fixShadows();
					} catch (ObjectNotFoundException e) {
						if (refVal.getObject() == null) {
							// account does not exist, no composite account in
							// ref -> this is really an error
							throw e;
						} else {
							// New account (with OID)
							account = refVal.getObject();
							// Create account context from embedded object
							accountSyncContext = getOrCreateAccountContext(context, account, result);
							ObjectDelta<AccountShadowType> accountPrimaryDelta = account.createAddDelta();
							accountSyncContext.setPrimaryDelta(accountPrimaryDelta);
							accountSyncContext.setFullShadow(true);
							isCombinedAdd = true;
						}
					}
				}
				if (context.isDoReconciliationForAllProjections() && !isCombinedAdd) {
					accountSyncContext.setDoReconciliation(true);
				}
			}
		}

		if (accountRefDelta.getValuesToDelete() != null) {
			for (PrismReferenceValue refVal : accountRefDelta.getValuesToDelete()) {
				String oid = refVal.getOid();
				LensProjectionContext<AccountShadowType> accountSyncContext = null;
				PrismObject<AccountShadowType> account = null;
				if (oid == null) {
					throw new SchemaException("Cannot delete account ref withot an oid in " + user);
				} else {
					try {
						account = cacheRepositoryService.getObject(AccountShadowType.class, oid, result);
						// Create account context from retrieved object
						accountSyncContext = getOrCreateAccountContext(context, account, result);
						accountSyncContext.setObjectOld(account);
					} catch (ObjectNotFoundException e) {
						// This is still OK. It means deleting an accountRef
						// that points to non-existing object
						// just log a warning
						LOGGER.warn("Deleting accountRef of " + user + " that points to non-existing OID "
								+ oid);
					}
				}
				if (accountSyncContext != null) {
					if (refVal.getObject() == null) {
						accountSyncContext.setPolicyDecision(PolicyDecision.UNLINK);
					} else {
						accountSyncContext.setPolicyDecision(PolicyDecision.DELETE);
						ObjectDelta<AccountShadowType> accountPrimaryDelta = account.createDeleteDelta();
						accountSyncContext.setPrimaryDelta(accountPrimaryDelta);
					}
				}
			}
		}

		// remove the accountRefs without oid. These will get into the way now.
		// The accounts
		// are in the context now and will be linked at the end of the process
		// (it they survive the policy)
		// We need to make sure this happens on the real primary user delta

		if (userPrimaryDelta.getChangeType() == ChangeType.ADD) {
			PrismReference accountRef = userPrimaryDelta.getObjectToAdd().findReference(
					UserType.F_ACCOUNT_REF);
			pruneOidlessReferences(accountRef.getValues());
		} else if (userPrimaryDelta.getChangeType() == ChangeType.MODIFY) {
			accountRefDelta = userPrimaryDelta.findReferenceModification(UserType.F_ACCOUNT_REF);
			pruneOidlessReferences(accountRefDelta.getValuesToAdd());
			pruneOidlessReferences(accountRefDelta.getValuesToReplace());
			pruneOidlessReferences(accountRefDelta.getValuesToDelete());
		}

	}

	private void loadAccountContextsSync(LensContext<UserType,AccountShadowType> context, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException {
		for (LensProjectionContext<AccountShadowType> accountCtx : context.getProjectionContexts()) {
			if (accountCtx.getObjectOld() != null) {
				// already loaded
				continue;
			}
			if (accountCtx.isDoReconciliation()) {
				// Do not load old account now. It will get loaded later in the
				// reconciliation step.
				continue;
			}
			ObjectDelta<AccountShadowType> syncDelta = accountCtx.getSyncDelta();
			if (syncDelta != null) {
				String oid = syncDelta.getOid();
				PrismObject<AccountShadowType> account = null;
				if (syncDelta.getChangeType() == ChangeType.ADD) {
					account = syncDelta.getObjectToAdd().clone();
					accountCtx.setObjectOld(account);
					accountCtx.fixShadows();
				} else {
					if (oid == null) {
						throw new IllegalArgumentException("No OID in sync delta in " + accountCtx);
					}
					account = cacheRepositoryService.getObject(AccountShadowType.class, oid, result);
					// We will not set old account if the delta is delete. The
					// account does not really exists now.
					// (but the OID and resource will be set from the repo
					// shadow)
					if (syncDelta.getChangeType() != ChangeType.DELETE) {
						syncDelta.applyTo(account);
						accountCtx.setObjectOld(account);
						accountCtx.fixShadows();
					}
				}
				// Make sure OID is set correctly
				accountCtx.setOid(oid);
				// Make sure that resource is also resolved
				if (accountCtx.getResource() == null) {
					String resourceOid = ResourceObjectShadowUtil.getResourceOid(account.asObjectable());
					if (resourceOid == null) {
						throw new IllegalArgumentException("No resource OID in " + account);
					}
					ResourceType resourceType = provisioningService.getObject(ResourceType.class,
							resourceOid, null, result).asObjectable();
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

	private LensProjectionContext<AccountShadowType> getOrCreateAccountContext(LensContext<UserType,AccountShadowType> context,
			PrismObject<AccountShadowType> account, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		AccountShadowType accountType = account.asObjectable();
		String resourceOid = ResourceObjectShadowUtil.getResourceOid(accountType);
		if (resourceOid == null) {
			throw new SchemaException("The " + account + " has null resource reference OID");
		}
		ResourceAccountType rat = new ResourceAccountType(resourceOid, accountType.getAccountType());
		LensProjectionContext<AccountShadowType> accountSyncContext = LensUtil.getOrCreateAccountContext(context, rat, provisioningService, result);
		accountSyncContext.setOid(account.getOid());
		return accountSyncContext;
	}

	private boolean accountContextAlreadyExists(String oid, LensContext<UserType,AccountShadowType> context) {
		for (LensProjectionContext<AccountShadowType> accContext : context.getProjectionContexts()) {
			if (oid.equals(accContext.getOid())) {
				return true;
			}
		}

		return false;
	}
	
	private void checkAccountContextReconciliation(LensContext<UserType,AccountShadowType> context, OperationResult result)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
			SecurityViolationException {

		OperationResult subResult = result.createSubresult(ContextLoader.class.getName()
				+ ".checkAccountContextReconciliation");
		try {
			for (LensProjectionContext<AccountShadowType> accContext : context.getProjectionContexts()) {
				if (!accContext.isDoReconciliation()) {
					// no need to load
					continue;
				}

				if (accContext.getObjectOld() != null && accContext.isFullShadow()) {
					// already loaded
					continue;
				}

				if (accContext.getOid() == null) {
					throw new SystemException(
							"Request to reconcile account with null OID and without a full representation in account sync context");
				}

				PrismObject<AccountShadowType> account = provisioningService.getObject(
						AccountShadowType.class, accContext.getOid(), null, subResult);
				accContext.setObjectOld(account);
				accContext.fixShadows();
				accContext.setFullShadow(true);
			}
		} finally {
			subResult.computeStatus();
		}
}

}
