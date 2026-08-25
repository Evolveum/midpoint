/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.NativeResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SchemaCapabilityType;

/**
 * Fetches the schema from the "real" resource.
 *
 * (A helper class for completion and test operations, and some other uses.)
 */
@Component
public class SchemaFetcher {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaFetcher.class);

    @Autowired private ConnectorManager connectorManager;
    @Autowired private ResourceConnectorsManager resourceConnectorsManager;

    /**
     * Fetches the schema right by calling appropriate {@link ConnectorInstance} method.
     *
     * @param capabilityMap Known native capabilities of the connectors (keyed by connector local name).
     * Used to select the schema-aware connector. If not present, there are heuristics to help us (using configured ones,
     * or just trying the main connector).
     *
     * @param productionMode If {@code false}, we won't cache the connector instance to avoid breaking the production
     * environment.
     */
    @Nullable NativeResourceSchema fetchResourceSchema(
            @NotNull ResourceType resource,
            @Nullable NativeConnectorsCapabilities capabilityMap,
            boolean productionMode,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, ObjectNotFoundException,
            SchemaException, SubscriptionComplianceException {
        return fetchResourceSchema(resource, capabilityMap, productionMode, false, result);
    }

    /**
     * @param forceFresh If {@code true}, the connector instance is forced to retrieve the schema from the resource,
     * instead of returning a previously fetched (possibly stale) copy. Use this when the retrieved schema is going
     * to be stored in the resource - e.g. after the stored schema was deleted and a fresh one is being fetched.
     */
    @Nullable NativeResourceSchema fetchResourceSchema(
            @NotNull ResourceType resource,
            @Nullable NativeConnectorsCapabilities capabilityMap,
            boolean productionMode,
            boolean forceFresh,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, ObjectNotFoundException,
            SchemaException, SubscriptionComplianceException {
        var connectorSpec = resourceConnectorsManager.selectConnector(resource, capabilityMap, SchemaCapabilityType.class);
        if (connectorSpec == null) {
            LOGGER.debug("No connector has schema capability, cannot fetch resource schema");
            return null;
        }
        InternalMonitor.recordCount(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
        var connectorInstance =
                connectorManager.getConfiguredAndInitializedConnectorInstance(
                        connectorSpec, false, productionMode, result);

        LOGGER.debug("Trying to get schema from {}", connectorSpec);
        return connectorInstance.fetchResourceSchema(forceFresh, result);
    }
}
