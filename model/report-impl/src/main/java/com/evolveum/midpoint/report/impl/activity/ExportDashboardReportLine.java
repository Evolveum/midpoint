/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.prism.Containerable;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a line of dashboard report to be exported.
 */
public class ExportDashboardReportLine<C extends Containerable> {

    /**
     * Line number.
     */
    private final int lineNumber;

    /**
     * Exported object.
     */
    @NotNull private final C container;

    /**
     * Name of widget from dashboard.
     */
    @NotNull private final String widgetIdentifier;

    @NotNull private final boolean isBasicWidgetRow;

    ExportDashboardReportLine(int lineNumber, C container, String widgetIdentifier) {
        this.lineNumber = lineNumber;
        this.container = container;
        this.widgetIdentifier = widgetIdentifier;
        this.isBasicWidgetRow = StringUtils.isEmpty(widgetIdentifier);
    }

    ExportDashboardReportLine(int sequentialNumber, C container) {
        this(sequentialNumber, container, null);
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public C getContainer() {
        return container;
    }

    public String getWidgetIdentifier() {
        return widgetIdentifier;
    }

    public boolean isBasicWidgetRow() {
        return isBasicWidgetRow;
    }

    @Override
    public String toString() {
        return "ExportDashboardReportLine{" +
                "lineNumber=" + lineNumber +
                ", container='" + container + '\'' +
                ", widgetIdentifier='" + widgetIdentifier + "\'" +
                '}';
    }

}
