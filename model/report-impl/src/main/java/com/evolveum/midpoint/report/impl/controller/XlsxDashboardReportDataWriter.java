/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;

/** Creates exported dashboard reports in XLSX format. */
public class XlsxDashboardReportDataWriter
        extends AbstractReportDataWriter<ExportedDashboardReportDataRow, ExportedDashboardReportHeaderRow>
        implements DashboardReportDataWriter {

    private static final String BASIC_WIDGET_ROW_KEY = "BaseWidgetID";
    private static final int SHEET_DEFAULT_COLUMN_WIDTH = 5000;

    @NotNull private final Map<String, ExportedWidgetData> data = new LinkedHashMap<>();
    @NotNull private final Map<String, String> widgetsData = new HashMap<>();
    @Nullable private final FileFormatConfigurationType configuration;

    public XlsxDashboardReportDataWriter(
            @NotNull ReportServiceImpl reportService,
            @NotNull Map<String, CompiledObjectCollectionView> mapOfCompiledView,
            @Nullable FileFormatConfigurationType configuration) {
        super(reportService);
        this.configuration = configuration;

        data.put(
                BASIC_WIDGET_ROW_KEY,
                new ExportedWidgetData(GenericSupport.getMessage(
                                reportService.getLocalizationService(), CommonHtmlSupport.REPORT_WIDGET_TABLE_NAME)));
        mapOfCompiledView.forEach((identifier, compiledView) ->
                data.put(identifier,
                        new ExportedWidgetData(
                                new CommonHtmlSupport(reportService.getClock(), compiledView)
                                        .getTableName(reportService.getLocalizationService()))));
    }

    @Override
    public void setHeaderRow(ExportedDashboardReportHeaderRow headerRow) {
        getWidgetData(headerRow.isBasicWidgetRow(), headerRow.getWidgetIdentifier()).headerRow = headerRow;
    }

    @Override
    public synchronized void appendDataRow(ExportedDashboardReportDataRow row) {
        List<ExportedDashboardReportDataRow> rows = getWidgetData(row.isBasicWidgetRow(), row.getWidgetIdentifier()).dataRows;
        int rowIndex;
        for (rowIndex = rows.size() - 1; rowIndex >= 0; rowIndex--) {
            if (rows.get(rowIndex).getSequentialNumber() <= row.getSequentialNumber()) {
                break;
            }
        }
        rows.add(rowIndex + 1, row);

        if (row.isBasicWidgetRow()) {
            widgetsData.put(
                    row.getWidgetIdentifier(),
                    String.join("", row.getValues().get(CommonHtmlSupport.getIndexOfNumberColumn())));
        }
    }

    private ExportedWidgetData getWidgetData(boolean basicWidgetRow, @Nullable String widgetIdentifier) {
        String resolvedIdentifier = basicWidgetRow ? BASIC_WIDGET_ROW_KEY : widgetIdentifier;
        ExportedWidgetData widgetData = data.get(resolvedIdentifier);
        if (widgetData == null) {
            throw new IllegalArgumentException("Unknown widget identifier " + widgetIdentifier);
        }
        return widgetData;
    }

    @Override
    public void writeCompletedReport(@NotNull OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            CellStyle wrappedStyle = workbook.createCellStyle();
            wrappedStyle.setWrapText(true);

            int rowIndex = 0;
            boolean firstSection = true;
            for (Map.Entry<String, ExportedWidgetData> entry : data.entrySet()) {
                ExportedWidgetData widgetData = entry.getValue();
                if (!BASIC_WIDGET_ROW_KEY.equals(entry.getKey()) && widgetData.dataRows.isEmpty()) {
                    continue;
                }
                if (!firstSection) {
                    rowIndex++;
                }
                firstSection = false;

                sheet.createRow(rowIndex++).createCell(0).setCellValue(widgetData.title);
                if (widgetData.headerRow != null) {
                    Row header = sheet.createRow(rowIndex++);
                    List<String> labels = widgetData.headerRow.getLabels();
                    for (int i = 0; i < labels.size(); i++) {
                        sheet.setColumnWidth(i, SHEET_DEFAULT_COLUMN_WIDTH);
                        header.createCell(i).setCellValue(labels.get(i));
                    }
                }
                for (ExportedDashboardReportDataRow dataRow : widgetData.dataRows) {
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

    @Override
    public @NotNull Map<String, String> getWidgetsData() {
        return widgetsData;
    }

    private static class ExportedWidgetData {

        private final String title;
        private final List<ExportedDashboardReportDataRow> dataRows = new ArrayList<>();
        private ExportedDashboardReportHeaderRow headerRow;

        private ExportedWidgetData(String title) {
            this.title = title;
        }
    }
}
