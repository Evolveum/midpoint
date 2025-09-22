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
import java.util.function.Supplier;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.sqale.qmodel.connector.QConnectorDevelopment;
import com.evolveum.midpoint.repo.sqale.qmodel.mining.cluster.QClusterObject;
import com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier.QOutlier;
import com.evolveum.midpoint.repo.sqale.qmodel.mining.session.QSessionData;
import com.evolveum.midpoint.repo.sqale.qmodel.role.*;

import com.evolveum.midpoint.repo.sqale.qmodel.schema.QSchema;

import com.google.common.base.Preconditions;
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
    ABSTRACT_ROLE(QAbstractRole.CLASS, AbstractRoleType.class, null),
    ACCESS_CERTIFICATION_CAMPAIGN(QAccessCertificationCampaign.class, AccessCertificationCampaignType.class, AccessCertificationCampaignType::new),
    ACCESS_CERTIFICATION_DEFINITION(QAccessCertificationDefinition.class, AccessCertificationDefinitionType.class, AccessCertificationDefinitionType::new),
    APPLICATION(QApplication.class, ApplicationType.class, ApplicationType::new),
    ARCHETYPE(QArchetype.class, ArchetypeType.class, ArchetypeType::new),
    ASSIGNMENT_HOLDER(QAssignmentHolder.CLASS, AssignmentHolderType.class, null),
    CASE(QCase.class, CaseType.class, CaseType::new),
    CONNECTOR(QConnector.class, ConnectorType.class, ConnectorType::new),
    CONNECTOR_HOST(QConnectorHost.class, ConnectorHostType.class, ConnectorHostType::new),
    CONNECTOR_DEVELOPMENT(QConnectorDevelopment.class, ConnectorDevelopmentType.class,ConnectorDevelopmentType::new),
    DASHBOARD(QDashboard.class, DashboardType.class, DashboardType::new),
    FOCUS(QFocus.CLASS, FocusType.class, null),
    FORM(QForm.class, FormType.class, FormType::new),
    FUNCTION_LIBRARY(QFunctionLibrary.class, FunctionLibraryType.class, FunctionLibraryType::new),
    GENERIC_OBJECT(QGenericObject.class, GenericObjectType.class, GenericObjectType::new),
    LOOKUP_TABLE(QLookupTable.class, LookupTableType.class, LookupTableType::new),
    MESSAGE_TEMPLATE(QMessageTemplate.class, MessageTemplateType.class, MessageTemplateType::new),
    NODE(QNode.class, NodeType.class, NodeType::new),
    OBJECT(QObject.CLASS, ObjectType.class, null),
    OBJECT_COLLECTION(QObjectCollection.class, ObjectCollectionType.class, ObjectCollectionType::new),
    OBJECT_TEMPLATE(QObjectTemplate.class, ObjectTemplateType.class, ObjectTemplateType::new),
    ORG(QOrg.class, OrgType.class, OrgType::new),
    REPORT(QReport.class, ReportType.class, ReportType::new),
    REPORT_DATA(QReportData.class, ReportDataType.class, ReportDataType::new),
    RESOURCE(QResource.class, ResourceType.class, ResourceType::new),
    ROLE(QRole.class, RoleType.class, RoleType::new),
    ROLE_ANALYSIS_CLUSTER(QClusterObject.class, RoleAnalysisClusterType.class, RoleAnalysisClusterType::new),
    ROLE_ANALYSIS_SESSION(QSessionData.class, RoleAnalysisSessionType.class, RoleAnalysisSessionType::new),
    ROLE_ANALYSIS_OUTLIER(QOutlier.class, RoleAnalysisOutlierType.class, RoleAnalysisOutlierType::new),
    SCHEMA(QSchema.class, SchemaType.class, SchemaType::new),
    SECURITY_POLICY(QSecurityPolicy.class, SecurityPolicyType.class, SecurityPolicyType::new),
    SEQUENCE(QSequence.class, SequenceType.class, SequenceType::new),
    SERVICE(QService.class, ServiceType.class, ServiceType::new),
    SHADOW(QShadow.class, ShadowType.class, ShadowType::new),
    SIMULATION_RESULT(QSimulationResult.class, SimulationResultType.class, SimulationResultType::new),
    SYSTEM_CONFIGURATION(QSystemConfiguration.class, SystemConfigurationType.class, SystemConfigurationType::new),
    MARK(QMark.class, MarkType.class, MarkType::new),
    TASK(QTask.class, TaskType.class, TaskType::new),
    USER(QUser.class, UserType.class, UserType::new),
    VALUE_POLICY(QValuePolicy.class, ValuePolicyType.class, ValuePolicyType::new),
    POLICY(QPolicy.class, PolicyType.class, PolicyType::new);


    private final Class<? extends QObject<?>> queryType;
    private final Class<? extends ObjectType> schemaType;
    private final Supplier<? extends ObjectType> constructor;

    MObjectType(Class<? extends QObject<?>> queryType, Class<? extends ObjectType> schemaType, Supplier<? extends ObjectType> constructor) {
        this.queryType = queryType;
        this.schemaType = schemaType;
        this.constructor = constructor;
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

    public ObjectType createObject() {
        Preconditions.checkState(constructor != null, "Trying to instantiate abstract type");
        return constructor.get();
    }
}
