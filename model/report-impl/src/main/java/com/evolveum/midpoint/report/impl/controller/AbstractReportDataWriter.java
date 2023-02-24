/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.report.impl.ReportServiceImpl;

/**
 * Creates and manipulates exported reports.
 */
public abstract class AbstractReportDataWriter<ED extends ExportedReportDataRow, EH extends ExportedReportHeaderRow>
        implements ReportDataWriter<ED, EH> {

    /**
     * Header row to be put into resulting CSV file.
     */
    private ExportedReportHeaderRow headerRow;

    /**
     * Data rows to be put into resulting CSV file.
     */
    @NotNull private final List<ED> dataRows = new ArrayList<>();

    @NotNull protected final ReportServiceImpl reportService;

    protected AbstractReportDataWriter(@NotNull ReportServiceImpl reportService) {
        this.reportService = reportService;
    }

    @Override
    public void setHeaderRow(EH headerRow) {
        this.headerRow = headerRow;
    }

    protected ExportedReportHeaderRow getHeaderRow() {
        return headerRow;
    }

    @NotNull
    protected List<ED> getDataRows() {
        return dataRows;
    }

    /**
     * Thread safety: Guarded by `this`.
     *
     * Tries to find a place where new row is to be inserted. It is the first row (from backwards) where the sequential number
     * is less than the number of row being inserted.
     *
     * Note: we are going from the end because we assume that the new object will be placed approximately there.
     * So the time complexity is more O(n) than O(n^2) as it would be if we would go from the beginning of the list.
     *
     * Beware that after implementing use = inner or left join (~ asRow mode), there may be multiple rows with the same
     * sequential number. They are produced within the same thread, so their order is guaranteed now: the insertion algorithm
     * places them into the order they are generated in.
     *
     * @param row Formatted (string) values for the row.
     */
    @Override
    public synchronized void appendDataRow(ED row) {
        int i;
        for (i = getDataRows().size() - 1; i >= 0; i--) {
            if (getDataRows().get(i).getSequentialNumber() <= row.getSequentialNumber()) {
                break;
            }
        }
        getDataRows().add(i + 1, row);
    }

    @Override
    public void reset() {
        headerRow = null;
        dataRows.clear();
    }

    @Override
    public abstract String getStringData();

    @Override
    public abstract boolean shouldWriteHeader();

    @Override
    public String completeReport(String aggregatedData) {
        return aggregatedData;
    }

    @Override
    public String completeReport() {
        return completeReport(getStringData());
    }
}
