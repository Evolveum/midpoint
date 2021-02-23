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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.task.api.TaskUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordCustomColumnPropertyType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Column related utilities shared by reporting and GUI.
 */
public class DefaultColumnUtils {

    // Maps need to preserve iteration order (like LinkedHashMap)
    private static final Map<Class<? extends Containerable>, List<ColumnWrapper>> COLUMNS_DEF;
    private static final List<ColumnWrapper> OBJECT_COLUMNS_DEF;

    static {

        OBJECT_COLUMNS_DEF = Collections.singletonList(new ColumnWrapper(ObjectType.F_NAME));

        COLUMNS_DEF = ImmutableMap.<Class<? extends Containerable>, List<ColumnWrapper>>builder()
                .put(AuditEventRecordType.class, Arrays.asList(
                        new ColumnWrapper(AuditEventRecordType.F_TIMESTAMP, true),
                        new ColumnWrapper(AuditEventRecordType.F_INITIATOR_REF),
                        new ColumnWrapper(AuditEventRecordType.F_EVENT_STAGE),
                        new ColumnWrapper(AuditEventRecordType.F_EVENT_TYPE),
                        new ColumnWrapper(AuditEventRecordType.F_TARGET_REF),
                        new ColumnWrapper(AuditEventRecordType.F_OUTCOME),
                        new ColumnWrapper(AuditEventRecordType.F_MESSAGE),
                        new ColumnWrapper(AuditEventRecordType.F_DELTA)))
                .put(ResourceType.class, Arrays.asList(
                        new ColumnWrapper(ResourceType.F_NAME),
                        new ColumnWrapper(ItemPath.create(ResourceType.F_CONNECTOR_REF, ConnectorType.F_CONNECTOR_TYPE), "ConnectorType.connectorType"),
                        new ColumnWrapper(ItemPath.create(ResourceType.F_CONNECTOR_REF, ConnectorType.F_CONNECTOR_VERSION), "ConnectorType.connectorVersion")))
                .put(UserType.class, Arrays.asList(
                        new ColumnWrapper(UserType.F_NAME, true),
                        new ColumnWrapper(UserType.F_GIVEN_NAME, true),
                        new ColumnWrapper(UserType.F_FAMILY_NAME, true),
                        new ColumnWrapper(UserType.F_FULL_NAME, true),
                        new ColumnWrapper(UserType.F_EMAIL_ADDRESS),
                        new ColumnWrapper(UserType.F_LINK_REF, "FocusType.accounts", DisplayValueType.NUMBER)))
                .put(AbstractRoleType.class, Arrays.asList(
                        new ColumnWrapper(AbstractRoleType.F_NAME),
                        new ColumnWrapper(AbstractRoleType.F_DISPLAY_NAME, true),
                        new ColumnWrapper(AbstractRoleType.F_DESCRIPTION),
                        new ColumnWrapper(AbstractRoleType.F_IDENTIFIER, true),
                        new ColumnWrapper(AbstractRoleType.F_LINK_REF)))
                .put(TaskType.class, Arrays.asList(
                        new ColumnWrapper(TaskType.F_NAME),
                        new ColumnWrapper(TaskType.F_CATEGORY),
                        new ColumnWrapper(TaskType.F_EXECUTION_STATUS),
                        new ColumnWrapper(TaskType.F_OBJECT_REF),
                        new ColumnWrapper(TaskType.F_NODE_AS_OBSERVED, "pageTasks.task.executingAt"),
                        new ColumnWrapper(TaskType.F_COMPLETION_TIMESTAMP, "TaskType.currentRunTime"),
                        new ColumnWrapper(TaskType.F_PROGRESS),
                        new ColumnWrapper(TaskType.F_SCHEDULE, "pageTasks.task.scheduledToRunAgain"),
                        new ColumnWrapper(ItemPath.create(TaskType.F_OPERATION_STATS,
                                OperationStatsType.F_ITERATIVE_TASK_INFORMATION,
                                IterativeTaskInformationType.F_TOTAL_FAILURE_COUNT), "pageTasks.task.errors"),
                        new ColumnWrapper(TaskType.F_RESULT_STATUS)))
                .put(ShadowType.class, Arrays.asList(
                        new ColumnWrapper(ShadowType.F_NAME),
                        new ColumnWrapper(ShadowType.F_RESOURCE_REF),
                        new ColumnWrapper(ShadowType.F_KIND),
                        new ColumnWrapper(ShadowType.F_INTENT),
                        new ColumnWrapper(ShadowType.F_SYNCHRONIZATION_SITUATION)))
                .put(AccessCertificationDefinitionType.class, Arrays.asList(
                        new ColumnWrapper(AccessCertificationDefinitionType.F_NAME),
                        new ColumnWrapper(AccessCertificationDefinitionType.F_DESCRIPTION)))
                .build();
    }

