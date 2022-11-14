/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade.OP_DELAYED_OPERATION;
import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsUtil.*;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;

import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.DeleteOperationState;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.resources.ResourceManager;
import com.evolveum.midpoint.provisioning.impl.shadows.errors.ErrorHandler;
import com.evolveum.midpoint.provisioning.impl.shadows.errors.ErrorHandlerLocator;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Helps with the `delete` operation.
 */
@Component
@Experimental
class DeleteHelper {

    private static final String OP_RESOURCE_OPERATION = ShadowsFacade.class.getName() + ".resourceOperation";

    private static final Trace LOGGER = TraceManager.getTrace(DeleteHelper.class);

    @Autowired private ErrorHandlerLocator errorHandlerLocator;
    @Autowired private ResourceManager resourceManager;
    @Autowired private Clock clock;
    @Autowired private PrismContext prismContext;
    @Autowired private ResourceObjectConverter resourceObjectConverter;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired protected ShadowManager shadowManager;
    @Autowired private EventDispatcher eventDispatcher;
    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private CommonHelper commonHelper;

    public ShadowType deleteShadow(
            ShadowType repoShadow,
            ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts,
            Task task,
            OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
            SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        Validate.notNull(repoShadow, "Object to delete must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.trace("Start deleting {}{}", repoShadow, lazy(() -> getAdditionalOperationDesc(scripts, options)));

        InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT);

        ProvisioningContext ctx;
        try {
            ctx = ctxFactory.createForShadow(repoShadow, task, result);
            ctx.assertDefinition();
        } catch (ObjectNotFoundException ex) {
            // If the force option is set, delete shadow from the repo even if the resource does not exist.
            if (ProvisioningOperationOptions.isForce(options)) {
                result.muteLastSubresultError();
                shadowManager.deleteShadow(repoShadow, task, result);
                result.recordHandledError(
                        "Resource defined in shadow does not exist. Shadow was deleted from the repository.");
                return null;
            } else {
                throw ex;
            }
        }

        cancelAllPendingOperations(ctx, repoShadow, result);

        DeleteOperationState opState = new DeleteOperationState(repoShadow);
        return deleteShadowAttempt(ctx, options, scripts, opState, result);
    }

    ShadowType deleteShadowAttempt(
            ProvisioningContext ctx,
            ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts,
            DeleteOperationState opState,
            OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        ctx.applyAttributesDefinition(opState.getRepoShadow());

        PendingOperationType duplicateOperation =
                shadowManager.checkAndRecordPendingDeleteOperationBeforeExecution(ctx, opState, result);
        if (duplicateOperation != null) {
            result.setInProgress();
            return opState.getRepoShadow();
        }

        ShadowType repoShadow = opState.getRepoShadow();
        ShadowLifecycleStateType shadowState = shadowCaretaker.determineShadowState(ctx, repoShadow);

        LOGGER.trace("Deleting object {} from {}, options={}, shadowState={}", repoShadow, ctx.getResource(), options, shadowState);

        OperationResultStatus finalOperationStatus;
        if (ctx.shouldExecuteResourceOperationDirectly()) {
            finalOperationStatus = deleteShadowDirectly(ctx, options, scripts, opState, shadowState, result);
        } else {
            finalOperationStatus = delayShadowDeletion(opState, result);
        }

        ShadowType resultShadow;
        try {
            resultShadow = shadowManager.recordDeleteResult(ctx, opState, options, result);
        } catch (ObjectNotFoundException ex) {
            result.setFatalError("Can't delete object " + repoShadow + ". Reason: " + ex.getMessage(), ex);
            throw ex.wrap("An error occurred while deleting resource object " + repoShadow);
        } catch (EncryptionException e) {
            throw new SystemException(e.getMessage(), e);
        }

        notifyAfterDelete(ctx, repoShadow, opState, result);

        setParentOperationStatus(result, opState, finalOperationStatus);

        LOGGER.trace("Delete operation for {} finished, result shadow: {}", repoShadow, resultShadow);
        return resultShadow;
    }

    @Nullable
    private OperationResultStatus delayShadowDeletion(ProvisioningOperationState<AsynchronousOperationResult> opState,
            OperationResult result) {
        opState.setExecutionStatus(PendingOperationExecutionStatusType.EXECUTION_PENDING);
        // Create dummy subresult with IN_PROGRESS state.
        // This will force the entire result (parent) to be IN_PROGRESS rather than SUCCESS.
        result.createSubresult(OP_DELAYED_OPERATION)
                .recordInProgress(); // using "record" to immediately close the result
        LOGGER.debug("DELETE {}: resource operation NOT executed, execution pending", opState.getRepoShadow());
        return null;
    }

