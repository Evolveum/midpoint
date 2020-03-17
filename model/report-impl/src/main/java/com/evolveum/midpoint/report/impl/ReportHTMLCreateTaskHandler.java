/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.report.api.ReportService;

import com.evolveum.midpoint.schema.util.MiscSchemaUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.interaction.DashboardService;
import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.model.api.util.DashboardUtils;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.AuditLocalizationConstants;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetPresentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetSourceTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectListViewType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportEngineSelectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import j2html.TagCreator;
import j2html.tags.ContainerTag;

/**
 * @author skublik
 */

@Component
public class ReportHTMLCreateTaskHandler extends ReportJasperCreateTaskHandler {

    static final String REPORT_HTML_CREATE_TASK_URI = "http://midpoint.evolveum.com/xml/ns/public/report/html/create/handler-3";
    private static final Trace LOGGER = TraceManager.getTrace(ReportHTMLCreateTaskHandler.class);

    private static final String REPORT_CSS_STYLE_FILE_NAME = "dashboard-report-style.css";

    private static final String LABEL_COLUMN = "label";
    private static final String NUMBER_COLUMN = "number";
    private static final String STATUS_COLUMN = "status";

    private static final String TIME_COLUMN = "time";
    private static final String INITIATOR_COLUMN = "initiator";
    private static final String EVENT_STAGE_COLUMN = "eventStage";
    private static final String EVENT_TYPE_COLUMN = "eventType";
    private static final String TARGET_COLUMN = "target";
    private static final String DELTA_COLUMN = "delta";
    private static final String MESSAGE_COLUMN = "message";
    private static final String TARGET_OWNER_COLUMN = "targetOwner";
    private static final String CHANNEL_COLUMN = "channel";
    private static final String OUTCOME_COLUMN = "outcome";
    private static final String TASK_OID_COLUMN = "taskOid";
    private static final String NODE_IDENTIFIER_COLUMN = "nodeIdentifier";
    private static final String ATTORNEY_COLUMN = "attorney";
    private static final String RESULT_COLUMN = "result";
    private static final String RESOURCE_OID_COLUMN = "resourceOid";

//    private static final String NAME_COLUMN = "Name";
//    private static final String CONNECTOR_TYPE_COLUMN = "Connector type";
//    private static final String VERSION_COLUMN = "Version";
//    private static final String GIVEN_NAME_COLUMN = "Given name";
//    private static final String FAMILY_NAME_COLUMN = "Family name";
//    private static final String FULL_NAME_COLUMN = "Full name";
//    private static final String EMAIL_COLUMN = "Email";
//    private static final String ACCOUNTS_COLUMN = "Accounts";
//    private static final String DISPLAY_NAME_COLUMN = "Display name";
//    private static final String DESCRIPTION_COLUMN = "Description";
//    private static final String IDENTIFIER_COLUMN = "Identifier";
//    private static final String CATEGORY_COLUMN = "Category";
//    private static final String OBJECT_REFERENCE_COLUMN = "Object reference";
//    private static final String EXECUTION_COLUMN = "Execution";
//    private static final String EXECUTING_AT_COLUMN = "Executing at";
//    private static final String PROGRES_COLUMN = "Progress";
//    private static final String CURRENT_RUN_TIME_COLUMN = "Current run time";
//    private static final String SCHEDULED_TO_START_AGAIN_COLUMN = "Scheduled to start again";

    private static final String NAME_COLUMN = "name";
    private static final String CONNECTOR_TYPE_COLUMN = "connectorType";
    private static final String CONNECTOR_VERSION_COLUMN = "connectorVersion";
    private static final String GIVEN_NAME_COLUMN = "givenName";
    private static final String FAMILY_NAME_COLUMN = "familyName";
    private static final String FULL_NAME_COLUMN = "fullName";
    private static final String EMAIL_COLUMN = "email";
    private static final String ACCOUNTS_COLUMN = "accounts";
    private static final String DISPLAY_NAME_COLUMN = "displayName";
    private static final String DESCRIPTION_COLUMN = "description";
    private static final String IDENTIFIER_COLUMN = "identifier";
    private static final String CATEGORY_COLUMN = "category";
    private static final String OBJECT_REFERENCE_COLUMN = "objectReference";
    private static final String EXECUTION_COLUMN = "execution";
    private static final String EXECUTING_AT_COLUMN = "executingAt";
    private static final String PROGRES_COLUMN = "progress";
    private static final String CURRENT_RUN_TIME_COLUMN = "currentRunTime";

    private static final String REPORT_GENERATED_ON = "Widget.generatedOn";
    private static final String NUMBER_OF_RECORDS = "Widget.numberOfRecords";

    private static final String UNDEFINED_NAME = "Widget.column.undefinedName";

