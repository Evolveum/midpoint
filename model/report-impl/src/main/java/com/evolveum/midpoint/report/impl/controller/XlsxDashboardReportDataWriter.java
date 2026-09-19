/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import org.apache.poi.ss.usermodel.Sheet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatTypeType;

/**
 * Creates exported dashboard reports in XLSX format.
 *
 * The workbook has one sheet with the widget summary (label, number, status)
 * followed by one sheet per widget that has table data.
 */
public class XlsxDashboardReportDataWriter
        extends AbstractReportDataWriter<ExportedDashboardReportDataRow, ExportedDashboardReportHeaderRow>
        implements DashboardReportDataWriter {

    private static final String BASIC_WIDGET_ROW_KEY = "BaseWidgetID";

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
        try (CommonXlsxSupport support = new CommonXlsxSupport()) {
            Sheet summarySheet = null;
            int summaryNextRow = 0;
            for (Map.Entry<String, ExportedWidgetData> entry : data.entrySet()) {
                boolean summary = BASIC_WIDGET_ROW_KEY.equals(entry.getKey());
                ExportedWidgetData widgetData = entry.getValue();
                if (!summary && widgetData.dataRows.isEmpty()) {
                    continue;
                }
                Sheet sheet = support.createSheet(widgetData.title);
                int nextRow = writeWidgetData(support, sheet, widgetData);
                if (summary) {
                    summarySheet = sheet;
                    summaryNextRow = nextRow;
                }
            }

            String subscriptionFooter = reportService.missingSubscriptionFooter();
            if (subscriptionFooter != null && summarySheet != null) {
                support.writeTextRow(summarySheet, summaryNextRow, subscriptionFooter);
            }

            support.write(outputStream);
        }
    }

    /** Returns the index of the first row after the written data. */
    private int writeWidgetData(CommonXlsxSupport support, Sheet sheet, ExportedWidgetData widgetData) {
        int rowIndex = 0;
        if (widgetData.headerRow != null) {
            support.writeHeader(sheet, rowIndex++, widgetData.headerRow.getLabels());
        }
        for (ExportedDashboardReportDataRow dataRow : widgetData.dataRows) {
            support.writeDataRow(sheet, rowIndex++, dataRow.getValues());
        }
        return rowIndex;
    }

    @Override
    public boolean shouldWriteHeader() {
        return true;
    }

    @Override
    public @NotNull FileFormatTypeType getFileFormatType() {
        return FileFormatTypeType.XLSX;
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
