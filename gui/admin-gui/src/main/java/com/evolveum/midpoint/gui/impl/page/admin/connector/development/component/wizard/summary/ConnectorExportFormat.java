/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.summary;

/**
 * Format of the exported connector package.
 */
public enum ConnectorExportFormat {

    /**
     * Deployable connector jar.
     */
    JAR("fa fa-file-code text-danger"),

    /**
     * Connector project packed as zip.
     */
    ZIP("fa fa-file-zipper text-warning");

    private final String icon;

    ConnectorExportFormat(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }
}
