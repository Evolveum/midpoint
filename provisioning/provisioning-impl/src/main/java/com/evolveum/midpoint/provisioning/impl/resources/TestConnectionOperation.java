/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible for testing the resource.
 *
 * To be used only from the local package only. All external access should be through {@link ResourceManager}.
 */
class TestConnectionOperation {

    private static final Trace LOGGER = TraceManager.getTrace(TestConnectionOperation.class);

    @NotNull private final PrismObject<ResourceType> resource;
    @NotNull private final Task task;
    @NotNull private final CommonBeans beans;

    TestConnectionOperation(@NotNull PrismObject<ResourceType> resource, @NotNull Task task, @NotNull CommonBeans beans) {
        this.resource = resource.cloneIfImmutable();
        this.task = task;
        this.beans = beans;
    }

    /**
     * Test the connection.
     *
     * @throws ObjectNotFoundException If the resource object cannot be found in repository (e.g. when trying to set its
     *                                 availability status).
     */
    public void execute(OperationResult result)
            throws ObjectNotFoundException {

        String resourceOid = resource.getOid();

        String operationDesc = "test resource " + resourceOid + " connection";

        List<ConnectorSpec> allConnectorSpecs;
        try {
            allConnectorSpecs = beans.resourceManager.getAllConnectorSpecs(resource);
        } catch (SchemaException | ConfigurationException e) {
            if (LOGGER.isTraceEnabled()) {
                // TODO why logging at error level only if trace is enabled?
                LOGGER.error("Configuration error: {}", e.getMessage(), e);
            }
            markResourceBroken(operationDesc + ", getting all connectors failed: " + e.getMessage(), result);
            result.recordFatalError("Configuration error: " + e.getMessage(), e);
            return;
        }

        Map<String, Collection<Object>> capabilityMap = new HashMap<>();
        for (ConnectorSpec connectorSpec: allConnectorSpecs) {

            OperationResult connectorTestResult = result
                    .createSubresult(ConnectorTestOperation.CONNECTOR_TEST.getOperation());
            connectorTestResult.addParam(OperationResult.PARAM_NAME, connectorSpec.getConnectorName());
            connectorTestResult.addParam(OperationResult.PARAM_OID, connectorSpec.getConnectorOid());

            testConnectionConnector(connectorSpec, capabilityMap, connectorTestResult);

            connectorTestResult.computeStatus();

            if (!connectorTestResult.isAcceptable()) {
                //nothing more to do.. if it failed while testing connection, status is set.
                // we do not need to continue and waste the time.
                return;
            }
        }

        // === test SCHEMA ===

        OperationResult schemaResult = result.createSubresult(ConnectorTestOperation.RESOURCE_SCHEMA.getOperation());

        ResourceSchema rawSchema;
        try {

            rawSchema = beans.resourceManager.fetchResourceSchema(resource, capabilityMap, schemaResult);

        } catch (CommunicationException e) {
            String statusChangeReason = operationDesc + " failed while fetching schema: " + e.getMessage();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.error("Communication error: {}", e.getMessage(), e);
            }
            markResourceDown(statusChangeReason, result);
            schemaResult.recordFatalError("Communication error: " + e.getMessage(), e);
            return;
        } catch (GenericFrameworkException | ConfigurationException | ObjectNotFoundException | SchemaException | RuntimeException e) {
            String statusChangeReason = operationDesc + " failed while fetching schema: " + e.getMessage();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.error("Error: {}", e.getMessage(), e);
            }
            markResourceBroken(statusChangeReason, result);
            schemaResult.recordFatalError("Error: " + e.getMessage(), e);
            return;
        }

        if (rawSchema == null || rawSchema.isEmpty()) {
            // Resource does not support schema
            // If there is a static schema in resource definition this may still be OK
            try {
                rawSchema = ResourceSchemaFactory.getRawSchema(resource);
            } catch (SchemaException e) {
                String statusChangeReason = operationDesc + " failed while parsing refined schema: " + e.getMessage();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.error("Error: {}", e.getMessage(), e);
                }
                markResourceBroken(statusChangeReason, result);
                schemaResult.recordFatalError(e);
                return;
            }

            if (rawSchema == null || rawSchema.isEmpty()) {
                String msg = "Connector does not support schema and no static schema available";
                String statusChangeReason = operationDesc + ". " + msg;
                markResourceBroken(statusChangeReason, result);
                schemaResult.recordFatalError(msg);
                return;
            }
        }

        // Invoke completeResource(). This will store the fetched schema to the ResourceType if there is no <schema>
        // definition already. Therefore the testResource() can be used to generate the resource schema - until we
        // have full schema caching capability.
        PrismObject<ResourceType> completedResource;
        try {
            // Re-fetching from repository to get up-to-date availability status (to avoid phantom state change records).
            PrismObject<ResourceType> repoResource = beans.cacheRepositoryService.getObject(
                    ResourceType.class, resourceOid, null, schemaResult);
            completedResource = new ResourceCompletionOperation(repoResource, null, rawSchema, true, capabilityMap, task, beans)
                    .execute(schemaResult);
        } catch (ObjectNotFoundException e) {
            String msg = "Object not found (unexpected error, probably a bug): " + e.getMessage();
            String statusChangeReason = operationDesc + " failed while completing resource. " + msg;
            markResourceBroken(statusChangeReason, result);
            schemaResult.recordFatalError(msg, e);
            return;
        } catch (ConfigurationException e) {
            String msg = "Configuration error: " + e.getMessage();
            String statusChangeReason = operationDesc + " failed while completing resource. " + msg;
            markResourceBroken(statusChangeReason, result);
            schemaResult.recordFatalError(msg, e);
            return;
        } catch (SchemaException e) {
            String msg = "Schema processing error (probably connector bug): " + e.getMessage();
            String statusChangeReason = operationDesc + " failed while completing resource. " + msg;
            markResourceBroken(statusChangeReason, result);
            schemaResult.recordFatalError(msg, e);
            return;
        } catch (ExpressionEvaluationException e) {
            String msg = "Expression error: " + e.getMessage();
            String statusChangeReason = operationDesc + " failed while completing resource. " + msg;
            markResourceBroken(statusChangeReason, result);
            schemaResult.recordFatalError(msg, e);
            return;
        } catch (RuntimeException e) {
            String msg = "Unspecified exception: " + e.getMessage();
            String statusChangeReason = operationDesc + " failed while completing resource. " + msg;
            markResourceBroken(statusChangeReason, result);
            schemaResult.recordFatalError(msg, e);
            return;
        }

        schemaResult.recordSuccess();

        try {
            updateResourceSchema(completedResource, allConnectorSpecs, result);
        } catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | RuntimeException e) {
            String statusChangeReason = operationDesc + " failed while updating resource schema: " + e.getMessage();
            markResourceBroken(statusChangeReason, result);
            result.recordFatalError("Couldn't update resource schema: " + e.getMessage(), e);
            //noinspection UnnecessaryReturnStatement
            return;
        }

        // TODO: connector sanity (e.g. refined schema, at least one account type, identifiers
        // in schema, etc.)

    }

    private void updateResourceSchema(
            PrismObject<ResourceType> resource, List<ConnectorSpec> allConnectorSpecs, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        ResourceSchema resourceSchema = ResourceSchemaFactory.getRawSchema(resource);
        if (resourceSchema != null) {
            for (ConnectorSpec connectorSpec : allConnectorSpecs) {
                ConnectorInstance instance = beans.connectorManager.getConfiguredConnectorInstance(
                        connectorSpec, false, result);
                instance.updateSchema(resourceSchema);
            }
        }
    }

    private void markResourceBroken(String statusChangeReason, OperationResult result) throws ObjectNotFoundException {
        beans.resourceManager.modifyResourceAvailabilityStatus(
                resource.getOid(), AvailabilityStatusType.BROKEN, statusChangeReason, task, result, true);
    }

    private void markResourceDown(String statusChangeReason, OperationResult result) throws ObjectNotFoundException {
        beans.resourceManager.modifyResourceAvailabilityStatus(
                resource.getOid(), AvailabilityStatusType.DOWN, statusChangeReason, task, result, true);
    }

    private void markResourceUp(String statusChangeReason, OperationResult result) throws ObjectNotFoundException {
        beans.resourceManager.modifyResourceAvailabilityStatus(
                resource.getOid(), AvailabilityStatusType.UP, statusChangeReason, task, result, false);
    }

    private void testConnectionConnector(
            ConnectorSpec connectorSpec,
            Map<String, Collection<Object>> capabilityMap,
            OperationResult parentResult) throws ObjectNotFoundException {

        // === test INITIALIZATION ===

        OperationResult initResult = parentResult
                .createSubresult(ConnectorTestOperation.CONNECTOR_INITIALIZATION.getOperation());

        LOGGER.debug("Testing connection using {}", connectorSpec);

        String operationCtx = "testing connection using " + connectorSpec;

        ConfiguredConnectorInstanceEntry connectorInstanceCacheEntry;
        try {
            // Make sure we are getting non-configured instance.
            connectorInstanceCacheEntry = beans.connectorManager.getOrCreateConnectorInstanceCacheEntry(connectorSpec, initResult);
            initResult.recordSuccess();
        } catch (ObjectNotFoundException e) {
            // The connector was not found. The resource definition is either
            // wrong or the connector is not installed.
            String msg = "The connector was not found: "+e.getMessage();
            operationCtx += " failed while getting connector instance. " + msg;
            markResourceBroken(operationCtx, parentResult);
            initResult.recordFatalError(msg, e);
            return;
        } catch (SchemaException e) {
            String msg = "Schema error while dealing with the connector definition: "+e.getMessage();
            operationCtx += " failed while getting connector instance. " + msg;
            markResourceBroken(operationCtx, parentResult);
            initResult.recordFatalError(msg, e);
            return;
        } catch (RuntimeException | Error e) {
            String msg = "Unexpected runtime error: "+e.getMessage();
            operationCtx += " failed while getting connector instance. " + msg;
            markResourceBroken(operationCtx, parentResult);
            initResult.recordFatalError(msg, e);
            return;
        }

        ConnectorInstance connector = connectorInstanceCacheEntry.getConnectorInstance();


        // === test CONFIGURATION ===

        OperationResult configResult = parentResult
                .createSubresult(ConnectorTestOperation.CONNECTOR_CONFIGURATION.getOperation());

        try {
            PrismObject<ResourceType> resource = connectorSpec.getResource();
            PrismObjectDefinition<ResourceType> newResourceDefinition = resource.getDefinition().clone();
            beans.resourceManager.applyConnectorSchemaToResource(connectorSpec, newResourceDefinition, resource, task, configResult);
            PrismContainer<ConnectorConfigurationType> connectorConfigurationContainer = connectorSpec.getConnectorConfiguration();
            PrismContainerValue<ConnectorConfigurationType> connectorConfiguration =
                    connectorConfigurationContainer != null ?
                            connectorConfigurationContainer.getValue() :
                            PrismContext.get().itemFactory().createContainerValue(); // TODO or should UCF accept null config PCV?

            InternalMonitor.recordCount(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT);

            connector.configure(connectorConfiguration, ResourceTypeUtil.getSchemaGenerationConstraints(resource), configResult);

            // We need to explicitly initialize the instance, e.g. in case that the schema and capabilities
            // cannot be detected by the connector and therefore are provided in the resource
            //
            // NOTE: the capabilities and schema that are used here are NOT necessarily those that are detected by the resource.
            //       The detected schema will come later. The schema here is the one that is stored in the resource
            //       definition (ResourceType). This may be schema that was detected previously. But it may also be a schema
            //       that was manually defined. This is needed to be passed to the connector in case that the connector
            //       cannot detect the schema and needs schema/capabilities definition to establish a connection.
            //       Most connectors will just ignore the schema and capabilities that are provided here.
            //       But some connectors may need it (e.g. CSV connector working with CSV file without a header).
            //
            ResourceSchema previousResourceSchema = ResourceSchemaFactory.getRawSchema(connectorSpec.getResource());
            Collection<Object> previousCapabilities = ResourceTypeUtil.getNativeCapabilitiesCollection(connectorSpec.getResource().asObjectable());
            connector.initialize(previousResourceSchema, previousCapabilities,
                    ResourceTypeUtil.isCaseIgnoreAttributeNames(connectorSpec.getResource().asObjectable()), configResult);

            configResult.recordSuccess();
        } catch (CommunicationException e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            markResourceDown(operationCtx, parentResult);
            configResult.recordFatalError("Communication error", e);
            return;
        } catch (GenericFrameworkException e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            markResourceBroken(operationCtx, parentResult);
            configResult.recordFatalError("Generic error", e);
            return;
        } catch (SchemaException e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            markResourceBroken(operationCtx, parentResult);
            configResult.recordFatalError("Schema error", e);
            return;
        } catch (ConfigurationException e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            markResourceBroken(operationCtx, parentResult);
            configResult.recordFatalError("Configuration error", e);
            return;
        } catch (ObjectNotFoundException e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            markResourceBroken(operationCtx, parentResult);
            configResult.recordFatalError("Object not found", e);
            return;
        } catch (ExpressionEvaluationException e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            markResourceBroken(operationCtx, parentResult);
            configResult.recordFatalError("Expression error", e);
            return;
        } catch (SecurityViolationException e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            markResourceBroken(operationCtx, parentResult);
            configResult.recordFatalError("Security violation", e);
            return;
        } catch (RuntimeException | Error e) {
            operationCtx += " failed while testing configuration: " + e.getMessage();
            markResourceBroken(operationCtx, parentResult);
            configResult.recordFatalError("Unexpected runtime error", e);
            return;
        }

        // === test CONNECTION ===

        // delegate the main part of the test to the connector
        connector.test(parentResult);

        parentResult.computeStatus();
        if (!parentResult.isAcceptable()) {
            operationCtx += ". Connector test failed: " + parentResult.getMessage();
            markResourceDown(operationCtx, parentResult);
            // No point in going on. Following tests will fail anyway, they will
            // just produce misleading
            // messages.
            return;
        } else {
            operationCtx += ". Connector test successful.";
            markResourceUp(operationCtx, parentResult);
        }

        // === test CAPABILITIES ===

        OperationResult capabilitiesResult = parentResult
                .createSubresult(ConnectorTestOperation.CONNECTOR_CAPABILITIES.getOperation());
        try {
            InternalMonitor.recordCount(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);
            Collection<Object> retrievedCapabilities = connector.fetchCapabilities(capabilitiesResult);

            capabilityMap.put(connectorSpec.getConnectorName(), retrievedCapabilities);
            capabilitiesResult.recordSuccess();
        } catch (CommunicationException e) {
            operationCtx += " failed while testing capabilities: " + e.getMessage();
            markResourceDown(operationCtx, parentResult);
            capabilitiesResult.recordFatalError("Communication error", e);
            return;
        } catch (GenericFrameworkException e) {
            operationCtx += " failed while testing capabilities: " + e.getMessage();
            markResourceBroken(operationCtx, parentResult);
            capabilitiesResult.recordFatalError("Generic error", e);
            return;
        } catch (ConfigurationException e) {
            operationCtx += " failed while testing capabilities: " + e.getMessage();
            markResourceBroken(operationCtx, parentResult);
            capabilitiesResult.recordFatalError("Configuration error", e);
            return;
        } catch (SchemaException e) {
            operationCtx += " failed while testing capabilities: " + e.getMessage();
            markResourceBroken(operationCtx, parentResult);
            capabilitiesResult.recordFatalError("Schema error", e);
            return;
        } catch (RuntimeException | Error e) {
            operationCtx += " failed while testing capabilities: " + e.getMessage();
            markResourceBroken(operationCtx, parentResult);
            capabilitiesResult.recordFatalError("Unexpected runtime error", e);
            return;
        }

        // Connector instance is fully configured at this point.
        // But the connector cache entry may not be set up properly and it is not yet placed into the cache.
        // Therefore make sure the caching bit is completed.
        // Place the connector to cache even if it was configured at the beginning. The connector is reconfigured now.
        beans.connectorManager.cacheConfiguredConnector(connectorInstanceCacheEntry, connectorSpec);
    }
}
