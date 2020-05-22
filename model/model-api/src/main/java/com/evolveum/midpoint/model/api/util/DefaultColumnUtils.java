/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.AuditConstants;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil;
import com.evolveum.midpoint.task.api.TaskUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

/**
 * @author skublik
 */

public class DefaultColumnUtils {

    private static final Trace LOGGER = TraceManager.getTrace(DefaultColumnUtils.class);

    private static LinkedHashMap<Class<? extends ObjectType>, List<ItemPath>> columnsDef;
    private static List<ItemPath> objectColumnsDef;
    private static List<ItemPath> defaultAuditColumnsDef;
    private static List<ItemPath> numberColumns;
    private static LinkedHashMap<Class<?>, LinkedHashMap<ItemPath, String>> specificLocalization;

    static {
        specificLocalization = new LinkedHashMap<Class<?>, LinkedHashMap<ItemPath, String>>() {
            private static final long serialVersionUID = 1L;
            {
                put(UserType.class, new LinkedHashMap<ItemPath, String>() {
                    private static final long serialVersionUID = 1L;
                    {
                        put(UserType.F_LINK_REF, "FocusType.accounts");
                    }
                });
                put(TaskType.class, new LinkedHashMap<ItemPath, String>() {
                    private static final long serialVersionUID = 1L;
                    {
                        put(TaskType.F_COMPLETION_TIMESTAMP, "TaskType.currentRunTime");
                        put(TaskType.F_NODE_AS_OBSERVED, "pageTasks.task.executingAt");
                        put(TaskType.F_SCHEDULE, "pageTasks.task.scheduledToRunAgain");
                        put(ItemPath.create(TaskType.F_OPERATION_STATS, OperationStatsType.F_ITERATIVE_TASK_INFORMATION,
                                IterativeTaskInformationType.F_TOTAL_FAILURE_COUNT), "pageTasks.task.errors");

                    }
                });
                put(ResourceType.class, new LinkedHashMap<ItemPath, String>() {
                    private static final long serialVersionUID = 1L;
                    {
                        put(ItemPath.create(ResourceType.F_CONNECTOR_REF, ConnectorType.F_CONNECTOR_TYPE),
                                "ConnectorType.connectorType");
                        put(ItemPath.create(ResourceType.F_CONNECTOR_REF, ConnectorType.F_CONNECTOR_VERSION),
                                "ConnectorType.connectorVersion");
                    }
                });
                put(AuditEventRecordType.class, new LinkedHashMap<ItemPath, String>() {
                    private static final long serialVersionUID = 1L;
                    {
                        put(new ItemName(AuditConstants.TIME_COLUMN), AuditConstants.TIME_COLUMN_KEY);
                        put(new ItemName(AuditConstants.INITIATOR_COLUMN), AuditConstants.INITIATOR_COLUMN_KEY);
                        put(new ItemName(AuditConstants.EVENT_STAGE_COLUMN), AuditConstants.EVENT_STAGE_COLUMN_KEY);
                        put(new ItemName(AuditConstants.EVENT_TYPE_COLUMN), AuditConstants.EVENT_TYPE_COLUMN_KEY);
                        put(new ItemName(AuditConstants.TARGET_COLUMN), AuditConstants.TARGET_COLUMN_KEY);
                        put(new ItemName(AuditConstants.OUTCOME_COLUMN), AuditConstants.OUTCOME_COLUMN_KEY);
                        put(new ItemName(AuditConstants.MESSAGE_COLUMN), AuditConstants.MESSAGE_COLUMN_KEY);
                        put(new ItemName(AuditConstants.DELTA_COLUMN), AuditConstants.DELTA_COLUMN_KEY);
                        put(new ItemName(AuditConstants.TARGET_OWNER_COLUMN), AuditConstants.TARGET_OWNER_COLUMN_KEY);
                        put(new ItemName(AuditConstants.CHANNEL_COLUMN), AuditConstants.CHANNEL_COLUMN_KEY);
                        put(new ItemName(AuditConstants.TASK_OID_COLUMN), AuditConstants.TASK_OID_COLUMN_KEY);
                        put(new ItemName(AuditConstants.NODE_IDENTIFIER_COLUMN), AuditConstants.NODE_IDENTIFIER_COLUMN_KEY);
                        put(new ItemName(AuditConstants.ATTORNEY_COLUMN), AuditConstants.ATTORNEY_COLUMN_KEY);
                        put(new ItemName(AuditConstants.RESULT_COLUMN), AuditConstants.RESULT_COLUMN_KEY);
                        put(new ItemName(AuditConstants.RESOURCE_OID_COLUMN), AuditConstants.RESOURCE_OID_COLUMN_KEY);
                    }
                });
            }
        };

        numberColumns = new ArrayList<ItemPath>(Arrays.asList(
                FocusType.F_LINK_REF
        ));

        objectColumnsDef = new ArrayList<ItemPath>(Arrays.asList(
                ObjectType.F_NAME
        ));

        defaultAuditColumnsDef = new ArrayList<ItemPath>(Arrays.asList(
                new ItemName(AuditConstants.TIME_COLUMN),
                new ItemName(AuditConstants.INITIATOR_COLUMN),
                new ItemName(AuditConstants.EVENT_STAGE_COLUMN),
                new ItemName(AuditConstants.EVENT_TYPE_COLUMN),
                new ItemName(AuditConstants.TARGET_COLUMN),
                new ItemName(AuditConstants.OUTCOME_COLUMN),
                new ItemName(AuditConstants.MESSAGE_COLUMN),
                new ItemName(AuditConstants.DELTA_COLUMN)
        ));

        columnsDef = new LinkedHashMap<Class<? extends ObjectType>, List<ItemPath>>() {
            private static final long serialVersionUID = 1L;

            {
                put(ResourceType.class, new ArrayList<ItemPath>(Arrays.asList(
                        ResourceType.F_NAME,
                        ItemPath.create(ResourceType.F_CONNECTOR_REF, ConnectorType.F_CONNECTOR_TYPE),
                        ItemPath.create(ResourceType.F_CONNECTOR_REF, ConnectorType.F_CONNECTOR_VERSION)
                )));

                put(UserType.class, new ArrayList<ItemPath>(Arrays.asList(
                        UserType.F_NAME,
                        UserType.F_GIVEN_NAME,
                        UserType.F_FAMILY_NAME,
                        UserType.F_FULL_NAME,
                        UserType.F_EMAIL_ADDRESS,
                        UserType.F_LINK_REF
                )));

                put(AbstractRoleType.class, new ArrayList<ItemPath>(Arrays.asList(
                        AbstractRoleType.F_NAME,
                        AbstractRoleType.F_DISPLAY_NAME,
                        AbstractRoleType.F_DESCRIPTION,
                        AbstractRoleType.F_IDENTIFIER,
                        AbstractRoleType.F_LINK_REF
                )));

                put(TaskType.class, new ArrayList<ItemPath>(Arrays.asList(
                        TaskType.F_NAME,
                        TaskType.F_CATEGORY,
                        TaskType.F_EXECUTION_STATUS,
                        TaskType.F_OBJECT_REF,
                        TaskType.F_NODE_AS_OBSERVED,
                        TaskType.F_COMPLETION_TIMESTAMP,
                        TaskType.F_PROGRESS,
                        TaskType.F_SCHEDULE,
                        ItemPath.create(TaskType.F_OPERATION_STATS, OperationStatsType.F_ITERATIVE_TASK_INFORMATION,
                                IterativeTaskInformationType.F_TOTAL_FAILURE_COUNT),
                        TaskType.F_RESULT_STATUS
                )));

                put(ShadowType.class, new ArrayList<ItemPath>(Arrays.asList(
                        ShadowType.F_NAME,
                        ShadowType.F_RESOURCE_REF,
                        ShadowType.F_KIND,
                        ShadowType.F_INTENT,
                        ShadowType.F_SYNCHRONIZATION_SITUATION
                )));
            }

        };
    }

