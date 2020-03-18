/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ShadowCache;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.shadowmanager.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 *  This is the common "engine" that processes changes coming from live synchronization, asynchronous updates,
 *  and model.notifyChange calls. All the auxiliary pre-processing (finding old shadow, determining object class,
 *  etc) is placed here.
 *
 *  This is the final stage of processing, executing given change in a single thread. It is called
 *  from ChangeProcessingCoordinator, either in the caller thread (in single-threaded scenarios) or from worker threads/LATs
 *  (in multi-threaded scenarios).
 */
@Component
public class ChangeProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(ChangeProcessor.class);

    private static final String OP_PROCESS_SYNCHRONIZATION = ChangeProcessor.class.getName() + ".processSynchronization";

    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired private ShadowManager shadowManager;
    @Autowired private ShadowCache shadowCache;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private SchemaHelper schemaHelper;

    /**
     * Executes the request.
     *
     * @param workerTask Task in context of which the worker task is executing.
     * @param coordinatorTask Coordinator task. Might be null.
     */
    public void execute(ProcessChangeRequest request, Task workerTask, Task coordinatorTask,
            TaskPartitionDefinitionType partition, OperationResult parentResult) {

        ProvisioningContext globalCtx = request.getGlobalContext();
        Change change = request.getChange();

        long started = 0;
        String objectName = null;
        String objectDisplayName = null;
        String objectOid = null;

        OperationResult result = parentResult.createMinorSubresult(OP_PROCESS_SYNCHRONIZATION);
        try {

            ProvisioningContext ctx = determineProvisioningContext(globalCtx, change, workerTask, result);
            if (ctx == null) {
                request.setSuccess(true);
                request.onSuccess();
                return;
            }

            // This is a bit of hack to propagate information about async update channel to upper layers
            // e.g. to implement MID-5853. In async update scenario the task here is the newly-created
            // that is used to execute the request. On the other hand, the task in globalCtx is the original
            // one that was used to start listening for changes. TODO This is to be cleaned up.
            // But for the time being let's forward with this hack.
            if (SchemaConstants.CHANGE_CHANNEL_ASYNC_UPDATE_URI.equals(workerTask.getChannel())) {
                ctx.setChannelOverride(SchemaConstants.CHANGE_CHANNEL_ASYNC_UPDATE_URI);
            }

            if (change.getObjectDelta() != null) {
                shadowCaretaker.applyAttributesDefinition(ctx, change.getObjectDelta());
            }
            if (change.getOldRepoShadow() != null) {
                shadowCaretaker.applyAttributesDefinition(ctx, change.getOldRepoShadow());
            }
            if (change.getCurrentResourceObject() != null) {
                shadowCaretaker.applyAttributesDefinition(ctx, change.getCurrentResourceObject());  // maybe redundant
            }

            started = System.currentTimeMillis();

            preProcessChange(ctx, change, result);

            if (change.getCurrentResourceObject() != null && change.getCurrentResourceObject().getName() != null) {
                objectName = change.getCurrentResourceObject().getName().getOrig();
                objectDisplayName = StatisticsUtil.getDisplayName(change.getCurrentResourceObject().asObjectable());
                objectOid = change.getCurrentResourceObject().getOid();     // probably null
            } else if (change.getOldRepoShadow() != null && change.getOldRepoShadow().getName() != null) {
                objectName = change.getOldRepoShadow().getName().getOrig();
                objectDisplayName = StatisticsUtil.getDisplayName(change.getOldRepoShadow().asObjectable());
                objectOid = change.getOldRepoShadow().getOid();
            } else {
                objectName = String.valueOf(change.getPrimaryIdentifierRealValue());
                objectDisplayName = null;
                objectOid = null;
            }

            // We should record iterative operation start earlier (before preProcessChange). But at that time we do not have
            // most of this information. TODO: is oldRepoShadow updated now?
            workerTask.recordIterativeOperationStart(objectName, objectDisplayName, ShadowType.COMPLEX_TYPE, objectOid);

            // This is the case when we want to skip processing of change because the shadow was not found nor created.
            // The usual reason is that this is the delete delta and the object was already deleted on both resource and in repo.
            if (change.getOldRepoShadow() == null) {
                assert change.isDelete();
                LOGGER.debug("Skipping processing change. Can't find appropriate shadow (e.g. the object was "
                        + "deleted on the resource meantime).");
                request.setSuccess(true);
                request.onSuccess();
                // Are we OK with the result being automatically computed here? (i.e. most probably SUCCESS?)
                return;
            }

            ResourceObjectShadowChangeDescription shadowChangeDescription = createResourceShadowChangeDescription(
                    change, ctx.getResource(), ctx.getChannel());
            shadowChangeDescription.setSimulate(request.isSimulate());

            LOGGER.trace("Created resource object shadow change description {}",
                    SchemaDebugUtil.prettyPrintLazily(shadowChangeDescription));
            OperationResult notifyChangeResult = result.createMinorSubresult(ShadowCache.class.getName() + ".notifyChange");
            notifyChangeResult.addParam("resourceObjectShadowChangeDescription", shadowChangeDescription.toString());

            try {
                shadowCache.notifyResourceObjectChangeListeners(shadowChangeDescription, ctx.getTask(), notifyChangeResult);
                notifyChangeResult.computeStatusIfUnknown();
            } catch (RuntimeException ex) {
                notifyChangeResult.recordFatalError(ex.getMessage(), ex);
                throw new SystemException("Synchronization error: " + ex.getMessage(), ex);
            } catch (Throwable t) {
                notifyChangeResult.recordFatalError(t.getMessage(), t);
            }

            notifyChangeResult.computeStatus("Error in notify change operation.");

            if (notifyChangeResult.isSuccess() || notifyChangeResult.isHandledError() || notifyChangeResult.isNotApplicable()) {
                // Do not delete dead shadows. Keep dead shadow around because they contain results
                // of the synchronization. Usual shadow refresh process should delete them eventually.
                // TODO: review. Maybe make this configuration later on.
                // But in that case model (SynchronizationService) needs to know whether shadow is
                // going to be deleted or whether it stays. Model needs to adjust links accordingly.
                // And we need to modify ResourceObjectChangeListener for that. Keeping all dead
                // shadows is much easier.
                //                deleteShadowFromRepoIfNeeded(change, result);
                request.setSuccess(true);
            } else {
                request.setSuccess(false);
            }

            try {
                validateResult(notifyChangeResult, workerTask, partition);
            } catch (PreconditionViolationException e) {
                throw new SystemException(e.getMessage(), e);
            }

            if (result.isUnknown()) {
                result.computeStatus();
            }

            if (request.isSuccess()) {
                request.onSuccess();
            } else {
                request.onError(result);
            }
            workerTask.recordIterativeOperationEnd(objectName, objectDisplayName, ShadowType.COMPLEX_TYPE, objectOid, started, null);

        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException | CommunicationException |
                ConfigurationException | ExpressionEvaluationException | SecurityViolationException | EncryptionException |
                PolicyViolationException | RuntimeException | Error e) {
            result.recordFatalError(e);
            request.setSuccess(false);
            request.onError(e, result);
            if (started > 0) {
                workerTask.recordIterativeOperationEnd(objectName, objectDisplayName, ShadowType.COMPLEX_TYPE, objectOid, started, e);
            }
        } finally {
            request.setDone(true);
            result.computeStatusIfUnknown();
            result.cleanupResult();
            request.onCompletion(workerTask, coordinatorTask, result);
        }
    }

    /**
     * Returns null if the context cannot be determined because the change is DELETE and there's no shadow in repo.
     *
     * As a side effect it may fill-in change.oldShadow.
     */
    @Nullable
    private ProvisioningContext determineProvisioningContext(ProvisioningContext globalCtx, Change change,
            Task workerTask, OperationResult parentResult)
            throws SchemaException, CommunicationException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, EncryptionException {
        if (change.getObjectClassDefinition() == null) {
            if (!change.isDelete()) {
                throw new SchemaException("No object class definition in change " + change);
            } else {
                // A specialty for DELETE changes: we try to determine the object class if not specified in the change
                // It is needed e.g. for some LDAP servers doing live synchronization.
                if (change.getOldRepoShadow() == null) {
                    PrismObject<ShadowType> existing = shadowManager.findOrAddShadowFromChange(globalCtx, change, parentResult);
                    if (existing == null) {
                        LOGGER.debug("No old shadow for delete synchronization event {}, we probably did not know about "
                                + "that object anyway, so well be ignoring this event", change);
                        return null;
                    } else {
                        change.setOldRepoShadow(existing);
                    }
                }
                return globalCtx.spawn(change.getOldRepoShadow(), workerTask);
            }
        } else {
            return globalCtx.spawn(change.getObjectClassDefinition().getTypeName(), workerTask);
        }
    }

    /**
     * Manages repository representation of the shadow affected by the change.
     * Polishes the change itself.
     *
     * Does no synchronization.
     */
    private void preProcessChange(ProvisioningContext ctx, Change change, OperationResult parentResult)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ObjectNotFoundException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

        if (change.getOldRepoShadow() == null) {
            PrismObject<ShadowType> repoShadow = shadowManager.findOrAddShadowFromChange(ctx, change, parentResult);
            if (repoShadow != null) {
                shadowCaretaker.applyAttributesDefinition(ctx, repoShadow);
                change.setOldRepoShadow(repoShadow);
            } else {
                assert change.isDelete();
                LOGGER.debug("No old shadow for synchronization event {}, the shadow must be gone in the meantime "
                        + "(this is probably harmless)", change);
                return;
            }
        }

        @NotNull PrismObject<ShadowType> oldShadow = change.getOldRepoShadow();
        LOGGER.trace("Processing change, old shadow: {}", ShadowUtil.shortDumpShadow(oldShadow));

        ProvisioningUtil.setProtectedFlag(ctx, oldShadow, matchingRuleRegistry, relationRegistry);

        if (change.getCurrentResourceObject() == null && !change.isDelete()) {
            LOGGER.trace("Going to compute current resource object because it's null and delta is not delete");
            // Temporary measure: let us determine the current resource object; either by fetching it from the resource
            // (if possible) or by taking cached values and applying the delta. In the future we might implement
            // pure delta changes that do not need to know the current state.
            if (change.isAdd()) {
                change.setCurrentResourceObject(change.getObjectDelta().getObjectToAdd().clone());
                LOGGER.trace("-> current object was taken from ADD delta:\n{}", change.getCurrentResourceObject().debugDumpLazily());
            } else {
                boolean passiveCaching = ctx.getCachingStrategy() == CachingStategyType.PASSIVE;
                ReadCapabilityType readCapability = ctx.getEffectiveCapability(ReadCapabilityType.class);
                boolean canReadFromResource = readCapability != null && !Boolean.TRUE.equals(readCapability.isCachingOnly());
                if (canReadFromResource && (!passiveCaching || change.isNotificationOnly())) {
                    // Either we don't use caching or we have a notification-only change. Such changes mean that we want to
                    // refresh the object from the resource.
                    Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
                            .doNotDiscovery().build();
                    PrismObject<ShadowType> resourceObject;
                    try {
                        resourceObject = shadowCache.getShadow(oldShadow.getOid(), oldShadow, change.getIdentifiers(), options,
                                ctx.getTask(), parentResult);
                    } catch (ObjectNotFoundException e) {
                        // The object on the resource does not exist (any more?).
                        LOGGER.warn("Object {} does not exist on the resource any more", oldShadow);
                        throw e;            // TODO
                    }
                    LOGGER.trace("-> current object was taken from the resource:\n{}", resourceObject.debugDumpLazily());
                    change.setCurrentResourceObject(resourceObject);
                } else if (passiveCaching) {
                    PrismObject<ShadowType> resourceObject = oldShadow.clone();     // this might not be correct w.r.t. index-only attributes!
                    if (change.getObjectDelta() != null) {
                        change.getObjectDelta().applyTo(resourceObject);
                        markIndexOnlyItemsAsIncomplete(resourceObject, change.getObjectDelta(), ctx);
                        LOGGER.trace("-> current object was taken from old shadow + delta:\n{}", resourceObject.debugDumpLazily());
                    } else {
                        LOGGER.trace("-> current object was taken from old shadow:\n{}", resourceObject.debugDumpLazily());
                    }
                    change.setCurrentResourceObject(resourceObject);
                } else {
                    throw new IllegalStateException("Cannot get current resource object: read capability is not present and passive caching is not configured");
                }
            }
        }
        assert change.getCurrentResourceObject() != null || change.isDelete();
        if (change.getCurrentResourceObject() != null) {
            // TODO do we need to complete the shadow now? Why? MID-5834
            PrismObject<ShadowType> currentResourceObjectShadowized = shadowCache.completeShadow(ctx, change.getCurrentResourceObject(), oldShadow, false, parentResult);
            change.setCurrentResourceObject(currentResourceObjectShadowized);
            // TODO: shadowState MID-5834
            shadowManager.updateShadow(ctx, currentResourceObjectShadowized, change.getObjectDelta(), oldShadow, null, parentResult);
        }

        if (change.getObjectDelta() != null && change.getObjectDelta().getOid() == null) {
            change.getObjectDelta().setOid(oldShadow.getOid());
        }

        if (change.isDelete()) {
            PrismObject<ShadowType> currentShadow = change.getCurrentResourceObject();
            if (currentShadow == null) {
                currentShadow = oldShadow.clone();
                change.setCurrentResourceObject(currentShadow);         // todo why is this? MID-5834
            }
            if (!ShadowUtil.isDead(currentShadow) || ShadowUtil.isExists(currentShadow)) {
                shadowManager.markShadowTombstone(currentShadow, parentResult);
            }
        }
    }

    /**
     * Index-only items in the resource object delta are necessarily incomplete: their old value was taken from repo
     * (i.e. was empty before delta application). We mark them as such. One of direct consequences is that updateShadow method
     * will know that it cannot use this data to update cached (index-only) attributes in repo shadow.
     */
    private void markIndexOnlyItemsAsIncomplete(PrismObject<ShadowType> resourceObject,
            ObjectDelta<ShadowType> resourceObjectDelta, ProvisioningContext ctx)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        RefinedObjectClassDefinition ocDef = ctx.computeCompositeObjectClassDefinition(resourceObject);
        for (RefinedAttributeDefinition<?> attrDef : ocDef.getAttributeDefinitions()) {
            if (attrDef.isIndexOnly()) {
                ItemPath path = ItemPath.create(ShadowType.F_ATTRIBUTES, attrDef.getItemName());
                LOGGER.trace("Marking item {} as incomplete because it's index-only", path);
                //noinspection unchecked
                resourceObject.findCreateItem(path, Item.class, attrDef, true).setIncomplete(true);
            }
        }
    }

    private ResourceObjectShadowChangeDescription createResourceShadowChangeDescription(
            Change change, ResourceType resourceType, String channel) {
        ResourceObjectShadowChangeDescription shadowChangeDescription = new ResourceObjectShadowChangeDescription();
        shadowChangeDescription.setObjectDelta(change.getObjectDelta());
        shadowChangeDescription.setResource(resourceType.asPrismObject());
        shadowChangeDescription.setOldShadow(change.getOldRepoShadow());
        shadowChangeDescription.setCurrentShadow(change.getCurrentResourceObject());
        shadowChangeDescription.setSourceChannel(channel != null ? channel : SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC_URI);
        return shadowChangeDescription;
    }

    private void validateResult(OperationResult result, Task task, TaskPartitionDefinitionType partition)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException,
            PreconditionViolationException {

        if (result.isSuccess() || result.isHandledError() || result.isNotApplicable()) {
            return;
        }

        Throwable ex = RepoCommonUtils.getResultException(result);
        ErrorSelectorType selector = partition != null ? partition.getErrorCriticality() : null;
        CriticalityType criticality = ExceptionUtil.getCriticality(selector, ex, CriticalityType.PARTIAL);
        RepoCommonUtils.processErrorCriticality(task, criticality, ex, result);
    }
}
