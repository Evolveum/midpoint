/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;

import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.VisibleForTesting;

/**
 * Very simple report reader, to be used for tests or for displaying parts of reports created.
 */
@Experimental
public class SimpleReportReader implements AutoCloseable {

    @NotNull private final CSVParser parser;

    /** Empty means all. */
    @NotNull private final List<String> columns;

    private SimpleReportReader(@NotNull CSVParser parser, @NotNull List<String> columns) {
        this.parser = parser;
        this.columns = columns;
    }

    @VisibleForTesting
    public static @NotNull SimpleReportReader createForLocalReportData(@NotNull String reportDataOid,
            @NotNull List<String> columns, @NotNull CommonTaskBeans beans, @NotNull OperationResult result)
            throws IOException, SchemaException, ObjectNotFoundException {

        var reportDataObject =
                beans.repositoryService.getObject(ReportDataType.class, reportDataOid, null, result);
        String filePath = MiscUtil.requireNonNull(
                reportDataObject.asObjectable().getFilePath(),
                () -> "No file in " + reportDataObject);
        return createFor(
                new FileInputStream(filePath),
                columns);
    }

    public static @NotNull SimpleReportReader createFor(@NotNull InputStream stream, @NotNull List<String> columns)
            throws IOException {
        CSVParser parser = CSVParser.parse(
                stream,
                StandardCharsets.UTF_8,
                CSVFormat.newFormat(',')
                        .builder()
                        .setQuote('"')
                        .setEscape('\\')
                        .setHeader()
                        .build());
        return new SimpleReportReader(parser, columns);
    }

    public List<List<String>> getRows() throws IOException {
        List<List<String>> rows = new ArrayList<>();

        List<String> headerNames = getHeaderNames();
        for (CSVRecord record : parser.getRecords()) {
            List<String> row = new ArrayList<>();
            int columns = record.size();
            for (int i = 0; i < columns; i++) {
                if (this.columns.isEmpty() || this.columns.contains(headerNames.get(i))) {
                    row.add(record.get(i));
                }
            }
            rows.add(row);
        }

        return rows;
    }

    public List<String> getHeaderNames() {
        return parser.getHeaderNames();
    }

    @Override
    public void close() throws Exception {
        parser.close();
    }
}
