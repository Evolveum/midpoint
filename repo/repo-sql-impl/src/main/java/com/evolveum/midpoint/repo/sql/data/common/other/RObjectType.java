/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.other;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Enumeration of types know by the repository.
 */
public enum RObjectType {

    CONNECTOR(RConnector.class, ConnectorType.class),
    CONNECTOR_HOST(RConnectorHost.class, ConnectorHostType.class),
    GENERIC_OBJECT(RGenericObject.class, GenericObjectType.class),
    TAG(RTag.class, TagType.class),
    OBJECT(RObject.class, ObjectType.class),
    VALUE_POLICY(RValuePolicy.class, ValuePolicyType.class),
    RESOURCE(RResource.class, ResourceType.class),
    SHADOW(RShadow.class, ShadowType.class),
    ROLE(RRole.class, RoleType.class),
    SYSTEM_CONFIGURATION(RSystemConfiguration.class, SystemConfigurationType.class),
    TASK(RTask.class, TaskType.class),
    USER(RUser.class, UserType.class),
    REPORT(RReport.class, ReportType.class),
    REPORT_DATA(RReportData.class, ReportDataType.class),
    OBJECT_TEMPLATE(RObjectTemplate.class, ObjectTemplateType.class),
    NODE(RNode.class, NodeType.class),
    ORG(ROrg.class, OrgType.class),
    ABSTRACT_ROLE(RAbstractRole.class, AbstractRoleType.class),
    FOCUS(RFocus.class, FocusType.class),
    ASSIGNMENT_HOLDER(RObject.class, AssignmentHolderType.class),
    SECURITY_POLICY(RSecurityPolicy.class, SecurityPolicyType.class),
    LOOKUP_TABLE(RLookupTable.class, LookupTableType.class),
    ACCESS_CERTIFICATION_DEFINITION(RAccessCertificationDefinition.class, AccessCertificationDefinitionType.class),
    ACCESS_CERTIFICATION_CAMPAIGN(RAccessCertificationCampaign.class, AccessCertificationCampaignType.class),
    SEQUENCE(RSequence.class, SequenceType.class),
    SERVICE(RService.class, ServiceType.class),
    FORM(RForm.class, FormType.class),
    CASE(RCase.class, CaseType.class),
    FUNCTION_LIBRARY(RFunctionLibrary.class, FunctionLibraryType.class),
    OBJECT_COLLECTION(RObjectCollection.class, ObjectCollectionType.class),
    ARCHETYPE(RArchetype.class, ArchetypeType.class),
    MESSAGE_TEMPLATE(RMessageTemplate.class, MessageTemplateType.class),
    DASHBOARD(RDashboard.class, DashboardType.class);

    @NotNull private final Class<? extends RObject> clazz;
    @NotNull private final Class<? extends ObjectType> jaxbClass;

    RObjectType(@NotNull Class<? extends RObject> clazz, @NotNull Class<? extends ObjectType> jaxbClass) {
        this.clazz = clazz;
        this.jaxbClass = jaxbClass;
    }

    public @NotNull Class<? extends RObject> getClazz() {
        return clazz;
    }

    public @NotNull Class<? extends ObjectType> getJaxbClass() {
        return jaxbClass;
    }

    @NotNull
    public static RObjectType fromOrdinal(int ordinal) {
        return values()[ordinal];
    }

    @NotNull
    public static <T extends RObject> RObjectType getType(Class<T> clazz) {
        for (RObjectType type : RObjectType.values()) {
            if (type.getClazz().equals(clazz)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Couldn't find type for class '" + clazz + "'.");
    }

    @NotNull
    public static <T extends ObjectType> RObjectType getByJaxbType(Class<T> clazz) {
        RObjectType type = getByJaxbTypeIfExists(clazz);
        if (type != null) {
            return type;
        } else {
            throw new IllegalArgumentException("Couldn't find type for class '" + clazz + "'.");
        }
    }

    @Nullable
    public static <T extends ObjectType> RObjectType getByJaxbTypeIfExists(Class<T> clazz) {
        for (RObjectType type : RObjectType.values()) {
            if (type.getJaxbClass().equals(clazz)) {
                return type;
            }
        }
        return null;
    }
}
