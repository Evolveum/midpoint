/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorInstanceSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;

/**
 * Manages connectors of given resource - e.g. selects appropriate one based on required capability.
 */
@Component
class ResourceConnectorsManager {

    @Autowired private ResourceCapabilitiesHelper capabilitiesHelper;

    /**
     * Returns the default connector plus all additional ones.
     */
    List<ConnectorSpec> getAllConnectorSpecs(@NotNull PrismObject<ResourceType> resource)
            throws SchemaException, ConfigurationException {
        List<ConnectorSpec> connectorSpecs = new ArrayList<>();
        connectorSpecs.add(createDefaultConnectorSpec(resource));
        for (ConnectorInstanceSpecificationType additionalConnector : resource.asObjectable().getAdditionalConnector()) {
            connectorSpecs.add(createConnectorSpec(resource, additionalConnector));
        }
        return connectorSpecs;
    }

    /**
     * Selects the connector that provides given capability.
     * Utilizes (optional) pre-fetched map of native capabilities.
     *
     * TODO make this nullable
     */
    @NotNull ConnectorSpec selectConnector(
            @NotNull PrismObject<ResourceType> resource,
            @Nullable NativeConnectorsCapabilities capabilityMap,
            @SuppressWarnings("SameParameterValue") @NotNull Class<? extends CapabilityType> capabilityClass)
            throws ConfigurationException {
        if (capabilityMap != null) {
            return selectConnectorUsingExplicitCapabilities(resource, capabilityMap, capabilityClass);
        } else {
            return selectConnector(resource, capabilityClass);
        }
    }

    /**
     * Selects connector that provides given capability, based on the provided capabilities. Default: main.
     *
     * TODO remove default, make this nullable
     */
    private @NotNull ConnectorSpec selectConnectorUsingExplicitCapabilities(
            @NotNull PrismObject<ResourceType> resource,
            @NotNull NativeConnectorsCapabilities nativeConnectorsCapabilities,
            @NotNull Class<? extends CapabilityType> capabilityClass) throws ConfigurationException {
        for (ConnectorInstanceSpecificationType additionalConnectorBean : resource.asObjectable().getAdditionalConnector()) {
            if (capabilitiesHelper.supportsCapability(additionalConnectorBean, nativeConnectorsCapabilities, capabilityClass)) {
                return createConnectorSpec(resource, additionalConnectorBean);
            }
        }
        return createDefaultConnectorSpec(resource);
    }

    /**
     * Selects connector that provides given capability, based on the information in the resource object. Default: main.
     *
     * TODO remove default, make this nullable
     */
    @NotNull <T extends CapabilityType> ConnectorSpec selectConnector(
            @NotNull PrismObject<ResourceType> resource,
            @NotNull Class<T> capabilityClass) throws ConfigurationException {
        for (ConnectorInstanceSpecificationType additionalConnectorBean : resource.asObjectable().getAdditionalConnector()) {
            if (capabilitiesHelper.supportsCapability(additionalConnectorBean, capabilityClass)) {
                return createConnectorSpec(resource, additionalConnectorBean);
            }
        }
        return createDefaultConnectorSpec(resource);
    }

    /** Creates the default ("main") connector specification. */
    @NotNull ConnectorSpec createDefaultConnectorSpec(@NotNull PrismObject<ResourceType> resource) {
        return new ConnectorSpec(
                resource,
                null,
                ResourceTypeUtil.getConnectorOid(resource),
                resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION));
    }

    /** Creates the connector specification for given additional connector. */
    @NotNull ConnectorSpec createConnectorSpec(
            @NotNull PrismObject<ResourceType> resource,
            @NotNull ConnectorInstanceSpecificationType additionalConnectorSpecBean)
            throws ConfigurationException {
        String connectorName = additionalConnectorSpecBean.getName();
        configCheck(StringUtils.isNotBlank(connectorName), "No connector name in additional connector in %s", resource);

        // connector OID is not required here, as it may come from the super-resource
        String connectorOid = getConnectorOid(additionalConnectorSpecBean);

        //noinspection unchecked
        PrismContainer<ConnectorConfigurationType> connectorConfiguration =
                additionalConnectorSpecBean.asPrismContainerValue().findContainer(
                        ConnectorInstanceSpecificationType.F_CONNECTOR_CONFIGURATION);

        return new ConnectorSpec(resource, connectorName, connectorOid, connectorConfiguration);
    }

    private String getConnectorOid(@NotNull ConnectorInstanceSpecificationType bean) {
        ObjectReferenceType ref = bean.getConnectorRef();
        return ref != null ? ref.getOid() : null;
    }
}
