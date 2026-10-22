/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

/**
 * Reads the input file of an import report into rows of named values, one {@link VariablesMap} per row.
 *
 * Rows are passed to the caller one by one as they are read, so that large files are not kept in memory
 * as a whole (MID-11009).
 */
public interface ReportDataReader {

    /**
     * Reads the rows of the file referenced by the report data object, passing each of them to the handler.
     * Reading stops early if the handler returns false.
     *
     * @param viewHeaders Column labels defined by the report view, in file column order. If empty, the header
     * row of the file is used to name the columns.
     */
    void read(@NotNull ReportDataType reportData, @NotNull List<String> viewHeaders,
            @NotNull Predicate<VariablesMap> rowHandler) throws IOException;

    /**
     * Counts the data rows of the file. As the whole file is read, a file that cannot be parsed is detected
     * here already ({@link #read} throws an {@link IOException}), i.e. before any row is processed.
     */
    default int countRows(@NotNull ReportDataType reportData, @NotNull List<String> viewHeaders) throws IOException {
        AtomicInteger count = new AtomicInteger();
        read(reportData, viewHeaders, row -> {
            count.incrementAndGet();
            return true;
        });
        return count.get();
    }
}
