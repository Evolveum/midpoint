/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resources;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Reference to a connector specification in a given resource.
 *
 * Although originally (before 4.6) this was more-or-less a static structure holding connector OID and configuration,
 * since 4.6 it is more a pointer into the connector definition for given resource. Individual components are determined
 * "on the fly" from the current resource object.
 *
 * It does even support setting of selected properties in the resource.
 *
 * TODO consider changing the name
 *
 * @author semancik
 */
public abstract class ConnectorSpec {

    @NotNull protected final ResourceType resource;

    private ConnectorSpec(@NotNull ResourceType resource) {
        this.resource = resource;
    }

    /** Creates the spec for "main" connector (not using name of `default` as that is a Java keyword). */
    public static @NotNull ConnectorSpec main(@NotNull ResourceType resource) {
        return new Main(resource);
    }

    /** Creates the connector specification for given additional connector. */
    public static @NotNull ConnectorSpec additional(
            @NotNull ResourceType resource,
            @NotNull ConnectorInstanceSpecificationType additionalConnectorSpecBean)
            throws ConfigurationException {
        return new Additional(resource, additionalConnectorSpecBean);
    }

    /**
     * Returns the all connectors (default a.k.a. main and all additional ones).
     *
     * The order is that additional connectors go first. This is to maintain pre-4.6 behavior related to selecting
     * a connector that supports a particular capability. Originally, the additional connectors took precedence
     * before the main one. So we put them in this list first, to preserve this behavior.
     *
     * TODO is this correct behavior at all?
     */
    public static @NotNull List<ConnectorSpec> all(@NotNull ResourceType resource) throws ConfigurationException {
        List<ConnectorSpec> connectorSpecs = new ArrayList<>();
        for (ConnectorInstanceSpecificationType additionalConnector : resource.getAdditionalConnector()) {
            connectorSpecs.add(ConnectorSpec.additional(resource, additionalConnector));
        }
        connectorSpecs.add(ConnectorSpec.main(resource));
        return connectorSpecs;
    }

    public static @Nullable ConnectorSpec find(@NotNull ResourceType resource, @Nullable String name)
            throws ConfigurationException {
        if (name == null) {
            return ConnectorSpec.main(resource);
        } else {
            for (ConnectorInstanceSpecificationType additionalConnector : resource.getAdditionalConnector()) {
                if (name.equals(additionalConnector.getName())) {
                    return additional(resource, additionalConnector);
                }
            }
            return null;
        }
    }

    public @NotNull ResourceType getResource() {
        return resource;
    }

    /** Returns `null` for main connector, and non-`null` value for additional ones. */
    public abstract @Nullable String getConnectorName();

    public abstract boolean isMain();

    /**
     * Returns the "base path" under which other items, like `capabilities` or `connectorRef` can be found.
     *
     * @throws IllegalStateException if the path cannot be determined e.g. because the additional connector PCVs
     * do not have their PCV IDs assigned yet.
     */
    public abstract @NotNull ItemPath getBasePath();

    /**
     * Returns {@link ItemPath} to the capabilities container. This may be useful when updating the resource object
     * by creating a list of modifications.
     *
     * @see #getBasePath()
     */
    @NotNull ItemPath getCapabilitiesItemPath() {
        return getBasePath().append(ResourceType.F_CAPABILITIES);
    }

    /**
     * Returns {@link ItemPath} to the configuration container.
     *
     * @see #getBasePath()
     */
    @NotNull ItemPath getConfigurationItemPath() {
        return getBasePath().append(ResourceType.F_CONNECTOR_CONFIGURATION);
    }

    /**
     * Note that connector OID is not required here, as the resource may be not resolved yet, or it may be
     * an abstract resource with missing connectorRef.
     */
    public abstract @Nullable String getConnectorOid();

    /**
     * To be used when we are sure to deal with fully expanded, non-abstract resources.
     */
    @NotNull String getConnectorOidRequired() {
        return MiscUtil.requireNonNull(
                getConnectorOid(),
                () -> new IllegalStateException("Expected to have connector OID but there was none; in " + this));
    }

    public abstract @Nullable PrismContainer<ConnectorConfigurationType> getConnectorConfiguration();

    @NotNull ConfiguredConnectorCacheKey getCacheKey() {
        return new ConfiguredConnectorCacheKey(resource.getOid(), getConnectorName());
    }

