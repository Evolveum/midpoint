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

import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;

import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Represents/executes "delete" operation on a shadow - either invoked directly, or during refresh or propagation.
 * See the variants of the `execute` method.
 */
public class ShadowDeleteOperation extends ShadowProvisioningOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowDeleteOperation.class);

    private static final String OP_RESOURCE_OPERATION = ShadowsFacade.class.getName() + ".resourceOperation";

    private final boolean inRefreshOrPropagation;

    private ShadowDeleteOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull ProvisioningOperationState opState,
            ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts,
            boolean inRefreshOrPropagation) {
        super(ctx, opState, scripts, options, opState.getRepoShadowRequired().getPrismObject().createDeleteDelta());
        this.inRefreshOrPropagation = inRefreshOrPropagation;
    }

    /** Executes when called explicitly from the client. */
    static ShadowType executeDirectly(
            @NotNull RawRepoShadow rawRepoShadow,
            ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        Validate.notNull(rawRepoShadow, "Object to delete must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        var b = ShadowsLocalBeans.get();

        LOGGER.trace("Start deleting {}{}", rawRepoShadow, lazy(() -> getAdditionalOperationDesc(scripts, options)));

        InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT);

        ProvisioningContext ctx;
        try {
            ctx = b.ctxFactory.createForShadow(rawRepoShadow.getBean(), task, result);
            ctx.setOperationContext(context);
            ctx.assertDefinition();
            ctx.checkExecutionFullyPersistent();
        } catch (ObjectNotFoundException ex) {
            // If the force option is set, delete shadow from the repo even if the resource does not exist.
            if (ProvisioningOperationOptions.isForce(options)) {
                result.muteLastSubresultError();
                // We will ignore issuing notifications and other ceremonies here. This is an emergency.
                b.repositoryService.deleteObject(ShadowType.class, rawRepoShadow.getOid(), result);
                result.recordHandledError(
                        "Resource defined in shadow does not exist. Shadow was deleted from the repository.");
                return null;
            } else {
                throw ex;
            }
        }

        RepoShadow repoShadow = ctx.adoptRawRepoShadow(rawRepoShadow);
        b.shadowUpdater.cancelAllPendingOperations(ctx, repoShadow, result);

        var opState = new ProvisioningOperationState(repoShadow);
        var repoShadowAfterDeletion =
                new ShadowDeleteOperation(ctx, opState, options, scripts, false)
                        .execute(result);
        return RepoShadow.getBean(repoShadowAfterDeletion);
    }

    static ProvisioningOperationState executeInRefresh(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull PendingOperation pendingOperation,
            @Nullable ProvisioningOperationOptions options,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        var opState = ProvisioningOperationState.fromPendingOperation(repoShadow, pendingOperation);
        if (repoShadow.doesExist()) {
            new ShadowDeleteOperation(ctx, opState, options, null, true)
                    .execute(result);
        } else {
            result.recordFatalError("Object does not exist on the resource yet, deletion attempt was skipped");
        }
        return opState;
    }

    static void executeInPropagation(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull PendingOperations sortedOperations,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        var opState = ProvisioningOperationState.fromPropagatedPendingOperations(repoShadow, sortedOperations);
        new ShadowDeleteOperation(ctx, opState, null, null, true)
                .execute(result);
    }

    private RepoShadow execute(OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        if (!inRefreshOrPropagation && checkAndRecordPendingOperationBeforeExecution(result)) {
            return opState.getRepoShadow();
        }

        RepoShadow repoShadow = opState.getRepoShadow(); // The shadow may have changed in opState during "checkAndRecord" op.

        LOGGER.trace("Deleting object {} from {}, options={}, shadowState={}",
                repoShadow, ctx.getResource(), options, repoShadow.getShadowLifecycleState());

        determineEffectiveMarksAndPolicies(repoShadow, result);

        if (ctx.shouldExecuteResourceOperationDirectly()) {
            executeDeletionOperationDirectly(repoShadow, result);
        } else {
            markOperationExecutionAsPending(result);
        }

        RepoShadow resultingShadow = resultRecorder.recordDeleteResult(this, result);
        // Since here opState.repoShadow may be erased

        sendSuccessOrInProgressNotification(repoShadow, result);
        setParentOperationStatus(result); // FIXME

        LOGGER.trace("Delete operation for {} finished, resulting shadow: {}", repoShadow, resultingShadow);
        return resultingShadow;
    }

    private void executeDeletionOperationDirectly(
            RepoShadow repoShadow, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, GenericFrameworkException, SecurityViolationException, PolicyViolationException {

        var shadowState = repoShadow.getShadowLifecycleState();
        if (shadowState == ShadowLifecycleStateType.TOMBSTONE) {
            // Do not even try to delete resource object for tombstone shadows.
            // There may be dead shadow and live shadow for the resource object with the same identifiers.
            // If we try to delete dead shadow then we might delete existing object by mistake
            LOGGER.trace("DELETE {}: skipping resource deletion on tombstone shadow", repoShadow);
            markNotApplicable(result);
            return;
        }

        if (shadowState == ShadowLifecycleStateType.PROPOSED) {
            // This operation will most probably fail, as there is no such object on the resource.
            LOGGER.trace("DELETE {}: skipping resource deletion on proposed shadow", repoShadow);
            markNotApplicable(result);
            return;
        }

        if (shadowState == ShadowLifecycleStateType.CONCEIVED && !repoShadow.hasPrimaryIdentifier()) {
            // This operation will most probably fail, as there is no primary identifier (e.g., ConnId does not allow this);
            // moreover, in the CONCEIVED state, the object probably does not exist on the resource anyway
            LOGGER.trace("DELETE {}: skipping resource deletion on conceived shadow without primary identifier", repoShadow);
            markNotApplicable(result);
            return;
        }

        ConnectorOperationOptions connOptions = createConnectorOperationOptions(result);

        LOGGER.trace("DELETE {}: resource deletion, execution starting", repoShadow);

        try {
            ctx.checkNotInMaintenance();

            var deleteResult = resourceObjectConverter.deleteResourceObject(ctx, repoShadow, scripts, connOptions, result);
            setOperationStatus(deleteResult);

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

    private void markNotApplicable(OperationResult result) {
        opState.setExecutionStatus(COMPLETED);
        result.createSubresult(OP_RESOURCE_OPERATION) // FIXME this hack
                .recordNotApplicable(); // using "record" to immediately close the result
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