    private static List<ColumnWrapper> getColumnsForType(Class<? extends Containerable> type) {
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

    public static <C extends Containerable> GuiObjectListViewType getDefaultView(Class<? extends C> type) {
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
        } else if (AccessCertificationDefinitionType.class.equals(type)) {
            return getDefaultAccessCertificationDefinitionView();
        } else if (AuditEventRecordType.class.equals(type)) {
            return getDefaultAuditEventsView();
        } else if (ObjectType.class.isAssignableFrom(type)){
            return getDefaultObjectView();
        }
        return null;
    }

    public static GuiObjectListViewType getDefaultAuditEventsView() {
        return getDefaultView(AuditEventRecordType.COMPLEX_TYPE, "default-audit-event", AuditEventRecordType.class);
    }

    private static GuiObjectListViewType getDefaultAccessCertificationDefinitionView() {
        return getDefaultView(AccessCertificationDefinitionType.COMPLEX_TYPE, "default-accessCertificationDefinition", AccessCertificationDefinitionType.class);
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

    private static <C extends Containerable> GuiObjectListViewType getDefaultView(QName qname, String identifier, Class<? extends
            C> type) {
        GuiObjectListViewType view = new GuiObjectListViewType();
        view.setType(qname);
        view.setIdentifier(identifier);
        createColumns(view, type);
        return view;
    }

    private static <C extends Containerable> void createColumns(GuiObjectListViewType view, Class<? extends C> type) {
        view.createColumnList();
        List<GuiObjectColumnType> columns = view.getColumn();
        List<ColumnWrapper> defaultColumns = getColumnsForType(type);
        String previousColumn = null;
        for (ColumnWrapper defaultColumn : defaultColumns) {
            String localPathPath = defaultColumn.getPath().lastName().getLocalPart();
            String columnName = localPathPath + "Column";
            GuiObjectColumnType column = new GuiObjectColumnType();
            column.setName(columnName);
            column.setPreviousColumn(previousColumn);
            ItemPathType itemPathType = new ItemPathType();
            itemPathType.setItemPath(defaultColumn.getPath());
            column.setPath(itemPathType);
            column.setDisplayValue(defaultColumn.getDisplayValue());
            if (defaultColumn.isSortable()) {
                column.setSortProperty(localPathPath);
            }
            if (!StringUtils.isEmpty(defaultColumn.getLabel())) {
                DisplayType display = new DisplayType();
                PolyStringType label = new PolyStringType(defaultColumn.getLabel());
                label.setTranslation(new PolyStringTranslationType().key(defaultColumn.getLabel()));
                display.setLabel(label);
                column.setDisplay(display);
            }
            columns.add(column);
            previousColumn = columnName;
        }
    }

    public static  String processSpecialColumn(
            ItemPath itemPath, PrismContainer<? extends Containerable> object, LocalizationService localization) {
        @Nullable Class<? extends Containerable> type = object.getCompileTimeClass();
        if (type == null || itemPath == null) {
            return null;
        }
        if (type.isAssignableFrom(TaskType.class)) {
            TaskType task = (TaskType) object.getRealValue();
            if (itemPath.equivalent(TaskType.F_COMPLETION_TIMESTAMP)) {
                XMLGregorianCalendar timestamp = task.getCompletionTimestamp();
                if (timestamp != null && task.getExecutionStatus() == TaskExecutionStateType.CLOSED) {
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
        @Nullable Class<? extends Containerable> type = object.getCompileTimeClass();
        if (type == null || itemPath == null) {
            return false;
        }
        if (type.isAssignableFrom(TaskType.class)) {
            return itemPath.equivalent(TaskType.F_COMPLETION_TIMESTAMP)
                    || itemPath.equivalent(TaskType.F_SCHEDULE)
                    || itemPath.equivalent(ItemPath.create(TaskType.F_OPERATION_STATS, OperationStatsType.F_ITERATIVE_TASK_INFORMATION,
                    IterativeTaskInformationType.F_TOTAL_FAILURE_COUNT))
                    || itemPath.equivalent(TaskType.F_PROGRESS);
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

    private static class ColumnWrapper {

        private ItemPath path;
        private String label = null;
        private boolean isSortable = false;
        private DisplayValueType displayValue = DisplayValueType.STRING;

        ColumnWrapper(@NotNull ItemPath path) {
            this.path = path;
        }

        ColumnWrapper(@NotNull ItemPath path, String label) {
            this.path = path;
            this.label = label;
        }

        ColumnWrapper(@NotNull ItemPath path, boolean isSortable) {
            this.path = path;
            this.isSortable = isSortable;
        }

        ColumnWrapper(@NotNull ItemPath path, String label, DisplayValueType displayValue) {
            this.path = path;
            this.label = label;
            this.displayValue = displayValue;
        }

        public ItemPath getPath() {
            return path;
        }

        public String getLabel() {
            return label;
        }

        public boolean isSortable() {
            return isSortable;
        }

        public DisplayValueType getDisplayValue() {
            return displayValue;
        }
    }
}
