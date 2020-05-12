/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller.export;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.interaction.DashboardService;
import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.model.api.util.DashboardUtils;
import com.evolveum.midpoint.model.api.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import j2html.TagCreator;
import j2html.tags.ContainerTag;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author skublik
 */

public class HtmlExportController extends ExportController {

    private static final Trace LOGGER = TraceManager.getTrace(HtmlExportController.class);

    private static final String REPORT_CSS_STYLE_FILE_NAME = "dashboard-report-style.css";

    private static final String REPORT_GENERATED_ON = "Widget.generatedOn";
    private static final String NUMBER_OF_RECORDS = "Widget.numberOfRecords";

    public HtmlExportController(ExportConfigurationType exportConfiguration, ReportServiceImpl reportService) {
        super(exportConfiguration, reportService);
    }

    @Override
    public byte[] processDashboard(DashboardReportEngineConfigurationType dashboardConfig, Task task, OperationResult result) throws Exception {
        ObjectReferenceType ref = dashboardConfig.getDashboardRef();
        Class<ObjectType> type = getReportService().getPrismContext().getSchemaRegistry().determineClassForType(ref.getType());
        DashboardType dashboard = (DashboardType) getReportService().getModelService()
                .getObject(type, ref.getOid(), null, task, result)
                .asObjectable();

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream in = classLoader.getResourceAsStream(REPORT_CSS_STYLE_FILE_NAME);
        if (in == null) {
            throw new IllegalStateException("Resource " + REPORT_CSS_STYLE_FILE_NAME + " couldn't be found");
        }
        byte[] data = IOUtils.toByteArray(in);
        String cssStyle = new String(data, Charset.defaultCharset());

        StringBuilder body = new StringBuilder();
        body.append("<div> <style> ").append(cssStyle).append(" </style>");

        ContainerTag widgetTable = createTable();
        widgetTable.with(createTHead("Widget.", getHeadsOfWidget()));

        ContainerTag widgetTBody = TagCreator.tbody();
        List<ContainerTag> tableboxesFromWidgets = new ArrayList<>();
        long startMillis = getReportService().getClock().currentTimeMillis();
        long i = 0;
        task.setExpectedTotal((long) dashboard.getWidget().size());
        recordProgress(task, i, result, LOGGER);
        for (DashboardWidgetType widget : dashboard.getWidget()) {
            DashboardWidget widgetData = getReportService().getDashboardService().createWidgetData(widget, task, result);
            widgetTBody.with(createTBodyRow(widgetData));
            if (widget == null) {
                throw new IllegalArgumentException("Widget in DashboardWidget is null");
            }
            DashboardWidgetPresentationType presentation = widget.getPresentation();
            if (!DashboardUtils.isDataFieldsOfPresentationNullOrEmpty(presentation)) {
                ContainerTag tableBox = null;
                DashboardWidgetSourceTypeType sourceType = DashboardUtils.getSourceType(widget);
                if (sourceType == null) {
                    throw new IllegalStateException("No source type specified in " + widget);
                }
                ObjectCollectionType collection = getReportService().getDashboardService().getObjectCollectionType(widget, task, result);
                CompiledObjectCollectionView compiledCollection = new CompiledObjectCollectionView();
                getReportService().getModelInteractionService().compileView(compiledCollection, collection.getDefaultView());

                if(!useDefaultColumn(widget)) {
                    getReportService().getModelInteractionService().compileView(compiledCollection, widget.getPresentation().getView());
                }
                QName collectionType = collection.getAuditSearch() != null ? AuditEventRecordType.COMPLEX_TYPE : collection.getType();
                GuiObjectListViewType reportView = getReportViewByType(dashboardConfig, collectionType);
                if (reportView != null) {
                    getReportService().getModelInteractionService().compileView(compiledCollection, reportView);
                }
                switch (sourceType) {
                    case OBJECT_COLLECTION:
                        tableBox = createTableBoxForObjectView(widgetData.getLabel(), collection, compiledCollection, task, result);
                        break;
                    case AUDIT_SEARCH:
                        tableBox = createTableBoxForAuditView(widgetData.getLabel(), collection, compiledCollection, task, result);
                        break;
                }
                if (tableBox != null) {
                    tableboxesFromWidgets.add(tableBox);
                }
            }
            i++;
            recordProgress(task, i, result, LOGGER);
        }
        widgetTable.with(widgetTBody);

        body.append(createTableBox(widgetTable, "Widgets", dashboard.getWidget().size(),
                convertMillisToString(startMillis), null).render());
        appendNewLine(body);
        tableboxesFromWidgets.forEach(table -> {
            body.append(table.render());
            appendNewLine(body);
        });
        body.append("</div>");

        return body.toString().getBytes();
    }

    private GuiObjectListViewType getReportViewByType(DashboardReportEngineConfigurationType dashboardConfig, QName type) {
        for (GuiObjectListViewType view : dashboardConfig.getView()) {
            if (QNameUtil.match(view.getType(), type)) {
                return view;
            }
        }
        return null;
    }

    @Override
    public byte[] processView() throws Exception {
        return new byte[0];
    }