    private static final QName CUSTOM = new QName("customPerformedColumn");

    @Autowired private Clock clock;
    @Autowired private TaskManager taskManager;
    @Autowired private AuditService auditService;
    @Autowired private ReportService reportService;
    @Autowired private ModelService modelService;
    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("modelObjectResolver") private ObjectResolver objectResolver;
    @Autowired private DashboardService dashboardService;
    @Autowired private LocalizationService localizationService;
    @Autowired private ExpressionFactory expressionFactory;

    private static LinkedHashMap<Class<? extends ObjectType>, LinkedHashMap<String, ItemPath>> columnDef;
    private static Set<String> headsOfWidget;
    private static Set<String> headsOfAuditEventRecords;
    private static HashMap<String, String> specialKeyLocalization;

    static {
        columnDef = new LinkedHashMap<Class<? extends ObjectType>, LinkedHashMap<String, ItemPath>>() {
            private static final long serialVersionUID = 1L;
            {
                put(ResourceType.class, new LinkedHashMap<String, ItemPath>() {
                    private static final long serialVersionUID = 1L;
                    {
                        put(NAME_COLUMN, ResourceType.F_NAME);
                        put(CONNECTOR_TYPE_COLUMN,
                                ItemPath.create(ResourceType.F_CONNECTOR_REF, ConnectorType.F_CONNECTOR_TYPE));
                        put(CONNECTOR_VERSION_COLUMN,
                                ItemPath.create(ResourceType.F_CONNECTOR_REF, ConnectorType.F_CONNECTOR_VERSION));
                    }
                });

                put(UserType.class, new LinkedHashMap<String, ItemPath>() {
                    private static final long serialVersionUID = 1L;
                    {
                        put(NAME_COLUMN, UserType.F_NAME);
                        put(GIVEN_NAME_COLUMN, UserType.F_GIVEN_NAME);
                        put(FAMILY_NAME_COLUMN, UserType.F_FAMILY_NAME);
                        put(FULL_NAME_COLUMN, UserType.F_FULL_NAME);
                        put(EMAIL_COLUMN, UserType.F_EMAIL_ADDRESS);
                        put(ACCOUNTS_COLUMN, ItemPath.create(AbstractRoleType.F_LINK_REF, CUSTOM));
                    }
                });

                put(AbstractRoleType.class, new LinkedHashMap<String, ItemPath>() {
                    private static final long serialVersionUID = 1L;
                    {
                        put(NAME_COLUMN, AbstractRoleType.F_NAME);
                        put(DISPLAY_NAME_COLUMN, AbstractRoleType.F_DISPLAY_NAME);
                        put(DESCRIPTION_COLUMN, AbstractRoleType.F_DESCRIPTION);
                        put(IDENTIFIER_COLUMN, AbstractRoleType.F_IDENTIFIER);
                        put(ACCOUNTS_COLUMN, ItemPath.create(AbstractRoleType.F_LINK_REF, CUSTOM));
                    }
                });

                put(TaskType.class, new LinkedHashMap<String, ItemPath>() {
                    private static final long serialVersionUID = 1L;
                    {
                        put(NAME_COLUMN, TaskType.F_NAME);
                        put(CATEGORY_COLUMN, TaskType.F_CATEGORY);
                        put(OBJECT_REFERENCE_COLUMN, TaskType.F_OBJECT_REF);
                        put(EXECUTION_COLUMN, TaskType.F_EXECUTION_STATUS);
                        put(EXECUTING_AT_COLUMN, TaskType.F_NODE_AS_OBSERVED);
                        put(PROGRES_COLUMN, TaskType.F_PROGRESS);
                        put(CURRENT_RUN_TIME_COLUMN, ItemPath.create(CUSTOM));
                        put(STATUS_COLUMN, TaskType.F_RESULT_STATUS);
                    }
                });
            }

        };

        headsOfWidget = new LinkedHashSet<String>() {
            {
                add(LABEL_COLUMN);
                add(NUMBER_COLUMN);
                add(STATUS_COLUMN);
            }
        };

        headsOfAuditEventRecords = new LinkedHashSet<String>() {
            {
                add(TIME_COLUMN);
                add(INITIATOR_COLUMN);
                add(EVENT_STAGE_COLUMN);
                add(EVENT_TYPE_COLUMN);
                add(TARGET_COLUMN);
//                add(TARGET_OWNER_COLUMN);
//                add(CHANNEL_COLUMN);
                add(OUTCOME_COLUMN);
                add(MESSAGE_COLUMN);
                add(DELTA_COLUMN);
            }
        };

        specialKeyLocalization = new HashMap<String, String>() {
            {
                put(CONNECTOR_TYPE_COLUMN, "ConnectorType.connectorType");
                put(CONNECTOR_VERSION_COLUMN, "ConnectorType.connectorVersion");
                put(TIME_COLUMN, AuditLocalizationConstants.TIME_COLUMN_KEY);
                put(INITIATOR_COLUMN, AuditLocalizationConstants.INITIATOR_COLUMN_KEY);
                put(EVENT_STAGE_COLUMN, AuditLocalizationConstants.EVENT_STAGE_COLUMN_KEY);
                put(EVENT_TYPE_COLUMN, AuditLocalizationConstants.EVENT_TYPE_COLUMN_KEY);
                put(TARGET_COLUMN, AuditLocalizationConstants.TARGET_COLUMN_KEY);
                put(DELTA_COLUMN, AuditLocalizationConstants.DELTA_COLUMN_KEY);
                put(MESSAGE_COLUMN, AuditLocalizationConstants.MESSAGE_COLUMN_KEY);
                put(TARGET_OWNER_COLUMN, AuditLocalizationConstants.TARGET_OWNER_COLUMN_KEY);
                put(CHANNEL_COLUMN, AuditLocalizationConstants.CHANNEL_COLUMN_KEY);
                put(OUTCOME_COLUMN, AuditLocalizationConstants.OUTCOME_COLUMN_KEY);
                put(TASK_OID_COLUMN, AuditLocalizationConstants.TASK_OID_COLUMN_KEY);
                put(NODE_IDENTIFIER_COLUMN, AuditLocalizationConstants.NODE_IDENTIFIER_COLUMN_KEY);
                put(ATTORNEY_COLUMN, AuditLocalizationConstants.ATTORNEY_COLUMN_KEY);
                put(RESULT_COLUMN, AuditLocalizationConstants.RESULT_COLUMN_KEY);
                put(RESOURCE_OID_COLUMN, AuditLocalizationConstants.RESOURCE_OID_COLUMN_KEY);
            }
        };
    }

