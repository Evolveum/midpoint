/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import static com.evolveum.midpoint.prism.schema.PrismSchema.isNotEmpty;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Responsible for "completing" a resource object, i.e. transforming the raw value fetched from the repository
 * into fully operational version - resolving super-resources, fetching schema and capabilities, and so on.
 *
 * *Limited use*
 *
 * Should be used only to "complete" resources before they are put into {@link ResourceCache}.
 * (Originally this functionality was executed as part of "test resource" operation, but it's no longer the case.)
 */
class ResourceCompletionOperation {

    private static final String OP_COMPLETE_RESOURCE = ResourceCompletionOperation.class.getName() + ".completeResource";

    private static final Trace LOGGER = TraceManager.getTrace(ResourceCompletionOperation.class);

    /**
     * The resource being completed. Must be mutable. This object is used throughout the operation,
     * except for final stages when the reloaded one is used instead.
     */
    @NotNull private final ResourceType resource;

    /** Root options used in the request to obtain the resource definition. We look e.g. after `noFetch` here. */
    @Nullable private final GetOperationOptions options;

    /** Resource schema. May be provided by the client. Updated by the operation. */
    private ResourceSchema rawResourceSchema;

    @NotNull private final Task task;

    @NotNull private final CommonBeans beans;

    @NotNull private final ResourceSchemaHelper schemaHelper;
    @NotNull private final SchemaFetcher schemaFetcher;

    @Nullable private ResourceExpansionOperation lastExpansionOperation;

    /**
     * Operation result for the operation itself. It is quite unusual to store the operation result
     * like this, but we need it to provide the overall success/failure of the operation.
     */
    private OperationResult result;

    /**
     * @param resource May be mutable or immutable. Must NOT be expanded. We do expansion ourselves to know ancestors OIDs.
     */
    ResourceCompletionOperation(
            @NotNull ResourceType resource,
            @Nullable GetOperationOptions options,
            @NotNull Task task,
            @NotNull CommonBeans beans) {
        argCheck(resource.getOid() != null, "OID of %s is null", resource);
        this.resource = resource.asPrismObject().cloneIfImmutable().asObjectable();
        this.options = options;
        this.task = task;
        this.beans = beans;
        this.schemaHelper = beans.resourceManager.schemaHelper;
        this.schemaFetcher = beans.resourceManager.schemaFetcher;
    }

    /**
     * TODO review/update this javadoc
     *
     * Make sure that the resource is complete.
     *
     * It will check if the resource has a sufficiently fresh schema, etc.
     *
     * Returned resource may be the same or may be a different instance, but it
     * is guaranteed that it will be "fresher" and will correspond to the
     * repository state (assuming that the provided resource also corresponded
     * to the repository state).
     *
     * The connector schema that was fetched before can be supplied to this
     * method. This is just an optimization. It comes handy e.g. in test
     * connection case.
     *
     * @return completed resource
     */
    public @NotNull ResourceType execute(OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, ConfigurationException {

        result = parentResult.createMinorSubresult(OP_COMPLETE_RESOURCE);
        try {
            expand(resource);
            applyConnectorSchemaAndExpressions();

            ResourceType completed;
            if (!ResourceTypeUtil.isComplete(resource)) {
                completed = complete();
            } else {
                completed = resource;
            }

            parseSchema(completed);
            return completed;
        } catch (StopException e) {
            LOGGER.trace("Completion operation was stopped");
            return resource;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * Expands the resource by resolving super-resource references.
     */
    private void expand(@NotNull ResourceType resource)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        if (resource.getSuper() != null) {
            lastExpansionOperation = new ResourceExpansionOperation(resource, beans);
            lastExpansionOperation.execute(result);
        } else {
            // We spare some CPU cycles by not instantiating the expansion operation object.
        }
    }

    private void applyConnectorSchemaAndExpressions() throws StopException {
        try {
            schemaHelper.applyConnectorSchemasToExpandedResource(resource, result);
            schemaHelper.evaluateExpressionsInConfigurationProperties(resource, task, result);
        } catch (Throwable t) {
            String message =
                    "An error occurred while applying connector schema and expressions to connector configuration of "
                            + resource + ": " + t.getMessage();
            result.recordPartialError(message, t); // Maybe fatal is more appropriate
            LOGGER.warn(message, t);
            throw new StopException();
        }
    }

    private @NotNull ResourceType complete()
            throws StopException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            ConfigurationException {

        LOGGER.trace("The resource is NOT complete. Trying to fetch schema and capabilities.");

        if (GetOperationOptions.isNoFetch(options)) {
            LOGGER.trace("We need to fetch schema, but the noFetch option is specified. Therefore returning whatever we have.");
            throw new StopException();
        }

        try {
            new RealCompletion().execute();
        } catch (Throwable t) {
            // Catch the exceptions. There are not critical. We need to catch them all because the connector may
            // throw even undocumented runtime exceptions.
            // Even non-complete resource may still be usable. The fetchResult indicates that there was an error
            result.recordPartialError("Cannot complete resource schema and capabilities: " + t.getMessage(), t);
            throw new StopException();
        }

        // Now we need to re-read the resource from the repository and re-apply the schemas. This ensures that we will
        // cache the correct version and that we avoid race conditions, etc.
        ResourceType reloaded =
                beans.resourceManager
                        .readResourceFromRepository(resource.getOid(), result);

        expand(reloaded);

        // Schema is in some cases applied (when expanding).
        schemaHelper.applyConnectorSchemasToExpandedResource(reloaded, result);
        schemaHelper.evaluateExpressionsInConfigurationProperties(reloaded, task, result);

        LOGGER.trace("Completed resource after reload:\n{}", reloaded.debugDumpLazily(1));

        return reloaded;
    }

    private void parseSchema(ResourceType completed) {
        try {
            // Make sure the schema is parseable. We are going to cache the resource, so we want to cache it
            // with the parsed schemas.
            ResourceSchemaFactory.getRawSchema(completed);
            ResourceSchema completeSchema = ResourceSchemaFactory.getCompleteSchema(completed);
            LOGGER.trace("Complete schema:\n{}", DebugUtil.debugDumpLazily(completeSchema, 1));
        } catch (Throwable e) {
            String message = "Error while processing schemaHandling section of " + completed + ": " + e.getMessage();
            result.recordPartialError(message, e);
            LOGGER.warn(message, e);
        }
    }

    OperationResultStatus getOperationResultStatus() {
        return result.getStatus();
    }

    /** Returns OIDs of objects that are ancestors to the current resource. Used e.g. for cache invalidation. */
    @NotNull Collection<String> getAncestorsOids() {
        return lastExpansionOperation != null ?
                lastExpansionOperation.getAncestorsOids() : List.of();
    }

    /**
     * This is the actual completion of non-complete resource.
     */
    private class RealCompletion {

        private final ResourceUpdater resourceUpdater =
                new ResourceUpdater(resource, true, false, beans);

        /** Here we store all we know about the connectors: mix of stored and fetched capabilities. */
        private final NativeConnectorsCapabilities nativeConnectorsCapabilities = NativeConnectorsCapabilities.empty();

        private void execute()
                throws SchemaException, CommunicationException, ObjectNotFoundException, GenericFrameworkException,
                ConfigurationException {

            // Capabilities
            // we need to process capabilities first. Schema is one of the connector capabilities.
            // We need to determine this capability to select the right connector for schema retrieval.
            completeCapabilities();

            completeSchema();

            resourceUpdater.applyModifications(result);
        }

        private void completeCapabilities()
                throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
            for (ConnectorSpec connectorSpec : ConnectorSpec.all(resource)) {
                completeConnectorCapabilities(connectorSpec);
            }
        }

        /**
         * Fetches the connector capabilities (if needed) and prepares appropriate deltas to update the resource object.
         */
        private void completeConnectorCapabilities(
                @NotNull ConnectorSpec connectorSpec)
                throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
            CapabilitiesType capabilitiesBean = connectorSpec.getCapabilities();
            CapabilityCollectionType nativeCapabilities;
            if (capabilitiesBean != null
                    && capabilitiesBean.getNative() != null
                    && !capabilitiesBean.getNative().asPrismContainerValue().hasNoItems()) { // FIXME empty means fetched! so we should use them
                nativeCapabilities = useCachedCapabilities(connectorSpec, capabilitiesBean);
            } else {
                nativeCapabilities = fetchAndStoreCapabilities(connectorSpec);
            }
            nativeConnectorsCapabilities.put(
                    connectorSpec.getConnectorName(),
                    nativeCapabilities);
        }

        /** Handles the situation where we want to use existing (cached) capabilities. */
        private CapabilityCollectionType useCachedCapabilities(
                @NotNull ConnectorSpec connectorSpec,
                @NotNull CapabilitiesType existingCapabilitiesBean) throws SchemaException {
            LOGGER.trace("Using capabilities that are cached in the resource object; for {}", connectorSpec);
            if (existingCapabilitiesBean.getCachingMetadata() == null) {
                LOGGER.trace("No caching metadata present, creating them");
                resourceUpdater.updateCapabilitiesCachingMetadata(connectorSpec);
            }
            return existingCapabilitiesBean.getNative();
        }

        private CapabilityCollectionType fetchAndStoreCapabilities(
                @NotNull ConnectorSpec connectorSpec)
                throws CommunicationException, ConfigurationException, SchemaException, ObjectNotFoundException {
            LOGGER.trace("No native capabilities cached in the resource object -> fetching them; for {}", connectorSpec);
            CapabilityCollectionType fetchedNativeCapabilities;
            try {
                InternalMonitor.recordCount(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);
                fetchedNativeCapabilities = beans.connectorManager
                        .getConfiguredAndInitializedConnectorInstance(connectorSpec, false, result)
                        .fetchCapabilities(result);
            } catch (GenericFrameworkException e) {
                throw new GenericConnectorException("Couldn't fetch capabilities because of a generic error in connector "
                        + connectorSpec + ": " + e.getMessage(), e);
            }

            resourceUpdater.updateNativeCapabilities(connectorSpec, fetchedNativeCapabilities);
            return fetchedNativeCapabilities;
        }

        private void completeSchema()
                throws CommunicationException, GenericFrameworkException, ConfigurationException, ObjectNotFoundException,
                SchemaException {

            // Try to get existing schema. We do not want to override this if it exists
            rawResourceSchema = ResourceSchemaFactory.getRawSchema(resource);
            if (isNotEmpty(rawResourceSchema)) {
                useExistingSchema();
            } else {
                fetchAndStoreSchema();
            }
        }

        private void fetchAndStoreSchema()
                throws CommunicationException, GenericFrameworkException, ConfigurationException, ObjectNotFoundException,
                SchemaException {
            fetchSchema();
            if (isNotEmpty(rawResourceSchema)) {
                adjustSchema();
                resourceUpdater.updateSchema(rawResourceSchema);
                resourceUpdater.markResourceUp();
            }
        }

        private void useExistingSchema() throws SchemaException {
            CachingMetadataType schemaCachingMetadata = getCurrentCachingMetadata();
            if (schemaCachingMetadata == null) {
                resourceUpdater.updateSchemaCachingMetadata();
            }
        }

        private CachingMetadataType getCurrentCachingMetadata() {
            XmlSchemaType schema = resource.getSchema();
            return schema != null ? schema.getCachingMetadata() : null;
        }

        private void fetchSchema()
                throws CommunicationException, GenericFrameworkException, ConfigurationException, ObjectNotFoundException,
                SchemaException {
            LOGGER.trace("Fetching resource schema for {}", resource);
            rawResourceSchema = schemaFetcher.fetchResourceSchema(resource, nativeConnectorsCapabilities, result);
            if (rawResourceSchema == null) {
                LOGGER.warn("No resource schema fetched from {}", resource);
            } else if (rawResourceSchema.isEmpty()) {
                LOGGER.warn("Empty resource schema fetched from {}", resource);
            } else {
                LOGGER.debug("Fetched resource schema for {}: {} definitions",
                        resource, rawResourceSchema.getDefinitions().size());
            }
        }

        private void adjustSchema() {
            rawResourceSchema =
                    new ResourceSchemaAdjuster(resource, rawResourceSchema)
                            .adjustSchema();
        }
    }

    /** Stopping the evaluation, and returning the {@link #resource}. */
    private static class StopException extends Exception {
    }
}
