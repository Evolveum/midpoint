/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller.fileformat;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;

import j2html.TagCreator;
import j2html.tags.ContainerTag;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates and manipulates exported reports in HTML format for dashboard reports.
 */
public class HtmlDashboardReportDataWriter extends HtmlReportDataWriter<ExportedDashboardReportDataRow, ExportedDashboardReportHeaderRow> {

    private static final String BASIC_WIDGET_ROW_KEY = "BaseWidgetID";

    /**
     * Data of one table (widget or objects searched by widget).
     */
    @NotNull private final Map<String, ExportedWidgetData> data = new LinkedHashMap<>();

    public HtmlDashboardReportDataWriter(ReportServiceImpl reportService, Map<String, CompiledObjectCollectionView> mapOfCompiledView) {
        super(reportService, null);
        setupDefaultExportedWidgetData(reportService);
        mapOfCompiledView.entrySet().forEach(entry -> {
            ExportedWidgetData widgetData = new ExportedWidgetData();
            widgetData.setSupport(new CommonHtmlSupport(reportService.getClock(), entry.getValue()));
            data.put(entry.getKey(), widgetData);
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
        data.entrySet().forEach(entry -> {
            sb.append("<"+entry.getKey()+">")
              .append(getStringDataInternal(entry.getValue().headerRow, entry.getValue().dataRows))
              .append("</"+entry.getKey()+">\n");
        });
        return sb.toString();
    }

    @Override
    public boolean shouldWriteHeader() {
        return true;
    }

    @Override
    public String completizeReport(String aggregatedData) {
        CommonHtmlSupport support = getDefaultSupport();
        String cssStyle = support.getCssStyle();

        StringBuilder body = new StringBuilder();
        body.append("<div> <style> ").append(cssStyle).append(" </style>");

        data.entrySet().forEach(entry -> {
            String tableData = Arrays.stream(
                    StringUtils.substringsBetween(aggregatedData, "<" +entry.getKey() + ">", "</" +entry.getKey() + ">"))
                    .collect(Collectors.joining());
            String tableBox = createTableBox(tableData, entry.getValue().support, true);
            body.append(tableBox).append("<br>");
        });

        body.append("</div>");

        return body.toString();
    }

    @Override
    public String completizeReport() {
        CommonHtmlSupport support = getDefaultSupport();
        String cssStyle = support.getCssStyle();

        StringBuilder body = new StringBuilder();
        body.append("<div> <style> ").append(cssStyle).append(" </style>");

        data.entrySet().forEach(entry -> {
            String tableData = getStringDataInternal(entry.getValue().headerRow, entry.getValue().dataRows);
            String tableBox = createTableBox(tableData, entry.getValue().support, false);
            body.append(tableBox).append("<br>");
        });

        body.append("</div>");

        return body.toString();
    }

    private CommonHtmlSupport getDefaultSupport() {
        return data.get(BASIC_WIDGET_ROW_KEY).support;
    }

    class ExportedWidgetData {

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
