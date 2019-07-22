/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.report.impl;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import com.evolveum.midpoint.report.api.ReportService;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.interaction.DashboardService;
import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.model.api.util.DashboardUtils;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
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
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetPresentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetSourceTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
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

import j2html.TagCreator;
import j2html.tags.ContainerTag;

/**
 * @author skublik
 */

@Component
public class ReportHTMLCreateTaskHandler extends ReportJasperCreateTaskHandler {

	public static final String REPORT_HTML_CREATE_TASK_URI = "http://midpoint.evolveum.com/xml/ns/public/report/html/create/handler-3";
	private static final Trace LOGGER = TraceManager.getTrace(ReportHTMLCreateTaskHandler.class);

	private static final String REPORT_CSS_STYLE_FILE_NAME = "dashboard-report-style.css";
	private static final String LABEL_COLUMN = "Label";
	private static final String NUMBER_COLUMN = "Number";
	private static final String STATUS_COLUMN = "Status";
	private static final String TIME_COLUMN = "Time";
	private static final String INITIATOR_COLUMN = "Initiator";
	private static final String EVENT_STAGE_COLUMN = "Event stage";
	private static final String EVENT_TYPE_COLUMN = "Event type";
	private static final String TARGET_COLUMN = "Target";
	private static final String DELTA_COLUMN = "Delta";
	private static final String MESSAGE_COLUMN = "Message";
	private static final String TARGET_OWNER_COLUMN = "Target owner";
	private static final String CHANNEL_COLUMN = "Channel";
	private static final String OUTCOME_COLUMN = "Outcome";
	private static final String NAME_COLUMN = "Name";
	private static final String CONNECTOR_TYPE_COLUMN = "Connector type";
	private static final String VERSION_COLUMN = "Version";
	private static final String GIVEN_NAME_COLUMN = "Given name";
	private static final String FAMILY_NAME_COLUMN = "Family name";
	private static final String FULL_NAME_COLUMN = "Full name";
	private static final String EMAIL_COLUMN = "Email";
	private static final String ACCOUNTS_COLUMN = "Accounts";
	private static final String DISPLAY_NAME_COLUMN = "Display name";
	private static final String DESCRIPTION_COLUMN = "Description";
	private static final String IDENTIFIER_COLUMN = "Identifier";
	private static final String CATEGORY_COLUMN = "Category";
	private static final String OBJECT_REFERENCE_COLUMN = "Object reference";
	private static final String EXECUTION_COLUMN = "Execution";
	private static final String EXECUTING_AT_COLUMN = "Executing at";
	private static final String PROGRES_COLUMN = "Progress";
	private static final String CURRENT_RUN_TIME_COLUMN = "Current run time";
	private static final String SCHEDULED_TO_START_AGAIN_COLUMN = "Scheduled to start again";
	
	private static final String REPORT_GENERATED_ON = "Report generated on: ";
	private static final String NUMBER_OF_RECORDS = "Number of records: ";
	
	private static final QName CUSTOM = new QName("custom");

	@Autowired private Clock clock;
	@Autowired private TaskManager taskManager;
	@Autowired private AuditService auditService;
	@Autowired private ReportService reportService;
	@Autowired private ModelService modelService;
	@Autowired private PrismContext prismContext;
	@Autowired @Qualifier("modelObjectResolver") private ObjectResolver objectResolver;
	@Autowired private DashboardService dashboardService;

