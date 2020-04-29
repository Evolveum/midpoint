/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;

/**
 * @author semancik
 *
 */
public class ConfiguredConnectorInstanceEntry {

    private String connectorOid;
    private PrismContainer<ConnectorConfigurationType> configuration;
    private ConnectorInstance connectorInstance;

    public String getConnectorOid() {
        return connectorOid;
    }

    public void setConnectorOid(String connectorOid) {
        this.connectorOid = connectorOid;
    }

    public PrismContainer<ConnectorConfigurationType> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(PrismContainer<ConnectorConfigurationType> configuration) {
        this.configuration = configuration;
    }

    public boolean isConfigured() {
        return configuration != null;
    }

    public ConnectorInstance getConnectorInstance() {
        return connectorInstance;
    }

    public void setConnectorInstance(ConnectorInstance connectorInstance) {
        this.connectorInstance = connectorInstance;
    }

    @Override
    public String toString() {
        return "ConfiguredConnectorInstanceEntry{" +
                "connectorOid='" + connectorOid + '\'' +
                ", connectorInstance=" + connectorInstance +
                '}';
    }
}
