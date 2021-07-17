/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller.fileformat;

/**
 * Creates and manipulates exported reports in HTML format.
 *
 * TODO implement
 */
public class HtmlReportDataWriter implements ReportDataWriter {

    @Override
    public void setHeaderRow(ExportedReportHeaderRow headerRow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void appendDataRow(ExportedReportDataRow row) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getStringData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean shouldWriteHeader() {
        throw new UnsupportedOperationException();
    }
}
