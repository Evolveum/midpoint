/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsUtil.createSuccessOperationDescription;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_PASSWORD_VALUE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.RepoShadowModifications;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.OperationResultUtil;
import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * The `refresh` operation - either full or quick.
 */
class ShadowRefreshOperation {

    private static final String OP_OPERATION_RETRY = ShadowsFacade.class.getName() + ".operationRetry";

    private static final Trace LOGGER = TraceManager.getTrace(ShadowRefreshOperation.class);

    @NotNull private final ShadowsLocalBeans b = ShadowsLocalBeans.get();

    /** Shadow-specific provisioning context. */
    @NotNull private final ProvisioningContext ctx;

    /** The shadow being refreshed. The value may change but should never be `null`. */
    @NotNull private RepoShadow shadow;

    /** ODOs for retried pending operations. */
    @NotNull private final Collection<ObjectDeltaOperation<ShadowType>> retriedOperations = new ArrayList<>();

    /** The overall status of the refresh operation. */
    private OperationResultStatus retriedOperationsResultStatus;

    /** Original options for the embedding operation (like "modify"). */
    private final ProvisioningOperationOptions options;

    private ShadowRefreshOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow shadow,
            @Nullable ProvisioningOperationOptions options) {
        this.ctx = ctx;
        this.shadow = shadow;
        this.options = options;
    }

    static @NotNull ShadowRefreshOperation executeFull(
            @NotNull RepoShadow repoShadow,
            ProvisioningOperationOptions options,
            ProvisioningOperationContext context,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException {
        ProvisioningContext ctx = ShadowsLocalBeans.get().ctxFactory.createForRepoShadow(repoShadow, task);
        ctx.applyCurrentDefinition(repoShadow.getBean()); // TODO is this necessary?
        return executeFullInternal(ctx, repoShadow, options, context, result);
    }

    static void executeFull(
            @NotNull RawRepoShadow rawRepoShadow,
            ProvisioningOperationOptions options,
            ProvisioningOperationContext context,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ProvisioningContext ctx = ShadowsLocalBeans.get().ctxFactory.createForShadow(rawRepoShadow.getBean(), task, result);
        var repoShadow = ctx.adoptRawRepoShadow(rawRepoShadow);
        executeFullInternal(ctx, repoShadow, options, context, result);
    }

    private static @NotNull ShadowRefreshOperation executeFullInternal(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            ProvisioningOperationOptions options,
            ProvisioningOperationContext context,
            OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException {
        ctx.setOperationContext(context);
        ctx.assertDefinition();

        var op = new ShadowRefreshOperation(ctx, repoShadow, options);
        op.executeFull(result);
        return op;
    }

    /**
     * Used to quickly and efficiently refresh shadow before GET operations.
     */
    static @NotNull RepoShadow executeQuick(ProvisioningContext ctx, RepoShadow repoShadow, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        var op = new ShadowRefreshOperation(ctx, repoShadow, null);
        op.executeQuick(result);
        return op.shadow;
    }

    private void executeFull(OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException,
            ExpressionEvaluationException {
        LOGGER.trace("Fully refreshing {}", shadow);

        try {
            b.shadowUpdater.refreshProvisioningIndexes(ctx, shadow, true, result);
        } catch (ObjectAlreadyExistsException e) {
            throw SystemException.unexpected(e, "when refreshing provisioning indexes");
        }

        if (!ctx.isExecutionFullyPersistent()) {
            // Unlike other places related to the simulation mode, we do not throw an exception here. The shadow refresh may be
            // invoked in various situations, and it is not sure that the caller(s) have full responsibility of these. Hence, we
            // silently ignore these requests here.
            LOGGER.trace("Skipping refresh of {} pending operations because the task is in simulation mode", shadow);
            return;
        }

        processPendingOperations(result);
        deleteDeadShadowIfPossible(result);
        if (shadow.isDeleted()) {
            LOGGER.trace("updateProvisioningIndexesAfterDeletion: no shadow");
        } else {
            updateProvisioningIndexesAfterDeletion(result);
            updateCachedPasswordIfNeeded(result);
        }
    }

    private void executeQuick(OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        LOGGER.trace("Quickly refreshing {}", shadow);

        ObjectDelta<ShadowType> shadowDelta = shadow.getPrismObject().createModifyDelta();
        expirePendingOperations(shadowDelta);
        RepoShadowModifications repoShadowModifications = RepoShadowModifications.of(shadowDelta.getModifications());
        b.shadowUpdater.executeRepoShadowModifications(ctx, shadow, repoShadowModifications, result);

        deleteDeadShadowIfPossible(result);
    }

    private void processPendingOperations(OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException {

        boolean isDead = shadow.isDead();
        if (!isDead && !shadow.hasPendingOperations()) {
            LOGGER.trace(
                    "Skipping refresh of {} pending operations because shadow is not dead and there are no pending operations",
                    shadow);
            return;
        }

        if (ctx.isInMaintenance()) {
            LOGGER.trace("Skipping refresh of {} pending operations because resource is in the maintenance mode", shadow);
            return;
        }

        PendingOperations sortedOperations = shadow.getPendingOperationsSorted();

        LOGGER.trace("Pending operations refresh of {}, dead={}, {} pending operations",
                shadow, isDead, sortedOperations.size());

        ctx.assertDefinition(); // probably not needed

        refreshShadowAsyncStatus(sortedOperations, result);

        retryOperations(sortedOperations, result);
    }

    /**
     * Refresh status of asynchronous operation, e.g. status of manual connector ticket.
     * This method will get new status from {@link ResourceObjectConverter} and it will process the
     * status in case that it has changed.
     */
    private void refreshShadowAsyncStatus(PendingOperations sortedOperations, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException {

        var gracePeriod = ctx.getGracePeriod();
        var notificationDeltas = new ArrayList<ObjectDelta<ShadowType>>();
        var shadowInception = false;
        OperationResultStatusType shadowInceptionOutcome = null;
        var shadowDelta = shadow.getPrismObject().createModifyDelta();

        for (var pendingOperation : sortedOperations) {

            if (!pendingOperation.isExecuting()) {
                continue;
            }

            String asyncRef = pendingOperation.getAsynchronousOperationReference();
            if (asyncRef == null) {
                continue;
            }

            OperationResultStatus newStatus;
            try {
                newStatus = b.resourceObjectConverter.refreshOperationStatus(ctx, shadow, asyncRef, result);
            } catch (CommunicationException e) {
                LOGGER.debug("Communication error while trying to refresh pending operation of {}. "
                        + "Skipping refresh of this operation.", shadow, e);
                result.recordPartialError(e);
                continue;
            } catch (ObjectNotFoundException e) {
                if (CaseType.class.equals(e.getType())) {
                    LOGGER.debug("The case was not found while trying to refresh pending operation of {}. "
                            + "Skipping refresh of this operation.", shadow, e);
                    result.recordPartialError(e);
                    continue;
                } else {
                    throw e;
                }
            }
            if (newStatus == null) {
                continue;
            }
            OperationResultStatusType newStatusType = newStatus.createStatusType();
            if (newStatusType == pendingOperation.getResultStatus()) {
                continue;
            }

            boolean operationCompleted =
                    ProvisioningUtil.isCompleted(newStatusType)
                            && pendingOperation.getCompletionTimestamp() == null;

            if (operationCompleted && gracePeriod == null) {
                LOGGER.trace("Deleting pending operation because it is completed (no grace): {}", pendingOperation);
                shadowDelta.addModification(pendingOperation.createDeleteDelta());
                if (pendingOperation.isAdd()) {
                    shadowInception = true;
                    shadowInceptionOutcome = newStatusType;
                } else if (pendingOperation.isDelete()) {
                    shadowInception = false;
                    shadowDelta.addModifications(
                            b.shadowUpdater.createTombstoneDeltas(shadow));
                }
                continue;
            }

            shadowDelta.addModification(
                    pendingOperation.createResultStatusDelta(newStatusType));

            if (operationCompleted) {
                shadowDelta.addModifications(
                        pendingOperation.createCompletionDeltas(
                                b.clock.currentTimeXMLGregorianCalendar()));

                var pendingDelta = pendingOperation.getDelta();
                if (pendingDelta.isAdd()) {
                    shadowInception = true;
                    shadowInceptionOutcome = newStatusType;
                } else if (pendingDelta.isModify()) {
                    for (var pendingModification : pendingDelta.getModifications()) {
                        shadowDelta.addModification(pendingModification.clone());
                    }
                } else if (pendingDelta.isDelete()) {
                    shadowInception = false;
                    shadowDelta.addModifications(
                            b.shadowUpdater.createTombstoneDeltas(shadow));
                }
                notificationDeltas.add(pendingDelta);
            }
        }

        if (shadowInception) {
            // We do not need to care about attributes in add deltas here. The add operation is already applied to
            // attributes. We need this to "allocate" the identifiers, so iteration mechanism in the
            // model can find unique values while taking pending create operations into consideration.
            shadowDelta.addModificationReplaceProperty(
                    ShadowType.F_EXISTS,
                    OperationResultUtil.isSuccessful(shadowInceptionOutcome));
        }

        expirePendingOperations(shadowDelta);

        if (!shadowDelta.isEmpty()) {
            ctx.applyCurrentDefinition(shadowDelta);
            b.shadowUpdater.modifyRepoShadow(ctx, shadow, shadowDelta.getModifications(), result);
        }

        for (var notificationDelta : notificationDeltas) {
            ResourceOperationDescription opDescription = createSuccessOperationDescription(ctx, shadow, notificationDelta);
            b.eventDispatcher.notifySuccess(opDescription, ctx.getTask(), result);
        }
    }

    private void retryOperations(PendingOperations sortedOperations, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {

        retriedOperationsResultStatus = OperationResultStatus.SUCCESS;

        if (shadow.isDead()) {
            return;
        }

        Duration retryPeriod = ProvisioningUtil.getRetryPeriod(ctx);
        LOGGER.trace("Selecting operations to retry from {} one(s); retry period: {}", sortedOperations.size(), retryPeriod);

        for (var pendingOperation : sortedOperations) {

            if (!pendingOperation.canBeRetried()) {
                LOGGER.trace("Skipping not retryable operation: {}", pendingOperation);
                continue;
            }

            // We really want to get "now" here. Retrying operation may take some time. We want good timestamps that do not lie.
            XMLGregorianCalendar now = b.clock.currentTimeXMLGregorianCalendar();
            if (!pendingOperation.isAfterRetryPeriod(retryPeriod, now)
                    && !ProvisioningOperationOptions.isForceRetry(options)) {
                LOGGER.trace("Skipping the operation whose retry time has not elapsed yet: {}", pendingOperation);
                continue;
            }

            LOGGER.trace("Going to retry operation {} on {}", pendingOperation, shadow);

            // Record attempt number and timestamp before the operation
            // TODO: later use this as an optimistic lock to make sure that two threads won't retry the operation at the same time

            int attemptNumber = pendingOperation.getAttemptNumber() + 1; // "or0" is there just for sure (btw, default is 1)
            var shadowModifications = RepoShadowModifications.of(
                    pendingOperation.createNextAttemptDeltas(attemptNumber, now));

            b.shadowUpdater.executeRepoShadowModifications(ctx, shadow, shadowModifications, parentResult);

            // The pending operation should be updated as part of the above call
            assert pendingOperation.getAttemptNumber() == attemptNumber;

            ctx.updateShadowState(shadow); // because pending operations were changed

            LOGGER.debug("Retrying operation {} on {}, attempt #{}", pendingOperation, shadow, attemptNumber);

            OperationResult result = parentResult.createSubresult(OP_OPERATION_RETRY);
            try {
                shadow = retryOperation(pendingOperation, result);
                result.computeStatus();
                if (result.isError()) {
                    retriedOperationsResultStatus = result.getStatus();
                }
                result.muteError();
            } catch (CommunicationException | GenericFrameworkException | ObjectAlreadyExistsException | SchemaException |
                    ObjectNotFoundException | ConfigurationException | SecurityViolationException e) {
                // This is final failure: the error is not handled.
                // Therefore the operation is now completed - finished with an error.
                // But we do not want to stop the task. Just log the error.
                LOGGER.error("Operation {} on {} ended up with an error after {} retries: {}",
                        pendingOperation.getDelta(), shadow, attemptNumber, e.getMessage(), e);
                // The retry itself was a success. Operation that was retried might have failed.
                // And that is recorded in the shadow. But we have successfully retried the operation.
                result.recordHandledError(e);
                retriedOperationsResultStatus = OperationResultStatus.FATAL_ERROR;
            } catch (Throwable e) {
                // This is unexpected error during retry. This means that there was other
                // failure that we did not expected. This is likely to be bug - or maybe wrong
                // error handling. This means that the retry was a failure.
                result.recordFatalError(e);
                retriedOperationsResultStatus = OperationResultStatus.FATAL_ERROR;
            } finally {
                result.close(); // Status should be set by now, we just want to close the result
            }

            retriedOperations.add(
                    new ObjectDeltaOperation<>(pendingOperation.getDelta(), result));
        }
    }

    private @NotNull RepoShadow retryOperation(
            @NotNull PendingOperation pendingOperation, @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {

        // TODO scripts, options
        ProvisioningOperationOptions options = ProvisioningOperationOptions.createForceRetry(false);
        if (pendingOperation.isAdd()) {
            return ShadowAddOperation.executeAsRetryInRefresh(ctx, shadow, pendingOperation, options, result);
        } else if (pendingOperation.isModify()) {
            return ShadowModifyOperation
                    .executeInRefresh(ctx, shadow, pendingOperation, options, result)
                    .getRepoShadow();
        } else if (pendingOperation.isDelete()) {
            return ShadowDeleteOperation
                    .executeInRefresh(ctx, shadow, pendingOperation, options, result)
                    .getRepoShadow();
        } else {
            throw new AssertionError(pendingOperation);
        }
    }

    private void deleteDeadShadowIfPossible(OperationResult result) {
        if (!shadow.isDead()) {
            return;
        }
        Duration gracePeriod = ctx.getGracePeriod();
        Duration deadRetentionPeriod = ProvisioningUtil.getDeadShadowRetentionPeriod(ctx);
        Duration expirationPeriod = XmlTypeConverter.longerDuration(gracePeriod, deadRetentionPeriod);
        XMLGregorianCalendar lastActivityTimestamp = null;

        var shadowBean = shadow.getBean();
        for (PendingOperationType pendingOperation : shadowBean.getPendingOperation()) {
            lastActivityTimestamp = XmlTypeConverter.laterTimestamp(lastActivityTimestamp, pendingOperation.getRequestTimestamp());
            lastActivityTimestamp = XmlTypeConverter.laterTimestamp(lastActivityTimestamp, pendingOperation.getLastAttemptTimestamp());
            lastActivityTimestamp = XmlTypeConverter.laterTimestamp(lastActivityTimestamp, pendingOperation.getCompletionTimestamp());
        }
        if (lastActivityTimestamp == null) {
            lastActivityTimestamp = ValueMetadataTypeUtil.getLastChangeTimestamp(shadowBean);
        }

        var now = b.clock.currentTimeXMLGregorianCalendar();

        // Explicitly check for zero deadRetentionPeriod to avoid some split-millisecond issues with dead shadow deletion.
        // If we have zero deadRetentionPeriod, we should get rid of all dead shadows immediately.
        if (XmlTypeConverter.isZero(deadRetentionPeriod) || expirationPeriod == null ||
                lastActivityTimestamp == null || XmlTypeConverter.isAfterInterval(lastActivityTimestamp, expirationPeriod, now)) {
            // Perish you stinking corpse!
            LOGGER.debug("Deleting dead {} because it is expired", shadow);
            Task task = ctx.getTask();
            b.shadowUpdater.deleteShadow(shadow, task, result);
            ResourceOperationDescription operationDescription =
                    createSuccessOperationDescription(ctx, shadow, shadow.getPrismObject().createDeleteDelta());
            b.eventDispatcher.notifySuccess(operationDescription, task, result);
            shadow.setDeleted();
            return;
        }

        LOGGER.trace("Keeping dead {} because it is not expired yet, last activity={}, expiration period={}",
                shadow, lastActivityTimestamp, expirationPeriod);
    }

    private void expirePendingOperations(ObjectDelta<ShadowType> shadowDelta) {
        var now = b.clock.currentTimeXMLGregorianCalendar();
        Duration gracePeriod = ctx.getGracePeriod();
        Duration pendingOperationRetentionPeriod = ProvisioningUtil.getPendingOperationRetentionPeriod(ctx);
        Duration expirePeriod = XmlTypeConverter.longerDuration(gracePeriod, pendingOperationRetentionPeriod);
        for (var pendingOperation : shadow.getPendingOperations()) {
            if (pendingOperation.isCompletedAndOverPeriod(now, expirePeriod)) {
                LOGGER.trace("Deleting pending operation because it is completed '{}' and expired: {}",
                        pendingOperation.getResultStatus(), pendingOperation);
                shadowDelta.addModification(pendingOperation.createDeleteDelta());
            }
        }
    }

    /**
     * When a deletion is determined to be failed, we try to restore the `primaryIdentifierValue` index.
     * (We may be unsuccessful if there was another shadow created in the meanwhile.)
     *
     * This method assumes that the pending operations have been already updated with the result of retried operations.
     *
     * (For simplicity and robustness, we just refresh provisioning indexes. It should be efficient enough.)
     */
    private void updateProvisioningIndexesAfterDeletion(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        if (retriedOperations.stream()
                .noneMatch(d -> ObjectDelta.isDelete(d.getObjectDelta()))) {
            LOGGER.trace("updateProvisioningIndexesAfterDeletion: no DELETE delta found");
            return;
        }
        try {
            b.shadowUpdater.refreshProvisioningIndexes(ctx, shadow, false, result);
        } catch (ObjectAlreadyExistsException e) {
            LOGGER.debug("Couldn't set `primaryIdentifierValue` for {} - probably a new one was created in the meanwhile. "
                    + "Marking this one as dead.", shadow, e);
            b.shadowUpdater.markShadowTombstone(shadow, ctx.getTask(), result);
        }
    }

    /**
     * Makes rough attempt to update the cached password under (possibly) changed policies:
     *
     * - encrypted -> hashed / none
     * - hashed -> none
     * - incomplete -> none
     *
     * Any other transitions are currently not possible (at least not without touching the resource).
     *
     * NOTE: Maybe we should move this code nearer to other places where cached password is managed? (`ShadowObjectComputer` etc)
     */
    private void updateCachedPasswordIfNeeded(OperationResult result) throws SchemaException, ObjectNotFoundException {
        LOGGER.trace("Considering updating cached password for {}", shadow);
        var property = ShadowUtil.getPasswordValueProperty(shadow.getBean());
        if (property == null || !property.isIncomplete() && property.hasNoValues()) {
            LOGGER.trace(" -> nothing to transform here");
            return;
        }
        var realValue = property.getRealValue();

        ItemDelta<?, ?> deltaToExecute;
        var definition = shadow.getObjectDefinition();
        if (!definition.areCredentialsCached()) {
            LOGGER.trace(" -> caching turned off, removing the cached value");
            deltaToExecute =
                    PrismContext.get().deltaFor(ShadowType.class)
                            .item(PATH_PASSWORD_VALUE)
                            .replace()
                            .asItemDelta();
        } else if (realValue == null) {
            LOGGER.trace(" -> no value stored (probably only the 'incomplete' flag), nothing to do");
            deltaToExecute = null;
        } else {
            LOGGER.trace(" -> caching is enabled, value is present; consulting the credentials storage manager");
            var credentialsPolicy = b.securityPolicyFinder.locateResourceObjectCredentialsPolicy(definition, result);
            var legacyCaching = definition.areCredentialsCachedLegacy();
            try {
                deltaToExecute =
                        b.credentialsStorageManager.updateShadowPasswordIfNeeded(realValue, credentialsPolicy, legacyCaching);
            } catch (EncryptionException e) {
                throw new SystemException("Couldn't transform cached password for " + shadow, e);
            }
        }

        if (deltaToExecute != null) {
            b.shadowUpdater.executeRepoShadowModifications(
                    ctx,
                    shadow,
                    RepoShadowModifications.of(List.of(deltaToExecute)),
                    result);
        }
    }

    public @NotNull RepoShadow getShadow() {
        return shadow;
    }

    @NotNull Collection<ObjectDeltaOperation<ShadowType>> getRetriedOperations() {
        return retriedOperations;
    }

    @NotNull OperationResultStatus getRetriedOperationsResultStatus() {
        return Objects.requireNonNullElse(retriedOperationsResultStatus, OperationResultStatus.SUCCESS);
    }
}