    private ContainerTag createTable() {
        return TagCreator.table().withClasses("table", "table-striped", "table-hover", "table-bordered");
    }

    private ContainerTag createTable(CompiledObjectCollectionView compiledCollection, List<PrismObject<ObjectType>> values,
            PrismObjectDefinition<ObjectType> def, Class<? extends ObjectType> type,
            Task task, OperationResult result) {
        ContainerTag table = createTable();
        ContainerTag tHead = TagCreator.thead();
        ContainerTag tBody = TagCreator.tbody();
        List<GuiObjectColumnType> columns = MiscSchemaUtil.orderCustomColumns(compiledCollection.getColumns());
        ContainerTag trForHead = TagCreator.tr().withStyle("width: 100%;");
        columns.forEach(column -> {
            Validate.notNull(column.getName(), "Name of column is null");
            ItemPath path = column.getPath() == null ? null : column.getPath().getItemPath();

            DisplayType columnDisplay = column.getDisplay();
            String label;
            if(columnDisplay != null && columnDisplay.getLabel() != null) {
                label = getMessage(columnDisplay.getLabel().getOrig());
            } else  {

                label = getColumnLabel(column.getName(), def, path);
            }
            ContainerTag th = TagCreator.th(TagCreator.div(TagCreator.span(label).withClass("sortableLabel")));
            if(columnDisplay != null) {
                if(StringUtils.isNotBlank(columnDisplay.getCssClass())) {
                    th.withClass(columnDisplay.getCssClass());
                }
                if(StringUtils.isNotBlank(columnDisplay.getCssStyle())) {
                    th.withStyle(columnDisplay.getCssStyle());
                }
            }
            trForHead.with(th);
        });
        tHead.with(trForHead);
        table.with(tHead);

        values.forEach(value -> {
            ContainerTag tr = TagCreator.tr();
            columns.forEach(column -> {
                ItemPath path = column.getPath() == null ? null : column.getPath().getItemPath();
                ExpressionType expression = column.getExpression();
                tr.with(TagCreator
                        .th(TagCreator.div(getRealValueAsString(column, value, path, expression, task, result))
                                .withStyle("white-space: pre-wrap")));
            });
            tBody.with(tr);
        });
        table.with(tBody);
        return table;
    }

    private ContainerTag createTable(CompiledObjectCollectionView compiledCollection, List<AuditEventRecord> records, Task task,
            OperationResult result) {
        ContainerTag table = createTable();
        ContainerTag tHead = TagCreator.thead();
        ContainerTag tBody = TagCreator.tbody();
        List<GuiObjectColumnType> columns = MiscSchemaUtil.orderCustomColumns(compiledCollection.getColumns());
        ContainerTag trForHead = TagCreator.tr().withStyle("width: 100%;");
        columns.forEach(column -> {
            Validate.notNull(column.getName(), "Name of column is null");

            DisplayType columnDisplay = column.getDisplay();
            String label;
            if(columnDisplay != null && columnDisplay.getLabel() != null) {
                label = getMessage(columnDisplay.getLabel().getOrig());
            } else {
                label = column.getName();
            }
            ContainerTag th = TagCreator.th(TagCreator.div(TagCreator.span(label).withClass("sortableLabel")));
            if(columnDisplay != null) {
                if(StringUtils.isNotBlank(columnDisplay.getCssClass())) {
                    th.withClass(columnDisplay.getCssClass());
                }
                if(StringUtils.isNotBlank(columnDisplay.getCssStyle())) {
                    th.withStyle(columnDisplay.getCssStyle());
                }
            }
            trForHead.with(th);
        });
        tHead.with(trForHead);
        table.with(tHead);

        records.forEach(record -> {
            ContainerTag tr = TagCreator.tr();
            columns.forEach(column -> {
                ExpressionType expression = column.getExpression();
                ItemPath path = column.getPath() == null ? null : column.getPath().getItemPath();
                tr.with(TagCreator
                        .th(TagCreator.div(getStringValueByAuditColumn(record, path, expression, task, result))
                                .withStyle("white-space: pre-wrap")));
            });
            tBody.with(tr);
        });
        table.with(tBody);
        return table;
    }

    private ContainerTag createTableBox(ContainerTag table, String nameOfTable, int countOfTableRecords,
            String createdTime, DisplayType display) {
        ContainerTag div = TagCreator.div().withClasses("box-body", "no-padding").with(TagCreator.h1(nameOfTable))
                .with(TagCreator.p(getMessage(REPORT_GENERATED_ON, createdTime)))
                .with(TagCreator.p(getMessage(NUMBER_OF_RECORDS, countOfTableRecords))).with(table);
        String style = "";
        String classes = "";
        if(display != null) {
            if(display.getCssStyle() != null) {
                style = display.getCssStyle();
            }
            if(display.getCssClass() != null) {
                classes = display.getCssClass();
            }
        }
        return TagCreator.div().withClasses("box", "boxed-table", classes).withStyle(style).with(div);
    }

