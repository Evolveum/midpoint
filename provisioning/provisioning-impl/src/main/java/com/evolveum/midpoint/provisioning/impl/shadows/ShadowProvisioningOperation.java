/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.shadows.errors.ErrorHandlerLocator;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.OperationResultRecorder;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowCreator;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowUpdater;

import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.RunAsCapabilityType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade.OP_DELAYED_OPERATION;
import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsUtil.createSuccessOperationDescription;

/**
 * Superclass for "primitive" resource-updating operations: add, modify, delete [resource object / shadow].
 */
public abstract class ShadowProvisioningOperation<OS extends ProvisioningOperationState<?>> {

    // Useful Spring beans

    final AccessChecker accessChecker = ShadowsLocalBeans.get().accessChecker;
    final ShadowFinder shadowFinder = ShadowsLocalBeans.get().shadowFinder;
    final ShadowCreator shadowCreator = ShadowsLocalBeans.get().shadowCreator;
    final OperationResultRecorder resultRecorder = ShadowsLocalBeans.get().operationResultRecorder;
    final ShadowUpdater shadowUpdater = ShadowsLocalBeans.get().shadowUpdater;
    private final ProvisioningContextFactory ctxFactory = ShadowsLocalBeans.get().ctxFactory;
    final ResourceObjectConverter resourceObjectConverter = ShadowsLocalBeans.get().resourceObjectConverter;
    final EntitlementsHelper entitlementsHelper = ShadowsLocalBeans.get().entitlementsHelper;
    final ShadowCaretaker shadowCaretaker = ShadowsLocalBeans.get().shadowCaretaker;
    final ErrorHandlerLocator errorHandlerLocator = ShadowsLocalBeans.get().errorHandlerLocator;
    private final EventDispatcher eventDispatcher = ShadowsLocalBeans.get().eventDispatcher;
    final Clock clock = ShadowsLocalBeans.get().clock;

    @NotNull final ProvisioningContext ctx;
    final OperationProvisioningScriptsType scripts;
    @NotNull final OS opState;
    final ProvisioningOperationOptions options;
    @NotNull final Task task;

    /** A delta that represents the original request. */
    @NotNull final ObjectDelta<ShadowType> requestedDelta;

    /**
     * A delta that represents the original request, narrowed down to operation(s) to be executed on the resource.
     *
     * - For ADD operation, we allow it to be the same as {@link #requestedDelta}.
     * - For DELETE op, they are obviously the same.
     * - But for MODIFY op, only resource-level modifications are transferred there.
     */
    @NotNull final ObjectDelta<ShadowType> resourceDelta;

    /** A delta that represents what was executed. */
    ObjectDelta<ShadowType> executedDelta;

    OperationResultStatus statusFromErrorHandling;

    ShadowProvisioningOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull OS opState,
            OperationProvisioningScriptsType scripts,
            ProvisioningOperationOptions options,
            @NotNull ObjectDelta<ShadowType> requestedDelta,
            @NotNull ObjectDelta<ShadowType> resourceDelta) {
        this.ctx = ctx;
        this.scripts = scripts;
        this.opState = opState;
        this.options = options;
        this.task = ctx.getTask();
        this.requestedDelta = requestedDelta;
        this.resourceDelta = resourceDelta;
    }

    ShadowProvisioningOperation(
            @NotNull ProvisioningContext ctx,
            @NotNull OS opState,
            OperationProvisioningScriptsType scripts,
            ProvisioningOperationOptions options,
            @NotNull ObjectDelta<ShadowType> requestedDelta) {
        this(ctx, opState, scripts, options, requestedDelta, requestedDelta);
    }

    public @NotNull ProvisioningContext getCtx() {
        return ctx;
    }

    public ProvisioningOperationOptions getOptions() {
        return options;
    }

    public @NotNull OS getOpState() {
        return opState;
    }

    /** "adding", "modifying", "deleting" */
    public abstract String getGerund();

    /** "ADD", "MODIFY", "DELETE" (e.g. for logging purposes) */
    public abstract String getLogVerb();

    abstract Trace getLogger();

    /** Returns the delta that was requested to be executed. */
    @NotNull ObjectDelta<ShadowType> getRequestedDelta() {
        return requestedDelta;
    }

    /**
     * Returns the delta that represents the operation on the resource.
     * E.g. for modify op it should contain only resource modifications.
     */
    public @NotNull ObjectDelta<ShadowType> getResourceDelta() {
        return resourceDelta;
    }

    void setExecutedDelta(ObjectDelta<ShadowType> executedDelta) {
        this.executedDelta = executedDelta;
    }

    /** Returns the delta that was requested to be executed OR that was really executed. */
    private @NotNull ObjectDelta<ShadowType> getEffectiveDelta() {
        return Objects.requireNonNullElse(executedDelta, requestedDelta);
    }

    public boolean isAdd() {
        return this instanceof ShadowAddOperation;
    }

    void handleErrorHandlerException(String message, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ObjectAlreadyExistsException, ExpressionEvaluationException {

        // Error handler had re-thrown the exception. We will throw the exception later.
        // But first we need to record changes in opState and in repository shadow.
        resultRecorder.recordOperationException(this, result);

        ShadowType shadow;
        if (isAdd()) {
            // This is more precise. Besides, there is no repo shadow in some cases (e.g. adding protected shadow). [TODO??]
            shadow = ((ShadowAddOperation) this).getResourceObjectAddedOrToAdd();
        } else {
            shadow = opState.getRepoShadow();
        }
        ResourceOperationDescription operationDescription =
                ShadowsUtil.createResourceFailureDescription(shadow, ctx.getResource(), getRequestedDelta(), message);
        eventDispatcher.notifyFailure(operationDescription, ctx.getTask(), result);
    }

    void markOperationExecutionAsPending(OperationResult parentResult) {
        opState.setExecutionStatus(PendingOperationExecutionStatusType.EXECUTION_PENDING);

        // Create dummy subresult with IN_PROGRESS state.
        // This will force the entire result (parent) to be IN_PROGRESS rather than SUCCESS.
        OperationResult result = parentResult.createSubresult(OP_DELAYED_OPERATION);
        result.recordInProgress();
        result.close();

        getLogger().debug("{}: Resource operation NOT executed, execution pending", getLogVerb());
    }

    /**
     * This is quite an ugly hack - setting the status/message in the root {@link ProvisioningService} operation result.
     */
    void setParentOperationStatus(OperationResult parentResult) {
        parentResult.computeStatus(true); // To provide the error message from the subresults
        if (statusFromErrorHandling != null) {
            parentResult.setStatus(statusFromErrorHandling);
        } else if (!opState.isCompleted()) {
            parentResult.setInProgress();
        }
        parentResult.setAsynchronousOperationReference(opState.getAsynchronousOperationReference());
    }

    void sendSuccessOrInProgressNotification(ShadowType shadow, OperationResult result) {
        ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, shadow, getEffectiveDelta());
        if (opState.isExecuting()) {
            eventDispatcher.notifyInProgress(operationDescription, ctx.getTask(), result);
        } else {
            eventDispatcher.notifySuccess(operationDescription, ctx.getTask(), result);
        }
    }

    ConnectorOperationOptions createConnectorOperationOptions(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
        if (options == null) {
            return null;
        }
        String runAsAccountOid = options.getRunAsAccountOid();
        if (runAsAccountOid == null) {
            return null;
        }
        RunAsCapabilityType capRunAs = ctx.getCapability(RunAsCapabilityType.class); // TODO check it's enabled!
        if (capRunAs == null) {
            getLogger().trace("Operation runAs requested, but resource does not have the capability. Ignoring runAs");
            return null;
        }
        PrismObject<ShadowType> runAsShadow;
        try {
            runAsShadow = shadowFinder.getShadow(runAsAccountOid, result);
        } catch (ObjectNotFoundException e) {
            throw new ConfigurationException("Requested non-existing 'runAs' shadow", e);
        }
        ProvisioningContext runAsCtx = ctxFactory.createForShadow(runAsShadow.asObjectable(), ctx.getResource(), ctx.getTask());
        runAsCtx.applyAttributesDefinition(runAsShadow);
        ResourceObjectIdentification runAsIdentification =
                ResourceObjectIdentification.createFromShadow(
                        runAsCtx.getObjectDefinitionRequired(), runAsShadow.asObjectable());
        ConnectorOperationOptions connOptions = new ConnectorOperationOptions();
        getLogger().trace("RunAs identification: {}", runAsIdentification);
        connOptions.setRunAsIdentification(runAsIdentification);
        return connOptions;
    }

    boolean checkAndRecordPendingOperationBeforeExecution(OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        if (resourceDelta.isEmpty()) {
            return false;
        }
        PendingOperationType duplicateOperation =
                shadowUpdater.checkAndRecordPendingOperationBeforeExecution(ctx, resourceDelta, opState, result);
        if (duplicateOperation != null) {
            result.setInProgress();
            return true;
        } else {
            return false;
        }
    }
}
