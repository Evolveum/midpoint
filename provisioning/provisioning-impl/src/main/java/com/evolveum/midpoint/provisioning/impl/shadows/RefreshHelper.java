/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.provisioning.impl.shadows.Util.createSuccessOperationDescription;
import static com.evolveum.midpoint.provisioning.impl.shadows.Util.needsRetry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;

import org.jetbrains.annotations.Nullable;
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
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.*;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.RefreshShadowOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
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
    @Autowired private ChangeNotificationDispatcher operationListener;
    @Autowired private ChangeNotificationDispatcher changeNotificationDispatcher;
    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private AddHelper addHelper;
    @Autowired private ModifyHelper modifyHelper;
    @Autowired private DeleteHelper deleteHelper;
    @Autowired private DefinitionsHelper definitionsHelper;

    @Nullable
    public RefreshShadowOperation refreshShadow(PrismObject<ShadowType> repoShadow, ProvisioningOperationOptions options,
            Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, EncryptionException {

        LOGGER.trace("Refreshing {}", repoShadow);
        ProvisioningContext ctx = ctxFactory.create(repoShadow, task, parentResult);
        ctx.assertDefinition();
        shadowCaretaker.applyAttributesDefinition(ctx, repoShadow);

        repoShadow = shadowManager.refreshProvisioningIndexes(ctx, repoShadow, task, parentResult);

        RefreshShadowOperation refreshShadowOperation = refreshShadowPendingOperations(ctx, repoShadow, options, task, parentResult);

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        repoShadow = cleanUpDeadShadow(ctx, repoShadow, now, task, parentResult);
        if (repoShadow == null) {
            refreshShadowOperation.setRefreshedShadow(null);
        }

        return refreshShadowOperation;
    }

    private RefreshShadowOperation refreshShadowPendingOperations(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow,
            ProvisioningOperationOptions options, Task task, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, EncryptionException {
        ShadowType shadowType = repoShadow.asObjectable();
        List<PendingOperationType> pendingOperations = shadowType.getPendingOperation();
        boolean isDead = ShadowUtil.isDead(shadowType);
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
        List<PendingOperationType> sortedOperations = shadowCaretaker.sortPendingOperations(shadowType.getPendingOperation());

        refreshShadowAsyncStatus(ctx, repoShadow, sortedOperations, task, parentResult);

        return refreshShadowRetryOperations(ctx, repoShadow, sortedOperations, options, task, parentResult);
    }

    /**
     * Used to quickly and efficiently refresh shadow before GET operations.
     */
    PrismObject<ShadowType> refreshShadowQuick(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow,
            XMLGregorianCalendar now, Task task, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        ObjectDelta<ShadowType> shadowDelta = repoShadow.createModifyDelta();
        expirePendingOperations(ctx, repoShadow, shadowDelta, now);

        if (!shadowDelta.isEmpty()) {
            shadowManager.modifyShadowAttributes(ctx, repoShadow, shadowDelta.getModifications(), parentResult);
            shadowDelta.applyTo(repoShadow);
        }

        repoShadow = cleanUpDeadShadow(ctx, repoShadow, now, task, parentResult);

        return repoShadow;
    }

    /**
     * Refresh status of asynchronous operation, e.g. status of manual connector ticket.
     * This method will get new status from resourceObjectConverter and it will process the
     * status in case that it has changed.
     */
    private void refreshShadowAsyncStatus(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow,
            List<PendingOperationType> sortedOperations, Task task, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);

        List<ObjectDelta<ShadowType>> notificationDeltas = new ArrayList<>();

        boolean shadowInception = false;
        OperationResultStatusType shadowInceptionOutcome = null;
        ObjectDelta<ShadowType> shadowDelta = repoShadow.createModifyDelta();
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
                LOGGER.debug("Communication error while trying to refresh pending operation of {}. Skipping refresh of this operation.", repoShadow, e);
                parentResult.recordPartialError(e);
                continue;
            }
            OperationResultStatus newStatus = refreshAsyncResult.getOperationResult().getStatus();

            if (newStatus == null) {
                continue;
            }
            OperationResultStatusType newStatusType = newStatus.createStatusType();
            if (newStatusType.equals(pendingOperation.getResultStatus())) {
                continue;
            }

            boolean operationCompleted = ProvisioningUtil.isCompleted(newStatusType) && pendingOperation.getCompletionTimestamp() == null;

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
                    shadowManager.addDeadShadowDeltas(repoShadow, (List)shadowDelta.getModifications());
                }
                continue;

            } else {
                PropertyDelta<OperationResultStatusType> resultStatusDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_RESULT_STATUS));
                resultStatusDelta.setRealValuesToReplace(newStatusType);
                shadowDelta.addModification(resultStatusDelta);
            }

            if (operationCompleted) {
                // Operation completed

                PropertyDelta<PendingOperationExecutionStatusType> executionStatusDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_EXECUTION_STATUS));
                executionStatusDelta.setRealValuesToReplace(PendingOperationExecutionStatusType.COMPLETED);
                shadowDelta.addModification(executionStatusDelta);

                PropertyDelta<XMLGregorianCalendar> completionTimestampDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_COMPLETION_TIMESTAMP));
                completionTimestampDelta.setRealValuesToReplace(clock.currentTimeXMLGregorianCalendar());
                shadowDelta.addModification(completionTimestampDelta);

                ObjectDeltaType pendingDeltaType = pendingOperation.getDelta();
                ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType, prismContext);

                if (pendingDelta.isAdd()) {
                    shadowInception = true;
                    shadowInceptionOutcome = newStatusType;
                }

                if (pendingDelta.isModify()) {

                    // Apply shadow naming attribute modification
                    PrismContainer<ShadowAttributesType> shadowAttributesContainer = repoShadow.findContainer(ItemPath.create(ShadowType.F_ATTRIBUTES));
                    ResourceAttributeContainer resourceAttributeContainer = ResourceAttributeContainer.convertFromContainer(shadowAttributesContainer, ctx.getObjectClassDefinition());
                    ResourceAttributeContainerDefinition resourceAttrDefinition = resourceAttributeContainer.getDefinition();
                    if(resourceAttrDefinition != null) {

                        // If naming attribute is present in delta...
                        ResourceAttributeDefinition namingAttribute = resourceAttrDefinition.getNamingAttribute();
                        if (namingAttribute != null) {
                            if (pendingDelta.hasItemDelta(ItemPath.create(ShadowType.F_ATTRIBUTES, namingAttribute.getItemName()))) {

                                // Retrieve a possible changed name per the defined naming attribute for the resource
                                ItemDelta namingAttributeDelta = pendingDelta.findItemDelta(ItemPath.create(ShadowType.F_ATTRIBUTES, namingAttribute.getItemName()));
                                Collection<?> valuesToReplace = namingAttributeDelta.getValuesToReplace();
                                Optional<?> valueToReplace = valuesToReplace.stream().findFirst();

                                if (valueToReplace.isPresent()){
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
            shadowCaretaker.applyAttributesDefinition(ctx, shadowDelta);
            shadowManager.modifyShadowAttributes(ctx, repoShadow, shadowDelta.getModifications(), parentResult);
        }

        for (ObjectDelta<ShadowType> notificationDelta: notificationDeltas) {
            ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, repoShadow,
                    notificationDelta, parentResult);
            operationListener.notifySuccess(operationDescription, task, parentResult);
        }

        if (!shadowDelta.isEmpty()) {
            shadowDelta.applyTo(repoShadow);
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

    private RefreshShadowOperation refreshShadowRetryOperations(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow, List<PendingOperationType> sortedOperations,
            ProvisioningOperationOptions options, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, EncryptionException {
        ShadowType shadowType = repoShadow.asObjectable();
        OperationResult retryResult = new OperationResult(OP_REFRESH_RETRY);
        if (ShadowUtil.isDead(shadowType)) {
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
            ObjectDelta<ShadowType> shadowDelta = repoShadow.createModifyDelta();
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
            shadowDelta.applyTo(repoShadow);

            ObjectDeltaType pendingDeltaType = pendingOperation.getDelta();
            ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType, prismContext);

            ProvisioningOperationState<? extends AsynchronousOperationResult> opState =
                    ProvisioningOperationState.fromPendingOperation(repoShadow, pendingOperation);

            LOGGER.debug("Retrying operation {} on {}, attempt #{}", pendingDelta, repoShadow, attemptNumber);

            OperationResult result = parentResult.createSubresult(OP_OPERATION_RETRY);
            ObjectDeltaOperation<ShadowType> objectDeltaOperation = new ObjectDeltaOperation<>(pendingDelta);
            try {
                retryOperation(ctx, pendingDelta, opState, task, result);
                repoShadow = opState.getRepoShadow();
                result.computeStatus();
                if (result.isError()) {
                    retryResult.setStatus(result.getStatus());
                }
                //TODO maybe add whole "result" as subresult to the retryResult?
                result.muteError();
            } catch (CommunicationException | GenericFrameworkException | ObjectAlreadyExistsException | SchemaException | ObjectNotFoundException | ConfigurationException | SecurityViolationException e) {
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
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState, Task task, OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {

        ProvisioningOperationOptions options = ProvisioningOperationOptions.createForceRetry(false);
        OperationProvisioningScriptsType scripts = null; // TODO
        if (pendingDelta.isAdd()) {
            PrismObject<ShadowType> resourceObjectToAdd = pendingDelta.getObjectToAdd();
            addHelper.addShadowAttempt(ctx, resourceObjectToAdd, scripts,
                    (ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>>) opState,
                    options, task, result);
            opState.setRepoShadow(resourceObjectToAdd);
        }

        if (pendingDelta.isModify()) {
            modifyHelper.modifyShadowAttempt(ctx, pendingDelta.getModifications(), scripts, options,
                    (ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>>) opState,
                    true, task, result);
        }

        if (pendingDelta.isDelete()) {
            PrismObject<ShadowType> shadow = deleteHelper.deleteShadowAttempt(ctx, options, scripts,
                    (ProvisioningOperationState<AsynchronousOperationResult>) opState,
                    task, result);
            if (shadow == null || ShadowUtil.isDead(shadow)) {
                notifyObjectDeleted(ctx, shadow, opState.getRepoShadow(), task, result);
            }
        }
    }

    /**
     * We have to notify model that the shadow was deleted. This is mainly to unlink the shadow from its owner. (MID-6862)
     *
     * It is interesting that the provisioning normally does not need to notify model on shadow deletion: It is because during
     * normal operation, the model itself requests the deletion, so it knows is has to do unlink the shadow. But this time
     * it is different. Model does not unlink if the deletion is not successful, so we have to do that after successful retry.
     */
    private void notifyObjectDeleted(ProvisioningContext ctx, PrismObject<ShadowType> shadowAfter,
            PrismObject<ShadowType> shadowBefore, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        PrismObject<ShadowType> shadow = getShadowForNotification(shadowAfter, shadowBefore);
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setObjectDelta(shadow.createDeleteDelta());
        change.setResource(ctx.getResource().asPrismObject());
        change.setShadowedResourceObject(shadow);
        change.setSourceChannel(java.util.Objects.requireNonNullElse(task.getChannel(), SchemaConstants.CHANNEL_DISCOVERY_URI)); // TODO good channel?
        changeNotificationDispatcher.notifyChange(change, task, result);
    }

    /**
     * What shadow should we use for the notification?
     *
     * @param shadowAfter Shadow as returned from `deleteShadowAttempt` method. Dead, but sometimes null.
     * @param shadowBefore Shadow as present in `opState`. Probably not dead? Not sure.
     * @return Shadow that can be used for the notification.
     */
    private PrismObject<ShadowType> getShadowForNotification(PrismObject<ShadowType> shadowAfter,
            PrismObject<ShadowType> shadowBefore) {
        if (shadowAfter != null) {
            return shadowAfter;
        } else if (shadowBefore != null) {
            return shadowBefore;
        } else {
            throw new IllegalStateException("No shadow to use for notification. Both 'before' and 'after' shadows are null.");
        }
    }

    private boolean isAfterRetryPeriod(PendingOperationType pendingOperation, Duration retryPeriod, XMLGregorianCalendar now) {
        XMLGregorianCalendar lastAttemptTimestamp = pendingOperation.getLastAttemptTimestamp();
        XMLGregorianCalendar scheduledRetryTimestamp = XmlTypeConverter.addDuration(lastAttemptTimestamp, retryPeriod);
        return XmlTypeConverter.compare(now, scheduledRetryTimestamp) == DatatypeConstants.GREATER;
    }

    private PrismObject<ShadowType> cleanUpDeadShadow(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow,
            XMLGregorianCalendar now, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ShadowType shadowType = repoShadow.asObjectable();
        if (!ShadowUtil.isDead(shadowType)) {
            return repoShadow;
        }
        Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);
        Duration deadRetentionPeriod = ProvisioningUtil.getDeadShadowRetentionPeriod(ctx);
        Duration expirationPeriod = XmlTypeConverter.longerDuration(gracePeriod, deadRetentionPeriod);
        XMLGregorianCalendar lastActivityTimestamp = null;

        for (PendingOperationType pendingOperation: shadowType.getPendingOperation()) {
            lastActivityTimestamp = XmlTypeConverter.laterTimestamp(lastActivityTimestamp, pendingOperation.getRequestTimestamp());
            lastActivityTimestamp = XmlTypeConverter.laterTimestamp(lastActivityTimestamp, pendingOperation.getLastAttemptTimestamp());
            lastActivityTimestamp = XmlTypeConverter.laterTimestamp(lastActivityTimestamp, pendingOperation.getCompletionTimestamp());
        }
        if (lastActivityTimestamp == null) {
            MetadataType metadata = shadowType.getMetadata();
            if (metadata != null) {
                lastActivityTimestamp = metadata.getModifyTimestamp();
                if (lastActivityTimestamp == null) {
                    lastActivityTimestamp = metadata.getCreateTimestamp();
                }
            }
        }

        // Explicitly check for zero deadRetentionPeriod to avoid some split-millisecond issues with dead shadow deletion.
        // If we have zero deadRetentionPeriod, we should get rid of all dead shadows immediately.
        if (XmlTypeConverter.isZero(deadRetentionPeriod) || ProvisioningUtil.isOverPeriod(now, expirationPeriod, lastActivityTimestamp)) {
            // Perish you stinking corpse!
            LOGGER.debug("Deleting dead {} because it is expired", repoShadow);
            shadowManager.deleteShadow(ctx, repoShadow, parentResult);
            ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
            change.setCleanDeadShadow(true);
            change.setShadowedResourceObject(repoShadow);
            change.setResource(ctx.getResource().asPrismObject());
            change.setObjectDelta(repoShadow.createDeleteDelta());
            change.setSourceChannel(SchemaConstants.CHANNEL_DISCOVERY_URI);
            changeNotificationDispatcher.notifyChange(change, task, parentResult);
            definitionsHelper.applyDefinition(repoShadow, parentResult);
            ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, repoShadow,
                    repoShadow.createDeleteDelta(), parentResult);
            operationListener.notifySuccess(operationDescription, task, parentResult);
            return null;
        } else {
            LOGGER.trace("Keeping dead {} because it is not expired yet, last activity={}, expiration period={}", repoShadow, lastActivityTimestamp, expirationPeriod);
            return repoShadow;
        }
    }

    private void expirePendingOperations(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow,
            ObjectDelta<ShadowType> shadowDelta, XMLGregorianCalendar now)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ShadowType shadowType = repoShadow.asObjectable();

        Duration gracePeriod = ProvisioningUtil.getGracePeriod(ctx);
        Duration pendingOperationRetentionPeriod = ProvisioningUtil.getPendingOperationRetentionPeriod(ctx);
        Duration expirePeriod = XmlTypeConverter.longerDuration(gracePeriod, pendingOperationRetentionPeriod);
        for (PendingOperationType pendingOperation: shadowType.getPendingOperation()) {
            if (ProvisioningUtil.isOverPeriod(now, expirePeriod, pendingOperation)) {
                LOGGER.trace("Deleting pending operation because it is completed '{}' and expired: {}",
                        pendingOperation.getResultStatus(), pendingOperation);
                shadowDelta.addModificationDeleteContainer(ShadowType.F_PENDING_OPERATION, pendingOperation.clone());
            }
        }
    }

}
