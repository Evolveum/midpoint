/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.QCase;
import com.evolveum.midpoint.repo.sqale.qmodel.QDashboard;
import com.evolveum.midpoint.repo.sqale.qmodel.QObjectCollection;
import com.evolveum.midpoint.repo.sqale.qmodel.connector.QConnector;
import com.evolveum.midpoint.repo.sqale.qmodel.connector.QConnectorHost;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocus;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUser;
import com.evolveum.midpoint.repo.sqale.qmodel.lookuptable.QLookupTable;
import com.evolveum.midpoint.repo.sqale.qmodel.node.QNode;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping.QAssignmentHolder;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.report.QReport;
import com.evolveum.midpoint.repo.sqale.qmodel.report.QReportData;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QAbstractRole;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QArchetype;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QRole;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QService;
import com.evolveum.midpoint.repo.sqale.qmodel.system.QSecurityPolicy;
import com.evolveum.midpoint.repo.sqale.qmodel.system.QSystemConfiguration;
import com.evolveum.midpoint.repo.sqale.qmodel.system.QValuePolicy;
import com.evolveum.midpoint.repo.sqale.qmodel.task.QTask;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public enum MObjectTypeMapping {

    // mapping of codes and schema types must be unique, but one Q-class can serve multiple types
    CONNECTOR(0, QConnector.class, ConnectorType.class),
    CONNECTOR_HOST(1, QConnectorHost.class, ConnectorHostType.class),
    GENERIC_OBJECT(2, null, GenericObjectType.class),
    OBJECT(3, QObject.CLASS, ObjectType.class),
    VALUE_POLICY(5, QValuePolicy.class, ValuePolicyType.class),
    RESOURCE(6, null, ResourceType.class),
    SHADOW(7, null, ShadowType.class),
    ROLE(8, QRole.class, RoleType.class),
    SYSTEM_CONFIGURATION(9, QSystemConfiguration.class, SystemConfigurationType.class),
    TASK(10, QTask.class, TaskType.class),
    USER(11, QUser.class, UserType.class),
    REPORT(12, QReport.class, ReportType.class),
    REPORT_DATA(13, QReportData.class, ReportDataType.class),
    OBJECT_TEMPLATE(14, null, ObjectTemplateType.class),
    NODE(15, QNode.class, NodeType.class),
    ORG(16, null, OrgType.class),
    ABSTRACT_ROLE(17, QAbstractRole.CLASS, AbstractRoleType.class),
    FOCUS(18, QFocus.CLASS, FocusType.class),
    ASSIGNMENT_HOLDER(19, QAssignmentHolder.class, AssignmentHolderType.class),
    SECURITY_POLICY(20, QSecurityPolicy.class, SecurityPolicyType.class),
    LOOKUP_TABLE(21, QLookupTable.class, LookupTableType.class),
    ACCESS_CERTIFICATION_DEFINITION(22, null, AccessCertificationDefinitionType.class),
    ACCESS_CERTIFICATION_CAMPAIGN(23, null, AccessCertificationCampaignType.class),
    SEQUENCE(24, null, SequenceType.class),
    SERVICE(25, QService.class, ServiceType.class),
    FORM(26, null, FormType.class),
    CASE(27, QCase.class, CaseType.class),
    FUNCTION_LIBRARY(28, null, FunctionLibraryType.class),
    OBJECT_COLLECTION(29, QObjectCollection.class, ObjectCollectionType.class),
    ARCHETYPE(30, QArchetype.class, ArchetypeType.class),
    DASHBOARD(31, QDashboard.class, DashboardType.class);

    private final int code;
    private final Class<? extends QObject<?>> queryType;
    private final Class<? extends ObjectType> schemaType;

    MObjectTypeMapping(
            int code,
            Class<? extends QObject<?>> queryType,
            Class<? extends ObjectType> schemaType) {
        this.code = code;
        this.queryType = queryType;
        this.schemaType = schemaType;
    }

    // DB code -> enum conversion
    public static final Map<Integer, MObjectTypeMapping> CODE_TO_ENUM = new HashMap<>();

    // schema type QName -> enum conversion
    public static final Map<Class<? extends ObjectType>, MObjectTypeMapping> SCHEMA_TYPE_TO_ENUM =
            new HashMap<>();

    static {
        for (MObjectTypeMapping value : values()) {
            if (CODE_TO_ENUM.put(value.code, value) != null) {
                throw new IllegalArgumentException("MObjectTypeMapping value " + value
                        + " uses duplicate code: " + value.code);
            }
            if (SCHEMA_TYPE_TO_ENUM.put(value.schemaType, value) != null) {
                throw new IllegalArgumentException("MObjectTypeMapping value " + value
                        + " uses duplicate schema type: " + value.schemaType);
            }
        }
    }

    @NotNull
    public static MObjectTypeMapping fromCode(int code) {
        return Objects.requireNonNull(CODE_TO_ENUM.get(code),
                "No MObjectTypeMapping found for object type code " + code);
    }

    @NotNull
    public static MObjectTypeMapping fromTypeQName(QName typeQName) {
        return fromSchemaType(ObjectTypes.getObjectTypeClass(typeQName));
    }

    @NotNull
    public static MObjectTypeMapping fromSchemaType(Class<? extends ObjectType> objectTypeClass) {
        return Objects.requireNonNull(SCHEMA_TYPE_TO_ENUM.get(objectTypeClass),
                "No MObjectTypeMapping found for object type " + objectTypeClass);
    }

    public int code() {
        return code;
    }

    public Class<? extends QObject<?>> getQueryType() {
        return queryType;
    }

    public Class<? extends ObjectType> getSchemaType() {
        return schemaType;
    }
}
