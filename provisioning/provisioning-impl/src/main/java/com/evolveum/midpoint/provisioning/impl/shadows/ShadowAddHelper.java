/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.prism.delta.DeltaFactory.Object.createAddDelta;
import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsUtil.*;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType.*;

import java.util.Collection;
import java.util.Objects;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.AddOperationState;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ConstraintsCheckingResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.shadows.errors.ErrorHandlerLocator;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * Helps with the `add` operation. Wraps {@link AddOperation}, providing code to invoke it neatly via
 * {@link #executeAddAttempt(ProvisioningContext, ShadowType, ProvisioningOperationOptions,
 * OperationProvisioningScriptsType, AddOperationState, OperationResult)} method.
 *
 * (Unlike {@link ShadowGetOperation} and {@link ShadowSearchLikeOperation}, the "add" operation is called from other
 * parts of the shadows package. Hence the need.)
 */
@Experimental
@Component
class ShadowAddHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowAddHelper.class);

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
            @NotNull ShadowType resourceObjectToAdd,
            OperationProvisioningScriptsType scripts,
            ProvisioningOperationOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {

        InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT);
        LOGGER.trace("Start adding shadow object{}:\n{}",
                lazy(() -> getAdditionalOperationDesc(scripts, options)),
                resourceObjectToAdd.debugDumpLazily(1));

        ProvisioningContext ctx = establishProvisioningContext(resourceObjectToAdd, task, result);
        AddOperationState opState = new AddOperationState();
        return executeAddAttempt(ctx, resourceObjectToAdd, options, scripts, opState, result);
    }

    private @NotNull ProvisioningContext establishProvisioningContext(
            @NotNull ShadowType resourceObjectToAdd, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException,
            CommunicationException {
        ResourceType resource = ctxFactory.getResource(resourceObjectToAdd, task, result);
        try {
            ProvisioningContext ctx = ctxFactory.createForShadow(resourceObjectToAdd, resource, task);
            ctx.assertDefinition();
            return ctx;
        } catch (SchemaException e) { // TODO why only for this kind of exception?
            eventDispatcher.notifyFailure(
                    createResourceFailureDescription(
                            resourceObjectToAdd, resource, resourceObjectToAdd.asPrismObject().createAddDelta(), e.getMessage()),
                    task,
                    result);
            throw e;
        }
    }

    /** Called also during refresh or propagation. */
    String executeAddAttempt(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType resourceObjectToAdd,
            ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts,
            @NotNull AddOperationState opState,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {
        try {
            return new AddOperation(ctx, resourceObjectToAdd, scripts, opState, options)
                    .execute(result);
        } catch (SchemaException e) { // TODO why only for this kind of exception?
            eventDispatcher.notifyFailure(
                    createResourceFailureDescription(
                            resourceObjectToAdd,
                            ctx.getResource(),
                            resourceObjectToAdd.asPrismObject().createAddDelta(),
                            e.getMessage()),
                    ctx.getTask(), result);
            throw e;
        }
    }

    private class AddOperation {

        @NotNull private final ProvisioningContext ctx;
        @NotNull private final ShadowType resourceObjectToAdd;
        private final OperationProvisioningScriptsType scripts;
        @NotNull private final AddOperationState opState;
        private final ProvisioningOperationOptions options;
        @NotNull private final Task task;

        private OperationResultStatus statusFromErrorHandling;

        AddOperation(
                @NotNull ProvisioningContext ctx,
                @NotNull ShadowType resourceObjectToAdd,
                OperationProvisioningScriptsType scripts,
                @NotNull AddOperationState opState,
                ProvisioningOperationOptions options) {
            this.ctx = ctx;
            this.resourceObjectToAdd = resourceObjectToAdd;
            this.scripts = scripts;
            this.opState = opState;
            this.options = options;
            this.task = ctx.getTask();
        }

        String execute(OperationResult result)
                throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
                ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
                ExpressionEvaluationException, EncryptionException {

            checkAttributesPresent();
            ctx.applyAttributesDefinition(resourceObjectToAdd);
            ctx.validateSchemaIfConfigured(resourceObjectToAdd);

            accessChecker.checkAddAccess(ctx, resourceObjectToAdd, result);

            setProductionFlag();

            executeShadowConstraintsCheck(result); // To avoid shadow duplication (if configured so)
            shadowManager.addNewProposedShadow(ctx, resourceObjectToAdd, opState, result); // If configured & if not existing yet

            entitlementsHelper.provideEntitlementsIdentifiers(ctx, resourceObjectToAdd, result);

            if (ctx.shouldExecuteResourceOperationDirectly()) {
                executeAddOperationDirectly(result);
            } else {
                markOperationExecutionAsPending(LOGGER, "ADD", opState, result);
            }

            // This is where the repo shadow is created or updated (if needed)
            shadowManager.recordAddResult(ctx, resourceObjectToAdd, opState, result);
            notifyAfterAdd(result);
            setParentOperationStatus(result, opState, statusFromErrorHandling); // FIXME

            return opState.getRepoShadowOid();
        }

        private void setProductionFlag() {
            resourceObjectToAdd.setSimulated(
                    !ctx.isObjectDefinitionInProduction());
        }

        private void executeAddOperationDirectly(OperationResult result)
                throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
                ExpressionEvaluationException, GenericFrameworkException, ObjectAlreadyExistsException,
                SecurityViolationException, PolicyViolationException {
            ConnectorOperationOptions connOptions = commonHelper.createConnectorOperationOptions(ctx, options, result);

            LOGGER.trace("ADD {}: resource operation, execution starting", resourceObjectToAdd);

            try {

                ctx.checkNotInMaintenance();
                doExecuteAddOperation(connOptions, false, result);

            } catch (ObjectAlreadyExistsException e) {

                LOGGER.trace("Object already exists error when trying to add {}, exploring the situation",
                        ShadowUtil.shortDumpShadowLazily(resourceObjectToAdd));

                // This exception may still be OK in some cases. Such as:
                // We are trying to add a shadow to a semi-manual connector.
                // But the resource object was recently deleted. The object is
                // still in the backing store (CSV file) because of a grace
                // period. Obviously, attempt to add such object would fail.
                // So, we need to handle this case specially. (MID-4414)

                OperationResult failedOperationResult = result.getLastSubresult(); // This may or may not be correct

                if (hasDeadShadowWithDeleteOperation(ctx, resourceObjectToAdd, result)) {

                    if (failedOperationResult.isError()) {
                        failedOperationResult.setStatus(OperationResultStatus.HANDLED_ERROR);
                    }

                    // Try again, this time without explicit uniqueness check
                    try {

                        LOGGER.trace("ADD {}: retrying resource operation without uniqueness check (previous dead shadow found), "
                                + "execution starting", resourceObjectToAdd);
                        doExecuteAddOperation(connOptions, true, result);

                    } catch (ObjectAlreadyExistsException innerException) {
                        // Mark shadow dead before we handle the error. ADD operation obviously failed. Therefore this particular
                        // shadow was not created as resource object. It is dead on the spot. Make sure that error handler won't confuse
                        // this shadow with the conflicting shadow that it is going to discover.
                        // This may also be a gestation quantum state collapsing to tombstone
                        shadowManager.markShadowTombstone(opState.getRepoShadow(), task, result);
                        statusFromErrorHandling = handleAddError(innerException, failedOperationResult, result);
                    } catch (Exception innerException) {
                        statusFromErrorHandling = handleAddError(innerException, result.getLastSubresult(), result);
                    }

                } else {

                    // Mark shadow dead before we handle the error. ADD operation obviously failed. Therefore this particular
                    // shadow was not created as resource object. It is dead on the spot. Make sure that error handler won't confuse
                    // this shadow with the conflicting shadow that it is going to discover.
                    // This may also be a gestation quantum state collapsing to tombstone
                    shadowManager.markShadowTombstone(opState.getRepoShadow(), task, result);
                    statusFromErrorHandling = handleAddError(e, failedOperationResult,  result);
                }

            } catch (Exception e) {
                statusFromErrorHandling = handleAddError(e, result.getLastSubresult(), result);
            }

            LOGGER.debug("ADD {}: resource operation executed, operation state: {}", resourceObjectToAdd, opState.shortDumpLazily());
        }

        private void doExecuteAddOperation(
                ConnectorOperationOptions connOptions, boolean skipExplicitUniquenessCheck, OperationResult result)
                throws ObjectNotFoundException, SchemaException, CommunicationException, ObjectAlreadyExistsException,
                ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
            AsynchronousOperationReturnValue<ShadowType> asyncResult =
                    resourceObjectConverter.addResourceObject(
                            ctx, resourceObjectToAdd, scripts, connOptions, skipExplicitUniquenessCheck, result);
            opState.recordRealAsynchronousResult(asyncResult);
        }

        private void checkAttributesPresent() throws SchemaException {
            PrismContainer<?> attributesContainer = resourceObjectToAdd.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
            if (attributesContainer == null || attributesContainer.isEmpty()) {
                throw new SchemaException("Attempt to add resource object without any attributes: " + resourceObjectToAdd);
            }
        }

        private boolean hasDeadShadowWithDeleteOperation(
                ProvisioningContext ctx, ShadowType shadowToAdd, OperationResult result)
                throws SchemaException {

            Collection<PrismObject<ShadowType>> previousDeadShadows =
                    shadowManager.searchForPreviousDeadShadows(ctx, shadowToAdd, result);
            if (previousDeadShadows.isEmpty()) {
                return false;
            }
            LOGGER.trace("Previous dead shadows:\n{}", DebugUtil.debugDumpLazily(previousDeadShadows, 1));

            XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
            for (PrismObject<ShadowType> previousDeadShadow : previousDeadShadows) {
                if (shadowCaretaker.findPendingLifecycleOperationInGracePeriod(ctx, previousDeadShadow.asObjectable(), now) == ChangeTypeType.DELETE) {
                    return true;
                }
            }
            return false;
        }

        private OperationResultStatus handleAddError(
                Exception cause, OperationResult failedOperationResult, OperationResult result)
                throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException,
                ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException,
                ExpressionEvaluationException {

            // TODO: record operationExecution

            LOGGER.debug("Handling provisioning ADD exception {}: {}", cause.getClass(), cause.getMessage());
            try {
                OperationResultStatus finalStatus = errorHandlerLocator
                        .locateErrorHandlerRequired(cause)
                        .handleAddError(ctx, resourceObjectToAdd, options, opState, cause, failedOperationResult, task, result);
                LOGGER.debug("Handled provisioning ADD exception, final status: {}, operation state: {}", finalStatus, opState.shortDumpLazily());
                return finalStatus;
            } catch (CommonException e) {
                LOGGER.debug("Handled provisioning ADD exception, final exception: {}, operation state: {}", e, opState.shortDumpLazily());
                ObjectDelta<ShadowType> delta = resourceObjectToAdd.asPrismObject().createAddDelta();
                commonHelper.handleErrorHandlerException(ctx, opState, delta, e.getMessage(), result);
                throw e;
            }
        }

        private void executeShadowConstraintsCheck(OperationResult result)
                throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
                ExpressionEvaluationException, ObjectAlreadyExistsException, SecurityViolationException {
            ShadowCheckType shadowConstraintsCheck = ctx.getShadowConstraintsCheck();
            if (shadowConstraintsCheck == ShadowCheckType.NONE) {
                return;
            }

            String shadowOid;
            if (opState.getRepoShadow() != null) {
                shadowOid = opState.getRepoShadowOid();
            } else {
                shadowOid = resourceObjectToAdd.getOid();
            }

            ConstraintsChecker checker = new ConstraintsChecker();
            checker.setCacheConfigurationManager(cacheConfigurationManager);
            checker.setShadowsFacade(shadowsFacade); // TODO ok?
            checker.setProvisioningContext(ctx);
            checker.setShadowObject(resourceObjectToAdd.asPrismObject());
            checker.setShadowOid(shadowOid);
            checker.setConstraintViolationConfirmer(shadow -> isNotDeadOrReaping(shadow, ctx));
            checker.setUseCache(false);

            ConstraintsCheckingResult checkingResult = checker.check(result);

            LOGGER.trace("Checked constraints with the result of {}, for:\n{}",
                    resourceObjectToAdd.debugDumpLazily(1), checkingResult.isSatisfiesConstraints());
            if (!checkingResult.isSatisfiesConstraints()) {
                throw new ObjectAlreadyExistsException("Conflicting shadow already exists on " + ctx.getResource());
            }
        }

        private boolean isNotDeadOrReaping(PrismObject<ShadowType> shadowObject, ProvisioningContext ctx)
                throws SchemaException, ConfigurationException {
            ShadowType shadow = shadowObject.asObjectable();
            if (ShadowUtil.isDead(shadow)) {
                return false;
            }
            ProvisioningContext shadowCtx = ctx.spawnForShadow(shadow);
            shadowCtx.updateShadowState(shadow);
            ShadowLifecycleStateType state = shadow.getShadowLifecycleState();
            return state != REAPING
                    && state != CORPSE
                    && state != TOMBSTONE;
        }

        private void notifyAfterAdd(OperationResult result) {
            // Not sure if the shadow provided is "precise enough".
            ShadowType newShadow = Objects.requireNonNullElse(opState.getCreatedShadow(), resourceObjectToAdd);
            if (newShadow.getOid() == null) {
                newShadow.setOid(opState.getRepoShadowOid());
            }
            ObjectDelta<ShadowType> delta = createAddDelta(newShadow.asPrismObject());
            notifyAboutSuccessOperation(ctx, newShadow, opState, delta, result);
        }
    }
}