    private static List<ItemPath> getColumnsForType(Class<? extends ObjectType> type ) {
        if(type.equals(RoleType.class)
                || type.equals(OrgType.class)
                || type.equals(ServiceType.class)) {
            return getColumnsDef().get(AbstractRoleType.class);
        }
        if(getColumnsDef().containsKey(type)) {
            return getColumnsDef().get(type);
        }
        return getObjectColumnsDef();
    }

    public static LinkedHashMap<Class<? extends ObjectType>, List<ItemPath>> getColumnsDef() {
        return columnsDef;
    }

    public static List<ItemPath> getObjectColumnsDef() {
        return objectColumnsDef;
    }

    public static List<ItemPath> getDefaultAuditColumnsDef() {
        return defaultAuditColumnsDef;
    }

    public static <O extends ObjectType> GuiObjectListViewType getDefaultView(Class<? extends O> type) {
        if (type == null) {
            return getDefaultObjectView();
        }

        if (UserType.class.equals(type)) {
            return getDefaultUserView();
        } else if (RoleType.class.equals(type)) {
            return getDefaultRoleView();
        } else if (OrgType.class.equals(type)) {
            return getDefaultOrgView();
        } else if (ServiceType.class.equals(type)) {
            return getDefaultServiceView();
        } else if (TaskType.class.equals(type)) {
            return getDefaultTaskView();
        } else if (ResourceType.class.equals(type)) {
            return getDefaultResourceView();
        } else if (ShadowType.class.equals(type)) {
            return getDefaultShadowView();
        } else {
            return getDefaultObjectView();
        }
    }

