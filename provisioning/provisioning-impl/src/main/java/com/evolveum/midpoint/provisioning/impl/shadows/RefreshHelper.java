/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.provisioning.impl.shadows.Util.createSuccessOperationDescription;
import static com.evolveum.midpoint.provisioning.impl.shadows.Util.needsRetry;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.RefreshShadowOperation;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.OperationResultUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
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
class RefreshHelper {

    private static final String OP_REFRESH_RETRY = ShadowsFacade.class.getName() + ".refreshRetry";
    private static final String OP_OPERATION_RETRY = ShadowsFacade.class.getName() + ".operationRetry";

    private static final Trace LOGGER = TraceManager.getTrace(RefreshHelper.class);

    @Autowired private Clock clock;
    @Autowired private PrismContext prismContext;
    @Autowired private ResourceObjectConverter resourceObjectConverter;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired protected ShadowManager shadowManager;
    @Autowired private EventDispatcher operationListener;
    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private ShadowAddHelper addHelper;
    @Autowired private ModifyHelper modifyHelper;
    @Autowired private DeleteHelper deleteHelper;
    @Autowired private DefinitionsHelper definitionsHelper;

    public @NotNull RefreshShadowOperation refreshShadow(
            ShadowType repoShadow, ProvisioningOperationOptions options, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, EncryptionException {

        LOGGER.trace("Refreshing {}", repoShadow);
        ProvisioningContext ctx = ctxFactory.createForShadow(repoShadow, task, result);
        ctx.assertDefinition();
        ctx.applyAttributesDefinition(repoShadow);

        try {
            shadowManager.refreshProvisioningIndexes(ctx, repoShadow, true, result);
        } catch (ObjectAlreadyExistsException e) {
            throw SystemException.unexpected(e, "when refreshing provisioning indexes");
        }

        RefreshShadowOperation refreshShadowOperation = processPendingOperations(ctx, repoShadow, options, task, result);

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        ShadowType shadowAfterCleanup = deleteDeadShadowIfPossible(ctx, repoShadow, now, task, result);
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
            LOGGER.trace("Skipping refresh of {} pending operations because shadow is not dead and there are no pending operations", repoShadow);
            RefreshShadowOperation rso = new RefreshShadowOperation();
            rso.setRefreshedShadow(repoShadow);
            return rso;
        }

        if (ResourceTypeUtil.isInMaintenance(ctx.getResource())) {
            LOGGER.trace("Skipping refresh of {} pending operations because resource shadow is in the maintenance.", repoShadow);
            RefreshShadowOperation rso = new RefreshShadowOperation();
            rso.setRefreshedShadow(repoShadow);
            return rso;
        }

        LOGGER.trace("Pending operations refresh of {}, dead={}, {} pending operations", repoShadow, isDead, pendingOperations.size());

        ctx.assertDefinition();
        List<PendingOperationType> sortedOperations = shadowCaretaker.sortPendingOperations(repoShadow.getPendingOperation());

        refreshShadowAsyncStatus(ctx, repoShadow, sortedOperations, task, result);

        return retryOperations(ctx, repoShadow, sortedOperations, options, task, result);
    }

    /**
     * Used to quickly and efficiently refresh shadow before GET operations.
     */
    ShadowType refreshShadowQuick(ProvisioningContext ctx, ShadowType repoShadow,
            XMLGregorianCalendar now, Task task, OperationResult result) throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        ObjectDelta<ShadowType> shadowDelta = repoShadow.asPrismObject().createModifyDelta();
        expirePendingOperations(ctx, repoShadow, shadowDelta, now);

        if (!shadowDelta.isEmpty()) {
            shadowManager.modifyShadowAttributes(ctx, repoShadow, shadowDelta.getModifications(), result);
            shadowDelta.applyTo(repoShadow.asPrismObject());
        }

