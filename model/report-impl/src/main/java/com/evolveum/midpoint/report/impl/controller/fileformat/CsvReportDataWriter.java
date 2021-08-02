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
public class CsvReportDataWriter extends AbstractReportDataWriter {

    @NotNull private final CommonCsvSupport support;

    public CsvReportDataWriter(FileFormatConfigurationType configuration) {
        this.support = new CommonCsvSupport(configuration);
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
}
