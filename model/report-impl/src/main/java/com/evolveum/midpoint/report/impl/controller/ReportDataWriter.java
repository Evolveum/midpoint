/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller;

import java.nio.charset.Charset;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;

/**
 * Responsible for creating and manipulating text representation of an exported report.
 *
 * Responsibilities:
 *
 * 1. Buffers semi-formatted ({@link ExportedReportHeaderRow} and {@link ExportedReportDataRow}) objects,
 * maintaining their correct order.
 *
 * 2. Produces final string form when asked to do so.
 *
 * 2. Holds the file-format-specific configuration.
 */
public interface ReportDataWriter<ED extends ExportedReportDataRow, EH extends ExportedReportHeaderRow> {

    /**
     * Sets the header row.
     */
    void setHeaderRow(EH headerRow);

    /**
     * Appends a row of data to the report.
     *
     * BEWARE: Can be called from multiple threads. Should take {@link ExportedReportDataRow#sequentialNumber} into account!
     *
     * @param row Formatted (string) values for the row.
     */
    void appendDataRow(ED row);

    /** Resets the state of the writer, e.g. erasing all stored data. */
    void reset();

    /**
     * Returns the final text output of the writer, formatted according to the rules of the file format (CSV/HTML)
     * and a particular configuration.
     */
    String getStringData();

    /**
     * Returns true if the output report should contain a header.
     *
     * Actually this method does not quite belong here, but is placed here for simplicity (the information is read
     * from the format-specific configuration which is handled by the writer).
     */
    boolean shouldWriteHeader();

    /**
     * Returns the final text output of the report, formatted according to the rules of the file format (CSV/HTML)
     * and an added prefix and suffix of report.
     */
    String completeReport(String aggregatedData);

    /**
     * Use data in data writer.
     */
    String completeReport();

    @Nullable
    default Function<String, String> getFunctionForWidgetStatus() {
        return null;
    }

    String getTypeSuffix();

    String getType();

    FileFormatConfigurationType getFileFormatConfiguration();

    /**
     * Encoding for the output, supported explicitly only by some types of writers.
     */
    @NotNull
    default Charset getEncoding() {
        return Charset.defaultCharset();
//        return StandardCharsets.UTF_8; TODO wouldn't this be better? Definitely more predictable.
    }
}
