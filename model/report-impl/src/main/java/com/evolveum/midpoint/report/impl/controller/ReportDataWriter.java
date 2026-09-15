/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.apache.commons.io.ByteOrderMark;
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
 * 2. Produces final text or binary form when asked to do so.
 *
 * 3. Holds the file-format-specific configuration.
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

    /**
     * Writes the completed report to the provided stream. The caller owns the stream.
     *
     * The default implementation preserves the historical text encoding behavior, including the UTF-8 BOM.
     * Binary writers should override this method.
     */
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
        return StandardCharsets.UTF_8;
    }
}
