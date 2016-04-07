package com.evolveum.midpoint.repo.sql.data.common.other;

import com.evolveum.midpoint.repo.sql.data.common.*;

/**
 * @author lazyman
 */
public enum RObjectType {

    CONNECTOR(RConnector.class),
    CONNECTOR_HOST(RConnectorHost.class),
    GENERIC_OBJECT(RGenericObject.class),
    OBJECT(RObject.class),
    VALUE_POLICY(RValuePolicy.class),
    RESOURCE(RResource.class),
    SHADOW(RShadow.class),
    ROLE(RRole.class),
    SYSTEM_CONFIGURATION(RSystemConfiguration.class),
    TASK(RTask.class),
    USER(RUser.class),
    REPORT(RReport.class),
    REPORT_OUTPUT(RReportOutput.class),
    OBJECT_TEMPLATE(RObjectTemplate.class),
    NODE(RNode.class),
    ORG(ROrg.class),
    ABSTRACT_ROLE(RAbstractRole.class),
    FOCUS(RFocus.class),
    SECURITY_POLICY(RSecurityPolicy.class),
    LOOKUP_TABLE(RLookupTable.class),
    ACCESS_CERTIFICATION_DEFINITION(RAccessCertificationDefinition.class),
    ACCESS_CERTIFICATION_CAMPAIGN(RAccessCertificationCampaign.class),
    SEQUENCE(RSequence.class),
    SERVICE(RService.class);

    private Class<? extends RObject> clazz;

    private RObjectType(Class<? extends RObject> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends RObject> getClazz() {
        return clazz;
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
