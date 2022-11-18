/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ConstraintsCheckingResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.AddOperationState;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsUtil.*;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType.*;

/**
 * Represents/executes "add" operation on a shadow - either invoked directly, or during refresh or propagation.
 * See the variants of the `execute` method.
 */
public class ShadowAddOperation extends ShadowProvisioningOperation<AddOperationState> {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowAddOperation.class);

    @NotNull private final ShadowType resourceObjectToAdd;

    private ShadowAddOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType resourceObjectToAdd,
            OperationProvisioningScriptsType scripts,
            @NotNull AddOperationState opState,
            ProvisioningOperationOptions options) {
        super(ctx, opState, scripts, options, resourceObjectToAdd.asPrismObject().createAddDelta());
        this.resourceObjectToAdd = resourceObjectToAdd;
    }

    /** This is called when explicit "add object" operation is invoked on the shadow facade. */
    static String executeDirectly(
            @NotNull ShadowType resourceObjectToAdd,
            OperationProvisioningScriptsType scripts,
            ProvisioningOperationOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {

        InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT); // TODO is it OK here?

        LOGGER.trace("Start adding shadow object{}:\n{}",
                lazy(() -> getAdditionalOperationDesc(scripts, options)),
                resourceObjectToAdd.debugDumpLazily(1));

        ProvisioningContext ctx = establishProvisioningContext(resourceObjectToAdd, task, result);
        AddOperationState opState = new AddOperationState();
        return new ShadowAddOperation(ctx, resourceObjectToAdd, scripts, opState, options)
                .execute(result);
    }

    static private @NotNull ProvisioningContext establishProvisioningContext(
            @NotNull ShadowType resourceObjectToAdd, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException,
            CommunicationException {
        ProvisioningContextFactory ctxFactory = ShadowsLocalBeans.get().ctxFactory;
        ResourceType resource = ctxFactory.getResource(resourceObjectToAdd, task, result);
        try {
            ProvisioningContext ctx = ctxFactory.createForShadow(resourceObjectToAdd, resource, task);
            ctx.assertDefinition();
            return ctx;
        } catch (SchemaException e) { // TODO why only for this kind of exception?
            ShadowsLocalBeans.get().eventDispatcher.notifyFailure(
                    createResourceFailureDescription(
                            resourceObjectToAdd, resource, resourceObjectToAdd.asPrismObject().createAddDelta(), e.getMessage()),
                    task,
                    result);
            throw e;
        }
    }

    static AddOperationState executeInRefresh(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType repoShadow,
            @NotNull ObjectDelta<ShadowType> pendingDelta,
            @NotNull PendingOperationType pendingOperation,
            ProvisioningOperationOptions options,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {
        AddOperationState opState = AddOperationState.fromPendingOperation(repoShadow, pendingOperation);
        ShadowType resourceObjectToAdd = pendingDelta.getObjectToAdd().asObjectable();
        new ShadowAddOperation(ctx, resourceObjectToAdd, null, opState, options)
                .execute(result);
        return opState;
    }

    static void executeInPropagation(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType repoShadow,
            @NotNull ShadowType resourceObjectToAdd, // May include e.g. additional modifications
            @NotNull List<PendingOperationType> sortedOperations,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {
        resourceObjectToAdd.setOid(repoShadow.getOid());
        AddOperationState opState = new AddOperationState(repoShadow);
        opState.setPropagatedPendingOperations(sortedOperations);
        new ShadowAddOperation(ctx, resourceObjectToAdd, null, opState, null)
                .execute(result);
    }

    private String execute(OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {

        try {
            checkAttributesPresent();
            ctx.applyAttributesDefinition(resourceObjectToAdd);
            ctx.validateSchemaIfConfigured(resourceObjectToAdd);

            accessChecker.checkAddAccess(ctx, resourceObjectToAdd, result);

            setProductionFlag();

            executeShadowConstraintsCheck(result); // To avoid shadow duplication (if configured so)
            shadowCreator.addNewProposedShadow(ctx, resourceObjectToAdd, opState, result); // If configured & if not existing yet

            // Changes made here will go to the pending operation, should the operation fail.
            entitlementsHelper.provideEntitlementsIdentifiers(ctx, resourceObjectToAdd, result);

            if (ctx.shouldExecuteResourceOperationDirectly()) {
                executeAddOperationDirectly(result);
            } else {
                markOperationExecutionAsPending(result);
            }

            // This is where the repo shadow is created or updated (if needed)
            resultRecorder.recordAddResult(this, result);
            setOid();
            sendSuccessOrInProgressNotification(opState.getRepoShadow(), result);
            setParentOperationStatus(result); // FIXME

            return opState.getRepoShadowOid();

        } catch (SchemaException e) {
            // TODO why only for this kind of exception?
            // TODO why a similar code is not present for modify and delete operations?
            ShadowsLocalBeans.get().eventDispatcher.notifyFailure(
                    createResourceFailureDescription(
                            resourceObjectToAdd,
                            ctx.getResource(),
                            resourceObjectToAdd.asPrismObject().createAddDelta(),
                            e.getMessage()),
                    ctx.getTask(), result);
            throw e;
        }
    }

    private void setProductionFlag() {
        resourceObjectToAdd.setSimulated(
                !ctx.isObjectDefinitionInProduction());
    }

    private void executeAddOperationDirectly(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, GenericFrameworkException, ObjectAlreadyExistsException,
            SecurityViolationException, PolicyViolationException {
        ConnectorOperationOptions connOptions = createConnectorOperationOptions(result);

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
                    shadowUpdater.markShadowTombstone(opState.getRepoShadow(), task, result);
                    statusFromErrorHandling = handleAddError(innerException, failedOperationResult, result);
                } catch (Exception innerException) {
                    statusFromErrorHandling = handleAddError(innerException, result.getLastSubresult(), result);
                }

            } else {

                // Mark shadow dead before we handle the error. ADD operation obviously failed. Therefore this particular
                // shadow was not created as resource object. It is dead on the spot. Make sure that error handler won't confuse
                // this shadow with the conflicting shadow that it is going to discover.
                // This may also be a gestation quantum state collapsing to tombstone
                shadowUpdater.markShadowTombstone(opState.getRepoShadow(), task, result);
                statusFromErrorHandling = handleAddError(e, failedOperationResult, result);
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

        ShadowType createdShadow = asyncResult.getReturnValue();
        if (createdShadow != null) {
            setExecutedDelta(
                    createdShadow.asPrismObject().createAddDelta());
        }
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
                shadowFinder.searchForPreviousDeadShadows(ctx, shadowToAdd, result);
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
                    .handleAddError(this, cause, failedOperationResult, result);
            LOGGER.debug("Handled provisioning ADD exception, final status: {}, operation state: {}", finalStatus, opState.shortDumpLazily());
            return finalStatus;
        } catch (CommonException e) {
            LOGGER.debug("Handled provisioning ADD exception, final exception: {}, operation state: {}", e, opState.shortDumpLazily());
            handleErrorHandlerException(e.getMessage(), result);
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
        checker.setProvisioningContext(ctx);
        checker.setShadowObject(resourceObjectToAdd.asPrismObject());
        checker.setShadowOid(shadowOid);
        checker.setConstraintViolationConfirmer(shadow -> isNotDeadOrReaping(shadow, ctx));
        checker.setDoNotUseCache();

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

    @Override
    public String getGerund() {
        return "adding";
    }

    @Override
    public String getLogVerb() {
        return "ADD";
    }

    @Override
    Trace getLogger() {
        return LOGGER;
    }

    public @NotNull ShadowType getResourceObjectToAdd() {
        return resourceObjectToAdd;
    }

    public @NotNull ShadowType getResourceObjectAddedOrToAdd() {
        ShadowType createdShadow = opState.getCreatedShadow();
        if (opState.wasStarted() && createdShadow != null) {
            return createdShadow;
        } else {
            return resourceObjectToAdd;
        }
    }

    private void setOid() {
        String oid = Objects.requireNonNull(opState.getRepoShadowOid(), "No shadow OID after ADD operation");

        // Setting OID everywhere is a bit brutal. But we want to make sure all returned objects/deltas will have it.
        resourceObjectToAdd.setOid(oid);
        requestedDelta.setOid(oid);
        if (opState.getCreatedShadow() != null) {
            opState.getCreatedShadow().setOid(oid);
        }
        if (executedDelta != null) {
            executedDelta.setOid(oid);
        }
    }
}
