/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

/**
 * Reads the input file of an import report into rows of named values, one {@link VariablesMap} per row.
 */
public interface ReportDataReader {

    /**
     * Reads all rows of the file referenced by the report data object.
     *
     * @param viewHeaders Column labels defined by the report view, in file column order. If empty, the header
     * row of the file is used to name the columns.
     */
    @NotNull List<VariablesMap> read(@NotNull ReportDataType reportData, @NotNull List<String> viewHeaders)
            throws IOException;
}
