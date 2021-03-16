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

import com.evolveum.midpoint.repo.sqale.qmodel.QDashboard;
import com.evolveum.midpoint.repo.sqale.qmodel.QObjectCollection;
import com.evolveum.midpoint.repo.sqale.qmodel.cases.QCase;
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
    ABSTRACT_ROLE(QAbstractRole.CLASS, AbstractRoleType.class),
    ACCESS_CERTIFICATION_CAMPAIGN(null, AccessCertificationCampaignType.class),
    ACCESS_CERTIFICATION_DEFINITION(null, AccessCertificationDefinitionType.class),
    ARCHETYPE(QArchetype.class, ArchetypeType.class),
    ASSIGNMENT_HOLDER(QAssignmentHolder.class, AssignmentHolderType.class),
    CASE(QCase.class, CaseType.class),
    CONNECTOR(QConnector.class, ConnectorType.class),
    CONNECTOR_HOST(QConnectorHost.class, ConnectorHostType.class),
    DASHBOARD(QDashboard.class, DashboardType.class),
    FOCUS(QFocus.CLASS, FocusType.class),
    FORM(null, FormType.class),
    FUNCTION_LIBRARY(null, FunctionLibraryType.class),
    GENERIC_OBJECT(null, GenericObjectType.class),
    LOOKUP_TABLE(QLookupTable.class, LookupTableType.class),
    NODE(QNode.class, NodeType.class),
    OBJECT(QObject.CLASS, ObjectType.class),
    OBJECT_COLLECTION(QObjectCollection.class, ObjectCollectionType.class),
    OBJECT_TEMPLATE(null, ObjectTemplateType.class),
    ORG(null, OrgType.class),
    REPORT(QReport.class, ReportType.class),
    REPORT_DATA(QReportData.class, ReportDataType.class),
    RESOURCE(null, ResourceType.class),
    ROLE(QRole.class, RoleType.class),
    SECURITY_POLICY(QSecurityPolicy.class, SecurityPolicyType.class),
    SEQUENCE(null, SequenceType.class),
    SERVICE(QService.class, ServiceType.class),
    SHADOW(null, ShadowType.class),
    SYSTEM_CONFIGURATION(QSystemConfiguration.class, SystemConfigurationType.class),
    TASK(QTask.class, TaskType.class),
    USER(QUser.class, UserType.class),
    VALUE_POLICY(QValuePolicy.class, ValuePolicyType.class);

    private final Class<? extends QObject<?>> queryType;
    private final Class<? extends ObjectType> schemaType;

    MObjectType(Class<? extends QObject<?>> queryType, Class<? extends ObjectType> schemaType) {
        this.queryType = queryType;
        this.schemaType = schemaType;
    }

    // schema type QName -> enum conversion
    public static final Map<Class<? extends ObjectType>, MObjectType> SCHEMA_TYPE_TO_ENUM =
            new HashMap<>();

    static {
        for (MObjectType value : values()) {
            if (SCHEMA_TYPE_TO_ENUM.put(value.schemaType, value) != null) {
                throw new IllegalArgumentException("MObjectTypeMapping value " + value
                        + " uses duplicate schema type: " + value.schemaType);
            }
        }
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

    public Class<? extends QObject<?>> getQueryType() {
        return queryType;
    }

    public Class<? extends ObjectType> getSchemaType() {
        return schemaType;
    }
}
