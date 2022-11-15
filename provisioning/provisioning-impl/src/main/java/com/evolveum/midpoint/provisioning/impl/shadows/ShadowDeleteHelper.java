/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsUtil.*;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.resources.ResourceManager;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.DeleteOperationState;
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
class ShadowDeleteHelper {

    private static final String OP_RESOURCE_OPERATION = ShadowsFacade.class.getName() + ".resourceOperation";

    private static final Trace LOGGER = TraceManager.getTrace(ShadowDeleteHelper.class);

    @Autowired private ErrorHandlerLocator errorHandlerLocator;
    @Autowired private ResourceManager resourceManager;
    @Autowired private PrismContext prismContext;
    @Autowired private ResourceObjectConverter resourceObjectConverter;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired protected ShadowManager shadowManager;
    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private CommonHelper commonHelper;

    public ShadowType deleteShadow(
            ShadowType repoShadow,
            ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts,
            Task task,
            OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException,
            EncryptionException {

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

        shadowManager.cancelAllPendingOperations(ctx, repoShadow, result);

        DeleteOperationState opState = new DeleteOperationState(repoShadow);
        return executeDeleteAttempt(ctx, options, scripts, opState, false, result);
    }

    /** Called also from refresh and propagation operations. */
    ShadowType executeDeleteAttempt(
            ProvisioningContext ctx,
            ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts,
            DeleteOperationState opState,
            boolean inRefreshOrPropagation,
            OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException,
            EncryptionException {

        return new DeleteOperation(ctx, options, scripts, opState, inRefreshOrPropagation)
                .execute(result);
    }

    class DeleteOperation {

        @NotNull private final ProvisioningContext ctx;
        private final ProvisioningOperationOptions options;
        private final OperationProvisioningScriptsType scripts;
        @NotNull private final DeleteOperationState opState;
        private final boolean inRefreshOrPropagation;

        OperationResultStatus statusFromErrorHandling;

        DeleteOperation(
                @NotNull ProvisioningContext ctx,
                ProvisioningOperationOptions options,
                OperationProvisioningScriptsType scripts,
                @NotNull DeleteOperationState opState,
                boolean inRefreshOrPropagation) {
            this.ctx = ctx;
            this.options = options;
            this.scripts = scripts;
            this.opState = opState;
            this.inRefreshOrPropagation = inRefreshOrPropagation;
        }

        ShadowType execute(OperationResult result)
                throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
                ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, EncryptionException {

            if (!inRefreshOrPropagation) {
                PendingOperationType duplicateOperation =
                        shadowManager.checkAndRecordPendingDeleteOperationBeforeExecution(ctx, opState, result);
                if (duplicateOperation != null) {
                    result.setInProgress();
                    return opState.getRepoShadow();
                }
            } else {
                LOGGER.trace("Not checking for duplicate pending operation, as we are already doing the refresh");
            }

            ShadowType repoShadow = opState.getRepoShadow(); // The shadow may have changed in opState during "checkAndRecord" op.
            ctx.applyAttributesDefinition(repoShadow);

            ShadowLifecycleStateType shadowState = shadowCaretaker.determineShadowState(ctx, repoShadow);

            LOGGER.trace("Deleting object {} from {}, options={}, shadowState={}",
                    repoShadow, ctx.getResource(), options, shadowState);

            if (ctx.shouldExecuteResourceOperationDirectly()) {
                executeDeletionOperationDirectly(repoShadow, shadowState, result);
            } else {
                markOperationExecutionAsPending(LOGGER, "DELETE", opState, result);
            }

            ShadowType resultingShadow = shadowManager.recordDeleteResult(ctx, opState, options, result);
            // Since here opState.repoShadow may be erased

            notifyAfterDelete(ctx, repoShadow, opState, result);
            setParentOperationStatus(result, opState, statusFromErrorHandling); // FIXME

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

            ConnectorOperationOptions connOptions = commonHelper.createConnectorOperationOptions(ctx, options, result);

            LOGGER.trace("DELETE {}: resource deletion, execution starting", repoShadow);

            try {
                ctx.checkNotInMaintenance();

                AsynchronousOperationResult asyncResult =
                        resourceObjectConverter.deleteResourceObject(ctx, repoShadow, scripts, connOptions, result);
                opState.recordRealAsynchronousResult(asyncResult);

                // TODO why we don't do this after MODIFY operation?
                resourceManager.modifyResourceAvailabilityStatus(ctx.getResourceOid(), AvailabilityStatusType.UP,
                        "deleting " + repoShadow + " finished successfully.", ctx.getTask(), result, false);

            } catch (Exception ex) {
                try {
                    statusFromErrorHandling = handleDeleteError(repoShadow, ex, result.getLastSubresult(), result);
                } catch (ObjectAlreadyExistsException e) {
                    throw SystemException.unexpected(e);
                }
            } finally {
                LOGGER.debug("DELETE {}: resource operation executed, operation state: {}",
                        repoShadow, opState.shortDumpLazily());
            }
        }

        private void notifyAfterDelete(
                ProvisioningContext ctx, ShadowType shadow, ProvisioningOperationState<?> opState, OperationResult result) {
            ObjectDelta<ShadowType> delta =
                    prismContext.deltaFactory().object().createDeleteDelta(ShadowType.class, shadow.getOid());
            notifyAboutSuccessOperation(ctx, shadow, opState, delta, result);
        }

        private OperationResultStatus handleDeleteError(
                ShadowType repoShadow, Exception cause, OperationResult failedOperationResult, OperationResult result)
                throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException,
                ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException,
                ExpressionEvaluationException {

            LOGGER.debug("Handling provisioning DELETE exception {}: {}", cause.getClass(), cause.getMessage());
            try {
                OperationResultStatus finalStatus = errorHandlerLocator
                        .locateErrorHandlerRequired(cause)
                        .handleDeleteError(ctx, repoShadow, options, opState, cause, failedOperationResult, result);
                LOGGER.debug("Handled provisioning DELETE exception, final status: {}, operation state: {}",
                        finalStatus, opState.shortDumpLazily());
                return finalStatus;
            } catch (CommonException e) {
                LOGGER.debug("Handled provisioning DELETE exception, final exception: {}, operation state: {}",
                        e, opState.shortDumpLazily());
                ObjectDelta<ShadowType> delta = repoShadow.asPrismObject().createDeleteDelta();
                commonHelper.handleErrorHandlerException(ctx, opState, delta, e.getMessage(), result);
                throw e;
            }
        }

        // Repository shadow may be replaced in opState, hence we want to get it always from there
    }
}
