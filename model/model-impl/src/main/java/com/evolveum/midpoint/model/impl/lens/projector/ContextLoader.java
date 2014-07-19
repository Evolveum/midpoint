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
package com.evolveum.midpoint.model.impl.lens.projector;

import static com.evolveum.midpoint.common.InternalsConfig.consistencyChecks;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensObjectDeltaOperation;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.SynchronizationIntent;
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
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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
	
	public <F extends ObjectType> void load(LensContext<F> context, String activityDescription, 
			OperationResult result) 
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, 
			SecurityViolationException {
		
		context.recompute();
		
		for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
			preprocessProjectionContext(context, projectionContext, result);
		}
		
		if (consistencyChecks) context.checkConsistence();
		
		determineFocusContext((LensContext<? extends FocusType>)context, result);
				
		LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext != null) {
			loadObjectCurrent(context, result);
			
	        loadFromSystemConfig(context, result);
	        context.recomputeFocus();
	    	
	    	if (FocusType.class.isAssignableFrom(context.getFocusClass())) {
		        // this also removes the accountRef deltas
		        loadLinkRefs((LensContext<? extends FocusType>)context, result);
	    	}
	    	
	    	// Some cleanup
	    	if (focusContext.getPrimaryDelta() != null && focusContext.getPrimaryDelta().isModify() && focusContext.getPrimaryDelta().isEmpty()) {
	    		focusContext.setPrimaryDelta(null);
	    	}
			
	    	for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
	    		if (projectionContext.getSynchronizationIntent() != null) {
	    			// Accounts with explicitly set intent are never rotten. These are explicitly requested actions
	    			// if they fail then they really should fail.
	    			projectionContext.setFresh(true);
	    		}
    		}
	    	
    	} else {
    		// Projection contexts are not rotten in this case. There is no focus so there is no way to refresh them.
    		for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
    			projectionContext.setFresh(true);
    		}
    	}
    	
    	removeRottenContexts(context);
    	    	
    	if (consistencyChecks) context.checkConsistence();
		
    	for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
    		finishLoadOfProjectionContext(context, projectionContext, result);
		}
        
        if (consistencyChecks) context.checkConsistence();
        
        context.recompute();
        
        if (consistencyChecks) {
        	fullCheckConsistence(context);
        }
        
        LensUtil.traceContext(LOGGER, activityDescription, "after load", false, context, false);

	}
	

	/**
	 * Removes projection contexts that are not fresh.
	 * These are usually artifacts left after the context reload. E.g. an account that used to be linked to a user before
	 * but was removed in the meantime.
	 */
	private <F extends ObjectType> void removeRottenContexts(LensContext<F> context) {
		Iterator<LensProjectionContext> projectionIterator = context.getProjectionContextsIterator();
		while (projectionIterator.hasNext()) {
			LensProjectionContext projectionContext = projectionIterator.next();
			if (projectionContext.getPrimaryDelta() != null && !projectionContext.getPrimaryDelta().isEmpty()) {
				// We must never remove contexts with primary delta. Even though it fails later on.
				// What the user wishes should be done (or at least attempted) regardless of the consequences.
				// Vox populi vox dei
				continue;
			}
			if (projectionContext.getWave() >= context.getExecutionWave()) {
				// We must not remove context from this and later execution waves. These haven't had the
				// chance to be executed yet
				continue;
			}
			ResourceShadowDiscriminator discr = projectionContext.getResourceShadowDiscriminator();
			if (discr != null && discr.getOrder() > 0) {
				// HACK never rot higher-order context. TODO: check if lower-order context is rotten, the also rot this one
				continue;
			}
			if (!projectionContext.isFresh()) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Removing rotten context {}", projectionContext.getHumanReadableName());
				}
				List<LensObjectDeltaOperation<ShadowType>> executedDeltas = projectionContext.getExecutedDeltas();
				context.getRottenExecutedDeltas().addAll(executedDeltas);
				projectionIterator.remove();
			}
		}
	}
	
	
	/**
	 * Make sure that the projection context is loaded as approppriate.
	 */
	public <F extends ObjectType> void makeSureProjectionIsLoaded(LensContext<F> context, 
			LensProjectionContext projectionContext, OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		preprocessProjectionContext(context, projectionContext, result);
		finishLoadOfProjectionContext(context, projectionContext, result);
	}
	
	/**
	 * Make sure that the context is OK and consistent. It means that is has a resource, it has correctly processed
	 * discriminator, etc.
	 */
	private <F extends ObjectType> void preprocessProjectionContext(LensContext<F> context, 
			LensProjectionContext projectionContext, OperationResult result) 
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		if (!ShadowType.class.isAssignableFrom(projectionContext.getObjectTypeClass())) {
			return;
		}
		String resourceOid = null;
		boolean isThombstone = false;
		ShadowKindType kind = ShadowKindType.ACCOUNT;
		String intent = null;
		int order = 0;
		ResourceShadowDiscriminator rsd = projectionContext.getResourceShadowDiscriminator();
		if (rsd != null) {
			resourceOid = rsd.getResourceOid();
			isThombstone = rsd.isThombstone();
			kind = rsd.getKind();
			intent = rsd.getIntent();
			order = rsd.getOrder();
		}
		if (resourceOid == null && projectionContext.getObjectCurrent() != null) {
			resourceOid = ShadowUtil.getResourceOid((ShadowType) projectionContext.getObjectCurrent().asObjectable());
		}
		if (resourceOid == null && projectionContext.getObjectNew() != null) {
			resourceOid = ShadowUtil.getResourceOid((ShadowType) projectionContext.getObjectNew().asObjectable());
		}
		// We still may not have resource OID here. E.g. in case of the delete when the account is not loaded yet. It is
		// perhaps safe to skip this. It will be sorted out later.

		if (resourceOid != null) {
			if (intent == null && projectionContext.getObjectNew() != null) {
                ShadowType shadowNewType = projectionContext.getObjectNew().asObjectable();
                kind = ShadowUtil.getKind(shadowNewType);
                intent = ShadowUtil.getIntent(shadowNewType);
			}
			ResourceType resource = projectionContext.getResource();
			if (resource == null) {
				resource = LensUtil.getResource(context, resourceOid, provisioningService, result);
				projectionContext.setResource(resource);
			}
            String refinedIntent = LensUtil.refineProjectionIntent(kind, intent, resource, prismContext);
            rsd = new ResourceShadowDiscriminator(resourceOid, kind, refinedIntent, isThombstone);
			rsd.setOrder(order);
			projectionContext.setResourceShadowDiscriminator(rsd);
		}
		if (projectionContext.getOid() == null && rsd.getOrder() != 0) {
			// Try to determine OID from lower-order contexts
			for (LensProjectionContext aProjCtx: context.getProjectionContexts()) {
				ResourceShadowDiscriminator aDiscr = aProjCtx.getResourceShadowDiscriminator();
				if (rsd.equivalent(aDiscr) && aProjCtx.getOid() != null) {
					projectionContext.setOid(aProjCtx.getOid());
					break;
				}
			}
		}
	}
	
	/** 
	 * try to load focus context from the projections, e.g. by determining account owners
	 */
	public <F extends FocusType> void determineFocusContext(LensContext<F> context,
			OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (context.getFocusContext() != null) {
			// already done
			return;
		}
		String focusOid = null;
        PrismObject<F> focusObject = null;
		LensProjectionContext projectionContextThatYeildedFocusOid = null;
		for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
			String projectionOid = projectionContext.getOid();
			if (projectionOid != null) {
                PrismObject<F> shadowOwner = null;
                try {
				    shadowOwner = cacheRepositoryService.searchShadowOwner(projectionOid, result);
                } catch (ObjectNotFoundException e) {
                    // This may happen, e.g. if the shadow is already deleted
                    // just ignore it. pretend that it has no owner
                    result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
                }
				if (shadowOwner != null) {
					if (focusOid == null || focusOid.equals(shadowOwner.getOid())) {
						focusOid = shadowOwner.getOid();
                        focusObject = shadowOwner;
						projectionContextThatYeildedFocusOid = projectionContext;
					} else {
						throw new IllegalArgumentException("The context does not have explicit focus. Attempt to determine focus failed because two " +
								"projections points to different foci: "+projectionContextThatYeildedFocusOid+"->"+focusOid+"; "+
								projectionContext+"->"+shadowOwner);
					}
				}
			}
		}
		if (focusOid != null) {
			LensFocusContext<F> focusContext = context.getOrCreateFocusContext(focusObject.getCompileTimeClass());
			PrismObject<F> object = cacheRepositoryService.getObject(focusContext.getObjectTypeClass(), focusOid, null, result);
	        focusContext.setLoadedObject(object);
		}
	}
	
	private <F extends ObjectType> void loadObjectCurrent(LensContext<F> context, OperationResult result) throws SchemaException, ObjectNotFoundException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			// Nothing to load
			return;
		}
		// Make sure that we RELOAD the user object if the context is not fresh
		// the user may have changed in the meantime
        if (focusContext.getObjectCurrent() != null && focusContext.isFresh()) {
            // already loaded
            return;
        }
        ObjectDelta<F> objectDelta = focusContext.getDelta();
        if (objectDelta != null && objectDelta.isAdd() && focusContext.getExecutedDeltas().isEmpty()) {
            //we're adding the focal object. No need to load it, it is in the delta
        	focusContext.setFresh(true);
            return;
        }
        if (focusContext.getObjectCurrent() != null && objectDelta != null && objectDelta.isDelete()) {
            // do not reload if the delta is delete. the reload will most likely fail anyway
        	// but DO NOT set the fresh flag in this case, it may be misleading
            return;
        }

        String userOid = focusContext.getOid();
        if (StringUtils.isBlank(userOid)) {
        	throw new IllegalArgumentException("No OID in primary focus delta");
        }

        PrismObject<F> object = cacheRepositoryService.getObject(focusContext.getObjectTypeClass(), userOid, null, result);
        focusContext.setLoadedObject(object);
        focusContext.setFresh(true);
    }
	
	private <F extends ObjectType> void loadFromSystemConfig(LensContext<F> context, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ConfigurationException {
		PrismObject<SystemConfigurationType> systemConfiguration = LensUtil.getSystemConfiguration(context, cacheRepositoryService, result);
		if (systemConfiguration == null) {
			// This happens in some tests. And also during first startup.
			return;
		}
		SystemConfigurationType systemConfigurationType = systemConfiguration.asObjectable();
		
		if (context.getFocusTemplate() == null) {
			PrismObject<ObjectTemplateType> focusTemplate = determineFocusTemplate(context, result);
			if (focusTemplate != null) {
				context.setFocusTemplate(focusTemplate.asObjectable());
			}
		}
		
		if (context.getAccountSynchronizationSettings() == null) {
		    ProjectionPolicyType globalAccountSynchronizationSettings = systemConfigurationType.getGlobalAccountSynchronizationSettings();
		    LOGGER.trace("Applying globalAccountSynchronizationSettings to context: {}", globalAccountSynchronizationSettings);
		    context.setAccountSynchronizationSettings(globalAccountSynchronizationSettings);
		}
		
		if (context.getGlobalPasswordPolicy() == null){
			
			ValuePolicyType globalPasswordPolicy = systemConfigurationType.getGlobalPasswordPolicy();
			
			if (globalPasswordPolicy == null){
				if (systemConfigurationType.getGlobalPasswordPolicyRef() != null){
					PrismObject<ValuePolicyType> passwordPolicy = cacheRepositoryService.getObject(ValuePolicyType.class, systemConfigurationType.getGlobalPasswordPolicyRef().getOid(), null, result);
					if (passwordPolicy != null){
						globalPasswordPolicy = passwordPolicy.asObjectable();
					}
				}
			}
			
			context.setGlobalPasswordPolicy(globalPasswordPolicy);
		}
	}
	
	private <F extends ObjectType> PrismObject<ObjectTemplateType> determineFocusTemplate(LensContext<F> context, OperationResult result) throws ObjectNotFoundException, SchemaException, ConfigurationException {
		SystemConfigurationType systemConfigurationType = context.getSystemConfiguration().asObjectable();
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			return null;
		}
		Class<F> focusType = focusContext.getObjectTypeClass();

		ObjectReferenceType templateRef = null;
		for (ObjectTypeTemplateType policyConfigurationType: systemConfigurationType.getObjectTemplate()) {
			QName typeQName = policyConfigurationType.getType();
			ObjectTypes objectType = ObjectTypes.getObjectTypeFromTypeQName(typeQName);
			if (objectType == null) {
				throw new ConfigurationException("Unknown type "+typeQName+" in object template definition in system configuration");
			}
			if (objectType.getClassDefinition() == focusType) {
				templateRef = policyConfigurationType.getObjectTemplateRef();
				focusContext.setObjectPolicyConfigurationType(policyConfigurationType);
			}
		}
		
		// Deprecated method to specify user template. For compatibility only
		if (templateRef == null && context.getFocusClass() == UserType.class) {
			templateRef = systemConfigurationType.getDefaultUserTemplateRef();
		}
		
		if (templateRef == null) {
			LOGGER.trace("No default object template");
			return null;
		} else {
			PrismObject<ObjectTemplateType> template = cacheRepositoryService.getObject(ObjectTemplateType.class, templateRef.getOid(), null, result);
		    return template;
		}

	}


	private <F extends FocusType> void loadLinkRefs(LensContext<F> context, OperationResult result) throws ObjectNotFoundException,
			SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			// Nothing to load
			return;
		}

		PrismObject<F> userCurrent = focusContext.getObjectCurrent();
		if (userCurrent != null) {
			loadLinkRefsFromFocus(context, userCurrent, result);
		}

		if (consistencyChecks) context.checkConsistence();
		
		loadLinkRefsFromDelta(context, userCurrent, focusContext.getPrimaryDelta(), result);
		
		if (consistencyChecks) context.checkConsistence();

		loadProjectionContextsSync(context, result);
		
		if (consistencyChecks) context.checkConsistence();
	}

	/**
	 * Does not overwrite existing account contexts, just adds new ones.
	 */
	private <F extends FocusType> void loadLinkRefsFromFocus(LensContext<F> context, PrismObject<F> focus,
			OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		PrismReference linkRef = focus.findReference(FocusType.F_LINK_REF);
		if (linkRef == null) {
			return;
		}
		for (PrismReferenceValue linkRefVal : linkRef.getValues()) {
			String oid = linkRefVal.getOid();
			if (StringUtils.isBlank(oid)) {
				LOGGER.trace("Null or empty OID in link reference {} in:\n{}", linkRef,
						focus.debugDump());
				throw new SchemaException("Null or empty OID in link reference in " + focus);
			}
			LensProjectionContext existingAccountContext = findAccountContext(oid, context);
			if (existingAccountContext != null) {
				// TODO: do we need to reload the account inside here? yes we need
				
				existingAccountContext.setFresh(true);
				continue;
			}
			PrismObject<ShadowType> shadow = linkRefVal.getObject();
			if (shadow == null) {
				// Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
				// We need to fetch from provisioning and not repository so the correct definition will be set.
				Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
				try {
					shadow = provisioningService.getObject(ShadowType.class, oid, options, null, result);
				} catch (ObjectNotFoundException e) {
					// Broken accountRef. We need to mark it for deletion
					LensProjectionContext accountContext = getOrCreateBrokenAccountContext(context, oid);
					accountContext.setFresh(true);
					accountContext.setExists(false);
					OperationResult getObjectSubresult = result.getLastSubresult();
					getObjectSubresult.setErrorsHandled();
					continue;
				}
			} else {
				// Make sure it has a proper definition. This may come from outside of the model.
				provisioningService.applyDefinition(shadow, result);
			}
			LensProjectionContext accountContext = getOrCreateAccountContext(context, shadow, result);
			accountContext.setFresh(true);
			accountContext.setExists(true);
			if (context.isDoReconciliationForAllProjections()) {
				accountContext.setDoReconciliation(true);
			}
			if (accountContext.isDoReconciliation()) {
				// Do not load old account now. It will get loaded later in the
				// reconciliation step.				
				continue;
			}
			accountContext.setLoadedObject(shadow);
		}
	}

	private <F extends FocusType> void loadLinkRefsFromDelta(LensContext<F> context, PrismObject<F> focus,
			ObjectDelta<F> focusPrimaryDelta, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException {
		if (focusPrimaryDelta == null) {
			return;
		}
		
		ReferenceDelta linkRefDelta;
		if (focusPrimaryDelta.getChangeType() == ChangeType.ADD) {
			PrismReference linkRef = focusPrimaryDelta.getObjectToAdd().findReference(
					FocusType.F_LINK_REF);
			if (linkRef == null) {
				// Adding new focus with no linkRef -> nothing to do
				return;
			}
			linkRefDelta = linkRef.createDelta(new ItemPath(FocusType.F_LINK_REF));
			linkRefDelta.addValuesToAdd(PrismValue.cloneValues(linkRef.getValues()));
		} else if (focusPrimaryDelta.getChangeType() == ChangeType.MODIFY) {
			linkRefDelta = focusPrimaryDelta.findReferenceModification(FocusType.F_LINK_REF);
			if (linkRefDelta == null) {
				return;
			}
		} else {
			// delete, all existing account are already marked for delete
			return;
		}
		
		if (linkRefDelta.isReplace()) {
			// process "replace" by distributing values to delete and add
			linkRefDelta = (ReferenceDelta) linkRefDelta.clone();
			PrismReference linkRef = focus.findReference(FocusType.F_LINK_REF);
			linkRefDelta.distributeReplace(linkRef == null ? null : linkRef.getValues());
		}
		
		if (linkRefDelta.getValuesToAdd() != null) {
			for (PrismReferenceValue refVal : linkRefDelta.getValuesToAdd()) {
				String oid = refVal.getOid();
				LensProjectionContext accountContext = null;
				PrismObject<ShadowType> account = null;
				boolean isCombinedAdd = false;
				if (oid == null) {
					// Adding new account
					account = refVal.getObject();
					if (account == null) {
						throw new SchemaException("Null or empty OID in account reference " + refVal + " in "
								+ focus);
					}
					provisioningService.applyDefinition(account, result);
					if (consistencyChecks) ShadowUtil.checkConsistence(account, "account from "+linkRefDelta);
					// Check for conflicting change
					accountContext = LensUtil.getProjectionContext(context, account, provisioningService, prismContext, result);
					if (accountContext != null) {
						// There is already existing context for the same discriminator. Tolerate this only if
						// the deltas match. It is an error otherwise.
						ObjectDelta<ShadowType> primaryDelta = accountContext.getPrimaryDelta();
						if (primaryDelta == null) {
							throw new SchemaException("Attempt to add "+account+" to a user that already contains "+
                                    accountContext.getHumanReadableKind()+" of type '"+
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
						accountContext = createProjectionContext(context, account, result);
					}
					// This is a new account that is to be added. So it should
					// go to account primary delta
					ObjectDelta<ShadowType> accountPrimaryDelta = account.createAddDelta();
					accountContext.setPrimaryDelta(accountPrimaryDelta);
					accountContext.setFullShadow(true);
					accountContext.setExists(false);
					isCombinedAdd = true;
				} else {
					// We have OID. This is either linking of existing account or
					// add of new account
					// therefore check for account existence to decide
					try {
						// Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
						// We need to fetch from provisioning and not repository so the correct definition will be set.
						Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
						account = provisioningService.getObject(ShadowType.class, oid, options, null, result);
						// Create account context from retrieved object
						accountContext = getOrCreateAccountContext(context, account, result);
						accountContext.setLoadedObject(account);
						accountContext.setExists(true);
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
							accountContext = createProjectionContext(context, account, result);
							ObjectDelta<ShadowType> accountPrimaryDelta = account.createAddDelta();
							accountContext.setPrimaryDelta(accountPrimaryDelta);
							accountContext.setFullShadow(true);
							accountContext.setExists(false);
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
		
		if (linkRefDelta.getValuesToDelete() != null) {
			for (PrismReferenceValue refVal : linkRefDelta.getValuesToDelete()) {
				String oid = refVal.getOid();
				LensProjectionContext accountContext = null;
				PrismObject<ShadowType> account = null;
				if (oid == null) {
					throw new SchemaException("Cannot delete account ref withot an oid in " + focus);
				} else {
					try {
						// Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
						// We need to fetch from provisioning and not repository so the correct definition will be set.
						Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
						account = provisioningService.getObject(ShadowType.class, oid, options, null, result);
						// Create account context from retrieved object
						accountContext = getOrCreateAccountContext(context, account, result);
						accountContext.setLoadedObject(account);
						accountContext.setExists(true);
					} catch (ObjectNotFoundException e) {
						try{
						// Broken accountRef. We need to try again with raw options, because the error should be thrown becaue of non-existent resource
						Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
						account = provisioningService.getObject(ShadowType.class, oid, options, null, result);
						accountContext = getOrCreateBrokenAccountContext(context, oid);
						accountContext.setFresh(true);
						accountContext.setExists(false);
						OperationResult getObjectSubresult = result.getLastSubresult();
						getObjectSubresult.setErrorsHandled();
						} catch (ObjectNotFoundException ex){
							// This is still OK. It means deleting an accountRef
							// that points to non-existing object
							// just log a warning
							LOGGER.warn("Deleting accountRef of " + focus + " that points to non-existing OID "
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
					accountContext.setFresh(true);
				}
				
			}
		}
		
		// remove the accountRefs without oid. These will get into the way now.
		// The accounts
		// are in the context now and will be linked at the end of the process
		// (it they survive the policy)
		// We need to make sure this happens on the real primary user delta

		if (focusPrimaryDelta.getChangeType() == ChangeType.ADD) {
			focusPrimaryDelta.getObjectToAdd().removeReference(FocusType.F_LINK_REF);
		} else if (focusPrimaryDelta.getChangeType() == ChangeType.MODIFY) {
			focusPrimaryDelta.removeReferenceModification(FocusType.F_LINK_REF);
		}

	}

	private <F extends ObjectType> void loadProjectionContextsSync(LensContext<F> context, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException {
		for (LensProjectionContext accountCtx : context.getProjectionContexts()) {
			if (accountCtx.isFresh() && accountCtx.getObjectCurrent() != null) {
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
					accountCtx.setLoadedObject(account);
					accountCtx.setExists(true);
				} else {
					if (oid == null) {
						throw new IllegalArgumentException("No OID in sync delta in " + accountCtx);
					}
					// Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
					// We need to fetch from provisioning and not repository so the correct definition will be set.
					Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
					account = provisioningService.getObject(ShadowType.class, oid, options, null, result);
					// We will not set old account if the delta is delete. The
					// account does not really exists now.
					// (but the OID and resource will be set from the repo
					// shadow)
					if (syncDelta.getChangeType() == ChangeType.DELETE) {
						accountCtx.setExists(false);
					} else {
						syncDelta.applyTo(account);
						accountCtx.setLoadedObject(account);
						accountCtx.setExists(true);
					}
				}
				// Make sure OID is set correctly
				accountCtx.setOid(oid);
				// Make sure that resource is also resolved
				if (accountCtx.getResource() == null) {
					String resourceOid = ShadowUtil.getResourceOid(account.asObjectable());
					if (resourceOid == null) {
						throw new IllegalArgumentException("No resource OID in " + account);
					}
					ResourceType resourceType = provisioningService.getObject(ResourceType.class,
							resourceOid, null, null, result).asObjectable();
					context.rememberResource(resourceType);
					accountCtx.setResource(resourceType);
				}
				accountCtx.setFresh(true);
			}
		}
	}

	private <F extends FocusType> LensProjectionContext getOrCreateAccountContext(LensContext<F> context,
			PrismObject<ShadowType> account, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		ShadowType accountType = account.asObjectable();
		String resourceOid = ShadowUtil.getResourceOid(accountType);
		if (resourceOid == null) {
			throw new SchemaException("The " + account + " has null resource reference OID");
		}
		
		LensProjectionContext accountSyncContext = context.findProjectionContextByOid(accountType.getOid());
		
		if (accountSyncContext == null) {
			String intent = ShadowUtil.getIntent(accountType);
			ShadowKindType kind = ShadowUtil.getKind(accountType);
			accountSyncContext = LensUtil.getOrCreateProjectionContext(context, resourceOid, kind, intent, provisioningService,
					prismContext, result);
			accountSyncContext.setOid(account.getOid());
		}
		return accountSyncContext;
	}
	
	private <F extends FocusType> LensProjectionContext createProjectionContext(LensContext<F> context,
			PrismObject<ShadowType> account, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		ShadowType shadowType = account.asObjectable();
		String resourceOid = ShadowUtil.getResourceOid(shadowType);
		if (resourceOid == null) {
			throw new SchemaException("The " + account + " has null resource reference OID");
		}
		String intent = ShadowUtil.getIntent(shadowType);
		ShadowKindType kind = ShadowUtil.getKind(shadowType);
		ResourceType resource = LensUtil.getResource(context, resourceOid, provisioningService, result);
		String accountIntent = LensUtil.refineProjectionIntent(kind, intent, resource, prismContext);
		ResourceShadowDiscriminator rsd = new ResourceShadowDiscriminator(resourceOid, kind, accountIntent);
		LensProjectionContext accountSyncContext = context.findProjectionContext(rsd);
		if (accountSyncContext != null) {
			throw new SchemaException("Attempt to add "+account+" to a user that already contains account of type '"+accountIntent+"' on "+resource);
		}
		accountSyncContext = context.createProjectionContext(rsd);
		accountSyncContext.setResource(resource);
		accountSyncContext.setOid(account.getOid());
		return accountSyncContext;
	}

	private <F extends ObjectType> LensProjectionContext findAccountContext(String accountOid, LensContext<F> context) {
		for (LensProjectionContext accContext : context.getProjectionContexts()) {
			if (accountOid.equals(accContext.getOid())) {
				return accContext;
			}
		}

		return null;
	}
	
	private <F extends ObjectType> LensProjectionContext getOrCreateBrokenAccountContext(LensContext<F> context,
			String brokenAccountOid) {
		LensProjectionContext accountContext = context.findProjectionContextByOid(brokenAccountOid);
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
	private <F extends ObjectType> void finishLoadOfProjectionContext(LensContext<F> context, 
			LensProjectionContext projContext, OperationResult result)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
			SecurityViolationException {

		if (projContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
			return;
		}
		
		// Remember OID before the object could be wiped
		String projectionObjectOid = projContext.getOid();
		if (projContext.isDoReconciliation() && !projContext.isFullShadow()) {
			// The current object is useless here. So lets just wipe it so it will get loaded
			projContext.setObjectCurrent(null);
		}
		
		// Load current object
		PrismObject<ShadowType> projectionObject = projContext.getObjectCurrent();
		if (projContext.getObjectCurrent() == null || needToReload(context, projContext)) {
			if (projContext.isAdd()) {
				// No need to load old object, there is none
				projContext.setExists(false);
				projContext.recompute();
				projectionObject = projContext.getObjectNew();
			} else {
				if (projectionObjectOid == null) {
					projContext.setExists(false);
					if (projContext.getResourceShadowDiscriminator() == null || projContext.getResourceShadowDiscriminator().getResourceOid() == null) {								
						throw new SystemException(
								"Projection with null OID, no representation and no resource OID in account sync context "+projContext);
					}
				} else {
					projContext.setExists(true);
					GetOperationOptions rootOptions = projContext.isDoReconciliation() ? 
							GetOperationOptions.createDoNotDiscovery() : GetOperationOptions.createNoFetch();
					Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(rootOptions);
					try{
						PrismObject<ShadowType> objectOld = provisioningService.getObject(
								projContext.getObjectTypeClass(), projectionObjectOid, options, null, result);
						projContext.setLoadedObject(objectOld);
                        ShadowType oldShadow = objectOld.asObjectable();
						if (projContext.isDoReconciliation()) {
	                        projContext.determineFullShadowFlag(oldShadow.getFetchResult());
						} else {
							projContext.setFullShadow(false);
						}
						projectionObject = objectOld;
					} catch (ObjectNotFoundException ex){
						projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
						LOGGER.warn("Could not find object with oid " + projectionObjectOid + ". Context for these object is marked as broken");
						return;
					} catch (SchemaException ex){
						projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
						LOGGER.warn("Schema problem while getting object with oid " + projectionObjectOid + ". Context for these object is marked as broken");
						return;
					}
				}
				projContext.setFresh(true);
			}
		} else {
			projectionObject = projContext.getObjectCurrent();
			if (projectionObjectOid != null) {
				projContext.setExists(true);
			}
		}
		
	
		// Determine Resource
		ResourceType resourceType = projContext.getResource();
		String resourceOid = null;
		if (resourceType == null) {
			if (projectionObject != null) {
				ShadowType shadowType = projectionObject.asObjectable();
				resourceOid = ShadowUtil.getResourceOid(shadowType);
			} else if (projContext.getResourceShadowDiscriminator() != null) {
				resourceOid = projContext.getResourceShadowDiscriminator().getResourceOid();
			} else {
				throw new IllegalStateException("No shadow and no resource intent means no resource OID in "+projContext);
			}
		} else {
			resourceOid = resourceType.getOid();
		}
		
		// Determine discriminator
		ResourceShadowDiscriminator discr = projContext.getResourceShadowDiscriminator();
		if (discr == null) {
			ShadowType accountShadowType = projectionObject.asObjectable();
			String intent = ShadowUtil.getIntent(accountShadowType);
			ShadowKindType kind = ShadowUtil.getKind(accountShadowType);
			discr = new ResourceShadowDiscriminator(resourceOid, kind, intent);
			projContext.setResourceShadowDiscriminator(discr);
		}
		
		// Load resource
		if (resourceType == null) {
			resourceType = context.getResource(resourceOid);
			if (resourceType == null) {
				PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, resourceOid, null, null, result);
				resourceType = resource.asObjectable();
				context.rememberResource(resourceType);
			}
			projContext.setResource(resourceType);
		}
		
		//Determine refined schema and password policies for account type
		RefinedObjectClassDefinition rad = projContext.getRefinedAccountDefinition();
		if (rad != null) {
			ObjectReferenceType passwordPolicyRef = rad.getPasswordPolicy();
			if (passwordPolicyRef != null && passwordPolicyRef.getOid() != null) {
				PrismObject<ValuePolicyType> passwordPolicy = cacheRepositoryService.getObject(
						ValuePolicyType.class, passwordPolicyRef.getOid(), null, result);
				if (passwordPolicy != null) {
					projContext.setAccountPasswordPolicy(passwordPolicy.asObjectable());
				}
			}
		}
		
	}
	
	private <F extends ObjectType> boolean needToReload(LensContext<F> context,
			LensProjectionContext projContext) {
		ResourceShadowDiscriminator discr = projContext.getResourceShadowDiscriminator();
		if (discr == null) {
			return false;
		}
		// This is kind of brutal. But effective. We are reloading all higher-order dependencies
		// before they are processed. This makes sure we have fresh state when they are re-computed.
		// Because higher-order dependencies may have more than one projection context and the
		// changes applied to one of them are not automatically reflected on on other. therefore we need to reload.
		if (discr.getOrder() == 0) {
			return false;
		}
		int executionWave = context.getExecutionWave();
		int projCtxWave = projContext.getWave();
		if (executionWave == projCtxWave - 1) {
			// Reload right before its execution wave
			return true;
		}
		return false;
	}
	
	private <F extends ObjectType> void fullCheckConsistence(LensContext<F> context) {
		context.checkConsistence();
		for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
			if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
				continue;
			}
			if (projectionContext.getResourceShadowDiscriminator() == null) {
				throw new IllegalStateException("No discriminator in "+projectionContext);
			}
		}
	}
	
}