        return deleteDeadShadowIfPossible(ctx, repoShadow, now, task, result);
    }

    /**
     * Refresh status of asynchronous operation, e.g. status of manual connector ticket.
     * This method will get new status from resourceObjectConverter and it will process the
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
        for (PendingOperationType pendingOperation: sortedOperations) {

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
                    shadowManager.addDeadShadowDeltas(repoShadow, (List)shadowDelta.getModifications());
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
                                    Object valueToReplaceObj = ((PrismPropertyValue)valueToReplace.get()).getValue();
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
                    for (ItemDelta<?, ?> pendingModification: pendingDelta.getModifications()) {
                        shadowDelta.addModification(pendingModification.clone());
                    }
                }

                if (pendingDelta.isDelete()) {
                    shadowInception = false;
                    //noinspection unchecked
                    shadowManager.addDeadShadowDeltas(repoShadow, (List)shadowDelta.getModifications());
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
            shadowManager.modifyShadowAttributes(ctx, repoShadow, shadowDelta.getModifications(), parentResult);
        }

        for (ObjectDelta<ShadowType> notificationDelta: notificationDeltas) {
            ResourceOperationDescription operationDescription =
                    createSuccessOperationDescription(ctx, repoShadow, notificationDelta, null);
            operationListener.notifySuccess(operationDescription, task, parentResult);
        }

        if (!shadowDelta.isEmpty()) {
            shadowDelta.applyTo(repoShadow.asPrismObject());
        }
    }

    private boolean needsRefresh(PendingOperationType pendingOperation) {
        PendingOperationExecutionStatusType executionStatus = pendingOperation.getExecutionStatus();
        if (executionStatus == null) {
            // LEGACY: 3.7 and earlier
            return OperationResultStatusType.IN_PROGRESS.equals(pendingOperation.getResultStatus());
        } else {
            return PendingOperationExecutionStatusType.EXECUTING.equals(executionStatus);
        }
    }

    private @NotNull RefreshShadowOperation retryOperations(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            List<PendingOperationType> sortedOperations,
            ProvisioningOperationOptions options,
            Task task,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        OperationResult retryResult = new OperationResult(OP_REFRESH_RETRY);
        if (ShadowUtil.isDead(repoShadow)) {
            RefreshShadowOperation rso = new RefreshShadowOperation();
            retryResult.recordSuccess();
            rso.setRefreshedShadow(repoShadow);
            rso.setRefreshResult(retryResult);
            return rso;
        }

        Duration retryPeriod = ProvisioningUtil.getRetryPeriod(ctx);

        Collection<ObjectDeltaOperation<ShadowType>> executedDeltas = new ArrayList<>();
        for (PendingOperationType pendingOperation: sortedOperations) {

            if (!needsRetry(pendingOperation)) {
                continue;
            }
            // We really want to get "now" here. Retrying operation may take some time. We want good timestamps that do not lie.
            XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
            if (!isAfterRetryPeriod(pendingOperation, retryPeriod, now)) {
                if (PendingOperationTypeType.RETRY != pendingOperation.getType()) {
                    continue;
                }
                if (!ProvisioningOperationOptions.isForceRetry(options)) {
                    continue;
                }
            }

            LOGGER.trace("Going to retry operation {} on {}", pendingOperation, repoShadow);

            // Record attempt number and timestamp before the operation
            // TODO: later use this as an optimistic lock to make sure that two threads won't retry the operation at the same time

            // TODO: move to a better place
            ObjectDelta<ShadowType> shadowDelta = repoShadow.asPrismObject().createModifyDelta();
            ItemPath containerPath = pendingOperation.asPrismContainerValue().getPath();

            int attemptNumber = pendingOperation.getAttemptNumber() + 1;
            PropertyDelta<Integer> attemptNumberDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_ATTEMPT_NUMBER));
            attemptNumberDelta.setRealValuesToReplace(attemptNumber);
            shadowDelta.addModification(attemptNumberDelta);

            PropertyDelta<XMLGregorianCalendar> lastAttemptTimestampDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_LAST_ATTEMPT_TIMESTAMP));
            lastAttemptTimestampDelta.setRealValuesToReplace(now);
            shadowDelta.addModification(lastAttemptTimestampDelta);

            PropertyDelta<OperationResultStatusType> resultStatusDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_RESULT_STATUS));
            resultStatusDelta.setRealValuesToReplace(OperationResultStatusType.IN_PROGRESS);
            shadowDelta.addModification(resultStatusDelta);

            shadowManager.modifyShadowAttributes(ctx, repoShadow, shadowDelta.getModifications(), parentResult);
            shadowDelta.applyTo(repoShadow.asPrismObject());

            ObjectDeltaType pendingDeltaType = pendingOperation.getDelta();
            ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType, prismContext);

            ProvisioningOperationState<? extends AsynchronousOperationResult> opState =
                    ProvisioningOperationState.fromPendingOperation(repoShadow, pendingOperation);

            LOGGER.debug("Retrying operation {} on {}, attempt #{}", pendingDelta, repoShadow, attemptNumber);

            ObjectDeltaOperation<ShadowType> objectDeltaOperation = new ObjectDeltaOperation<>(pendingDelta);
            OperationResult result = parentResult.createSubresult(OP_OPERATION_RETRY);
            try {
                retryOperation(ctx, pendingDelta, opState, result);
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
                result.computeStatusIfUnknown(); // Status should be set by now, we just want to close the result
            }

            objectDeltaOperation.setExecutionResult(result);
            executedDeltas.add(objectDeltaOperation);
        }

        RefreshShadowOperation rso = new RefreshShadowOperation();
        rso.setExecutedDeltas(executedDeltas);
        rso.setRefreshedShadow(repoShadow);
        parentResult.computeStatus();

        rso.setRefreshResult(retryResult);

        LOGGER.trace("refreshShadowOperation {}", rso.debugDumpLazily());
        return rso;
    }

    private void retryOperation(ProvisioningContext ctx, ObjectDelta<ShadowType> pendingDelta,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState, OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {

        ProvisioningOperationOptions options = ProvisioningOperationOptions.createForceRetry(false);
        OperationProvisioningScriptsType scripts = null; // TODO
        if (pendingDelta.isAdd()) {
            PrismObject<ShadowType> resourceObjectToAdd = pendingDelta.getObjectToAdd();
            //noinspection unchecked
            addHelper.executeAddShadowAttempt(ctx, resourceObjectToAdd.asObjectable(), scripts,
                    (ProvisioningOperationState<AsynchronousOperationReturnValue<ShadowType>>) opState,
                    options, result);
        }

        if (pendingDelta.isModify()) {
            if (opState.objectExists()) {
                //noinspection unchecked
                modifyHelper.modifyShadowAttempt(ctx, pendingDelta.getModifications(), scripts, options,
                        (ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>>) opState,
                        true, result);
            } else {
                result.recordFatalError("Object does not exist on the resource yet, modification attempt was skipped");
            }
        }

        if (pendingDelta.isDelete()) {
            if (opState.objectExists()) {
                //noinspection unchecked
                deleteHelper.deleteShadowAttempt(ctx, options, scripts,
                        (ProvisioningOperationState<AsynchronousOperationResult>) opState,
                        result);
            } else {
                result.recordFatalError("Object does not exist on the resource yet, deletion attempt was skipped");
            }
        }
    }

    private boolean isAfterRetryPeriod(PendingOperationType pendingOperation, Duration retryPeriod, XMLGregorianCalendar now) {
        XMLGregorianCalendar lastAttemptTimestamp = pendingOperation.getLastAttemptTimestamp();
        XMLGregorianCalendar scheduledRetryTimestamp = XmlTypeConverter.addDuration(lastAttemptTimestamp, retryPeriod);
        return XmlTypeConverter.compare(now, scheduledRetryTimestamp) == DatatypeConstants.GREATER;
    }

    private ShadowType deleteDeadShadowIfPossible(ProvisioningContext ctx, ShadowType repoShadow,
            XMLGregorianCalendar now, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (!ShadowUtil.isDead(repoShadow)) {
            return repoShadow;
        }
        Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);
        Duration deadRetentionPeriod = ProvisioningUtil.getDeadShadowRetentionPeriod(ctx);
        Duration expirationPeriod = XmlTypeConverter.longerDuration(gracePeriod, deadRetentionPeriod);
        XMLGregorianCalendar lastActivityTimestamp = null;

        for (PendingOperationType pendingOperation: repoShadow.getPendingOperation()) {
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
            shadowManager.deleteShadow(repoShadow, task, parentResult);
            definitionsHelper.applyDefinition(repoShadow, task, parentResult);
            ResourceOperationDescription operationDescription =
                    createSuccessOperationDescription(ctx, repoShadow, repoShadow.asPrismObject().createDeleteDelta(), null);
            operationListener.notifySuccess(operationDescription, task, parentResult);
            return null;
        }

        LOGGER.trace("Keeping dead {} because it is not expired yet, last activity={}, expiration period={}", repoShadow,
                lastActivityTimestamp, expirationPeriod);
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
        for (PendingOperationType pendingOperation: repoShadow.getPendingOperation()) {
            if (ProvisioningUtil.isOverPeriod(now, expirePeriod, pendingOperation)) {
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
            shadowManager.refreshProvisioningIndexes(ctx, shadow, false, result);
        } catch (ObjectAlreadyExistsException e) {
            LOGGER.debug("Couldn't set `primaryIdentifierValue` for {} - probably a new one was created in the meanwhile. "
                    + "Marking this one as dead.", shadow, e);
            shadowManager.markShadowTombstone(shadow, ctx.getTask(), result);
        }
    }
}
