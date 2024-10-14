/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.sqale.qmodel.mining.cluster.QClusterObject;
import com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier.QOutlier;
import com.evolveum.midpoint.repo.sqale.qmodel.mining.session.QSessionData;
import com.evolveum.midpoint.repo.sqale.qmodel.role.*;

import com.evolveum.midpoint.repo.sqale.schema.QSchema;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.accesscert.QAccessCertificationCampaign;
import com.evolveum.midpoint.repo.sqale.qmodel.accesscert.QAccessCertificationDefinition;
import com.evolveum.midpoint.repo.sqale.qmodel.cases.QCase;
import com.evolveum.midpoint.repo.sqale.qmodel.connector.QConnector;
import com.evolveum.midpoint.repo.sqale.qmodel.connector.QConnectorHost;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocus;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QGenericObject;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUser;
import com.evolveum.midpoint.repo.sqale.qmodel.lookuptable.QLookupTable;
import com.evolveum.midpoint.repo.sqale.qmodel.node.QNode;
import com.evolveum.midpoint.repo.sqale.qmodel.notification.QMessageTemplate;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrg;
import com.evolveum.midpoint.repo.sqale.qmodel.other.*;
import com.evolveum.midpoint.repo.sqale.qmodel.report.QReport;
import com.evolveum.midpoint.repo.sqale.qmodel.report.QReportData;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.QResource;
import com.evolveum.midpoint.repo.sqale.qmodel.shadow.QShadow;
import com.evolveum.midpoint.repo.sqale.qmodel.system.QSecurityPolicy;
import com.evolveum.midpoint.repo.sqale.qmodel.system.QSystemConfiguration;
import com.evolveum.midpoint.repo.sqale.qmodel.system.QValuePolicy;
import com.evolveum.midpoint.repo.sqale.qmodel.tag.QMark;
import com.evolveum.midpoint.repo.sqale.qmodel.task.QTask;
import com.evolveum.midpoint.repo.sqale.qmodel.simulation.QSimulationResult;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Definition enum, counterpart for our custom Postgres type `ObjectType`.
 * The enum values are rarely used directly, except for some abstract types.
 * They are however used for the mapping between Q-classes and schema types using the static methods
 * provided on this enum class.
 */
public enum MObjectType {

    // mapping of codes and schema types must be unique
    ABSTRACT_ROLE(QAbstractRole.CLASS, AbstractRoleType.class),
    ACCESS_CERTIFICATION_CAMPAIGN(
            QAccessCertificationCampaign.class, AccessCertificationCampaignType.class),
    ACCESS_CERTIFICATION_DEFINITION(
            QAccessCertificationDefinition.class, AccessCertificationDefinitionType.class),
    ARCHETYPE(QArchetype.class, ArchetypeType.class),
    ASSIGNMENT_HOLDER(QAssignmentHolder.CLASS, AssignmentHolderType.class),
    CASE(QCase.class, CaseType.class),
    CONNECTOR(QConnector.class, ConnectorType.class),
    CONNECTOR_HOST(QConnectorHost.class, ConnectorHostType.class),
    DASHBOARD(QDashboard.class, DashboardType.class),
    FOCUS(QFocus.CLASS, FocusType.class),
    FORM(QForm.class, FormType.class),
    FUNCTION_LIBRARY(QFunctionLibrary.class, FunctionLibraryType.class),
    GENERIC_OBJECT(QGenericObject.class, GenericObjectType.class),
    LOOKUP_TABLE(QLookupTable.class, LookupTableType.class),
    MESSAGE_TEMPLATE(QMessageTemplate.class, MessageTemplateType.class),
    NODE(QNode.class, NodeType.class),
    OBJECT(QObject.CLASS, ObjectType.class),
    OBJECT_COLLECTION(QObjectCollection.class, ObjectCollectionType.class),
    OBJECT_TEMPLATE(QObjectTemplate.class, ObjectTemplateType.class),
    ORG(QOrg.class, OrgType.class),
    REPORT(QReport.class, ReportType.class),
    REPORT_DATA(QReportData.class, ReportDataType.class),
    RESOURCE(QResource.class, ResourceType.class),
    ROLE(QRole.class, RoleType.class),
    ROLE_ANALYSIS_CLUSTER(QClusterObject.class, RoleAnalysisClusterType.class),
    ROLE_ANALYSIS_SESSION(QSessionData.class, RoleAnalysisSessionType.class),
    ROLE_ANALYSIS_OUTLIER(QOutlier.class, RoleAnalysisOutlierType.class),
    SCHEMA(QSchema.class, SchemaType.class),
    SECURITY_POLICY(QSecurityPolicy.class, SecurityPolicyType.class),
    SEQUENCE(QSequence.class, SequenceType.class),
    SERVICE(QService.class, ServiceType.class),
    SHADOW(QShadow.class, ShadowType.class),
    SIMULATION_RESULT(QSimulationResult.class, SimulationResultType.class),
    SYSTEM_CONFIGURATION(QSystemConfiguration.class, SystemConfigurationType.class),
    MARK(QMark.class, MarkType.class),
    TASK(QTask.class, TaskType.class),
    USER(QUser.class, UserType.class),
    VALUE_POLICY(QValuePolicy.class, ValuePolicyType.class),
    POLICY(QPolicy.class, PolicyType.class);

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

    public QName getTypeName() {
        return ObjectTypes.getObjectType(schemaType).getTypeQName();
    }
}
