/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsUtil.getAdditionalOperationDesc;
import static com.evolveum.midpoint.schema.util.ShadowUtil.getNonResourceModifications;
import static com.evolveum.midpoint.schema.util.ShadowUtil.getResourceModifications;
import static com.evolveum.midpoint.util.exception.SeverityAwareException.Severity.PARTIAL_ERROR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;

import java.util.ArrayList;
import java.util.Collection;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Represents/executes `modify` operation on a shadow - either invoked directly, or during refresh or propagation.
 * See:
 *
 * - {@link #executeDirectly(RawRepoShadow, Collection, OperationProvisioningScriptsType, ProvisioningOperationOptions, ProvisioningOperationContext, Task, OperationResult)}
 * - {@link #executeInRefresh(ProvisioningContext, RepoShadow, PendingOperation, ProvisioningOperationOptions, OperationResult)}
 * - {@link #executeInPropagation(ProvisioningContext, RepoShadow, Collection, PendingOperations, OperationResult)}
 */
public class ShadowModifyOperation extends ShadowProvisioningOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowModifyOperation.class);

    /** Modifications whose execution was (originally) requested. */
    @NotNull private final ImmutableList<? extends ItemDelta<?, ?>> requestedModifications;

    /** Modifications from {@link #requestedModifications} that are NOT to be executed on the resource. */
    @NotNull private final ImmutableList<? extends ItemDelta<?, ?>> requestedNonResourceModifications;

    /** The same as modifications in {@link ShadowProvisioningOperation#resourceDelta}. Just for code clarity. */
    @NotNull private final Collection<? extends ItemDelta<?, ?>> resourceDeltaModifications;

    /** Requested modifications, later updated with known ones obtained from the {@link ResourceObjectConverter}. High-level. */
    @NotNull private final Collection<? extends ItemDelta<?, ?>> effectiveModifications;

    /** Requested low-level modifications, later updated with known ones obtained from the {@link ResourceObjectConverter}. */
    private Collection<? extends ItemDelta<?, ?>> effectiveResourceLevelModifications;

    private final boolean inRefreshOrPropagation;

    private final XMLGregorianCalendar now;

    /** The "refresh-before-modify" operation (if executed). */
    private ShadowRefreshOperation shadowRefreshOperation;

    /** modifications must have appropriate definitions */
    private ShadowModifyOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts,
            @NotNull ProvisioningOperationState opState,
            boolean inRefreshOrPropagation) {
        super(ctx, opState, scripts, options,
                createModificationDelta(opState, modifications),
                createModificationDelta(opState, getResourceModifications(modifications)));
        this.requestedModifications = ImmutableList.copyOf(modifications);
        this.requestedNonResourceModifications = getNonResourceModifications(modifications);
        this.resourceDeltaModifications = resourceDelta.getModifications();
        // TODO To be discussed: should we append the executed modifications to the original list of requested modifications?
        //  This has an interesting side effect: when auditing, midPoint stores not only computed (requested) modifications,
        //  but also the ones induced by the connector/resource. For example (TestModelServiceContract.test212) we audit
        //  not only the change of attributes/name (morgan->sirhenry) but also the induced change of attributes/uid. On the
        //  other hand, it is nice and convenient, on the other, this behavior is undocumented and "hacky".
        this.effectiveModifications = modifications;
        this.inRefreshOrPropagation = inRefreshOrPropagation;
        this.now = clock.currentTimeXMLGregorianCalendar();
    }

    private static ObjectDelta<ShadowType> createModificationDelta(
            ProvisioningOperationState opState, Collection<? extends ItemDelta<?, ?>> modifications) {
        ObjectDelta<ShadowType> delta = opState.getRepoShadowRequired().getPrismObject().createModifyDelta();
        delta.addModifications(
                ItemDeltaCollectionsUtil.cloneCollection(modifications));
        return delta;
    }

    /** Executes the modify operation requested explicitly from the client. */
    public static String executeDirectly(
            @NotNull RawRepoShadow rawRepoShadow,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @Nullable OperationProvisioningScriptsType scripts,
            @Nullable ProvisioningOperationOptions options,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException,
            ObjectAlreadyExistsException {

        Validate.notNull(rawRepoShadow, "Object to modify must not be null.");
        Validate.notNull(modifications, "Object modification must not be null.");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Start modifying {}{}:\n{}", rawRepoShadow, getAdditionalOperationDesc(scripts, options),
                    DebugUtil.debugDump(modifications, 1));
        }

        InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT);

        ProvisioningContext ctx = ShadowsLocalBeans.get().ctxFactory.createForShadow(
                rawRepoShadow.getBean(),
                getAdditionalAuxObjectClassNames(modifications),
                task,
                result);
        ctx.setOperationContext(context);
        ctx.assertDefinition();
        ctx.checkExecutionFullyPersistent();
        var repoShadow = ctx.adoptRawRepoShadow(rawRepoShadow);

        ProvisioningOperationState opState = new ProvisioningOperationState(repoShadow);

        options = setForceRetryIfNotDisabled(options);

        ctx.applyCurrentDefinition(modifications);

        // We have to resolve entitlements here. For example, we want them to be stored in the pending operation list,
        // should the execution fail. (Because the shadow OIDs may be volatile.) Before 4.7, the inclusion to the pending
        // list was automatic, because the list of modifications was shared throughout the operation. However, now it is
        // no longer the case: we have separate "requested delta", "resource delta", and "executed delta" there.
        ShadowsLocalBeans.get().associationsHelper.provideObjectsIdentifiersToDelta(
                ctx, modifications, "delta for shadow " + repoShadow.getOid(), result);

        return new ShadowModifyOperation(ctx, modifications, options, scripts, opState, false)
                .execute(result);
    }

    /**
     * If not explicitly disabled, we want to force retry operations during modify.
     * It is quite cheap and probably safer than not doing it.
     */
    private static ProvisioningOperationOptions setForceRetryIfNotDisabled(ProvisioningOperationOptions options) {
        if (options == null) {
            return ProvisioningOperationOptions.createForceRetry(Boolean.TRUE);
        }
        if (options.getForceRetry() == null) {
            options.setForceRetry(Boolean.TRUE);
        }
        return options;
    }

    @NotNull
    private static Collection<QName> getAdditionalAuxObjectClassNames(
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications) {
        Collection<QName> additionalAuxiliaryObjectClassQNames = new ArrayList<>();
        for (ItemDelta<?, ?> modification : modifications) {
            if (ShadowType.F_AUXILIARY_OBJECT_CLASS.equivalent(modification.getPath())) {
                //noinspection unchecked
                PropertyDelta<QName> auxDelta = (PropertyDelta<QName>) modification;
                for (PrismPropertyValue<QName> pVal : auxDelta.getValues(QName.class)) {
                    additionalAuxiliaryObjectClassQNames.add(pVal.getValue());
                }
            }
        }
        return additionalAuxiliaryObjectClassQNames;
    }

    static ProvisioningOperationState executeInRefresh(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull PendingOperation pendingOperation,
            @Nullable ProvisioningOperationOptions options,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException,
            ObjectAlreadyExistsException {
        assert pendingOperation.isModify();
        var opState = ProvisioningOperationState.fromPendingOperation(repoShadow, pendingOperation);
        if (repoShadow.doesExist()) {
            var modifications = pendingOperation.getDelta().getModifications();
            ctx.applyCurrentDefinition(modifications);
            new ShadowModifyOperation(ctx, modifications, options, null, opState, true)
                .execute(result);
        } else {
            result.recordFatalError("Object does not exist on the resource yet, modification attempt was skipped");
        }
        return opState;
    }

    static void executeInPropagation(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @NotNull PendingOperations sortedOperations,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, GenericFrameworkException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException,
            PolicyViolationException, ObjectAlreadyExistsException {
        var opState = ProvisioningOperationState.fromPropagatedPendingOperations(repoShadow, sortedOperations);
        ctx.applyCurrentDefinition(modifications);
        new ShadowModifyOperation(ctx, modifications, null, null, opState, true)
                .execute(result);
    }

    private String execute(OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException,
            ObjectAlreadyExistsException {

        if (!inRefreshOrPropagation && checkAndRecordPendingOperationBeforeExecution(result)) {
            return opState.getRepoShadowOid();
        }

        RepoShadow repoShadow = opState.getRepoShadowRequired(); // Shadow in opState was updated in the above call!
        ctx.applyDefinitionInNewCtx(repoShadow);

        determineEffectiveMarksAndPolicies(repoShadow, result);
        accessChecker.checkAttributesModifyAccess(ctx, requestedModifications, result);

        if (resourceDelta.isEmpty()) {
            opState.setExecutionStatus(COMPLETED);
            LOGGER.trace("MODIFY {}: repository-only modification", repoShadow);
        } else {
            if (ctx.shouldExecuteResourceOperationDirectly()) {
                LOGGER.trace("MODIFY resource object represented by {}: resource modification, execution starting\n{}",
                        repoShadow, DebugUtil.debugDumpLazily(requestedModifications));

                refreshBeforeExecution(result); // Will be skipped in maintenance mode
                if (wasRefreshOperationSuccessful()) {
                    // FIXME entitlement identifiers should be already there; but it looks like pending operations currently
                    //  do not store embedded shadows correctly, so let's re-resolve them now. This should be fixed!
                    associationsHelper.provideObjectsIdentifiersToDelta(ctx, resourceDeltaModifications, "", result);
                    associationsHelper.convertAssociationDeltasToReferenceAttributeDeltas(resourceDeltaModifications);
                    executeModifyOperationDirectly(result);
                } else {
                    opState.markAsPostponed(
                            shadowRefreshOperation.getRetriedOperationsResultStatus());
                }
            } else {
                markOperationExecutionAsPending(result);
            }
        }

        resultRecorder.recordModifyResult(this, result);
        sendSuccessOrInProgressNotification(opState.getRepoShadowRequired(), result);
        setParentOperationStatus(result); // FIXME

        return repoShadow.getOid();
    }

    private void refreshBeforeExecution(OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException {
        RepoShadow repoShadow = opState.getRepoShadowRequired();
        if (inRefreshOrPropagation || !repoShadow.getPendingOperations().hasRetryableOperation()) {
            return;
        }
        LOGGER.trace("Refreshing shadow before executing the modification operation");
        shadowRefreshOperation =
                ShadowRefreshOperation.executeFull(repoShadow, options, ctx.getOperationContext(), ctx.getTask(), result);
        var shadowAfterRefresh = shadowRefreshOperation.getShadow();
        if (shadowAfterRefresh.isDeleted()) {
            LOGGER.trace("Shadow is gone. Nothing more to do");
            throw new ObjectNotFoundException(
                    "Shadow disappeared during modify", null, ShadowType.class, repoShadow.getOid(), PARTIAL_ERROR);
        } else {
            opState.setRepoShadow(shadowAfterRefresh);
        }
    }

    private boolean wasRefreshOperationSuccessful() {
        if (shadowRefreshOperation == null) {
            LOGGER.trace("Nothing refreshed, modify can continue.");
            return true;
        }

        if (shadowRefreshOperation.getRetriedOperations().isEmpty()) {
            LOGGER.trace("No retried operations during refresh. Continue with modify operation.");
            return true;
        }

        if (shadowRefreshOperation.getShadow().isDeleted()) {
            LOGGER.trace("Shadow is gone. Probably it was deleted during refresh. Finishing modify operation now.");
            return false;
        }

        for (ObjectDeltaOperation<ShadowType> retriedOdo : shadowRefreshOperation.getRetriedOperations()) {
            OperationResult result = retriedOdo.getExecutionResult();
            if (result == null || !result.isSuccess()) {
                LOGGER.trace("Refresh operation not successful. Current modify operation will be postponed.");
                return false;
            }
        }

        return true;
    }

    private void executeModifyOperationDirectly(OperationResult result)
            throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException {
        RepoShadow repoShadow = opState.getRepoShadowRequired();
        try {
            ctx.checkNotInMaintenance();

            assert resourceDeltaModifications == resourceDelta.getModifications();

            var connOptions = createConnectorOperationOptions(result);
            var modifyOpResult = resourceObjectConverter.modifyResourceObject(
                    ctx, repoShadow, scripts, connOptions, resourceDeltaModifications, now, result);

            setOperationStatus(modifyOpResult);

            // TODO should we mark the resource as UP here, as we do for ADD and DELETE?

            effectiveResourceLevelModifications = new ArrayList<>(resourceDeltaModifications);
            ItemDeltaCollectionsUtil.mergeAll(
                    effectiveResourceLevelModifications, modifyOpResult.getExecutedDeltas());

            ItemDeltaCollectionsUtil.mergeAll(
                    effectiveModifications, modifyOpResult.getExecutedDeltas());
            setExecutedDelta(
                    createModificationDelta(opState, effectiveModifications));

        } catch (Exception ex) {
            LOGGER.debug("Provisioning exception: {}:{}, attempting to handle it", ex.getClass(), ex.getMessage(), ex);
            statusFromErrorHandling = handleModifyError(ex, result.getLastSubresult(), result);
        }

        LOGGER.debug("MODIFY resource object {}: resource operation executed, operation state: {}",
                repoShadow, opState.shortDumpLazily());
    }

    private OperationResultStatus handleModifyError(
            @NotNull Exception cause, OperationResult failedOperationResult, @NotNull OperationResult result)
            throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException {

        LOGGER.debug("Handling provisioning MODIFY exception {}: {}", cause.getClass(), cause.getMessage());
        try {

            OperationResultStatus finalStatus = errorHandlerLocator
                    .locateErrorHandlerRequired(cause)
                    .handleModifyError(this, cause, failedOperationResult, result);
            LOGGER.debug("Handled provisioning MODIFY exception, final status: {}, operation state: {}",
                    finalStatus, opState.shortDumpLazily());
            return finalStatus;

        } catch (CommonException e) {
            LOGGER.debug("Handled provisioning MODIFY exception, final exception: {}, operation state: {}",
                    e, opState.shortDumpLazily());
            handleErrorHandlerException(e.getMessage(), result);
            throw e;
        }
    }

    @Override
    public String getGerund() {
        return "modifying";
    }

    @Override
    public String getLogVerb() {
        return "MODIFY";
    }

    @Override
    Trace getLogger() {
        return LOGGER;
    }

    public @NotNull ImmutableList<? extends ItemDelta<?, ?>> getRequestedModifications() {
        return requestedModifications;
    }

    public @NotNull Collection<? extends ItemDelta<?, ?>> getEffectiveModifications() {
        return effectiveModifications;
    }

    public Collection<? extends ItemDelta<?, ?>> getEffectiveResourceLevelModifications() {
        return effectiveResourceLevelModifications;
    }

    public @NotNull ImmutableList<? extends ItemDelta<?, ?>> getRequestedNonResourceModifications() {
        return requestedNonResourceModifications;
    }
}
