/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.util;

import static com.evolveum.midpoint.schema.util.task.ActivityProgressInformationBuilder.InformationSource.FULL_STATE_PREFERRED;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformation;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.schema.util.task.TaskTypeUtil;
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

    private static final ItemPath TASK_ERRORS = ItemPath.create("taskErrors");

    static {

        OBJECT_COLUMNS_DEF = Collections.singletonList(new ColumnWrapper(ObjectType.F_NAME));

        COLUMNS_DEF = ImmutableMap.<Class<? extends Containerable>, List<ColumnWrapper>>builder()
                .put(AuditEventRecordType.class, Arrays.asList(
                        new ColumnWrapper(AuditEventRecordType.F_TIMESTAMP, true),
                        new ColumnWrapper(AuditEventRecordType.F_INITIATOR_REF),
                        new ColumnWrapper(AuditEventRecordType.F_EVENT_STAGE, true),
                        new ColumnWrapper(AuditEventRecordType.F_EVENT_TYPE),
                        new ColumnWrapper(AuditEventRecordType.F_TARGET_REF),
                        new ColumnWrapper(AuditEventRecordType.F_TARGET_OWNER_REF),
                        new ColumnWrapper(AuditEventRecordType.F_CHANNEL, true),
                        new ColumnWrapper(AuditEventRecordType.F_OUTCOME, true)))
                .put(ResourceType.class, Arrays.asList(
                        new ColumnWrapper(ResourceType.F_NAME),
                        new ColumnWrapper(ItemPath.create(ResourceType.F_CONNECTOR_REF, ConnectorType.F_CONNECTOR_TYPE), "ConnectorType.connectorType"),
                        new ColumnWrapper(ItemPath.create(ResourceType.F_CONNECTOR_REF, ConnectorType.F_CONNECTOR_VERSION), "ConnectorType.connectorVersion")))
                .put(UserType.class, Arrays.asList(
                        new ColumnWrapper(UserType.F_NAME, true),
                        new ColumnWrapper(UserType.F_PERSONAL_NUMBER, true),
                        new ColumnWrapper(UserType.F_FULL_NAME, true),
                        new ColumnWrapper(UserType.F_EMAIL_ADDRESS),
                        new ColumnWrapper(UserType.F_LINK_REF, "FocusType.accounts", DisplayValueType.NUMBER)))
                .put(AbstractRoleType.class, Arrays.asList(
                        new ColumnWrapper(AbstractRoleType.F_NAME),
                        new ColumnWrapper(AbstractRoleType.F_DISPLAY_NAME, true),
                        new ColumnWrapper(AbstractRoleType.F_DESCRIPTION),
                        new ColumnWrapper(AbstractRoleType.F_IDENTIFIER, true),
                        new ColumnWrapper(AbstractRoleType.F_LINK_REF, "FocusType.linkRef", DisplayValueType.NUMBER)))
                .put(TaskType.class, Arrays.asList(
                        new ColumnWrapper(TaskType.F_NAME),
                        new ColumnWrapper(TaskType.F_EXECUTION_STATE),
                        new ColumnWrapper(TaskType.F_OBJECT_REF),
                        new ColumnWrapper(TaskType.F_NODE_AS_OBSERVED, "pageTasks.task.executingAt"),
                        new ColumnWrapper(TaskType.F_COMPLETION_TIMESTAMP, "TaskType.currentRunTime"),
                        new ColumnWrapper(TaskType.F_SCHEDULE, "pageTasks.task.scheduledToRunAgain"),
                        new ColumnWrapper(TaskType.F_PROGRESS),
                        new ColumnWrapper(TASK_ERRORS, "pageTasks.task.errors"),
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
                .put(MessageTemplateType.class, Arrays.asList(
                        new ColumnWrapper(MessageTemplateType.F_NAME),
                        new ColumnWrapper(MessageTemplateType.F_DESCRIPTION)))
                .put(MarkType.class, Arrays.asList(
                        new ColumnWrapper(MarkType.F_NAME),
                        new ColumnWrapper(ItemPath.create(MarkType.F_DISPLAY, DisplayType.F_LABEL)),
                        new ColumnWrapper(MarkType.F_DESCRIPTION),
                        new ColumnWrapper(ItemPath.create(MarkType.F_EVENT_MARK, EventMarkInformationType.F_DOMAIN)),
                        new ColumnWrapper(ItemPath.create(MarkType.F_EVENT_MARK, EventMarkInformationType.F_ENABLED_BY_DEFAULT))))
                .put(SimulationResultType.class, Arrays.asList(
                        new ColumnWrapper(SimulationResultType.F_NAME),
                        new ColumnWrapper(SimulationResultType.F_DESCRIPTION),
                        new ColumnWrapper(SimulationResultType.F_START_TIMESTAMP, true)))
                .put(SimulationResultProcessedObjectType.class, Arrays.asList(
                        new ColumnWrapper(SimulationResultProcessedObjectType.F_NAME, true),
                        new ColumnWrapper(SimulationResultProcessedObjectType.F_TYPE, true),
                        new ColumnWrapper(SimulationResultProcessedObjectType.F_STATE, true),
                        new ColumnWrapper(SimulationResultProcessedObjectType.F_DELTA)))
                .build();
    }

    private static List<ColumnWrapper> getColumnsForType(Class<? extends Containerable> type) {
        if (type.equals(RoleType.class)
                || type.equals(OrgType.class)
                || type.equals(ServiceType.class)
                || type.equals(PolicyType.class)
                || type.equals(ArchetypeType.class)) {
            return COLUMNS_DEF.get(AbstractRoleType.class);
        }
        if (COLUMNS_DEF.containsKey(type)) {
            return COLUMNS_DEF.get(type);
        }
        return OBJECT_COLUMNS_DEF;
    }

    public static GuiObjectListViewType getDefaultView(Class<?> type) {
        if (type == null) {
            return getDefaultObjectView();
        }

        // todo wtf is this???
        if (UserType.class.equals(type)) {
            return getDefaultUserView();
        } else if (RoleType.class.equals(type)) {
            return getDefaultRoleView();
        } else if (OrgType.class.equals(type)) {
            return getDefaultOrgView();
        } else if (ServiceType.class.equals(type)) {
            return getDefaultServiceView();
        } else if (PolicyType.class.equals(type)) {
            return getDefaultServiceView();
        } else if (ArchetypeType.class.equals(type)) {
            return getDefaultArchetypeView();
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
        } else if (MessageTemplateType.class.equals(type)) {
            return getDefaultMessageTemplateView();
        } else if (SimulationResultType.class.equals(type)) {
            return getSimulationResultView();
        } else if (SimulationResultProcessedObjectType.class.equals(type)) {
            return getDefaultSimulationResultProcessedObjectView();
        } else if (MarkType.class.equals(type)) {
            return getMarkView();
        } else if (ObjectType.class.isAssignableFrom(type)) {
            return getDefaultObjectView();
        }
        return null;
    }

    public static GuiObjectListViewType getMarkView() {
        return getDefaultView(MarkType.COMPLEX_TYPE, "default-mark", MarkType.class);
    }

    public static GuiObjectListViewType getSimulationResultView() {
        return getDefaultView(SimulationResultType.COMPLEX_TYPE, "default-simulation-result", SimulationResultType.class);
    }

    public static GuiObjectListViewType getDefaultSimulationResultProcessedObjectView() {
        return getDefaultView(SimulationResultProcessedObjectType.COMPLEX_TYPE, "default-simulation-result-processed-object", SimulationResultProcessedObjectType.class);
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

    public static GuiObjectListViewType getDefaultPolicyView() {
        return getDefaultView(PolicyType.COMPLEX_TYPE, "default-policy", PolicyType.class);
    }

    public static GuiObjectListViewType getDefaultArchetypeView() {
        return getDefaultView(ArchetypeType.COMPLEX_TYPE, "default-archetype", ArchetypeType.class);
    }

    public static GuiObjectListViewType getDefaultMessageTemplateView() {
        return getDefaultView(MessageTemplateType.COMPLEX_TYPE, "default-messageTemplate", MessageTemplateType.class);
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

    private static <C extends Containerable> GuiObjectListViewType getDefaultView(QName qname, String identifier, Class<? extends C> type) {
        GuiObjectListViewType view = new GuiObjectListViewType();
        view.setType(qname);
        view.setIdentifier(identifier);
        createColumns(view, type);
        return view;
    }

    private static <C extends Containerable> void createColumns(GuiObjectListViewType view, Class<? extends C> type) {
        view.createColumnList();

        List<GuiObjectColumnType> columns = view.getColumn();

        String previousColumn = null;
        for (ColumnWrapper columnWrapper : getColumnsForType(type)) {
            String localPathPath = columnWrapper.getPath().lastName().getLocalPart();
            String columnName = localPathPath + "Column";
            GuiObjectColumnType column = new GuiObjectColumnType();
            column.setName(columnName);
            column.setPreviousColumn(previousColumn);
            ItemPathType itemPathType = new ItemPathType();
            itemPathType.setItemPath(columnWrapper.getPath());
            column.setPath(itemPathType);
            column.setDisplayValue(columnWrapper.getDisplayValue());
            if (columnWrapper.isSortable()) {
                column.setSortProperty(localPathPath);
            }
            if (!StringUtils.isEmpty(columnWrapper.getLabel())) {
                DisplayType display = new DisplayType();
                PolyStringType label = new PolyStringType(columnWrapper.getLabel());
                label.setTranslation(new PolyStringTranslationType().key(columnWrapper.getLabel()));
                display.setLabel(label);
                column.setDisplay(display);
            }
            columns.add(column);
            previousColumn = columnName;
        }
    }

    public static String processSpecialColumn(
            ItemPath itemPath, Object object, LocalizationService localization) {
        if (itemPath == null) {
            return null;
        }
        if (object instanceof PrismValue) {
            object = ((PrismValue) object).getRealValue();
        }
        if (object instanceof TaskType) {
            TaskType task = (TaskType) object;
            if (itemPath.equivalent(TaskType.F_COMPLETION_TIMESTAMP)) {
                XMLGregorianCalendar timestamp = task.getCompletionTimestamp();
                if (timestamp != null && task.getExecutionState() == TaskExecutionStateType.CLOSED) {
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
                String key = TaskTypeUtil.createScheduledToRunAgain(task, localizationObject);
                Object[] params = localizationObject.isEmpty() ? null : localizationObject.toArray();
                return localization.translate(key, params, Locale.getDefault(), key);
            } else if (itemPath.equivalent(TaskType.F_PROGRESS)) {
                // TODO revise this; re-add "stalled since" information
                if (ActivityStateUtil.isProgressAvailableLocally(task)) {
                    ActivityProgressInformation progress = ActivityProgressInformation.fromRootTask(task, FULL_STATE_PREFERRED);
                    return progress.toHumanReadableString(false); // TODO create toLocalizedString method
                } else {
                    return "";
                }
            } else if (itemPath.equivalent(TASK_ERRORS)) {
                if (ActivityStateUtil.isProgressAvailableLocally(task)) {
                    ActivityProgressInformation progress = ActivityProgressInformation.fromRootTask(task, FULL_STATE_PREFERRED);
                    return String.valueOf(progress.getErrorsRecursive());
                } else {
                    return "0";
                }
            }
        } else if (object instanceof AuditEventRecordType) {
            for (AuditEventRecordCustomColumnPropertyType customColumn : ((AuditEventRecordType) object).getCustomColumnProperty()) {
                if (customColumn.getName().equals(itemPath.toString())) {
                    return customColumn.getValue();
                }
            }
        }
        return null;
    }

    public static boolean isSpecialColumn(ItemPath itemPath, Object value) {
        if (value == null || itemPath == null) {
            return false;
        }
        if (value instanceof TaskType) {
            return itemPath.equivalent(TaskType.F_COMPLETION_TIMESTAMP)
                    || itemPath.equivalent(TaskType.F_SCHEDULE)
                    || itemPath.equivalent(TaskType.F_PROGRESS)
                    || itemPath.equivalent(TASK_ERRORS);
        } else if (value instanceof AuditEventRecordType) {
            for (AuditEventRecordCustomColumnPropertyType customColumn : ((AuditEventRecordType) value).getCustomColumnProperty()) {
                if (customColumn.getName().equals(itemPath.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Collection<SelectorOptions<GetOperationOptions>> createOption(
            Class<?> type, SchemaService schemaService) {
        if (type == null) {
            return null;
        }
        List<QName> propertiesToGet = new ArrayList<>();
        GetOperationOptionsBuilder getOperationOptionsBuilder = schemaService.getOperationOptionsBuilder();
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

        private final ItemPath path;
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
