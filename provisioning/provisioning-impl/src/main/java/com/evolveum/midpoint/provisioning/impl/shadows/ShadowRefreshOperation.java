/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.addDuration;
import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsUtil.createSuccessOperationDescription;
import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType.RETRY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.provisioning.impl.RepoShadowModifications;
import com.evolveum.midpoint.schema.util.RawRepoShadow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValueCollectionsUtil;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.OperationResultUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * The `refresh` operation - either full or quick.
 */
class ShadowRefreshOperation {

    private static final String OP_REFRESH_RETRY = ShadowsFacade.class.getName() + ".refreshRetry";
    private static final String OP_OPERATION_RETRY = ShadowsFacade.class.getName() + ".operationRetry";

    private static final Trace LOGGER = TraceManager.getTrace(ShadowRefreshOperation.class);

    @NotNull private final ShadowsLocalBeans b = ShadowsLocalBeans.get();

    /** Shadow-specific provisioning context. */
    @NotNull private final ProvisioningContext ctx;

    /** The shadow being refreshed. The value may change but should never be `null`. */
    @NotNull private RepoShadow shadow;

    /** ODOs for retried pending operations. */
    @NotNull private final Collection<ObjectDeltaOperation<ShadowType>> retriedOperations = new ArrayList<>();

    /** Very simplified result corresponding to retried execution of pending operations (just success/failure + last msg). */
    private OperationResult retriedOperationsResult;

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
        updateProvisioningIndexesAfterDeletion(result);
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

