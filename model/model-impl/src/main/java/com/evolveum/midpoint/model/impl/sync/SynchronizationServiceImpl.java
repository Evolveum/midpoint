/*

 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import static com.evolveum.midpoint.prism.PrismObject.asObjectable;
import static com.evolveum.midpoint.schema.internals.InternalsConfig.consistencyChecks;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationExclusionReasonType.*;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.reactions.SynchronizationActionExecutor;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Synchronization service receives change notifications from provisioning. It
 * decides which synchronization policy to use and evaluates it (correlation,
 * confirmation, situations, reaction, ...)
 * <p>
 * Note: don't autowire this bean by implementing class, as it is
 * proxied by Spring AOP. Use the interface instead.
 *
 * TODO improve the error handling for the whole class
 *
 * @author lazyman
 * @author Radovan Semancik
 */
@Service(value = "synchronizationService")
public class SynchronizationServiceImpl implements SynchronizationService {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationServiceImpl.class);

    private static final String CLASS_NAME_WITH_DOT = SynchronizationServiceImpl.class.getName() + ".";

    private static final String OP_SETUP_SITUATION = CLASS_NAME_WITH_DOT + "setupSituation";
    private static final String OP_NOTIFY_CHANGE = CLASS_NAME_WITH_DOT + "notifyChange";

    @Autowired private PrismContext prismContext;
    @Autowired private ModelBeans beans;
    @Autowired private SynchronizationContextLoader syncContextLoader;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    @Override
    public void notifyChange(
            @NotNull ResourceObjectShadowChangeDescription change,
            @NotNull Task task,
            @NotNull OperationResult parentResult) {

        OperationResult result = parentResult.subresult(OP_NOTIFY_CHANGE)
                .addArbitraryObjectAsParam("change", change)
                .addArbitraryObjectAsContext("task", task)
                .build();

        try {
            logStart(change);
            checkConsistence(change);

            // The sorter is evaluated in the following method call
            SynchronizationContext<?> syncCtx = syncContextLoader.loadSynchronizationContextFromChange(change, task, result);

            if (shouldSkipSynchronization(syncCtx, result)) {
                return; // sync metadata are saved by the above method
            }
            SynchronizationContext.Complete<?> completeCtx = (SynchronizationContext.Complete<?>) syncCtx;

            setupLinkedOwnerAndSituation(completeCtx, change, result);

            completeCtx.recordSyncStart();

            completeCtx.getUpdater()
                    .updateAllSyncMetadata()
                    .commit(result);

            if (!completeCtx.isDryRun()) {
                new SynchronizationActionExecutor<>(completeCtx)
                        .react(result);
                // Note that exceptions from action execution are not propagated here.
            }

            LOGGER.debug("SYNCHRONIZATION: DONE (mode '{}') for {}",
                    completeCtx.getExecutionMode(), completeCtx.getShadowedResourceObject());

        } catch (SystemException ex) {
            // avoid unnecessary re-wrap
            result.recordFatalError(ex);
            throw ex;
        } catch (Exception ex) {
            result.recordFatalError(ex);
            throw new SystemException(ex);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void logStart(@NotNull ResourceObjectShadowChangeDescription change) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("SYNCHRONIZATION: received change notification:\n{}", DebugUtil.debugDump(change, 1));
        } else if (isLogDebug(change)) {
            LOGGER.debug("SYNCHRONIZATION: received change notification {}", change);
        }
    }

    /**
     * TODO: Consider situations when one account belongs to two different users. It should correspond to
     *  the {@link SynchronizationSituationType#DISPUTED} situation.
     */
    @Nullable
    private PrismObject<FocusType> findShadowOwner(String shadowOid, OperationResult result) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(FocusType.class)
                .item(FocusType.F_LINK_REF).ref(shadowOid, null, PrismConstants.Q_ANY)
                .build();
        // TODO read-only later
        SearchResultList<PrismObject<FocusType>> owners =
                repositoryService.searchObjects(FocusType.class, query, null, result);

        if (owners.isEmpty()) {
            return null;
        }
        if (owners.size() > 1) {
            LOGGER.warn("Found {} owners for shadow oid {}, returning first owner.", owners.size(), shadowOid);
        }
        return owners.get(0);
    }

    /**
     * Checks for common reasons to skip synchronization:
     *
     * - no applicable synchronization policy,
     * - synchronization disabled,
     * - protected resource object.
     */
    private boolean shouldSkipSynchronization(SynchronizationContext<?> syncCtx, OperationResult result)
            throws SchemaException {
        if (!syncCtx.isComplete()) {
            // This means that either the shadow is not classified, or there is no type definition nor sync section
            // for its type (kind/intent).
            String message = String.format(
                    "SYNCHRONIZATION no applicable synchronization policy and/or type definition for %s (%s) on %s, "
                            + "ignoring change from channel %s",
                    syncCtx.getShadowedResourceObject(),
                    syncCtx.getShadowedResourceObject().getObjectClass(),
                    syncCtx.getResource(),
                    syncCtx.getChannel());
            LOGGER.debug(message);
            syncCtx.getUpdater()
                    .updateBothSyncTimestamps() // TODO should we really record this as full synchronization?
                    .commit(result);
            result.recordNotApplicable(message);
            syncCtx.recordSyncExclusion(NO_SYNCHRONIZATION_POLICY);
            return true;
        }

        if (!syncCtx.isSynchronizationEnabled()) {
            String message = String.format(
                    "SYNCHRONIZATION is not enabled for %s, ignoring change from channel %s",
                    syncCtx.getResource(), syncCtx.getChannel());
            LOGGER.debug(message);
            syncCtx.getUpdater()
                    .updateBothSyncTimestamps() // TODO should we really record this as full synchronization?
                    .updateCoordinatesIfMissing()
                    .commit(result);
            result.recordNotApplicable(message);
            syncCtx.recordSyncExclusion(SYNCHRONIZATION_DISABLED);
            return true;
        }

        if (syncCtx.isProtected()) {
            String message = String.format(
                    "SYNCHRONIZATION is skipped for protected shadow %s, ignoring change from channel %s",
                    syncCtx.getShadowedResourceObject(), syncCtx.getChannel());
            LOGGER.debug(message);
            syncCtx.getUpdater()
                    .updateBothSyncTimestamps() // TODO should we really record this as full synchronization?
                    .updateCoordinatesIfMissing()
                    .commit(result);
            result.recordNotApplicable(message);
            syncCtx.recordSyncExclusion(PROTECTED);
            return true;
        }

        return false;
    }

    private void checkConsistence(ResourceObjectShadowChangeDescription change) {
        Validate.notNull(change, "Resource object shadow change description must not be null.");
        Validate.notNull(change.getShadowedResourceObject(), "Current shadow must not be null.");
        Validate.notNull(change.getResource(), "Resource in change must not be null.");

        if (consistencyChecks) {
            change.checkConsistence();
        }
    }

    private <F extends FocusType> void setupLinkedOwnerAndSituation(SynchronizationContext.Complete<F> syncCtx,
            ResourceObjectShadowChangeDescription change, OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.subresult(OP_SETUP_SITUATION)
                .setMinor()
                .addArbitraryObjectAsParam("syncCtx", syncCtx)
                .addArbitraryObjectAsParam("change", change)
                .build();

        LOGGER.trace("Determining situation for resource object shadow. Focus class: {}. Applicable policy: {}.",
                syncCtx.getFocusClass(), syncCtx.getPolicyName());

        try {
            findLinkedOwner(syncCtx, change, result);

            if (syncCtx.getLinkedOwner() == null || syncCtx.isCorrelatorsUpdateRequested()) {
                determineSituationWithCorrelators(syncCtx, change, result); // TODO change the name (if sorter is used)
            } else {
                determineSituationWithoutCorrelators(syncCtx, change, result);
            }
            logSituation(syncCtx, change);
        } catch (Exception ex) {
            result.recordFatalError(ex);
            LOGGER.error("Error occurred during resource object shadow owner lookup.");
            throw new SystemException(
                    "Error occurred during resource object shadow owner lookup, reason: " + ex.getMessage(), ex);
        } finally {
            result.close();
        }
    }

    private <F extends FocusType> void findLinkedOwner(SynchronizationContext<F> syncCtx,
            ResourceObjectShadowChangeDescription change, OperationResult result) throws SchemaException {

        if (syncCtx.getLinkedOwner() != null) {
            // TODO This never occurs. Clarify!
            return;
        }

        PrismObject<FocusType> owner = findShadowOwner(change.getShadowOid(), result);
        //noinspection unchecked
        syncCtx.setLinkedOwner((F) asObjectable(owner));
    }

    private <F extends FocusType> void determineSituationWithoutCorrelators(SynchronizationContext<F> syncCtx,
            ResourceObjectShadowChangeDescription change, OperationResult result) throws ConfigurationException {

        assert syncCtx.getLinkedOwner() != null;

        checkLinkedAndCorrelatedOwnersMatch(syncCtx, result);

        if (change.isDelete()) {
            syncCtx.setSituationIfNull(SynchronizationSituationType.DELETED);
        } else {
            syncCtx.setSituationIfNull(SynchronizationSituationType.LINKED);
        }
    }

    private <F extends FocusType> void checkLinkedAndCorrelatedOwnersMatch(SynchronizationContext<F> syncCtx,
            OperationResult result) throws ConfigurationException {
        F linkedOwner = syncCtx.getLinkedOwner();
        F correlatedOwner = syncCtx.getCorrelatedOwner(); // may be null; or may be provided by sync sorter

        LOGGER.trace("Shadow {} has linked owner: {}, correlated owner: {}", syncCtx.getShadowedResourceObject(),
                linkedOwner, correlatedOwner);

        if (correlatedOwner != null && linkedOwner != null && !correlatedOwner.getOid().equals(linkedOwner.getOid())) {
            LOGGER.error("Cannot synchronize {}, linked owner and expected owner are not the same. "
                    + "Linked owner: {}, expected owner: {}", syncCtx.getShadowedResourceObject(), linkedOwner, correlatedOwner);
            String msg = "Cannot synchronize " + syncCtx.getShadowedResourceObject()
                    + ", linked owner and expected owner are not the same. Linked owner: " + linkedOwner
                    + ", expected owner: " + correlatedOwner;
            result.recordFatalError(msg);
            throw new ConfigurationException(msg);
        }
    }

    /**
     * EITHER (todo update the description):
     *
     * account is not linked to user. you have to use correlation and
     * confirmation rule to be sure user for this account doesn't exists
     * resourceShadow only contains the data that were in the repository before
     * the change. But the correlation/confirmation should work on the updated
     * data. Therefore let's apply the changes before running
     * correlation/confirmation
     *
     * OR
     *
     * We need to update the correlator state.
     */
    private <F extends FocusType> void determineSituationWithCorrelators(SynchronizationContext<F> syncCtx,
            ResourceObjectShadowChangeDescription change, OperationResult result)
            throws CommonException {

        if (change.isDelete()) {
            // account was deleted and it was not linked; there is nothing to do (not even updating the correlators)
            syncCtx.setSituationIfNull(SynchronizationSituationType.DELETED);
            return;
        }

        if (!syncCtx.isCorrelatorsUpdateRequested() && syncCtx.getCorrelatedOwner() != null) { // e.g. from sync sorter
            LOGGER.trace("Correlated owner present in synchronization context: {}", syncCtx.getCorrelatedOwner());
            syncCtx.setSituationIfNull(SynchronizationSituationType.UNLINKED);
            return;
        }

        PrismObject<? extends ShadowType> resourceObject = change.getShadowedResourceObject();
        ResourceType resource = change.getResource().asObjectable();
        setupResourceRefInShadowIfNeeded(resourceObject.asObjectable(), resource);

        evaluatePreMappings(syncCtx, result);

        if (syncCtx.isUpdatingCorrelatorsOnly()) {
            new CorrelationProcessing<>(syncCtx, beans)
                    .update(result);
            return;
        }

        CorrelationResult correlationResult =
                new CorrelationProcessing<>(syncCtx, beans)
                        .correlate(result);

        LOGGER.debug("Correlation result:\n{}", correlationResult.debugDumpLazily(1));

        SynchronizationSituationType state;
        F owner;
        switch (correlationResult.getSituation()) {
            case EXISTING_OWNER:
                state = SynchronizationSituationType.UNLINKED;
                //noinspection unchecked
                owner = (F) correlationResult.getOwner();
                break;
            case NO_OWNER:
                state = SynchronizationSituationType.UNMATCHED;
                owner = null;
                break;
            case UNCERTAIN:
            case ERROR:
                state = SynchronizationSituationType.DISPUTED;
                owner = null;
                break;
            default:
                throw new AssertionError(correlationResult.getSituation());
        }
        LOGGER.debug("Determined synchronization situation: {} with owner: {}", state, owner);

        syncCtx.setCorrelatedOwner(owner);
        syncCtx.setSituationIfNull(state);

        if (correlationResult.isError()) {
            // This is a very crude and preliminary error handling: we just write pending deltas to the shadow
            // (if there are any), to have a record of the unsuccessful correlation. Normally, we should do this
            // along with the other sync metadata. But the error handling in this class is not ready for it (yet).
            syncCtx.getUpdater().commit(result);
            correlationResult.throwCommonOrRuntimeExceptionIfPresent();
            throw new AssertionError("Not here");
        }
    }

    private <F extends FocusType> void evaluatePreMappings(SynchronizationContext<F> syncCtx, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        new PreMappingsEvaluation<>(syncCtx, beans)
                .evaluate(result);
    }

    // This is maybe not needed
    private void setupResourceRefInShadowIfNeeded(ShadowType shadow, ResourceType resource) {
        if (shadow.getResourceRef() == null) {
            ObjectReferenceType reference = new ObjectReferenceType();
            reference.setOid(resource.getOid());
            reference.setType(ResourceType.COMPLEX_TYPE);
            shadow.setResourceRef(reference);
        }
    }

    private <F extends FocusType> void logSituation(SynchronizationContext<F> syncCtx, ResourceObjectShadowChangeDescription change) {
        String syncSituationValue = syncCtx.getSituation() != null ? syncCtx.getSituation().value() : null;
        if (isLogDebug(change)) {
            LOGGER.debug("SYNCHRONIZATION: SITUATION: '{}', currentOwner={}, correlatedOwner={}",
                    syncSituationValue, syncCtx.getLinkedOwner(),
                    syncCtx.getCorrelatedOwner());
        } else {
            LOGGER.trace("SYNCHRONIZATION: SITUATION: '{}', currentOwner={}, correlatedOwner={}",
                    syncSituationValue, syncCtx.getLinkedOwner(),
                    syncCtx.getCorrelatedOwner());
        }
    }

    @Override
    public String getName() {
        return "model synchronization service";
    }

    private static boolean isLogDebug(ResourceObjectShadowChangeDescription change) {
        // Reconciliation changes are routine. Do not let them pollute the log files.
        return !SchemaConstants.CHANNEL_RECON_URI.equals(change.getSourceChannel());
    }
}
