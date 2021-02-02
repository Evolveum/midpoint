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

import com.evolveum.midpoint.repo.sqale.qmodel.node.QNode;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public enum MObjectTypeMapping {

    CONNECTOR(0, null, ConnectorType.class),
    CONNECTOR_HOST(1, null, ConnectorHostType.class),
    GENERIC_OBJECT(2, null, GenericObjectType.class),
    OBJECT(3, QObject.CLASS, ObjectType.class),
    VALUE_POLICY(5, null, ValuePolicyType.class),
    RESOURCE(6, null, ResourceType.class),
    SHADOW(7, null, ShadowType.class),
    ROLE(8, null, RoleType.class),
    SYSTEM_CONFIGURATION(9, null, SystemConfigurationType.class),
    TASK(10, null, TaskType.class),
    USER(11, null, UserType.class),
    REPORT(12, null, ReportType.class),
    REPORT_DATA(13, null, ReportDataType.class),
    OBJECT_TEMPLATE(14, null, ObjectTemplateType.class),
    NODE(15, QNode.class, NodeType.class),
    ORG(16, null, OrgType.class),
    ABSTRACT_ROLE(17, null, AbstractRoleType.class),
    FOCUS(18, null, FocusType.class),
    ASSIGNMENT_HOLDER(19, null, AssignmentHolderType.class),
    SECURITY_POLICY(20, null, SecurityPolicyType.class),
    LOOKUP_TABLE(21, null, LookupTableType.class),
    ACCESS_CERTIFICATION_DEFINITION(22, null, AccessCertificationDefinitionType.class),
    ACCESS_CERTIFICATION_CAMPAIGN(23, null, AccessCertificationCampaignType.class),
    SEQUENCE(24, null, SequenceType.class),
    SERVICE(25, null, ServiceType.class),
    FORM(26, null, FormType.class),
    CASE(27, null, CaseType.class),
    FUNCTION_LIBRARY(28, null, FunctionLibraryType.class),
    OBJECT_COLLECTION(29, null, ObjectCollectionType.class),
    ARCHETYPE(30, null, ArchetypeType.class),
    DASHBOARD(31, null, DashboardType.class);

    private final int code;
    private final Class<? extends QObject<?>> queryType;
    private final Class<? extends ObjectType> schemaType;

    // 'MObjectTypeMapping(int,
    // java.lang.Class<com.evolveum.midpoint.repo.sqale.qmodel.object.QObject<?>>,
    // java.lang.Class<com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType>)' in 'com.evolveum.midpoint.repo.sqale.MObjectTypeMapping'
    // cannot be applied to '(int, null,
    // java.lang.Class<com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType>)'
    MObjectTypeMapping(int code, Class<? extends QObject<?>> queryType, Class<? extends ObjectType> schemaType) {
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
            CODE_TO_ENUM.put(value.code, value);
            SCHEMA_TYPE_TO_ENUM.put(value.schemaType, value);
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
