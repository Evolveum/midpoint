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
package com.evolveum.midpoint.model.lens.projector;

import static com.evolveum.midpoint.common.InternalsConfig.consistencyChecks;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensFocusContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.model.lens.SynchronizationIntent;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

/**
 * Context loader loads the missing parts of the context. The context enters the projector with just the minimum information.
 * Context loader gets missing data such as accounts. It gets them from the repository or provisioning as necessary. It follows
 * the account links in user (accountRef) and user deltas. 
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class ContextLoader {

	@Autowired(required = true)
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;
	
	@Autowired(required = true)
    private ProvisioningService provisioningService;
	
	@Autowired(required = true)
	private PrismContext prismContext;
	
	private static final Trace LOGGER = TraceManager.getTrace(ContextLoader.class);
	
	public <F extends ObjectType, P extends ObjectType> void load(LensContext<F,P> context, String activityDescription, 
			OperationResult result) 
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, 
			SecurityViolationException {
		
		context.recompute();
		
		for (LensProjectionContext<P> projectionContext: context.getProjectionContexts()) {
			preprocessProjectionContext(context, projectionContext, result);
		}
		
		if (consistencyChecks) context.checkConsistence();
		
		determineFocusContext(context, result);
				
		LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext != null) {
			loadObjectOld(context, result);
			
			if (UserType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
				LensContext<UserType,ShadowType> ucContext = (LensContext<UserType,ShadowType>) context;
				
		        loadFromSystemConfig(ucContext, result);
		        context.recomputeFocus();
		        
		        // this also removes the accountRef deltas
		        loadAccountRefs(ucContext, result);
	    	}
			
	    	// Some cleanup
	    	if (focusContext.getPrimaryDelta() != null && focusContext.getPrimaryDelta().isModify() && focusContext.getPrimaryDelta().isEmpty()) {
	    		focusContext.setPrimaryDelta(null);
	    	}
	    	
	    	for (LensProjectionContext<P> projectionContext: context.getProjectionContexts()) {
	    		if (projectionContext.getSynchronizationIntent() != null) {
	    			// Accounts with explicitly set intent are never rotten. These are explicitly requested actions
	    			// if they fail then they really should fail.
	    			projectionContext.setFresh(true);
	    		}
    		}
	    	
    	} else {
    		// Projection contexts are not rotten in this case. There is no focus so there is no way to refresh them.
    		for (LensProjectionContext<P> projectionContext: context.getProjectionContexts()) {
    			projectionContext.setFresh(true);
    		}
    	}
    	
    	context.removeRottenContexts();
    	    	
    	if (consistencyChecks) context.checkConsistence();
		
        checkProjectionContexts(context, result);
        
        if (consistencyChecks) context.checkConsistence();
        
        context.recompute();
        
        if (consistencyChecks) context.checkConsistence();
        
        LensUtil.traceContext(LOGGER, activityDescription, "load", false, context, false);

	}
	
	/**
	 * Make sure that the context is OK and consistent. It means that is has a resource, it has correctly processed
	 * discriminator, etc.
	 */
	private <F extends ObjectType, P extends ObjectType> void preprocessProjectionContext(LensContext<F,P> context, 
			LensProjectionContext<P> projectionContext, OperationResult result) 
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		if (!ShadowType.class.isAssignableFrom(projectionContext.getObjectTypeClass())) {
			return;
		}
		String resourceOid = null;
		boolean isThombstone = false;
		String intent = null;
		ResourceShadowDiscriminator rsd = projectionContext.getResourceShadowDiscriminator();
		if (rsd != null) {
			resourceOid = rsd.getResourceOid();
			isThombstone = rsd.isThombstone();
			intent = rsd.getIntent();
		}
		if (resourceOid == null && projectionContext.getObjectOld() != null) {
			resourceOid = ResourceObjectShadowUtil.getResourceOid((ShadowType) projectionContext.getObjectOld().asObjectable());
		}
		if (resourceOid == null && projectionContext.getObjectNew() != null) {
			resourceOid = ResourceObjectShadowUtil.getResourceOid((ShadowType) projectionContext.getObjectNew().asObjectable());
		}
		// We still may not have resource OID here. E.g. in case of the delete when the account is not loaded yet. It is
		// perhaps safe to skip this. It will be sorted out later.
		if (resourceOid == null) {
			return;
		}
		if (intent == null && projectionContext.getObjectNew() != null) {
			intent = ((ShadowType) projectionContext.getObjectNew().asObjectable()).getIntent();
		}
		ResourceType resource = projectionContext.getResource();
		if (resource == null) {
			resource = LensUtil.getResource(context, resourceOid, provisioningService, result);
			projectionContext.setResource(resource);
		}
		String refinedIntent = LensUtil.refineAccountType(intent, resource, prismContext);
		rsd = new ResourceShadowDiscriminator(resourceOid, refinedIntent, isThombstone);
		projectionContext.setResourceShadowDiscriminator(rsd);
	}
	
	/** 
	 * try to load focus context from the projections, e.g. by determining account owners
	 */
	public <F extends ObjectType, P extends ObjectType> void determineFocusContext(LensContext<F,P> context, 
			OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (context.getFocusContext() != null) {
			// already done
			return;
		}
		String focusOid = null;
		LensProjectionContext<P> projectionContextThatYeildedFocusOid = null;
		for (LensProjectionContext<P> projectionContext: context.getProjectionContexts()) {
			if (ShadowType.class.isAssignableFrom(projectionContext.getObjectTypeClass())) {
				LensProjectionContext<ShadowType> accountContext = (LensProjectionContext<ShadowType>)projectionContext;
				String accountOid = accountContext.getOid();
				if (accountOid != null) {
					PrismObject<UserType> accountOwner = cacheRepositoryService.listAccountShadowOwner(accountOid, result);
					if (accountOwner != null) {
						if (focusOid == null || focusOid.equals(accountOwner.getOid())) {
							focusOid = accountOwner.getOid();
							projectionContextThatYeildedFocusOid = projectionContext;
						} else {
							throw new IllegalArgumentException("The context does not have explicit focus. Attempt to determine focus failed because two " +
									"projections points to different foci: "+projectionContextThatYeildedFocusOid+"->"+focusOid+"; "+
									projectionContext+"->"+accountOwner);
						}
					}
				}
			}
		}
		if (focusOid != null) {
			// FIXME: hardcoded to user right now
			LensFocusContext<F> focusContext = context.getOrCreateFocusContext((Class<F>) UserType.class);
			PrismObject<F> object = cacheRepositoryService.getObject(focusContext.getObjectTypeClass(), focusOid, result);
	        focusContext.setObjectOld(object);
		}
	}
	
	private <F extends ObjectType, P extends ObjectType> void loadObjectOld(LensContext<F,P> context, OperationResult result) throws SchemaException, ObjectNotFoundException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			// Nothing to load
			return;
		}
		// Make sure that we RELOAD the user object if the context is not fresh
		// the user may have changed in the meantime
        if (focusContext.getObjectOld() != null && focusContext.isFresh()) {
            // already loaded
            return;
        }
        ObjectDelta<F> objectDelta = focusContext.getDelta();
        if (objectDelta != null && objectDelta.isAdd()) {
            //we're adding the focal object. No need to load it, it is in the delta
        	focusContext.setFresh(true);
            return;
        }
        if (focusContext.getObjectOld() != null && objectDelta != null && objectDelta.isDelete()) {
            // do not reload if the delta is delete. the reload will most likely fail anyway
        	// but DO NOT set the fresh flag in this case, it may be misleading
            return;
        }

        String userOid = focusContext.getOid();
        if (StringUtils.isBlank(userOid)) {
        	throw new IllegalArgumentException("No OID in primary focus delta");
        }

        PrismObject<F> object = cacheRepositoryService.getObject(focusContext.getObjectTypeClass(), userOid, result);
        focusContext.setObjectOld(object);
        focusContext.setFresh(true);
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
			ObjectReferenceType defaultUserTemplateRef = systemConfigurationType.getDefaultUserTemplateRef();
			if (defaultUserTemplateRef == null) {
				LOGGER.trace("No default user template");
			} else {
				PrismObject<UserTemplateType> defaultUserTemplate = cacheRepositoryService.getObject(UserTemplateType.class, defaultUserTemplateRef.getOid(), result);
			    context.setUserTemplate(defaultUserTemplate.asObjectable());
			}
		}
		
		if (context.getAccountSynchronizationSettings() == null) {
		    AccountSynchronizationSettingsType globalAccountSynchronizationSettings = systemConfigurationType.getGlobalAccountSynchronizationSettings();
		    LOGGER.trace("Applying globalAccountSynchronizationSettings to context: {}", globalAccountSynchronizationSettings);
		    context.setAccountSynchronizationSettings(globalAccountSynchronizationSettings);
		}
		
		if (context.getGlobalPasswordPolicy() == null){
			
			
			ValuePolicyType globalPasswordPolicy = systemConfigurationType.getGlobalPasswordPolicy();
			
			if (globalPasswordPolicy == null){
				if (systemConfigurationType.getGlobalPasswordPolicyRef() != null){
					PrismObject<ValuePolicyType> passwordPolicy = cacheRepositoryService.getObject(ValuePolicyType.class, systemConfigurationType.getGlobalPasswordPolicyRef().getOid(), result);
					if (passwordPolicy != null){
						globalPasswordPolicy = passwordPolicy.asObjectable();
					}
				}
			}
			
			context.setGlobalPasswordPolicy(globalPasswordPolicy);
		}
	}
	
	private void loadAccountRefs(LensContext<UserType,ShadowType> context, OperationResult result) throws ObjectNotFoundException,
			SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
		if (focusContext == null) {
			// Nothing to load
			return;
		}
		SynchronizationPolicyDecision policyDecision = null;
		if (focusContext.getPrimaryDelta() != null && focusContext.getPrimaryDelta().isDelete()) {
			// If user is deleted, all accounts should also be deleted
			policyDecision = SynchronizationPolicyDecision.DELETE;
		}

		PrismObject<UserType> userOld = focusContext.getObjectOld();
		if (userOld != null) {
			loadAccountRefsFromUser(context, userOld, policyDecision, result);
		}

		if (consistencyChecks) context.checkConsistence();
		
		loadAccountRefsFromDelta(context, userOld, focusContext.getPrimaryDelta(), result);
		
		if (consistencyChecks) context.checkConsistence();

		loadAccountContextsSync(context, result);
		
		if (consistencyChecks) context.checkConsistence();
	}

	/**
	 * Does not overwrite existing account contexts, just adds new ones.
	 */
	private void loadAccountRefsFromUser(LensContext<UserType,ShadowType> context, PrismObject<UserType> user,
			SynchronizationPolicyDecision policyDecision, OperationResult result) throws ObjectNotFoundException,
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
			LensProjectionContext<ShadowType> existingAccountContext = findAccountContext(oid, context);
			if (existingAccountContext != null) {
				// TODO: do we need to reload the account inside here?
				existingAccountContext.setFresh(true);
				continue;
			}
			PrismObject<ShadowType> account = accountRefVal.getObject();
			if (account == null) {
				// Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
				// We need to fetch from provisioning and not repository so the correct definition will be set.
				GetOperationOptions options = GetOperationOptions.createNoFetch();
				try {
					account = provisioningService.getObject(ShadowType.class, oid, options , result);
				} catch (ObjectNotFoundException e) {
					// Broken accountRef. We need to mark it for deletion
					LensProjectionContext<ShadowType> accountContext = getOrCreateBrokenAccountContext(context, oid);
					accountContext.setFresh(true);
					OperationResult getObjectSubresult = result.getLastSubresult();
					getObjectSubresult.setErrorsHandled();
					continue;
				}
			} else {
				// Make sure it has a proper definition. This may come from outside of the model.
				provisioningService.applyDefinition(account, result);
			}
			LensProjectionContext<ShadowType> accountContext = getOrCreateAccountContext(context, account, result);
			if (accountContext.getSynchronizationIntent() == null) {
				accountContext.setSynchronizationPolicyDecision(policyDecision);
			}
			accountContext.setFresh(true);
			if (context.isDoReconciliationForAllProjections()) {
				accountContext.setDoReconciliation(true);
			}
			if (accountContext.isDoReconciliation()) {
				// Do not load old account now. It will get loaded later in the
				// reconciliation step.				
				continue;
			}
			accountContext.setObjectOld(account);
		}
	}

	private void loadAccountRefsFromDelta(LensContext<UserType,ShadowType> context, PrismObject<UserType> user,
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
			accountRefDelta = accountRef.createDelta(new ItemPath(UserType.F_ACCOUNT_REF));
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
				LensProjectionContext<ShadowType> accountContext = null;
				PrismObject<ShadowType> account = null;
				boolean isCombinedAdd = false;
				if (oid == null) {
					// Adding new account
					account = refVal.getObject();
					if (account == null) {
						throw new SchemaException("Null or empty OID in account reference " + refVal + " in "
								+ user);
					}
					provisioningService.applyDefinition(account, result);
					if (consistencyChecks) ResourceObjectShadowUtil.checkConsistence(account, "account from "+accountRefDelta);
					// Check for conflicting change
					accountContext = LensUtil.getAccountContext(context, account, provisioningService, prismContext, result);
					if (accountContext != null) {
						// There is already existing context for the same discriminator. Tolerate this only if
						// the deltas match. It is an error otherwise.
						ObjectDelta<ShadowType> primaryDelta = accountContext.getPrimaryDelta();
						if (primaryDelta == null) {
							throw new SchemaException("Attempt to add "+account+" to a user that already contains account of type '"+
									accountContext.getResourceShadowDiscriminator().getIntent()+"' on "+accountContext.getResource());
						}
						if (!primaryDelta.isAdd()) {
							throw new SchemaException("Conflicting changes in the context. " +
									"Add of accountRef in the user delta with embedded object conflicts with explicit delta "+primaryDelta);
						}
						if (!account.equals(primaryDelta.getObjectToAdd())) {
							throw new SchemaException("Conflicting changes in the context. " +
									"Add of accountRef in the user delta with embedded object is not adding the same object as explicit delta "+primaryDelta);
						}
					} else {
						// Create account context from embedded object
						accountContext = createAccountContext(context, account, result);
					}
					// This is a new account that is to be added. So it should
					// go to account primary delta
					ObjectDelta<ShadowType> accountPrimaryDelta = account.createAddDelta();
					accountContext.setPrimaryDelta(accountPrimaryDelta);
					accountContext.setFullShadow(true);
					isCombinedAdd = true;
				} else {
					// We have OID. This is either linking of existing account or
					// add of new account
					// therefore check for account existence to decide
					try {
						// Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
						// We need to fetch from provisioning and not repository so the correct definition will be set.
						GetOperationOptions options = GetOperationOptions.createNoFetch();
						account = provisioningService.getObject(ShadowType.class, oid, options , result);
						// Create account context from retrieved object
						accountContext = getOrCreateAccountContext(context, account, result);
						accountContext.setObjectOld(account);
					} catch (ObjectNotFoundException e) {
						if (refVal.getObject() == null) {
							// account does not exist, no composite account in
							// ref -> this is really an error
							throw e;
						} else {
							// New account (with OID)
							result.muteLastSubresultError();
							account = refVal.getObject();
							if (!account.hasCompleteDefinition()) {
								provisioningService.applyDefinition(account, result);
							}
							// Create account context from embedded object
							accountContext = createAccountContext(context, account, result);
							ObjectDelta<ShadowType> accountPrimaryDelta = account.createAddDelta();
							accountContext.setPrimaryDelta(accountPrimaryDelta);
							accountContext.setFullShadow(true);
							isCombinedAdd = true;
						}
					}
				}
				if (context.isDoReconciliationForAllProjections() && !isCombinedAdd) {
					accountContext.setDoReconciliation(true);
				}
				accountContext.setFresh(true);
			}
		}
		
		if (accountRefDelta.getValuesToDelete() != null) {
			for (PrismReferenceValue refVal : accountRefDelta.getValuesToDelete()) {
				String oid = refVal.getOid();
				LensProjectionContext<ShadowType> accountContext = null;
				PrismObject<ShadowType> account = null;
				if (oid == null) {
					throw new SchemaException("Cannot delete account ref withot an oid in " + user);
				} else {
					try {
						// Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
						// We need to fetch from provisioning and not repository so the correct definition will be set.
						GetOperationOptions options = GetOperationOptions.createNoFetch();
						account = provisioningService.getObject(ShadowType.class, oid, options , result);
						// Create account context from retrieved object
						accountContext = getOrCreateAccountContext(context, account, result);
						accountContext.setObjectOld(account);
					} catch (ObjectNotFoundException e) {
						try{
						// Broken accountRef. We need to try again with raw options, because the error should be thrown becaue of non-existent resource
						GetOperationOptions options = GetOperationOptions.createRaw();
						account = provisioningService.getObject(ShadowType.class, oid, options , result);
						accountContext = getOrCreateBrokenAccountContext(context, oid);
						accountContext.setFresh(true);
						OperationResult getObjectSubresult = result.getLastSubresult();
						getObjectSubresult.setErrorsHandled();
						} catch (ObjectNotFoundException ex){
							// This is still OK. It means deleting an accountRef
							// that points to non-existing object
							// just log a warning
							LOGGER.warn("Deleting accountRef of " + user + " that points to non-existing OID "
									+ oid);
						}
						
					}
				}
				if (accountContext != null) {
					if (refVal.getObject() == null) {
						accountContext.setSynchronizationIntent(SynchronizationIntent.UNLINK);
					} else {
						accountContext.setSynchronizationIntent(SynchronizationIntent.DELETE);
						ObjectDelta<ShadowType> accountPrimaryDelta = account.createDeleteDelta();
						accountContext.setPrimaryDelta(accountPrimaryDelta);
					}
				}
				accountContext.setFresh(true);
			}
		}
		
		// remove the accountRefs without oid. These will get into the way now.
		// The accounts
		// are in the context now and will be linked at the end of the process
		// (it they survive the policy)
		// We need to make sure this happens on the real primary user delta

		if (userPrimaryDelta.getChangeType() == ChangeType.ADD) {
			userPrimaryDelta.getObjectToAdd().removeReference(UserType.F_ACCOUNT_REF);
		} else if (userPrimaryDelta.getChangeType() == ChangeType.MODIFY) {
			userPrimaryDelta.removeReferenceModification(UserType.F_ACCOUNT_REF);
		}

	}

	private void loadAccountContextsSync(LensContext<UserType,ShadowType> context, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException {
		for (LensProjectionContext<ShadowType> accountCtx : context.getProjectionContexts()) {
			if (accountCtx.isFresh() && accountCtx.getObjectOld() != null) {
				// already loaded
				continue;
			}
			ObjectDelta<ShadowType> syncDelta = accountCtx.getSyncDelta();
			if (syncDelta != null) {
				if (accountCtx.isDoReconciliation()) {
					// Do not load old account now. It will get loaded later in the
					// reconciliation step. Just mark it as fresh.
					accountCtx.setFresh(true);
					continue;
				}
				String oid = syncDelta.getOid();
				PrismObject<ShadowType> account = null;
				if (syncDelta.getChangeType() == ChangeType.ADD) {
					account = syncDelta.getObjectToAdd().clone();
					accountCtx.setObjectOld(account);
				} else {
					if (oid == null) {
						throw new IllegalArgumentException("No OID in sync delta in " + accountCtx);
					}
					// Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
					// We need to fetch from provisioning and not repository so the correct definition will be set.
					GetOperationOptions options = GetOperationOptions.createNoFetch();
					account = provisioningService.getObject(ShadowType.class, oid, options , result);
					// We will not set old account if the delta is delete. The
					// account does not really exists now.
					// (but the OID and resource will be set from the repo
					// shadow)
					if (syncDelta.getChangeType() != ChangeType.DELETE) {
						syncDelta.applyTo(account);
						accountCtx.setObjectOld(account);
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
				accountCtx.setFresh(true);
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

	private LensProjectionContext<ShadowType> getOrCreateAccountContext(LensContext<UserType,ShadowType> context,
			PrismObject<ShadowType> account, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		ShadowType accountType = account.asObjectable();
		String resourceOid = ResourceObjectShadowUtil.getResourceOid(accountType);
		if (resourceOid == null) {
			throw new SchemaException("The " + account + " has null resource reference OID");
		}
		String intent = ResourceObjectShadowUtil.getIntent(accountType);
		LensProjectionContext<ShadowType> accountSyncContext = LensUtil.getOrCreateAccountContext(context, resourceOid,
				intent, provisioningService, prismContext, result);
		accountSyncContext.setOid(account.getOid());
		return accountSyncContext;
	}
	
	private LensProjectionContext<ShadowType> createAccountContext(LensContext<UserType,ShadowType> context,
			PrismObject<ShadowType> account, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		ShadowType accountType = account.asObjectable();
		String resourceOid = ResourceObjectShadowUtil.getResourceOid(accountType);
		if (resourceOid == null) {
			throw new SchemaException("The " + account + " has null resource reference OID");
		}
		String intent = ResourceObjectShadowUtil.getIntent(accountType);
		ResourceType resource = LensUtil.getResource(context, resourceOid, provisioningService, result);
		String accountIntent = LensUtil.refineAccountType(intent, resource, prismContext);
		ResourceShadowDiscriminator rsd = new ResourceShadowDiscriminator(resourceOid, accountIntent);
		LensProjectionContext<ShadowType> accountSyncContext = context.findProjectionContext(rsd);
		if (accountSyncContext != null) {
			throw new SchemaException("Attempt to add "+account+" to a user that already contains account of type '"+accountIntent+"' on "+resource);
		}
		accountSyncContext = context.createProjectionContext(rsd);
		accountSyncContext.setResource(resource);
		accountSyncContext.setOid(account.getOid());
		return accountSyncContext;
	}

	private LensProjectionContext<ShadowType> findAccountContext(String accountOid, LensContext<UserType,ShadowType> context) {
		for (LensProjectionContext<ShadowType> accContext : context.getProjectionContexts()) {
			if (accountOid.equals(accContext.getOid())) {
				return accContext;
			}
		}

		return null;
	}
	
	private LensProjectionContext<ShadowType> getOrCreateBrokenAccountContext(LensContext<UserType,ShadowType> context,
			String brokenAccountOid) {
		LensProjectionContext<ShadowType> accountContext = context.findProjectionContextByOid(brokenAccountOid);
		if (accountContext != null) {
			if (accountContext.getSynchronizationPolicyDecision() != SynchronizationPolicyDecision.BROKEN) {
				throw new SystemException("Account context for broken account OID="+brokenAccountOid+" exists but it is not marked" +
						" as broken: "+accountContext);
			}
			return accountContext;
		}
		
		accountContext = context.createProjectionContext(null);
		accountContext.setOid(brokenAccountOid);
		accountContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
		return accountContext;
	}
	
	/**
	 * Check reconcile flag in account sync context and set accountOld
     * variable if it's not set (from provisioning), load resource (if not set already), etc.
	 */
	private <F extends ObjectType, P extends ObjectType> void checkProjectionContexts(LensContext<F,P> context, OperationResult result)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
			SecurityViolationException {

		for (LensProjectionContext<P> projContext : context.getProjectionContexts()) {
			
			if (projContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
				continue;
			}
			
			// Remember OID before the object could be wiped
			String projectionObjectOid = projContext.getOid();
			if (projContext.isDoReconciliation() && !projContext.isFullShadow()) {
				// The old object is useless here. So lets just wipe it so it will get loaded
				projContext.setObjectOld(null);
			}
			
			// Load old object
			PrismObject<P> projectionObject = projContext.getObjectOld();
			if (projContext.getObjectOld() != null) {
				projectionObject = projContext.getObjectOld();
			} else {
				if (projContext.isAdd()) {
					// No need to load old object, there is none
					projContext.recompute();
					projectionObject = projContext.getObjectNew();
				} else {
					if (projectionObjectOid == null) {
						if (projContext.getResourceShadowDiscriminator() == null || projContext.getResourceShadowDiscriminator().getResourceOid() == null) {								
							throw new SystemException(
									"Projection with null OID, no representation and no resource OID in account sync context "+projContext);
						}
					} else {
						GetOperationOptions options = null;
						if (projContext.isDoReconciliation()) {
							projContext.setFullShadow(true);
						} else {
							projContext.setFullShadow(false);
							options = GetOperationOptions.createNoFetch();
						}
						PrismObject<P> objectOld = provisioningService.getObject(
								projContext.getObjectTypeClass(), projectionObjectOid, options, result);
						projContext.setObjectOld(objectOld);
						projectionObject = objectOld;
					}
				}
			}
			
			Class<P> projClass = projContext.getObjectTypeClass();
			if (ShadowType.class.isAssignableFrom(projClass)) {
			
				// Determine Resource
				ResourceType resourceType = projContext.getResource();
				String resourceOid = null;
				if (resourceType == null) {
					if (projectionObject != null) {
						ShadowType shadowType = ((PrismObject<ShadowType>)projectionObject).asObjectable();
						resourceOid = ResourceObjectShadowUtil.getResourceOid(shadowType);
					} else if (projContext.getResourceShadowDiscriminator() != null) {
						resourceOid = projContext.getResourceShadowDiscriminator().getResourceOid();
					} else {
						throw new IllegalStateException("No shadow and no resource intent means no resource OID in "+projContext);
					}
				} else {
					resourceOid = resourceType.getOid();
				}
				
				// Determine RAT
				ResourceShadowDiscriminator discr = projContext.getResourceShadowDiscriminator();
				if (discr == null) {
					if (ShadowType.class.isAssignableFrom(projClass)) {
						ShadowType accountShadowType = ((PrismObject<ShadowType>)projectionObject).asObjectable();
						String intent = ResourceObjectShadowUtil.getIntent(accountShadowType);
						discr = new ResourceShadowDiscriminator(resourceOid, intent);
						projContext.setResourceShadowDiscriminator(discr);
						
					}
				}
				
				// Load resource
				if (resourceType == null) {
					resourceType = context.getResource(resourceOid);
					if (resourceType == null) {
						PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, resourceOid, null, result);
						resourceType = resource.asObjectable();
						context.rememberResource(resourceType);
					}
					projContext.setResource(resourceType);
				}
				
				//Determine refined schema and password policies for account type
				RefinedObjectClassDefinition rad = projContext.getRefinedAccountDefinition();
				if (rad != null && ShadowType.class.isAssignableFrom(projClass)) {
					ObjectReferenceType passwordPolicyRef = rad.getPasswordPolicy();
					if (passwordPolicyRef != null && passwordPolicyRef.getOid() != null) {
						PrismObject<ValuePolicyType> passwordPolicy = cacheRepositoryService.getObject(
								ValuePolicyType.class, passwordPolicyRef.getOid(), result);
						if (passwordPolicy != null) {
							projContext.setAccountPasswordPolicy(passwordPolicy.asObjectable());
						}
					}
				}
				
			}
		}
	}
	
}
