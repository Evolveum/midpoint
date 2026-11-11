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
     * Whether the exported package gets a new connector version.
     */
    private boolean versionChangeEnabled;

    /**
     * New connector version entered by the user.
     */
    private String newVersion;

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

    public boolean isVersionChangeEnabled() {
        return versionChangeEnabled;
    }

    public void setVersionChangeEnabled(boolean versionChangeEnabled) {
        this.versionChangeEnabled = versionChangeEnabled;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
    }
}