    @Override
    protected void initialize() {
        LOGGER.trace("Registering with taskManager as a handler for {}", REPORT_HTML_CREATE_TASK_URI);
        taskManager.registerHandler(REPORT_HTML_CREATE_TASK_URI, this);
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        OperationResult parentResult = task.getResult();
        OperationResult result = parentResult
                .createSubresult(ReportHTMLCreateTaskHandler.class.getSimpleName() + ".run");
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(result);
        super.recordProgress(task, 0, result);
        try {
            ReportType parentReport = objectResolver.resolve(task.getObjectRefOrClone(), ReportType.class, null,
                    "resolving report", task, result);

            if (!reportService.isAuthorizedToRunReport(parentReport.asPrismObject(), task, parentResult)) {
                LOGGER.error("Task {} is not authorized to run report {}", task, parentReport);
                throw new SecurityViolationException("Not authorized");
            }

            if (parentReport.getReportEngine() == null) {
                throw new IllegalArgumentException("Report Object doesn't have ReportEngine attribute");
            }
            if (parentReport.getReportEngine().equals(ReportEngineSelectionType.JASPER)) {
                parentReport.setExport(ExportType.HTML);
                return super.run(task, partition);

            } else if (parentReport.getReportEngine().equals(ReportEngineSelectionType.DASHBOARD)) {

                if (parentReport.getDashboard() != null && parentReport.getDashboard().getDashboardRef() != null) {
                    ObjectReferenceType ref = parentReport.getDashboard().getDashboardRef();
                    Class<ObjectType> type = prismContext.getSchemaRegistry().determineClassForType(ref.getType());
                    Task taskSearchDashboard = taskManager.createTaskInstance("Search dashboard");
                    DashboardType dashboard = (DashboardType) modelService
                            .getObject(type, ref.getOid(), null, taskSearchDashboard, taskSearchDashboard.getResult())
                            .asObjectable();
                    ClassLoader classLoader = getClass().getClassLoader();
                    InputStream in = classLoader.getResourceAsStream(REPORT_CSS_STYLE_FILE_NAME);
                    if (in == null) {
                        throw new IllegalStateException("Resource " + REPORT_CSS_STYLE_FILE_NAME + " couldn't be found");
                    }
                    byte[] data = IOUtils.toByteArray(in);
                    String style = new String(data, Charset.defaultCharset());

                    String reportFilePath = getDestinationFileName(parentReport);
                    FileUtils.writeByteArrayToFile(new File(reportFilePath), getBody(dashboard, style, task, result).getBytes());
                    super.saveReportOutputType(reportFilePath, parentReport, task, result);
                    LOGGER.trace("create report output type : {}", reportFilePath);

                    if (parentReport.getPostReportScript() != null) {
                        super.processPostReportScript(parentReport, reportFilePath, task, result);
                    }
                    result.computeStatus();
                } else {
                    LOGGER.error("Dashboard or DashboardRef is null");
                    throw new IllegalArgumentException("Dashboard or DashboardRef is null");
                }

            }
        } catch (Exception ex) {
            LOGGER.error("CreateReport: {}", ex.getMessage(), ex);
            result.recordFatalError(ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        // This "run" is finished. But the task goes on ...
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        LOGGER.trace("CreateReportTaskHandler.run stopping");
        return runResult;
    }

    private String getBody(DashboardType dashboard, String cssStyle, Task task, OperationResult result) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, ObjectNotFoundException {
        StringBuilder body = new StringBuilder();
        body.append("<div> <style> ").append(cssStyle).append(" </style>");

        ContainerTag widgetTable = createTable();
        widgetTable.with(createTHead("Widget.", getHeadsOfWidget()));

        ContainerTag widgetTBody = TagCreator.tbody();
        List<ContainerTag> tableboxesFromWidgets = new ArrayList<>();
        long startMillis = clock.currentTimeMillis();
        for (DashboardWidgetType widget : dashboard.getWidget()) {
            DashboardWidget widgetData = dashboardService.createWidgetData(widget, task, result);
            widgetTBody.with(createTBodyRow(widgetData));
            ContainerTag tableBox = createTableBoxForWidget(widgetData, task, result);
            if (tableBox != null) {
                tableboxesFromWidgets.add(tableBox);
            }
        }
        widgetTable.with(widgetTBody);

        body.append(createTableBox(widgetTable, "Widgets", dashboard.getWidget().size(),
                convertMillisToString(startMillis), null).render());
        appendSpace(body);
        tableboxesFromWidgets.forEach(table -> {
            body.append(table.render());
            appendSpace(body);
        });
        body.append("</div>");

        return body.toString();
    }

    private String convertMillisToString(long millis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss", Locale.getDefault());
        return dateFormat.format(millis);
    }

    private void appendSpace(StringBuilder body) {
        body.append("<br>");
    }

    private ContainerTag createTableBoxForWidget(DashboardWidget widgetData, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException,
    CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        DashboardWidgetType widget = widgetData.getWidget();
        if (widget == null) {
            throw new IllegalArgumentException("Widget in DashboardWidget is null");
        }
        DashboardWidgetPresentationType presentation = widget.getPresentation();
        DashboardWidgetSourceTypeType sourceType = DashboardUtils.getSourceType(widget);
        if (sourceType == null) {
            throw new IllegalStateException("No source type specified in " + widget);
        }
        switch (sourceType) {
        case OBJECT_COLLECTION:
            if (!DashboardUtils.isDataFieldsOfPresentationNullOrEmpty(presentation)) {
                ObjectCollectionType collection = dashboardService.getObjectCollectionType(widget, task, result);
                long startMillis = clock.currentTimeMillis();
                List<PrismObject<ObjectType>> values = dashboardService.searchObjectFromCollection(collection, true, task, result);
                if (values == null || values.isEmpty()) {
                    return null;
                }
                ContainerTag table = createTable();
                PrismObjectDefinition<ObjectType> def = values.get(0).getDefinition();
                //noinspection unchecked
                Class<ObjectType> type = (Class<ObjectType>) prismContext.getSchemaRegistry()
                        .getCompileTimeClassForObjectType(collection.getType());
                DisplayType display = null;
                if(!useDefaultColumn(widget)) {
                    table = createTable(widget.getPresentation().getView(), values, def, type,
                            task, result);
                    display = widget.getPresentation().getView().getDisplay();
                } else {
                    table = createTable(values, def, type, task, result);
                }
                return createTableBox(table, widgetData.getLabel(), values.size(),
                        convertMillisToString(startMillis), display);
            }
            break;
        case AUDIT_SEARCH:
            if (!DashboardUtils.isDataFieldsOfPresentationNullOrEmpty(presentation)) {
                Map<String, Object> parameters = new HashMap<>();

                ObjectCollectionType collection = dashboardService.getObjectCollectionType(widget, task, result);
                long startMillis = clock.currentTimeMillis();
                String query = DashboardUtils
                        .getQueryForListRecords(DashboardUtils.createQuery(collection, parameters, false, clock));
                List<AuditEventRecord> records = auditService.listRecords(query, parameters, result);
                if (records == null || records.isEmpty()) {
                    return null;
                }

                ContainerTag table = createTable();
                DisplayType display = null;
                if(!useDefaultColumn(widget)) {
                    table = createTable(widget.getPresentation().getView(), records, task, result);
                    display = widget.getPresentation().getView().getDisplay();
                } else {
                    table.with(createTHead(getHeadsOfAuditEventRecords()));
                    ContainerTag tBody = TagCreator.tbody();
                    records.forEach(record -> {
                        ContainerTag tr = TagCreator.tr();
                        getHeadsOfAuditEventRecords().forEach(column -> {
                            tr.with(TagCreator.th(TagCreator.div(getStringForColumnOfAuditRecord(column, record, null)).withStyle("white-space: pre-wrap")));
                        });
                        tBody.with(tr);
                    });
                    table.with(tBody);
                }
                return createTableBox(table, widgetData.getLabel(), records.size(),
                        convertMillisToString(startMillis), null);
            }
            break;
        }
        return null;
    }

    private boolean useDefaultColumn(DashboardWidgetType widget) {
        return widget.getPresentation() == null || widget.getPresentation().getView() == null
                || widget.getPresentation().getView().getColumn() == null || widget.getPresentation().getView().getColumn().isEmpty();
    }

    private String getStringForColumnOfAuditRecord(String column, AuditEventRecord record, ItemPathType path,
            ExpressionType expression, Task task, OperationResult result) {
        if(expression == null) {
            return getStringForColumnOfAuditRecord(column, record, path);
        }
        Object object = null;
        switch (column) {
        case TIME_COLUMN:
            object = record.getTimestamp();
            break;
        case INITIATOR_COLUMN:
            object = record.getInitiator();
            break;
        case EVENT_STAGE_COLUMN:
            object = record.getEventStage();
            break;
        case EVENT_TYPE_COLUMN:
            object = record.getEventType();
            break;
        case TARGET_COLUMN:
            object = record.getTarget();
            break;
        case TARGET_OWNER_COLUMN:
            object = record.getTargetOwner();
            break;
        case CHANNEL_COLUMN:
            object = record.getChannel();
            break;
        case OUTCOME_COLUMN:
            object = record.getOutcome();
            break;
        case MESSAGE_COLUMN:
            object = record.getMessage();
            break;
        case DELTA_COLUMN:
            object = record.getDeltas();
            break;
        case TASK_OID_COLUMN:
            object = record.getTaskOid();
            break;
        case NODE_IDENTIFIER_COLUMN:
            object = record.getNodeIdentifier();
            break;
        case ATTORNEY_COLUMN:
            object = record.getAttorney();
            break;
        case RESULT_COLUMN:
            object = record.getResult();
            break;
        case RESOURCE_OID_COLUMN:
            object = record.getResourceOids();
            break;
        default:
            if(record.getCustomColumnProperty().containsKey(path)) {
                object = record.getCustomColumnProperty().get(path);
            } else {
                LOGGER.error("Unknown name of column for AuditReport " + column);
            }
            break;
        }
        return evaluateExpression(expression, object, task, result);
    }

    private String getStringForColumnOfAuditRecord(String column, AuditEventRecord record, ItemPathType path) {
        switch (column) {
        case TIME_COLUMN:
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss", Locale.US);
            return dateFormat.format(record.getTimestamp());
        case INITIATOR_COLUMN:
            return record.getInitiator() == null ? "" : record.getInitiator().getName().getOrig();
        case EVENT_STAGE_COLUMN:
            return record.getEventStage() == null ? "" : getMessage(record.getEventStage());
        case EVENT_TYPE_COLUMN:
            return record.getEventType() == null ? "" : getMessage(record.getEventType());
        case TARGET_COLUMN:
            return record.getTarget() == null ? "" : getObjectNameFromRef(record.getTarget().getRealValue());
        case TARGET_OWNER_COLUMN:
            return record.getTargetOwner() == null ? "" :record.getTargetOwner().getName().getOrig();
        case CHANNEL_COLUMN:
            return record.getChannel() == null ? "" : QNameUtil.uriToQName(record.getChannel()).getLocalPart();
        case OUTCOME_COLUMN:
            return record.getOutcome() == null ? "" : getMessage(record.getOutcome());
        case MESSAGE_COLUMN:
            return record.getMessage() == null ? "" : record.getMessage();
        case DELTA_COLUMN:
            if (record.getDeltas() == null || record.getDeltas().isEmpty()) {
                return "";
            }
            StringBuilder sbDelta = new StringBuilder();
            Collection<ObjectDeltaOperation<? extends ObjectType>> deltas = record.getDeltas();
            Iterator<ObjectDeltaOperation<? extends ObjectType>> iterator = deltas.iterator();
            int index = 0;
            while (iterator.hasNext()) {
                ObjectDeltaOperation delta = iterator.next();
                sbDelta.append(ReportUtils.printDelta(delta));
                if ((index+1)!=deltas.size()) {
                    sbDelta.append("\n");
                }
                index++;
            }
            return sbDelta.toString();
        case TASK_OID_COLUMN:
            return record.getTaskOid() == null ? "" : record.getTaskOid();
        case NODE_IDENTIFIER_COLUMN:
            return record.getNodeIdentifier() == null ? "" : record.getNodeIdentifier();
        case ATTORNEY_COLUMN:
            return record.getAttorney() == null ? "" : record.getAttorney().getName().getOrig();
        case RESULT_COLUMN:
            return record.getResult() == null ? "" : record.getResult();
        case RESOURCE_OID_COLUMN:
            Set<String> resourceOids = record.getResourceOids();
            if(resourceOids == null || resourceOids.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            int i = 1;
            for(String oid : resourceOids) {
                sb.append(oid);
                if(i != resourceOids.size()) {
                    sb.append("\n");
                }
                i++;
            }
            return sb.toString();
        default:
            if(record.getCustomColumnProperty().containsKey(path)) {
                return record.getCustomColumnProperty().get(path);
            }
        }

        return "";

    }

    private String getObjectNameFromRef(Referencable ref) {
        if (ref == null) {
            return "";
        }
        if (ref.getTargetName() != null && ref.getTargetName().getOrig() != null) {
            return ref.getTargetName().getOrig();
        }
        PrismObject object = getObjectFromReference(ref);

        if (object == null) {
            return ref.getOid();
        }

        if (object.getName() == null || object.getName().getOrig() == null) {
            return "";
        }
        return object.getName().getOrig();
    }

    private String getRealValueAsString(String nameOfColumn, PrismObject<ObjectType> object, ItemPath itemPath,
            ExpressionType expression, Task task, OperationResult result) {
        Iterator<QName> iterator = (Iterator<QName>) itemPath.getSegments().iterator();
        Item valueObject = object;


        while (iterator.hasNext()) {
            QName name = iterator.next();
            if (QNameUtil.match(name, CUSTOM)) {
                return getCustomValueForColumn(valueObject, nameOfColumn);
            }
            valueObject = (Item) valueObject.find(ItemPath.create(name));
            if (valueObject instanceof PrismProperty && iterator.hasNext()) {
                throw new IllegalArgumentException("Found object is PrismProperty, but ItemPath isn't empty");
            }
            if (expression == null && valueObject instanceof PrismContainer && !iterator.hasNext()) {
                throw new IllegalArgumentException("Found object is PrismContainer, but ItemPath is empty");
            }
            if (valueObject instanceof PrismReference) {
                Referencable ref = ((PrismReference) valueObject).getRealValue();
                if (!iterator.hasNext()) {
                    if(expression == null) {
                        return getObjectNameFromRef(ref);
                    }
                    return evaluateExpression(expression, valueObject, task, result);
                }

                valueObject = getObjectFromReference(ref);
            }
            if (valueObject == null) {
                if(nameOfColumn.equals(ACCOUNTS_COLUMN)) {
                    return "0";
                }
                return "";
            }
        }
        if(expression != null) {
            return evaluateExpression(expression, valueObject, task, result);
        }
        Object realValue = ((PrismProperty) valueObject).getRealValue();
        if (realValue == null){
            return "";
        } else if (realValue instanceof Enum) {
            return getMessage((Enum)realValue);
        }
        return realValue.toString();
    }

    private String evaluateExpression(ExpressionType expression, Item valueObject, Task task, OperationResult result) {
        Object object;
        if(valueObject == null) {
            object = null;
        } else {
            object = valueObject.getRealValue();
        }
        return evaluateExpression(expression, object, task, result);
    }

private String evaluateExpression(ExpressionType expression, Object valueObject, Task task, OperationResult result) {

        ExpressionVariables variables = new ExpressionVariables();
        if(valueObject == null) {
            variables.put(ExpressionConstants.VAR_OBJECT, null, Object.class);
        } else {
            variables.put(ExpressionConstants.VAR_OBJECT, valueObject, valueObject.getClass());
        }
        Collection<String> values = null;
        try {
            values = ExpressionUtil.evaluateStringExpression(variables, prismContext, expression, null, expressionFactory, "value for column", task, result);
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
                | ConfigurationException | SecurityViolationException e) {
            LOGGER.error("Couldn't execute expression " + expression, e);
        }
        if (values == null || values.isEmpty()){
            return "";
        }
        if(values.size() != 1) {
            throw new IllegalArgumentException("Expected collection with one value, but it is " + values);
        }
        return values.iterator().next();
    }

    private boolean isCustomPerformedColumn(ItemPath itemPath) {
        List<?> segments = itemPath.getSegments();
        return segments.size() > 0 && QNameUtil.match((QName)segments.get(segments.size() - 1), CUSTOM);
    }

    private String getCustomValueForColumn(Item valueObject, String nameOfColumn) {
        switch (nameOfColumn) {
        case ACCOUNTS_COLUMN:
            if(!(valueObject instanceof PrismObject)){
                return "";
            }
            return String.valueOf(((PrismObject)valueObject).getRealValues().size());
        case CURRENT_RUN_TIME_COLUMN:
            if(!(valueObject instanceof PrismObject)
                    && !(((PrismObject)valueObject).getRealValue() instanceof TaskType)){
                return "";
            }
            TaskType task = (TaskType)((PrismObject)valueObject).getRealValue();
            XMLGregorianCalendar timestapm = task.getCompletionTimestamp();
            if(timestapm != null && task.getExecutionStatus().equals(TaskExecutionStatusType.CLOSED)) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss", Locale.US);
                return "closed at " + dateFormat.format(task.getCompletionTimestamp().toGregorianCalendar().getTime());
            }
            return "";
        }
        return "";
    }

    private PrismObject<ObjectType> getObjectFromReference(Referencable ref) {
        Task task = taskManager.createTaskInstance("Get object");
        Class<ObjectType> type = prismContext.getSchemaRegistry().determineClassForType(ref.getType());

        if (ref.asReferenceValue().getObject() != null) {
            return ref.asReferenceValue().getObject();
        }

        PrismObject<ObjectType> object = null;
        try {
            object = modelService.getObject(type, ref.getOid(), null, task, task.getResult());
        } catch (Exception e) {
            LOGGER.error("Couldn't get object from objectRef " + ref, e);
        }
        return object;
    }

    private ContainerTag createTable() {
        return TagCreator.table().withClasses("table", "table-striped", "table-hover", "table-bordered");
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

    private ContainerTag createTable(GuiObjectListViewType view, List<AuditEventRecord> records, Task task,
            OperationResult result) {
        ContainerTag table = createTable();
        ContainerTag tHead = TagCreator.thead();
        ContainerTag tBody = TagCreator.tbody();
        List<GuiObjectColumnType> columns = MiscSchemaUtil.orderCustomColumns(view.getColumn());
        ContainerTag trForHead = TagCreator.tr().withStyle("width: 100%;");
        columns.forEach(column -> {
            Validate.notNull(column.getName(), "Name of column is null");

            DisplayType columnDisplay = column.getDisplay();
            String label;
            if(columnDisplay != null && columnDisplay.getLabel() != null) {
                label = getMessage(columnDisplay.getLabel().getOrig());
            } else if (specialKeyLocalization.containsKey(column.getName())) {
                label = getMessage(specialKeyLocalization.get(column.getName()));
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
                tr.with(TagCreator
                        .th(TagCreator.div(getStringForColumnOfAuditRecord(column.getName(), record, column.getPath(), expression, task, result)).withStyle("white-space: pre-wrap")));
            });
            tBody.with(tr);
        });
        table.with(tBody);
        return table;
    }

    private ContainerTag createTable(GuiObjectListViewType view, List<PrismObject<ObjectType>> values,
            PrismObjectDefinition<ObjectType> def, Class<? extends ObjectType> type,
            Task task, OperationResult result) {
        ContainerTag table = createTable();
        ContainerTag tHead = TagCreator.thead();
        ContainerTag tBody = TagCreator.tbody();
        List<GuiObjectColumnType> columns = MiscSchemaUtil.orderCustomColumns(view.getColumn());
        ContainerTag trForHead = TagCreator.tr().withStyle("width: 100%;");
        columns.forEach(column -> {
            Validate.notNull(column.getName(), "Name of column is null");
            ItemPath path = ItemPath.create(column.getPath());
            if(path == null) {
                LinkedHashMap<String, ItemPath> columnForType = getColumnsForType(type);
                if (columnForType == null) {
                    LOGGER.error("Couldn't found default columns for class {}", type.getName());
                }
                path = columnForType.get(column.getName());
                if(path == null) {
                    LOGGER.error("Couldn't found path for column {} in default columns for class {}", column.getName(), type.getName());
                }
            }

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
                ItemPath path = ItemPath.create(column.getPath());
                if(path == null) {
                    path = getColumnsForType(type).get(column.getName());
                }
                ExpressionType expression = column.getExpression();
                tr.with(TagCreator
                        .th(TagCreator.div(getRealValueAsString(column.getName(), value, path, expression, task, result)).withStyle("white-space: pre-wrap")));
            });
            tBody.with(tr);
        });
        table.with(tBody);
        return table;
    }

