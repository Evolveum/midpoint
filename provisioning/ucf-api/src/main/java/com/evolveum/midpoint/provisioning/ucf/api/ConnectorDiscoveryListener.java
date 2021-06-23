package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;

public interface ConnectorDiscoveryListener {

    void newConnectorDiscovered(ConnectorHostType host);
}
