/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.task.api.RunningTask;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

import j2html.TagCreator;
import j2html.tags.ContainerTag;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.model.api.util.DashboardUtils;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */

public class HtmlController extends FileFormatController {

    private static final Trace LOGGER = TraceManager.getTrace(HtmlController.class);

    private static final String REPORT_CSS_STYLE_FILE_NAME = "dashboard-report-style.css";

    private static final String REPORT_GENERATED_ON = "Widget.generatedOn";
    private static final String NUMBER_OF_RECORDS = "Widget.numberOfRecords";

    public HtmlController(FileFormatConfigurationType fileFormatConfiguration, ReportType report, ReportServiceImpl reportService) {
        super(fileFormatConfiguration, report, reportService);
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
            if (widget == null) {
                throw new IllegalArgumentException("Widget in DashboardWidget is null");
            }
            DashboardWidget widgetData = getReportService().getDashboardService().createWidgetData(widget, task, result);
            widgetTBody.with(createTBodyRow(widgetData));
            if (!Boolean.TRUE.equals(dashboardConfig.isShowOnlyWidgetsTable())) {
                DashboardWidgetPresentationType presentation = widget.getPresentation();
                if (!DashboardUtils.isDataFieldsOfPresentationNullOrEmpty(presentation)) {
                    ContainerTag tableBox = null;
                    DashboardWidgetSourceTypeType sourceType = DashboardUtils.getSourceType(widget);
                    if (sourceType == null) {
                        throw new IllegalStateException("No source type specified in " + widget);
                    }
                    CollectionRefSpecificationType collectionRefSpecification = getReportService().getDashboardService().getCollectionRefSpecificationType(widget, task, result);
                    ObjectCollectionType collection = getReportService().getDashboardService().getObjectCollectionType(widget, task, result);

                    CompiledObjectCollectionView compiledCollection = new CompiledObjectCollectionView();
                    if (collection != null) {
                        getReportService().getModelInteractionService().applyView(compiledCollection, collection.getDefaultView());
                    } else if (collectionRefSpecification.getBaseCollectionRef() != null
                            && collectionRefSpecification.getBaseCollectionRef().getCollectionRef() != null) {
                        ObjectCollectionType baseCollection = (ObjectCollectionType) getObjectFromReference(collectionRefSpecification.getBaseCollectionRef().getCollectionRef()).asObjectable();
                        getReportService().getModelInteractionService().applyView(compiledCollection, baseCollection.getDefaultView());
                    }

                    if (widget.getPresentation() != null && widget.getPresentation().getView() != null) {
                        getReportService().getModelInteractionService().applyView(compiledCollection, widget.getPresentation().getView());
                    }

                    QName collectionType = resolveTypeQname(collectionRefSpecification, compiledCollection);
                    GuiObjectListViewType reportView = getReportViewByType(dashboardConfig, collectionType);
                    if (reportView != null) {
                        getReportService().getModelInteractionService().applyView(compiledCollection, reportView);
                    }

                    switch (sourceType) {
                        case OBJECT_COLLECTION:
                            tableBox = createTableBoxForObjectView(widgetData.getLabel(), collectionRefSpecification, compiledCollection, null, task, result, false);
                            break;
                        case AUDIT_SEARCH:
                            if (collection == null) {
                                LOGGER.error("CollectionRef is null for report of audit records");
                                throw new IllegalArgumentException("CollectionRef is null for report of audit records");
                            }
                            tableBox = createTableBoxForAuditView(widgetData.getLabel(), collectionRefSpecification, compiledCollection, null, task, result, false);
                            break;
                    }
                    if (tableBox != null) {
                        tableboxesFromWidgets.add(tableBox);
                    }
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
    public byte[] processCollection(String nameOfReport, ObjectCollectionReportEngineConfigurationType collectionConfig, Task task, OperationResult result) throws Exception {
        CollectionRefSpecificationType collectionRefSpecification = collectionConfig.getCollection();
        ObjectReferenceType ref = collectionRefSpecification.getCollectionRef();
        ObjectCollectionType collection = null;
        if (ref != null) {
            Class<ObjectType> type = getReportService().getPrismContext().getSchemaRegistry().determineClassForType(ref.getType());
            collection = (ObjectCollectionType) getReportService().getModelService()
                    .getObject(type, ref.getOid(), null, task, result)
                    .asObjectable();
        }

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream in = classLoader.getResourceAsStream(REPORT_CSS_STYLE_FILE_NAME);
        if (in == null) {
            throw new IllegalStateException("Resource " + REPORT_CSS_STYLE_FILE_NAME + " couldn't be found");
        }
        byte[] data = IOUtils.toByteArray(in);
        String cssStyle = new String(data, Charset.defaultCharset());

        StringBuilder body = new StringBuilder();
        body.append("<div> <style> ").append(cssStyle).append(" </style>");

        CompiledObjectCollectionView compiledCollection = new CompiledObjectCollectionView();
        String defaultName = nameOfReport;
        if (collection != null) {
            if (!Boolean.TRUE.equals(collectionConfig.isUseOnlyReportView())) {
                getReportService().getModelInteractionService().applyView(compiledCollection, collection.getDefaultView());
            }
            defaultName = collection.getName().getOrig();
        } else if (collectionRefSpecification.getBaseCollectionRef() != null
                && collectionRefSpecification.getBaseCollectionRef().getCollectionRef() != null) {
            ObjectCollectionType baseCollection = (ObjectCollectionType) getObjectFromReference(collectionRefSpecification.getBaseCollectionRef().getCollectionRef()).asObjectable();
            if (!Boolean.TRUE.equals(collectionConfig.isUseOnlyReportView())) {
                getReportService().getModelInteractionService().applyView(compiledCollection, baseCollection.getDefaultView());
            }
            defaultName = baseCollection.getName().getOrig();
        }

        GuiObjectListViewType reportView = collectionConfig.getView();
        if (reportView != null) {
            getReportService().getModelInteractionService().applyView(compiledCollection, reportView);
        }

        String label;
        if (compiledCollection.getDisplay() != null && StringUtils.isEmpty(compiledCollection.getDisplay().getLabel().getOrig())) {
            label = compiledCollection.getDisplay().getLabel().getOrig();
        } else {
            label = defaultName;
        }

        ContainerTag tableBox;
        if (!isAuditCollection(collection)) {
            tableBox = createTableBoxForObjectView(label, collectionRefSpecification, compiledCollection,
                    collectionConfig.getCondition(), task, result, true);
        } else {
            tableBox = createTableBoxForAuditView(label, collectionRefSpecification, compiledCollection,
                    collectionConfig.getCondition(), task, result, true);
        }

        body.append(tableBox.render());

        body.append("</div>");

        return body.toString().getBytes();
    }

    private ContainerTag createTable() {
        return TagCreator.table().withClasses("table", "table-striped", "table-hover", "table-bordered");
    }

    private ContainerTag createTable(CompiledObjectCollectionView compiledCollection, List<PrismObject<ObjectType>> values,
            PrismObjectDefinition<ObjectType> def, Class<? extends ObjectType> type,
            boolean recordProgress, Task task, OperationResult result) {
        ContainerTag table = createTable();
        ContainerTag tHead = TagCreator.thead();
        ContainerTag tBody = TagCreator.tbody();
        List<GuiObjectColumnType> columns = MiscSchemaUtil.orderCustomColumns(compiledCollection.getColumns());
        ContainerTag trForHead = TagCreator.tr().withStyle("width: 100%;");

        columns.forEach(column -> {
            Validate.notNull(column.getName(), "Name of column is null");

            String label = getColumnLabel(column, def);
            DisplayType columnDisplay = column.getDisplay();
            ContainerTag th = TagCreator.th(TagCreator.div(TagCreator.span(label).withClass("sortableLabel")));
            if (columnDisplay != null) {
                if (StringUtils.isNotBlank(columnDisplay.getCssClass())) {
                    th.withClass(columnDisplay.getCssClass());
                }
                if (StringUtils.isNotBlank(columnDisplay.getCssStyle())) {
                    th.withStyle(columnDisplay.getCssStyle());
                }
            }
            trForHead.with(th);

        });
        tHead.with(trForHead);
        table.with(tHead);

        int i = 0;
        if (recordProgress) {
            task.setExpectedTotal((long) values.size());
            recordProgress(task, i, result, LOGGER);
        }
        for (PrismObject<ObjectType> value : values) {
            ContainerTag tr = TagCreator.tr();
            columns.forEach(column -> {
                ItemPath path = column.getPath() == null ? null : column.getPath().getItemPath();
                ExpressionType expression = column.getExport() != null ? column.getExport().getExpression() : null;
                tr.with(TagCreator
                        .th(TagCreator.div(getRealValueAsString(column, value, path, expression, task, result))
                                .withStyle("white-space: pre-wrap")));
            });
            tBody.with(tr);
            if (recordProgress) {
                i++;
                recordProgress(task, i, result, LOGGER);
            }
        }
        table.with(tBody);
        return table;
    }

    private ContainerTag createTable(CompiledObjectCollectionView compiledCollection, List<AuditEventRecordType> records, boolean recordProgress,
            Task task, OperationResult result) {
        ContainerTag table = createTable();
        ContainerTag tHead = TagCreator.thead();
        ContainerTag tBody = TagCreator.tbody();
        List<GuiObjectColumnType> columns = MiscSchemaUtil.orderCustomColumns(compiledCollection.getColumns());
        ContainerTag trForHead = TagCreator.tr().withStyle("width: 100%;");
        PrismContainerDefinition<AuditEventRecordType> def = getReportService().getPrismContext().getSchemaRegistry()
                .findItemDefinitionByCompileTimeClass(AuditEventRecordType.class, PrismContainerDefinition.class);
        columns.forEach(column -> {
            Validate.notNull(column.getName(), "Name of column is null");

            DisplayType columnDisplay = column.getDisplay();
            String label = getColumnLabel(column, def);
            ContainerTag th = TagCreator.th(TagCreator.div(TagCreator.span(label).withClass("sortableLabel")));
            if (columnDisplay != null) {
                if (StringUtils.isNotBlank(columnDisplay.getCssClass())) {
                    th.withClass(columnDisplay.getCssClass());
                }
                if (StringUtils.isNotBlank(columnDisplay.getCssStyle())) {
                    th.withStyle(columnDisplay.getCssStyle());
                }
            }
            trForHead.with(th);
        });
        tHead.with(trForHead);
        table.with(tHead);

        int i = 0;
        if (recordProgress) {
            task.setExpectedTotal((long) records.size());
            recordProgress(task, i, result, LOGGER);
        }
        for (AuditEventRecordType record : records) {
            ContainerTag tr = TagCreator.tr();
            columns.forEach(column -> {
                ExpressionType expression = column.getExport() != null ? column.getExport().getExpression() : null;
                ItemPath path = column.getPath() == null ? null : column.getPath().getItemPath();
                try {
                    tr.with(TagCreator
                            .th(TagCreator.div(getRealValueAsString(column, getAuditRecordAsContainer(record),
                                    path, expression, task, result))
                                    .withStyle("white-space: pre-wrap")));
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't create singleValueContainer for audit record " + record);
                }
            });
            tBody.with(tr);
            if (recordProgress) {
                i++;
                recordProgress(task, i, result, LOGGER);
            }
        }
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
        if (display != null) {
            if (display.getCssStyle() != null) {
                style = display.getCssStyle();
            }
            if (display.getCssClass() != null) {
                classes = display.getCssClass();
            }
        }
        return TagCreator.div().withClasses("box", "boxed-table", classes).withStyle(style).with(div);
    }

    private ContainerTag createTableBoxForObjectView(String label, CollectionRefSpecificationType collection, @NotNull CompiledObjectCollectionView compiledCollection,
            ExpressionType condition, Task task, OperationResult result, boolean recordProgress) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        long startMillis = getReportService().getClock().currentTimeMillis();
        Class<ObjectType> type = resolveType(collection, compiledCollection);
        Collection<SelectorOptions<GetOperationOptions>> options = DefaultColumnUtils.createOption(type, getReportService().getSchemaHelper());
        List<PrismObject<ObjectType>> values = getReportService().getDashboardService()
                .searchObjectFromCollection(collection, compiledCollection.getObjectType(), options, condition, task, result);
        if (values == null || values.isEmpty()) {
            if (recordProgress) {
                values = new ArrayList<>();
            } else {
                return null;
            }
        }
        PrismObjectDefinition<ObjectType> def = getReportService().getPrismContext().getSchemaRegistry().findItemDefinitionByCompileTimeClass(type, PrismObjectDefinition.class);

        if (compiledCollection.getColumns().isEmpty()) {
            getReportService().getModelInteractionService().applyView(compiledCollection, DefaultColumnUtils.getDefaultView(type));
        }

        ContainerTag table = createTable(compiledCollection, values, def, type, recordProgress, task, result);
        DisplayType display = compiledCollection.getDisplay();
        return createTableBox(table, label, values.size(),
                convertMillisToString(startMillis), display);
    }

    private ContainerTag createTableBoxForAuditView(
            String label, CollectionRefSpecificationType collection, @NotNull CompiledObjectCollectionView compiledCollection,
            ExpressionType condition, Task task, OperationResult result, boolean recordProgress) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        long startMillis = getReportService().getClock().currentTimeMillis();
        List<AuditEventRecordType> records = getReportService().getDashboardService().searchObjectFromCollection(collection, condition, task, result);

        if (compiledCollection.getColumns().isEmpty()) {
            getReportService().getModelInteractionService().applyView(compiledCollection, DefaultColumnUtils.getDefaultAuditEventsView());
        }
        ContainerTag table = createTable(compiledCollection, records, recordProgress, task, result);
        DisplayType display = compiledCollection.getDisplay();
        return createTableBox(table, label, records.size(),
                convertMillisToString(startMillis), display);
    }

    private ContainerTag createTHead(String prefix, Set<String> set) {
        return TagCreator.thead(TagCreator.tr(TagCreator.each(set,
                header -> TagCreator.th(TagCreator.div(TagCreator.span(getMessage(prefix + header)).withClass("sortableLabel")))))
                .withStyle("width: 100%;"));
    }

    private ContainerTag createTBodyRow(DashboardWidget data) {
        return TagCreator.tr(
                TagCreator.each(getHeadsOfWidget(),
                        header -> getContainerTagForWidgetHeader(header, data)));

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

    private String convertMillisToString(long millis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss", Locale.getDefault());
        return dateFormat.format(millis);
    }

    private void appendNewLine(StringBuilder body) {
        body.append("<br>");
    }

    protected void appendMultivalueDelimiter(StringBuilder body) {
        appendNewLine(body);
    }

    @Override
    public void importCollectionReport(ReportType report, VariablesMap listOfVariables, RunningTask task, OperationResult result) {
        throw new UnsupportedOperationException("Unsupported operation import for HTML file format");
    }

    @Override
    public List<VariablesMap> createVariablesFromFile(ReportType report, ReportDataType reportData, boolean useImportScript, Task task, OperationResult result) throws IOException {
        throw new UnsupportedOperationException("Unsupported operation import for HTML file format");
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
