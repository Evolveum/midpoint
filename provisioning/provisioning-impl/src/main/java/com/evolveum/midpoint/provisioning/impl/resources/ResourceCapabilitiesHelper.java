/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import java.util.Collection;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorInstanceSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

/**
 * Helps {@link ResourceManager} with managing capabilities. (A Spring bean for now.)
 *
 * To be used only from the local package only. All external access should be through {@link ResourceManager}.
 */
@Component
class ResourceCapabilitiesHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceCapabilitiesHelper.class);

    /**
     * Returns connector capabilities merged with capabilities defined at object type level.
     */
    public <T extends CapabilityType> CapabilitiesType getConnectorCapabilities(ResourceType resource,
            ResourceObjectTypeDefinition objectTypeDefinition, Class<T> operationCapabilityClass) {
        if (resource == null) {
            return null;
        }

        CapabilitiesType connectorCapabilities = null;
        for (ConnectorInstanceSpecificationType additionalConnectorType : resource.getAdditionalConnector()) {
            if (supportsCapability(additionalConnectorType, operationCapabilityClass)) {
                connectorCapabilities = additionalConnectorType.getCapabilities();
            }
        }

        if (connectorCapabilities == null) {
            connectorCapabilities = resource.getCapabilities();
        }

        CapabilitiesType finalCapabilities = applyObjectClassCapabilities(connectorCapabilities, objectTypeDefinition);
        LOGGER.trace("Returning final capabilities:\n{} ", finalCapabilities);
        return finalCapabilities;
    }

    <T extends CapabilityType> boolean supportsCapability(
            ConnectorInstanceSpecificationType additionalConnectorSpecBean, Class<T> capabilityClass) {
        return CapabilityUtil.isCapabilityEnabled(
                CapabilityUtil.getEffectiveCapability(
                        additionalConnectorSpecBean.getCapabilities(), capabilityClass));
    }

    <T extends CapabilityType> boolean supportsCapability(
            ConnectorInstanceSpecificationType additionalConnectorSpecBean,
            Collection<Object> nativeCapabilities,
            Class<T> capabilityClass) {
        CapabilitiesType specifiedCapabilitiesType = additionalConnectorSpecBean.getCapabilities();
        if (specifiedCapabilitiesType != null) {
            CapabilityCollectionType configuredCapCollectionType = specifiedCapabilitiesType.getConfigured();
            if (configuredCapCollectionType != null) {
                T configuredCap = CapabilityUtil.getCapability(configuredCapCollectionType.getAny(), capabilityClass);
                if (configuredCap != null && !CapabilityUtil.isCapabilityEnabled(configuredCap)) {
                    return false;
                }
            }
        }
        return CapabilityUtil.isCapabilityEnabled(
                CapabilityUtil.getCapability(nativeCapabilities, capabilityClass));
    }

    /**
     * Merges object class specific capabilities with capabilities defined at connector level.
     * The specific capabilities take precedence over the connector-level ones.
     * (A unit of comparison is the whole capability, identified by its root level element.)
     */
    private CapabilitiesType applyObjectClassCapabilities(CapabilitiesType connectorCapabilities,
            ResourceObjectTypeDefinition objectTypeDefinition) {

        if (objectTypeDefinition == null) {
            LOGGER.trace("No object type definition, skipping merge.");
            return connectorCapabilities;
        }

        CapabilitiesType objectTypeCapabilities = objectTypeDefinition.getConfiguredCapabilities();
        if (objectTypeCapabilities == null) {
            LOGGER.trace("No capabilities for {} specified, skipping merge.", objectTypeDefinition);
            return connectorCapabilities;
        }

        CapabilityCollectionType configuredObjectTypeCapabilities = objectTypeCapabilities.getConfigured();
        if (configuredObjectTypeCapabilities == null) {
            LOGGER.trace("No configured capabilities in {} specified, skipping merge", objectTypeDefinition);
            return connectorCapabilities;
        }

        CapabilitiesType finalCapabilities = new CapabilitiesType();
        if (connectorCapabilities.getNative() != null) {
            finalCapabilities.setNative(connectorCapabilities.getNative());
        }

        if (!hasConfiguredCapabilities(connectorCapabilities)) {
            LOGGER.trace("No configured capabilities found for connector, replacing with capabilities defined for {}",
                    objectTypeDefinition);
            finalCapabilities.setConfigured(configuredObjectTypeCapabilities);
            return finalCapabilities;
        }

        for (Object capability : connectorCapabilities.getConfigured().getAny()) {
            if (!CapabilityUtil.containsCapabilityWithSameElementName(configuredObjectTypeCapabilities.getAny(), capability)) {
                configuredObjectTypeCapabilities.getAny().add(capability);
            }
        }

        finalCapabilities.setConfigured(configuredObjectTypeCapabilities);
        return finalCapabilities;
    }

    private boolean hasConfiguredCapabilities(CapabilitiesType supportedCapabilities) {
        CapabilityCollectionType configured = supportedCapabilities.getConfigured();
        return configured != null && !configured.getAny().isEmpty();
    }
}
