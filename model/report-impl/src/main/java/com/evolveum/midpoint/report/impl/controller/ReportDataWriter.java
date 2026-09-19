/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;

/**
 * Responsible for creating and manipulating an exported report.
 *
 * Responsibilities:
 *
 * 1. Buffers semi-formatted ({@link ExportedReportHeaderRow} and {@link ExportedReportDataRow}) objects,
 * maintaining their correct order.
 *
 * 2. Writes the final form of the report when asked to do so.
 *
 * 3. Holds the file-format-specific configuration.
 *
 * This is the contract common to all formats, text and binary alike. Formats whose output can be
 * produced and concatenated as text (needed e.g. for distributed export) implement {@link TextReportDataWriter}.
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
     * Returns true if the output report should contain a header.
     *
     * Actually this method does not quite belong here, but is placed here for simplicity (the information is read
     * from the format-specific configuration which is handled by the writer).
     */
    boolean shouldWriteHeader();

    /**
     * Writes the completed report (built from the data buffered in this writer) to the provided stream.
     * The caller owns the stream.
     */
    void writeCompletedReport(@NotNull OutputStream outputStream) throws IOException;

    @Nullable
    default Function<String, String> getFunctionForWidgetStatus() {
        return null;
    }

    String getTypeSuffix();

    String getType();

    FileFormatConfigurationType getFileFormatConfiguration();
}
