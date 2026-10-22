/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

/**
 * Reads import data from CSV files, using the CSV configuration of the report.
 *
 * Multiple values in one cell are separated by the configured multivalue delimiter,
 * matching the CSV export.
 */
public class CsvReportDataReader implements ReportDataReader {

    private static final byte[] ZIP_MAGIC = { 'P', 'K', 3, 4 };

    @NotNull private final CommonCsvSupport support;

    public CsvReportDataReader(@Nullable FileFormatConfigurationType configuration) {
        this.support = new CommonCsvSupport(configuration);
    }

    @Override
    public void read(@NotNull ReportDataType reportData, @NotNull List<String> viewHeaders,
            @NotNull Predicate<VariablesMap> rowHandler) throws IOException {
        List<String> headers = new ArrayList<>(viewHeaders);
        CSVFormat csvFormat = support.createCsvFormat();
        if (headers.isEmpty()) {
            csvFormat = csvFormat.withFirstRecordAsHeader();
        }
        if (support.isHeader()) {
            if (!headers.isEmpty()) {
                csvFormat = csvFormat.withHeader(headers.toArray(new String[0]));
            }
            csvFormat = csvFormat.withSkipHeaderRecord(true);
        } else {
            if (headers.isEmpty()) {
                throw new IllegalArgumentException("Couldn't find headers please "
                        + "define them via view element or write them to csv file and set "
                        + "header element in file format configuration to true.");
            }
            csvFormat = csvFormat.withSkipHeaderRecord(false);
        }

        try (Reader reader = openReader(reportData);
                CSVParser csvParser = new CSVParser(reader, csvFormat)) {
            if (headers.isEmpty()) {
                headers = csvParser.getHeaderNames();
            }
            for (CSVRecord csvRecord : csvParser) {
                // passes the row to the caller (e.g. for import); false means the caller wants no more rows
                if (!rowHandler.test(toVariables(csvRecord, headers))) {
                    break;
                }
            }
        } catch (UncheckedIOException e) {
            // the parser reports I/O and parsing errors as unchecked exceptions when iterating
            throw e.getCause();
        }
    }

    private VariablesMap toVariables(CSVRecord csvRecord, List<String> headers) {
        VariablesMap variables = new VariablesMap();
        for (String name : headers) {
            String value;
            if (support.isHeader()) {
                value = csvRecord.get(name);
            } else {
                value = csvRecord.get(headers.indexOf(name));
            }
            if (value != null && value.isEmpty()) {
                value = null;
            }
            if (value != null && value.contains(support.getMultivalueDelimiter())) {
                String[] realValues = value.split(support.getMultivalueDelimiter());
                variables.put(name, Arrays.asList(realValues), String.class);
            } else {
                variables.put(name, value, String.class);
            }
        }
        return variables;
    }

    private Reader openReader(ReportDataType reportData) throws IOException {
        InputStream in = new BufferedInputStream(Files.newInputStream(Paths.get(reportData.getFilePath())));
        try {
            rejectZipContent(in, reportData);
            BOMInputStream bomIn = BOMInputStream.builder()
                    .setInputStream(in)
                    .get();
            return new InputStreamReader(bomIn, support.getEncoding());
        } catch (IOException | RuntimeException e) {
            in.close();
            throw e;
        }
    }

    /**
     * The CSV parser does not fail on binary input; it produces garbage records instead, which would then be
     * imported as objects. So we refuse the most likely mistake explicitly: an XLSX (ZIP) file declared as CSV.
     */
    private static void rejectZipContent(InputStream in, ReportDataType reportData) throws IOException {
        in.mark(ZIP_MAGIC.length);
        byte[] head = in.readNBytes(ZIP_MAGIC.length);
        in.reset();
        if (Arrays.equals(head, ZIP_MAGIC)) {
            throw new IOException("File " + reportData.getFilePath() + " is a ZIP archive (an XLSX file?), not a CSV file."
                    + " Set the file format of the report or of the report data accordingly.");
        }
    }
}
