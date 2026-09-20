/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.report.impl.ReportServiceImpl;

/**
 * Base for text-producing report writers: buffering from {@link AbstractReportDataWriter}
 * plus the default completion behavior of {@link TextReportDataWriter}.
 */
public abstract class AbstractTextReportDataWriter<ED extends ExportedReportDataRow, EH extends ExportedReportHeaderRow>
        extends AbstractReportDataWriter<ED, EH>
        implements TextReportDataWriter<ED, EH> {

    protected AbstractTextReportDataWriter(@NotNull ReportServiceImpl reportService) {
        super(reportService);
    }

    @Override
    public String completeReport(String aggregatedData) {
        return aggregatedData;
    }

    @Override
    public String completeReport() {
        return completeReport(getStringData());
    }
}
