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

/**
 * A {@link ReportDataWriter} whose output is text, produced in pieces that can be concatenated.
 *
 * This is what the distributed report export relies on: each bucket produces partial data via
 * {@link #getStringData()}, the pieces are concatenated by the aggregation activity, and the final
 * report is created by {@link #completeReport(String)}. Binary formats (e.g. XLSX) do not implement
 * this interface and are therefore not usable for distributed export.
 */
public interface TextReportDataWriter<ED extends ExportedReportDataRow, EH extends ExportedReportHeaderRow>
        extends ReportDataWriter<ED, EH> {

    /**
     * Returns the text form of the data buffered so far, formatted according to the rules of the file format
     * and a particular configuration. Contains no report-level prefix or suffix, so outputs of multiple writers
     * can be concatenated.
     */
    String getStringData();

    /**
     * Returns the final text output of the report, built from (concatenated) partial data
     * and an added prefix and suffix of the report.
     */
    String completeReport(String aggregatedData);

    /**
     * Returns the final text output of the report, built from the data buffered in this writer.
     */
    String completeReport();

    /**
     * Encoding for the output, supported explicitly only by some types of writers.
     */
    @NotNull
    default Charset getEncoding() {
        return StandardCharsets.UTF_8;
    }

    /**
     * Writes {@link #completeReport()} using {@link #getEncoding()}, including the UTF-8 BOM where applicable.
     */
    @Override
    default void writeCompletedReport(@NotNull OutputStream outputStream) throws IOException {
        writeText(completeReport(), getEncoding(), outputStream);
    }

    static void writeText(
            @NotNull String text,
            @NotNull Charset encoding,
            @NotNull OutputStream outputStream) throws IOException {
        if (StandardCharsets.UTF_8.equals(encoding)) {
            outputStream.write(ByteOrderMark.UTF_8.getBytes());
        }
        outputStream.write(text.getBytes(encoding));
    }
}
