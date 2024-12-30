/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsUtil.getAdditionalOperationDesc;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType.*;

import java.util.Objects;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.api.ConstraintsCheckingResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.ShadowLifecycleStateDeterminer;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectShadow;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowCheckType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * Represents/executes "add" operation on a shadow - either invoked directly, or during refresh or propagation.
 * See the variants of the `execute` method.
 */
public class ShadowAddOperation extends ShadowProvisioningOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowAddOperation.class);

    /** Object to be added: provided by the caller, or from a pending operation. */
    @NotNull private final ResourceObjectShadow objectToAdd;

    /**
     * Object that was reported by the "resource objects" layer as the created one or the one submitted for creation.
     *
     * Present only if the operation was really attempted, i.e., it was not delayed for grouping.
     */
    private ResourceObjectShadow createdObject;

    private ShadowAddOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectShadow objectToAdd,
            OperationProvisioningScriptsType scripts,
            @NotNull ProvisioningOperationState opState,
            ProvisioningOperationOptions options) {
        super(ctx, opState, scripts, options, objectToAdd.getPrismObject().createAddDelta());
        this.objectToAdd = objectToAdd;
    }

    static String executeDirectly(
            @NotNull ShadowType beanToAdd,
            @Nullable OperationProvisioningScriptsType scripts,
            @Nullable ProvisioningOperationOptions options,
            @Nullable ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {

        InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT); // TODO is it OK here?

        LOGGER.trace("Start adding shadow object{}:\n{}",
                lazy(() -> getAdditionalOperationDesc(scripts, options)),
                beanToAdd.debugDumpLazily(1));

        var ctx = establishProvisioningContext(beanToAdd, context, task, result);
        ctx.checkExecutionFullyPersistent();
        var shadowToAdd = ctx.adoptNotYetExistingResourceObject(beanToAdd);
        var opState = new ProvisioningOperationState();
        return new ShadowAddOperation(ctx, shadowToAdd, scripts, opState, options)
                .execute(result);
    }

    static private @NotNull ProvisioningContext establishProvisioningContext(
            ShadowType resourceObjectToAdd, ProvisioningOperationContext context, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException,
            CommunicationException {
        var resource = b().ctxFactory.getResource(resourceObjectToAdd, task, result);
        var ctx = b().ctxFactory.createForShadow(resourceObjectToAdd, resource, task);
        ctx.assertDefinition();
        ctx.setOperationContext(context);
        return ctx;
    }

    static RepoShadow executeAsRetryInRefresh(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull PendingOperation pendingOperation,
            @Nullable ProvisioningOperationOptions options,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {
        var beanToAdd = pendingOperation.getDelta().getObjectToAdd().asObjectable();
        var shadowToAdd = ctx.adoptNotYetExistingResourceObject(beanToAdd);
        var opState = ProvisioningOperationState.fromPendingOperation(repoShadow, pendingOperation);
        new ShadowAddOperation(ctx, shadowToAdd, null, opState, options)
                .execute(result);
        return opState.getRepoShadow();
    }

    static void executeInPropagation(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull ShadowType beanToAdd, // May include additional modifications (as this is the propagation = grouping ops)
            @NotNull PendingOperations sortedOperations,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {
        var shadowToAdd = ctx.adoptNotYetExistingResourceObject(beanToAdd);
        var opState = ProvisioningOperationState.fromPropagatedPendingOperations(repoShadow, sortedOperations);
        new ShadowAddOperation(ctx, shadowToAdd, null, opState, null)
                .execute(result);
    }

    private String execute(OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {

        objectToAdd.checkConsistence();

        checkAttributesOrAssociationsPresent();
        ctx.validateSchemaIfConfigured(objectToAdd.getBean());

        // Changes made here will go to the pending operation, should the operation fail or be delayed.
        // TODO why do we need this here? It should be sufficient when we are going to execute the operation below
        associationsHelper.provideObjectsIdentifiersToSubject(ctx, objectToAdd, result);

        determineEffectiveMarksAndPolicies(objectToAdd, result);
        accessChecker.checkAttributesAddAccess(ctx, objectToAdd, result);

        executeShadowConstraintsCheck(result); // To avoid shadow duplication (if configured so)
        shadowCreator.addNewProposedShadow(ctx, objectToAdd, opState, result); // If configured & if not existing yet

        if (ctx.shouldExecuteResourceOperationDirectly()) {
            associationsHelper.provideObjectsIdentifiersToSubject(ctx, objectToAdd, result);
            associationsHelper.convertAssociationsToReferenceAttributes(objectToAdd);
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
    }

    private void executeAddOperationDirectly(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, GenericFrameworkException, ObjectAlreadyExistsException,
            SecurityViolationException, PolicyViolationException {

        LOGGER.trace("ADD {}: resource operation, execution starting", objectToAdd);

        var connOptions = createConnectorOperationOptions(result);

        try {

            ctx.checkNotInMaintenance();
            doExecuteAddOperation(connOptions, false, result);

        } catch (ObjectAlreadyExistsException e) {

            LOGGER.trace("Object already exists error when trying to add {}, exploring the situation",
                    objectToAdd.shortDumpLazily());

            // This exception may still be OK in some cases. Such as:
            // We are trying to add a shadow to a semi-manual connector.
            // But the resource object was recently deleted. The object is
            // still in the backing store (CSV file) because of a grace
            // period. Obviously, attempt to add such object would fail.
            // So, we need to handle this case specially. (MID-4414)

            OperationResult failedOperationResult = result.getLastSubresult(); // This may or may not be correct

            if (hasDeadShadowWithDeleteOperation(result)) {

                if (failedOperationResult.isError()) {
                    failedOperationResult.setStatus(OperationResultStatus.HANDLED_ERROR);
                }

                // Try again, this time without explicit uniqueness check
                try {

                    LOGGER.trace("ADD {}: retrying resource operation without uniqueness check (previous dead shadow found)",
                            objectToAdd);
                    doExecuteAddOperation(connOptions, true, result);

                } catch (ObjectAlreadyExistsException innerException) {
                    markShadowTombstone(result);
                    statusFromErrorHandling = handleAddError(innerException, failedOperationResult, result);
                } catch (Exception innerException) {
                    statusFromErrorHandling = handleAddError(innerException, result.getLastSubresult(), result);
                }

            } else {
                markShadowTombstone(result);
                statusFromErrorHandling = handleAddError(e, failedOperationResult, result);
            }

        } catch (Exception e) {
            statusFromErrorHandling = handleAddError(e, result.getLastSubresult(), result);
        }

        LOGGER.debug("ADD {}: resource operation executed, operation state: {}", objectToAdd, opState.shortDumpLazily());
    }

    private void markShadowTombstone(OperationResult result) throws SchemaException {
        // Mark shadow dead before we handle the error. ADD operation obviously failed. Therefore this particular
        // shadow was not created as resource object. It is dead on the spot. Make sure that error handler won't
        // confuse this shadow with the conflicting shadow that it is going to discover.
        // This may also be a gestation quantum state collapsing to tombstone
        shadowUpdater.markShadowTombstone(opState.getRepoShadow(), task, result);
    }

    private void doExecuteAddOperation(
            ConnectorOperationOptions connOptions, boolean skipExplicitUniquenessCheck, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        var addOpResult =
                resourceObjectConverter.addResourceObject(
                        ctx, objectToAdd, scripts, connOptions, skipExplicitUniquenessCheck, result);
        setOperationStatus(addOpResult);

        createdObject = addOpResult.getCreatedObject();
        setExecutedDelta(
                createdObject.getPrismObject().createAddDelta());
    }

    private void checkAttributesOrAssociationsPresent() throws SchemaException {
        if (objectToAdd.getAttributes().isEmpty() && objectToAdd.getAssociations().isEmpty()) {
            throw new SchemaException("Attempt to add resource object without any attributes nor associations: " + objectToAdd);
        }
    }

    private boolean hasDeadShadowWithDeleteOperation(OperationResult result)
            throws SchemaException {

        var previousDeadShadows = shadowFinder.searchForPreviousDeadShadows(ctx, objectToAdd, result);
        if (previousDeadShadows.isEmpty()) {
            return false;
        }
        LOGGER.trace("Previous dead shadows:\n{}", DebugUtil.debugDumpLazily(previousDeadShadows, 1));

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        return previousDeadShadows.stream()
                .anyMatch(deadShadow ->
                        ShadowLifecycleStateDeterminer.findPendingLifecycleOperationInGracePeriod(ctx, deadShadow.asObjectable(), now) == ChangeTypeType.DELETE);
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
            shadowOid = objectToAdd.getOid();
        }

        ConstraintsChecker checker = new ConstraintsChecker();
        checker.setProvisioningContext(ctx);
        checker.setShadowObject(objectToAdd.getPrismObject());
        checker.setShadowOid(shadowOid);
        checker.setConstraintViolationConfirmer(shadow -> isNotDeadOrReaping(shadow, ctx));
        checker.setDoNotUseCache();

        ConstraintsCheckingResult checkingResult = checker.check(result);

        LOGGER.trace("Checked constraints with the result of {}, for:\n{}",
                objectToAdd.debugDumpLazily(1), checkingResult.isSatisfiesConstraints());
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

    public @NotNull ResourceObjectShadow getResourceObjectToAdd() {
        return objectToAdd;
    }

    public @NotNull ResourceObjectShadow getShadowAddedOrToAdd() {
        if (opState.wasStarted() && createdObject != null) {
            return createdObject;
        } else {
            return objectToAdd;
        }
    }

    private void setOid() {
        String oid = Objects.requireNonNull(opState.getRepoShadowOid(), "No shadow OID after ADD operation");

        // Setting OID everywhere is a bit brutal. But we want to make sure all returned objects/deltas will have it.
        // TODO is this really correct? We don't need/want it in resource objects, only in the shadows.
        objectToAdd.setOid(oid);
        requestedDelta.setOid(oid);
        if (createdObject != null) {
            createdObject.setOid(oid);
        }
        if (executedDelta != null) {
            executedDelta.setOid(oid);
        }
    }

    private static ShadowsLocalBeans b() {
        return ShadowsLocalBeans.get();
    }
}