    public static GuiObjectListViewType getDefaultAuditEventsView() {
        GuiObjectListViewType view = new GuiObjectListViewType();
        view.setType(AuditEventRecordType.COMPLEX_TYPE);
        view.setIdentifier("default-audit-event");
        view.createColumnList();
        List<GuiObjectColumnType> columns = view.getColumn();
        List<ItemPath> defaultColumns = getDefaultAuditColumnsDef();
        String previousColumn = null;
        for (ItemPath defaultColumn : defaultColumns) {
            String columnName = defaultColumn.lastName().getLocalPart() + "Column";
            GuiObjectColumnType column = new GuiObjectColumnType();
            column.setName(columnName);
            column.setPreviousColumn(previousColumn);
            ItemPathType itemPathType = new ItemPathType();
            itemPathType.setItemPath(defaultColumn);
            column.setPath(itemPathType);
            String key = getLocalizationKeyForAuditColumn(defaultColumn);
            if (key != null) {
                DisplayType display = new DisplayType();
                display.setLabel(new PolyStringType(key));
                column.setDisplay(display);
            }
            columns.add(column);
            previousColumn = columnName;
        }
        return view;
    }

    public static String getLocalizationKeyForAuditColumn(ItemPath itemPath) {
        if (specificLocalization.containsKey(AuditEventRecordType.class)
                && specificLocalization.get(AuditEventRecordType.class).containsKey(itemPath)){
            return specificLocalization.get(AuditEventRecordType.class).get(itemPath);
        }
        return null;
    }

    public static GuiObjectListViewType getDefaultShadowView() {
        return getDefaultView(ShadowType.COMPLEX_TYPE, "default-shadow", ShadowType.class);
    }

    public static GuiObjectListViewType getDefaultResourceView() {
        return getDefaultView(ResourceType.COMPLEX_TYPE, "default-resource", ResourceType.class);
    }

    public static GuiObjectListViewType getDefaultTaskView() {
        return getDefaultView(TaskType.COMPLEX_TYPE, "default-task", TaskType.class);
    }

    public static GuiObjectListViewType getDefaultServiceView() {
        return getDefaultView(ServiceType.COMPLEX_TYPE, "default-service", ServiceType.class);
    }

    public static GuiObjectListViewType getDefaultOrgView() {
        return getDefaultView(OrgType.COMPLEX_TYPE, "default-org", OrgType.class);
    }

    private static GuiObjectListViewType getDefaultRoleView() {
        return getDefaultView(RoleType.COMPLEX_TYPE, "default-role", RoleType.class);
    }

    public static GuiObjectListViewType getDefaultUserView() {
        return getDefaultView(UserType.COMPLEX_TYPE, "default-user", UserType.class);
    }

