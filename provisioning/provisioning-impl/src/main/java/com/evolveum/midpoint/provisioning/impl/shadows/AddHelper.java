/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade.OP_DELAYED_OPERATION;
import static com.evolveum.midpoint.provisioning.impl.shadows.Util.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType.*;

import java.util.Collection;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ConstraintsCheckingResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.shadows.errors.ErrorHandler;
import com.evolveum.midpoint.provisioning.impl.shadows.errors.ErrorHandlerLocator;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * Helps with the `add` operation.
 */
@Experimental
@Component
class AddHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AddHelper.class);

    @Autowired private ErrorHandlerLocator errorHandlerLocator;
    @Autowired private Clock clock;
    @Autowired private ShadowsFacade shadowsFacade;
    @Autowired private ResourceObjectConverter resourceObjectConverter;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired protected ShadowManager shadowManager;
    @Autowired private EventDispatcher eventDispatcher;
    @Autowired private AccessChecker accessChecker;
    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;
    @Autowired private CommonHelper commonHelper;
    @Autowired private EntitlementsHelper entitlementsHelper;

    String addResourceObject(
            @NotNull PrismObject<ShadowType> resourceObjectToAdd,
            OperationProvisioningScriptsType scripts,
            ProvisioningOperationOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {

        InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Start adding shadow object{}:\n{}",
                    getAdditionalOperationDesc(scripts, options), resourceObjectToAdd.debugDump(1));
        }

        ResourceType resource = ctxFactory.getResource(resourceObjectToAdd.asObjectable(), task, result);
        ProvisioningContext ctx;
        try {
            ctx = ctxFactory.createForShadow(resourceObjectToAdd.asObjectable(), resource, task);
            ctx.assertDefinition();
        } catch (SchemaException e) {
            result.recordFatalErrorNotFinish(e);
            ResourceOperationDescription operationDescription =
                    ProvisioningUtil.createResourceFailureDescription(
                            resourceObjectToAdd, resource, resourceObjectToAdd.createAddDelta(), result);
            eventDispatcher.notifyFailure(operationDescription, task, result);
            throw e;
        }

        ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState
                = new ProvisioningOperationState<>();

        return addShadowAttempt(ctx, resourceObjectToAdd, scripts, opState, options, task, result);
    }

    String addShadowAttempt(ProvisioningContext ctx,
            PrismObject<ShadowType> resourceObjectToAdd,
            OperationProvisioningScriptsType scripts,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            ProvisioningOperationOptions options,
            Task task,
            OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException,
            ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, EncryptionException {

        PrismContainer<?> attributesContainer = resourceObjectToAdd.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer == null || attributesContainer.isEmpty()) {
            SchemaException e = new SchemaException("Attempt to add resource object without any attributes: " + resourceObjectToAdd);
            parentResult.recordFatalError(e);
            ResourceOperationDescription operationDescription = ProvisioningUtil.createResourceFailureDescription(
                    resourceObjectToAdd, ctx.getResource(), resourceObjectToAdd.createAddDelta(), parentResult);
            eventDispatcher.notifyFailure(operationDescription, task, parentResult);
            throw e;
        }
        if (!(attributesContainer instanceof ResourceAttributeContainer)) {
            shadowCaretaker.applyAttributesDefinition(ctx, resourceObjectToAdd);
        }

        preAddChecks(ctx, resourceObjectToAdd, opState, parentResult);

        shadowManager.addNewProposedShadow(ctx, resourceObjectToAdd, opState, task, parentResult);

        entitlementsHelper.preprocessEntitlements(ctx, resourceObjectToAdd, parentResult);

        shadowCaretaker.applyAttributesDefinition(ctx, resourceObjectToAdd);
        shadowManager.setKindIfNecessary(resourceObjectToAdd.asObjectable(), ctx);
        accessChecker.checkAdd(ctx, resourceObjectToAdd, parentResult);
        PrismObject<ShadowType> addedShadow = null;
        OperationResultStatus finalOperationStatus = null;

        if (shouldExecuteResourceOperationDirectly(ctx)) {

            ConnectorOperationOptions connOptions = commonHelper.createConnectorOperationOptions(ctx, options, parentResult);

            LOGGER.trace("ADD {}: resource operation, execution starting", resourceObjectToAdd);

            try {
                if (ResourceTypeUtil.isInMaintenance(ctx.getResource())) {
                    throw new MaintenanceException("Resource " + ctx.getResource() + " is in the maintenance"); // this tells mp to create pending delta
                }

                // RESOURCE OPERATION: add
                AsynchronousOperationReturnValue<PrismObject<ShadowType>> asyncReturnValue =
                        resourceObjectConverter.addResourceObject(
                                ctx, resourceObjectToAdd, scripts, connOptions, false, parentResult);
                opState.processAsyncResult(asyncReturnValue);
                addedShadow = asyncReturnValue.getReturnValue();

            } catch (ObjectAlreadyExistsException e) {

                LOGGER.trace("Object already exists error when trying to add {}, exploring the situation",
                        ShadowUtil.shortDumpShadowLazily(resourceObjectToAdd));

                // This exception may still be OK in some cases. Such as:
                // We are trying to add a shadow to a semi-manual connector.
                // But the resource object was recently deleted. The object is
                // still in the backing store (CSV file) because of a grace
                // period. Obviously, attempt to add such object would fail.
                // So, we need to handle this case specially. (MID-4414)

                OperationResult failedOperationResult = parentResult.getLastSubresult();

                if (hasDeadShadowWithDeleteOperation(ctx, resourceObjectToAdd, parentResult)) {

                    if (failedOperationResult.isError()) {
                        failedOperationResult.setStatus(OperationResultStatus.HANDLED_ERROR);
                    }

                    // Try again, this time without explicit uniqueness check
                    try {

                        LOGGER.trace("ADD {}: retrying resource operation without uniqueness check (previous dead shadow found), execution starting", resourceObjectToAdd);
                        AsynchronousOperationReturnValue<PrismObject<ShadowType>> asyncReturnValue =
                                resourceObjectConverter
                                        .addResourceObject(ctx, resourceObjectToAdd, scripts, connOptions, true, parentResult);
                        opState.processAsyncResult(asyncReturnValue);
                        addedShadow = asyncReturnValue.getReturnValue();

                    } catch (ObjectAlreadyExistsException innerException) {
                        // Mark shadow dead before we handle the error. ADD operation obviously failed. Therefore this particular
                        // shadow was not created as resource object. It is dead on the spot. Make sure that error handler won't confuse
                        // this shadow with the conflicting shadow that it is going to discover.
                        // This may also be a gestation quantum state collapsing to tombstone
                        shadowManager.markShadowTombstone(opState.getRepoShadow(), task, parentResult);
                        finalOperationStatus = handleAddError(ctx, resourceObjectToAdd, options, opState, innerException, failedOperationResult, task, parentResult);
                    } catch (Exception innerException) {
                        finalOperationStatus = handleAddError(ctx, resourceObjectToAdd, options, opState, innerException, parentResult.getLastSubresult(), task, parentResult);
                    }

                } else {
                    // Mark shadow dead before we handle the error. ADD operation obviously failed. Therefore this particular
                    // shadow was not created as resource object. It is dead on the spot. Make sure that error handler won't confuse
                    // this shadow with the conflicting shadow that it is going to discover.
                    // This may also be a gestation quantum state collapsing to tombstone
                    shadowManager.markShadowTombstone(opState.getRepoShadow(), task, parentResult);
                    finalOperationStatus = handleAddError(ctx, resourceObjectToAdd, options, opState, e, failedOperationResult, task, parentResult);
                }

            } catch (MaintenanceException e) {
                finalOperationStatus = handleAddError(ctx, resourceObjectToAdd, options, opState, e, parentResult.getLastSubresult(), task, parentResult);
                if (opState.getRepoShadow() != null) {
                    setParentOperationStatus(parentResult, opState, finalOperationStatus);
                    return opState.getRepoShadow().getOid();
                }
            } catch (Exception e) {
                finalOperationStatus = handleAddError(ctx, resourceObjectToAdd, options, opState, e, parentResult.getLastSubresult(), task, parentResult);
            }

            LOGGER.debug("ADD {}: resource operation executed, operation state: {}", resourceObjectToAdd, opState.shortDumpLazily());

        } else {
            opState.setExecutionStatus(PendingOperationExecutionStatusType.EXECUTION_PENDING);
            // Create dummy subresult with IN_PROGRESS state.
            // This will force the entire result (parent) to be IN_PROGRESS rather than SUCCESS.
            parentResult.createSubresult(OP_DELAYED_OPERATION)
                    .recordInProgress(); // using "record" to immediately close the result
            LOGGER.debug("ADD {}: resource operation NOT executed, execution pending", resourceObjectToAdd);
        }

        // REPO OPERATION: add
        // This is where the repo shadow is created or updated (if needed)
        shadowManager.recordAddResult(ctx, resourceObjectToAdd, opState, parentResult);

        if (addedShadow == null) {
            addedShadow = resourceObjectToAdd;
        }
        addedShadow.setOid(opState.getRepoShadow().getOid());

        notifyAfterAdd(ctx, addedShadow, opState, task, parentResult);

        setParentOperationStatus(parentResult, opState, finalOperationStatus);

        return opState.getRepoShadow().getOid();
    }

    private boolean hasDeadShadowWithDeleteOperation(ProvisioningContext ctx, PrismObject<ShadowType> shadowToAdd,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {

        Collection<PrismObject<ShadowType>> previousDeadShadows = shadowManager.searchForPreviousDeadShadows(ctx, shadowToAdd, result);
        if (previousDeadShadows.isEmpty()) {
            return false;
        }
        LOGGER.trace("Previous dead shadows:\n{}", DebugUtil.debugDumpLazily(previousDeadShadows, 1));

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        for (PrismObject<ShadowType> previousDeadShadow : previousDeadShadows) {
            if (shadowCaretaker.findPreviousPendingLifecycleOperationInGracePeriod(ctx, previousDeadShadow, now) == ChangeTypeType.DELETE) {
                return true;
            }
        }
        return false;
    }

    private OperationResultStatus handleAddError(ProvisioningContext ctx,
            PrismObject<ShadowType> shadowToAdd,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            Exception cause,
            OperationResult failedOperationResult,
            Task task,
            OperationResult parentResult)
            throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        // TODO: record operationExecution

        ErrorHandler handler = errorHandlerLocator.locateErrorHandler(cause);
        if (handler == null) {
            parentResult.recordFatalErrorNotFinish("Error without a handler: " + cause.getMessage(), cause);
            throw new SystemException(cause.getMessage(), cause);
        }
        LOGGER.debug("Handling provisioning ADD exception {}: {}", cause.getClass(), cause.getMessage());
        try {

            OperationResultStatus finalStatus = handler.handleAddError(ctx, shadowToAdd, options, opState, cause, failedOperationResult, task, parentResult);
            LOGGER.debug("Handled provisioning ADD exception, final status: {}, operation state: {}", finalStatus, opState.shortDumpLazily());
            return finalStatus;

        } catch (CommonException e) {
            LOGGER.debug("Handled provisioning ADD exception, final exception: {}, operation state: {}", e, opState.shortDumpLazily());
            ObjectDelta<ShadowType> delta = shadowToAdd.createAddDelta();
            commonHelper.handleErrorHandlerException(ctx, opState, delta, task, parentResult);
            throw e;
        }

    }

    private void preAddChecks(ProvisioningContext ctx, PrismObject<ShadowType> resourceObjectToAdd,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, SecurityViolationException {
        checkConstraints(resourceObjectToAdd, opState, ctx, result);
        ctx.validateSchema(resourceObjectToAdd.asObjectable());
    }

    private void checkConstraints(PrismObject<ShadowType> resourceObjectToAdd,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            ProvisioningContext ctx,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, ObjectAlreadyExistsException, SecurityViolationException {
        ShadowCheckType shadowConstraintsCheck = ResourceTypeUtil.getShadowConstraintsCheck(ctx.getResource());
        if (shadowConstraintsCheck == ShadowCheckType.NONE) {
            return;
        }

        String shadowOid;
        if (opState.getRepoShadow() != null) {
            shadowOid = opState.getRepoShadow().getOid();
        } else {
            shadowOid = resourceObjectToAdd.getOid();
        }

        ConstraintsChecker checker = new ConstraintsChecker();
        checker.setCacheConfigurationManager(cacheConfigurationManager);
        checker.setShadowsFacade(shadowsFacade); // TODO ok?
        checker.setProvisioningContext(ctx);
        checker.setShadowObject(resourceObjectToAdd);
        checker.setShadowOid(shadowOid);
        checker.setConstraintViolationConfirmer(shadow -> isNotDeadOrReaping(shadow, ctx));
        checker.setUseCache(false);

        ConstraintsCheckingResult checkingResult = checker.check(result);

        LOGGER.trace("Checked {} constraints, result={}", resourceObjectToAdd.debugDumpLazily(),
                checkingResult.isSatisfiesConstraints());
        if (!checkingResult.isSatisfiesConstraints()) {
            throw new ObjectAlreadyExistsException("Conflicting shadow already exists on "+ctx.getResource());
        }
    }

    private boolean isNotDeadOrReaping(PrismObject<ShadowType> shadow, ProvisioningContext ctx)
            throws SchemaException, ConfigurationException {
        if (ShadowUtil.isDead(shadow)) {
            return false;
        }
        ProvisioningContext shadowCtx = ctx.spawnForShadow(shadow.asObjectable());
        shadowsFacade.determineShadowState(shadowCtx, shadow);
        ShadowLifecycleStateType shadowLifecycleState = shadow.asObjectable().getShadowLifecycleState();
        return shadowLifecycleState != REAPING
                && shadowLifecycleState != CORPSE
                && shadowLifecycleState != TOMBSTONE;
    }

    void notifyAfterAdd(
            ProvisioningContext ctx,
            PrismObject<ShadowType> addedShadow,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            Task task,
            OperationResult parentResult) {
        ObjectDelta<ShadowType> delta = DeltaFactory.Object.createAddDelta(addedShadow);
        ResourceOperationDescription operationDescription = Util.createSuccessOperationDescription(
                ctx, addedShadow, delta, parentResult);

        if (opState.isExecuting()) {
            eventDispatcher.notifyInProgress(operationDescription, task, parentResult);
        } else if (opState.isCompleted()) {
            eventDispatcher.notifySuccess(operationDescription, task, parentResult);
        }
    }

}
