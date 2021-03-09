/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade.OP_DELAYED_OPERATION;
import static com.evolveum.midpoint.provisioning.impl.shadows.Util.*;

import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.*;
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
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
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

    private static final Trace LOGGER = TraceManager.getTrace(AddHelper.class);

    @Autowired private ErrorHandlerLocator errorHandlerLocator;
    @Autowired private ResourceManager resourceManager;
    @Autowired private Clock clock;
    @Autowired private PrismContext prismContext;
    @Autowired private ResourceObjectConverter resourceObjectConverter;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired protected ShadowManager shadowManager;
    @Autowired private ChangeNotificationDispatcher operationListener;
    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private CommonHelper commonHelper;

    public PrismObject<ShadowType> deleteShadow(PrismObject<ShadowType> repoShadow, ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts, Task task, OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
            SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        Validate.notNull(repoShadow, "Object to delete must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Start deleting {}{}", repoShadow, getAdditionalOperationDesc(scripts, options));
        }

        InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT);

        ProvisioningContext ctx = ctxFactory.create(repoShadow, task, parentResult);
        try {
            ctx.assertDefinition();
        } catch (ObjectNotFoundException ex) {
            // if the force option is set, delete shadow from the repo
            // although the resource does not exists..
            if (ProvisioningOperationOptions.isForce(options)) {
                parentResult.muteLastSubresultError();
                shadowManager.deleteShadow(ctx, repoShadow, parentResult);
                parentResult.recordHandledError(
                        "Resource defined in shadow does not exists. Shadow was deleted from the repository.");
                return null;
            } else {
                throw ex;
            }
        }

        PrismObject<ShadowType> updatedRepoShadow = cancelAllPendingOperations(ctx, repoShadow, parentResult);

        ProvisioningOperationState<AsynchronousOperationResult> opState = new ProvisioningOperationState<>();
        opState.setRepoShadow(updatedRepoShadow);

        return deleteShadowAttempt(ctx, options, scripts, opState, task, parentResult);
    }

    PrismObject<ShadowType> deleteShadowAttempt(ProvisioningContext ctx,
            ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts,
            ProvisioningOperationState<AsynchronousOperationResult> opState,
            Task task,
            OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
            SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        final PrismObject<ShadowType> repoShadow = opState.getRepoShadow();
        shadowCaretaker.applyAttributesDefinition(ctx, repoShadow);

        PendingOperationType duplicateOperation = shadowManager.checkAndRecordPendingDeleteOperationBeforeExecution(ctx,
                repoShadow, opState, task, parentResult);
        if (duplicateOperation != null) {
            parentResult.recordInProgress();
            return repoShadow;
        }

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        ShadowState shadowState = shadowCaretaker.determineShadowState(ctx, repoShadow, now);

        LOGGER.trace("Deleting object {} from {}, options={}, shadowState={}", repoShadow, ctx.getResource(), options, shadowState);
        OperationResultStatus finalOperationStatus = null;

        if (shouldExecuteResourceOperationDirectly(ctx)) {

            if (shadowState == ShadowState.TOMBSTONE) {

                // Do not even try to delete resource object for tombstone shadows.
                // There may be dead shadow and live shadow for the resource object with the same identifiers.
                // If we try to delete dead shadow then we might delete existing object by mistake
                LOGGER.trace("DELETE {}: skipping resource deletion on tombstone shadow", repoShadow);

                opState.setExecutionStatus(PendingOperationExecutionStatusType.COMPLETED);
                OperationResult delayedSubresult = parentResult.createSubresult(OP_RESOURCE_OPERATION);
                delayedSubresult.setStatus(OperationResultStatus.NOT_APPLICABLE);

            } else {

                ConnectorOperationOptions connOptions = commonHelper.createConnectorOperationOptions(ctx, options, parentResult);

                LOGGER.trace("DELETE {}: resource deletion, execution starting", repoShadow);

                try {
                    if (ResourceTypeUtil.isInMaintenance(ctx.getResource())) {
                        throw new MaintenanceException("Resource " + ctx.getResource() + " is in the maintenance");
                    }

                    AsynchronousOperationResult asyncReturnValue = resourceObjectConverter
                            .deleteResourceObject(ctx, repoShadow, scripts, connOptions, parentResult);
                    opState.processAsyncResult(asyncReturnValue);

                    String operationCtx = "deleting " + repoShadow + " finished successfully.";
                    resourceManager.modifyResourceAvailabilityStatus(ctx.getResourceOid(), AvailabilityStatusType.UP, operationCtx, task, parentResult, false);

                } catch (Exception ex) {
                    try {
                        finalOperationStatus = handleDeleteError(ctx, repoShadow, options, opState, ex, parentResult.getLastSubresult(), task, parentResult);
                    } catch (ObjectAlreadyExistsException e) {
                        parentResult.recordFatalError(e);
                        throw new SystemException(e.getMessage(), e);
                    }
                }

                LOGGER.debug("DELETE {}: resource operation executed, operation state: {}", repoShadow, opState.shortDumpLazily());
            }

        } else {
            opState.setExecutionStatus(PendingOperationExecutionStatusType.EXECUTION_PENDING);
            // Create dummy subresult with IN_PROGRESS state.
            // This will force the entire result (parent) to be IN_PROGRESS rather than SUCCESS.
            OperationResult delayedSubresult = parentResult.createSubresult(OP_DELAYED_OPERATION);
            delayedSubresult.setStatus(OperationResultStatus.IN_PROGRESS);
            LOGGER.debug("DELETE {}: resource operation NOT executed, execution pending", repoShadow);
        }

        now = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> resultShadow;
        try {
            resultShadow = shadowManager.recordDeleteResult(ctx, repoShadow, opState, options, now, parentResult);
        } catch (ObjectNotFoundException ex) {
            parentResult.recordFatalError("Can't delete object " + repoShadow + ". Reason: " + ex.getMessage(),
                    ex);
            throw new ObjectNotFoundException("An error occurred while deleting resource object " + repoShadow
                    + " with identifiers " + repoShadow + ": " + ex.getMessage(), ex);
        } catch (EncryptionException e) {
            throw new SystemException(e.getMessage(), e);
        }

        notifyAfterDelete(ctx, repoShadow, opState, task, parentResult);

        setParentOperationStatus(parentResult, opState, finalOperationStatus);

        LOGGER.trace("Delete operation for {} finished, result shadow: {}", repoShadow, resultShadow);
        return resultShadow;
    }

    ProvisioningOperationState<AsynchronousOperationResult> executeResourceDelete(ProvisioningContext ctx,
            PrismObject<ShadowType> shadow, OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options,
            Task task,
            OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        ProvisioningOperationState<AsynchronousOperationResult> opState = new ProvisioningOperationState<>();
        opState.setRepoShadow(shadow);
        ConnectorOperationOptions connOptions = commonHelper.createConnectorOperationOptions(ctx, options, parentResult);
        try {

            AsynchronousOperationResult asyncReturnValue = resourceObjectConverter
                    .deleteResourceObject(ctx, shadow, scripts, connOptions , parentResult);
            opState.processAsyncResult(asyncReturnValue);

        } catch (Exception ex) {
            try {
                handleDeleteError(ctx, shadow, options, opState, ex, parentResult.getLastSubresult(), task, parentResult);
            } catch (ObjectAlreadyExistsException e) {
                parentResult.recordFatalError(e);
                throw new SystemException(e.getMessage(), e);
            }
        }

        return opState;
    }

    void notifyAfterDelete(
            ProvisioningContext ctx,
            PrismObject<ShadowType> shadow,
            ProvisioningOperationState<AsynchronousOperationResult> opState,
            Task task,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createDeleteDelta(shadow.getCompileTimeClass(),
                shadow.getOid());
        ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, shadow,
                delta, parentResult);

        if (opState.isExecuting()) {
            operationListener.notifyInProgress(operationDescription, task, parentResult);
        } else {
            operationListener.notifySuccess(operationDescription, task, parentResult);
        }
    }

    // This is very simple code that essentially works only for postponed operations (retries).
    // TODO: better support for async and manual operations
    private PrismObject<ShadowType> cancelAllPendingOperations(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException,
            ExpressionEvaluationException {

        List<PendingOperationType> pendingOperations = repoShadow.asObjectable().getPendingOperation();
        if (pendingOperations.isEmpty()) {
            return repoShadow;
        }
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        ObjectDelta<ShadowType> shadowDelta = repoShadow.createModifyDelta();
        for (PendingOperationType pendingOperation: pendingOperations) {
            if (pendingOperation.getExecutionStatus() == PendingOperationExecutionStatusType.COMPLETED) {
                continue;
            }
            if (pendingOperation.getType() != PendingOperationTypeType.RETRY) {
                // Other operations are not cancellable now
                continue;
            }
            ItemPath containerPath = pendingOperation.asPrismContainerValue().getPath();
            PropertyDelta<PendingOperationExecutionStatusType> executionStatusDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_EXECUTION_STATUS));
            executionStatusDelta.setRealValuesToReplace(PendingOperationExecutionStatusType.COMPLETED);
            shadowDelta.addModification(executionStatusDelta);
            PropertyDelta<XMLGregorianCalendar> completionTimestampDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_COMPLETION_TIMESTAMP));
            completionTimestampDelta.setRealValuesToReplace(now);
            shadowDelta.addModification(completionTimestampDelta);
            PropertyDelta<OperationResultStatusType> resultStatusDelta = shadowDelta.createPropertyModification(containerPath.append(PendingOperationType.F_RESULT_STATUS));
            resultStatusDelta.setRealValuesToReplace(OperationResultStatusType.NOT_APPLICABLE);
            shadowDelta.addModification(resultStatusDelta);
        }
        if (shadowDelta.isEmpty()) {
            return repoShadow;
        }
        LOGGER.debug("Cancelling pending operations on {}", repoShadow);
        shadowManager.modifyShadowAttributes(ctx, repoShadow, shadowDelta.getModifications(), result);
        shadowDelta.applyTo(repoShadow);
        return repoShadow;
    }

    private OperationResultStatus handleDeleteError(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationResult> opState,
            Exception cause,
            OperationResult failedOperationResult,
            Task task,
            OperationResult parentResult)
            throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        ErrorHandler handler = errorHandlerLocator.locateErrorHandler(cause);
        if (handler == null) {
            parentResult.recordFatalError("Error without a handler: " + cause.getMessage(), cause);
            throw new SystemException(cause.getMessage(), cause);
        }
        LOGGER.debug("Handling provisioning DELETE exception {}: {}", cause.getClass(), cause.getMessage());
        try {

            OperationResultStatus finalStatus = handler.handleDeleteError(ctx, repoShadow, options, opState, cause, failedOperationResult, task, parentResult);
            LOGGER.debug("Handled provisioning DELETE exception, final status: {}, operation state: {}", finalStatus, opState.shortDumpLazily());
            return finalStatus;

        } catch (CommonException e) {
            LOGGER.debug("Handled provisioning DELETE exception, final exception: {}, operation state: {}", e, opState.shortDumpLazily());
            ObjectDelta<ShadowType> delta = repoShadow.createDeleteDelta();
            commonHelper.handleErrorHandlerException(ctx, opState, delta, task, parentResult);
            throw e;
        }
    }

}
