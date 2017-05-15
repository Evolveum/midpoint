/*
 * Copyright (c) 2010-2017 Evolveum
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

import static com.evolveum.midpoint.schema.internals.InternalsConfig.consistencyChecks;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.controller.ModelUtils;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
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
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
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
	private SystemObjectCache systemObjectCache;
	
	@Autowired(required = true)
    private ProvisioningService provisioningService;
	
	@Autowired(required = true)
	private PrismContext prismContext;
	
	private static final Trace LOGGER = TraceManager.getTrace(ContextLoader.class);
	
	public <F extends ObjectType> void load(LensContext<F> context, String activityDescription, 
			Task task, OperationResult result) 
			throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, 
			SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        context.checkAbortRequested();

		context.recompute();
		
		for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
			preprocessProjectionContext(context, projectionContext, task, result);
		}
		
		if (consistencyChecks) context.checkConsistence();
		
		determineFocusContext((LensContext<? extends FocusType>)context, result);
				
		LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext != null) {
			loadObjectCurrent(context, result);
			
			context.recomputeFocus();
	        
			loadFromSystemConfig(context, result);
	        
	    	if (FocusType.class.isAssignableFrom(context.getFocusClass())) {
		        // this also removes the accountRef deltas
		        loadLinkRefs((LensContext<? extends FocusType>)context, task, result);
				LOGGER.trace("loadLinkRefs done");
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
	    	
	    	setPrimaryDeltaOldValue(focusContext);
	    	
    	} else {
    		// Projection contexts are not rotten in this case. There is no focus so there is no way to refresh them.
    		for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
    			projectionContext.setFresh(true);
    		}
    	}
    	
    	removeRottenContexts(context);
    	    	
    	if (consistencyChecks) context.checkConsistence();
		
    	for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
            context.checkAbortRequested();
    		finishLoadOfProjectionContext(context, projectionContext, task, result);
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

				if (projectionContext.isToBeArchived()) {
					context.getHistoricResourceObjects().add(projectionContext.getResourceShadowDiscriminator());
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
																  LensProjectionContext projectionContext, Task task, OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		preprocessProjectionContext(context, projectionContext, task, result);
		finishLoadOfProjectionContext(context, projectionContext, task, result);
	}
	
	/**
	 * Make sure that the context is OK and consistent. It means that is has a resource, it has correctly processed
	 * discriminator, etc.
	 */
	private <F extends ObjectType> void preprocessProjectionContext(LensContext<F> context,
																	LensProjectionContext projectionContext, Task task, OperationResult result)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
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
				resource = LensUtil.getResourceReadOnly(context, resourceOid, provisioningService, task, result);
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
                PrismObject<F> shadowOwner = cacheRepositoryService.searchShadowOwner(projectionOid,
							SelectorOptions.createCollection(GetOperationOptions.createAllowNotFound()),
							result);
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

        // Always load a complete object here, including the not-returned-by-default properties.
        // This is temporary measure to make sure that the mappings will have all they need.
        // See MID-2635
        Collection<SelectorOptions<GetOperationOptions>> options = 
        		SelectorOptions.createCollection(GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
		PrismObject<F> object = cacheRepositoryService.getObject(focusContext.getObjectTypeClass(), userOid, options, result);
        focusContext.setLoadedObject(object);
        focusContext.setFresh(true);
		LOGGER.trace("Focal object loaded: {}", object);
    }
	
	private <O extends ObjectType> void setPrimaryDeltaOldValue(LensElementContext<O> ctx) throws SchemaException, ObjectNotFoundException {
		if (ctx.getPrimaryDelta() != null && ctx.getObjectOld() != null && ctx.isModify()) {
			PrismObject<O> objectOld = ctx.getObjectOld();
			for (ItemDelta<?,?> itemDelta: ctx.getPrimaryDelta().getModifications()) {
				LensUtil.setDeltaOldValue(ctx, itemDelta);
			}
		}
	}
	
	private <F extends ObjectType> void loadFromSystemConfig(LensContext<F> context, OperationResult result)
			throws ObjectNotFoundException, SchemaException, ConfigurationException {
		PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
		if (systemConfiguration == null) {
			// This happens in some tests. And also during first startup.
			return;
		}
		context.setSystemConfiguration(systemConfiguration);
		SystemConfigurationType systemConfigurationType = systemConfiguration.asObjectable();

        if (context.getFocusContext() != null) {
        	PrismObject<F> object = context.getFocusContext().getObjectAny();
            if (context.getFocusContext().getObjectPolicyConfigurationType() == null) {
                List<String> subTypes = FocusTypeUtil.determineSubTypes(object);
				ObjectPolicyConfigurationType policyConfigurationType =
                        ModelUtils.determineObjectPolicyConfiguration(context.getFocusContext().getObjectTypeClass(), subTypes, 
                        		systemConfigurationType);
				LOGGER.trace("Selected policy configuration: {}", policyConfigurationType);
                context.getFocusContext().setObjectPolicyConfigurationType(policyConfigurationType);
            }
        }
		
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
		
	}

    // expects that object policy configuration is already set in focusContext
	private <F extends ObjectType> PrismObject<ObjectTemplateType> determineFocusTemplate(LensContext<F> context, OperationResult result) throws ObjectNotFoundException, SchemaException, ConfigurationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			return null;
		}
		ObjectPolicyConfigurationType policyConfigurationType = focusContext.getObjectPolicyConfigurationType();
		if (policyConfigurationType == null) {
			LOGGER.trace("No default object template (no policy)");
			return null;
		}
		ObjectReferenceType templateRef = policyConfigurationType.getObjectTemplateRef();
		if (templateRef == null) {
			LOGGER.trace("No default object template (no templateRef)");
			return null;
		}
		
		PrismObject<ObjectTemplateType> template = cacheRepositoryService.getObject(ObjectTemplateType.class, templateRef.getOid(), null, result);
	    return template;
	}


	private <F extends FocusType> void loadLinkRefs(LensContext<F> context, Task task, OperationResult result) throws ObjectNotFoundException,
			SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			// Nothing to load
			return;
		}

		LOGGER.trace("loadLinkRefs starting");

		PrismObject<F> userCurrent = focusContext.getObjectCurrent();
		if (userCurrent != null) {
			loadLinkRefsFromFocus(context, userCurrent, task, result);
			LOGGER.trace("loadLinkRefsFromFocus done");
		}

		if (consistencyChecks) context.checkConsistence();
		
		loadLinkRefsFromDelta(context, userCurrent, focusContext.getPrimaryDelta(), task, result);
		LOGGER.trace("loadLinkRefsFromDelta done");
		
		if (consistencyChecks) context.checkConsistence();

		loadProjectionContextsSync(context, task, result);
		LOGGER.trace("loadProjectionContextsSync done");
		
		if (consistencyChecks) context.checkConsistence();
	}

	/**
	 * Does not overwrite existing account contexts, just adds new ones. 
	 */
	private <F extends FocusType> void loadLinkRefsFromFocus(LensContext<F> context, PrismObject<F> focus,
			Task task, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
		PrismReference linkRef = focus.findReference(FocusType.F_LINK_REF);
		if (linkRef == null) {
			return;
		}
		for (PrismReferenceValue linkRefVal : linkRef.getValues()) {
			String oid = linkRefVal.getOid();
			if (StringUtils.isBlank(oid)) {
				LOGGER.trace("Null or empty OID in link reference {} in:\n{}", linkRef,
						focus.debugDump(1));
				throw new SchemaException("Null or empty OID in link reference in " + focus);
			}
			LensProjectionContext existingAccountContext = findAccountContext(oid, context);
			
			if (!canBeLoaded(context, existingAccountContext)){
				continue;
			}
			
			if (existingAccountContext != null) {
				// TODO: do we need to reload the account inside here? yes we need
				
				existingAccountContext.setFresh(true);
				continue;
			}
			PrismObject<ShadowType> shadow = linkRefVal.getObject();
			if (shadow == null) {
				// Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
				// We need to fetch from provisioning and not repository so the correct definition will be set.
				GetOperationOptions rootOpts = GetOperationOptions.createNoFetch();
				rootOpts.setPointInTimeType(PointInTimeType.FUTURE);
				Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(rootOpts);
				LOGGER.trace("Loading shadow {} from linkRef, options={}", oid, options);
				try {
					shadow = provisioningService.getObject(ShadowType.class, oid, options, task, result);
				} catch (ObjectNotFoundException e) {
					// Broken accountRef. We need to mark it for deletion
					LensProjectionContext accountContext = getOrCreateEmptyThombstoneProjectionContext(context, oid);
					accountContext.setFresh(true);
					accountContext.setExists(false);
					OperationResult getObjectSubresult = result.getLastSubresult();
					getObjectSubresult.setErrorsHandled();
					continue;
				}
			} else {
				// Make sure it has a proper definition. This may come from outside of the model.
				provisioningService.applyDefinition(shadow, task, result);
			}
			LensProjectionContext accountContext = getOrCreateAccountContext(context, shadow, task, result);
			accountContext.setFresh(true);
			accountContext.setExists(shadow != null);
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
			ObjectDelta<F> focusPrimaryDelta, Task task, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
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
				PrismObject<ShadowType> shadow = null;
				boolean isCombinedAdd = false;
				if (oid == null) {
					// Adding new account
					shadow = refVal.getObject();
					if (shadow == null) {
						throw new SchemaException("Null or empty OID in account reference " + refVal + " in "
								+ focus);
					}
					provisioningService.applyDefinition(shadow, task, result);
					if (consistencyChecks) ShadowUtil.checkConsistence(shadow, "account from "+linkRefDelta);
					// Check for conflicting change
					accountContext = LensUtil.getProjectionContext(context, shadow, provisioningService, prismContext, task, result);
					if (accountContext != null) {
						// There is already existing context for the same discriminator. Tolerate this only if
						// the deltas match. It is an error otherwise.
						ObjectDelta<ShadowType> primaryDelta = accountContext.getPrimaryDelta();
						if (primaryDelta == null) {
							throw new SchemaException("Attempt to add "+shadow+" to a user that already contains "+
                                    accountContext.getHumanReadableKind()+" of type '"+
									accountContext.getResourceShadowDiscriminator().getIntent()+"' on "+accountContext.getResource());
						}
						if (!primaryDelta.isAdd()) {
							throw new SchemaException("Conflicting changes in the context. " +
									"Add of accountRef in the user delta with embedded object conflicts with explicit delta "+primaryDelta);
						}
						if (!shadow.equals(primaryDelta.getObjectToAdd())) {
							throw new SchemaException("Conflicting changes in the context. " +
									"Add of accountRef in the user delta with embedded object is not adding the same object as explicit delta "+primaryDelta);
						}
					} else {
						// Create account context from embedded object
						accountContext = createProjectionContext(context, shadow, task, result);
					}
					// This is a new account that is to be added. So it should
					// go to account primary delta
					ObjectDelta<ShadowType> accountPrimaryDelta = shadow.createAddDelta();
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
						GetOperationOptions rootOpts = GetOperationOptions.createNoFetch();
						rootOpts.setPointInTimeType(PointInTimeType.FUTURE);
						Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(rootOpts);
						shadow = provisioningService.getObject(ShadowType.class, oid, options, task, result);
						// Create account context from retrieved object
						accountContext = getOrCreateAccountContext(context, shadow, task, result);
						accountContext.setLoadedObject(shadow);
						accountContext.setExists(true);
					} catch (ObjectNotFoundException e) {
						if (refVal.getObject() == null) {
							// account does not exist, no composite account in
							// ref -> this is really an error
							throw e;
						} else {
							// New account (with OID)
							result.muteLastSubresultError();
							shadow = refVal.getObject();
							if (!shadow.hasCompleteDefinition()) {
								provisioningService.applyDefinition(shadow, task, result);
							}
							// Create account context from embedded object
							accountContext = createProjectionContext(context, shadow, task, result);
							ObjectDelta<ShadowType> accountPrimaryDelta = shadow.createAddDelta();
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
					throw new SchemaException("Cannot delete account ref without an oid in " + focus);
				} else {
					try {
						// Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
						// We need to fetch from provisioning and not repository so the correct definition will be set.
						Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
						account = provisioningService.getObject(ShadowType.class, oid, options, task, result);
						// Create account context from retrieved object
						accountContext = getOrCreateAccountContext(context, account, task, result);
						accountContext.setLoadedObject(account);
						accountContext.setExists(true);
					} catch (ObjectNotFoundException e) {
						try{
						// Broken accountRef. We need to try again with raw options, because the error should be thrown because of non-existent resource
						Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
						account = provisioningService.getObject(ShadowType.class, oid, options, task, result);
						accountContext = getOrCreateEmptyThombstoneProjectionContext(context, oid);
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

	private <F extends ObjectType> void loadProjectionContextsSync(LensContext<F> context, Task task, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException, ExpressionEvaluationException {
		for (LensProjectionContext projCtx : context.getProjectionContexts()) {
			if (projCtx.isFresh() && projCtx.getObjectCurrent() != null) {
				// already loaded
				continue;
			}
			ObjectDelta<ShadowType> syncDelta = projCtx.getSyncDelta();
			if (syncDelta != null) {
				if (projCtx.isDoReconciliation()) {
					// Do not load old account now. It will get loaded later in the
					// reconciliation step. Just mark it as fresh.
					projCtx.setFresh(true);
					continue;
				}
				String oid = syncDelta.getOid();
				PrismObject<ShadowType> shadow = null;
				
				if (syncDelta.getChangeType() == ChangeType.ADD) {
					shadow = syncDelta.getObjectToAdd().clone();
					projCtx.setLoadedObject(shadow);
					projCtx.setExists(true);
					
				} else {
					
					if (oid == null) {
						throw new IllegalArgumentException("No OID in sync delta in " + projCtx);
					}
					// Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
					// We need to fetch from provisioning and not repository so the correct definition will be set.
					GetOperationOptions option = GetOperationOptions.createNoFetch();
					option.setDoNotDiscovery(true);
					option.setPointInTimeType(PointInTimeType.FUTURE);
					Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(option);
					
					try {
						
						shadow = provisioningService.getObject(ShadowType.class, oid, options, task, result);
						
					} catch (ObjectNotFoundException e) {
						LOGGER.trace("Loading shadow {} from sync delta failed: not found", oid);
						projCtx.setExists(false);
						projCtx.setObjectCurrent(null);
					}
					
					// We will not set old account if the delta is delete. The
					// account does not really exists now.
					// (but the OID and resource will be set from the repo
					// shadow)
					if (syncDelta.getChangeType() == ChangeType.DELETE) {
						projCtx.setExists(false);
						projCtx.getResourceShadowDiscriminator().setThombstone(true);
					} else if (shadow != null) {
						syncDelta.applyTo(shadow);
						projCtx.setLoadedObject(shadow);
						projCtx.setExists(true);
					}
				}
				
				// Make sure OID is set correctly
				projCtx.setOid(oid);
				// Make sure that resource is also resolved
				if (projCtx.getResource() == null && shadow != null) {
					String resourceOid = ShadowUtil.getResourceOid(shadow.asObjectable());
					if (resourceOid == null) {
						throw new IllegalArgumentException("No resource OID in " + shadow);
					}
					ResourceType resourceType = LensUtil.getResourceReadOnly(context, resourceOid, provisioningService, task, result);
					projCtx.setResource(resourceType);
				}
				projCtx.setFresh(true);
			}
		}
	}

	private <F extends ObjectType> boolean canBeLoaded(LensContext<F> context, LensProjectionContext projCtx){
		if (QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_DISCOVERY).equals(context.getChannel()) && projCtx == null && ModelExecuteOptions.isLimitPropagation(context.getOptions())) {
			// avoid to create projection context if the channel which
			// triggered this operation is discovery..we need only
			// projection context of discovered shadow
			return false;
		}
		return true;
	}
	
	private <F extends FocusType> LensProjectionContext getOrCreateAccountContext(LensContext<F> context,
			PrismObject<ShadowType> projection, Task task, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
		ShadowType accountType = projection.asObjectable();
		String resourceOid = ShadowUtil.getResourceOid(accountType);
		if (resourceOid == null) {
			throw new SchemaException("The " + projection + " has null resource reference OID");
		}
		
		LensProjectionContext projectionContext = context.findProjectionContextByOid(accountType.getOid());
		
		if (projectionContext == null) {
			String intent = ShadowUtil.getIntent(accountType);
			ShadowKindType kind = ShadowUtil.getKind(accountType);
			ResourceType resource = LensUtil.getResourceReadOnly(context, resourceOid, provisioningService, task, result);
			intent = LensUtil.refineProjectionIntent(kind, intent, resource, prismContext);
			boolean thombstone = false;
			if (ShadowUtil.isDead(accountType)) {
				thombstone = true;
			}
			ResourceShadowDiscriminator rsd = new ResourceShadowDiscriminator(resourceOid, kind, intent, thombstone);
			projectionContext = LensUtil.getOrCreateProjectionContext(context, rsd);
			
			if (projectionContext.getOid() == null) {
				projectionContext.setOid(projection.getOid());
			} else if (projection.getOid() != null && !projectionContext.getOid().equals(projection.getOid())) {
				// Conflict. We have existing projection and another project that is added (with the same discriminator).
				// Chances are that the old object is already deleted (e.g. during rename). So let's be
				// slightly inefficient here and check for existing shadow existence
				try {
					GetOperationOptions rootOpt = GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE);
					rootOpt.setDoNotDiscovery(true);
					Collection<SelectorOptions<GetOperationOptions>> opts = SelectorOptions.createCollection(rootOpt);
					LOGGER.trace("Projection conflict detected, exsting: {}, new {}", projectionContext.getOid(), projection.getOid());
					PrismObject<ShadowType> existingShadow = provisioningService.getObject(ShadowType.class, projectionContext.getOid(), opts, task, result);
					// Maybe it is the other way around
					try {
						PrismObject<ShadowType> newShadow = provisioningService.getObject(ShadowType.class, projection.getOid(), opts, task, result);
						// Obviously, two projections with the same discriminator exists
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Projection {} already exists in context\nExisting:\n{}\nNew:\n{}", new Object[]{rsd, 								
									existingShadow.debugDump(1), newShadow.debugDump(1)});
						}
						throw new PolicyViolationException("Projection "+rsd+" already exists in context (existing "+existingShadow+", new "+projection);
					} catch (ObjectNotFoundException e) {
						// This is somehow expected, fix it and we can go on
						result.muteLastSubresultError();
						// We have to create new context in this case, but it has to have thumbstone set
						rsd.setThombstone(true);
						projectionContext = LensUtil.getOrCreateProjectionContext(context, rsd);
						// We have to mark it as dead right now, otherwise the uniqueness check may fail
						markShadowDead(projection.getOid(), result);
					}
				} catch (ObjectNotFoundException e) {
					// This is somehow expected, fix it and we can go on
					result.muteLastSubresultError();
					String shadowOid = projectionContext.getOid();
					projectionContext.getResourceShadowDiscriminator().setThombstone(true);
					projectionContext = LensUtil.getOrCreateProjectionContext(context, rsd);
					// We have to mark it as dead right now, otherwise the uniqueness check may fail
					markShadowDead(shadowOid, result);
				}
			}
		}
		return projectionContext;
	}
	
	private void markShadowDead(String oid, OperationResult result) {
		if (oid == null) {
			// nothing to mark
			return;
		}
		Collection<? extends ItemDelta<?, ?>> modifications = MiscSchemaUtil.createCollection(PropertyDelta.createReplaceDelta(prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class), 
				ShadowType.F_DEAD, true));
		try {
			cacheRepositoryService.modifyObject(ShadowType.class, oid, modifications, result);
			// TODO report to task?
		} catch (ObjectNotFoundException e) {
			// Done already
			result.muteLastSubresultError();
		} catch (ObjectAlreadyExistsException | SchemaException e) {
			// Should not happen
			throw new SystemException(e.getMessage(), e);
		}
	}


	private <F extends FocusType> LensProjectionContext createProjectionContext(LensContext<F> context,
																				PrismObject<ShadowType> account, Task task, OperationResult result) throws ObjectNotFoundException,
			CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		ShadowType shadowType = account.asObjectable();
		String resourceOid = ShadowUtil.getResourceOid(shadowType);
		if (resourceOid == null) {
			throw new SchemaException("The " + account + " has null resource reference OID");
		}
		String intent = ShadowUtil.getIntent(shadowType);
		ShadowKindType kind = ShadowUtil.getKind(shadowType);
		ResourceType resource = LensUtil.getResourceReadOnly(context, resourceOid, provisioningService, task, result);
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
	
	private <F extends ObjectType> LensProjectionContext getOrCreateEmptyThombstoneProjectionContext(LensContext<F> context,
			String missingShadowOid) {
		LensProjectionContext projContext = context.findProjectionContextByOid(missingShadowOid);
		if (projContext == null) {
			projContext = context.createProjectionContext(null);
			projContext.setOid(missingShadowOid);
		}
		
		if (projContext.getResourceShadowDiscriminator() == null) {
			projContext.setResourceShadowDiscriminator(new ResourceShadowDiscriminator(null, null, null, true));
		} else {
			projContext.getResourceShadowDiscriminator().setThombstone(true);
		}
		
		projContext.setFullShadow(false);
		projContext.setObjectCurrent(null);
		
		return projContext;
	}
	
	/**
	 * Check reconcile flag in account sync context and set accountOld
     * variable if it's not set (from provisioning), load resource (if not set already), etc.
	 */
	private <F extends ObjectType> void finishLoadOfProjectionContext(LensContext<F> context, 
			LensProjectionContext projContext, Task task, OperationResult result)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
			SecurityViolationException, ExpressionEvaluationException {
		
		String projectionHumanReadableName = projContext.getHumanReadableName();

		if (projContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
			return;
		}

		// MID-2436 (volatile objects) - as a quick but effective hack, we set reconciliation:=TRUE for volatile accounts
		ResourceObjectTypeDefinitionType objectDefinition = projContext.getResourceObjectTypeDefinitionType();
		if (objectDefinition != null && objectDefinition.getVolatility() == ResourceObjectVolatilityType.UNPREDICTABLE && !projContext.isDoReconciliation()) {
			LOGGER.trace("Resource object volatility is UNPREDICTABLE => setting doReconciliation to TRUE for {}", projContext.getResourceShadowDiscriminator());
			projContext.setDoReconciliation(true);
		}

		// Remember OID before the object could be wiped
		String projectionObjectOid = projContext.getOid();
		if (projContext.isDoReconciliation() && !projContext.isFullShadow()) {
			// The current object is useless here. So lets just wipe it so it will get loaded
			projContext.setObjectCurrent(null);
		}
		
		// Load current object
		boolean thombstone = false;
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
								"Projection "+projectionHumanReadableName+" with null OID, no representation and no resource OID in account sync context "+projContext);
					}
				} else {
					projContext.setExists(true);
					GetOperationOptions rootOptions = GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE);
					if (projContext.isDoReconciliation()) {
						if (SchemaConstants.CHANGE_CHANNEL_DISCOVERY_URI.equals(context.getChannel())) {
							// Avoid discovery loops
							rootOptions.setDoNotDiscovery(true);
						}
					} else { 
						rootOptions.setNoFetch(true);
					}
					rootOptions.setAllowNotFound(true);
					Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(rootOptions);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Loading shadow {} for projection {}, options={}", projectionObjectOid, projectionHumanReadableName, options);
					}
					
					try {
						PrismObject<ShadowType> objectOld = provisioningService.getObject(
								projContext.getObjectTypeClass(), projectionObjectOid, options, task, result);
						if (LOGGER.isTraceEnabled()) {
							if (!GetOperationOptions.isNoFetch(rootOptions) && !GetOperationOptions.isRaw(rootOptions)) {
								if (LOGGER.isTraceEnabled()) {
									LOGGER.trace("Full shadow loaded for {}:\n{}", projectionHumanReadableName, objectOld.debugDump(1));
								}
							}
						}
						Validate.notNull(objectOld.getOid());
						if (InternalsConfig.consistencyChecks) {
							String resourceOid = projContext.getResourceOid();
							if (resourceOid != null && !resourceOid.equals(objectOld.asObjectable().getResourceRef().getOid())) {
								throw new IllegalStateException("Loaded shadow with wrong resourceRef. Loading shadow "+projectionObjectOid+", got "+
										objectOld.getOid()+", expected resourceRef "+resourceOid+", but was "+objectOld.asObjectable().getResourceRef().getOid()+
										" for context "+projectionHumanReadableName);
							}
						}
						projContext.setLoadedObject(objectOld);
                        ShadowType oldShadow = objectOld.asObjectable();
						if (projContext.isDoReconciliation()) {
	                        projContext.determineFullShadowFlag(oldShadow.getFetchResult());
						} else {
							projContext.setFullShadow(false);
						}
						projectionObject = objectOld;
						
					} catch (ObjectNotFoundException ex) {
						// This does not mean BROKEN. The projection was there, but it gone now. What we really want here
						// is a thombstone projection.
						thombstone = true;
						projContext.setFullShadow(false);
						LOGGER.warn("Could not find object with oid {}. The projection context {} is marked as thombstone.", projectionObjectOid, projectionHumanReadableName);
						
						
					} catch (CommunicationException | SchemaException | ConfigurationException | SecurityViolationException 
			    			| RuntimeException | Error e) {
						
						LOGGER.warn("Problem while getting object with oid {}. Projection context {} is marked as broken: {}: {}", 
								projectionObjectOid, projectionHumanReadableName, e.getClass().getSimpleName(), e.getMessage());
						projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
						
						ResourceType resourceType = projContext.getResource();
						if (resourceType == null) {
							throw e;
						} else {
							ErrorSelectorType errorSelector = null;
							if (resourceType.getConsistency() != null) {
								errorSelector = resourceType.getConsistency().getConnectorErrorCriticality();
							}
							if (errorSelector == null) {
								if (e instanceof SchemaException) {
									// Just continue evaluation. The error is recorded in the result.
									// The consistency mechanism has (most likely) already done the best.
									// We cannot do any better.
									return;
								} else {
									throw e;
								}
							} else {
								if (ExceptionUtil.isSelected(errorSelector, e)) {
									throw e;
								} else {
									return;
								}
							}
						}
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
			} else if (!thombstone) {
				throw new IllegalStateException("No shadow, no discriminator and not thombstone? That won't do. Projection "+projectionHumanReadableName);
			}
		} else {
			resourceOid = resourceType.getOid();
		}
		
		// Determine discriminator
		ResourceShadowDiscriminator discr = projContext.getResourceShadowDiscriminator();
		if (discr == null) {
			if (projectionObject != null) {
				ShadowType accountShadowType = projectionObject.asObjectable();
				String intent = ShadowUtil.getIntent(accountShadowType);
				ShadowKindType kind = ShadowUtil.getKind(accountShadowType);
				discr = new ResourceShadowDiscriminator(resourceOid, kind, intent, thombstone);
			} else {
				discr = new ResourceShadowDiscriminator(null, null, null, thombstone);
			}
			projContext.setResourceShadowDiscriminator(discr);
		} else {
			if (thombstone) {
				// We do not want to reset thombstone flag if it was set before
				discr.setThombstone(thombstone);
			}
		}
		
		// Load resource
		if (resourceType == null && resourceOid != null) {
			resourceType = LensUtil.getResourceReadOnly(context, resourceOid, provisioningService, task, result);
			projContext.setResource(resourceType);
		}
		
		//Determine refined schema and password policies for account type
		RefinedObjectClassDefinition structuralObjectClassDef = projContext.getStructuralObjectClassDefinition();
		if (structuralObjectClassDef != null) {
			ObjectReferenceType passwordPolicyRef = structuralObjectClassDef.getPasswordPolicy();
			if (passwordPolicyRef != null && passwordPolicyRef.getOid() != null) {
				PrismObject<ValuePolicyType> passwordPolicy = cacheRepositoryService.getObject(
						ValuePolicyType.class, passwordPolicyRef.getOid(), null, result);
				if (passwordPolicy != null) {
					projContext.setAccountPasswordPolicy(passwordPolicy.asObjectable());
				}
			}
		}
		
		//set limitation, e.g. if this projection context should be recomputed and processed by projector
		if (ModelExecuteOptions.isLimitPropagation(context.getOptions())){
			if (context.getTriggeredResourceOid() != null){
				if (!context.getTriggeredResourceOid().equals(resourceOid)){
					projContext.setCanProject(false);
				}
			}
		}
		
		setPrimaryDeltaOldValue(projContext);
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
	
	public <F extends ObjectType> void loadFullShadow(LensContext<F> context, LensProjectionContext projCtx, Task task, OperationResult result)
			throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		if (projCtx.isFullShadow()) {
			// already loaded
			return;
		}
		if (projCtx.isAdd() && projCtx.getOid() == null) {
			// nothing to load yet
			return;
		}
		if (projCtx.isThombstone()) {
			// loading is futile
			return;
		}
		ResourceShadowDiscriminator discr = projCtx.getResourceShadowDiscriminator();
		if (discr != null && discr.getOrder() > 0) {
			// It may be just too early to load the projection
			if (LensUtil.hasLowerOrderContext(context, projCtx) && (context.getExecutionWave() < projCtx.getWave())) {
				// We cannot reliably load the context now
				return;
			}
		}
		
		GetOperationOptions getOptions = GetOperationOptions.createAllowNotFound();
		getOptions.setPointInTimeType(PointInTimeType.FUTURE);
		if (SchemaConstants.CHANGE_CHANNEL_DISCOVERY_URI.equals(context.getChannel())) {
			LOGGER.trace("Loading full resource object {} from provisioning - with doNotDiscover to avoid loops", projCtx);
			// Avoid discovery loops
			getOptions.setDoNotDiscovery(true);
		} else {
			LOGGER.trace("Loading full resource object {} from provisioning (discovery enabled), channel: {}", projCtx, context.getChannel());
		}
		try {	
			Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(getOptions);
			applyAttributesToGet(projCtx, options);
			PrismObject<ShadowType> objectCurrent = provisioningService.getObject(ShadowType.class,
					projCtx.getOid(), options, task, result);
			Validate.notNull(objectCurrent.getOid());
			// TODO: use setLoadedObject() instead?
			projCtx.setObjectCurrent(objectCurrent);
			ShadowType oldShadow = objectCurrent.asObjectable();
			projCtx.determineFullShadowFlag(oldShadow.getFetchResult());
			// The getObject may return different OID than we have requested in case that compensation happened
			// TODO: this probably need to be fixed in the consistency mechanism
			// TODO: the following line is a temporary fix
			projCtx.setOid(objectCurrent.getOid());
		
		} catch (ObjectNotFoundException ex) {
			LOGGER.trace("Load of full resource object {} ended with ObjectNotFoundException (options={})", projCtx, getOptions);
			if (projCtx.isDelete()){
				//this is OK, shadow was deleted, but we will continue in processing with old shadow..and set it as full so prevent from other full loading
				projCtx.setFullShadow(true);
			} else {
				
				boolean compensated = false;
				if (!GetOperationOptions.isDoNotDiscovery(getOptions)) {
					// The account might have been re-created by the discovery.
					// Reload focus, try to find out if there is a new matching link (and the old is gone)
					LensFocusContext<F> focusContext = context.getFocusContext();
					if (focusContext != null) {
						Class<F> focusClass = focusContext.getObjectTypeClass();
						if (FocusType.class.isAssignableFrom(focusClass)) {
							LOGGER.trace("Reloading focus to check for new links");
							PrismObject<F> focusCurrent = cacheRepositoryService.getObject(focusContext.getObjectTypeClass(), focusContext.getOid(), null, result);
							FocusType focusType = (FocusType) focusCurrent.asObjectable();
							for (ObjectReferenceType linkRef: focusType.getLinkRef()) {
								if (linkRef.getOid().equals(projCtx.getOid())) {
									// The deleted shadow is still in the linkRef. This should not happen, but it obviously happens sometimes.
									// Maybe some strange race condition? Anyway, we want a robust behavior and this linkeRef should NOT be there.
									// So simple remove it.
									LOGGER.warn("The OID "+projCtx.getOid()+" of deleted shadow still exists in the linkRef after discovery ("+focusCurrent+"), removing it");
									ReferenceDelta unlinkDelta = ReferenceDelta.createModificationDelete(
							        		FocusType.F_LINK_REF, focusContext.getObjectDefinition(), linkRef.asReferenceValue().clone());
									focusContext.swallowToSecondaryDelta(unlinkDelta);
									continue;
								}
								boolean found = false;
								for (LensProjectionContext pCtx: context.getProjectionContexts()) {
									if (linkRef.getOid().equals(pCtx.getOid())) {
										found = true;
										break;
									}
								}
								if (!found) {
									// This link is new, it is not in the existing lens context
									PrismObject<ShadowType> newLinkRepoShadow = cacheRepositoryService.getObject(ShadowType.class, linkRef.getOid(), null, result);
									if (ShadowUtil.matches(newLinkRepoShadow, projCtx.getResourceShadowDiscriminator())) {
										LOGGER.trace("Found new matching link: {}, updating projection context", newLinkRepoShadow);
										LOGGER.trace("Applying definition from provisioning first.");		// MID-3317
										provisioningService.applyDefinition(newLinkRepoShadow, task, result);
										projCtx.setObjectCurrent(newLinkRepoShadow);
										projCtx.setOid(newLinkRepoShadow.getOid());
										projCtx.recompute();
										compensated = true;
										break;
									} else {
										LOGGER.trace("Found new link: {}, but skipping it because it does not match the projection context", newLinkRepoShadow);
									}
								}
							}
						}
					}
					
				}
			
				if (!compensated) {
					LOGGER.trace("ObjectNotFound error is not compensated, setting context to thombstone");
					projCtx.getResourceShadowDiscriminator().setThombstone(true);
					projCtx.setExists(false);
					projCtx.setFullShadow(false);
				}
			}
		}
		
		projCtx.recompute();

		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Loaded full resource object:\n{}", projCtx.debugDump(1));
		}
	}

	private void applyAttributesToGet(LensProjectionContext projCtx, Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException {
		if ( !LensUtil.isPasswordReturnedByDefault(projCtx)
				&& LensUtil.needsFullShadowForCredentialProcessing(projCtx)) {
			options.add(SelectorOptions.create(SchemaConstants.PATH_PASSWORD_VALUE, GetOperationOptions.createRetrieve()));
		}
	}

	
}
