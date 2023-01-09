/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.loader;

import static com.evolveum.midpoint.schema.internals.InternalsConfig.consistencyChecks;
import static com.evolveum.midpoint.util.MiscUtil.*;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.context.SynchronizationIntent;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValueCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Responsible for the acquisition of all projections for a focus. See {@link #load(OperationResult)} method.
 *
 * TODO better name for the class?
 *
 * Note: full resource objects are not loaded in this class (now). See {@link ProjectionUpdateOperation}
 * (for reconciliation) and {@link ProjectionFullLoadOperation} (for ad-hoc full shadow loading).
 *
 * BEWARE: removes linkRef modifications from the primary delta, if there were any
 */
public class ProjectionsLoadOperation<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(ProjectionsLoadOperation.class);

    private static final String OP_LOAD = ProjectionsLoadOperation.class.getName() + "." + "load";

    @NotNull private final LensContext<F> context;
    @NotNull private final LensFocusContext<F> focusContext;
    @NotNull private final Task task;
    @NotNull private final ModelBeans beans;

    public ProjectionsLoadOperation(@NotNull LensContext<F> context, @NotNull Task task) {
        this.context = context;
        this.focusContext = context.getFocusContext();
        this.task = task;
        this.beans = ModelBeans.get();
    }

    public void load(OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.subresult(OP_LOAD)
                .setMinor()
                .build();

        LOGGER.trace("loading projections starting");
        try {

            getOrCreateProjectionContextsFromFocusLinkRefs(result);
            getOrCreateProjectionContextsFromFocusPrimaryDelta(result);
            initializeContextsFromSyncDeltas(result);

            context.checkConsistenceIfNeeded();
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * Loads projections from focus.linkRef values.
     *
     * Does not overwrite existing account contexts, just adds new ones if needed.
     */
    private void getOrCreateProjectionContextsFromFocusLinkRefs(OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        PrismObject<F> focus = focusContext.getObjectCurrent();
        if (focus == null) {
            return;
        }

        PrismReference linkRef = focus.findReference(FocusType.F_LINK_REF);
        if (linkRef == null) {
            return;
        }

        for (PrismReferenceValue linkRefVal : linkRef.getValues()) {
            getOrCreateProjectionContextFromAnyLinkRefVal(linkRefVal, result);
        }

        context.checkConsistenceIfNeeded();
    }

    private void getOrCreateProjectionContextFromAnyLinkRefVal(@NotNull PrismReferenceValue linkRefVal, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException, PolicyViolationException {

        String oid = linkRefVal.getOid();
        if (isInactive(linkRefVal)) {
            LOGGER.trace("Inactive linkRef: will only refresh it, no processing. Relation={}, ref={}",
                    linkRefVal.getRelation(), linkRefVal);
            refreshInactiveLinkedShadow(oid, result);
            return;
        }

        if (StringUtils.isBlank(oid)) {
            PrismObject<F> focus = focusContext.getObjectCurrent();
            LOGGER.trace("Null or empty OID in link reference {} in:\n{}", linkRefVal, focus.debugDump(1));
            throw new SchemaException("Null or empty OID in link reference in " + focus);
        }

        LensProjectionContext existingProjCtx = context.findProjectionContextByOid(oid);
        if (existingProjCtx != null) {
            LOGGER.trace("Found existing projection context for linkRefVal {}: {}", linkRefVal, existingProjCtx);
            LOGGER.trace("Setting the freshness to TRUE");
            // FIXME Why we are marking the context as fresh? This is really strange. It's here since 2012.
            //  If we don't set the context as fresh, the context will be wiped as part of rotten
            //  contexts removal in ContextLoadOperation. Not good.
            existingProjCtx.setFresh(true);
        } else {
            LOGGER.trace("Going to create a projection context for linkRefVal {}", linkRefVal);
            getOrCreateProjectionContextFromActiveLinkRefVal(linkRefVal, result);
        }
    }

    private void getOrCreateProjectionContextFromActiveLinkRefVal(@NotNull PrismReferenceValue linkRefVal, OperationResult result)
            throws CommunicationException, SchemaException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException, PolicyViolationException {

        PrismObject<ShadowType> shadow = getShadowForLinkRefVal(linkRefVal, result);
        if (shadow == null) {
            return; // "Gone" projection context is already created
        }

        ContextAcquisitionResult acqResult = getOrCreateProjectionContext(shadow.asObjectable(), result);
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
            projectionContext.recompute();
        }
    }

    /**
     * Gets a shadow (embedded or referenced, with definitions applied) from linkRef.
     * Returns null if it does not exist in repo; "gone" proj ctx is created in such case.
     */
    @Nullable
    private PrismObject<ShadowType> getShadowForLinkRefVal(@NotNull PrismReferenceValue linkRefVal, OperationResult result)
            throws CommunicationException, SchemaException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        //noinspection unchecked
        PrismObject<ShadowType> embeddedShadow = linkRefVal.getObject();
        if (embeddedShadow != null) {
            // Make sure it has a proper definition. This may come from outside of the model.
            beans.provisioningService.applyDefinition(embeddedShadow, task, result);
            beans.provisioningService.determineShadowState(embeddedShadow, task, result);
            return embeddedShadow;
        }

        String oid = linkRefVal.getOid();
        var options = createStandardShadowGetOptions();
        LOGGER.trace("Loading shadow {} from linkRef, options={}", oid, options);
        try {
            return beans.provisioningService.getObject(ShadowType.class, oid, options, task, result);
        } catch (ObjectNotFoundException e) {
            createBrokenProjectionContext(oid);
            result.getLastSubresult()
                    .setErrorsHandled();
            return null;
        }
    }

    private void createBrokenProjectionContext(String oid) {
        LOGGER.trace("Broken linkRef {}. We need to mark it for deletion.", oid);
        LensProjectionContext projectionContext = getOrCreateEmptyGoneProjectionContext(oid);
        projectionContext.setFresh(true);
        projectionContext.setExists(false);
        projectionContext.setShadowExistsInRepo(false);
    }

    private boolean isInactive(PrismReferenceValue linkRefVal) {
        return !SchemaService.get().relationRegistry().isMember(linkRefVal.getRelation());
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
            beans.provisioningService.getObject(ShadowType.class, oid, options, task, result);
        } catch (ObjectNotFoundException e) {
            // We will NOT delete the linkRef here. Instead, we will persuade LinkUpdater to do it, by creating a broken
            // projection context (just as if the link would be regular one). This is the only situation when there is
            // a projection context created for inactive linkRef.
            createBrokenProjectionContext(oid);
            result.getLastSubresult()
                    .setErrorsHandled();
        } catch (Exception e) {
            result.muteLastSubresultError();
            LOGGER.debug("Couldn't refresh linked shadow {}. Continuing.", oid, e);
        }
    }

    private void getOrCreateProjectionContextsFromFocusPrimaryDelta(OperationResult result) throws SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        ObjectDelta<F> focusPrimaryDelta = focusContext.getPrimaryDelta();
        if (focusPrimaryDelta == null) {
            return;
        }

        ReferenceDelta linkRefDelta = getLinkRefDelta(focusPrimaryDelta);
        if (linkRefDelta == null) {
            return;
        }

        if (linkRefDelta.isReplace()) {
            linkRefDelta = distributeLinkRefReplace(linkRefDelta);
        }

        getOrCreateContextsForValuesToAdd(linkRefDelta.getValuesToAdd(), result);
        getOrCreateContextsForValuesToDelete(linkRefDelta.getValuesToDelete(), result);

        removeLinkRefModifications();
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

    private void getOrCreateContextsForValuesToAdd(Collection<PrismReferenceValue> valuesToAdd, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException, PolicyViolationException {
        for (PrismReferenceValue refVal : emptyIfNull(valuesToAdd)) {
            if (isInactive(refVal)) {
                LOGGER.trace("getOrCreateContextsForValuesToAdd: Skipping inactive linkRef to add (relation={}): {}",
                        refVal.getRelation(), refVal);
            } else {
                LOGGER.trace("getOrCreateContextsForValuesToAdd: Processing value to add: {}", refVal);
                getOrCreateContextForValueToAdd(refVal, result);
            }
        }
    }

    private void getOrCreateContextForValueToAdd(PrismReferenceValue refVal, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException, PolicyViolationException {
        String oid = refVal.getOid();
        LensProjectionContext projectionContext;
        Holder<Boolean> objectDoesExistInRepoHolder = new Holder<>(false);
        if (oid == null) {
            projectionContext = getOrCreateContextForValueToAddNoOid(refVal, result);
        } else {
            projectionContext = getOrCreateProjectionForValueToAddWithOid(refVal, objectDoesExistInRepoHolder, result);
        }
        if (context.isDoReconciliationForAllProjections() && objectDoesExistInRepoHolder.getValue()) {
            projectionContext.setDoReconciliation(true);
        }
        projectionContext.setFresh(true);
    }

    @NotNull
    private LensProjectionContext getOrCreateContextForValueToAddNoOid(@NotNull PrismReferenceValue refVal,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException {
        //noinspection unchecked
        PrismObject<ShadowType> shadow = refVal.getObject();
        if (shadow == null) {
            throw new SchemaException("Null or empty OID in account reference " + refVal + " in " + focusContext.getObjectCurrent());
        }
        beans.provisioningService.applyDefinition(shadow, task, result);
        if (consistencyChecks) ShadowUtil.checkConsistence(shadow, "account from " + refVal);

        LensProjectionContext projectionContext;
        // Check for conflicting change
        LensProjectionContext existingProjectionContext =
                LensUtil.getProjectionContext(context, shadow, beans.provisioningService, beans.prismContext, task, result);
        if (existingProjectionContext != null) {
            // There is already existing context for the same discriminator. Tolerate this only if
            // the deltas match. It is an error otherwise.
            ObjectDelta<ShadowType> primaryDelta = existingProjectionContext.getPrimaryDelta();
            if (primaryDelta == null) {
                throw new SchemaException("Attempt to add "+shadow+" to a focus that already contains "+
                        existingProjectionContext.getHumanReadableKind()+" of type '"+
                        existingProjectionContext.getResourceShadowDiscriminator().getIntent()+"' on "+
                        existingProjectionContext.getResource());
            }
            if (!primaryDelta.isAdd()) {
                throw new SchemaException("Conflicting changes in the context. " +
                        "Add of linkRef in the focus delta with embedded object conflicts with explicit delta "+primaryDelta);
            }
            if (!shadow.equals(primaryDelta.getObjectToAdd())) {
                throw new SchemaException("Conflicting changes in the context. " +
                        "Add of linkRef in the focus delta with embedded object is not adding the same object as explicit delta "+primaryDelta);
            }
            projectionContext = existingProjectionContext;
        } else {
            // Create account context from embedded object
            projectionContext = createNewProjectionContext(shadow, result);
        }
        // This is a new account that is to be added. So it should go to account primary delta.
        projectionContext.setPrimaryDeltaAfterStart(shadow.createAddDelta());
        projectionContext.setFullShadow(true);
        projectionContext.setExists(false);
        return projectionContext;
    }

    /**
     * We have OID. This is either linking of existing account or add of new account
     * therefore let's check for account existence to decide.
     */
    private @NotNull LensProjectionContext getOrCreateProjectionForValueToAddWithOid(@NotNull PrismReferenceValue refVal,
            @NotNull Holder<Boolean> objectDoesExistInRepoHolder, @NotNull OperationResult result)
            throws CommunicationException, SchemaException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, PolicyViolationException, ObjectNotFoundException {

        String oid = refVal.getOid();
        try {
            // Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
            // We need to fetch from provisioning and not repository so the correct definition will be set.
            var options =
                    SchemaService.get().getOperationOptionsBuilder()
                            .noFetch()
                            .futurePointInTime()
                            //.readOnly() [not yet]
                            .build();
            PrismObject<ShadowType> shadow = beans.provisioningService.getObject(ShadowType.class, oid, options, task, result);
            // Create account context from retrieved object
            LensProjectionContext projectionContext = getOrCreateProjectionContext(shadow.asObjectable(), result).context; // TODO what about shadowSet etc?
            projectionContext.setLoadedObject(shadow);
            projectionContext.setExists(ShadowUtil.isExists(shadow.asObjectable()));
            objectDoesExistInRepoHolder.setValue(true);
            return projectionContext;
        } catch (ObjectNotFoundException e) {
            //noinspection unchecked
            PrismObject<ShadowType> embeddedShadow = refVal.getObject();
            if (embeddedShadow == null) {
                // Account does not exist, and no composite account in ref -> this is really an error.
                throw e;
            } else {
                // New account (but with OID)
                result.muteLastSubresultError();
                if (!embeddedShadow.hasCompleteDefinition()) {
                    beans.provisioningService.applyDefinition(embeddedShadow, task, result);
                }
                // Create account context from embedded object
                LensProjectionContext projectionContext = createNewProjectionContext(embeddedShadow, result);
                projectionContext.setPrimaryDeltaAfterStart(embeddedShadow.createAddDelta());
                projectionContext.setFullShadow(true);
                projectionContext.setExists(false);
                projectionContext.setShadowExistsInRepo(false);
                return projectionContext;
            }
        }
    }

    private void getOrCreateContextsForValuesToDelete(@Nullable Collection<PrismReferenceValue> valuesToDelete,
            @NotNull OperationResult result) throws SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {
        for (PrismReferenceValue refVal : emptyIfNull(valuesToDelete)) {
            if (isInactive(refVal)) {
                LOGGER.trace("getOrCreateContextsForValuesToDelete: Skipping inactive linkRef to delete (relation={}): {}",
                        refVal.getRelation(), refVal);
            } else {
                LOGGER.trace("getOrCreateContextsForValuesToDelete: Processing value to delete: {}", refVal);
                getOrCreateContextForValueToDelete(refVal, result);
            }
        }
    }

    private void getOrCreateContextForValueToDelete(@NotNull PrismReferenceValue refVal, @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, PolicyViolationException {
        String oid = refVal.getOid();
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
            shadow = beans.provisioningService.getObject(ShadowType.class, oid, options, task, result);
            // Create account context from retrieved object
            projectionContext = getOrCreateProjectionContext(shadow.asObjectable(), result).context; // TODO what about shadowSet etc?
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
                shadow = beans.provisioningService.getObject(ShadowType.class, oid, options, task, result);
                projectionContext = getOrCreateEmptyGoneProjectionContext(oid);
                projectionContext.setFresh(true);
                projectionContext.setExists(false);
                projectionContext.setShadowExistsInRepo(false);
                LOGGER.trace("Loaded projection context: {}", projectionContext);
                OperationResult getObjectSubresult = result.getLastSubresult();
                getObjectSubresult.setErrorsHandled();
            } catch (ObjectNotFoundException ex) {
                // This is still OK. It means deleting an accountRef that points to non-existing object just log a warning
                LOGGER.warn("Deleting accountRef of " + focusContext.getObjectCurrent() + " that points to non-existing OID " + oid);
                return;
            }
        }
        if (refVal.getObject() == null) {
            projectionContext.setSynchronizationIntent(SynchronizationIntent.UNLINK);
        } else {
            // I.e. this is when we request to delete link containing full object.
            projectionContext.setSynchronizationIntent(SynchronizationIntent.DELETE);
            projectionContext.setPrimaryDeltaAfterStart(shadow.createDeleteDelta());
        }
        projectionContext.setFresh(true);
    }

    /**
     * Initializes contexts from sync deltas, if needed.
     */
    private void initializeContextsFromSyncDeltas(OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        for (LensProjectionContext projCtx : context.getProjectionContexts()) {
            if (projCtx.isFresh() && projCtx.getObjectCurrent() != null) {
                continue; // Already loaded
            }
            ObjectDelta<ShadowType> syncDelta = projCtx.getSyncDelta();
            if (syncDelta == null) {
                continue; // Nothing to apply
            }

            if (projCtx.isDoReconciliation()) {
                // Do not load old account now. It will get loaded later in the reconciliation step. Just mark it as fresh.
                projCtx.setFresh(true); // TODO is this correct?
                continue;
            }

            String oid = syncDelta.getOid();
            PrismObject<ShadowType> shadow;

            if (syncDelta.getChangeType() == ChangeType.ADD) {
                shadow = syncDelta.getObjectToAdd().clone();
                projCtx.setLoadedObject(shadow);
                projCtx.setExists(ShadowUtil.isExists(shadow.asObjectable()));

            } else {
                argCheck(oid != null, "No OID in non-ADD sync delta in %s", projCtx);

                // Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account
                // here. We need to fetch from provisioning and not repository so the correct definition will be set.
                var options = SchemaService.get().getOperationOptionsBuilder()
                        .doNotDiscovery()
                        .futurePointInTime()
                        //.readOnly() [not yet]
                        .build();
                try {
                    shadow = beans.provisioningService.getObject(ShadowType.class, oid, options, task, result);
                } catch (ObjectNotFoundException e) {
                    LOGGER.trace("Loading shadow {} from sync delta failed: not found", oid);
                    projCtx.setExists(false);
                    projCtx.clearCurrentObject();
                    projCtx.setShadowExistsInRepo(false);
                    shadow = null;
                }

                // We will not set old account if the delta is delete. The account does not really exists now.
                // (but the OID and resource will be set from the repo shadow)
                if (syncDelta.getChangeType() == ChangeType.DELETE) {
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
                        LensUtil.getResourceReadOnly(context, resourceOid, beans.provisioningService, task, result));
            }
            projCtx.setFresh(true);
        }
    }

    private LensProjectionContext getOrCreateEmptyGoneProjectionContext(String missingShadowOid) {
        LensProjectionContext projContext;

        LensProjectionContext existing = context.findProjectionContextByOid(missingShadowOid);
        if (existing != null) {
            projContext = existing;
        } else {
            projContext = context.createProjectionContext(null);
            projContext.setOid(missingShadowOid);
        }

        if (projContext.getResourceShadowDiscriminator() == null) {
            projContext.setResourceShadowDiscriminator(
                    new ResourceShadowDiscriminator(null, null, null, null, true));
        } else {
            projContext.markGone();
        }

        projContext.setFullShadow(false);
        projContext.clearCurrentObject();

        return projContext;
    }

    private Collection<SelectorOptions<GetOperationOptions>> createStandardShadowGetOptions() {
        // Using NO_FETCH so we avoid reading in a full account. This is more efficient as we don't need full account here.
        // We need to fetch from provisioning and not repository so the correct definition will be set.
        return SchemaService.get().getOperationOptionsBuilder()
                .noFetch()
                .futurePointInTime()
                //.readOnly() [not yet]
                .build();
    }

    @Experimental // This is a temporary solution: TODO factor out context acquisition into separate class.
    private static class ContextAcquisitionResult {
        private final LensProjectionContext context;
        private final boolean created; // TODO what do to with this?
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

    private void markShadowDead(String oid, OperationResult result) {
        if (oid == null) {
            // nothing to mark
            return;
        }
        Collection<? extends ItemDelta<?, ?>> modifications = MiscSchemaUtil.createCollection(
                beans.prismContext.deltaFactory().property().createReplaceDelta(beans.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class),
                        ShadowType.F_DEAD, true));
        try {
            beans.cacheRepositoryService.modifyObject(ShadowType.class, oid, modifications, result);
            // TODO report to task?
        } catch (ObjectNotFoundException e) {
            // Done already
            result.muteLastSubresultError();
        } catch (ObjectAlreadyExistsException | SchemaException e) {
            // Should not happen
            throw new SystemException(e.getMessage(), e);
        }
    }

    private LensProjectionContext createNewProjectionContext(PrismObject<ShadowType> shadowObject, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        ShadowType shadow = shadowObject.asObjectable();
        String resourceOid = java.util.Objects.requireNonNull(
                ShadowUtil.getResourceOid(shadow),
                () -> "The " + shadow + " has null resource reference OID");
        String intent = ShadowUtil.getIntent(shadow);
        ShadowKindType kind = ShadowUtil.getKind(shadow);
        ResourceType resource = LensUtil.getResourceReadOnly(context, resourceOid, beans.provisioningService, task, result);
        String accountIntent = LensUtil.refineProjectionIntent(kind, intent, resource);
        ResourceShadowDiscriminator rsd =
                new ResourceShadowDiscriminator(resourceOid, kind, accountIntent, shadow.getTag(), false);
        LensProjectionContext existingProjectionContext = context.findProjectionContext(rsd);
        if (existingProjectionContext != null) {
            throw new SchemaException("Attempt to add " + shadowObject + " to a focus that already contains projection of type '"
                    + accountIntent + "' on " + resource);
        }
        LensProjectionContext newProjectionContext = context.createProjectionContext(rsd);
        newProjectionContext.setResource(resource);
        newProjectionContext.setOid(shadowObject.getOid());
        return newProjectionContext;
    }

    /**
     * Gets or creates a projection context for given shadow.
     *
     * 1. First tries directly by shadow OID.
     * 2. If not successful, tries to get/create context by shadow RSD (checking also the conflict of projection contexts).
     */
    private ContextAcquisitionResult getOrCreateProjectionContext(@NotNull ShadowType shadow, @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        String resourceOid = MiscUtil.requireNonNull(
                ShadowUtil.getResourceOid(shadow),
                () -> "The " + shadow + " has null resource reference OID");

        LensProjectionContext existingContext = context.findProjectionContextByOid(shadow.getOid());
        LOGGER.trace("Projection context by shadow OID: {} yielded: {}", shadow.getOid(), existingContext);
        if (existingContext != null) {
            return ContextAcquisitionResult.existing(existingContext);
        } else {
            return getOrCreateProjectionContextByRsd(shadow, resourceOid, result);
        }
    }

    private @NotNull ContextAcquisitionResult getOrCreateProjectionContextByRsd(@NotNull ShadowType shadow,
            @NotNull String resourceOid, @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {

        ResourceShadowDiscriminator rsd = createResourceShadowDiscriminator(shadow, resourceOid, result);

        LensUtil.GetOrCreateProjectionContextResult projCtxResult = LensUtil.getOrCreateProjectionContext(context, rsd);
        LOGGER.trace("Projection context for {}: {}", rsd, projCtxResult);

        LensProjectionContext projCtx = projCtxResult.context;
        if (projCtx.getOid() != null && shadow.getOid() != null && !projCtx.getOid().equals(shadow.getOid())) {
            assert !projCtxResult.created;
            return treatProjectionContextConflict(shadow, rsd, projCtx, result);
        } else {
            if (projCtx.getOid() == null) {
                projCtx.setOid(shadow.getOid());
            }
            return new ContextAcquisitionResult(projCtx, projCtxResult.created, false);
        }
    }

    /**
     * Resolves a conflict: a project context was found but it belongs to a different shadow OID.
     *
     * We have existing projection and another projection that was added (with the same discriminator).
     * Chances are that the old object is already deleted (e.g. during rename). So let's be
     * slightly inefficient here and check for resource object existence.
     *
     * There is a small amount of magic when dealing with the objects that no longer exist in repo.
     */
    @NotNull
    private ContextAcquisitionResult treatProjectionContextConflict(@NotNull ShadowType shadow,
            @NotNull ResourceShadowDiscriminator rsd, @NotNull LensProjectionContext existingCtx,
            @NotNull OperationResult result)
            throws CommunicationException, SchemaException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, PolicyViolationException {

        LOGGER.trace("Projection conflict detected, existing: {}, new {}", existingCtx.getOid(), shadow.getOid());
        try {
            var opts = SchemaService.get().getOperationOptionsBuilder()
                    .doNotDiscovery()
                    .futurePointInTime()
                    //.readOnly() [not yet]
                    .build();
            LOGGER.trace("Loading resource object corresponding to the existing projection ({})", existingCtx.getOid());
            PrismObject<ShadowType> objectForExistingCtx =
                    beans.provisioningService.getObject(ShadowType.class, existingCtx.getOid(), opts, task, result);

            // Maybe it is the other way around
            try {
                LOGGER.trace("Loading resource object corresponding to the newly added projection ({})", shadow.getOid());
                PrismObject<ShadowType> objectForNewCtx =
                        beans.provisioningService.getObject(ShadowType.class, shadow.getOid(), opts, task, result);

                // Obviously, two resource objects with the same discriminator exist.
                LOGGER.trace("Projection {} already exists in context\nExisting:\n{}\nNew:\n{}", rsd,
                        objectForExistingCtx.debugDumpLazily(1), objectForNewCtx.debugDumpLazily(1));

                if (!ShadowUtil.isDead(objectForNewCtx.asObjectable())) {
                    throw new PolicyViolationException("Projection " + rsd + " already exists in context (existing " +
                            objectForExistingCtx + ", new " + shadow);
                }

                // Dead shadow for the new context. This is somehow expected, fix it and we can go on.
                rsd.setGone(true);

                // Let us create or find the "newest" context, i.e. context for rsd updated with gone=true.
                // We will use/reuse it with no other checks.
                LensUtil.GetOrCreateProjectionContextResult newestCtxResult = LensUtil.getOrCreateProjectionContext(context, rsd);
                LensProjectionContext newestCtx = newestCtxResult.context;
                newestCtx.setExists(ShadowUtil.isExists(objectForNewCtx.asObjectable()));
                newestCtx.setFullShadow(false);
                newestCtx.setLoadedObject(objectForNewCtx); // TODO ok even if we reused existing context?
                newestCtx.setOid(objectForNewCtx.getOid());
                return new ContextAcquisitionResult(newestCtx, newestCtxResult.created, true);

            } catch (ObjectNotFoundException e) {

                // Object for the new context does not exist. It looks like that this exception means that it does not
                // exist in repository - not just on resource. (See ObjectNotFoundHandler in provisioning.)
                result.muteLastSubresultError();

                // We have to create new context in this case, but it has to have "gone" set.
                rsd.setGone(true);

                // Let us create or find the "newest" context, i.e. context for rsd updated with gone=true.
                LensUtil.GetOrCreateProjectionContextResult newestCtxResult = LensUtil.getOrCreateProjectionContext(context, rsd);
                LensProjectionContext newestCtx = newestCtxResult.context;

                // We have to mark the shadow as dead right now, otherwise the uniqueness check may fail.
                // (This is suspicious because we believe the object does not exist in repo.)
                markShadowDead(shadow.getOid(), result);

                newestCtx.setShadowExistsInRepo(false);

                // We return the result with shadowSet=true: It means that there's no need to set the shadow by caller.
                return new ContextAcquisitionResult(newestCtx, newestCtxResult.created, true);
            }
        } catch (ObjectNotFoundException e) {

            // Object for the existing context does not exist (in repo).
            // This is somehow expected, fix it and we can go on.

            result.muteLastSubresultError();
            String shadowOid = existingCtx.getOid();
            existingCtx.getResourceShadowDiscriminator().setGone(true);

            // Let us again try to create or find the "newest" context. The conflicting context is now set as gone.
            LensUtil.GetOrCreateProjectionContextResult newestCtxResult = LensUtil.getOrCreateProjectionContext(context, rsd);
            LensProjectionContext newestCtx = newestCtxResult.context;
            newestCtx.setShadowExistsInRepo(false);

            // We have to mark it as dead right now, otherwise the uniqueness check may fail.
            // (This is suspicious because we believe the object does not exist in repo.)
            markShadowDead(shadowOid, result);

            // We return the result with shadowSet=true: It means that there's no need to set the shadow by caller.
            return new ContextAcquisitionResult(newestCtx, newestCtxResult.created, true);
        }
    }

    private @NotNull ResourceShadowDiscriminator createResourceShadowDiscriminator(@NotNull ShadowType shadow,
            @NotNull String resourceOid, @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        String intent = ShadowUtil.getIntent(shadow);
        ShadowKindType kind = ShadowUtil.getKind(shadow);
        ResourceType resource = LensUtil.getResourceReadOnly(context, resourceOid, beans.provisioningService, task, result);
        intent = LensUtil.refineProjectionIntent(kind, intent, resource);
        return new ResourceShadowDiscriminator(resourceOid, kind, intent, shadow.getTag(), ShadowUtil.isGone(shadow));
    }
}
