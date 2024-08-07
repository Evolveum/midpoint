/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.addDuration;
import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsUtil.createSuccessOperationDescription;
import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsUtil.isRetryableOperation;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType.RETRY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContainer;
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
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowUpdater;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.RefreshShadowOperation;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.OperationResultUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Helps with the `refresh` operation.
 */
@Component
@Experimental
class ShadowRefreshHelper {

    private static final String OP_REFRESH_RETRY = ShadowsFacade.class.getName() + ".refreshRetry";
    private static final String OP_OPERATION_RETRY = ShadowsFacade.class.getName() + ".operationRetry";

    private static final Trace LOGGER = TraceManager.getTrace(ShadowRefreshHelper.class);

    @Autowired private Clock clock;
    @Autowired private PrismContext prismContext;
    @Autowired private ResourceObjectConverter resourceObjectConverter;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired protected ShadowUpdater shadowUpdater;
    @Autowired private EventDispatcher operationListener;
    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private DefinitionsHelper definitionsHelper;

    public @NotNull RefreshShadowOperation refreshShadow(
            ShadowType repoShadow, ProvisioningOperationOptions options, ProvisioningOperationContext context, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, EncryptionException {

        LOGGER.trace("Refreshing {}", repoShadow);
        ProvisioningContext ctx = ctxFactory.createForShadow(repoShadow, task, result);
        ctx.setOperationContext(context);
        ctx.assertDefinition();
        ctx.applyAttributesDefinition(repoShadow);

        try {
            shadowUpdater.refreshProvisioningIndexes(ctx, repoShadow, true, result);
        } catch (ObjectAlreadyExistsException e) {
            throw SystemException.unexpected(e, "when refreshing provisioning indexes");
        }

        if (!ctx.isExecutionFullyPersistent()) {
            // Unlike other places related to the simulation mode, we do not throw an exception here. The shadow refresh may be
            // invoked in various situations, and it is not sure that the caller(s) have full responsibility of these. Hence, we
            // silently ignore these requests here.
            LOGGER.trace("Skipping refresh of {} pending operations because the task is in simulation mode", repoShadow);
            return new RefreshShadowOperation(repoShadow);
        }

        RefreshShadowOperation refreshShadowOperation = processPendingOperations(ctx, repoShadow, options, task, result);

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        ShadowType shadowAfterCleanup = deleteDeadShadowIfPossible(ctx, repoShadow, now, result);
        if (shadowAfterCleanup == null) {
            refreshShadowOperation.setRefreshedShadow(null);
        }

        updateProvisioningIndexesAfterDeletion(ctx, refreshShadowOperation, result);

        return refreshShadowOperation;
    }

    private @NotNull RefreshShadowOperation processPendingOperations(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            ProvisioningOperationOptions options,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException {

        List<PendingOperationType> pendingOperations = repoShadow.getPendingOperation();
        boolean isDead = ShadowUtil.isDead(repoShadow);
        if (!isDead && pendingOperations.isEmpty()) {
            LOGGER.trace(
                    "Skipping refresh of {} pending operations because shadow is not dead and there are no pending operations",
                    repoShadow);
            return new RefreshShadowOperation(repoShadow);
        }

        if (ctx.isInMaintenance()) {
            LOGGER.trace("Skipping refresh of {} pending operations because resource is in the maintenance mode", repoShadow);
            return new RefreshShadowOperation(repoShadow);
        }

        LOGGER.trace("Pending operations refresh of {}, dead={}, {} pending operations",
                repoShadow, isDead, pendingOperations.size());

        ctx.assertDefinition();
        List<PendingOperationType> sortedOperations = shadowCaretaker.sortPendingOperations(repoShadow.getPendingOperation());

        refreshShadowAsyncStatus(ctx, repoShadow, sortedOperations, task, result);

        return retryOperations(ctx, repoShadow, sortedOperations, options, result);
    }

    /**
     * Used to quickly and efficiently refresh shadow before GET operations.
     */
    ShadowType refreshShadowQuick(
            ProvisioningContext ctx, ShadowType repoShadow, XMLGregorianCalendar now, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {

        ObjectDelta<ShadowType> shadowDelta = repoShadow.asPrismObject().createModifyDelta();
        expirePendingOperations(ctx, repoShadow, shadowDelta, now);

        if (!shadowDelta.isEmpty()) {
            shadowUpdater.executeRepoShadowModifications(ctx, repoShadow, shadowDelta.getModifications(), result);
            shadowDelta.applyTo(repoShadow.asPrismObject());
        }

        return deleteDeadShadowIfPossible(ctx, repoShadow, now, result);
    }

    /**
     * Refresh status of asynchronous operation, e.g. status of manual connector ticket.
     * This method will get new status from {@link ResourceObjectConverter} and it will process the
     * status in case that it has changed.
     */
    private void refreshShadowAsyncStatus(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            List<PendingOperationType> sortedOperations,
            Task task,
            OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException, ConfigurationException, ExpressionEvaluationException {
        Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);

        List<ObjectDelta<ShadowType>> notificationDeltas = new ArrayList<>();

        boolean shadowInception = false;
        OperationResultStatusType shadowInceptionOutcome = null;
        ObjectDelta<ShadowType> shadowDelta = repoShadow.asPrismObject().createModifyDelta();
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
                refreshAsyncResult = resourceObjectConverter.refreshOperationStatus(ctx, repoShadow, asyncRef, parentResult);
            } catch (CommunicationException e) {
                LOGGER.debug("Communication error while trying to refresh pending operation of {}. "
                        + "Skipping refresh of this operation.", repoShadow, e);
                parentResult.recordPartialError(e);
                continue;
            } catch (ObjectNotFoundException e) {
                if (CaseType.class.equals(e.getType())) {
                    LOGGER.debug("The case was not found while trying to refresh pending operation of {}. "
                            + "Skipping refresh of this operation.", repoShadow, e);
                    parentResult.recordPartialError(e);
                    continue;
                } else {
                    throw e;
                }
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
                ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType, prismContext);

                if (pendingDelta.isAdd()) {
                    shadowInception = true;
                    shadowInceptionOutcome = newStatusType;
                }

                if (pendingDelta.isDelete()) {
                    shadowInception = false;
                    //noinspection unchecked
                    shadowUpdater.addTombstoneDeltas(repoShadow, (List) shadowDelta.getModifications());
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
                completionTimestampDelta.setRealValuesToReplace(clock.currentTimeXMLGregorianCalendar());
                shadowDelta.addModification(completionTimestampDelta);

                ObjectDelta<ShadowType> pendingDelta =
                        DeltaConvertor.createObjectDelta(pendingOperation.getDelta(), prismContext);

                if (pendingDelta.isAdd()) {
                    shadowInception = true;
                    shadowInceptionOutcome = newStatusType;
                }

                if (pendingDelta.isModify()) {

                    // Apply shadow naming attribute modification
                    PrismContainer<ShadowAttributesType> shadowAttributesContainer =
                            repoShadow.asPrismObject().findContainer(ItemPath.create(ShadowType.F_ATTRIBUTES));

                    ResourceAttributeContainer resourceAttributeContainer =
                            ResourceAttributeContainer.convertFromContainer(
                                    shadowAttributesContainer, ctx.getObjectDefinitionRequired());
                    ResourceAttributeContainerDefinition resourceAttrDefinition = resourceAttributeContainer.getDefinition();
                    if (resourceAttrDefinition != null) {

                        // If naming attribute is present in delta...
                        ResourceAttributeDefinition<?> namingAttribute = resourceAttrDefinition.getNamingAttribute();
                        if (namingAttribute != null) {
                            ItemPath namingAttributePath =
                                    ItemPath.create(ShadowType.F_ATTRIBUTES, namingAttribute.getItemName());
                            if (pendingDelta.hasItemDelta(namingAttributePath)) {

                                // Retrieve a possible changed name per the defined naming attribute for the resource
                                ItemDelta namingAttributeDelta = pendingDelta.findItemDelta(namingAttributePath);
                                Collection<?> valuesToReplace = namingAttributeDelta.getValuesToReplace();
                                Optional<?> valueToReplace = valuesToReplace.stream().findFirst();

                                if (valueToReplace.isPresent()) {
                                    Object valueToReplaceObj = ((PrismPropertyValue) valueToReplace.get()).getValue();
                                    if (valueToReplaceObj instanceof String) {

                                        // Apply the new naming attribute value to the shadow name by adding the change to the modification set for shadow delta
                                        PropertyDelta<PolyString> nameDelta = shadowDelta.createPropertyModification(ItemPath.create(ShadowType.F_NAME));
                                        Collection<PolyString> modificationSet = new ArrayList<>();
                                        PolyString nameAttributeReplacement = new PolyString((String) valueToReplaceObj);
                                        modificationSet.add(nameAttributeReplacement);
                                        nameDelta.setValuesToReplace(PrismValueCollectionsUtil.createCollection(prismContext, modificationSet));
                                        shadowDelta.addModification(nameDelta);
                                    }
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
                    //noinspection unchecked
                    shadowUpdater.addTombstoneDeltas(repoShadow, (List) shadowDelta.getModifications());
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

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        expirePendingOperations(ctx, repoShadow, shadowDelta, now);

        if (!shadowDelta.isEmpty()) {
            ctx.applyAttributesDefinition(shadowDelta);
            shadowUpdater.modifyRepoShadow(ctx, repoShadow, shadowDelta.getModifications(), parentResult);
        }

        for (ObjectDelta<ShadowType> notificationDelta : notificationDeltas) {
            ResourceOperationDescription opDescription = createSuccessOperationDescription(ctx, repoShadow, notificationDelta);
            operationListener.notifySuccess(opDescription, task, parentResult);
        }

        if (!shadowDelta.isEmpty()) {
            shadowDelta.applyTo(repoShadow.asPrismObject());
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

    private @NotNull RefreshShadowOperation retryOperations(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            List<PendingOperationType> sortedOperations,
            ProvisioningOperationOptions options,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        OperationResult retryResult = new OperationResult(OP_REFRESH_RETRY);
        if (ShadowUtil.isDead(repoShadow)) {
            RefreshShadowOperation rso = new RefreshShadowOperation(repoShadow);
            retryResult.recordSuccess();
            rso.setRefreshResult(retryResult);
            return rso;
        }

        Duration retryPeriod = ProvisioningUtil.getRetryPeriod(ctx);
        LOGGER.trace("Selecting operations to retry from {} one(s); retry period: {}", sortedOperations.size(), retryPeriod);

        Collection<ObjectDeltaOperation<ShadowType>> executedDeltas = new ArrayList<>();
        for (PendingOperationType pendingOperation : sortedOperations) {

            if (!isRetryableOperation(pendingOperation)) {
                LOGGER.trace("Skipping not retryable operation: {}", pendingOperation);
                continue;
            }

            // We really want to get "now" here. Retrying operation may take some time. We want good timestamps that do not lie.
            XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
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

            LOGGER.trace("Going to retry operation {} on {}", pendingOperation, repoShadow);

            // Record attempt number and timestamp before the operation
            // TODO: later use this as an optimistic lock to make sure that two threads won't retry the operation at the same time

            // TODO: move to a better place
            ItemPath containerPath = pendingOperation.asPrismContainerValue().getPath();

            int attemptNumber = or0(pendingOperation.getAttemptNumber()) + 1; // "or0" is there just for sure (btw, default is 1)
            List<ItemDelta<?, ?>> shadowDeltas = prismContext.deltaFor(ShadowType.class)
                    .item(containerPath.append(PendingOperationType.F_ATTEMPT_NUMBER))
                    .replace(attemptNumber)
                    .item(containerPath.append(PendingOperationType.F_LAST_ATTEMPT_TIMESTAMP))
                    .replace(now)
                    .item(containerPath.append(PendingOperationType.F_RESULT_STATUS))
                    .replace(OperationResultStatusType.IN_PROGRESS)
                    .asItemDeltas();

            shadowUpdater.executeRepoShadowModifications(ctx, repoShadow, shadowDeltas, parentResult);
            // The pending operation should be updated as part of the above call
            assert pendingOperation.getAttemptNumber() == attemptNumber;

            ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingOperation.getDelta());

            LOGGER.debug("Retrying operation {} on {}, attempt #{}", pendingDelta, repoShadow, attemptNumber);

            OperationResult result = parentResult.createSubresult(OP_OPERATION_RETRY);
            try {
                ProvisioningOperationState<?> opState = retryOperation(ctx, pendingDelta, repoShadow, pendingOperation, result);
                repoShadow = opState.getRepoShadow();
                result.computeStatus();
                if (result.isError()) {
                    retryResult.setStatus(result.getStatus());
                }
                //TODO maybe add whole "result" as subresult to the retryResult?
                result.muteError();
            } catch (CommunicationException | GenericFrameworkException | ObjectAlreadyExistsException | SchemaException |
                    ObjectNotFoundException | ConfigurationException | SecurityViolationException e) {
                // This is final failure: the error is not handled.
                // Therefore the operation is now completed - finished with an error.
                // But we do not want to stop the task. Just log the error.
                LOGGER.error("Operation {} on {} ended up with an error after {} retries: {}",
                        pendingDelta, repoShadow, attemptNumber, e.getMessage(), e);
                // The retry itself was a success. Operation that was retried might have failed.
                // And that is recorded in the shadow. But we have successfully retried the operation.
                result.recordHandledError(e);
                retryResult.recordFatalError("Operation " + pendingDelta + " on " + repoShadow + " ended with an error after " + attemptNumber + " retries: " + e.getMessage());
            } catch (Throwable e) {
                // This is unexpected error during retry. This means that there was other
                // failure that we did not expected. This is likely to be bug - or maybe wrong
                // error handling. This means that the retry was a failure.
                result.recordFatalError(e);
                retryResult.recordFatalError(e);
            } finally {
                result.close(); // Status should be set by now, we just want to close the result
            }

            ObjectDeltaOperation<ShadowType> objectDeltaOperation = new ObjectDeltaOperation<>(pendingDelta);
            objectDeltaOperation.setExecutionResult(result);
            executedDeltas.add(objectDeltaOperation);
        }

        RefreshShadowOperation rso = new RefreshShadowOperation(repoShadow);
        rso.setExecutedDeltas(executedDeltas);
        rso.setRefreshResult(retryResult);

        LOGGER.trace("refreshShadowOperation {}", rso.debugDumpLazily());
        return rso;
    }

    private @NotNull ProvisioningOperationState<?> retryOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull ObjectDelta<ShadowType> pendingDelta,
            @NotNull ShadowType repoShadow,
            PendingOperationType pendingOperation,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {

        // TODO scripts, options
        ProvisioningOperationOptions options = ProvisioningOperationOptions.createForceRetry(false);
        if (pendingDelta.isAdd()) {
            return ShadowAddOperation.executeInRefresh(ctx, repoShadow, pendingDelta, pendingOperation, options, result);
        } else if (pendingDelta.isModify()) {
            return ShadowModifyOperation.executeInRefresh(
                    ctx, repoShadow, pendingDelta.getModifications(), pendingOperation, options, result);
        } else if (pendingDelta.isDelete()) {
            return ShadowDeleteOperation.executeInRefresh(ctx, repoShadow, pendingOperation, options, result);
        } else {
            throw new IllegalStateException("Unknown type of delta: " + pendingDelta);
        }
    }

    private boolean isAfterRetryPeriod(PendingOperationType pendingOperation, Duration retryPeriod, XMLGregorianCalendar now) {
        XMLGregorianCalendar scheduledRetryTime = addDuration(pendingOperation.getLastAttemptTimestamp(), retryPeriod);
        return XmlTypeConverter.compare(now, scheduledRetryTime) == DatatypeConstants.GREATER;
    }

    private ShadowType deleteDeadShadowIfPossible(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            XMLGregorianCalendar now,
            OperationResult result) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (!ShadowUtil.isDead(repoShadow)) {
            return repoShadow;
        }
        Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);
        Duration deadRetentionPeriod = ProvisioningUtil.getDeadShadowRetentionPeriod(ctx);
        Duration expirationPeriod = XmlTypeConverter.longerDuration(gracePeriod, deadRetentionPeriod);
        XMLGregorianCalendar lastActivityTimestamp = null;

        for (PendingOperationType pendingOperation : repoShadow.getPendingOperation()) {
            lastActivityTimestamp = XmlTypeConverter.laterTimestamp(lastActivityTimestamp, pendingOperation.getRequestTimestamp());
            lastActivityTimestamp = XmlTypeConverter.laterTimestamp(lastActivityTimestamp, pendingOperation.getLastAttemptTimestamp());
            lastActivityTimestamp = XmlTypeConverter.laterTimestamp(lastActivityTimestamp, pendingOperation.getCompletionTimestamp());
        }
        if (lastActivityTimestamp == null) {
            MetadataType metadata = repoShadow.getMetadata();
            if (metadata != null) {
                lastActivityTimestamp = metadata.getModifyTimestamp();
                if (lastActivityTimestamp == null) {
                    lastActivityTimestamp = metadata.getCreateTimestamp();
                }
            }
        }

        // Explicitly check for zero deadRetentionPeriod to avoid some split-millisecond issues with dead shadow deletion.
        // If we have zero deadRetentionPeriod, we should get rid of all dead shadows immediately.
        if (XmlTypeConverter.isZero(deadRetentionPeriod) || expirationPeriod == null ||
                lastActivityTimestamp == null || XmlTypeConverter.isAfterInterval(lastActivityTimestamp, expirationPeriod, now)) {
            // Perish you stinking corpse!
            LOGGER.debug("Deleting dead {} because it is expired", repoShadow);
            Task task = ctx.getTask();
            shadowUpdater.deleteShadow(repoShadow, task, result);
            definitionsHelper.applyDefinition(repoShadow, task, result);
            ResourceOperationDescription operationDescription =
                    createSuccessOperationDescription(ctx, repoShadow, repoShadow.asPrismObject().createDeleteDelta());
            operationListener.notifySuccess(operationDescription, task, result);
            return null;
        }

        LOGGER.trace("Keeping dead {} because it is not expired yet, last activity={}, expiration period={}",
                repoShadow, lastActivityTimestamp, expirationPeriod);
        return repoShadow;
    }

    private void expirePendingOperations(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            ObjectDelta<ShadowType> shadowDelta,
            XMLGregorianCalendar now)
            throws SchemaException {
        Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);
        Duration pendingOperationRetentionPeriod = ProvisioningUtil.getPendingOperationRetentionPeriod(ctx);
        Duration expirePeriod = XmlTypeConverter.longerDuration(gracePeriod, pendingOperationRetentionPeriod);
        for (PendingOperationType pendingOperation : repoShadow.getPendingOperation()) {
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
    private void updateProvisioningIndexesAfterDeletion(
            ProvisioningContext ctx, RefreshShadowOperation refreshShadowOperation, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        ShadowType shadow = refreshShadowOperation.getRefreshedShadow();
        if (shadow == null) {
            LOGGER.trace("updateProvisioningIndexesAfterDeletion: no shadow");
            return;
        }
        if (emptyIfNull(refreshShadowOperation.getExecutedDeltas()).stream()
                .noneMatch(d -> ObjectDelta.isDelete(d.getObjectDelta()))) {
            LOGGER.trace("updateProvisioningIndexesAfterDeletion: no DELETE delta found");
            return;
        }
        try {
            shadowUpdater.refreshProvisioningIndexes(ctx, shadow, false, result);
        } catch (ObjectAlreadyExistsException e) {
            LOGGER.debug("Couldn't set `primaryIdentifierValue` for {} - probably a new one was created in the meanwhile. "
                    + "Marking this one as dead.", shadow, e);
            shadowUpdater.markShadowTombstone(shadow, ctx.getTask(), result);
        }
    }
}
