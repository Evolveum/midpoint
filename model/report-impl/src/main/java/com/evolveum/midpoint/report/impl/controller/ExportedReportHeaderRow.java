/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Header row for report being exported.
 *
 */
public class ExportedReportHeaderRow {

    /**
     * Labels for the header row.
     */
    @NotNull private final List<String> labels;

    /**
     * Columns for the header row.
     */
    @NotNull private final List<ExportedReportHeaderColumn> columns;

    protected ExportedReportHeaderRow(@NotNull List<ExportedReportHeaderColumn> columns, @NotNull List<String> labels) {
        this.columns = columns;
        this.labels = labels;
    }

    static <EH extends ExportedReportHeaderRow> EH fromColumns(List<ExportedReportHeaderColumn> columns) {
        List<String> labels = columns.stream().map(ExportedReportHeaderColumn::getLabel).collect(Collectors.toList());
        return (EH) new ExportedReportHeaderRow(columns, labels);
    }

    public @NotNull List<String> getLabels() {
        return labels;
    }

    public @NotNull List<ExportedReportHeaderColumn> getColumns() {
        return columns;
    }
}
