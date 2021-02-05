/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncUpdateSource;
import com.evolveum.midpoint.provisioning.ucf.api.async.UcfAsyncUpdateChangeListener;
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
import com.evolveum.midpoint.task.api.Tracer;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateErrorHandlingActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AsyncUpdateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;

/**
 *  Connector that is able to obtain and process asynchronous updates.
 *  It can be used to receive messages from JMS or AMQP messaging systems; or maybe from REST calls in the future.
 *
 *  Currently we keep no state besides the configuration and open sources and listening activities (in {@link ConnectorListener}).
 *  It is because calls to this connector should be really infrequent. Sources are therefore instantiated on demand,
 *  e.g. on {@link #test(OperationResult)} or {@link #listenForChanges(UcfAsyncUpdateChangeListener, Supplier, OperationResult)} calls.
 */
@SuppressWarnings("DefaultAnnotationParam")
@ManagedConnector(type="AsyncUpdateConnector", version="1.0.0")
public class AsyncUpdateConnectorInstance extends AbstractManagedConnectorInstance implements UcfExpressionEvaluatorAware,
        SecurityContextManagerAware, TracerAware, TaskManagerAware, RepositoryAware {

    @SuppressWarnings("unused")
    private static final Trace LOGGER = TraceManager.getTrace(AsyncUpdateConnectorInstance.class);

    private static final com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory CAPABILITY_OBJECT_FACTORY
            = new com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory();

    private ConnectorConfiguration configuration;

    private final SourceManager sourceManager = new SourceManager(this);

    /**
     * The expression evaluator has to come from the higher layers because it needs features not present in UCF impl module.
     */
    private UcfExpressionEvaluator ucfExpressionEvaluator;

    private SecurityContextManager securityContextManager;

    private Tracer tracer;

    private TaskManager taskManager;

    private RepositoryService repositoryService;

    /**
     * Current listener. There can be at most one. See {@link #listenForChanges(UcfAsyncUpdateChangeListener, Supplier, OperationResult)}.
     */
    private final AtomicReference<ConnectorListener> listener = new AtomicReference<>();

    @ManagedConnectorConfiguration
    public ConnectorConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ConnectorConfiguration configuration) {
        LOGGER.info("Setting new configuration in {}", this); // todo debug
        configuration.validate();
        if (configuration.hasSourcesChanged(this.configuration)) {
            ConnectorListener currentListener = listener.get();
            if (currentListener != null) {
                LOGGER.info("Configuration of sources has changed. Restarting listening in {}", this); // todo debug
                currentListener.restart(configuration);
            }
        }
        this.configuration = configuration;
    }

    @Override
    protected void connect(OperationResult result) {
        // no-op
    }

    @Override
    protected void disconnect(OperationResult result) {
        // no-op - we act on configuration change in setConfiguration method because
        // we need the original configuration to know the difference
    }

    @Override
    public void test(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(ConnectorTestOperation.CONNECTOR_CONNECTION.getOperation());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, AsyncUpdateConnectorInstance.class);
        result.addContext("connector", getConnectorObject().toString());
        Collection<AsyncUpdateSource> sources = sourceManager.createSources(configuration.getAllSources());
        try {
            sources.forEach(s -> s.test(result));
            result.computeStatus();
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't test async update sources: " + e.getMessage(), e);
        }
    }

    @Override
    public void dispose() {
        // This operation is invoked on system shutdown; for simplicity let's not try to cancel open listening activities
        // as they were probably cancelled on respective Async Update tasks going down; and will be cancelled on system
        // shutdown anyway.
        //
        // This will change if the use of dispose() will change.
    }

    @Override
    public void listenForChanges(@NotNull UcfAsyncUpdateChangeListener changeListener, @NotNull Supplier<Boolean> canRunSupplier,
            @NotNull OperationResult parentResult) throws SchemaException {

        if (listener.get() != null) {
            throw new IllegalStateException("A listening is already in progress in " + this);
        }

        Authentication authentication = securityContextManager.getAuthentication();
        TransformationalAsyncUpdateMessageListener transformationalListener =
                new TransformationalAsyncUpdateMessageListener(changeListener, authentication, this);
        ConnectorListener newListener = new ConnectorListener(this, transformationalListener);

        boolean success = listener.compareAndSet(null, newListener);
        if (success) {
            // listener was null, now is set
            try {
                newListener.listenForChanges(configuration, canRunSupplier);
            } finally {
                listener.set(null);
            }
        } else {
            throw new IllegalStateException("Another listening has been started in " + this);
        }
    }

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
        capabilities.add(CAPABILITY_OBJECT_FACTORY.createAsyncUpdate(new AsyncUpdateCapabilityType()));
        return capabilities;

        // TODO activation, credentials?
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
    public Tracer getTracer() {
        return tracer;
    }

    @Override
    public void setTracer(Tracer tracer) {
        this.tracer = tracer;
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

    ExpressionType getTransformExpression() {
        return configuration.getTransformExpression();
    }

    @NotNull
    public AsyncUpdateErrorHandlingActionType getErrorHandlingAction() {
        return ObjectUtils.defaultIfNull(configuration.getErrorHandlingAction(),
                AsyncUpdateErrorHandlingActionType.STOP_PROCESSING);
    }

    SourceManager getSourceManager() {
        return sourceManager;
    }

    //region Unsupported operations
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
            FetchedObjectHandler handler, AttributesToReturn attributesToReturn,
            PagedSearchCapabilityType pagedSearchConfiguration, SearchHierarchyConstraints searchHierarchyConstraints,
            UcfFetchErrorReportingMethod ucfErrorReportingMethod,
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
    public AsynchronousOperationReturnValue<Collection<ResourceAttribute<?>>> addObject(PrismObject<? extends ShadowType> object,
            StateReporter reporter, OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("addObject");
        return null;
    }

    @Override
    public AsynchronousOperationReturnValue<Collection<PropertyModificationOperation>> modifyObject(
            ResourceObjectIdentification identification, PrismObject<ShadowType> shadow, @NotNull Collection<Operation> changes,
            ConnectorOperationOptions options, StateReporter reporter, OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("modifyObject");
        return null;
    }

    @Override
    public AsynchronousOperationResult deleteObject(ObjectClassComplexTypeDefinition objectClass,
            PrismObject<ShadowType> shadow, Collection<? extends ResourceAttribute<?>> identifiers, StateReporter reporter, OperationResult parentResult) {
        InternalMonitor.recordConnectorOperation("deleteObject");
        return null;
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
    public UcfFetchChangesResult fetchChanges(ObjectClassComplexTypeDefinition objectClass, PrismProperty<?> lastToken,
            AttributesToReturn attrsToReturn, Integer maxChanges, StateReporter reporter,
            @NotNull UcfLiveSyncChangeListener changeHandler, OperationResult parentResult) {
        return null;
    }

    //endregion

    @Override
    public String toString() {
        return "AsyncUpdateConnectorInstance (" + getInstanceName() + ")";
    }

    @Override
    protected void setResourceSchema(ResourceSchema resourceSchema) {
        super.setResourceSchema(resourceSchema);
        // TODO eliminate these diagnostic messages when no longer needed (MID-5931)
        if (resourceSchema == null) {
            LOGGER.warn("Setting null resource schema for {}. This might or might not be OK, depending on circumstances", this);
        } else {
            LOGGER.info("Setting resource schema for {}", this);
        }
    }
}
