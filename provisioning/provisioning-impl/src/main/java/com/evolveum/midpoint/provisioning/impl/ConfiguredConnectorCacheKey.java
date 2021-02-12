/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

/**
 * @author semancik
 */
public class ConfiguredConnectorCacheKey {

    private final String resourceOid;
    private final String connectorName;

    ConfiguredConnectorCacheKey(String resourceOid, String connectorName) {
        super();
        this.resourceOid = resourceOid;
        this.connectorName = connectorName;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public String getConnectorName() {
        return connectorName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((connectorName == null) ? 0 : connectorName.hashCode());
        result = prime * result + ((resourceOid == null) ? 0 : resourceOid.hashCode());
        return result;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConfiguredConnectorCacheKey other = (ConfiguredConnectorCacheKey) obj;
        if (connectorName == null) {
            if (other.connectorName != null)
                return false;
        } else if (!connectorName.equals(other.connectorName))
            return false;
        if (resourceOid == null) {
            if (other.resourceOid != null)
                return false;
        } else if (!resourceOid.equals(other.resourceOid))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ConfiguredConnectorCacheKey(" + resourceOid + ":" + connectorName + ")";
    }
}
