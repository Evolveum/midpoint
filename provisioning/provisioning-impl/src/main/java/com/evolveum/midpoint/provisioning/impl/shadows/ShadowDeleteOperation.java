/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsUtil.*;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.DeleteOperationState;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Represents/executes "delete" operation on a shadow - either invoked directly, or during refresh or propagation.
 * See the variants of the `execute` method.
 */
public class ShadowDeleteOperation extends ShadowProvisioningOperation<DeleteOperationState> {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowDeleteOperation.class);

    private static final String OP_RESOURCE_OPERATION = ShadowsFacade.class.getName() + ".resourceOperation";

    private final boolean inRefreshOrPropagation;

    private ShadowDeleteOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull DeleteOperationState opState,
            ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts,
            boolean inRefreshOrPropagation) {
        super(ctx, opState, scripts, options, opState.getRepoShadowRequired().asPrismObject().createDeleteDelta());
        this.inRefreshOrPropagation = inRefreshOrPropagation;
    }

    /** Executes when called explicitly from the client. */
    static ShadowType executeDirectly(
            @NotNull ShadowType repoShadow,
            ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        Validate.notNull(repoShadow, "Object to delete must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.trace("Start deleting {}{}", repoShadow, lazy(() -> getAdditionalOperationDesc(scripts, options)));

        InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT);

        ProvisioningContext ctx;
        try {
            ctx = ShadowsLocalBeans.get().ctxFactory.createForShadow(repoShadow, task, result);
            ctx.assertDefinition();
            ctx.checkNotInSimulation();
        } catch (ObjectNotFoundException ex) {
            // If the force option is set, delete shadow from the repo even if the resource does not exist.
            if (ProvisioningOperationOptions.isForce(options)) {
                result.muteLastSubresultError();
                ShadowsLocalBeans.get().shadowUpdater.deleteShadow(repoShadow, task, result);
                result.recordHandledError(
                        "Resource defined in shadow does not exist. Shadow was deleted from the repository.");
                return null;
            } else {
                throw ex;
            }
        }

        ShadowsLocalBeans.get().shadowUpdater.cancelAllPendingOperations(ctx, repoShadow, result);

        DeleteOperationState opState = new DeleteOperationState(repoShadow);
        return new ShadowDeleteOperation(ctx, opState, options, scripts, false)
                .execute(result);
    }

    static DeleteOperationState executeInRefresh(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType repoShadow,
            @NotNull PendingOperationType pendingOperation,
            @Nullable ProvisioningOperationOptions options,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        DeleteOperationState opState = DeleteOperationState.fromPendingOperation(repoShadow, pendingOperation);
        if (ShadowUtil.isExists(repoShadow)) {
            new ShadowDeleteOperation(ctx, opState, options, null, true)
                    .execute(result);
        } else {
            result.recordFatalError("Object does not exist on the resource yet, deletion attempt was skipped");
        }
        return opState;
    }

    static void executeInPropagation(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType repoShadow,
            List<PendingOperationType> sortedOperations,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        DeleteOperationState opState = new DeleteOperationState(repoShadow);
        opState.setPropagatedPendingOperations(sortedOperations);
        new ShadowDeleteOperation(ctx, opState, null, null, true)
                .execute(result);
    }

    private ShadowType execute(OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        if (!inRefreshOrPropagation && checkAndRecordPendingOperationBeforeExecution(result)) {
            return opState.getRepoShadow();
        }

        ShadowType repoShadow = opState.getRepoShadow(); // The shadow may have changed in opState during "checkAndRecord" op.
        ctx.applyAttributesDefinition(repoShadow);

        ShadowLifecycleStateType shadowState = ctx.determineShadowState(repoShadow);

        LOGGER.trace("Deleting object {} from {}, options={}, shadowState={}",
                repoShadow, ctx.getResource(), options, shadowState);

        if (ctx.shouldExecuteResourceOperationDirectly()) {
            executeDeletionOperationDirectly(repoShadow, shadowState, result);
        } else {
            markOperationExecutionAsPending(result);
        }

        ShadowType resultingShadow = resultRecorder.recordDeleteResult(this, result);
        // Since here opState.repoShadow may be erased

        sendSuccessOrInProgressNotification(repoShadow, result);
        setParentOperationStatus(result); // FIXME

        LOGGER.trace("Delete operation for {} finished, resulting shadow: {}", repoShadow, resultingShadow);
        return resultingShadow;
    }

    private void executeDeletionOperationDirectly(
            ShadowType repoShadow, ShadowLifecycleStateType shadowState, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, GenericFrameworkException, SecurityViolationException, PolicyViolationException {

        if (shadowState == ShadowLifecycleStateType.TOMBSTONE) {

            // Do not even try to delete resource object for tombstone shadows.
            // There may be dead shadow and live shadow for the resource object with the same identifiers.
            // If we try to delete dead shadow then we might delete existing object by mistake
            LOGGER.trace("DELETE {}: skipping resource deletion on tombstone shadow", repoShadow);

            opState.setExecutionStatus(COMPLETED);
            result.createSubresult(OP_RESOURCE_OPERATION) // FIXME this hack
                    .recordNotApplicable(); // using "record" to immediately close the result
            return;
        }

        ConnectorOperationOptions connOptions = createConnectorOperationOptions(result);

        LOGGER.trace("DELETE {}: resource deletion, execution starting", repoShadow);

        try {
            ctx.checkNotInMaintenance();

            AsynchronousOperationResult asyncResult =
                    resourceObjectConverter.deleteResourceObject(ctx, repoShadow, scripts, connOptions, result);
            opState.recordRealAsynchronousResult(asyncResult);

            setExecutedDelta(
                    getRequestedDelta());

            // TODO why we don't do this after MODIFY operation?
            ShadowsLocalBeans.get().resourceManager
                    .modifyResourceAvailabilityStatus(
                            ctx.getResourceOid(), AvailabilityStatusType.UP,
                            "deleting " + repoShadow + " finished successfully.", ctx.getTask(), result, false);

        } catch (Exception ex) {
            try {
                statusFromErrorHandling = handleDeleteError(ex, result.getLastSubresult(), result);
            } catch (ObjectAlreadyExistsException e) {
                throw SystemException.unexpected(e);
            }
        } finally {
            LOGGER.debug("DELETE {}: resource operation executed, operation state: {}",
                    repoShadow, opState.shortDumpLazily());
        }
    }

    private OperationResultStatus handleDeleteError(
            Exception cause, OperationResult failedOperationResult, OperationResult result)
            throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException {

        LOGGER.debug("Handling provisioning DELETE exception {}: {}", cause.getClass(), cause.getMessage());
        try {
            OperationResultStatus finalStatus = errorHandlerLocator
                    .locateErrorHandlerRequired(cause)
                    .handleDeleteError(this, cause, failedOperationResult, result);
            LOGGER.debug("Handled provisioning DELETE exception, final status: {}, operation state: {}",
                    finalStatus, opState.shortDumpLazily());
            return finalStatus;
        } catch (CommonException e) {
            LOGGER.debug("Handled provisioning DELETE exception, final exception: {}, operation state: {}",
                    e, opState.shortDumpLazily());
            handleErrorHandlerException(e.getMessage(), result);
            throw e;
        }
    }

    @Override
    public String getGerund() {
        return "deleting";
    }

    @Override
    public String getLogVerb() {
        return "DELETE";
    }

    @Override
    Trace getLogger() {
        return LOGGER;
    }
}
