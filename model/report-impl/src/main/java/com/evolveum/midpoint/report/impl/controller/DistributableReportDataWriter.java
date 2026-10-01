/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.io.IOException;
import java.io.OutputStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * A {@link ReportDataWriter} whose output can be produced in pieces and merged later.
 * This is what the distributed (bucketed) report export relies on:
 *
 * 1. Each bucket is processed by a writer that buffers the rows of that bucket and then stores them
 * as partial data into a partial {@link ReportDataType} object ({@link #storePartialData(ReportDataType)}).
 * The first bucket also carries the header row, if any.
 *
 * 2. The aggregation activity feeds the partial objects, in bucket order, to a fresh writer
 * ({@link #appendPartialData(ReportDataType)}) and finally asks it to write the complete report
 * ({@link #writeAggregatedReport(OutputStream)}).
 *
 * Partial objects live only until the aggregation consumes them.
 */
public interface DistributableReportDataWriter<ED extends ExportedReportDataRow, EH extends ExportedReportHeaderRow>
        extends ReportDataWriter<ED, EH> {

    /** Stores the rows buffered in this writer (and the header row, if set) into the partial report data object. */
    void storePartialData(@NotNull ReportDataType partialReportData) throws IOException;

    /** Takes over the partial data stored by {@link #storePartialData(ReportDataType)}. Called in bucket order. */
    void appendPartialData(@NotNull ReportDataType partialReportData) throws IOException;

    /** Writes the report assembled from the appended partial data. The caller owns the stream. */
    void writeAggregatedReport(@NotNull OutputStream outputStream) throws IOException;

    /** Partial data as text; a value without explicit type (legacy) is taken as text. */
    static @Nullable String getTextData(@NotNull ReportDataType partialReportData) {
        return getData(partialReportData, String.class);
    }

    /** Partial data as bytes; a value without explicit type is parsed as base64. */
    static byte @Nullable [] getBinaryData(@NotNull ReportDataType partialReportData) {
        return getData(partialReportData, byte[].class);
    }

    private static <T> @Nullable T getData(@NotNull ReportDataType partialReportData, @NotNull Class<T> expected) {
        Object data = partialReportData.getData();
        if (data == null) {
            return null;
        }
        if (data instanceof RawType raw) {
            try {
                return raw.getParsedRealValue(expected);
            } catch (SchemaException e) {
                throw new SystemException("Couldn't parse partial report data of " + partialReportData + " as "
                        + expected.getSimpleName() + ": " + e.getMessage(), e);
            }
        }
        if (expected.isInstance(data)) {
            return expected.cast(data);
        }
        throw new SystemException("Partial report data of " + partialReportData + " is " + data.getClass().getSimpleName()
                + ", expected " + expected.getSimpleName() + " (report format mismatch?)");
    }
}
