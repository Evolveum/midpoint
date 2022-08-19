/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller;

import java.util.List;

import j2html.TagCreator;
import j2html.tags.ContainerTag;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;

/**
 * Creates and manipulates exported reports in HTML format.
 */
public class HtmlReportDataWriter<ED extends ExportedReportDataRow, EH extends ExportedReportHeaderRow>
        extends AbstractReportDataWriter<ED, EH> {

    @NotNull private final CommonHtmlSupport support;

    final LocalizationService localizationService;

    @Nullable private final FileFormatConfigurationType configuration;

    public HtmlReportDataWriter(
            ReportServiceImpl reportService,
            CompiledObjectCollectionView compiledView,
            @Nullable FileFormatConfigurationType configuration) {
        super(reportService);
        this.support = new CommonHtmlSupport(reportService.getClock(), compiledView);
        this.localizationService = reportService.getLocalizationService();
        this.configuration = configuration;
    }

    @Override
    public String getStringData() {
        return getStringDataInternal(getHeaderRow(), getDataRows());
    }

    protected String getStringDataInternal(ExportedReportHeaderRow headerRow, List<ED> dataRows) {
        StringBuilder sb = new StringBuilder();
        if (headerRow != null) {
            sb.append(createTHead(headerRow));
        }
        ContainerTag tBody = TagCreator.tbody();
        dataRows.forEach(row -> {
            ContainerTag tr = TagCreator.tr();
            row.getValues().forEach(values -> {
                if (values.size() == 1 && values.iterator().next().startsWith(CommonHtmlSupport.VALUE_CSS_STYLE_TAG)) {
                    String value = values.iterator().next();
                    tr.with(TagCreator.th().withStyle(value.substring((value.indexOf("{") + 1), value.indexOf("}"))));
                } else {
                    tr.with(TagCreator.th(formatColumn(values)));
                }
            });
            tBody.with(tr);
        });
        if (tBody.getNumChildren() != 0) {
            sb.append(tBody.render());
        }
        return sb.toString();
    }

    @Override
    public boolean shouldWriteHeader() {
        return true;
    }

    private String createTHead(ExportedReportHeaderRow headerRow) {
        ContainerTag trForHead = TagCreator.tr().withStyle("width: 100%;");

        headerRow.getColumns().forEach(column -> {
            ContainerTag th = TagCreator.th(TagCreator.div(TagCreator.span(column.getLabel()).withClass("sortableLabel")));
            th.withClass(column.getCssClass());
            th.withStyle(column.getCssStyle());
            trForHead.with(th);
        });
        return TagCreator.thead(trForHead).render();
    }

    private ContainerTag formatColumn(List<String> values) {
        ContainerTag div = TagCreator.div().withStyle("white-space: pre-wrap");
        values.forEach((value) -> {
            if (div.getNumChildren() != 0) {
                div.with(TagCreator.br());
            }
            div.withText(value);
        });
        return div;
    }

    @Override
    public String completeReport(String aggregatedData) {
        return completeReportInternal(aggregatedData, true);
    }

    @Override
    public String completeReport() {
        return completeReportInternal(getStringData(), false);
    }

    @Override
    public String getTypeSuffix() {
        return ".html";
    }

    @Override
    public String getType() {
        return "HTML";
    }

    @Override
    public FileFormatConfigurationType getFileFormatConfiguration() {
        return configuration;
    }

    private String completeReportInternal(String aggregatedData, boolean parseData) {
        String cssStyle = support.getCssStyle();

        StringBuilder body = new StringBuilder();
        body.append("<div> <style> ").append(cssStyle).append(" </style>");

        String table = createTableBox(aggregatedData, support, parseData);
        body.append(table).append("</div>");

        // This is used by exported HTML report only. We probably don't want to add this to
        // the Dashboard reports, because there is a footer in the GUI already.
        String subscriptionFooter = reportService.missingSubscriptionFooter();
        if (subscriptionFooter != null) {
            body.append("<div>")
                    .append(subscriptionFooter)
                    .append("</div>");
        }

        return body.toString();
    }

    protected String createTableBox(String aggregatedData, CommonHtmlSupport support, boolean parseData) {
        StringBuilder table = new StringBuilder();
        String style = support.getCssStyleOfTable();
        String classes = support.getCssClassOfTable();

        ContainerTag div = TagCreator.div().withClasses("box-body", "no-padding").with(TagCreator.h1(support.getTableName(localizationService)))
                .with(TagCreator.p(GenericSupport.getMessage(localizationService, CommonHtmlSupport.REPORT_GENERATED_ON, support.getActualTime())));

        String tableBox = TagCreator.div().withClasses("box", "boxed-table", classes).withStyle(style).with(div).render();
        tableBox = tableBox.substring(0, tableBox.length() - 6);

        String parsedData;
        if (parseData) {
            parsedData = parseAggregatedData(aggregatedData);
        } else {
            parsedData = aggregatedData;
        }

        table.append(tableBox)
                .append("<table class=\"table table-striped table-hover table-bordered\">")
                .append(parsedData)
                .append("</table>")
                .append("</div>");
        return table.toString();
    }

    private String parseAggregatedData(String aggregatedData) {
        if (StringUtils.isEmpty(aggregatedData)) {
            return aggregatedData;
        }
        StringBuilder sb = new StringBuilder();
        String formattedData = aggregatedData;
        if (aggregatedData.contains("</thead>")) {
            String tHeader = aggregatedData.substring(0, aggregatedData.indexOf("</thead>") + 8);
            sb.append(tHeader);
            formattedData = formattedData.replace(tHeader, "");
        }
        sb.append("<tbody>");
        sb.append(formattedData.replaceAll("<tbody>", "").replaceAll("</tbody>", ""));
        sb.append("</tbody>");
        return sb.toString();
    }
}
