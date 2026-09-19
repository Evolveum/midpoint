/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;

/** Creates exported reports in XLSX format. */
public class XlsxReportDataWriter
        extends AbstractReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow> {

    private static final int SHEET_DEFAULT_COLUMN_WIDTH = 5000;

    @Nullable private final FileFormatConfigurationType configuration;

    public XlsxReportDataWriter(
            @NotNull ReportServiceImpl reportService,
            @Nullable FileFormatConfigurationType configuration) {
        super(reportService);
        this.configuration = configuration;
    }

    @Override
    public void writeCompletedReport(@NotNull OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            CellStyle wrappedStyle = workbook.createCellStyle();
            wrappedStyle.setWrapText(true);

            int rowIndex = 0;
            ExportedReportHeaderRow headerRow = getHeaderRow();
            if (headerRow != null) {
                Row row = sheet.createRow(rowIndex++);
                List<String> labels = headerRow.getLabels();
                for (int i = 0; i < labels.size(); i++) {
                    sheet.setColumnWidth(i, SHEET_DEFAULT_COLUMN_WIDTH);
                    row.createCell(i).setCellValue(labels.get(i));
                }
            }

            for (ExportedReportDataRow dataRow : getDataRows()) {
                Row row = sheet.createRow(rowIndex++);
                List<List<String>> values = dataRow.getValues();
                for (int i = 0; i < values.size(); i++) {
                    var cell = row.createCell(i);
                    cell.setCellValue(String.join("\n", values.get(i)));
                    if (values.get(i).size() > 1) {
                        cell.setCellStyle(wrappedStyle);
                    }
                }
            }

            String subscriptionFooter = reportService.missingSubscriptionFooter();
            if (subscriptionFooter != null) {
                sheet.createRow(rowIndex).createCell(0).setCellValue(subscriptionFooter);
            }

            workbook.write(outputStream);
        }
    }

    @Override
    public boolean shouldWriteHeader() {
        return true;
    }

    @Override
    public String getTypeSuffix() {
        return ".xlsx";
    }

    @Override
    public String getType() {
        return "XLSX";
    }

    @Override
    public FileFormatConfigurationType getFileFormatConfiguration() {
        return configuration;
    }
}
