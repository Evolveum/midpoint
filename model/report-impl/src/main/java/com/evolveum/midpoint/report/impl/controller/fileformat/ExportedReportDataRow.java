/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller.fileformat;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Single row of data for report being exported.
 *
 * It is "semi-formatted" in the sense that the values are stored as strings, but some technicalities,
 * like separation of values in multi-valued columns, are abstracted away here.
 *
 * TODO It is to be seen if this representation is general enough to be used in HTML as well (not only in CSV)
 *
 * Thread safety: does not need to be thread safe.
 */
public class ExportedReportDataRow {

    /**
     * Sequential number of a row. It determines the position of a row in a report part corresponding to a work bucket.
     */
    private final int sequentialNumber;

    /**
     * Individual columns. Each contains a list of values.
     */
    @NotNull private final List<List<String>> values = new ArrayList<>();

    ExportedReportDataRow(int sequentialNumber) {
        this.sequentialNumber = sequentialNumber;
    }

    public int getSequentialNumber() {
        return sequentialNumber;
    }

    public @NotNull List<List<String>> getValues() {
        return values;
    }

    void addColumn(@NotNull List<String> columnValues) {
        values.add(columnValues);
    }
}