    private ContainerTag createTableBoxForObjectView(String label, ObjectCollectionType collection, @NotNull CompiledObjectCollectionView compiledCollection,
            Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        long startMillis = getReportService().getClock().currentTimeMillis();
        Class<ObjectType> type = (Class<ObjectType>) getReportService().getPrismContext().getSchemaRegistry()
                .getCompileTimeClassForObjectType(collection.getType());
        Collection<SelectorOptions<GetOperationOptions>> options = DefaultColumnUtils.createOption(type, getReportService().getSchemaHelper());
        List<PrismObject<ObjectType>> values = getReportService().getDashboardService().searchObjectFromCollection(collection, true, options, task, result);
        if (values == null || values.isEmpty()) {
            return null;
        }
        PrismObjectDefinition<ObjectType> def = values.get(0).getDefinition();
        //noinspection unchecked

        if (compiledCollection.getColumns().isEmpty()) {
            getReportService().getModelInteractionService().compileView(compiledCollection, DefaultColumnUtils.getDefaultColumns(type));
        }
        ContainerTag table = createTable(compiledCollection, values, def, type, task, result);
        DisplayType display = compiledCollection.getDisplay();
        return createTableBox(table, label, values.size(),
                convertMillisToString(startMillis), display);
    }

    private ContainerTag createTableBoxForAuditView(String label, ObjectCollectionType collection, @NotNull CompiledObjectCollectionView compiledCollection,
            Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Map<String, Object> parameters = new HashMap<>();
        long startMillis = getReportService().getClock().currentTimeMillis();
        String query = DashboardUtils
                .getQueryForListRecords(DashboardUtils.createQuery(collection, parameters, false, getReportService().getClock()));
        List<AuditEventRecord> records = getReportService().getAuditService().listRecords(query, parameters, result);
        if (records == null || records.isEmpty()) {
            return null;
        }

//        ContainerTag table = createTable();
        if (compiledCollection.getColumns().isEmpty()) {
            getReportService().getModelInteractionService().compileView(compiledCollection, DefaultColumnUtils.getDefaultAuditEventsView());
        }
//        if(!useDefaultColumn(widget)) {
//            table = createTable(widget.getPresentation().getView(), records, task, result);
//            display = widget.getPresentation().getView().getDisplay();
//        } else {
//            table.with(createTHead(getHeadsOfAuditEventRecords()));
//            ContainerTag tBody = TagCreator.tbody();
//            records.forEach(record -> {
//                ContainerTag tr = TagCreator.tr();
//                getHeadsOfAuditEventRecords().forEach(column -> {
//                    tr.with(TagCreator.th(TagCreator.div(getStringForColumnOfAuditRecord(column, record, null)).withStyle("white-space: pre-wrap")));
//                });
//                tBody.with(tr);
//            });
//            table.with(tBody);
//        }
        ContainerTag table = createTable(compiledCollection, records, task, result);
        DisplayType display = compiledCollection.getDisplay();
        return createTableBox(table, label, records.size(),
                convertMillisToString(startMillis), display);
    }

    private ContainerTag createTHead(String prefix, Set<String> set) {
        return TagCreator.thead(TagCreator.tr(TagCreator.each(set,
                header -> TagCreator.th(TagCreator.div(TagCreator.span(getMessage(prefix+header)).withClass("sortableLabel")))))
                    .withStyle("width: 100%;"));
//                header -> TagCreator.th(TagCreator.div(TagCreator.span(getMessage(prefix+header)).withClass("sortableLabel")))
//                        .withStyle(getStyleForColumn(header)))).withStyle("width: 100%;"));
    }

    private ContainerTag createTBodyRow(DashboardWidget data) {
        return TagCreator.tr(TagCreator.each(getHeadsOfWidget(), header -> getContainerTagForWidgetHeader(header, data)

        ));

    }

    private ContainerTag getContainerTagForWidgetHeader(String header, DashboardWidget data) {
        if (header.equals(LABEL_COLUMN)) {
            ContainerTag div = TagCreator.div(data.getLabel());
            return TagCreator.th().with(div);
        }
        if (header.equals(NUMBER_COLUMN)) {
            ContainerTag div = TagCreator.div(data.getNumberMessage());
            return TagCreator.th().with(div);
        }
        if (header.equals(STATUS_COLUMN)) {
            ContainerTag div = TagCreator.div().withStyle("width: 100%; height: 20px; ");
            ContainerTag th = TagCreator.th();
            if (data.getDisplay() != null && StringUtils.isNoneBlank(data.getDisplay().getColor())) {
                th.withStyle("background-color: " + data.getDisplay().getColor() + "; !important;");
            }
            th.with(div);
            return th;
        }
        return TagCreator.th();
    }

    private boolean useDefaultColumn(DashboardWidgetType widget) {
        return widget.getPresentation() == null || widget.getPresentation().getView() == null
                || widget.getPresentation().getView().getColumn() == null || widget.getPresentation().getView().getColumn().isEmpty();
    }

    private String convertMillisToString(long millis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss", Locale.getDefault());
        return dateFormat.format(millis);
    }

    protected void appendNewLine(StringBuilder body) {
        body.append("<br>");
    }

    @Override
    public String getTypeSuffix() {
        return ".html";
    }

    @Override
    public String getType() {
        return "HTML";
    }
}
