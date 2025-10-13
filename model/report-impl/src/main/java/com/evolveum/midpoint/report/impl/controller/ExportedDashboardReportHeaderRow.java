/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Header row for report being exported.
 *
 */
public class ExportedDashboardReportHeaderRow extends ExportedReportHeaderRow{

    /**
     * Widget identifier.
     */
    @Nullable private final String widgetIdentifier;

    /**
     * Declare if this is basic widget row.
     */
    private final boolean isBasicWidgetRow;

    private ExportedDashboardReportHeaderRow(@NotNull List<ExportedReportHeaderColumn> columns, @NotNull List<String> labels,
            @Nullable String widgetIdentifier) {
        super(columns, labels);
        this.widgetIdentifier = widgetIdentifier;
        this.isBasicWidgetRow = StringUtils.isEmpty(widgetIdentifier);
    }

    static ExportedDashboardReportHeaderRow fromColumns(List<ExportedReportHeaderColumn> columns, String widgetName) {
        List<String> labels = columns.stream().map(ExportedReportHeaderColumn::getLabel).collect(Collectors.toList());
        return new ExportedDashboardReportHeaderRow(columns, labels, widgetName);
    }

    @Nullable public String getWidgetIdentifier() {
        return widgetIdentifier;
    }

    public boolean isBasicWidgetRow() {
        return isBasicWidgetRow;
    }
}
