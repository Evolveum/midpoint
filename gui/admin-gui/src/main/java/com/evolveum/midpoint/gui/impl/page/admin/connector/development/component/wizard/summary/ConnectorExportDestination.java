/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.summary;

/**
 * Destination the exported connector package is exported to.
 */
public enum ConnectorExportDestination {

    /**
     * Current user device.
     */
    DEVICE("ConnectorExportDestination.DEVICE.description"),

    /**
     * Connector directory of this midPoint instance.
     */
    CONNECTOR_DIRECTORY("ConnectorExportDestination.CONNECTOR_DIRECTORY.description");

    private final String descriptionKey;

    ConnectorExportDestination(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    /**
     * Whether the package of the given format can be exported to this destination.
     *
     * @param format format of the exported package
     * @return true if the format can be exported to this destination
     */
    public boolean supports(ConnectorExportFormat format) {
        return switch (this) {
            case DEVICE -> true;
            case CONNECTOR_DIRECTORY -> format == ConnectorExportFormat.JAR;
        };
    }
}
