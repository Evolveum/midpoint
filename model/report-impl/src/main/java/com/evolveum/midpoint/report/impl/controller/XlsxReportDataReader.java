/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

/**
 * Reads import data from the first sheet of an XLSX workbook.
 *
 * Cells are read as displayed text (numbers and dates formatted by their cell format), so a file created by
 * a spreadsheet application and a file created by {@link XlsxReportDataWriter} are handled the same way.
 * Multiple values in one cell are separated by the configured multivalue delimiter (line break by default),
 * matching the XLSX export.
 * Rows without any value are skipped.
 */
public class XlsxReportDataReader implements ReportDataReader {

    @Nullable private final FileFormatConfigurationType configuration;

    public XlsxReportDataReader(@Nullable FileFormatConfigurationType configuration) {
        this.configuration = configuration;
    }

    @Override
    public @NotNull List<VariablesMap> read(@NotNull ReportDataType reportData, @NotNull List<String> viewHeaders)
            throws IOException {
        boolean fileHasHeader = CommonXlsxSupport.isHeader(configuration);
        String multivalueDelimiter = CommonXlsxSupport.getMultivalueDelimiter(configuration);
        // a line break in a cell may be CRLF, depending on the application that wrote the file
        String splitRegex = CommonXlsxSupport.LINE_BREAK.equals(multivalueDelimiter) ?
                "\r?\n" : Pattern.quote(multivalueDelimiter);
        if (viewHeaders.isEmpty() && !fileHasHeader) {
            throw new IllegalArgumentException("Couldn't find headers please "
                    + "define them via view element or write them to the first row of the sheet and set "
                    + "header element in file format configuration to true.");
        }

        File file = new File(reportData.getFilePath());
        List<VariablesMap> variablesMaps = new ArrayList<>();
        try (Workbook workbook = openWorkbook(file)) {
            if (workbook.getNumberOfSheets() == 0) {
                return variablesMaps;
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            List<String> headers = new ArrayList<>(viewHeaders);
            boolean headerPending = fileHasHeader;
            for (Row row : sheet) {
                List<String> cells = readCells(row, formatter);
                if (cells.stream().allMatch(StringUtils::isEmpty)) {
                    continue;
                }
                if (headerPending) {
                    headerPending = false;
                    if (headers.isEmpty()) {
                        headers.addAll(cells);
                    }
                    continue;
                }
                VariablesMap variables = new VariablesMap();
                for (int i = 0; i < headers.size(); i++) {
                    String name = headers.get(i);
                    if (StringUtils.isEmpty(name)) {
                        continue;
                    }
                    String value = i < cells.size() ? StringUtils.defaultIfEmpty(cells.get(i), null) : null;
                    if (value != null && value.contains(multivalueDelimiter)) {
                        variables.put(name, Arrays.asList(value.split(splitRegex)), String.class);
                    } else {
                        variables.put(name, value, String.class);
                    }
                }
                variablesMaps.add(variables);
            }
        }
        return variablesMaps;
    }

    private static Workbook openWorkbook(File file) throws IOException {
        try {
            return WorkbookFactory.create(file, null, true);
        } catch (NotOfficeXmlFileException e) {
            throw new IOException("File " + file + " is not an XLSX file: " + e.getMessage(), e);
        }
    }

    /** Values of all cells up to the last defined one; missing cells yield empty strings. */
    private static List<String> readCells(Row row, DataFormatter formatter) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            values.add(cell != null ? formatter.formatCellValue(cell) : "");
        }
        return values;
    }
}