    private OperationResultStatus deleteShadowDirectly(
            ProvisioningContext ctx,
            ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts,
            DeleteOperationState opState,
            ShadowLifecycleStateType shadowState,
            OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, GenericFrameworkException, SecurityViolationException, PolicyViolationException {

        ShadowType repoShadow = opState.getRepoShadow();

        if (shadowState == ShadowLifecycleStateType.TOMBSTONE) {

            // Do not even try to delete resource object for tombstone shadows.
            // There may be dead shadow and live shadow for the resource object with the same identifiers.
            // If we try to delete dead shadow then we might delete existing object by mistake
            LOGGER.trace("DELETE {}: skipping resource deletion on tombstone shadow", repoShadow);

            opState.setExecutionStatus(COMPLETED);
            result.createSubresult(OP_RESOURCE_OPERATION)
                    .recordNotApplicable(); // using "record" to immediately close the result
            return null;

        }

        ConnectorOperationOptions connOptions = commonHelper.createConnectorOperationOptions(ctx, options, result);

        LOGGER.trace("DELETE {}: resource deletion, execution starting", repoShadow);

        try {
            ctx.checkNotInMaintenance();

            AsynchronousOperationResult asyncResult =
                    resourceObjectConverter.deleteResourceObject(ctx, repoShadow, scripts, connOptions, result);
            opState.recordRealAsynchronousResult(asyncResult);

            resourceManager.modifyResourceAvailabilityStatus(ctx.getResourceOid(), AvailabilityStatusType.UP,
                    "deleting " + repoShadow + " finished successfully.", ctx.getTask(), result, false);

            return null;

        } catch (Exception ex) {
            try {
                return handleDeleteError(ctx, repoShadow, options, opState, ex, result.getLastSubresult(), result);
            } catch (ObjectAlreadyExistsException e) {
                result.recordFatalError(e);
                throw new SystemException(e.getMessage(), e);
            }
        } finally {
            LOGGER.debug("DELETE {}: resource operation executed, operation state: {}", repoShadow, opState.shortDumpLazily());
        }
    }

    DeleteOperationState executeResourceDelete(
            ProvisioningContext ctx,
            ShadowType shadow,
            OperationProvisioningScriptsType scripts,
            ProvisioningOperationOptions options,
            OperationResult result)
            throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        DeleteOperationState opState = new DeleteOperationState(shadow);
        ConnectorOperationOptions connOptions = commonHelper.createConnectorOperationOptions(ctx, options, result);
        try {

            AsynchronousOperationResult asyncResult =
                    resourceObjectConverter.deleteResourceObject(ctx, shadow, scripts, connOptions, result);
            opState.recordRealAsynchronousResult(asyncResult);

        } catch (Exception ex) {
            try {
                handleDeleteError(ctx, shadow, options, opState, ex, result.getLastSubresult(), result);
            } catch (ObjectAlreadyExistsException e) {
                result.recordFatalError(e);
                throw new SystemException(e.getMessage(), e);
            }
        }

        return opState;
    }

    void notifyAfterDelete(
            ProvisioningContext ctx,
            ShadowType shadow,
            ProvisioningOperationState<AsynchronousOperationResult> opState,
            OperationResult result) {
        ObjectDelta<ShadowType> delta =
                prismContext.deltaFactory().object().createDeleteDelta(ShadowType.class, shadow.getOid());
        ResourceOperationDescription operationDescription =
                createSuccessOperationDescription(ctx, shadow, delta, null);

        if (opState.isExecuting()) {
            eventDispatcher.notifyInProgress(operationDescription, ctx.getTask(), result);
        } else {
            eventDispatcher.notifySuccess(operationDescription, ctx.getTask(), result);
        }
    }

    // This is very simple code that essentially works only for postponed operations (retries).
    // TODO: better support for async and manual operations
    private void cancelAllPendingOperations(
            ProvisioningContext ctx, ShadowType repoShadow, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {

        List<PendingOperationType> pendingOperations = repoShadow.getPendingOperation();
        if (pendingOperations.isEmpty()) {
            return;
        }
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        ObjectDelta<ShadowType> shadowDelta = repoShadow.asPrismObject().createModifyDelta();
        for (PendingOperationType pendingOperation: pendingOperations) {
            if (pendingOperation.getExecutionStatus() == COMPLETED) {
                continue;
            }
            if (pendingOperation.getType() != PendingOperationTypeType.RETRY) {
                // Other operations are not cancellable now
                continue;
            }
            ItemPath containerPath = pendingOperation.asPrismContainerValue().getPath();
            PropertyDelta<PendingOperationExecutionStatusType> executionStatusDelta =
                    shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_EXECUTION_STATUS));
            executionStatusDelta.setRealValuesToReplace(COMPLETED);
            shadowDelta.addModification(executionStatusDelta);
            PropertyDelta<XMLGregorianCalendar> completionTimestampDelta =
                    shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_COMPLETION_TIMESTAMP));
            completionTimestampDelta.setRealValuesToReplace(now);
            shadowDelta.addModification(completionTimestampDelta);
            PropertyDelta<OperationResultStatusType> resultStatusDelta =
                    shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_RESULT_STATUS));
            resultStatusDelta.setRealValuesToReplace(OperationResultStatusType.NOT_APPLICABLE);
            shadowDelta.addModification(resultStatusDelta);
        }
        if (shadowDelta.isEmpty()) {
            return;
        }
        LOGGER.debug("Cancelling pending operations on {}", repoShadow);
        shadowManager.modifyShadowAttributes(ctx, repoShadow, shadowDelta.getModifications(), result);
        shadowDelta.applyTo(repoShadow.asPrismObject());
    }

    private OperationResultStatus handleDeleteError(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            ProvisioningOperationOptions options,
            DeleteOperationState opState,
            Exception cause,
            OperationResult failedOperationResult,
            OperationResult result)
            throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException {

        ErrorHandler handler = errorHandlerLocator.locateErrorHandlerRequired(cause);
        LOGGER.debug("Handling provisioning DELETE exception {}: {}", cause.getClass(), cause.getMessage());
        try {

            OperationResultStatus finalStatus = handler.handleDeleteError(ctx, repoShadow, options, opState, cause, failedOperationResult, result);
            LOGGER.debug("Handled provisioning DELETE exception, final status: {}, operation state: {}", finalStatus, opState.shortDumpLazily());
            return finalStatus;

        } catch (CommonException e) {
            LOGGER.debug("Handled provisioning DELETE exception, final exception: {}, operation state: {}", e, opState.shortDumpLazily());
            ObjectDelta<ShadowType> delta = repoShadow.asPrismObject().createDeleteDelta();
            commonHelper.handleErrorHandlerException(ctx, opState, delta, e.getMessage(), result);
            throw e;
        }
    }

}