    public abstract @Nullable CapabilitiesType getCapabilities();

    public @Nullable CapabilityCollectionType getNativeCapabilities() {
        CapabilitiesType capabilities = getCapabilities();
        return capabilities != null ? capabilities.getNative() : null;
    }

    /** Sets the connector capabilities in the resource in-memory object. */
    public abstract void setCapabilities(CapabilitiesType capabilities);

    /** Sets the connector capabilities caching metadata in the resource in-memory object. */
    public abstract void setCapabilitiesCachingMetadata(CachingMetadataType cachingMetadata);

    private static class Main extends ConnectorSpec {

        private Main(@NotNull ResourceType resource) {
            super(resource);
        }

        @Override
        public boolean isMain() {
            return true;
        }

        @Override
        @NotNull
        public ItemPath getBasePath() {
            return ItemPath.EMPTY_PATH;
        }

        @Override
        public @Nullable String getConnectorName() {
            return null;
        }

        @Override
        public @Nullable String getConnectorOid() {
            return ResourceTypeUtil.getConnectorOid(resource);
        }

        @Override
        public @Nullable PrismContainer<ConnectorConfigurationType> getConnectorConfiguration() {
            return resource.asPrismObject().findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        }

        @Override
        public @Nullable CapabilitiesType getCapabilities() {
            return resource.getCapabilities();
        }

        @Override
        public void setCapabilities(CapabilitiesType capabilities) {
            resource.setCapabilities(capabilities);
        }

        @Override
        public void setCapabilitiesCachingMetadata(CachingMetadataType cachingMetadata) {
            CapabilitiesType capabilities = resource.getCapabilities();
            if (capabilities == null) {
                capabilities = new CapabilitiesType();
                resource.setCapabilities(capabilities);
            }
            capabilities.setCachingMetadata(cachingMetadata);
        }
        @Override
        public String toString() {
            return "ConnectorSpec.Main(" + resource + ")";
        }
    }

    private static class Additional extends ConnectorSpec {

        /** Keeping the name here just to be sure it's non-null even if the bean changes (but it should not!) */
        @NotNull private final String name;
        @NotNull private final ConnectorInstanceSpecificationType definitionBean;

        private Additional(@NotNull ResourceType resource, @NotNull ConnectorInstanceSpecificationType definitionBean)
                throws ConfigurationException {
            super(resource);

            String name = definitionBean.getName();
            configCheck(StringUtils.isNotBlank(name), "No connector name in additional connector in %s", resource);

            this.name = name;
            this.definitionBean = definitionBean;
        }

        @Override
        public boolean isMain() {
            return false;
        }

        @Override
        public @NotNull String getConnectorName() {
            return name;
        }

        @Override
        @NotNull
        public ItemPath getBasePath() {
            ItemPath path = definitionBean.asPrismContainerValue().getPath();
            checkPathValid(path);
            return path;
        }

        @Override
        public @Nullable String getConnectorOid() {
            return getOid(definitionBean.getConnectorRef());
        }

        @Override
        public @Nullable PrismContainer<ConnectorConfigurationType> getConnectorConfiguration() {
            //noinspection unchecked
            return definitionBean.asPrismContainerValue().findContainer(
                    ConnectorInstanceSpecificationType.F_CONNECTOR_CONFIGURATION);
        }

        @Override
        public @Nullable CapabilitiesType getCapabilities() {
            return definitionBean.getCapabilities();
        }

        @Override
        public void setCapabilities(CapabilitiesType capabilities) {
            definitionBean.setCapabilities(capabilities);
        }

        @Override
        public void setCapabilitiesCachingMetadata(CachingMetadataType cachingMetadata) {
            CapabilitiesType capabilities = definitionBean.getCapabilities();
            if (capabilities == null) {
                capabilities = new CapabilitiesType();
                definitionBean.setCapabilities(capabilities);
            }
            capabilities.setCachingMetadata(cachingMetadata);
        }

        private void checkPathValid(ItemPath path) {
            Long connectorPcvId = ItemPath.toIdOrNull(path.getSegment(1));
            stateCheck(connectorPcvId != null, "Additional connector has no PCV ID: %s", this);
        }

        @Override
        public String toString() {
            return "ConnectorSpec.Additional(" + resource + ":" + name + ")";
        }
    }
}
