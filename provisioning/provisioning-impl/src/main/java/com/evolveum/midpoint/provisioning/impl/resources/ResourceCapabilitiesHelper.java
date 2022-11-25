/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import static com.evolveum.midpoint.schema.CapabilityUtil.isCapabilityEnabled;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorInstanceSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

import java.util.Arrays;

/**
 * Helps {@link ResourceManager} with managing capabilities. (A Spring bean for now.)
 *
 * To be used only from the local package only. All external access should be through {@link ResourceManager}.
 *
 * TODO this class has shrunken a bit; reconsider its fate!
 */
@Component
class ResourceCapabilitiesHelper {

    /**
     * Gets a specific capability from resource/connectors/object-class.
     *
     * Notes:
     *
     * - Resource vs connector: The capability from specific connector is used only if it's enabled.
     */
    <T extends CapabilityType> T getCapability(
            @NotNull ResourceType resource,
            @Nullable ResourceObjectDefinition objectDefinition,
            @NotNull Class<T> capabilityClass) {

        if (objectDefinition instanceof ResourceObjectTypeDefinition) {
            // TODO allow configured capabilities also for refined object classes
            T inType = ((ResourceObjectTypeDefinition) objectDefinition).getConfiguredCapability(capabilityClass);
            if (inType != null) {
                return inType;
            }
        }

        for (ConnectorInstanceSpecificationType additionalConnectorBean : resource.getAdditionalConnector()) {
            T inConnector = getEnabledCapability(additionalConnectorBean, capabilityClass);
            if (inConnector != null) {
                return inConnector;
            }

        }

        return CapabilityUtil.getCapability(resource.getCapabilities(), capabilityClass);
    }

    /**
     * Returns the additional connector capability - but only if it's enabled.
     */
    private <T extends CapabilityType> T getEnabledCapability(
            @NotNull ConnectorInstanceSpecificationType additionalConnectorSpecBean,
            @NotNull Class<T> capabilityClass) {
        T capability = CapabilityUtil.getCapability(additionalConnectorSpecBean.getCapabilities(), capabilityClass);
        return isCapabilityEnabled(capability) ? capability : null;
    }

    /**
     * Returns `true` if the connector supports given capability.
     *
     * Uses this order:
     *
     * 1. configured capabilities
     * 2. fresh native capabilities (if present) _or_ stored native capabilities (if fresh ones are not present)
     */
    boolean supportsCapability(
            @NotNull ConnectorSpec connectorSpec,
            @Nullable NativeConnectorsCapabilities nativeConnectorsCapabilities,
            @NotNull Class<? extends CapabilityType> capabilityClass) {
        CapabilitiesType storedCaps = connectorSpec.getCapabilities();
        CapabilityCollectionType configuredCaps = storedCaps != null ? storedCaps.getConfigured() : null;
        CapabilityCollectionType nativeCapsToUse =
                nativeConnectorsCapabilities != null ?
                        nativeConnectorsCapabilities.get(connectorSpec.getConnectorName()) :
                        storedCaps != null ? storedCaps.getNative() : null;
        CapabilityType matchingCapability = CapabilityUtil.getCapability(
                Arrays.asList(configuredCaps, nativeCapsToUse), // avoiding List.of(...) because of nullable values
                capabilityClass);
        return isCapabilityEnabled(matchingCapability);
    }
}