        List<PendingOperationType> sortedOperations = shadow.getPendingOperationsSorted();

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
    private void refreshShadowAsyncStatus(List<PendingOperationType> sortedOperations, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException {
        Duration gracePeriod = ctx.getGracePeriod();

        List<ObjectDelta<ShadowType>> notificationDeltas = new ArrayList<>();

        boolean shadowInception = false;
        OperationResultStatusType shadowInceptionOutcome = null;
        ObjectDelta<ShadowType> shadowDelta = shadow.getPrismObject().createModifyDelta();
        for (PendingOperationType pendingOperation : sortedOperations) {

            if (!needsRefresh(pendingOperation)) {
                continue;
            }

            ItemPath containerPath = pendingOperation.asPrismContainerValue().getPath();

            String asyncRef = pendingOperation.getAsynchronousOperationReference();
            if (asyncRef == null) {
                continue;
            }

            AsynchronousOperationResult refreshAsyncResult;
            try {
                refreshAsyncResult = b.resourceObjectConverter.refreshOperationStatus(ctx, shadow, asyncRef, result);
            } catch (CommunicationException e) {
                LOGGER.debug("Communication error while trying to refresh pending operation of {}. "
                        + "Skipping refresh of this operation.", shadow, e);
                result.recordPartialError(e);
                continue;
            }
            OperationResultStatus newStatus = refreshAsyncResult.getOperationResult().getStatus();

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
                shadowDelta.addModificationDeleteContainer(ShadowType.F_PENDING_OPERATION, pendingOperation.clone());

                ObjectDeltaType pendingDeltaType = pendingOperation.getDelta();
                ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType);

                if (pendingDelta.isAdd()) {
                    shadowInception = true;
                    shadowInceptionOutcome = newStatusType;
                }

                if (pendingDelta.isDelete()) {
                    shadowInception = false;
                    shadowDelta.addModifications(
                            b.shadowUpdater.createTombstoneDeltas(shadow));
                }
                continue;

            } else {
                PropertyDelta<OperationResultStatusType> resultStatusDelta =
                        shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_RESULT_STATUS));
                resultStatusDelta.setRealValuesToReplace(newStatusType);
                shadowDelta.addModification(resultStatusDelta);
            }

            if (operationCompleted) {
                // Operation completed

                PropertyDelta<PendingOperationExecutionStatusType> executionStatusDelta =
                        shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_EXECUTION_STATUS));
                executionStatusDelta.setRealValuesToReplace(PendingOperationExecutionStatusType.COMPLETED);
                shadowDelta.addModification(executionStatusDelta);

                PropertyDelta<XMLGregorianCalendar> completionTimestampDelta =
                        shadowDelta.createPropertyModification(
                                containerPath.append(PendingOperationType.F_COMPLETION_TIMESTAMP));
                completionTimestampDelta.setRealValuesToReplace(b.clock.currentTimeXMLGregorianCalendar());
                shadowDelta.addModification(completionTimestampDelta);

                ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingOperation.getDelta());

                if (pendingDelta.isAdd()) {
                    shadowInception = true;
                    shadowInceptionOutcome = newStatusType;
                }

                if (pendingDelta.isModify()) {

                    // Apply shadow naming attribute modification
                    ResourceAttributeContainerDefinition resourceAttrDefinition = shadow.getAttributesContainerDefinition();

                    // If naming attribute is present in delta...
                    ResourceAttributeDefinition<?> namingAttribute = resourceAttrDefinition.getNamingAttribute();
                    if (namingAttribute != null) {
                        ItemPath namingAttributePath =
                                ItemPath.create(ShadowType.F_ATTRIBUTES, namingAttribute.getItemName());
                        if (pendingDelta.hasItemDelta(namingAttributePath)) {

                            // Retrieve a possible changed name per the defined naming attribute for the resource
                            ItemDelta<?, ?> namingAttributeDelta = pendingDelta.findItemDelta(namingAttributePath);
                            Collection<?> valuesToReplace = namingAttributeDelta.getValuesToReplace();
                            Optional<?> valueToReplace = valuesToReplace.stream().findFirst();

                            if (valueToReplace.isPresent()) {
                                Object valueToReplaceObj = ((PrismPropertyValue<?>) valueToReplace.get()).getValue();
                                if (valueToReplaceObj instanceof String) {

                                    // Apply the new naming attribute value to the shadow name by adding the change to the modification set for shadow delta
                                    PropertyDelta<PolyString> nameDelta = shadowDelta.createPropertyModification(ItemPath.create(ShadowType.F_NAME));
                                    Collection<PolyString> modificationSet = new ArrayList<>();
                                    PolyString nameAttributeReplacement = new PolyString((String) valueToReplaceObj);
                                    modificationSet.add(nameAttributeReplacement);
                                    nameDelta.setValuesToReplace(PrismValueCollectionsUtil.createCollection(modificationSet));
                                    shadowDelta.addModification(nameDelta);
                                }
                            }
                        }
                    }

                    // Apply shadow attribute modifications
                    for (ItemDelta<?, ?> pendingModification : pendingDelta.getModifications()) {
                        shadowDelta.addModification(pendingModification.clone());
                    }
                }

                if (pendingDelta.isDelete()) {
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
            PropertyDelta<Boolean> existsDelta = shadowDelta.createPropertyModification(ShadowType.F_EXISTS);
            if (OperationResultUtil.isSuccessful(shadowInceptionOutcome)) {
                existsDelta.setRealValuesToReplace(true);
            } else {
                existsDelta.setRealValuesToReplace(false);
            }
            shadowDelta.addModification(existsDelta);
        }

        expirePendingOperations(shadowDelta);

        if (!shadowDelta.isEmpty()) {
            ctx.applyCurrentDefinition(shadowDelta);
            b.shadowUpdater.modifyRepoShadow(ctx, shadow, shadowDelta.getModifications(), result);
        }

        for (ObjectDelta<ShadowType> notificationDelta : notificationDeltas) {
            ResourceOperationDescription opDescription = createSuccessOperationDescription(ctx, shadow, notificationDelta);
            b.eventDispatcher.notifySuccess(opDescription, ctx.getTask(), result);
        }

        if (!shadowDelta.isEmpty()) {
            shadowDelta.applyTo(shadow.getPrismObject());// todo remove (already applied) MID-2119
        }
    }

    private boolean needsRefresh(PendingOperationType pendingOperation) {
        PendingOperationExecutionStatusType executionStatus = pendingOperation.getExecutionStatus();
        if (executionStatus == null) {
            // LEGACY: 3.7 and earlier
            return pendingOperation.getResultStatus() == OperationResultStatusType.IN_PROGRESS;
        } else {
            return executionStatus == PendingOperationExecutionStatusType.EXECUTING;
        }
    }

    private void retryOperations(List<PendingOperationType> sortedOperations, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {

        retriedOperationsResult = new OperationResult(OP_REFRESH_RETRY);
        retriedOperationsResult.setSuccess();

        if (shadow.isDead()) {
            return;
        }

        Duration retryPeriod = ProvisioningUtil.getRetryPeriod(ctx);
        LOGGER.trace("Selecting operations to retry from {} one(s); retry period: {}", sortedOperations.size(), retryPeriod);

        for (PendingOperationType pendingOperation : sortedOperations) {

            if (!RepoShadow.isRetryableOperation(pendingOperation)) {
                LOGGER.trace("Skipping not retryable operation: {}", pendingOperation);
                continue;
            }

            // We really want to get "now" here. Retrying operation may take some time. We want good timestamps that do not lie.
            XMLGregorianCalendar now = b.clock.currentTimeXMLGregorianCalendar();
            if (!isAfterRetryPeriod(pendingOperation, retryPeriod, now)) {
                if (pendingOperation.getType() != RETRY) { // TODO why this distinction?
                    LOGGER.trace(
                            "Skipping the non-RETRY-type operation whose retry time has not elapsed yet: {}", pendingOperation);
                    continue;
                }
                if (!ProvisioningOperationOptions.isForceRetry(options)) {
                    LOGGER.trace("Skipping the RETRY-type operation whose retry time has not elapsed yet: {}", pendingOperation);
                    continue;
                }
            }

            LOGGER.trace("Going to retry operation {} on {}", pendingOperation, shadow);

            // Record attempt number and timestamp before the operation
            // TODO: later use this as an optimistic lock to make sure that two threads won't retry the operation at the same time

            // TODO: move to a better place
            ItemPath containerPath = pendingOperation.asPrismContainerValue().getPath();

            int attemptNumber = or0(pendingOperation.getAttemptNumber()) + 1; // "or0" is there just for sure (btw, default is 1)
            List<ItemDelta<?, ?>> shadowDeltas = PrismContext.get().deltaFor(ShadowType.class)
                    .item(containerPath.append(PendingOperationType.F_ATTEMPT_NUMBER))
                    .replace(attemptNumber)
                    .item(containerPath.append(PendingOperationType.F_LAST_ATTEMPT_TIMESTAMP))
                    .replace(now)
                    .item(containerPath.append(PendingOperationType.F_RESULT_STATUS))
                    .replace(OperationResultStatusType.IN_PROGRESS)
                    .asItemDeltas();

            RepoShadowModifications shadowModifications = RepoShadowModifications.of(shadowDeltas);
            b.shadowUpdater.executeRepoShadowModifications(ctx, shadow, shadowModifications, parentResult);
            // The pending operation should be updated as part of the above call
            assert pendingOperation.getAttemptNumber() == attemptNumber;

            ctx.updateShadowState(shadow); // because pending operations were changed

            ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingOperation.getDelta());

            LOGGER.debug("Retrying operation {} on {}, attempt #{}", pendingDelta, shadow, attemptNumber);

            OperationResult result = parentResult.createSubresult(OP_OPERATION_RETRY);
            try {
                shadow = retryOperation(pendingDelta, pendingOperation, result);
                result.computeStatus();
                if (result.isError()) {
                    retriedOperationsResult.setStatus(result.getStatus());
                }
                result.muteError();
            } catch (CommunicationException | GenericFrameworkException | ObjectAlreadyExistsException | SchemaException |
                    ObjectNotFoundException | ConfigurationException | SecurityViolationException e) {
                // This is final failure: the error is not handled.
                // Therefore the operation is now completed - finished with an error.
                // But we do not want to stop the task. Just log the error.
                LOGGER.error("Operation {} on {} ended up with an error after {} retries: {}",
                        pendingDelta, shadow, attemptNumber, e.getMessage(), e);
                // The retry itself was a success. Operation that was retried might have failed.
                // And that is recorded in the shadow. But we have successfully retried the operation.
                result.recordHandledError(e);
                retriedOperationsResult.recordFatalError(
                        "Operation %s on %s ended with an error after %d retries: %s".formatted(
                                pendingDelta, shadow, attemptNumber, e.getMessage()));
            } catch (Throwable e) {
                // This is unexpected error during retry. This means that there was other
                // failure that we did not expected. This is likely to be bug - or maybe wrong
                // error handling. This means that the retry was a failure.
                result.recordFatalError(e);
                retriedOperationsResult.recordFatalError(e);
            } finally {
                result.close(); // Status should be set by now, we just want to close the result
            }

            ObjectDeltaOperation<ShadowType> objectDeltaOperation = new ObjectDeltaOperation<>(pendingDelta);
            objectDeltaOperation.setExecutionResult(result);
            retriedOperations.add(objectDeltaOperation);
        }
    }

    private @NotNull RepoShadow retryOperation(
            @NotNull ObjectDelta<ShadowType> pendingDelta,
            PendingOperationType pendingOperation,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {

        // TODO scripts, options
        ProvisioningOperationOptions options = ProvisioningOperationOptions.createForceRetry(false);
        if (pendingDelta.isAdd()) {
            return ShadowAddOperation
                    .executeInRefresh(ctx, shadow, pendingDelta, pendingOperation, options, result)
                    .getRepoShadow();
        } else if (pendingDelta.isModify()) {
            return ShadowModifyOperation
                    .executeInRefresh(ctx, shadow, pendingDelta.getModifications(), pendingOperation, options, result)
                    .getRepoShadow();
        } else if (pendingDelta.isDelete()) {
            return ShadowDeleteOperation
                    .executeInRefresh(ctx, shadow, pendingOperation, options, result)
                    .getRepoShadow();
        } else {
            throw new IllegalStateException("Unknown type of delta: " + pendingDelta);
        }
    }

    private boolean isAfterRetryPeriod(PendingOperationType pendingOperation, Duration retryPeriod, XMLGregorianCalendar now) {
        XMLGregorianCalendar scheduledRetryTime = addDuration(pendingOperation.getLastAttemptTimestamp(), retryPeriod);
        return XmlTypeConverter.compare(now, scheduledRetryTime) == DatatypeConstants.GREATER;
    }

    private void deleteDeadShadowIfPossible(OperationResult result) {
        if (!shadow.isDead()) {
            return;
        }
        Duration gracePeriod = ctx.getGracePeriod();
        Duration deadRetentionPeriod = ProvisioningUtil.getDeadShadowRetentionPeriod(ctx);
        Duration expirationPeriod = XmlTypeConverter.longerDuration(gracePeriod, deadRetentionPeriod);
        XMLGregorianCalendar lastActivityTimestamp = null;

        for (PendingOperationType pendingOperation : shadow.getBean().getPendingOperation()) {
            lastActivityTimestamp = XmlTypeConverter.laterTimestamp(lastActivityTimestamp, pendingOperation.getRequestTimestamp());
            lastActivityTimestamp = XmlTypeConverter.laterTimestamp(lastActivityTimestamp, pendingOperation.getLastAttemptTimestamp());
            lastActivityTimestamp = XmlTypeConverter.laterTimestamp(lastActivityTimestamp, pendingOperation.getCompletionTimestamp());
        }
        if (lastActivityTimestamp == null) {
            MetadataType metadata = shadow.getBean().getMetadata();
            if (metadata != null) {
                lastActivityTimestamp = metadata.getModifyTimestamp();
                if (lastActivityTimestamp == null) {
                    lastActivityTimestamp = metadata.getCreateTimestamp();
                }
            }
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

    private void expirePendingOperations(ObjectDelta<ShadowType> shadowDelta)
            throws SchemaException {
        var now = b.clock.currentTimeXMLGregorianCalendar();
        Duration gracePeriod = ctx.getGracePeriod();
        Duration pendingOperationRetentionPeriod = ProvisioningUtil.getPendingOperationRetentionPeriod(ctx);
        Duration expirePeriod = XmlTypeConverter.longerDuration(gracePeriod, pendingOperationRetentionPeriod);
        for (PendingOperationType pendingOperation : shadow.getBean().getPendingOperation()) {
            if (ProvisioningUtil.isCompletedAndOverPeriod(now, expirePeriod, pendingOperation)) {
                LOGGER.trace("Deleting pending operation because it is completed '{}' and expired: {}",
                        pendingOperation.getResultStatus(), pendingOperation);
                shadowDelta.addModificationDeleteContainer(ShadowType.F_PENDING_OPERATION, pendingOperation.clone());
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
        if (shadow.isDeleted()) {
            LOGGER.trace("updateProvisioningIndexesAfterDeletion: no shadow");
            return;
        }
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

    public @NotNull RepoShadow getShadow() {
        return shadow;
    }

    @NotNull Collection<ObjectDeltaOperation<ShadowType>> getRetriedOperations() {
        return retriedOperations;
    }

    @Nullable OperationResult getRetriedOperationsResult() {
        return retriedOperationsResult;
    }
}
