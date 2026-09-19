/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generally useful methods for writing XLSX report files.
 * To be used by XLSX report writers.
 *
 * Wraps a streaming ({@link SXSSFWorkbook}) workbook, so only a window of rows is kept in memory
 * while writing; the rest is flushed to a temporary file.
 *
 * All values are written as string cells.
 */
class CommonXlsxSupport implements AutoCloseable {

    private static final int SHEET_DEFAULT_COLUMN_WIDTH = 5000;
    private static final int ROW_ACCESS_WINDOW_SIZE = 100;
    private static final String DEFAULT_SHEET_NAME = "Report";
    /** Excel limit for sheet names; POI enforces it in {@link WorkbookUtil#createSafeSheetName(String)}. */
    private static final int MAX_SHEET_NAME_LENGTH = 31;

    @NotNull private final SXSSFWorkbook workbook;
    @NotNull private final CellStyle wrappedStyle;
    @NotNull private final Set<String> sheetNames = new HashSet<>();

    CommonXlsxSupport() {
        workbook = new SXSSFWorkbook(ROW_ACCESS_WINDOW_SIZE);
        wrappedStyle = workbook.createCellStyle();
        wrappedStyle.setWrapText(true);
    }

    /** Creates a sheet with a name derived from the given title; the name is made safe and unique. */
    @NotNull Sheet createSheet(@Nullable String title) {
        String base = WorkbookUtil.createSafeSheetName(StringUtils.defaultIfBlank(title, DEFAULT_SHEET_NAME));
        String name = base;
        for (int i = 2; !sheetNames.add(name); i++) {
            String suffix = " (" + i + ")";
            name = WorkbookUtil.createSafeSheetName(
                    StringUtils.abbreviate(base, "", MAX_SHEET_NAME_LENGTH - suffix.length()) + suffix);
        }
        return workbook.createSheet(name);
    }

    /** Writes a single-cell row, e.g. a title or a footer. */
    void writeTextRow(@NotNull Sheet sheet, int rowIndex, @NotNull String text) {
        sheet.createRow(rowIndex).createCell(0).setCellValue(text);
    }

    void writeHeader(@NotNull Sheet sheet, int rowIndex, @NotNull List<String> labels) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < labels.size(); i++) {
            sheet.setColumnWidth(i, SHEET_DEFAULT_COLUMN_WIDTH);
            row.createCell(i).setCellValue(labels.get(i));
        }
    }

    /** Writes a data row. Multiple values of a column go into one cell, each on its own line. */
    void writeDataRow(@NotNull Sheet sheet, int rowIndex, @NotNull List<List<String>> values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.size(); i++) {
            Cell cell = row.createCell(i);
            String text = String.join("\n", values.get(i));
            cell.setCellValue(text);
            if (text.contains("\n")) {
                cell.setCellStyle(wrappedStyle);
            }
        }
    }

    void write(@NotNull OutputStream outputStream) throws IOException {
        workbook.write(outputStream);
    }

    @Override
    public void close() throws IOException {
        try {
            workbook.close();
        } finally {
            workbook.dispose(); // deletes the temporary files of the streaming workbook
        }
    }
}
