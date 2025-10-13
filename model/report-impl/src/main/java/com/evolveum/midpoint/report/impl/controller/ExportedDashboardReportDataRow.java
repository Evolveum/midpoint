/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import org.jetbrains.annotations.NotNull;

/**
 * Single row of data for report being exported.
 *
 * It is "semi-formatted" in the sense that the values are stored as strings, but some technicalities,
 * like separation of values in multi-valued columns, are abstracted away here.
 *
 * Thread safety: does not need to be thread safe.
 */
public class ExportedDashboardReportDataRow extends ExportedReportDataRow {

    /**
     * Widget identifier.
     */
    @NotNull private final String widgetIdentifier;

    /**
     * Declare if this is basic widget row.
     */
    private final boolean isBasicWidgetRow;

    ExportedDashboardReportDataRow(int sequentialNumber, @NotNull String widgetIdentifier, boolean isBasicWidgetRow) {
        super(sequentialNumber);
        this.widgetIdentifier = widgetIdentifier;
        this.isBasicWidgetRow = isBasicWidgetRow;
    }

    ExportedDashboardReportDataRow(int sequentialNumber, String widgetIdentifier) {
        this(sequentialNumber, widgetIdentifier, false);
    }

    @NotNull
    public String getWidgetIdentifier() {
        return widgetIdentifier;
    }

    public boolean isBasicWidgetRow() {
        return isBasicWidgetRow;
    }
}
