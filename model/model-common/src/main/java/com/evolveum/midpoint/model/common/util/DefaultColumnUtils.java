/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.util;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordCustomColumnPropertyType;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.task.api.TaskUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Column related utilities shared by reporting and GUI.
 */
public class DefaultColumnUtils {

    private static final Trace LOGGER = TraceManager.getTrace(DefaultColumnUtils.class);

    // Maps need to preserve iteration order (like LinkedHashMap)
    private static final Map<Class<? extends ObjectType>, List<ItemPath>> COLUMNS_DEF;
    private static final List<ItemPath> OBJECT_COLUMNS_DEF;
    private static final List<ItemPath> DEFAULT_AUDIT_COLUMNS_DEF;
    private static final List<ItemPath> NUMBER_COLUMNS;
    private static final Map<Class<?>, Map<ItemPath, String>> SPECIFIC_LOCALIZATION;

    static {
        SPECIFIC_LOCALIZATION = ImmutableMap.<Class<?>, Map<ItemPath, String>>builder()
                .put(UserType.class, ImmutableMap.<ItemPath, String>builder()
                        .put(UserType.F_LINK_REF, "FocusType.accounts")
                        .build())
                .put(TaskType.class, ImmutableMap.<ItemPath, String>builder()
                        .put(TaskType.F_COMPLETION_TIMESTAMP, "TaskType.currentRunTime")
                        .put(TaskType.F_NODE_AS_OBSERVED, "pageTasks.task.executingAt")
                        .put(TaskType.F_SCHEDULE, "pageTasks.task.scheduledToRunAgain")
                        .put(ItemPath.create(TaskType.F_OPERATION_STATS, OperationStatsType.F_ITERATIVE_TASK_INFORMATION,
                                IterativeTaskInformationType.F_TOTAL_FAILURE_COUNT),
                                "pageTasks.task.errors")
                        .build())
                .put(ResourceType.class, ImmutableMap.<ItemPath, String>builder()
                        .put(ItemPath.create(ResourceType.F_CONNECTOR_REF, ConnectorType.F_CONNECTOR_TYPE),
                                "ConnectorType.connectorType")
                        .put(ItemPath.create(ResourceType.F_CONNECTOR_REF, ConnectorType.F_CONNECTOR_VERSION),
                                "ConnectorType.connectorVersion")
                        .build())
                .build();

        NUMBER_COLUMNS = Collections.singletonList(
                FocusType.F_LINK_REF);

        OBJECT_COLUMNS_DEF = Collections.singletonList(ObjectType.F_NAME);

        DEFAULT_AUDIT_COLUMNS_DEF = Arrays.asList(
                AuditEventRecordType.F_TIMESTAMP,
                AuditEventRecordType.F_INITIATOR_REF,
                AuditEventRecordType.F_EVENT_STAGE,
                AuditEventRecordType.F_EVENT_TYPE,
                AuditEventRecordType.F_TARGET_REF,
                AuditEventRecordType.F_OUTCOME,
                AuditEventRecordType.F_MESSAGE,
                AuditEventRecordType.F_DELTA
        );

        COLUMNS_DEF = ImmutableMap.<Class<? extends ObjectType>, List<ItemPath>>builder()
                .put(ResourceType.class, Arrays.asList(
                        ResourceType.F_NAME,
                        ItemPath.create(ResourceType.F_CONNECTOR_REF, ConnectorType.F_CONNECTOR_TYPE),
                        ItemPath.create(ResourceType.F_CONNECTOR_REF, ConnectorType.F_CONNECTOR_VERSION)))
                .put(UserType.class, Arrays.asList(
                        UserType.F_NAME,
                        UserType.F_GIVEN_NAME,
                        UserType.F_FAMILY_NAME,
                        UserType.F_FULL_NAME,
                        UserType.F_EMAIL_ADDRESS,
                        UserType.F_LINK_REF))
                .put(AbstractRoleType.class, Arrays.asList(
                        AbstractRoleType.F_NAME,
                        AbstractRoleType.F_DISPLAY_NAME,
                        AbstractRoleType.F_DESCRIPTION,
                        AbstractRoleType.F_IDENTIFIER,
                        AbstractRoleType.F_LINK_REF))
                .put(TaskType.class, Arrays.asList(
                        TaskType.F_NAME,
                        TaskType.F_CATEGORY,
                        TaskType.F_EXECUTION_STATUS,
                        TaskType.F_OBJECT_REF,
                        TaskType.F_NODE_AS_OBSERVED,
                        TaskType.F_COMPLETION_TIMESTAMP,
                        TaskType.F_PROGRESS,
                        TaskType.F_SCHEDULE,
                        ItemPath.create(TaskType.F_OPERATION_STATS,
                                OperationStatsType.F_ITERATIVE_TASK_INFORMATION,
                                IterativeTaskInformationType.F_TOTAL_FAILURE_COUNT),
                        TaskType.F_RESULT_STATUS))
                .put(ShadowType.class, Arrays.asList(
                        ShadowType.F_NAME,
                        ShadowType.F_RESOURCE_REF,
                        ShadowType.F_KIND,
                        ShadowType.F_INTENT,
                        ShadowType.F_SYNCHRONIZATION_SITUATION))
                .build();
    }