    private ContainerTag createTable(List<PrismObject<ObjectType>> values, PrismObjectDefinition<ObjectType> def,
            Class<ObjectType> type, Task task, OperationResult result) {
        HashMap<String, ItemPath> columns = getColumnsForType(type);
        if (columns == null) {
            LOGGER.error("Couldn't create table for objects with class {}", type.getName());
        }
        ContainerTag table = createTable();
        table.with(createTHead(columns, def));
        ContainerTag tBody = TagCreator.tbody();
        values.forEach(value -> {
            ContainerTag tr = TagCreator.tr();
            columns.keySet().forEach(column -> {
                tr.with(TagCreator
                        .th(TagCreator.div(getRealValueAsString(column, value, columns.get(column), null, task, result)).withStyle("white-space: pre-wrap")));
            });
            tBody.with(tr);
        });
        table.with(tBody);
        return table;
    }

    private String getLabelNameForCustom(String nameOfColumn) {
        String key = "";
        switch (nameOfColumn) {
        case ACCOUNTS_COLUMN:
            key = "FocusType.accounts";
        case CURRENT_RUN_TIME_COLUMN:
            key = "TaskType.currentRunTime";
        }
        return getMessage(key);
    }

    private ContainerTag createTHead(Set<String> set) {
        return TagCreator.thead(TagCreator.tr(TagCreator.each(set,
                header -> TagCreator.th(TagCreator.div(TagCreator.span(getMessage(specialKeyLocalization.get(header))).withClass("sortableLabel")))
                .withStyle(getStyleForColumn(header)))).withStyle("width: 100%;"));
    }

