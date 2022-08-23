/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller;

import java.util.*;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;

/**
 * Creates and manipulates exported reports in HTML format for dashboard reports.
 */
public class HtmlDashboardReportDataWriter
        extends HtmlReportDataWriter<ExportedDashboardReportDataRow, ExportedDashboardReportHeaderRow> implements DashboardReportDataWriter {

    private static final String BASIC_WIDGET_ROW_KEY = "BaseWidgetID";

    /**
     * Data of one table (widget or objects searched by widget).
     */
    @NotNull private final Map<String, ExportedWidgetData> data = new LinkedHashMap<>();

    /**
     * Data of widgets (number message).
     */
    @NotNull private final Map<String, String> widgetsData = new HashMap<>();

    public HtmlDashboardReportDataWriter(ReportServiceImpl reportService, Map<String,
            CompiledObjectCollectionView> mapOfCompiledView,
            @Nullable FileFormatConfigurationType configuration) {
        super(reportService, null, configuration);
        setupDefaultExportedWidgetData(reportService);
        mapOfCompiledView.forEach((key, value) -> {
            ExportedWidgetData widgetData = new ExportedWidgetData();
            widgetData.setSupport(new CommonHtmlSupport(reportService.getClock(), value));
            data.put(key, widgetData);
        });
    }

    private void setupDefaultExportedWidgetData(ReportServiceImpl reportService) {
        ExportedWidgetData widgetData = new ExportedWidgetData();
        CompiledObjectCollectionView compiledView = new CompiledObjectCollectionView();
        compiledView.setViewIdentifier(GenericSupport.getMessage(localizationService, CommonHtmlSupport.REPORT_WIDGET_TABLE_NAME));
        widgetData.setSupport(new CommonHtmlSupport(reportService.getClock(), compiledView));
        data.put(BASIC_WIDGET_ROW_KEY, widgetData);
    }

    @Override
    public void setHeaderRow(ExportedDashboardReportHeaderRow headerRow) {
        String widgetIdentifier = checkIdentifier(headerRow.isBasicWidgetRow(), headerRow.getWidgetIdentifier());
        data.get(widgetIdentifier).setHeaderRow(headerRow);
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
    public synchronized void appendDataRow(ExportedDashboardReportDataRow row) {
        String widgetIdentifier = checkIdentifier(row.isBasicWidgetRow(), row.getWidgetIdentifier());
        List<ExportedDashboardReportDataRow> rows = data.get(widgetIdentifier).dataRows;
        int i;
        for (i = rows.size() - 1; i >= 0; i--) {
            if (rows.get(i).getSequentialNumber() < row.getSequentialNumber()) {
                break;
            }
        }
        rows.add(i + 1, row);

        if (row.isBasicWidgetRow()) {
            widgetsData.put(
                    row.getWidgetIdentifier(),
                    String.join("", row.getValues().get(CommonHtmlSupport.getIndexOfNumberColumn())));
        }
    }

    private String checkIdentifier(boolean basicWidgetRow, String widgetIdentifier) {
        String resolvedWidgetIdentifier;
        if (basicWidgetRow) {
            if (!data.containsKey(BASIC_WIDGET_ROW_KEY)) {
                data.put(BASIC_WIDGET_ROW_KEY, new ExportedWidgetData());
            }
            resolvedWidgetIdentifier = BASIC_WIDGET_ROW_KEY;
        } else {
            resolvedWidgetIdentifier = widgetIdentifier;
        }
        if (!data.containsKey(resolvedWidgetIdentifier)) {
            throw new IllegalArgumentException("Unknown widget identifier " + widgetIdentifier);
        }
        return resolvedWidgetIdentifier;
    }

    @Override
    public String getStringData() {
        StringBuilder sb = new StringBuilder();
        data.forEach((key, value) ->
                sb.append("<")
                        .append(key)
                        .append(">")
                        .append(getStringDataInternal(value.headerRow, value.dataRows))
                        .append("</")
                        .append(key)
                        .append(">\n"));
        return sb.toString();
    }

    @Override
    public boolean shouldWriteHeader() {
        return true;
    }

    @Override
    public String completeReport(String aggregatedData) {
        CommonHtmlSupport support = getDefaultSupport();
        String cssStyle = support.getCssStyle();

        StringBuilder body = new StringBuilder();
        body.append("<div> <style> ").append(cssStyle).append(" </style>");

        data.forEach((key, value) -> {
            String tableData = String.join("",
                    StringUtils.substringsBetween(aggregatedData, "<" + key + ">", "</" + key + ">"));
            if (!BASIC_WIDGET_ROW_KEY.equals(key) && !tableData.contains("<tbody>")) {
                return;
            }
            String tableBox = createTableBox(tableData, value.support, true);
            body.append(tableBox).append("<br>");
        });

        body.append("</div>");

        String subscriptionFooter = reportService.missingSubscriptionFooter();
        if (subscriptionFooter != null) {
            body.append("<div>")
                    .append(subscriptionFooter)
                    .append("</div>");
        }

        return body.toString();
    }

    @Override
    public String completeReport() {
        CommonHtmlSupport support = getDefaultSupport();
        String cssStyle = support.getCssStyle();

        StringBuilder body = new StringBuilder();
        body.append("<div> <style> ").append(cssStyle).append(" </style>");

        data.forEach((key, value) -> {
            if (!BASIC_WIDGET_ROW_KEY.equals(key) && value.dataRows.isEmpty()) {
                return;
            }
            String tableData = getStringDataInternal(value.headerRow, value.dataRows);
            String tableBox = createTableBox(tableData, value.support, false);
            body.append(tableBox).append("<br>");
        });

        body.append("</div>");

        return body.toString();
    }

    private CommonHtmlSupport getDefaultSupport() {
        return data.get(BASIC_WIDGET_ROW_KEY).support;
    }

    @Override
    public @Nullable Function<String, String> getFunctionForWidgetStatus() {
        return (value) -> CommonHtmlSupport.VALUE_CSS_STYLE_TAG + "{background-color: " + value + " !important;}";
    }

    @Override
    @NotNull
    public Map<String, String> getWidgetsData() {
        return widgetsData;
    }

    static class ExportedWidgetData {

        List<ExportedDashboardReportDataRow> dataRows = new ArrayList<>();

        ExportedDashboardReportHeaderRow headerRow;

        CommonHtmlSupport support;

        public void setHeaderRow(ExportedDashboardReportHeaderRow headerRow) {
            this.headerRow = headerRow;
        }

        public void setSupport(CommonHtmlSupport support) {
            this.support = support;
        }
    }
}