    private static List<ItemPath> getColumnsForType(Class<? extends ObjectType> type) {
        if (type.equals(RoleType.class)
                || type.equals(OrgType.class)
                || type.equals(ServiceType.class)) {
            return COLUMNS_DEF.get(AbstractRoleType.class);
        }
        if (COLUMNS_DEF.containsKey(type)) {
            return COLUMNS_DEF.get(type);
        }
        return OBJECT_COLUMNS_DEF;
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
        String previousColumn = null;
        for (ItemPath defaultColumn : DEFAULT_AUDIT_COLUMNS_DEF) {
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
        if (SPECIFIC_LOCALIZATION.containsKey(AuditEventRecordType.class)
                && SPECIFIC_LOCALIZATION.get(AuditEventRecordType.class).containsKey(itemPath)) {
            return SPECIFIC_LOCALIZATION.get(AuditEventRecordType.class).get(itemPath);
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

    private static <O extends ObjectType> GuiObjectListViewType getDefaultView(QName qname, String identifier, Class<? extends
            O> type) {
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
            if (NUMBER_COLUMNS.contains(defaultColumn)) {
                column.setDisplayValue(DisplayValueType.NUMBER);
            }
            if (SPECIFIC_LOCALIZATION.containsKey(type) && SPECIFIC_LOCALIZATION.get(type).containsKey(defaultColumn)) {
                DisplayType display = new DisplayType();
                display.setLabel(new PolyStringType(SPECIFIC_LOCALIZATION.get(type).get(defaultColumn)));
                column.setDisplay(display);
            }
            columns.add(column);
            previousColumn = columnName;
        }
    }

    public static  String processSpecialColumn(
            ItemPath itemPath, PrismContainer<? extends Containerable> object, LocalizationService localization) {
        @Nullable Class type = object.getCompileTimeClass();
        if (type == null || itemPath == null) {
            return null;
        }
        if (type.isAssignableFrom(TaskType.class)) {
            TaskType task = (TaskType) object.getRealValue();
            if (itemPath.equivalent(TaskType.F_COMPLETION_TIMESTAMP)) {
                XMLGregorianCalendar timestamp = task.getCompletionTimestamp();
                if (timestamp != null && task.getExecutionStatus().equals(TaskExecutionStatusType.CLOSED)) {
                    // Do we want default locale or default locale for FORMAT category?
                    // For latter no .withLocale() would be needed.
                    DateTimeFormatter formatter = DateTimeFormatter
                            .ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM)
                            .withLocale(Locale.getDefault());
                    ZonedDateTime time = timestamp.toGregorianCalendar().toZonedDateTime();
                    String dateTime = formatter.format(time);
                    String key = "pageTasks.task.closedAt";
                    return localization.translate(key, null, Locale.getDefault(), key) + " " + dateTime;
                }
                return "";
            } else if (itemPath.equivalent(TaskType.F_SCHEDULE)) {
                List<Object> localizationObject = new ArrayList<>();
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
                List<Object> localizationObject = new ArrayList<>();
                String key = TaskUtil.getProgressDescription(task, localizationObject);
                Object[] params = localizationObject.isEmpty() ? null : localizationObject.toArray();
                return localization.translate(key, params, Locale.getDefault(), key);
            }
        } else if (type.isAssignableFrom(AuditEventRecordType.class)) {
            for (AuditEventRecordCustomColumnPropertyType customColumn : ((AuditEventRecordType)object.getValue().asContainerable()).getCustomColumnProperty()) {
                if (customColumn.getName().equals(itemPath.toString())) {
                    return customColumn.getValue();
                }
            }
        }
        return null;
    }

    public static boolean isSpecialColumn(ItemPath itemPath, PrismContainer<? extends Containerable> object) {
        @Nullable Class type = object.getCompileTimeClass();
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
        } else if (type.isAssignableFrom(AuditEventRecordType.class)) {
            for (AuditEventRecordCustomColumnPropertyType customColumn : ((AuditEventRecordType)object.getValue().asContainerable()).getCustomColumnProperty()) {
                if (customColumn.getName().equals(itemPath.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Collection<SelectorOptions<GetOperationOptions>> createOption(
            Class<ObjectType> type, SchemaHelper schemaHelper) {
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
        }
//        } else if (ShadowType.class.isAssignableFrom(type)) {
//            getOperationOptionsBuilder = getOperationOptionsBuilder.raw();
//        }
        return getOperationOptionsBuilder
                .items(propertiesToGet.toArray(new Object[0])).retrieve()
                .build();
    }
}
