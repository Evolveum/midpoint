/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.AuditConstants;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author skublik
 */

public class DefaultColumnUtils {

    private static LinkedHashMap<Class<? extends ObjectType>, List<ItemPath>> columnsDef;
    private static List<ItemPath> objectColumnsDef;
    private static List<ItemPath> auditColumnsDef;
    private static List<ItemPath> numberColumns;

    static {
        numberColumns = new ArrayList<ItemPath>(Arrays.asList(
                FocusType.F_LINK_REF
        ));

        objectColumnsDef = new ArrayList<ItemPath>(Arrays.asList(
                ObjectType.F_NAME
        ));

        auditColumnsDef = new ArrayList<ItemPath>(Arrays.asList(
                ItemPath.create(AuditConstants.TIME_COLUMN),
                ItemPath.create(AuditConstants.INITIATOR_COLUMN),
                ItemPath.create(AuditConstants.EVENT_STAGE_COLUMN),
                ItemPath.create(AuditConstants.EVENT_TYPE_COLUMN),
                ItemPath.create(AuditConstants.TARGET_COLUMN),
                ItemPath.create(AuditConstants.OUTCOME_COLUMN),
                ItemPath.create(AuditConstants.MESSAGE_COLUMN),
                ItemPath.create(AuditConstants.DELTA_COLUMN)
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

    public static List<ItemPath> getAuditColumnsDef() {
        return auditColumnsDef;
    }

    public static <O extends ObjectType> GuiObjectListViewType getDefaultColumns(Class<? extends O> type) {
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
        List<ItemPath> defaultColumns = getAuditColumnsDef();
        String previousColumn = null;
        for (ItemPath defaultColumn : defaultColumns) {
            String columnName = defaultColumn.lastName().getLocalPart() + "Column";
            GuiObjectColumnType column = new GuiObjectColumnType();
            column.setName(columnName);
            column.setPreviousColumn(previousColumn);
            ItemPathType itemPathType = new ItemPathType();
            itemPathType.setItemPath(defaultColumn);
            column.setPath(itemPathType);
            columns.add(column);
        }
        return view;
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
            columns.add(column);
            previousColumn = columnName;
        }
    }


}
