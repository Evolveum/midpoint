/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.ByteOrderMark;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

/**
 * Base for text-producing report writers (CSV, HTML).
 *
 * The text form of the buffered rows ({@link #getStringData()}) contains no report-level prefix or suffix,
 * so pieces from several buckets can be concatenated; {@link #completeReport(String)} adds the prefix and suffix
 * to the (concatenated) data.
 */
public abstract class AbstractTextReportDataWriter<ED extends ExportedReportDataRow, EH extends ExportedReportHeaderRow>
        extends AbstractReportDataWriter<ED, EH>
        implements DistributableReportDataWriter<ED, EH> {

    /**
     * Concatenated partial data (distributed export).
     *
     * TODO eliminate gathering in memory: write to a file immediately after getting the data.
     */
    private final StringBuilder aggregatedData = new StringBuilder();

    protected AbstractTextReportDataWriter(@NotNull ReportServiceImpl reportService) {
        super(reportService);
    }

    /** Text form of the data buffered so far, formatted according to the rules of the file format and its configuration. */
    public abstract String getStringData();

    /** Final text of the report built from (concatenated) partial data, with the report prefix and suffix added. */
    public String completeReport(String aggregatedData) {
        return aggregatedData;
    }

    /** Final text of the report built from the data buffered in this writer. */
    public String completeReport() {
        return completeReport(getStringData());
    }

    /** Encoding of the output; explicitly configurable only for some formats. */
    public @NotNull Charset getEncoding() {
        return StandardCharsets.UTF_8;
    }

    @Override
    public void writeCompletedReport(@NotNull OutputStream outputStream) throws IOException {
        writeText(completeReport(), getEncoding(), outputStream);
    }

    @Override
    public void storePartialData(@NotNull ReportDataType partialReportData) {
        partialReportData.setData(getStringData());
    }

    @Override
    public void appendPartialData(@NotNull ReportDataType partialReportData) {
        String data = DistributableReportDataWriter.getTextData(partialReportData);
        if (data != null) {
            aggregatedData.append(data);
        }
    }

    @Override
    public void writeAggregatedReport(@NotNull OutputStream outputStream) throws IOException {
        writeText(completeReport(aggregatedData.toString()), getEncoding(), outputStream);
    }

    /** Writes the text in the given encoding, with the UTF-8 BOM where applicable (historical behavior). */
    private static void writeText(@NotNull String text, @NotNull Charset encoding, @NotNull OutputStream outputStream)
            throws IOException {
        if (StandardCharsets.UTF_8.equals(encoding)) {
            outputStream.write(ByteOrderMark.UTF_8.getBytes());
        }
        outputStream.write(text.getBytes(encoding));
    }
}
