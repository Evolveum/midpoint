/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import static com.evolveum.midpoint.prism.PrismObject.asObjectable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningRequest;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.provisioning.ucf.api.UcfExecutionContext;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningTarget;
import com.evolveum.midpoint.provisioning.ucf.api.connectors.AbstractManagedConnectorInstance;
import com.evolveum.midpoint.repo.api.RepositoryAware;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.SecurityContextManagerAware;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskManagerAware;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PredefinedOperationRequestTransformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

/**
 *  Connector that is able to invoke asynchronous provisioning.
 *  It can be used to send messages to JMS or AMQP messaging systems; or maybe to REST endpoints in the future.
 */
@SuppressWarnings("DefaultAnnotationParam")
@ManagedConnector(type="AsyncProvisioningConnector", version="1.0.0")
public class AsyncProvisioningConnectorInstance extends AbstractManagedConnectorInstance implements UcfExpressionEvaluatorAware,
        SecurityContextManagerAware, TaskManagerAware, RepositoryAware {

    @SuppressWarnings("unused")
    private static final Trace LOGGER = TraceManager.getTrace(AsyncProvisioningConnectorInstance.class);

    private ConnectorConfiguration configuration;

    /** Always holds immutable collection. */
    private final AtomicReference<List<AsyncProvisioningTarget>> targetsReference = new AtomicReference<>(emptyList());

    private final TargetManager targetManager = new TargetManager(this);
    private final OperationRequestTransformer transformer = new OperationRequestTransformer(this);

    /**
     * The expression evaluator has to come from the higher layers because it needs features not present in UCF impl module.
     */
    private UcfExpressionEvaluator ucfExpressionEvaluator;

    private SecurityContextManager securityContextManager;

    private TaskManager taskManager;

    private RepositoryService repositoryService;

    @ManagedConnectorConfiguration
    public ConnectorConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ConnectorConfiguration configuration) {
        LOGGER.info("Setting new configuration in {}", this); // todo debug
        configuration.validate();
        this.configuration = configuration;
    }

    @NotNull
    private List<AsyncProvisioningTarget> createNewTargets() {
        return unmodifiableList(targetManager.createTargets(configuration.getAllTargets()));
    }

    /**
     * Gracefully shuts down existing target and then tries to replace it with its new instantiation.
     */
    private AsyncProvisioningTarget restartTarget(AsyncProvisioningTarget target) {
        target.disconnect();

        List<AsyncProvisioningTarget> existingTargets = targetsReference.get();
        int i = existingTargets.indexOf(target);
        if (i < 0) {
            LOGGER.info("Not restarting {}, because it's no longer among the targets", target);
            return existingTargets.isEmpty() ? null : existingTargets.get(0);
        }

        AsyncProvisioningTarget restartedTarget = target.copy();
        restartedTarget.connect();

        List<AsyncProvisioningTarget> updatedTargets = updateTargetsList(existingTargets, i, restartedTarget);
        if (targetsReference.compareAndSet(existingTargets, updatedTargets)) {
            LOGGER.debug("Target {} was successfully restarted and replaced in the targets list", restartedTarget);
        } else {
            LOGGER.info("Target {} was restarted but the targets list couldn't be updated, as it was changed in the meanwhile",
                    restartedTarget);
        }
        return restartedTarget;
    }

    @NotNull
    private List<AsyncProvisioningTarget> updateTargetsList(List<AsyncProvisioningTarget> existingTargets, int i,
            AsyncProvisioningTarget restartedTarget) {
        List<AsyncProvisioningTarget> updatedTargets = new ArrayList<>(existingTargets.subList(0, i));
        updatedTargets.add(restartedTarget);
        updatedTargets.addAll(existingTargets.subList(i + 1, existingTargets.size()));
        return updatedTargets;
    }

    @Override
    protected void connect(OperationResult result) {
        List<AsyncProvisioningTarget> newTargets = createNewTargets();
        newTargets.forEach(AsyncProvisioningTarget::connect);
        targetsReference.set(newTargets);
    }

    @Override
    protected void disconnect(OperationResult result) {
        targetsReference.get().forEach(AsyncProvisioningTarget::disconnect);
    }

    @Override
    public void test(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_TEST);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, getClass());
        result.addContext("connector", getConnectorObject().toString());
        try {
            targetsReference.get().forEach(t -> t.test(result));
            result.computeStatus();
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't test async provisioning targets: " + e.getMessage(), e);
        }
    }

    @Override
    public void testPartialConfiguration(OperationResult parentResult) {
        // no-op
    }

    @Override
    public @NotNull Collection<PrismProperty<?>> discoverConfiguration(OperationResult parentResult) {
        return Collections.emptySet();
    }

    @Override
    public @NotNull CapabilityCollectionType getNativeCapabilities(OperationResult result) throws CommunicationException, GenericFrameworkException, ConfigurationException {
        return fetchCapabilities(result);
    }

    @Override
    public @NotNull UcfAddReturnValue addObject(
            @NotNull PrismObject<? extends ShadowType> object,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull OperationResult parentResult) {
        UcfExecutionContext.checkExecutionFullyPersistent(ctx);
        InternalMonitor.recordConnectorOperation("addObject");
        OperationResult result = parentResult.createSubresult(OP_ADD_OBJECT);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, getClass());
        try {
            OperationRequested operation = new OperationRequested.Add(object.asObjectable());
            createAndSendRequest(operation, ctx.getTask(), result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
        return UcfAddReturnValue.fromResult(result, getOperationType(result));
    }

    private static @Nullable PendingOperationTypeType getOperationType(OperationResult result) {
        return result.isInProgress() ? PendingOperationTypeType.ASYNCHRONOUS : null;
    }

    @Override
    public @NotNull UcfModifyReturnValue modifyObject(
            @NotNull ResourceObjectIdentification.WithPrimary identification,
            PrismObject<ShadowType> shadow,
            @NotNull Collection<Operation> changes,
            ConnectorOperationOptions options,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull OperationResult parentResult) {
        UcfExecutionContext.checkExecutionFullyPersistent(ctx);
        InternalMonitor.recordConnectorOperation("modifyObject");
        OperationResult result = parentResult.createSubresult(OP_MODIFY_OBJECT);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, getClass());
        try {
            OperationRequested operation =
                    new OperationRequested.Modify(identification, asObjectable(shadow), changes, options);
            createAndSendRequest(operation, ctx.getTask(), result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
        return UcfModifyReturnValue.fromResult(result, getOperationType(result));
    }

    @Override
    public @NotNull UcfDeleteResult deleteObject(
            @NotNull ResourceObjectIdentification<?> identification,
            PrismObject<ShadowType> shadow,
            @NotNull UcfExecutionContext ctx,
            @NotNull OperationResult parentResult) throws SchemaException {
        UcfExecutionContext.checkExecutionFullyPersistent(ctx);
        InternalMonitor.recordConnectorOperation("deleteObject");
        OperationResult result = parentResult.createSubresult(OP_DELETE_OBJECT);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, getClass());
        try {
            OperationRequested operation =
                    new OperationRequested.Delete(identification, asObjectable(shadow));
            createAndSendRequest(operation, ctx.getTask(), result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
        return UcfDeleteResult.fromResult(result, getOperationType(result));
    }

    private void createAndSendRequest(
            OperationRequested operation, Task task, OperationResult result) {
        AsyncProvisioningRequest request = transformer.transformOperationRequested(operation, task, result);
        String asyncOperationReference = sendRequest(request, result);
        if (configuration.isOperationExecutionConfirmation()) {
            result.setInProgress();
            result.setAsynchronousOperationReference(asyncOperationReference);
        } else {
            result.setStatus(OperationResultStatus.SUCCESS);
        }
    }

    private String sendRequest(AsyncProvisioningRequest request, OperationResult result) {
        List<AsyncProvisioningTarget> targets = targetsReference.get();
        if (targets.isEmpty()) {
            throw new IllegalStateException("No targets available");
        }

        Throwable lastException = null;
        for (AsyncProvisioningTarget target : targets) {
            try {
                return target.send(request, result);
            } catch (Throwable t) {
                LOGGER.warn("Couldn't send a request to target {}, restarting the target and trying again", target, t);
                AsyncProvisioningTarget restartedTarget = restartTarget(target);
                if (restartedTarget != null) {
                    try {
                        result.muteLastSubresultError();
                        return restartedTarget.send(request, result);
                    } catch (Throwable t2) {
                        LOGGER.warn("Couldn't send a request to target {} again, trying the next one", target, t2);
                        lastException = t2;
                    }
                } else {
                    LOGGER.info("Target couldn't be restarted (maybe it was re-created in the meanwhile), trying the next one");
                    lastException = t;
                }
            }
        }
        assert lastException != null;
        throw new SystemException("Couldn't send request to any of the targets available. Last exception: " +
                lastException.getMessage(), lastException);
    }

    //region Trivia
    @Override
    public ConnectorOperationalStatus getOperationalStatus() {
        ConnectorOperationalStatus status = new ConnectorOperationalStatus();
        status.setConnectorClassName(this.getClass().getName());
        return status;
    }

    @Override
    public @NotNull CapabilityCollectionType fetchCapabilities(OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("capabilities");
        return new CapabilityCollectionType()
                .create(new CreateCapabilityType())
                .update(new UpdateCapabilityType()
                        .addRemoveAttributeValues(true))
                .delete(new DeleteCapabilityType());
    }

    @Override
    public UcfExpressionEvaluator getUcfExpressionEvaluator() {
        return ucfExpressionEvaluator;
    }

    @Override
    public void setUcfExpressionEvaluator(UcfExpressionEvaluator evaluator) {
        this.ucfExpressionEvaluator = evaluator;
    }

    @Override
    public SecurityContextManager getSecurityContextManager() {
        return securityContextManager;
    }

    @Override
    public void setSecurityContextManager(SecurityContextManager securityContextManager) {
        this.securityContextManager = securityContextManager;
    }

    @Override
    public TaskManager getTaskManager() {
        return taskManager;
    }

    @Override
    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    @Override
    public void setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    PredefinedOperationRequestTransformationType getPredefinedTransformation() {
        return configuration.getPredefinedTransformation();
    }

    ExpressionType getTransformExpression() {
        return configuration.getTransformExpression();
    }

    @Override
    public NativeResourceSchema fetchResourceSchema(@NotNull OperationResult parentResult) {
        // Schema discovery is not supported. Schema must be defined manually. Or other connector has to provide it.
        InternalMonitor.recordConnectorOperation("schema");
        return null;
    }

    @Override
    public UcfResourceObject fetchObject(
            @NotNull ResourceObjectIdentification.WithPrimary resourceObjectIdentification,
            @Nullable ShadowItemsToReturn shadowItemsToReturn,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("fetchObject");
        return null;
    }

    @Override
    public SearchResultMetadata search(@NotNull ResourceObjectDefinition objectDefinition, ObjectQuery query,
            @NotNull UcfObjectHandler handler, @Nullable ShadowItemsToReturn shadowItemsToReturn,
            @Nullable PagedSearchCapabilityType pagedSearchConfiguration, @Nullable SearchHierarchyConstraints searchHierarchyConstraints,
            @Nullable UcfFetchErrorReportingMethod ucfErrorReportingMethod,
            @NotNull SchemaAwareUcfExecutionContext ctx, @NotNull OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("search");
        return null;
    }

    @Override
    public int count(ResourceObjectDefinition objectDefinition, ObjectQuery query,
            PagedSearchCapabilityType pagedSearchConfigurationType, UcfExecutionContext ctx, OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("count");
        return 0;
    }

    @Override
    public Object executeScript(ExecuteProvisioningScriptOperation scriptOperation,
            UcfExecutionContext ctx, OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("executeScript");
        return null;
    }

    @Override
    public UcfFetchChangesResult fetchChanges(
            @Nullable ResourceObjectDefinition objectDefinition,
            @Nullable UcfSyncToken lastToken,
            @Nullable ShadowItemsToReturn attrsToReturn,
            @Nullable Integer maxChanges,
            @NotNull SchemaAwareUcfExecutionContext ctx,
            @NotNull UcfLiveSyncChangeListener changeHandler,
            @NotNull OperationResult parentResult) {
        return null;
    }

    @Override
    public String toString() {
        return "AsyncProvisioningConnectorInstance (" + getInstanceName() + ")";
    }
    //endregion
}
