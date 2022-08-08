/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;

/**
 * Creates and manipulates exported reports in CSV format.
 */
public class CsvReportDataWriter extends AbstractReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow>
        implements DashboardReportDataWriter {

    @NotNull private final CommonCsvSupport support;

    @Nullable private final FileFormatConfigurationType configuration;

    @NotNull private final Map<String, String> widgetsData = new HashMap<>();

    public CsvReportDataWriter(@Nullable FileFormatConfigurationType configuration) {
        this.support = new CommonCsvSupport(configuration);
        this.configuration = configuration;
    }

    @Override
    public synchronized void appendDataRow(ExportedReportDataRow row) {
        if (row instanceof ExportedDashboardReportDataRow
                && ((ExportedDashboardReportDataRow) row).isBasicWidgetRow()) {
            widgetsData.put(
                    ((ExportedDashboardReportDataRow) row).getWidgetIdentifier(),
                    String.join("", row.getValues().get(CommonHtmlSupport.getIndexOfNumberColumn())));
        }
        super.appendDataRow(row);
    }

    @Override
    public String getStringData() {
        try {
            StringWriter stringWriter = new StringWriter();
            CSVFormat csvFormat = createCsvFormat();
            CSVPrinter printer = new CSVPrinter(stringWriter, csvFormat);

            for (ExportedReportDataRow row : getDataRows()) {
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

    @Override
    public String getTypeSuffix() {
        return ".csv";
    }

    @Override
    public String getType() {
        return "CSV";
    }

    @Override
    public FileFormatConfigurationType getFileFormatConfiguration() {
        return configuration;
    }

    private CSVFormat createCsvFormat() {
        CSVFormat csvFormat = support.createCsvFormat();
        if (getHeaderRow() != null) {
            return csvFormat
                    .withHeader(createPhysicalColumnsList(getHeaderRow()).toArray(new String[0]))
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

    @Override
    @NotNull
    public Map<String, String> getWidgetsData() {
        return widgetsData;
    }

    @Override
    public @NotNull Charset getEncoding() {
        String encoding = support.getEncoding();
        try {
            return encoding != null ? Charset.forName(encoding) : super.getEncoding();
        } catch (Exception e) {
            return super.getEncoding();
        }
    }
}
