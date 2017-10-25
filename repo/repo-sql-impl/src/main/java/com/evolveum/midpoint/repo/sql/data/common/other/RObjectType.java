package com.evolveum.midpoint.repo.sql.data.common.other;

import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lazyman
 */
public enum RObjectType {

    CONNECTOR(RConnector.class, ConnectorType.class),
    CONNECTOR_HOST(RConnectorHost.class, ConnectorHostType.class),
    GENERIC_OBJECT(RGenericObject.class, GenericObjectType.class),
    OBJECT(RObject.class, ObjectType.class),
    VALUE_POLICY(RValuePolicy.class, ValuePolicyType.class),
    RESOURCE(RResource.class, ResourceType.class),
    SHADOW(RShadow.class, ShadowType.class),
    ROLE(RRole.class, RoleType.class),
    SYSTEM_CONFIGURATION(RSystemConfiguration.class, SystemConfigurationType.class),
    TASK(RTask.class, TaskType.class),
    USER(RUser.class, UserType.class),
    REPORT(RReport.class, ReportType.class),
    REPORT_OUTPUT(RReportOutput.class, ReportOutputType.class),
    OBJECT_TEMPLATE(RObjectTemplate.class, ObjectTemplateType.class),
    NODE(RNode.class, NodeType.class),
    ORG(ROrg.class, OrgType.class),
    ABSTRACT_ROLE(RAbstractRole.class, AbstractRoleType.class),
    FOCUS(RFocus.class, FocusType.class),
    SECURITY_POLICY(RSecurityPolicy.class, SecurityPolicyType.class),
    LOOKUP_TABLE(RLookupTable.class, LookupTableType.class),
    ACCESS_CERTIFICATION_DEFINITION(RAccessCertificationDefinition.class, AccessCertificationDefinitionType.class),
    ACCESS_CERTIFICATION_CAMPAIGN(RAccessCertificationCampaign.class, AccessCertificationCampaignType.class),
    SEQUENCE(RSequence.class, SequenceType.class),
    SERVICE(RService.class, ServiceType.class),
    FORM(RForm.class, FormType.class),
    CASE(RCase.class, CaseType.class),
    FUNCTION_LIBRARY(RFunctionLibrary.class, FunctionLibraryType.class);

    private Class<? extends RObject> clazz;
    private Class<? extends ObjectType> jaxbClass;

    RObjectType(Class<? extends RObject> clazz, Class<? extends ObjectType> jaxbClass) {
        this.clazz = clazz;
        this.jaxbClass = jaxbClass;
    }

    public Class<? extends RObject> getClazz() {
        return clazz;
    }

    public Class<? extends ObjectType> getJaxbClass() {
        return jaxbClass;
    }

    public static <T extends RObject> RObjectType getType(Class<T> clazz) {
        for (RObjectType type : RObjectType.values()) {
            if (type.getClazz().equals(clazz)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Couldn't find type for class '" + clazz + "'.");
    }
}
