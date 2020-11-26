/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningRequest;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PredefinedOperationRequestTransformationType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncChangeListener;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningTarget;
import com.evolveum.midpoint.provisioning.ucf.api.connectors.AbstractManagedConnectorInstance;
import com.evolveum.midpoint.repo.api.RepositoryAware;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.SecurityContextManagerAware;
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskManagerAware;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import static com.evolveum.midpoint.prism.PrismObject.asObjectable;

import static java.util.Collections.*;

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

    private static final String OP_ADD_OBJECT = AsyncProvisioningConnectorInstance.class.getName() + ".addObject";
    private static final String OP_MODIFY_OBJECT = AsyncProvisioningConnectorInstance.class.getName() + ".modifyObject";
    private static final String OP_DELETE_OBJECT = AsyncProvisioningConnectorInstance.class.getName() + ".deleteObject";

    private static final com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory CAPABILITY_OBJECT_FACTORY
            = new com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory();

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
        OperationResult result = parentResult.createSubresult(ConnectorTestOperation.CONNECTOR_CONNECTION.getOperation());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, AsyncProvisioningConnectorInstance.class);
        result.addContext("connector", getConnectorObject().toString());
        try {
            targetsReference.get().forEach(t -> t.test(result));
            result.computeStatus();
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't test async provisioning targets: " + e.getMessage(), e);
        }
    }

    @Override
    public AsynchronousOperationReturnValue<Collection<ResourceAttribute<?>>> addObject(PrismObject<? extends ShadowType> object,
            StateReporter reporter, OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("addObject");
        OperationResult result = parentResult.createSubresult(OP_ADD_OBJECT);
        try {
            OperationRequested operation = new OperationRequested.Add(object.asObjectable(), getPrismContext());
            return createAndSendRequest(operation, reporter.getTask(), result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public AsynchronousOperationReturnValue<Collection<PropertyModificationOperation>> modifyObject(
            ResourceObjectIdentification identification, PrismObject<ShadowType> shadow, @NotNull Collection<Operation> changes,
            ConnectorOperationOptions options, StateReporter reporter, OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("modifyObject");
        OperationResult result = parentResult.createSubresult(OP_MODIFY_OBJECT);
        try {
            OperationRequested operation =
                    new OperationRequested.Modify(identification, asObjectable(shadow), changes, options, getPrismContext());
            return createAndSendRequest(operation, reporter.getTask(), result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public AsynchronousOperationResult deleteObject(ObjectClassComplexTypeDefinition objectClass,
            PrismObject<ShadowType> shadow, Collection<? extends ResourceAttribute<?>> identifiers, StateReporter reporter,
            OperationResult parentResult) throws SchemaException {
        InternalMonitor.recordConnectorOperation("deleteObject");
        OperationResult result = parentResult.createSubresult(OP_DELETE_OBJECT);
        try {
            OperationRequested operation =
                    new OperationRequested.Delete(objectClass, asObjectable(shadow), identifiers, getPrismContext());
            return createAndSendRequest(operation, reporter.getTask(), result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private <X> AsynchronousOperationReturnValue<X> createAndSendRequest(OperationRequested operation, Task task,
            OperationResult result) {
        AsyncProvisioningRequest request = transformer.transformOperationRequested(operation, task, result);
        String asyncOperationReference = sendRequest(request, result);

        AsynchronousOperationReturnValue<X> ret = new AsynchronousOperationReturnValue<>();
        if (configuration.isOperationExecutionConfirmation()) {
            ret.setOperationType(PendingOperationTypeType.ASYNCHRONOUS);
            result.recordInProgress();
            result.setAsynchronousOperationReference(asyncOperationReference);
        } else {
            result.recordSuccess();
        }
        ret.setOperationResult(result);
        return ret;
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
    public Collection<Object> fetchCapabilities(OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("capabilities");

        Collection<Object> capabilities = new ArrayList<>();
        capabilities.add(CAPABILITY_OBJECT_FACTORY.createCreate(new CreateCapabilityType()));
        capabilities.add(CAPABILITY_OBJECT_FACTORY.createUpdate(new UpdateCapabilityType()
                .addRemoveAttributeValues(true)));
        capabilities.add(CAPABILITY_OBJECT_FACTORY.createDelete(new DeleteCapabilityType()));
        // TODO
        return capabilities;
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
    public void listenForChanges(@NotNull AsyncChangeListener changeListener, @NotNull Supplier<Boolean> canRunSupplier,
            @NotNull OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceSchema fetchResourceSchema(OperationResult parentResult) {
        // Schema discovery is not supported. Schema must be defined manually. Or other connector has to provide it.
        InternalMonitor.recordConnectorOperation("schema");
        return null;
    }

    @Override
    public PrismObject<ShadowType> fetchObject(ResourceObjectIdentification resourceObjectIdentification,
            AttributesToReturn attributesToReturn, StateReporter reporter, OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("fetchObject");
        return null;
    }

    @Override
    public SearchResultMetadata search(ObjectClassComplexTypeDefinition objectClassDefinition, ObjectQuery query,
            ShadowResultHandler handler, AttributesToReturn attributesToReturn,
            PagedSearchCapabilityType pagedSearchConfigurationType, SearchHierarchyConstraints searchHierarchyConstraints,
            StateReporter reporter, OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("search");
        return null;
    }

    @Override
    public int count(ObjectClassComplexTypeDefinition objectClassDefinition, ObjectQuery query,
            PagedSearchCapabilityType pagedSearchConfigurationType, StateReporter reporter, OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("count");
        return 0;
    }

    @Override
    public Object executeScript(ExecuteProvisioningScriptOperation scriptOperation, StateReporter reporter,
            OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("executeScript");
        return null;
    }

    @Override
    public PrismProperty<?> deserializeToken(Object serializedToken) {
        return null;
    }

    @Override
    public <T> PrismProperty<T> fetchCurrentToken(ObjectClassComplexTypeDefinition objectClass, StateReporter reporter,
            OperationResult parentResult) {
        return null;
    }

    @Override
    public void fetchChanges(ObjectClassComplexTypeDefinition objectClass, PrismProperty<?> lastToken,
            AttributesToReturn attrsToReturn, Integer maxChanges, StateReporter reporter,
            LiveSyncChangeListener changeHandler, OperationResult parentResult) {
    }

    @Override
    public String toString() {
        return "AsyncProvisioningConnectorInstance (" + getInstanceName() + ")";
    }
    //endregion
}
