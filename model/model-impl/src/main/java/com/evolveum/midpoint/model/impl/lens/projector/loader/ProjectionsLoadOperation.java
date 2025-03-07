/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.loader;

import static com.evolveum.midpoint.model.api.context.ProjectionContextKey.missing;
import static com.evolveum.midpoint.model.impl.lens.LensContext.getOrCreateProjectionContext;
import static com.evolveum.midpoint.schema.internals.InternalsConfig.consistencyChecks;
import static com.evolveum.midpoint.util.MiscUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.context.ProjectionContextFilter;
import com.evolveum.midpoint.model.api.context.ProjectionContextKey;

import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.LensContext.GetOrCreateProjectionContextResult;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.context.SynchronizationIntent;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Responsible for the acquisition of all projections for a focus. See {@link #load(OperationResult)} method.
 *
 * Note: full resource objects are not loaded in this class (now), except for:
 *
 * 1. when they need to be classified - see {@link ProjectionContextKeyFactoryImpl#createKey(ShadowType, Task, OperationResult)},
 * 2. when a conflict needs to be resolved - see {@link ShadowLevelLoadOperation#treatContextConflict(ShadowType,
 * ProjectionContextKey, LensProjectionContext, OperationResult)}
 *
 * For full shadow loading, see {@link ProjectionUpdateOperation} (for reconciliation)
 * and {@link ProjectionFullLoadOperation} (for ad-hoc full shadow loading).
 *
 * *BEWARE*: Removes linkRef modifications from the primary delta, if there were any. See {@link #removeLinkRefModifications()}.
 *
 * TODO better name for the class?
 */
public class ProjectionsLoadOperation<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(ProjectionsLoadOperation.class);

    private static final String OP_LOAD = ProjectionsLoadOperation.class.getName() + "." + "load";

    @NotNull private final LensContext<F> context;
    @NotNull private final LensFocusContext<F> focusContext;
    @NotNull private final Task task;
    @NotNull private final ModelBeans beans;
    @NotNull private final ProvisioningService provisioningService;

    ProjectionsLoadOperation(@NotNull LensContext<F> context, @NotNull Task task) {
        this.context = context;
        this.focusContext = context.getFocusContext();
        this.task = task;
        this.beans = ModelBeans.get();
        this.provisioningService = beans.provisioningService;
    }

    public void load(OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException {

        OperationResult result = parentResult.subresult(OP_LOAD)
                .setMinor()
                .build();

        LOGGER.trace("Projections (shadows) loading starting: {} projection contexts at start",
                context.getProjectionContexts().size());
        try {

            getOrCreateProjectionContextsFromFocusLinkRefs(result);
            getOrCreateProjectionContextsFromFocusPrimaryDelta(result);
            updateContextsFromSyncDeltas(result);

            context.checkConsistenceIfNeeded();
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
        LOGGER.trace("Projections loading done: {} projection contexts at end", context.getProjectionContexts().size());
    }

    /**
     * Loads projections from focus.linkRef values.
     *
     * Does not overwrite existing projection contexts, just adds new ones if needed.
     */
    private void getOrCreateProjectionContextsFromFocusLinkRefs(OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        LOGGER.trace("Loading projection contexts from focus linkRefs starting");

        PrismObject<F> focus = focusContext.getObjectCurrent();
        List<ObjectReferenceType> linkRefs = focus != null ? focus.asObjectable().getLinkRef() : List.of();

        for (ObjectReferenceType linkRef : linkRefs) {
            new LinkLevelLoadOperation(linkRef)
                    .getOrCreateFromExistingValue(result);
        }

        context.checkConsistenceIfNeeded();

        LOGGER.trace("Loading projection contexts from focus linkRefs done ({} linkRefs considered)", linkRefs.size());
    }

    private void getOrCreateProjectionContextsFromFocusPrimaryDelta(OperationResult result) throws SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException {

        LOGGER.trace("Loading projection contexts from focus primary delta starting");

        ObjectDelta<F> focusPrimaryDelta = focusContext.getPrimaryDelta();
        if (focusPrimaryDelta != null) {
            ReferenceDelta linkRefDelta = getLinkRefDelta(focusPrimaryDelta);
            LOGGER.trace("linkRef delta: {}", linkRefDelta);
            if (linkRefDelta != null) {
                if (linkRefDelta.isReplace()) {
                    linkRefDelta = distributeLinkRefReplace(linkRefDelta);
                }

                getOrCreateContextsForValuesToAdd(linkRefDelta.getValuesToAdd(), result);
                getOrCreateContextsForValuesToDelete(linkRefDelta.getValuesToDelete(), result);

                removeLinkRefModifications();
            }
        } else {
            LOGGER.trace("(no focus primary delta)");
        }

        LOGGER.trace("Loading projection contexts from focus primary delta done");
    }

    private @Nullable ReferenceDelta getLinkRefDelta(ObjectDelta<F> focusPrimaryDelta) {
        if (focusPrimaryDelta.getChangeType() == ChangeType.ADD) {
            PrismReference linkRef = focusPrimaryDelta.getObjectToAdd().findReference(FocusType.F_LINK_REF);
            if (linkRef == null) {
                // Adding new focus with no linkRef -> nothing to do
                return null;
            } else {
                ReferenceDelta linkRefDelta = linkRef.createDelta(FocusType.F_LINK_REF);
                linkRefDelta.addValuesToAdd(PrismValueCollectionsUtil.cloneValues(linkRef.getValues()));
                return linkRefDelta;
            }
        } else if (focusPrimaryDelta.getChangeType() == ChangeType.MODIFY) {
            return focusPrimaryDelta.findReferenceModification(FocusType.F_LINK_REF);
        } else {
            // delete, all existing account are already marked for delete
            return null;
        }
    }

    private @NotNull ReferenceDelta distributeLinkRefReplace(ReferenceDelta linkRefDelta) {
        PrismObject<F> focus = focusContext.getObjectCurrent();
        // process "replace" by distributing values to delete and add
        ReferenceDelta linkRefDeltaClone = linkRefDelta.clone();
        PrismReference linkRef = focus.findReference(FocusType.F_LINK_REF);
        linkRefDeltaClone.distributeReplace(linkRef != null ? linkRef.getValues() : null);
        return linkRefDeltaClone;
    }

    /**
     * Remove the linkRef modifications. These will get into the way now.
     * The accounts are in the context now and will be linked at the end of the process
     * (it they survive the policy)
     * We need to make sure this happens on the real primary focus delta.
     */
    private void removeLinkRefModifications() throws SchemaException {
        focusContext.modifyPrimaryDelta(delta -> {
            if (delta.getChangeType() == ChangeType.ADD) {
                delta.getObjectToAdd().removeReference(FocusType.F_LINK_REF);
            } else if (delta.getChangeType() == ChangeType.MODIFY) {
                delta.removeReferenceModification(FocusType.F_LINK_REF);
            }
        });
    }

    private void getOrCreateContextsForValuesToAdd(
            @Nullable Collection<PrismReferenceValue> valuesToAdd, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException, PolicyViolationException {
        for (PrismReferenceValue refVal : emptyIfNull(valuesToAdd)) {
            if (isInactive(refVal.asReferencable())) {
                LOGGER.trace("getOrCreateContextsForValuesToAdd: Skipping inactive linkRef to add (relation={}): {}",
                        refVal.getRelation(), refVal);
            } else {
                LOGGER.trace("getOrCreateContextsForValuesToAdd: Processing value to add: {}", refVal);
                new LinkLevelLoadOperation(refVal.asReferencable())
                        .getOrCreateForValueToAdd(result);
            }
        }
    }

    private boolean isInactive(Referencable linkRef) {
        return !SchemaService.get().relationRegistry().isMember(linkRef.getRelation());
    }

    private void getOrCreateContextsForValuesToDelete(
            @Nullable Collection<PrismReferenceValue> valuesToDelete, @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PolicyViolationException,
            ObjectNotFoundException, ObjectAlreadyExistsException {
        var inactiveLinksToDelete = new ArrayList<PrismReferenceValue>();
        for (var refVal : emptyIfNull(valuesToDelete)) {
            if (isInactive(refVal.asReferencable())) {
                inactiveLinksToDelete.add(refVal);
            } else {
                LOGGER.trace("getOrCreateContextsForValuesToDelete: Processing value to delete: {}", refVal);
                new LinkLevelLoadOperation(refVal.asReferencable())
                        .getOrCreateForValueToDelete(result);
            }
        }
        processInactiveLinkRefDeletion(inactiveLinksToDelete, result);
    }

    /**
     * Inactive linkRefs (pointing to dead shadows) are executed immediately. Doing that via {@link ChangeExecutor} would
     * require excessive changes, because that would require creating projection contexts, which we don't do now.
     *
     * This is much simpler; and perhaps appropriate, as this operation is not really a part of the main processing.
     * Moreover, it is not going to the simulation result (just like manipulation of link status is not there).
     */
    private void processInactiveLinkRefDeletion(
            @NotNull Collection<PrismReferenceValue> linksToDelete, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, PolicyViolationException, ObjectNotFoundException, ObjectAlreadyExistsException {
        if (linksToDelete.isEmpty()) {
            return;
        } else if (!task.isExecutionFullyPersistent()) {
            LOGGER.trace("Ignoring inactive linkRef values to delete, because of the simulation mode: {}", linksToDelete);
            return;
        }

        var itemDeltas = PrismContext.get().deltaFor(FocusType.class)
                .item(FocusType.F_LINK_REF)
                .delete(CloneUtil.cloneCollectionMembers(linksToDelete))
                .asItemDeltas();
        LOGGER.debug("Removing inactive linkRef values from focus:\n{}", DebugUtil.debugDumpLazily(itemDeltas, 1));
        beans.cacheRepositoryService.modifyObject(
                focusContext.getObjectTypeClass(),
                focusContext.getObjectCurrent().getOid(),
                itemDeltas,
                result);

        for (PrismReferenceValue link : linksToDelete) {
            if (link.getObject() != null) {
                String oid = link.getOid();
                LOGGER.debug("Deleting dead shadow {}", oid);
                try {
                    // Note that the repo shadow may or may not be really deleted. It depends e.g. on the shadow retention policy.
                    beans.provisioningService.deleteObject(ShadowType.class, oid, null, null, task, result);
                } catch (ObjectNotFoundException e) {
                    LOGGER.debug("Dead shadow {} not found, not deleting it", oid);
                }
            }
        }
    }

    /**
     * Updates (already existing) contexts from their sync deltas, if needed.
     */
    private void updateContextsFromSyncDeltas(OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        LOGGER.trace("Initialization of projection contexts from sync delta(s) starting - if there are any");
        for (LensProjectionContext projCtx : context.getProjectionContexts()) {
            if (projCtx.isFresh() && projCtx.getObjectCurrent() != null) {
                LOGGER.trace("Not considering sync delta in {} as it is already loaded: fresh and has current object", projCtx);
                continue;
            }
            ObjectDelta<ShadowType> syncDelta = projCtx.getSyncDelta();
            if (syncDelta == null) {
                LOGGER.trace("No sync delta in {}", projCtx);
                continue;
            }
            LOGGER.trace("Found sync delta in {}: {}", projCtx, syncDelta);

            if (projCtx.isDoReconciliation()) {
                LOGGER.trace("Not loading the state now. It will get loaded later in the reconciliation step."
                        + " Just marking it as fresh.");
                projCtx.setFresh(true); // TODO is this correct?
                continue;
            }

            String oid = syncDelta.getOid();
            PrismObject<ShadowType> shadow;

            if (syncDelta.isAdd()) {
                shadow = syncDelta.getObjectToAdd().clone();
                projCtx.setLoadedObject(shadow);
                projCtx.setExists(ShadowUtil.isExists(shadow.asObjectable()));

            } else {
                argCheck(oid != null, "No OID in non-ADD sync delta in %s", projCtx);

                // Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account
                // here. We need to fetch from provisioning and not repository so the correct definition will be set.
                var options = SchemaService.get().getOperationOptionsBuilder()
                        .noFetch()
                        .doNotDiscovery()
                        .futurePointInTime()
                        .allowNotFound()
                        //.readOnly() [not yet]
                        .build();
                try {
                    shadow = provisioningService.getObject(ShadowType.class, oid, options, task, result);
                } catch (ObjectNotFoundException e) {
                    LOGGER.trace("Loading shadow {} from sync delta failed: not found", oid);
                    projCtx.clearCurrentObject();
                    projCtx.setShadowExistsInRepo(false);
                    projCtx.markGone();
                    shadow = null;
                }

                // We will not set old account if the delta is delete. The account does not really exists now.
                // (but the OID and resource will be set from the repo shadow)
                if (syncDelta.isDelete()) {
                    projCtx.markGone();
                } else if (shadow != null) {
                    syncDelta.applyTo(shadow);
                    projCtx.setLoadedObject(shadow);
                    projCtx.setExists(ShadowUtil.isExists(shadow.asObjectable()));
                }
            }

            // Make sure OID is set correctly
            projCtx.setOid(oid);

            // Make sure that resource is also resolved
            if (projCtx.getResource() == null && shadow != null) {
                String resourceOid = ShadowUtil.getResourceOid(shadow.asObjectable());
                argCheck(resourceOid != null, "No resource OID in %s", shadow);
                projCtx.setResource(
                        LensUtil.getResourceReadOnly(context, resourceOid, provisioningService, task, result));
            }
            projCtx.setFresh(true);
        }
        LOGGER.trace("Initialization of projection contexts from sync delta(s) done");
    }

    //region Utility methods (used in link/shadow processing)
    private void checkNewShadowClassified(@NotNull ShadowType newShadow) {
        ShadowKindType kind = newShadow.getKind();
        String intent = newShadow.getIntent();
        argCheck(ShadowUtil.isKnown(kind) && ShadowUtil.isKnown(intent),
                "Shadow being added is not classified: %s/%s: %s. Starting with midPoint 4.6, all shadows that are"
                        + " added as explicitly provided objects in linkRef must have both kind and intent properties set.",
                kind, intent, newShadow);
    }

    private LensProjectionContext getOrCreateEmptyGone(String missingShadowOid) {
        List<LensProjectionContext> allByOid = context.findProjectionContextsByOid(missingShadowOid);
        for (LensProjectionContext existing : allByOid) {
            existing.markGone();
            existing.clearCurrentObject();
        }
        if (!allByOid.isEmpty()) {
            return allByOid.get(0);
        } else {
            LensProjectionContext projContext = context.createProjectionContext(missing());
            projContext.setOid(missingShadowOid);
            return projContext;
        }
    }
    //endregion

    /**
     * Loads the projection context that is related to specific `linkRef` value.
     *
     * (This class is here just to give the code some structure. The determination of context from a shadow
     * is located in {@link ShadowLevelLoadOperation}.)
     */
    private class LinkLevelLoadOperation {

        @NotNull private final Referencable linkRef;

        LinkLevelLoadOperation(@NotNull Referencable linkRef) {
            this.linkRef = linkRef;
        }

        private void getOrCreateFromExistingValue(OperationResult result)
                throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
                ExpressionEvaluationException, ObjectNotFoundException, PolicyViolationException {

            LOGGER.trace("Loading projection from linkRef {}", linkRef);
            String oid = linkRef.getOid();
            if (isInactive(linkRef)) {
                LOGGER.trace("Inactive linkRef: will only refresh it, no processing. Relation={}", linkRef.getRelation());
                refreshInactiveLinkedShadow(oid, result);
                return;
            }

            if (StringUtils.isBlank(oid)) {
                PrismObject<F> focus = focusContext.getObjectCurrent();
                LOGGER.trace("Null or empty OID in link reference {} in:\n{}", linkRef, focus.debugDump(1));
                throw new SchemaException("Null or empty OID in link reference in " + focus);
            }

            LensProjectionContext existingProjCtx = context.findProjectionContextByOid(oid);
            if (existingProjCtx != null) { // FIXME what if there are more such contexts?
                LOGGER.trace("Found existing projection context for linkRef {}: {}", linkRef, existingProjCtx);
                LOGGER.trace("Setting the freshness to TRUE");
                // FIXME Why we are marking the context as fresh? This is really strange. It's here since 2012.
                //  If we don't set the context as fresh, the context will be wiped as part of rotten
                //  contexts removal in ContextLoadOperation. Not good.
                existingProjCtx.setFresh(true);
            } else {
                LOGGER.trace("Going to create a projection context for linkRef {}", linkRef);
                getOrCreateFromActiveLinkRef(result);
            }
        }

        /**
         * We do this just to restore old behavior that ensures that linked shadows are quick-refreshed,
         * deleting e.g. expired pending operations (see TestMultiResource.test429).
         */
        private void refreshInactiveLinkedShadow(String oid, OperationResult result) {
            Collection<SelectorOptions<GetOperationOptions>> options;
            if (context.isDoReconciliationForAllProjections()) {
                // Ensures an attempt to complete any pending operations.
                // TODO Shouldn't we include FUTURE option as well? E.g. to avoid failing on not-yet-created accounts?
                //  (Fortunately, we ignore any exceptions but anyway: FUTURE is used in other cases in this class.)
                options = SchemaService.get().getOperationOptionsBuilder()
                        .forceRetry()
                        .readOnly()
                        .build();
            } else {
                // This ensures only minimal processing, e.g. the quick shadow refresh is done.
                options = SchemaService.get().getOperationOptionsBuilder()
                        .noFetch()
                        .futurePointInTime()
                        .readOnly()
                        .build();
            }
            try {
                provisioningService.getObject(ShadowType.class, oid, options, task, result);
            } catch (ObjectNotFoundException e) {
                // We will NOT delete the linkRef here. Instead, we will persuade LinkUpdater to do it, by creating a broken
                // projection context (just as if the link would be regular one). This is the only situation when there is
                // a projection context created for inactive linkRef.
                getOrCreateEmptyGoneProjectionContext(oid);
                result.getLastSubresult()
                        .muteErrorsRecursively();
            } catch (Exception e) {
                result.muteLastSubresultError();
                LOGGER.debug("Couldn't refresh linked shadow {}. Continuing.", oid, e);
            }
        }

        private void getOrCreateFromActiveLinkRef(OperationResult result)
                throws CommunicationException, SchemaException, ConfigurationException, SecurityViolationException,
                ExpressionEvaluationException, ObjectNotFoundException, PolicyViolationException {

            PrismObject<ShadowType> shadow = getShadow(result);
            if (shadow == null) {
                return; // "Gone" projection context is already created
            }

            shadow.freeze(); // to avoid unnecessary cloning when shadow enters the projection context
            ContextAcquisitionResult acqResult =
                    new ShadowLevelLoadOperation(shadow.asObjectable())
                            .getOrCreate(result);
            LensProjectionContext projectionContext = acqResult.context;
            if (acqResult.shadowSet) {
                projectionContext.setFresh(projectionContext.getObjectOld() != null); // TODO reconsider
            } else {
                projectionContext.setFresh(true);
                projectionContext.setExists(ShadowUtil.isExists(shadow.asObjectable()));
                if (ShadowUtil.isGone(shadow.asObjectable())) {
                    projectionContext.markGone();
                    LOGGER.trace("Loading gone shadow {} for projection {}.", shadow, projectionContext.getHumanReadableName());
                    return;
                }
                if (projectionContext.isDoReconciliation()) {
                    // Do not load old account now. It will get loaded later in the reconciliation step.
                    return;
                }
                projectionContext.setLoadedObject(shadow);
            }
        }

        /**
         * Gets a shadow (embedded or referenced, with definitions applied) from linkRef.
         * Returns null if it does not exist in repo; "gone" proj ctx is created in such case.
         */
        private @Nullable PrismObject<ShadowType> getShadow(OperationResult result)
                throws CommunicationException, SchemaException, ConfigurationException, SecurityViolationException,
                ExpressionEvaluationException, ObjectNotFoundException {
            PrismObject<ShadowType> embeddedShadow = getEmbeddedShadow();
            if (embeddedShadow != null) {
                // Make sure it has a proper definition. This may come from outside of the model.
                provisioningService.applyDefinition(embeddedShadow, task, result);
                provisioningService.determineShadowState(embeddedShadow, task, result);
                if (embeddedShadow.asObjectable().getEffectiveOperationPolicy() == null) {
                    // As far as marks are to be computed, we assume that the shadow does not exist yet in the repository.
                    provisioningService.updateShadowMarksAndPolicies(embeddedShadow, true, task, result);
                }
                return embeddedShadow;
            }

            String oid = linkRef.getOid();
            // Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
            // We need to fetch from provisioning and not repository so the correct definition will be set.
            var options = SchemaService.get().getOperationOptionsBuilder()
                    .noFetch()
                    .futurePointInTime()
                    .readOnly()
                    .build();
            LOGGER.trace("Loading shadow {} from linkRef, options={}", oid, options);
            try {
                var shadow = provisioningService.getObject(ShadowType.class, oid, options, task, result);
                LOGGER.trace("Found {}", ShadowUtil.getDiagInfoLazily(shadow));
                return shadow;
            } catch (ObjectNotFoundException e) {
                LOGGER.trace("Got 'not found exception'", e);
                getOrCreateEmptyGoneProjectionContext(oid);
                result.getLastSubresult()
                        .muteErrorsRecursively();
                return null;
            }
        }

        private void getOrCreateForValueToAdd(OperationResult result)
                throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
                ExpressionEvaluationException, SecurityViolationException, PolicyViolationException {
            String oid = linkRef.getOid();
            LensProjectionContext projectionContext;
            Holder<Boolean> objectDoesExistInRepoHolder = new Holder<>(false);
            if (oid == null) {
                projectionContext = getOrCreateForEmbeddedShadow(result);
            } else {
                projectionContext = getOrCreateForValueToAddWithOid(objectDoesExistInRepoHolder, result);
            }
            if (context.isDoReconciliationForAllProjections() && objectDoesExistInRepoHolder.getValue()) {
                projectionContext.setDoReconciliation(true);
            }
            projectionContext.setFresh(true);
        }

        /**
         * We have OID. This is either linking of existing account or adding new account - therefore let's check
         * for account existence to decide.
         */
        private @NotNull LensProjectionContext getOrCreateForValueToAddWithOid(
                @NotNull Holder<Boolean> objectDoesExistInRepoHolder, @NotNull OperationResult result)
                throws CommunicationException, SchemaException, ConfigurationException, SecurityViolationException,
                ExpressionEvaluationException, PolicyViolationException, ObjectNotFoundException {

            String oid = linkRef.getOid();
            try {
                // Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
                // We need to fetch from provisioning and not repository so the correct definition will be set.
                var options =
                        SchemaService.get().getOperationOptionsBuilder()
                                .noFetch()
                                .futurePointInTime()
                                //.readOnly() [not yet]
                                .build();
                PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, oid, options, task, result);
                // Create account context from retrieved object
                LensProjectionContext projectionContext =
                        new ShadowLevelLoadOperation(shadow.asObjectable())
                                .getOrCreate(result).context; // TODO what about shadowSet etc?
                projectionContext.setLoadedObject(shadow);
                projectionContext.setExists(ShadowUtil.isExists(shadow.asObjectable()));
                objectDoesExistInRepoHolder.setValue(true);
                return projectionContext;
            } catch (ObjectNotFoundException e) {
                PrismObject<ShadowType> embeddedShadow = getEmbeddedShadow();
                if (embeddedShadow == null) {
                    // Account does not exist, and no composite account in ref -> this is really an error.
                    throw e;
                } else {
                    // New account (but with OID)
                    result.muteLastSubresultError();
                    if (!embeddedShadow.hasCompleteDefinition()) {
                        provisioningService.applyDefinition(embeddedShadow, task, result);
                    }
                    // Create account context from embedded object
                    LensProjectionContext projectionContext =
                            new ShadowLevelLoadOperation(embeddedShadow.asObjectable())
                                    .createNew(result);
                    projectionContext.setPrimaryDeltaAfterStart(embeddedShadow.createAddDelta());
                    projectionContext.setFullShadow(true);
                    projectionContext.setExists(false);
                    projectionContext.setShadowExistsInRepo(false);
                    return projectionContext;
                }
            }
        }

        private PrismObject<ShadowType> getEmbeddedShadow() {
            return linkRef.asReferenceValue().getObject();
        }

        private void getOrCreateForValueToDelete(@NotNull OperationResult result)
                throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
                ExpressionEvaluationException, PolicyViolationException {
            String oid = linkRef.getOid();
            schemaCheck(oid != null,
                    "Cannot delete account ref without an oid in %s", focusContext.getObjectCurrent());

            LensProjectionContext projectionContext;
            PrismObject<ShadowType> shadow;
            try {
                // Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
                // We need to fetch from provisioning and not repository so the correct definition will be set.
                Collection<SelectorOptions<GetOperationOptions>> options =
                        SchemaService.get().getOperationOptionsBuilder()
                                .noFetch()
                                //.readOnly() [not yet]
                                .build();
                shadow = provisioningService.getObject(ShadowType.class, oid, options, task, result);
                // Create account context from retrieved object
                projectionContext =
                        new ShadowLevelLoadOperation(shadow.asObjectable())
                                .getOrCreate(result).context; // TODO what about shadowSet etc?
                projectionContext.setLoadedObject(shadow);
                projectionContext.setExists(ShadowUtil.isExists(shadow.asObjectable()));
                LOGGER.trace("Loaded projection context: {}", projectionContext);
            } catch (ObjectNotFoundException e) {
                try {
                    LOGGER.trace("Broken linkRef? We need to try again with raw options, because the error could be "
                            + "thrown because of non-existent resource", e);
                    Collection<SelectorOptions<GetOperationOptions>> options =
                            SchemaService.get().getOperationOptionsBuilder()
                                    .raw()
                                    //.readOnly() [not yet]
                                    .build();
                    shadow = provisioningService.getObject(ShadowType.class, oid, options, task, result);
                    provisioningService.determineShadowState(shadow, task, result);
                    provisioningService.updateShadowMarksAndPolicies(shadow, false, task, result);
                    projectionContext = getOrCreateEmptyGone(oid);
                    projectionContext.setFresh(true);
                    projectionContext.setExists(false);
                    projectionContext.setShadowExistsInRepo(false);
                    LOGGER.trace("Loaded projection context: {}", projectionContext);
                    OperationResult getObjectSubresult = result.getLastSubresult();
                    getObjectSubresult.muteErrorsRecursively();
                } catch (ObjectNotFoundException ex) {
                    // This is still OK. It means deleting an accountRef that points to non-existing object just log a warning
                    LOGGER.warn("Deleting accountRef of " + focusContext.getObjectCurrent() + " that points to non-existing OID " + oid);
                    return;
                }
            }
            if (getEmbeddedShadow() == null) {
                projectionContext.setSynchronizationIntent(SynchronizationIntent.UNLINK);
            } else {
                // I.e. this is when we request to delete link containing full object.
                projectionContext.setSynchronizationIntent(SynchronizationIntent.DELETE);
                projectionContext.setPrimaryDeltaAfterStart(shadow.createDeleteDelta());
            }
            projectionContext.setFresh(true);
        }

        @NotNull
        private LensProjectionContext getOrCreateForEmbeddedShadow(@NotNull OperationResult result)
                throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
                ExpressionEvaluationException {
            PrismObject<ShadowType> embeddedShadowObject =
                    MiscUtil.requireNonNull(getEmbeddedShadow(),
                            () -> "No OID nor object in account reference " + linkRef + " in " + focusContext.getObjectCurrent());

            ShadowType embeddedShadow = embeddedShadowObject.asObjectable();
            checkNewShadowClassified(embeddedShadow);
            provisioningService.applyDefinition(embeddedShadowObject, task, result);
            if (consistencyChecks) ShadowUtil.checkConsistence(embeddedShadowObject, "account from " + linkRef);

            // Check for conflicting change
            Collection<LensProjectionContext> conflictingContexts = findConflictingContexts(embeddedShadow);
            List<LensProjectionContext> acceptableConflictingContexts = new ArrayList<>();
            for (LensProjectionContext conflictingContext : conflictingContexts) {
                checkExistingContextSanity(embeddedShadow, conflictingContext);
                acceptableConflictingContexts.add(conflictingContext);
            }

            LensProjectionContext projectionContext;
            if (acceptableConflictingContexts.size() > 1) {
                throw new IllegalStateException("Multiple matching contexts: " + acceptableConflictingContexts);
            } else if (acceptableConflictingContexts.size() == 1) {
                projectionContext = acceptableConflictingContexts.get(0);
            } else {
                projectionContext =
                        new ShadowLevelLoadOperation(embeddedShadow)
                                .createNew(result);
            }
            // This is a new account that is to be added. So it should go to account primary delta.
            projectionContext.setPrimaryDeltaAfterStart(embeddedShadowObject.createAddDelta());
            projectionContext.setFullShadow(true);
            projectionContext.setExists(false);
            return projectionContext;
        }

        private Collection<LensProjectionContext> findConflictingContexts(ShadowType shadowToAdd) {
            ShadowKindType kind = ShadowUtil.getKind(shadowToAdd);
            String intent = ShadowUtil.getIntent(shadowToAdd);
            String resourceOid = MiscUtil.argNonNull(
                    ShadowUtil.getResourceOid(shadowToAdd),
                    () -> "No resource OID in shadow being added: " + shadowToAdd);
            return context.findProjectionContexts(
                    new ProjectionContextFilter(resourceOid, kind, intent, shadowToAdd.getTag()));
        }

        /**
         * There is already existing context for the same key. Tolerate this only if the deltas match. It is an error otherwise.
         */
        private void checkExistingContextSanity(
                ShadowType embeddedShadow,
                LensProjectionContext existingProjectionContext) throws SchemaException {
            ObjectDelta<ShadowType> primaryDelta = existingProjectionContext.getPrimaryDelta();
            if (primaryDelta == null) {
                throw new SchemaException("Attempt to add " + embeddedShadow + " to a focus that already contains " +
                        existingProjectionContext.getHumanReadableKind() + " of type '" +
                        existingProjectionContext.getKey().getIntent() + "' on " +
                        existingProjectionContext.getResource());
            }
            if (!primaryDelta.isAdd()) {
                throw new SchemaException("Conflicting changes in the context. " +
                        "Add of linkRef in the focus delta with embedded object conflicts with explicit delta " + primaryDelta);
            }
            if (!embeddedShadow.asPrismObject().equals(primaryDelta.getObjectToAdd())) {
                throw new SchemaException("Conflicting changes in the context. Add of linkRef in the focus delta with embedded "
                        + "object is not adding the same object as explicit delta " + primaryDelta);
            }
        }
    }

    private void getOrCreateEmptyGoneProjectionContext(String oid) {
        LOGGER.trace("Broken linkRef {}. We need to mark it for deletion by ensuring 'gone' projection context.", oid);
        LensProjectionContext projectionContext = getOrCreateEmptyGone(oid);
        projectionContext.setFresh(true);
        projectionContext.setExists(false);
        projectionContext.setShadowExistsInRepo(false);
    }

    /**
     * Loads the projection context from specified shadow (determined in {@link LinkLevelLoadOperation}).
     */
    private class ShadowLevelLoadOperation {

        @NotNull private final ShadowType shadow;

        private ShadowLevelLoadOperation(@NotNull ShadowType shadow) {
            this.shadow = shadow;
        }

        /**
         * Creates a _new_ projection context - i.e. we are sure we do not want to re-use existing context.
         * That's why we do a uniqueness check here.
         */
        private LensProjectionContext createNew(OperationResult result)
                throws ObjectNotFoundException, SchemaException {
            checkNewShadowClassified(shadow);
            ProjectionContextKey key = beans.projectionContextKeyFactory.createKey(shadow, task, result);
            LensProjectionContext existingProjectionContext = context.findProjectionContextByKeyExact(key);
            if (existingProjectionContext != null) {
                // TODO better message
                throw new SchemaException("Attempt to add " + shadow + " to a focus that already contains projection of type '"
                        + key.getKind() + "/" + key.getIntent() + "' on " + key.getResourceOid());
            }
            LensProjectionContext newCtx = context.createProjectionContext(key);
            newCtx.setOid(shadow.getOid());
            return newCtx;
        }

        /**
         * Gets or creates a projection context for given shadow.
         *
         * 1. First tries directly by shadow OID.
         * 2. If not successful, tries to get/create context by shadow coordinates (checking also the conflict with existing
         * projection contexts).
         */
        private ContextAcquisitionResult getOrCreate(@NotNull OperationResult result)
                throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
                SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
            LensProjectionContext contextByShadowOid = context.findProjectionContextByOid(shadow.getOid());
            // TODO what if there are more contexts for given shadow OID?
            LOGGER.trace("Projection context by shadow OID: {} yielded: {}", shadow.getOid(), contextByShadowOid);
            if (contextByShadowOid != null) {
                return ContextAcquisitionResult.existing(contextByShadowOid);
            }

            ProjectionContextKey key = beans.projectionContextKeyFactory.createKey(shadow, task, result);

            GetOrCreateProjectionContextResult projCtxResult = getOrCreateProjectionContext(context, key);
            LOGGER.trace("Projection context for {} ({}): {}", shadow, key, projCtxResult);

            LensProjectionContext projCtx = projCtxResult.context;
            if (projCtx.getOid() != null && shadow.getOid() != null && !projCtx.getOid().equals(shadow.getOid())) {
                assert !projCtxResult.created;
                return treatContextConflict(shadow, key, projCtx, result);
            } else {
                if (projCtx.getOid() == null) {
                    projCtx.setOid(shadow.getOid());
                }
                return new ContextAcquisitionResult(projCtx, projCtxResult.created, false);
            }
        }

        /**
         * Resolves a conflict:
         *
         * . We have a `newShadow` (~ `conflictingKey`) and we wanted to find/create context for it.
         * . We found existing context (`existingCtx`) for the `conflictingKey`, but alas, it belongs to a different shadow OID!
         *
         * Chances are that the old object (`existingCtx.object`) is already deleted, e.g. during rename. So let's be
         * slightly inefficient here and check for resource object existence.
         *
         * There is a small amount of magic when dealing with the objects that no longer exist in repo.
         */
        private @NotNull ContextAcquisitionResult treatContextConflict(
                @NotNull ShadowType newShadow,
                @NotNull ProjectionContextKey conflictingKey,
                @NotNull LensProjectionContext existingCtx,
                @NotNull OperationResult result)
                throws CommunicationException, SchemaException, ConfigurationException, SecurityViolationException,
                ExpressionEvaluationException, PolicyViolationException {

            LOGGER.trace("Projection conflict detected on key: {}. Existing context: {}, new shadow {}",
                    conflictingKey, existingCtx.getOid(), newShadow.getOid());
            try {
                var opts = SchemaService.get().getOperationOptionsBuilder()
                        .doNotDiscovery()
                        .futurePointInTime()
                        //.readOnly() [not yet]
                        .build();
                LOGGER.trace("Loading resource object corresponding to the existing projection ({})", existingCtx.getOid());
                ShadowType objectForExistingCtx = provisioningService
                        .getObject(ShadowType.class, existingCtx.getOid(), opts, task, result)
                        .asObjectable();

                // Maybe it is the other way around
                try {
                    LOGGER.trace("Loading resource object corresponding to the newly added projection ({})", newShadow.getOid());
                    ShadowType objectForNewCtx = provisioningService
                            .getObject(ShadowType.class, newShadow.getOid(), opts, task, result)
                            .asObjectable();

                    // Obviously, two resource objects with the same discriminator exist.
                    LOGGER.trace("Projection {} already exists in context\nExisting:\n{}\nNew:\n{}", conflictingKey,
                            objectForExistingCtx.debugDumpLazily(1), objectForNewCtx.debugDumpLazily(1));

                    if (!ShadowUtil.isDead(objectForNewCtx)) {
                        throw new PolicyViolationException(
                                String.format("Projection %s already exists in lens context (existing %s, new %s)",
                                        conflictingKey, objectForExistingCtx, newShadow));
                    }

                    // Dead shadow for the new context. This is somehow expected, fix it and we can go on.
                    conflictingKey = conflictingKey.gone();

                    // Let us create or find the "newest" context, i.e. context for the key updated with gone=true.
                    // We will use/reuse it with no other checks.
                    GetOrCreateProjectionContextResult newestCtxResult = getOrCreateProjectionContext(context, conflictingKey);
                    LensProjectionContext newestCtx = newestCtxResult.context;
                    newestCtx.setExists(ShadowUtil.isExists(objectForNewCtx));
                    newestCtx.setFullShadow(false);
                    newestCtx.setLoadedObject(objectForNewCtx.asPrismObject()); // TODO ok even if we reused existing context?
                    newestCtx.setOid(objectForNewCtx.getOid());
                    return new ContextAcquisitionResult(newestCtx, newestCtxResult.created, true);

                } catch (ObjectNotFoundException e) {

                    // Object for the new context does not exist. It looks like that this exception means that it does not
                    // exist in repository - not just on resource. (See ObjectNotFoundHandler in provisioning.)
                    result.muteLastSubresultError();

                    // We have to create new context in this case, but it has to have "gone" set.
                    conflictingKey = conflictingKey.gone();

                    // Let us create or find the "newest" context, i.e. context for rsd updated with gone=true.
                    GetOrCreateProjectionContextResult newestCtxResult = getOrCreateProjectionContext(context, conflictingKey);
                    LensProjectionContext newestCtx = newestCtxResult.context;

                    newestCtx.setShadowExistsInRepo(false);

                    // We return the result with shadowSet=true: It means that there's no need to set the shadow by caller.
                    return new ContextAcquisitionResult(newestCtx, newestCtxResult.created, true);
                }
            } catch (ObjectNotFoundException e) {

                // Object for the existing context does not exist (in repo).
                // This is somehow expected, fix it and we can go on.

                result.muteLastSubresultError();
                existingCtx.markGone();

                // Let us again try to create or find the "newest" context. The conflicting context is now set as gone.
                GetOrCreateProjectionContextResult newestCtxResult = getOrCreateProjectionContext(context, conflictingKey);
                LensProjectionContext newestCtx = newestCtxResult.context;
                newestCtx.setShadowExistsInRepo(false);

                // We return the result with shadowSet=true: It means that there's no need to set the shadow by caller.
                return new ContextAcquisitionResult(newestCtx, newestCtxResult.created, true);
            }
        }
    }

    @Experimental // This is a temporary solution: TODO factor out context acquisition into separate class some day.
    private static class ContextAcquisitionResult {
        private final LensProjectionContext context;
        @SuppressWarnings({ "FieldCanBeLocal", "unused" }) private final boolean created; // TODO what do to with this?
        private final boolean shadowSet;

        private ContextAcquisitionResult(LensProjectionContext context, boolean created, boolean shadowSet) {
            this.context = context;
            this.created = created;
            this.shadowSet = shadowSet;
        }

        private static ContextAcquisitionResult existing(LensProjectionContext ctx) {
            return new ContextAcquisitionResult(ctx, false, false);
        }
    }
}
