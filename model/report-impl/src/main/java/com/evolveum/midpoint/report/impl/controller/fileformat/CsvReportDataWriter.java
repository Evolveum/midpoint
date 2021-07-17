/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller.fileformat;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates and manipulates exported reports in CSV format.
 */
public class CsvReportDataWriter implements ReportDataWriter {

    @NotNull private final CommonCsvSupport support;

    /**
     * Header row to be put into resulting CSV file.
     *
     * TODO move to some abstract superclass?
     */
    private ExportedReportHeaderRow headerRow;

    /**
     * Data rows to be put into resulting CSV file.
     *
     * TODO move to some abstract superclass?
     */
    @NotNull private final List<ExportedReportDataRow> dataRows = new ArrayList<>();

    public CsvReportDataWriter(FileFormatConfigurationType configuration) {
        this.support = new CommonCsvSupport(configuration);
    }

    @Override
    public void setHeaderRow(ExportedReportHeaderRow headerRow) {
        this.headerRow = headerRow;
    }

    /**
     * Thread safety: Guarded by `this`.
     *
     * Tries to find a place where new row is to be inserted. It is the first row (from backwards) where the sequential number
     * is less than the number of row being inserted.
     *
     * Note: we are going from the end because we assume that the new object will be placed approximately there.
     * So the time complexity is more O(n) than O(n^2) as it would be if we would go from the beginning of the list.
     *
     * @param row Formatted (string) values for the row.
     */
    @Override
    public synchronized void appendDataRow(ExportedReportDataRow row) {
        int i;
        for (i = dataRows.size() - 1; i >= 0; i--) {
            if (dataRows.get(i).getSequentialNumber() < row.getSequentialNumber()) {
                break;
            }
        }
        dataRows.add(i + 1, row);
    }

    @Override
    public void reset() {
        headerRow = null;
        dataRows.clear();
    }

    @Override
    public String getStringData() {
        try {
            StringWriter stringWriter = new StringWriter();
            CSVFormat csvFormat = createCsvFormat();
            CSVPrinter printer = new CSVPrinter(stringWriter, csvFormat);

            for (ExportedReportDataRow row : dataRows) {
                printer.printRecord(createPhysicalColumnsList(row));
            }
            printer.flush();
            return stringWriter.toString();
        } catch (IOException e) {
            throw new SystemException("Unexpected IOException: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean shouldWriteHeader() {
        return support.isHeader();
    }

    private CSVFormat createCsvFormat() {
        CSVFormat csvFormat = support.createCsvFormat();
        if (headerRow != null) {
            return csvFormat
                    .withHeader(createPhysicalColumnsList(headerRow).toArray(new String[0]))
                    .withSkipHeaderRecord(false);
        } else {
            return csvFormat.withSkipHeaderRecord(true);
        }
    }

    private List<String> createPhysicalColumnsList(ExportedReportHeaderRow row) {
        return row.getLabels();
    }

    private List<String> createPhysicalColumnsList(ExportedReportDataRow row) {
        return row.getValues().stream()
                .map(this::formatColumn)
                .collect(Collectors.toList());
    }

    private String formatColumn(List<String> values) {
        return values.stream()
                .map(support::removeNewLines)
                .collect(Collectors.joining(support.getMultivalueDelimiter()));
    }
}