    public static GuiObjectListViewType getDefaultObjectView() {
        return getDefaultView(ObjectType.COMPLEX_TYPE, "default-object", ObjectType.class);
    }

    private static <O extends ObjectType> GuiObjectListViewType getDefaultView(QName qname, String identifier, Class<? extends O> type) {
        GuiObjectListViewType view = new GuiObjectListViewType();
        view.setType(qname);
        view.setIdentifier(identifier);
        createColumns(view, type);
        return view;
    }

    private static <O extends ObjectType> void createColumns(GuiObjectListViewType view, Class<? extends O> type) {
        view.createColumnList();
        List<GuiObjectColumnType> columns = view.getColumn();
        List<ItemPath> defaultColumns = getColumnsForType(type);
        String previousColumn = null;
        for (ItemPath defaultColumn : defaultColumns) {
            String columnName = defaultColumn.lastName().getLocalPart() + "Column";
            GuiObjectColumnType column = new GuiObjectColumnType();
            column.setName(columnName);
            column.setPreviousColumn(previousColumn);
            ItemPathType itemPathType = new ItemPathType();
            itemPathType.setItemPath(defaultColumn);
            column.setPath(itemPathType);
            if (numberColumns.contains(defaultColumn)) {
                column.setDisplayValue(DisplayValueType.NUMBER);
            }
            if (specificLocalization.containsKey(type) && specificLocalization.get(type).containsKey(defaultColumn)){
                DisplayType display = new DisplayType();
                display.setLabel(new PolyStringType(specificLocalization.get(type).get(defaultColumn)));
                column.setDisplay(display);
            }
            columns.add(column);
            previousColumn = columnName;
        }
    }

    public static Object getObjectByAuditColumn(AuditEventRecord record, ItemPath path) {
        Object object = null;
        switch (path.toString()) {
            case AuditConstants.TIME_COLUMN:
                object = record.getTimestamp();
                break;
            case AuditConstants.INITIATOR_COLUMN:
                object = record.getInitiator();
                break;
            case AuditConstants.EVENT_STAGE_COLUMN:
                object = record.getEventStage();
                break;
            case AuditConstants.EVENT_TYPE_COLUMN:
                object = record.getEventType();
                break;
            case AuditConstants.TARGET_COLUMN:
                object = record.getTarget();
                break;
            case AuditConstants.TARGET_OWNER_COLUMN:
                object = record.getTargetOwner();
                break;
            case AuditConstants.CHANNEL_COLUMN:
                object = record.getChannel();
                break;
            case AuditConstants.OUTCOME_COLUMN:
                object = record.getOutcome();
                break;
            case AuditConstants.MESSAGE_COLUMN:
                object = record.getMessage();
                break;
            case AuditConstants.DELTA_COLUMN:
                object = record.getDeltas();
                break;
            case AuditConstants.TASK_OID_COLUMN:
                object = record.getTaskOid();
                break;
            case AuditConstants.NODE_IDENTIFIER_COLUMN:
                object = record.getNodeIdentifier();
                break;
            case AuditConstants.ATTORNEY_COLUMN:
                object = record.getAttorney();
                break;
            case AuditConstants.RESULT_COLUMN:
                object = record.getResult();
                break;
            case AuditConstants.RESOURCE_OID_COLUMN:
                object = record.getResourceOids();
                break;
            default:
                if(record.getCustomColumnProperty().containsKey(path)) {
                    object = record.getCustomColumnProperty().get(path);
                } else {
                    LOGGER.error("Unknown name of column for AuditReport " + path);
                    throw new IllegalArgumentException("Unknown name of column for AuditReport " + path);
                }
                break;
        }
        return object;
    }

