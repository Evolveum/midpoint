/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller.fileformat;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
    private final String widgetIdentifier;

    /**
     * Declare if this is basic widget row.
     */
    @NotNull private final boolean isBasicWidgetRow;

    ExportedDashboardReportDataRow(int sequentialNumber, String widgetIdentifier) {
        super(sequentialNumber);
        this.widgetIdentifier = widgetIdentifier;
        this.isBasicWidgetRow = StringUtils.isEmpty(widgetIdentifier);
    }

    ExportedDashboardReportDataRow(int sequentialNumber) {
        this(sequentialNumber, null);
    }

    @Nullable
    public String getWidgetIdentifier() {
        return widgetIdentifier;
    }

    @NotNull public boolean isBasicWidgetRow() {
        return isBasicWidgetRow;
    }
}
