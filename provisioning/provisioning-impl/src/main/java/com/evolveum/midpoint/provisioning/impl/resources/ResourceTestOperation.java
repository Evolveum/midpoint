/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static com.evolveum.midpoint.provisioning.api.ResourceTestOptions.TestMode.FULL;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType.*;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.provisioning.api.ResourceTestOptions;
import com.evolveum.midpoint.provisioning.api.ResourceTestOptions.ResourceCompletionMode;
import com.evolveum.midpoint.provisioning.api.ResourceTestOptions.TestMode;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.constants.TestResourceOpNames;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.NativeResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

/**
 * Responsible for testing the resource. Provides different flavors of the "test resource" operation, according
 * to {@link ResourceTestOptions} used.
 *
 * The resource being tested is either a regular one, stored (and updated) in the repository, or an in-memory, "draft" object.
 * Options drive how it should be updated.
 *
 * Notes to maintainers:
 *
 * - When test failure occurs, {@link OperationResult} should be updated, and {@link TestFailedException} should be thrown.
 * Resource availability status is updated upon catching that exception.
 */
class ResourceTestOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceTestOperation.class);

    @NotNull protected final ResourceType resource;

    /** No defaults in this object! Everything must be resolved. */
    @NotNull protected final ResourceTestOptions options;

    @NotNull private final String operationDesc;

    @NotNull protected final Task task;
    @NotNull protected final CommonBeans beans;
    @NotNull private final ResourceSchemaHelper schemaHelper;

    /**
     * Extracted connector specs. Should not be modified.
     */
    @NotNull private final List<ConnectorSpec> allConnectorSpecs;

    /**
     * Native capabilities of individual connectors that are fetched during the operation. Should not be modified afterwards.
     */
    @NotNull private final NativeConnectorsCapabilities nativeConnectorsCapabilities = NativeConnectorsCapabilities.empty();

    /**
     * Resource schema that is fetched during the operation. Should not be modified afterwards. (TODO what about adjusting?)
     */
    private NativeResourceSchema nativeResourceSchema;

    private boolean resourceSchemaWasFetched;

    /**
     * @param resource Must be mutable. Must be expanded. May or may not have OID.
     */
    ResourceTestOperation(
            @NotNull ResourceType resource,
            @Nullable ResourceTestOptions options,
            @NotNull Task task,
            @NotNull CommonBeans beans)
            throws ConfigurationException {
        resource.checkMutable();
        this.resource = resource;
        this.options = resolveDefaultsInOptions(this.resource, options);
        this.operationDesc = createOperationDesc(this.resource, this.options);
        this.task = task;
        this.beans = beans;
        this.schemaHelper = beans.resourceManager.schemaHelper;
        this.allConnectorSpecs = ConnectorSpec.all(this.resource);
    }

    private String createOperationDesc(ResourceType resource, ResourceTestOptions options) {
        String name = getOrig(resource.getName());
        if (options.isFullMode()) {
            return name != null ?
                    "test resource " + name :
                    "test resource";
        } else {
            return name != null ?
                    "test partial configuration of resource " + name :
                    "test partial resource configuration";
        }
    }

    /** Resolves defaults and checks the consistency of the options. */
    private @NotNull ResourceTestOptions resolveDefaultsInOptions(@NotNull ResourceType resource, ResourceTestOptions options) {

        if (options == null) {
            options = ResourceTestOptions.DEFAULT;
        }

        ResourceCompletionMode completionMode = options.getResourceCompletionMode();
        Boolean isUpdateInRepository = options.isUpdateInRepository();

        if (options.getTestMode() == null) {
            options = options.testMode(FULL);
        }

        if (options.isFullMode()) {

            // Note that some defaults may be set in upper layers (ProvisioningServiceImpl).

            boolean updateInRepositoryDefault;
            boolean updateInMemoryDefault;
            if (resource.getOid() == null) {
                updateInRepositoryDefault = false; // Actually, for non-OID resources, the repo is never updated
                updateInMemoryDefault = true; // TODO decide about this
            } else {
                updateInRepositoryDefault = true;
                updateInMemoryDefault = false; // This is the legacy behavior
            }

            if (options.isUpdateInRepository() == null) {
                options = options.updateInRepository(updateInRepositoryDefault);
            }

            if (options.isUpdateInMemory() == null) {
                options = options.updateInMemory(updateInMemoryDefault);
            }

            // Completion mode can be arbitrary; so just set the defaults
            if (options.getResourceCompletionMode() == null) {
                if (options.isUpdateInRepository()) {
                    options = options.resourceCompletionMode(ResourceCompletionMode.IF_NOT_COMPLETE);
                } else {
                    options = options.resourceCompletionMode(ResourceCompletionMode.NEVER); // TODO decide about this
                }
            }

        } else {

            // We don't want to fetch schema/capabilities at all.
            argCheck(completionMode == null || completionMode == ResourceCompletionMode.NEVER,
                    "Unsupported completion mode for partial test: %s", completionMode);
            options = options.resourceCompletionMode(ResourceCompletionMode.NEVER);

            // Never update the repository!
            argCheck(!Boolean.TRUE.equals(isUpdateInRepository),
                    "Repository updates are not supported for partial test: %s", isUpdateInRepository);
            options = options.updateInRepository(false);

            // In-memory updates can be on or off, it does not matter here. Just set the default.
            if (options.isUpdateInMemory() == null) {
                options = options.updateInMemory(true);
            }
        }

        options.checkAllValuesSet();

        return options;
    }

    /**
     * Tests the resource and updates the resource object in repository (with capabilities and schema).
     * (This is what is called a completion.)
     */
    public @NotNull OperationResult execute(OperationResult parentResult)
            throws ObjectNotFoundException, ConfigurationException, SchemaException {

        OperationResult testResult;
        if (options.isFullMode()) {
            testResult = executeTest(
                    result -> {
                        testAllConnectors(result);
                        testResourceSchema(result);
                    },
                    parentResult);
        } else {
            testResult = executeTest(
                    this::testMainConnector,
                    parentResult);
        }

        if (shouldComplete()) {
            storeCapabilitiesAndSchema(parentResult);
        }

        return testResult;
    }

    private boolean shouldComplete() {
        switch (options.getResourceCompletionMode()) {
            case ALWAYS:
                return true;
            case IF_NOT_COMPLETE:
                // This is to preserve pre-4.6 behavior: the resource completion operation was invoked,
                // but no resource updating was carried out if the resource was already complete.
                return !ResourceTypeUtil.isComplete(resource);
            case NEVER:
                return false;
            default:
                throw new AssertionError(options.getResourceCompletionMode());
        }
    }

    private @NotNull OperationResult executeTest(TestProcedure testProcedure, OperationResult parentResult)
            throws ObjectNotFoundException {
        OperationResult testResult =
                parentResult.subresult(TestResourceOpNames.RESOURCE_TEST.getOperation())
                        .addParam("resource", resource)
                        .addArbitraryObjectAsParam("options", options)
                        .build();
        try {
            testProcedure.invoke(testResult);
            if (options.isFullMode()) {
                setResourceAvailabilityStatus(UP, "resource test was successful", testResult);
            }
        } catch (TestFailedException e) {
            // Operation result is already updated
            LOGGER.trace("Test has failed. Going to update the resource availability status.");
            setResourceAvailabilityStatus(e.status, e.statusChangeReason, testResult);
        } catch (Throwable t) {
            // Exceptions here are really unexpected. Any expected (e.g. connection-related) exceptions
            // should be treated within individual test methods.
            testResult.recordFatalError(t);
            throw t;
        } finally {
            testResult.close();
        }
        return testResult;
    }

    private void testAllConnectors(OperationResult result) throws TestFailedException {
        for (ConnectorSpec connectorSpec : allConnectorSpecs) {
            testConnector(connectorSpec, result);
        }
    }

    private void testMainConnector(OperationResult result) throws TestFailedException {
        testConnector(
                ConnectorSpec.main(resource),
                result);
    }

    private void testConnector(
            @NotNull ConnectorSpec connectorSpec,
            @NotNull OperationResult parentResult) throws TestFailedException {
        OperationResult result =
                parentResult.subresult(TestResourceOpNames.CONNECTOR_TEST.getOperation())
                        .addParam(OperationResult.PARAM_NAME, connectorSpec.getConnectorName())
                        .addParam(OperationResult.PARAM_OID, connectorSpec.getConnectorOid())
                        .addArbitraryObjectAsParam("mode", options.getTestMode())
                        .build();
        try {
            new TestConnectorOperation(connectorSpec)
                    .execute(result);
        } catch (Throwable t) {
            // This works even for TestFailedException
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }

        assert result.isAcceptable() : "Any non-acceptable result should have been handled by now";
    }

    private void testResourceSchema(OperationResult parentResult) throws TestFailedException {
        OperationResult result = parentResult.createSubresult(TestResourceOpNames.RESOURCE_SCHEMA.getOperation());
        try {
            fetchSchema(result);

            if (!NativeResourceSchema.isNullOrEmpty(nativeResourceSchema)) {
                resourceSchemaWasFetched = true;
            } else {
                // Resource does not support schema. If there is a static schema in resource definition this may still be OK.
                readStoredSchema(result);
            }

            checkSchemaAndUpdateConnectorInstances(result);

        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }

        // Should we check the result.isAcceptable() here? Probably not.
    }

    private void fetchSchema(OperationResult result) throws TestFailedException {
        try {
            nativeResourceSchema = beans.resourceManager.schemaFetcher.fetchResourceSchema(
                    resource, nativeConnectorsCapabilities, true, result);
        } catch (CommunicationException e) {
            onSchemaFetchProblem(e, "Communication error", DOWN, result);
        } catch (Throwable e) {
            onSchemaFetchProblem(e, "Error", BROKEN, result);
        }
    }

    private void onSchemaFetchProblem(Throwable t, String reason, AvailabilityStatusType status, OperationResult result)
            throws TestFailedException {
        throw TestFailedException.record(
                "Couldn't fetch schema. " + reason,
                operationDesc + " failed while fetching schema. " + reason,
                status, t, result);
    }

    private void readStoredSchema(OperationResult result) throws TestFailedException {
        try {
            nativeResourceSchema = ResourceSchemaFactory.getNativeSchema(resource);
        } catch (Exception e) {
            throw TestFailedException.record(
                    "Couldn't read stored schema",
                    operationDesc + " failed while reading stored schema",
                    BROKEN, e, result);
        }

        if (NativeResourceSchema.isNullOrEmpty(nativeResourceSchema)) {
            throw TestFailedException.record(
                    "Connector does not support schema and no static schema is available",
                    operationDesc + " failed: Connector does not support schema and no static schema is available",
                    BROKEN, null, result);
        }
    }

    /** Currently we simply check the schema by parsing it. Later we can extend this to more elaborate checks. */
    private void checkSchemaAndUpdateConnectorInstances(OperationResult result) throws TestFailedException {
        assert !NativeResourceSchema.isNullOrEmpty(nativeResourceSchema);
        try {
            ResourceSchemaFactory.parseCompleteSchema(resource, nativeResourceSchema);
            schemaHelper.updateSchemaInConnectorInstances(resource, nativeResourceSchema, result);
        } catch (Exception e) {
            throw TestFailedException.record(
                    "Couldn't process resource schema refinements",
                    operationDesc + " failed while processing schema refinements",
                    BROKEN, e, result);
        }
    }

    /** Stores the fresh schema and caps into the repository. */
    private void storeCapabilitiesAndSchema(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {

        if (!shouldUpdateInMemory() && !shouldUpdateRepository()) {
            return;
        }

        ResourceUpdater updater = new ResourceUpdater(
                resource,
                shouldUpdateRepository(),
                shouldUpdateInMemory(),
                beans);

        // This ensures that capabilities metadata are updated as well.
        for (ConnectorSpec connectorSpec : ConnectorSpec.all(resource)) {
            updater.updateNativeCapabilities(
                    connectorSpec,
                    nativeConnectorsCapabilities.get(connectorSpec.getConnectorName()));
        }

        if (resourceSchemaWasFetched) {
            updater.updateSchema(nativeResourceSchema);
        } else if (areSchemaCachingMetadataMissing() && NativeResourceSchema.isNotEmpty(nativeResourceSchema)) {
            updater.updateSchemaCachingMetadata();
        }

        updater.applyModifications(result);
    }

    private boolean areSchemaCachingMetadataMissing() {
        XmlSchemaType schema = resource.getSchema();
        return schema == null || schema.getCachingMetadata() == null;
    }

    /**
     * Tests a specific connector, in a partial or full mode.
     */
    private class TestConnectorOperation {

        @NotNull private final ConnectorSpec connectorSpec;
        @NotNull private final String desc;

        private ConfiguredConnectorInstanceEntry connectorCacheEntry;
        private ConnectorInstance connector;

        TestConnectorOperation(@NotNull ConnectorSpec connectorSpec) {
            this.connectorSpec = connectorSpec;
            this.desc = "testing connection using " + connectorSpec;
        }

        void execute(OperationResult result) throws TestFailedException {
            instantiateConnector(result);
            initializeConnector(result);
            testConnector(result);
            if (options.isFullMode()) {
                fetchConnectorCapabilities(result);
                if (!options.isDoNotCacheConnector()) {
                    cacheConfiguredConnector();
                }
            }
        }

        private void instantiateConnector(OperationResult parentResult) throws TestFailedException {
            OperationResult result = parentResult.createSubresult(TestResourceOpNames.CONNECTOR_INSTANTIATION.getOperation());

            try {
                // The returned entry is either configured one (from the cache) or unconfigured one (created anew on cache miss).
                connectorCacheEntry = beans.connectorManager.getOrCreateConnectorInstanceCacheEntry(connectorSpec, result);
                connector = connectorCacheEntry.getConnectorInstance();
            } catch (ObjectNotFoundException e) {
                // The connector or connector host or other necessary object was not found. The resource definition
                // is either wrong or the connector is not installed.
                if (ConnectorType.class.equals(e.getType())) {
                    onInstantiationProblem(e, "The connector was not found", result);
                } else {
                    onInstantiationProblem(e, "A required object was not found", result);
                }
            } catch (ConfigurationException e) {
                onInstantiationProblem(e, "Configuration error while dealing with the connector definition", result);
            } catch (SchemaException e) {
                onInstantiationProblem(e, "Schema error while dealing with the connector definition", result);
            } catch (Throwable t) {
                onInstantiationProblem(t, "Unexpected error", result);
            } finally {
                result.close();
            }

            // This was not in the original (4.5) code, but seems to be quite logical.
            stopIfResultNotOk(BROKEN, () -> " failed during connector instantiation", result);
        }

        private void onInstantiationProblem(Throwable t, String reason, OperationResult result) throws TestFailedException {
            throw TestFailedException.record(
                    "Connector instantiation failed. " + reason,
                    operationDesc + " failed during connector instantiation. " + reason,
                    BROKEN, t, result);
        }

        /** Configures and initializes the connector. (The latter applies only in the full mode!) */
        private void initializeConnector(OperationResult parentResult) throws TestFailedException {
            OperationResult result = parentResult.createSubresult(TestResourceOpNames.CONNECTOR_INITIALIZATION.getOperation());

            try {
                PrismObjectDefinition<ResourceType> resourceDefinition = resource.asPrismObject().getDefinition();
                PrismObjectDefinition<ResourceType> newResourceDefinition;
                if (resourceDefinition.isImmutable()) {
                    newResourceDefinition = resourceDefinition.clone();
                } else {
                    newResourceDefinition = resourceDefinition;
                }
                schemaHelper.applyConnectorSchemaToResource(connectorSpec, connectorSpec, newResourceDefinition, result);
                schemaHelper.evaluateExpressionsInConfigurationProperties(connectorSpec, resource, task, result);

                CommonBeans.get().connectorManager
                        .configureAndInitializeConnectorInstance(connector, connectorSpec, options.isFullMode(), false, result);

            } catch (CommunicationException e) {
                onConfigurationProblem(e, "Communication error", DOWN, result);
            } catch (GenericFrameworkException e) {
                onConfigurationProblem(e, "Generic error", BROKEN, result);
            } catch (SchemaException e) {
                onConfigurationProblem(e, "Schema error", BROKEN, result);
            } catch (ConfigurationException e) {
                onConfigurationProblem(e, "Configuration error", BROKEN, result);
            } catch (ObjectNotFoundException e) {
                onConfigurationProblem(e, "Required object not found", BROKEN, result);
            } catch (ExpressionEvaluationException e) {
                onConfigurationProblem(e, "Expression error", BROKEN, result);
            } catch (SecurityViolationException e) {
                onConfigurationProblem(e, "Security violation", BROKEN, result);
            } catch (Throwable t) {
                onConfigurationProblem(t, "Unexpected runtime error", BROKEN, result);
            } finally {
                result.close();
            }

            // This was not in the original (4.5) code, but seems to be quite logical.
            stopIfResultNotOk(BROKEN, () -> " failed during connector initialization", result);
        }

        private void onConfigurationProblem(Throwable t, String reason, AvailabilityStatusType status, OperationResult result)
                throws TestFailedException {
            throw TestFailedException.record(
                    "Connector initialization failed. " + reason,
                    operationDesc + " failed during connector initialization. " + reason,
                    status, t, result);
        }

        private void testConnector(OperationResult parentResult) throws TestFailedException {
            TestMode testMode = options.getTestMode();
            OperationResult result = parentResult
                    .subresult(TestResourceOpNames.CONNECTOR_CONNECTION.getOperation())
                    .addArbitraryObjectAsParam("mode", testMode)
                    .build();
            try {
                switch (testMode) {
                    case FULL:
                        connector.test(result);
                        break;
                    case PARTIAL:
                        connector.testPartialConfiguration(result);
                        break;
                    default:
                        throw new AssertionError(testMode);
                }
            } catch (Throwable t) {
                result.recordFatalError(t);
                LOGGER.debug("Test connector exception for  " + t.getMessage(), t);
                // Not re-throwing the exception (by the way, the exception is not expected here).
            } finally {
                result.close();
            }

            stopIfResultNotOk(DOWN, () -> " failed (mode: " + testMode + "): " + result.getMessage(), result);
        }

        private void fetchConnectorCapabilities(OperationResult parentResult) throws TestFailedException {
            OperationResult result = parentResult.createSubresult(TestResourceOpNames.CONNECTOR_CAPABILITIES.getOperation());
            try {
                InternalMonitor.recordCount(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);
                CapabilityCollectionType retrievedCapabilities = connector.fetchCapabilities(result);
                nativeConnectorsCapabilities.put(connectorSpec.getConnectorName(), retrievedCapabilities);
            } catch (CommunicationException e) {
                onCapabilitiesProblem(e, "Communication error", DOWN, result);
            } catch (GenericFrameworkException e) {
                onCapabilitiesProblem(e, "Generic error", BROKEN, result);
            } catch (ConfigurationException e) {
                onCapabilitiesProblem(e, "Configuration error", BROKEN, result);
            } catch (SchemaException e) {
                onCapabilitiesProblem(e, "Schema error", BROKEN, result);
            } catch (RuntimeException | Error e) {
                onCapabilitiesProblem(e, "Unexpected error", BROKEN, result);
            } finally {
                result.close();
            }

            stopIfResultNotOk(DOWN, () -> " failed while fetching capabilities", result);
        }

        private void onCapabilitiesProblem(Throwable t, String reason, AvailabilityStatusType status, OperationResult result)
                throws TestFailedException {
            throw TestFailedException.record(
                    "Capabilities fetching failed. " + reason,
                    operationDesc + " failed during fetching capabilities. " + reason,
                    status, t, result);
        }

        private void stopIfResultNotOk(AvailabilityStatusType status, Supplier<String> messageSupplier, OperationResult result)
                throws TestFailedException {
            if (!result.isAcceptable()) {
                throw new TestFailedException(result.getMessage(), status, desc + messageSupplier.get(), null);
            }
        }

        /**
         * Connector instance is fully configured and initialized at this point. But the connector cache entry may not be set up
         * properly and it is not yet placed into the cache. Therefore make sure the caching bit is completed.
         * Place the connector to cache even if it was configured at the beginning. The connector is reconfigured now.
         */
        private void cacheConfiguredConnector() {
            beans.connectorManager.cacheConfiguredAndInitializedConnectorInstance(connectorCacheEntry, connectorSpec);
        }
    }

    // TODO consider moving to ResourceUpdater (maybe!)
    private void setResourceAvailabilityStatus(
            AvailabilityStatusType status, String statusChangeReason, OperationResult result)
            throws ObjectNotFoundException {
        if (shouldUpdateRepository()) {
            beans.resourceManager.modifyResourceAvailabilityStatus(
                    resource.getOid(), status, statusChangeReason, task, result, false);
        }
        if (shouldUpdateInMemory()) {
            beans.resourceManager.modifyResourceAvailabilityStatus(resource, status, statusChangeReason);
        }
    }

    private boolean shouldUpdateInMemory() {
        return options.isUpdateInMemory();
    }

    private boolean shouldUpdateRepository() {
        return options.isUpdateInRepository()
                && isResourceInRepository();
    }

    private boolean isResourceInRepository() {
        return resource.getOid() != null;
    }

    private interface TestProcedure {
        void invoke(OperationResult testResult) throws ObjectNotFoundException, TestFailedException;
    }

    /**
     * Indicates a failure of a test step. Carries the information how to update resource status.
     *
     * BEWARE! Thrower is responsible for updating the local operation result.
     */
    private static class TestFailedException extends Exception {
        @NotNull private final AvailabilityStatusType status;
        @NotNull private final String statusChangeReason;

        private TestFailedException(
                @NotNull String message,
                @NotNull AvailabilityStatusType status,
                @NotNull String statusChangeReason,
                @Nullable Throwable cause) {
            super(message, cause);
            this.status = status;
            this.statusChangeReason = statusChangeReason;
        }

        /**
         * Convenience method: also updates the operation result.
         *
         * We provide a precise message here, so the {@link OperationResult#recordFatalError(Throwable)} calls
         * up the call stack will correctly set it into parent (or even current) {@link OperationResult} instances.
         */
        private static TestFailedException record(
                @NotNull String resultMessage,
                @NotNull String stateChangeMessage,
                @NotNull AvailabilityStatusType status,
                @Nullable Throwable cause,
                @NotNull OperationResult result) {
            if (cause != null) {
                resultMessage += ": " + cause.getMessage();
                stateChangeMessage += ": " + cause.getMessage();
            }
            result.recordFatalError(resultMessage, cause);
            return new TestFailedException(resultMessage, status, stateChangeMessage, cause);
        }
    }
}