	private static LinkedHashMap<Class<? extends ObjectType>, LinkedHashMap<String, ItemPath>> columnDef;
	private static Set<String> headsOfWidget;
	private static Set<String> headsOfAuditEventRecords;

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
						put(VERSION_COLUMN,
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
//						put(SCHEDULED_TO_START_AGAIN_COLUMN, ItemPath.create(CUSTOM));
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
//				add(TARGET_OWNER_COLUMN);
//				add(TARGET_OWNER_COLUMN);
//				add(CHANNEL_COLUMN);
				add(OUTCOME_COLUMN);
				add(MESSAGE_COLUMN);
				add(DELTA_COLUMN);
			}
		};
	}

	@Override
	protected void initialize() {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Registering with taskManager as a handler for " + REPORT_HTML_CREATE_TASK_URI);
		}
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
					String style = "";
					ClassLoader classLoader = getClass().getClassLoader();
					InputStream in = classLoader.getResourceAsStream(REPORT_CSS_STYLE_FILE_NAME);
					byte[] data = IOUtils.toByteArray(in);
					style = new String(data, Charset.defaultCharset());

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
		widgetTable.with(createTHead(getHeadsOfWidget()));

		ContainerTag widgetTBody = TagCreator.tbody();
		List<ContainerTag> tableboxesFromWidgets = new ArrayList<ContainerTag>();
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
				convertMillisToString(startMillis)).render());
		appendSpace(body);
		tableboxesFromWidgets.forEach(table -> {
			body.append(table.render());
			appendSpace(body);
		});
		body.append("</div>");

		return body.toString();
	}

	private String convertMillisToString(long millis) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss", Locale.US);
		return dateFormat.format(millis);
	}

	private void appendSpace(StringBuilder body) {
		body.append("<br>");
	}

	private ContainerTag createTableBoxForWidget(DashboardWidget widgetData, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		DashboardWidgetType widget = widgetData.getWidget();
		if (widget == null) {
			throw new IllegalArgumentException("Widget in DashboardWidget is null");
		}
		DashboardWidgetSourceTypeType sourceType = DashboardUtils.getSourceType(widget);
		DashboardWidgetPresentationType presentation = widget.getPresentation();
		switch (sourceType) {
		case OBJECT_COLLECTION:
			if (!DashboardUtils.isDataFieldsOfPresentationNullOrEmpty(presentation)) {
				ObjectCollectionType collection = dashboardService.getObjectCollectionType(widget, task, result);
				long startMillis = clock.currentTimeMillis();
				List<PrismObject<ObjectType>> values = dashboardService.searchObjectFromCollection(collection, true, task, result);
				if (values == null || values.isEmpty()) {
					return null;
				}
				Class<ObjectType> type = (Class<ObjectType>) prismContext.getSchemaRegistry()
						.getCompileTimeClassForObjectType(collection.getType());
				HashMap<String, ItemPath> columns = getColumnsForType(type);
				if (columns == null) {
					LOGGER.error("Couldn't create table for objects with class {}", type.getName());
				}
				ContainerTag table = createTable();
				table.with(createTHead(columns.keySet()));
				ContainerTag tBody = TagCreator.tbody();
				values.forEach(value -> {
					ContainerTag tr = TagCreator.tr();
					columns.keySet().forEach(column -> {
						tr.with(TagCreator
								.th(TagCreator.div(getRealValueAsString(column, value, columns.get(column)))));
					});
					tBody.with(tr);
				});
				table.with(tBody);
				return createTableBox(table, widgetData.getLabel(), values.size(),
						convertMillisToString(startMillis));
			}
			break;
		case AUDIT_SEARCH:
			if (!DashboardUtils.isDataFieldsOfPresentationNullOrEmpty(presentation)) {
				Map<String, Object> parameters = new HashMap<String, Object>();

				ObjectCollectionType collection = dashboardService.getObjectCollectionType(widget, task, result);
				long startMillis = clock.currentTimeMillis();
				String query = DashboardUtils
						.getQueryForListRecords(DashboardUtils.createQuery(collection, parameters, false, clock));
				List<AuditEventRecord> records = auditService.listRecords(query, parameters);
				if (records == null || records.isEmpty()) {
					return null;
				}

				List<AuditEventRecordType> auditRecordList = new ArrayList<>();
				for (AuditEventRecord record : records) {
					auditRecordList.add(record.createAuditEventRecordType());
				}

				ContainerTag table = createTable(true);
				table.with(createTHead(getHeadsOfAuditEventRecords()));
				ContainerTag tBody = TagCreator.tbody();
				auditRecordList.forEach(record -> {
					ContainerTag tr = TagCreator.tr();
					getHeadsOfAuditEventRecords().forEach(column -> {
						tr.with(TagCreator.th(TagCreator.div(getColumnForAuditEventRecord(column, record))));
					});
					tBody.with(tr);
				});
				table.with(tBody);
				return createTableBox(table, widgetData.getLabel(), auditRecordList.size(),
						convertMillisToString(startMillis));
			}
			break;
		}
		return null;
	}

	private String getColumnForAuditEventRecord(String column, AuditEventRecordType record) {
		switch (column) {
		case TIME_COLUMN:
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d. MMM yyyy HH:mm:ss", Locale.US);
			return dateFormat.format(record.getTimestamp().toGregorianCalendar().getTime());
		case INITIATOR_COLUMN:
			ObjectReferenceType initiatorRef = record.getInitiatorRef();
			return getObjectNameFromRef(initiatorRef);
		case EVENT_STAGE_COLUMN:
			return record.getEventStage() == null ? "" : record.getEventStage().toString();
		case EVENT_TYPE_COLUMN:
			return record.getEventType() == null ? "" : record.getEventType().toString();
		case TARGET_COLUMN:
			ObjectReferenceType targetRef = record.getTargetRef();
			return getObjectNameFromRef(targetRef);
		case TARGET_OWNER_COLUMN:
			ObjectReferenceType targetOwnerRef = record.getTargetOwnerRef();
			return getObjectNameFromRef(targetOwnerRef);
		case CHANNEL_COLUMN:

			return record.getChannel() == null ? "" : QNameUtil.uriToQName(record.getChannel()).getLocalPart();
		case OUTCOME_COLUMN:
			if (record.getOutcome() == null) {
				return "";
			}
			return record.getOutcome().toString();
		case MESSAGE_COLUMN:
			if (record.getMessage() == null) {
				return "";
			}
			return record.getMessage();
		case DELTA_COLUMN:
			if (record.getDelta() == null || record.getDelta().isEmpty()) {
				return "";
			}
			StringBuilder sb = new StringBuilder();
			List<ObjectDeltaOperationType> deltas = record.getDelta();
			for (int i = 0; i < deltas.size(); i++) {
				ObjectDeltaOperationType delta = deltas.get(i);
				sb.append(ReportUtils.printDelta(delta.getObjectDelta(), 
						(delta.getObjectName() == null ? null : delta.getObjectName().toString()),
						(delta.getResourceName()) == null ? null : delta.getResourceName().toString()));
				if ((i+1)!=deltas.size()) {
					sb.append("\n");
				}
			}
			return sb.toString();
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

	private String getRealValueAsString(String nameOfColumn, PrismObject<ObjectType> object, ItemPath itemPath) {
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
			if (valueObject instanceof PrismContainer && !iterator.hasNext()) {
				throw new IllegalArgumentException("Found object is PrismContainer, but ItemPath is empty");
			}
			if (valueObject instanceof PrismReference) {
				Referencable ref = ((PrismReference) valueObject).getRealValue();
				if (!iterator.hasNext()) {
					return getObjectNameFromRef(ref);
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
		return ((PrismProperty) valueObject).getRealValue().toString();
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
		case SCHEDULED_TO_START_AGAIN_COLUMN:
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
		return createTable(false);
	}

	private ContainerTag createTable(boolean isAudit) {
		if(isAudit) {
			return TagCreator.table().withClasses("table", "table-striped", "table-hover", "table-bordered", "table-audit");
		}
		return TagCreator.table().withClasses("table", "table-striped", "table-hover", "table-bordered");
	}

	private ContainerTag createTableBox(ContainerTag table, String nameOfTable, int countOfTableRecords,
			String createdTime) {
		ContainerTag div = TagCreator.div().withClasses("box-body", "no-padding").with(TagCreator.h1(nameOfTable))
				.with(TagCreator.p(REPORT_GENERATED_ON + createdTime))
				.with(TagCreator.p(NUMBER_OF_RECORDS + countOfTableRecords)).with(table);
		return TagCreator.div().withClasses("box", "boxed-table").with(div);
	}

	private ContainerTag createTHead(Set<String> set) {
		return TagCreator.thead(TagCreator.tr(TagCreator.each(set,
				header -> TagCreator.th(TagCreator.div(TagCreator.span(header).withClass("sortableLabel"))).withStyle(getStyleForColumn(header)))).withStyle("width: 100%;"));
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

}
