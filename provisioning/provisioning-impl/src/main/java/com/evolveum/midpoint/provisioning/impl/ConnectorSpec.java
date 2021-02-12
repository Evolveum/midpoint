/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author semancik
 *
 */
public class ConnectorSpec {

    private final PrismObject<ResourceType> resource;
    private final String connectorName;
    private final String connectorOid;
    private final PrismContainer<ConnectorConfigurationType> connectorConfiguration;

    ConnectorSpec(PrismObject<ResourceType> resource, String connectorName, String connectorOid,
            PrismContainer<ConnectorConfigurationType> connectorConfiguration) {
        this.resource = resource;
        this.connectorName = connectorName;
        this.connectorOid = connectorOid;
        this.connectorConfiguration = connectorConfiguration;
    }

    public PrismObject<ResourceType> getResource() {
        return resource;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public String getConnectorOid() {
        return connectorOid;
    }

    public PrismContainer<ConnectorConfigurationType> getConnectorConfiguration() {
        return connectorConfiguration;
    }

    public ConfiguredConnectorCacheKey getCacheKey() {
        return new ConfiguredConnectorCacheKey(resource.getOid(), connectorName);
    }

    @Override
    public String toString() {
        return "ConnectorSpec(" + resource + ", name=" + connectorName + ", oid=" + connectorOid + ")";
    }
}
