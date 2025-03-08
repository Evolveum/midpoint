/*

 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import static com.evolveum.midpoint.prism.PrismObject.asObjectable;
import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;
import static com.evolveum.midpoint.schema.internals.InternalsConfig.consistencyChecks;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationExclusionReasonType.*;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.correlation.CompleteCorrelationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.reactions.SynchronizationActionExecutor;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

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
    @Autowired private SynchronizationContextCreator syncContextCreator;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private Clock clock;

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

            // Object type and synchronization policy are determined here. Sorter is evaluated, if present.
            SynchronizationContext<?> syncCtx = syncContextCreator.createSynchronizationContext(change, task, result);

            syncCtx.getUpdater().updateCoordinates();

            var skipSyncReason = shouldSkipSynchronization(syncCtx, result);
            if (skipSyncReason != null) {
                // sync metadata updates are prepared by the above method
                syncCtx.getUpdater().commit(result);
                result.recordNotApplicable(skipSyncReason);
                return;
            }
            // FIXME: Somewhere here we should validate preFocus

            SynchronizationContext.Complete<?> completeCtx = (SynchronizationContext.Complete<?>) syncCtx;
            setupLinkedOwnerAndSituation(completeCtx, change, result);

            completeCtx.recordSyncStartInTask();
            completeCtx.getUpdater()
                    .updateAllSyncMetadataRespectingMode()
                    .commit(result);

            boolean synchronizationFailure;
            if (completeCtx.isNotDryRunLikeMode()) {
                synchronizationFailure =
                        new SynchronizationActionExecutor<>(completeCtx)
                                .react(result);
                // Note that standard exceptions from action execution are not re-thrown to here; in such cases,
                // the processing continues normally here, with synchronizationFailure set to true.
            } else {
                synchronizationFailure = false;
            }

            if (completeCtx.isExecutionFullyPersistent() && !synchronizationFailure) {
                completeCtx.getUpdater()
                        .updateFullSyncTimestamp(clock.currentTimeXMLGregorianCalendar())
                        .commit(result);
            }

            LOGGER.debug("SYNCHRONIZATION: DONE (mode '{}') for {}",
                    completeCtx.getExecutionMode(), completeCtx.getShadowLikeValue());

        } catch (SystemException ex) {
            // avoid unnecessary re-wrap
            result.recordException(ex);
            throw ex;
        } catch (Exception ex) {
            result.recordException(ex);
            throw new SystemException(ex);
        } finally {
            result.close();
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
    private <F extends FocusType> @Nullable F findLinkedOwner(SynchronizationContext.Complete<F> syncCtx, OperationResult result)
            throws SchemaException {

        ShadowType shadow = syncCtx.getShadowedResourceObject();

        var owners = repositoryService.searchObjects(
                FocusType.class,
                prismContext.queryFor(FocusType.class)
                        .item(FocusType.F_LINK_REF)
                        .ref(shadow.getOid(), null, PrismConstants.Q_ANY)
                        .build(),
                readOnly(),
                result);

        if (owners.isEmpty()) {
            return null;
        }

        if (owners.size() > 1) {
            LOGGER.warn("Found {} owners for {}, returning first owner: {}", owners.size(), shadow, owners);
        }

        FocusType owner = asObjectable(owners.get(0));

        Class<F> expectedClass = syncCtx.getFocusClass();
        if (expectedClass.isAssignableFrom(owner.getClass())) {
            //noinspection unchecked
            return (F) owner;
        } else {
            throw new SchemaException(
                    String.format("Expected owner of type %s but %s was found instead; for %s",
                            expectedClass.getSimpleName(), owner, shadow));
        }
    }

    /**
     * Checks for common reasons to skip synchronization:
     *
     * - no applicable synchronization policy (~ incomplete context),
     * - synchronization disabled,
     * - protected resource object.
     *
     * TODO open question: should we record skipped objects as "fully synchronized"? (currently in all cases except for !visible)
     *  Why not: technically, this is NOT a full synchronization
     *  Why yes: we want to avoid re-processing of these objects in 3rd stage of reconciliation (that looks after full sync ts)
     *  For the time being, let us keep this. But should decide on it some day.
     *
     * @return non-null reason for sync skip (or null if it should not be skipped)
     */
    private String shouldSkipSynchronization(SynchronizationContext<?> syncCtx, OperationResult result)
            throws SchemaException, ConfigurationException {
        ShadowType shadow = syncCtx.getShadowedResourceObject();
        QName objectClass = shadow.getObjectClass();
        ResourceType resource = syncCtx.getResource();
        String channel = syncCtx.getChannel();

        if (!syncCtx.isVisible()) {
            String message = String.format(
                    "SYNCHRONIZATION the synchronization policy for %s (%s) on %s is not visible, ignoring change from channel %s",
                    shadow, objectClass, resource, channel);
            LOGGER.debug(message);
            syncCtx.getUpdater().updateBasicSyncTimestamp();
            syncCtx.recordSyncExclusionInTask(NO_SYNCHRONIZATION_POLICY); // at least temporary
            return message;
        }

        if (!syncCtx.isComplete()) {
            // This means that either the shadow is not classified, or there is no type definition nor sync section
            // for its type (kind/intent).
            String message = String.format(
                    "SYNCHRONIZATION no applicable synchronization policy and/or type definition for %s (%s) on %s, "
                            + "ignoring change from channel %s", shadow, objectClass, resource, channel);
            LOGGER.debug(message);
            syncCtx.getUpdater().updateBothSyncTimestamps(); // TODO see the above question on full sync timestamp
            syncCtx.recordSyncExclusionInTask(NO_SYNCHRONIZATION_POLICY);
            return message;
        }

        if (!syncCtx.isSynchronizationEnabled()) {
            String message = String.format(
                    "SYNCHRONIZATION is not enabled for %s, ignoring change from channel %s", resource, channel);
            LOGGER.debug(message);
            syncCtx.getUpdater().updateBothSyncTimestamps(); // TODO see the above question on full sync timestamp
            syncCtx.recordSyncExclusionInTask(SYNCHRONIZATION_DISABLED);
            return message;
        }

        if (syncCtx.isMarkedSkipSynchronization(result) || syncCtx.isProtected()) {
            String message = String.format(
                    "SYNCHRONIZATION is skipped for marked/protected shadow %s, ignoring change from channel %s", shadow, channel);
            LOGGER.debug(message);
            syncCtx.getUpdater().updateBothSyncTimestamps(); // TODO see the above question on full sync timestamp
            syncCtx.recordSyncExclusionInTask(PROTECTED);
            return message;
        }

        return null;
    }

    private void checkConsistence(ResourceObjectShadowChangeDescription change) {
        Validate.notNull(change, "Resource object shadow change description must not be null.");
        Validate.notNull(change.getShadowedResourceObject(), "Current shadow must not be null.");
        Validate.notNull(change.getResource(), "Resource in change must not be null.");

        if (consistencyChecks) {
            change.checkConsistence();
        }
    }

    private <F extends FocusType> void setupLinkedOwnerAndSituation(
            SynchronizationContext.Complete<F> syncCtx,
            ResourceObjectShadowChangeDescription change,
            OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.subresult(OP_SETUP_SITUATION)
                .setMinor()
                .addArbitraryObjectAsParam("syncCtx", syncCtx)
                .addArbitraryObjectAsParam("change", change)
                .build();

        LOGGER.trace("Determining situation for resource object shadow. Focus class: {}. Applicable policy: {}.",
                syncCtx.getFocusClass(), syncCtx.getPolicyName());

        try {
            F linkedOwner = findLinkedOwner(syncCtx, result);
            syncCtx.setLinkedOwner(linkedOwner);

            if (linkedOwner == null || syncCtx.isCorrelatorsUpdateRequested()) {
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
        var shadow = syncCtx.getShadowedResourceObject();

        LOGGER.trace("Shadow {} has linked owner: {}, correlated owner: {}", shadow, linkedOwner, correlatedOwner);

        if (correlatedOwner != null && linkedOwner != null && !correlatedOwner.getOid().equals(linkedOwner.getOid())) {
            LOGGER.error("Cannot synchronize {}, linked owner and expected owner are not the same. "
                    + "Linked owner: {}, expected owner: {}", shadow, linkedOwner, correlatedOwner);
            String msg = "Cannot synchronize " + shadow
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
    private <F extends FocusType> void determineSituationWithCorrelators(
            SynchronizationContext.Complete<F> syncCtx, ResourceObjectShadowChangeDescription change, OperationResult result)
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

        setupResourceRefInShadowIfNeeded(change);

        PreMappingsEvaluator.computePreFocus(syncCtx, result);
        setObjectTemplateForCorrelation(syncCtx, syncCtx.getTask(), result);

        if (syncCtx.isUpdatingCorrelatorsOnly()) {
            new CorrelationProcessing<>(syncCtx, beans)
                    .update(result);
            return;
        }

        CompleteCorrelationResult correlationResult =
                new CorrelationProcessing<>(syncCtx, beans)
                        .correlate(result);

        LOGGER.debug("Correlation result:\n{}", correlationResult.debugDumpLazily(1));

        SynchronizationState<F> syncState = SynchronizationState.fromCorrelationResult(correlationResult);
        LOGGER.debug("Determined synchronization state: {}", syncState);

        syncCtx.setCorrelatedOwner(syncState.owner());
        syncCtx.setSituationIfNull(syncState.situation());

        if (correlationResult.isError()) {
            // This is a very crude and preliminary error handling: we just write pending deltas to the shadow
            // (if there are any), to have a record of the unsuccessful correlation. Normally, we should do this
            // along with the other sync metadata. But the error handling in this class is not ready for it (yet).
            syncCtx.getUpdater().commit(result);
            correlationResult.throwCommonOrRuntimeExceptionIfPresent();
            throw new AssertionError("Not here");
        }
    }

    private <F extends FocusType> void setObjectTemplateForCorrelation(
            SynchronizationContext.Complete<F> syncCtx, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        syncCtx.setObjectTemplateForCorrelation(
                beans.correlationServiceImpl.determineObjectTemplate(
                        syncCtx.getArchetypeOid(),
                        syncCtx.getPreFocus(),
                        null,
                        task,
                        result));
    }

    // This is maybe not needed
    private void setupResourceRefInShadowIfNeeded(ResourceObjectShadowChangeDescription change) {
        ShadowType shadowedResourceObject = change.getShadowedResourceObject().asObjectable();

        if (shadowedResourceObject.getResourceRef() == null) {
            shadowedResourceObject.setResourceRef(
                    ObjectTypeUtil.createObjectRef(
                            change.getResource()));
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
