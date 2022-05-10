/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TODO description
 *
 * Note: {@link #connectorOid} may be `null` e.g. for abstract resources.
 *
 * @author semancik
 */
public class ConnectorSpec {

    @NotNull private final PrismObject<ResourceType> resource;
    @Nullable private final String connectorName;
    @Nullable private final String connectorOid;
    @Nullable private final PrismContainer<ConnectorConfigurationType> connectorConfiguration;

    ConnectorSpec(
            @NotNull PrismObject<ResourceType> resource,
            @Nullable String connectorName,
            @Nullable String connectorOid,
            @Nullable PrismContainer<ConnectorConfigurationType> connectorConfiguration) {
        this.resource = resource;
        this.connectorName = connectorName;
        this.connectorOid = connectorOid;
        this.connectorConfiguration = connectorConfiguration;
    }

    public @NotNull PrismObject<ResourceType> getResource() {
        return resource;
    }

    public @Nullable String getConnectorName() {
        return connectorName;
    }

    public @Nullable String getConnectorOid() {
        return connectorOid;
    }

    public @Nullable PrismContainer<ConnectorConfigurationType> getConnectorConfiguration() {
        return connectorConfiguration;
    }

    public @NotNull ConfiguredConnectorCacheKey getCacheKey() {
        return new ConfiguredConnectorCacheKey(resource.getOid(), connectorName);
    }

    @Override
    public String toString() {
        return "ConnectorSpec(" + resource + ", name=" + connectorName + ", oid=" + connectorOid + ")";
    }
}
