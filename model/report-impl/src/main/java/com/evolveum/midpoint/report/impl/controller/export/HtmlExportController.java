/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller.export;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.model.api.util.DashboardUtils;
import com.evolveum.midpoint.model.api.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.*;
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
                CollectionRefSpecificationType collectionRefSpecification = getReportService().getDashboardService().getCollectionRefSpecificationType(widget, task, result);
                ObjectCollectionType collection = getReportService().getDashboardService().getObjectCollectionType(widget, task, result);

                CompiledObjectCollectionView compiledCollection = new CompiledObjectCollectionView();
                if (collection != null) {
                    getReportService().getModelInteractionService().applyView(compiledCollection, collection.getDefaultView());
                } else if (collectionRefSpecification.getBaseCollectionRef() != null
                        && collectionRefSpecification.getBaseCollectionRef().getCollectionRef() != null) {
                    ObjectCollectionType baseCollection = (ObjectCollectionType)getObjectFromReference(collectionRefSpecification.getBaseCollectionRef().getCollectionRef()).asObjectable();
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
                        tableBox = createTableBoxForObjectView(widgetData.getLabel(), collectionRefSpecification, compiledCollection, false, task, result);
                        break;
                    case AUDIT_SEARCH:
                        if (collection == null) {
                            LOGGER.error("CollectionRef is null for report of audit records");
                            throw new IllegalArgumentException("CollectionRef is null for report of audit records");
                        }
                        tableBox = createTableBoxForAuditView(widgetData.getLabel(), collection, compiledCollection, false, task, result);
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
            ObjectCollectionType baseCollection = (ObjectCollectionType)getObjectFromReference(collectionRefSpecification.getBaseCollectionRef().getCollectionRef()).asObjectable();
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
        boolean isAuditCollection = collection != null && collection.getAuditSearch() != null ? true : false;
        if (!isAuditCollection) {
            tableBox = createTableBoxForObjectView(label, collectionConfig.getCollection(), compiledCollection, true, task, result);
        } else {
            tableBox = createTableBoxForAuditView(label, collection, compiledCollection, true, task, result);
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

        int i = 0;
        if (recordProgress) {
            task.setExpectedTotal((long) values.size());
            recordProgress(task, i, result, LOGGER);
        }
        for (PrismObject<ObjectType> value : values) {
            ContainerTag tr = TagCreator.tr();
            columns.forEach(column -> {
                ItemPath path = column.getPath() == null ? null : column.getPath().getItemPath();
                ExpressionType expression = column.getExpression();
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

    private ContainerTag createTable(CompiledObjectCollectionView compiledCollection, List<AuditEventRecord> records, boolean recordProgress,
            Task task, OperationResult result) {
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

        int i = 0;
        if (recordProgress) {
            task.setExpectedTotal((long) records.size());
            recordProgress(task, i, result, LOGGER);
        }
        for (AuditEventRecord record : records) {
            ContainerTag tr = TagCreator.tr();
            columns.forEach(column -> {
                ExpressionType expression = column.getExpression();
                ItemPath path = column.getPath() == null ? null : column.getPath().getItemPath();
                tr.with(TagCreator
                        .th(TagCreator.div(getStringValueByAuditColumn(record, path, expression, task, result))
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

    private ContainerTag createTableBoxForObjectView(String label, CollectionRefSpecificationType collection, @NotNull CompiledObjectCollectionView compiledCollection,
            boolean recordProgress, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        long startMillis = getReportService().getClock().currentTimeMillis();
        Class<ObjectType> type = resolveType(collection, compiledCollection);
        Collection<SelectorOptions<GetOperationOptions>> options = DefaultColumnUtils.createOption(type, getReportService().getSchemaHelper());
        List<PrismObject<ObjectType>> values = getReportService().getDashboardService()
                .searchObjectFromCollection(collection, compiledCollection.getObjectType(), options, task, result);
        if (values == null || values.isEmpty()) {
            if (recordProgress) {
                values = new ArrayList<>();
            } else {
                return null;
            }
        }
        PrismObjectDefinition<ObjectType> def = getReportService().getPrismContext().getSchemaRegistry().findItemDefinitionsByCompileTimeClass(type, PrismObjectDefinition.class).get(0);

        if (compiledCollection.getColumns().isEmpty()) {
            getReportService().getModelInteractionService().applyView(compiledCollection, DefaultColumnUtils.getDefaultView(type));
        }

        ContainerTag table = createTable(compiledCollection, values, def, type, recordProgress, task, result);
        DisplayType display = compiledCollection.getDisplay();
        return createTableBox(table, label, values.size(),
                convertMillisToString(startMillis), display);
    }

    private ContainerTag createTableBoxForAuditView(String label, ObjectCollectionType collection, @NotNull CompiledObjectCollectionView compiledCollection,
            boolean recordProgress, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Map<String, Object> parameters = new HashMap<>();
        long startMillis = getReportService().getClock().currentTimeMillis();
        String query = DashboardUtils
                .getQueryForListRecords(DashboardUtils.createQuery(collection, parameters, false, getReportService().getClock()));
        List<AuditEventRecord> records = getReportService().getAuditService().listRecords(query, parameters, result);
        if (records == null || records.isEmpty()) {
            return null;
        }

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
                header -> TagCreator.th(TagCreator.div(TagCreator.span(getMessage(prefix+header)).withClass("sortableLabel")))))
                    .withStyle("width: 100%;"));
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
    public String getTypeSuffix() {
        return ".html";
    }

    @Override
    public String getType() {
        return "HTML";
    }
}