    private ContainerTag createTHead(String prefix, Set<String> set) {
        return TagCreator.thead(TagCreator.tr(TagCreator.each(set,
                header -> TagCreator.th(TagCreator.div(TagCreator.span(getMessage(prefix+header)).withClass("sortableLabel")))
                .withStyle(getStyleForColumn(header)))).withStyle("width: 100%;"));
    }

    private ContainerTag createTHead(HashMap<String, ItemPath> columns, PrismObjectDefinition<ObjectType> objectDefinition) {
        ContainerTag tr = TagCreator.tr().withStyle("width: 100%;");
        columns.keySet().forEach(columnName -> {
            tr.with(TagCreator.th(TagCreator.div(TagCreator.span(getColumnLabel(columnName, objectDefinition, columns.get(columnName)))
                    .withClass("sortableLabel"))));
        });
        return TagCreator.thead(tr);
    }

    private String getColumnLabel(String name, PrismObjectDefinition<ObjectType> objectDefinition, ItemPath path) {
        if(specialKeyLocalization.containsKey(name)) {
            return getMessage(specialKeyLocalization.get(name));
        }
        if(isCustomPerformedColumn(path)) {
            return getLabelNameForCustom(name);
        }
        ItemDefinition def = objectDefinition.findItemDefinition(path);
        if(def == null) {
            throw new IllegalArgumentException("Could'n find item for path " + path);
        }
        String displayName = def.getDisplayName();
        return getMessage(displayName);
    }

