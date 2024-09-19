/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.provisioning.ucf.api.ConnectorConfiguration;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * A {@link ConnectorInstance} along with connector OID (that is used mainly to check whether the connector instance
 * is applicable for a given resource).
 *
 * TODO we could obtain connector OID from the {@link ConnectorInstance} as well, if we'd like to
 *
 * @author semancik
 */
class ConfiguredConnectorInstanceEntry {

    @NotNull private final String connectorOid;

    @NotNull private final ConnectorInstance connectorInstance;

    ConfiguredConnectorInstanceEntry(@NotNull String connectorOid, @NotNull ConnectorInstance connectorInstance) {
        this.connectorOid = connectorOid;
        this.connectorInstance = connectorInstance;
    }

    boolean matchesConnectorOid(String oidToMatch) {
        return connectorOid.equals(oidToMatch);
    }

    public ConnectorConfiguration getConfiguration() {
        return connectorInstance.getCurrentConfiguration();
    }

    /** Assumes {@link #isConfigured()} is {@code true}. */
    boolean isFreshRegardingSpec(@NotNull ConnectorSpec connectorSpec) {
        var currentConfiguration =
                stateNonNull(connectorInstance.getCurrentConfiguration(), "Not configured? %s", this);
        return matchesConnectorOid(connectorSpec.getConnectorOid())
                && currentConfiguration.equivalent(connectorSpec.getConnectorConfiguration());
    }

    public boolean isConfigured() {
        return connectorInstance.getCurrentConfiguration() != null;
    }

    @NotNull ConnectorInstance getConnectorInstance() {
        return connectorInstance;
    }

    @Override
    public String toString() {
        return "ConfiguredConnectorInstanceEntry{" +
                "connectorOid='" + connectorOid + '\'' +
                ", connectorInstance=" + connectorInstance +
                '}';
    }
}
