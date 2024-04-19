/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

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
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SchemaCapabilityType;

/**
 * Fetches the schema from the "real" resource.
 *
 * (A helper class for completion and test operations.)
 */
@Component
public class SchemaFetcher {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaFetcher.class);

    @Autowired private ConnectorManager connectorManager;
    @Autowired private ResourceConnectorsManager resourceConnectorsManager;

    /**
     * TODO
     *
     * @param capabilityMap Known native capabilities of the connectors (keyed by connector local name).
     * Used to select the schema-aware connector.
     */
    @Nullable NativeResourceSchema fetchResourceSchema(
            @NotNull ResourceType resource,
            @Nullable NativeConnectorsCapabilities capabilityMap,
            @NotNull OperationResult parentResult)
            throws CommunicationException, GenericFrameworkException, ConfigurationException, ObjectNotFoundException,
            SchemaException {
        ConnectorSpec connectorSpec =
                resourceConnectorsManager.selectConnector(resource, capabilityMap, SchemaCapabilityType.class);
        if (connectorSpec == null) {
            LOGGER.debug("No connector has schema capability, cannot fetch resource schema");
            return null;
        }
        InternalMonitor.recordCount(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
        ConnectorInstance connectorInstance = connectorManager.getConfiguredAndInitializedConnectorInstance(connectorSpec, false, parentResult);

        LOGGER.debug("Trying to get schema from {}", connectorSpec);
        var nativeResourceSchema = connectorInstance.fetchResourceSchema(parentResult);

//        if (nativeResourceSchema != null && ResourceTypeUtil.isValidateSchema(resource)) {
//            nativeResourceSchema.validate(); // FIXME
//        }
        return nativeResourceSchema;
    }
}
