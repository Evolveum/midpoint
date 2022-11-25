/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

/**
 * Manages connectors of given resource - e.g. selects appropriate one based on required capability.
 */
@Component
class ResourceConnectorsManager {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceExpansionOperation.class);

    @Autowired private ResourceCapabilitiesHelper capabilitiesHelper;

    /**
     * Selects the connector that provides given capability.
     * Utilizes (optional) pre-fetched map of native capabilities.
     *
     * If no connector supports given capability, this method returns `null`.
     *
     * An exception to this rule is the situation when the native capabilities of the main connector are not known.
     * This may mean that the resource is unreachable. We will try return the main connector in this case, to give midPoint
     * a chance to try the operation. See e.g. `TestOpenDjNegative.test195SynchronizeAllClasses`.
     */
    @Nullable ConnectorSpec selectConnector(
            @NotNull ResourceType resource,
            @Nullable NativeConnectorsCapabilities nativeConnectorsCapabilities,
            @SuppressWarnings("SameParameterValue") @NotNull Class<? extends CapabilityType> capabilityClass)
            throws ConfigurationException {
        for (ConnectorSpec connectorSpec : ConnectorSpec.all(resource)) {
            if (capabilitiesHelper.supportsCapability(connectorSpec, nativeConnectorsCapabilities, capabilityClass)) {
                return connectorSpec;
            }
        }
        if (isMainConnectorCapabilityUnknown(resource, nativeConnectorsCapabilities, capabilityClass)) {
            LOGGER.trace("Support of {} by main connector in {} is not known, let us give it a try",
                    capabilityClass.getSimpleName(), resource);
            return ConnectorSpec.main(resource);
        } else {
            LOGGER.trace("Capability {} is not supported by {}", capabilityClass.getSimpleName(), resource);
            return null;
        }
    }

    private boolean isMainConnectorCapabilityUnknown(
            @NotNull ResourceType resource,
            @Nullable NativeConnectorsCapabilities nativeConnectorsCapabilities,
            @NotNull Class<? extends CapabilityType> capabilityClass) {
        if (nativeConnectorsCapabilities != null) {
            if (nativeConnectorsCapabilities.get(null) != null) {
                LOGGER.trace("Native main connector capabilities present (in a map)");
                return false;
            }
        } else {
            if (ResourceTypeUtil.hasCapabilitiesCached(resource)) {
                LOGGER.trace("Main connector capabilities cached (in resource)");
                return false;
            }
        }

        if (CapabilityUtil.getCapability(resource.getCapabilities(), capabilityClass) != null) {
            LOGGER.trace("Main connector capability {} is explicitly configured", capabilityClass.getSimpleName());
            return false;
        }

        return true;
    }

    /**
     * Convenience variant of {@link #selectConnector(ResourceType, NativeConnectorsCapabilities, Class)}.
     */
    @SuppressWarnings("unused") // Remove if not used for a longer time
    @Nullable private <T extends CapabilityType> ConnectorSpec selectConnector(
            @NotNull PrismObject<ResourceType> resource,
            @NotNull Class<T> capabilityClass) throws ConfigurationException {
        return selectConnector(resource.asObjectable(), null, capabilityClass);
    }

    /**
     * Convenience variant of {@link #selectConnector(ResourceType, NativeConnectorsCapabilities, Class)}.
     *
     * Throws an exception if no matching connector is found.
     */
    @NotNull <T extends CapabilityType> ConnectorSpec selectConnectorRequired(
            @NotNull ResourceType resource,
            @NotNull Class<T> capabilityClass) throws ConfigurationException {
        return MiscUtil.requireNonNull(
                selectConnector(resource, null, capabilityClass),
                () -> new UnsupportedOperationException(
                        "No connector supporting " + capabilityClass.getSimpleName() + " found in " + resource));
    }
}
