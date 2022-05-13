/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import static com.evolveum.midpoint.schema.CapabilityUtil.isCapabilityEnabled;

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
            @Nullable ResourceObjectTypeDefinition objectTypeDefinition,
            @NotNull Class<T> capabilityClass) {

        if (objectTypeDefinition != null) {
            T inType = objectTypeDefinition.getConfiguredCapability(capabilityClass);
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
     * Returns `true` if the additional connector supports given capability.
     *
     * Looks only at capabilities stored in the connector spec bean.
     */
    boolean supportsCapability(
            @NotNull ConnectorInstanceSpecificationType additionalConnectorSpecBean,
            @NotNull Class<? extends CapabilityType> capabilityClass) {
        return getEnabledCapability(additionalConnectorSpecBean, capabilityClass) != null;
    }

    /**
     * Returns `true` if the additional connector supports given capability.
     *
     * Looks first at configured capabilities (from bean), and then at provided (fresh) native capabilities.
     */
    boolean supportsCapability(
            ConnectorInstanceSpecificationType additionalConnectorBean,
            NativeConnectorsCapabilities nativeConnectorsCapabilities,
            Class<? extends CapabilityType> capabilityClass) {
        CapabilitiesType connectorCapabilitiesBean = additionalConnectorBean.getCapabilities();
        if (connectorCapabilitiesBean != null) {
            CapabilityCollectionType configuredCapCollectionType = connectorCapabilitiesBean.getConfigured();
            if (configuredCapCollectionType != null) {
                CapabilityType configuredCap = CapabilityUtil.getCapability(configuredCapCollectionType, capabilityClass);
                if (configuredCap != null && !isCapabilityEnabled(configuredCap)) {
                    return false;
                }
            }
        }
        return isCapabilityEnabled(
                CapabilityUtil.getCapability(
                        nativeConnectorsCapabilities.get(additionalConnectorBean.getName()),
                        capabilityClass));
    }
}