    public static <O extends ObjectType> String processSpecialColumn(ItemPath itemPath, PrismObject<O> object,
            LocalizationService localization){
        @Nullable Class<O> type = object.getCompileTimeClass();
        if (type == null || itemPath == null) {
            return null;
        }
        if (type.isAssignableFrom(TaskType.class)) {
            TaskType task = (TaskType) object.getRealValue();
            if (itemPath.equivalent(TaskType.F_COMPLETION_TIMESTAMP)) {
                XMLGregorianCalendar timestamp = task.getCompletionTimestamp();
                if(timestamp != null && task.getExecutionStatus().equals(TaskExecutionStatusType.CLOSED)) {
                    String pattern = DateTimeFormat.patternForStyle("SM", Locale.getDefault());
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                    ZonedDateTime time = timestamp.toGregorianCalendar().toZonedDateTime();
                    String dateTime = formatter.format(time);
//                    String time = formatter.format(timestamp.toGregorianCalendar().toZonedDateTime());
                    String key = "pageTasks.task.closedAt";
                    return localization.translate(key, null, Locale.getDefault(), key) + " " + dateTime;
                }
                return "";
            } else if (itemPath.equivalent(TaskType.F_SCHEDULE)) {
                List<Object> localizationObject = new ArrayList<Object>();
                String key = TaskUtil.createScheduledToRunAgain(task, localizationObject);
                Object[] params = localizationObject.isEmpty() ? null : localizationObject.toArray();
                return localization.translate(key, params, Locale.getDefault(), key);
            } else if (itemPath.equivalent(ItemPath.create(TaskType.F_OPERATION_STATS, OperationStatsType.F_ITERATIVE_TASK_INFORMATION,
                    IterativeTaskInformationType.F_TOTAL_FAILURE_COUNT))) {
                if (task.getOperationStats() != null && task.getOperationStats().getIterativeTaskInformation() != null) {
                   return String.valueOf(task.getOperationStats().getIterativeTaskInformation().getTotalFailureCount());
                }
                return "0";
            } else if (itemPath.equivalent(TaskType.F_PROGRESS)) {
                List<Object> localizationObject = new ArrayList<Object>();
                String key = TaskUtil.getProgressDescription(task, localizationObject);
                Object[] params = localizationObject.isEmpty() ? null : localizationObject.toArray();
                return localization.translate(key, params, Locale.getDefault(), key);
            }
        }
        return null;
    }



    public static <O extends ObjectType> boolean isSpecialColumn(ItemPath itemPath, PrismObject<O> object){
        @Nullable Class<O> type = object.getCompileTimeClass();
        if (type == null || itemPath == null) {
            return false;
        }
        if (type.isAssignableFrom(TaskType.class)) {
            if (itemPath.equivalent(TaskType.F_COMPLETION_TIMESTAMP)
                || itemPath.equivalent(TaskType.F_SCHEDULE)
                || itemPath.equivalent(ItemPath.create(TaskType.F_OPERATION_STATS, OperationStatsType.F_ITERATIVE_TASK_INFORMATION,
                    IterativeTaskInformationType.F_TOTAL_FAILURE_COUNT))
                || itemPath.equivalent(TaskType.F_PROGRESS)) {
                return true;
            }
        }
        return false;
    }

    public static Collection<SelectorOptions<GetOperationOptions>> createOption(Class<ObjectType> type, SchemaHelper schemaHelper) {
        if (type == null) {
            return null;
        }
        List<QName> propertiesToGet = new ArrayList<>();
        GetOperationOptionsBuilder getOperationOptionsBuilder = schemaHelper.getOperationOptionsBuilder();
        getOperationOptionsBuilder = getOperationOptionsBuilder.resolveNames();

        if (TaskType.class.isAssignableFrom(type)) {
            propertiesToGet.add(TaskType.F_NODE_AS_OBSERVED);
            propertiesToGet.add(TaskType.F_NEXT_RUN_START_TIMESTAMP);
            propertiesToGet.add(TaskType.F_NEXT_RETRY_TIMESTAMP);
            propertiesToGet.add(TaskType.F_SUBTASK_REF);
        } else if (ResourceType.class.isAssignableFrom(type)) {
            propertiesToGet.add(ResourceType.F_CONNECTOR_REF);
        } else if (ShadowType.class.isAssignableFrom(type)) {
            getOperationOptionsBuilder = getOperationOptionsBuilder.raw();
        }
        return getOperationOptionsBuilder
                .items(propertiesToGet.toArray(new Object[0])).retrieve()
                .build();
    }
}
