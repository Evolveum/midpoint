/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsUtil.*;
import static com.evolveum.midpoint.util.exception.CommonException.Severity.PARTIAL_ERROR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;

import java.util.ArrayList;
import java.util.Collection;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;

import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.ModifyOperationState;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.*;
import com.evolveum.midpoint.provisioning.impl.shadows.errors.ErrorHandlerLocator;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.RefreshShadowOperation;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Helps with the `modify` operation.
 */
@Component
@Experimental
class ShadowModifyHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowModifyHelper.class);

    @Autowired private ErrorHandlerLocator errorHandlerLocator;
    @Autowired private Clock clock;
    @Autowired private PrismContext prismContext;
    @Autowired private ResourceObjectConverter resourceObjectConverter;
    @Autowired protected ShadowManager shadowManager;
    @Autowired private AccessChecker accessChecker;
    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private EntitlementsHelper entitlementsHelper;
    @Autowired private CommonHelper commonHelper;
    @Autowired private ShadowRefreshHelper refreshHelper;

    String modifyShadow(
            @NotNull ShadowType repoShadow,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @Nullable OperationProvisioningScriptsType scripts,
            @Nullable ProvisioningOperationOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException,
            EncryptionException, ObjectAlreadyExistsException {

        Validate.notNull(repoShadow, "Object to modify must not be null.");
        Validate.notNull(modifications, "Object modification must not be null.");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Start modifying {}{}:\n{}", repoShadow, getAdditionalOperationDesc(scripts, options),
                    DebugUtil.debugDump(modifications, 1));
        }

        InternalMonitor.recordCount(InternalCounters.SHADOW_CHANGE_OPERATION_COUNT);

        ProvisioningContext ctx = ctxFactory.createForShadow(
                repoShadow,
                getAdditionalAuxObjectClassNames(modifications),
                task,
                result);
        ctx.assertDefinition();

        ModifyOperationState opState = new ModifyOperationState(repoShadow);

        options = setForceRetryIfNotDisabled(options);

        return executeModifyAttempt(ctx, modifications, options, scripts, opState, false, result);
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

    /**
     * This is called also from refresh or propagation operations.
     *
     * @param inRefreshOrPropagation True if we are in refresh or propagation operation
     */
    String executeModifyAttempt(
            @NotNull ProvisioningContext ctx,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @Nullable ProvisioningOperationOptions options,
            @Nullable OperationProvisioningScriptsType scripts,
            @NotNull ModifyOperationState opState,
            boolean inRefreshOrPropagation,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException,
            EncryptionException, ObjectAlreadyExistsException {
        return new ModifyOperation(ctx, modifications, options, scripts, opState, inRefreshOrPropagation)
                .execute(result);
    }

    class ModifyOperation {

        @NotNull private final ProvisioningContext ctx;

        /** Note that the modifications are update with known ones obtained from the {@link ResourceObjectConverter}. */
        @NotNull private final Collection<? extends ItemDelta<?, ?>> modifications;

        private final ProvisioningOperationOptions options;
        private final OperationProvisioningScriptsType scripts;
        @NotNull private final ModifyOperationState opState;
        private final boolean inRefreshOrPropagation;
        private final XMLGregorianCalendar now;
        OperationResultStatus statusFromErrorHandling;

        /** Result of "refresh-before-modify" operation (if executed). */
        RefreshShadowOperation refreshShadowOperation;

        ModifyOperation(
                @NotNull ProvisioningContext ctx,
                @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
                ProvisioningOperationOptions options,
                OperationProvisioningScriptsType scripts,
                @NotNull ModifyOperationState opState,
                boolean inRefreshOrPropagation) {
            this.ctx = ctx;
            this.modifications = modifications;
            this.options = options;
            this.scripts = scripts;
            this.opState = opState;
            this.inRefreshOrPropagation = inRefreshOrPropagation;
            this.now = clock.currentTimeXMLGregorianCalendar();
        }

        String execute(OperationResult result)
                throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
                ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException,
                EncryptionException, ObjectAlreadyExistsException {

            if (!inRefreshOrPropagation) {
                PendingOperationType duplicateOperation =
                        shadowManager.checkAndRecordPendingModifyOperationBeforeExecution(ctx, modifications, opState, result);
                if (duplicateOperation != null) {
                    result.setInProgress(); // suspicious
                    return opState.getRepoShadowOid();
                }
            } else {
                LOGGER.trace("Not checking for duplicate pending operation, as we are already doing the refresh/propagation");
            }

            ShadowType repoShadow = opState.getRepoShadowRequired(); // Shadow in opState was updated in the above call!
            ctx.applyAttributesDefinition(repoShadow);

            accessChecker.checkModifyAccess(ctx, modifications, result);

            entitlementsHelper.provideEntitlementsIdentifiers(
                    ctx, modifications, "delta for shadow " + repoShadow.getOid(), result);

            if (shadowManager.containsNoResourceModification(modifications)) {
                opState.setExecutionStatus(COMPLETED);
                LOGGER.trace("MODIFY {}: repository-only modification", repoShadow);
            } else {
                if (ctx.shouldExecuteResourceOperationDirectly()) {
                    LOGGER.trace("MODIFY {}: resource modification, execution starting\n{}",
                            repoShadow, DebugUtil.debugDumpLazily(modifications));

                    refreshBeforeExecution(result); // Will be skipped in maintenance mode
                    if (wasRefreshOperationSuccessful()) {
                        executeModifyOperationDirectly(result);
                    } else {
                        opState.markAsPostponed(refreshShadowOperation.getRefreshResult());
                    }

                } else {
                    markOperationExecutionAsPending(LOGGER, "MODIFY", opState, result);
                }
            }

            shadowManager.recordModifyResult(ctx, repoShadow, modifications, opState, result);
            notifyAfterModify(result);
            setParentOperationStatus(result, opState, statusFromErrorHandling); // FIXME

            return repoShadow.getOid();
        }

        private void refreshBeforeExecution(OperationResult result)
                throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
                ExpressionEvaluationException, EncryptionException {
            ShadowType repoShadow = opState.getRepoShadowRequired();
            if (inRefreshOrPropagation || !ShadowsUtil.hasRetryableOperation(repoShadow)) {
                return;
            }
            LOGGER.trace("Refreshing shadow before executing the modification operation");
            refreshShadowOperation = refreshHelper.refreshShadow(repoShadow, options, ctx.getTask(), result);
            ShadowType shadowAfterRefresh = refreshShadowOperation.getRefreshedShadow();
            if (shadowAfterRefresh == null) {
                LOGGER.trace("Shadow is gone. Nothing more to do");
                throw new ObjectNotFoundException(
                        "Shadow disappeared during modify", null, ShadowType.class, repoShadow.getOid(), PARTIAL_ERROR);
            } else {
                opState.setRepoShadow(shadowAfterRefresh);
            }
        }

        private boolean wasRefreshOperationSuccessful() {
            if (refreshShadowOperation == null) {
                LOGGER.trace("Nothing refreshed, modify can continue.");
                return true;
            }

            if (refreshShadowOperation.getExecutedDeltas() == null || refreshShadowOperation.getExecutedDeltas().isEmpty()) {
                LOGGER.trace("No executed deltas after refresh. Continue with modify operation.");
                return true;
            }

            if (refreshShadowOperation.getRefreshedShadow() == null) {
                LOGGER.trace("Shadow is gone. Probably it was deleted during refresh. Finishing modify operation now.");
                return false;
            }

            Collection<ObjectDeltaOperation<ShadowType>> objectDeltaOperations = refreshShadowOperation.getExecutedDeltas();
            for (ObjectDeltaOperation<ShadowType> shadowDelta : objectDeltaOperations) {
                OperationResult result = shadowDelta.getExecutionResult();
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
            ShadowType repoShadow = opState.getRepoShadowRequired();
            try {
                ctx.checkNotInMaintenance();

                ConnectorOperationOptions connOptions = commonHelper.createConnectorOperationOptions(ctx, options, result);
                AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue<?>>>> asyncResult =
                        resourceObjectConverter
                                .modifyResourceObject(ctx, repoShadow, scripts, connOptions, modifications, now, result);
                opState.recordRealAsynchronousResult(asyncResult);

                // TODO should we mark the resource as UP here, as we do for ADD and DELETE?

                Collection<PropertyDelta<PrismPropertyValue<?>>> knownExecutedDeltas = asyncResult.getReturnValue();
                if (knownExecutedDeltas != null) {
                    ItemDeltaCollectionsUtil.addNotEquivalent(modifications, knownExecutedDeltas);
                }

            } catch (Exception ex) {
                LOGGER.debug("Provisioning exception: {}:{}, attempting to handle it", ex.getClass(), ex.getMessage(), ex);
                statusFromErrorHandling = handleModifyError(ex, result.getLastSubresult(), result);
            }

            LOGGER.debug("MODIFY {}: resource operation executed, operation state: {}", repoShadow, opState.shortDumpLazily());
        }

        private void notifyAfterModify(OperationResult result) {
            ShadowType repoShadow = opState.getRepoShadowRequired();
            ObjectDelta<ShadowType> delta =
                    prismContext.deltaFactory().object().createModifyDelta(repoShadow.getOid(), modifications, ShadowType.class);
            notifyAboutSuccessOperation(ctx, repoShadow, opState, delta, result);
        }

        private OperationResultStatus handleModifyError(
                @NotNull Exception cause, OperationResult failedOperationResult, @NotNull OperationResult result)
                throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException,
                ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, PolicyViolationException,
                ExpressionEvaluationException {

            ShadowType repoShadow = opState.getRepoShadowRequired();
            LOGGER.debug("Handling provisioning MODIFY exception {}: {}", cause.getClass(), cause.getMessage());
            try {

                OperationResultStatus finalStatus = errorHandlerLocator
                        .locateErrorHandlerRequired(cause)
                        .handleModifyError(ctx, repoShadow, modifications, options, opState, cause, failedOperationResult, result);
                LOGGER.debug("Handled provisioning MODIFY exception, final status: {}, operation state: {}",
                        finalStatus, opState.shortDumpLazily());
                return finalStatus;

            } catch (CommonException e) {
                LOGGER.debug("Handled provisioning MODIFY exception, final exception: {}, operation state: {}",
                        e, opState.shortDumpLazily());
                ObjectDelta<ShadowType> delta = repoShadow.asPrismObject().createModifyDelta();
                delta.addModifications(modifications);
                commonHelper.handleErrorHandlerException(ctx, opState, delta, e.getMessage(), result);
                throw e;
            }
        }
    }
}