    private String getStyleForColumn(String column) {
        switch (column) {
        case TIME_COLUMN:
            return "width: 10%;";
        case INITIATOR_COLUMN:
            return "width: 8%;";
        case EVENT_STAGE_COLUMN:
            return "width: 5%;";
        case EVENT_TYPE_COLUMN:
            return "width: 10%;";
        case TARGET_COLUMN:
            return "width: 8%;";
        case OUTCOME_COLUMN:
            return "width: 7%;";
        case DELTA_COLUMN:
            return "width: 35%;";
        }
        return "";
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
            addStatusColor(th, data.getDisplay());
            th.with(div);
            return th;
        }
        return TagCreator.th();
    }

    private void addStatusColor(ContainerTag div, DisplayType display) {
        if (display != null && StringUtils.isNoneBlank(display.getColor())) {
            div.withStyle("background-color: " + display.getColor() + "; !important;");
        }

    }

    private Set<String> getHeadsOfWidget() {
        return headsOfWidget;
    }

    private static Set<String> getHeadsOfAuditEventRecords() {
        return headsOfAuditEventRecords;
    }

    private static LinkedHashMap<Class<? extends ObjectType>, LinkedHashMap<String, ItemPath>> getColumnDef() {
        return columnDef;
    }

    private static LinkedHashMap<String, ItemPath> getColumnsForType(Class<? extends ObjectType> type ) {
        if(type.equals(RoleType.class)
                || type.equals(OrgType.class)
                || type.equals(ServiceType.class)) {
            return getColumnDef().get(AbstractRoleType.class);
        }
        return getColumnDef().get(type);
    }

    @Override
    protected ExportType getExport(ReportType report) {
        return ExportType.HTML;
    }

    private String getMessage(Enum e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getDeclaringClass().getSimpleName()).append('.');
        sb.append(e.name());
        return getMessage(sb.toString());
    }

    private String getMessage(String key) {
        return getMessage(key, null);
    }

    private String getMessage(String key, Object... params) {
        return localizationService.translate(key, params, Locale.getDefault(), key);
    }
}
