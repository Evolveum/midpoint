/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;

/**
 * Listener for automatic connector discovery provided by Connector Factory.
 */
public interface ConnectorDiscoveryListener {

    /**
     * @param host null if host is this instance
     */
    void newConnectorDiscovered(ConnectorHostType host);
}
