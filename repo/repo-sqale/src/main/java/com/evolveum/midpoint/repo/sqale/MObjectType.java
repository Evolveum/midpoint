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

import com.evolveum.midpoint.repo.sqale.qmodel.cases.QCase;
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

public enum MObjectType {

    // mapping of codes and schema types must be unique
    ABSTRACT_ROLE(16, QAbstractRole.CLASS, AbstractRoleType.class),
    ACCESS_CERTIFICATION_CAMPAIGN(22, null, AccessCertificationCampaignType.class),
    ACCESS_CERTIFICATION_DEFINITION(21, null, AccessCertificationDefinitionType.class),
    ARCHETYPE(29, QArchetype.class, ArchetypeType.class),
    ASSIGNMENT_HOLDER(18, QAssignmentHolder.class, AssignmentHolderType.class),
    CASE(26, QCase.class, CaseType.class),
    CONNECTOR(0, QConnector.class, ConnectorType.class),
    CONNECTOR_HOST(1, QConnectorHost.class, ConnectorHostType.class),
    DASHBOARD(30, QDashboard.class, DashboardType.class),
    FOCUS(17, QFocus.CLASS, FocusType.class),
    FORM(25, null, FormType.class),
    FUNCTION_LIBRARY(27, null, FunctionLibraryType.class),
    GENERIC_OBJECT(2, null, GenericObjectType.class),
    LOOKUP_TABLE(20, QLookupTable.class, LookupTableType.class),
    NODE(14, QNode.class, NodeType.class),
    OBJECT(3, QObject.CLASS, ObjectType.class),
    OBJECT_COLLECTION(28, QObjectCollection.class, ObjectCollectionType.class),
    OBJECT_TEMPLATE(13, null, ObjectTemplateType.class),
    ORG(15, null, OrgType.class),
    REPORT(11, QReport.class, ReportType.class),
    REPORT_DATA(12, QReportData.class, ReportDataType.class),
    RESOURCE(5, null, ResourceType.class),
    ROLE(7, QRole.class, RoleType.class),
    SECURITY_POLICY(19, QSecurityPolicy.class, SecurityPolicyType.class),
    SEQUENCE(23, null, SequenceType.class),
    SERVICE(24, QService.class, ServiceType.class),
    SHADOW(6, null, ShadowType.class),
    SYSTEM_CONFIGURATION(8, QSystemConfiguration.class, SystemConfigurationType.class),
    TASK(9, QTask.class, TaskType.class),
    USER(10, QUser.class, UserType.class),
    VALUE_POLICY(4, QValuePolicy.class, ValuePolicyType.class);

    // TODO drop the codes, pg enum type will cover it
    private final int code;
    private final Class<? extends QObject<?>> queryType;
    private final Class<? extends ObjectType> schemaType;

    MObjectType(
            int code,
            Class<? extends QObject<?>> queryType,
            Class<? extends ObjectType> schemaType) {
        this.code = code;
        this.queryType = queryType;
        this.schemaType = schemaType;
    }

    // DB code -> enum conversion
    public static final Map<Integer, MObjectType> CODE_TO_ENUM = new HashMap<>();

    // schema type QName -> enum conversion
    public static final Map<Class<? extends ObjectType>, MObjectType> SCHEMA_TYPE_TO_ENUM =
            new HashMap<>();

    static {
        for (MObjectType value : values()) {
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
    public static MObjectType fromCode(int code) {
        return Objects.requireNonNull(CODE_TO_ENUM.get(code),
                "No MObjectTypeMapping found for object type code " + code);
    }

    @NotNull
    public static MObjectType fromTypeQName(QName typeQName) {
        return fromSchemaType(ObjectTypes.getObjectTypeClass(typeQName));
    }

    @NotNull
    public static MObjectType fromSchemaType(Class<? extends ObjectType> objectTypeClass) {
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
