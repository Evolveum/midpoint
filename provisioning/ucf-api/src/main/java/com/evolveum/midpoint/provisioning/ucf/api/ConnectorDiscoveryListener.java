/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
