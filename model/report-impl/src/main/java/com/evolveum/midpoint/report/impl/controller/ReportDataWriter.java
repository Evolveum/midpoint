/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

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
 *
 * TODO better name?
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
    String completizeReport(String aggregatedData);

    /**
     * Use data in data writer.
     */
    String completizeReport();

    @Nullable
    default Function<String, String> getFunctionForWidgetStatus(){
        return null;
    }

    String getTypeSuffix();

    String getType();

    FileFormatConfigurationType getFileFormatConfiguration();
}
