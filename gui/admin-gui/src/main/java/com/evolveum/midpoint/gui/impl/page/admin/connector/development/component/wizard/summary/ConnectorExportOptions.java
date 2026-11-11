/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.summary;

import java.io.Serial;
import java.io.Serializable;

/**
 * Options of the connector export chosen by the user.
 */
public class ConnectorExportOptions implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /**
     * Format of the exported package, null until the user selects one.
     */
    private ConnectorExportFormat format;

    /**
     * Destination of the exported package, null until the user selects one.
     */
    private ConnectorExportDestination destination;

    /**
     * Connector version of the exported package.
     */
    private String exportedVersion;

    /**
     * Whether the current connector version is also changed to the exported version.
     */
    private boolean connectorVersionUpdateEnabled;

    public ConnectorExportOptions(String exportedVersion) {
        this.exportedVersion = exportedVersion;
    }

    public ConnectorExportFormat getFormat() {
        return format;
    }

    public void setFormat(ConnectorExportFormat format) {
        this.format = format;
    }

    public ConnectorExportDestination getDestination() {
        return destination;
    }

    public void setDestination(ConnectorExportDestination destination) {
        this.destination = destination;
    }

    public String getExportedVersion() {
        return exportedVersion;
    }

    public void setExportedVersion(String exportedVersion) {
        this.exportedVersion = exportedVersion;
    }

    public boolean isConnectorVersionUpdateEnabled() {
        return connectorVersionUpdateEnabled;
    }

    public void setConnectorVersionUpdateEnabled(boolean connectorVersionUpdateEnabled) {
        this.connectorVersionUpdateEnabled = connectorVersionUpdateEnabled;
    }
}
