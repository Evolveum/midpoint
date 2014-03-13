package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * @author lazyman
 */
public class ObjectDBValidator {

    public static void validateObject(PrismObject object, Session session) {
        Query query = session.createQuery("from RObject where oid=:oid");
        query.setString("oid", object.getOid());
        RObject rObject = (RObject) query.uniqueResult();

        if (ObjectType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateObject(rObject, object);
        }

        if (SystemConfigurationType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateSystemConfiguration((RSystemConfiguration) rObject, object);
        } else if (TaskType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateTask((RTask) rObject, object);
        } else if (ConnectorHostType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateConnectorHost((RConnectorHost) rObject, object);
        } else if (ConnectorType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateConnector((RConnector) rObject, object);
        } else if (FocusType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateFocus((RFocus) rObject, object);
        } else if (ValuePolicyType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateValuePolicy((RValuePolicy) rObject, object);
        } else if (ReportType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateReport((RReport) rObject, object);
        } else if (ResourceType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateResource((RResource) rObject, object);
        } else if (ShadowType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateShadow((RShadow) rObject, object);
        } else if (ReportOutputType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateReportOutput((RReportOutput) rObject, object);
        } else if (GenericObjectType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateGenericObject((RGenericObject) rObject, object);
        } else if (ObjectTemplateType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateObjectTemplate((RObjectTemplate) rObject, object);
        } else if (NodeType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateNode((RNode) rObject, object);
        }
    }

    private static void validateConnector(RConnector connector, PrismObject object) {

    }

    private static void validateConnectorHost(RConnectorHost host, PrismObject object) {

    }

    private static void validateFocus(RFocus focus, PrismObject object) {

    }

    private static void validateValuePolicy(RValuePolicy policy, PrismObject object) {

    }

    private static void validateReportOutput(RReportOutput report, PrismObject object) {

    }

    private static void validateGenericObject(RGenericObject rObject, PrismObject object) {

    }

    private static void validateObjectTemplate(RObjectTemplate template, PrismObject object) {

    }

    private static void validateNode(RNode node, PrismObject object) {

    }

    private static void validateReport(RReport report, PrismObject object) {

    }

    private static void validateResource(RResource resource, PrismObject object) {

    }

    private static void validateShadow(RShadow shadow, PrismObject object) {

    }

    private static void validateObject(RObject rObject, PrismObject object) {

    }

    private static void validateSystemConfiguration(RSystemConfiguration config, PrismObject object) {

    }

    private static void validateTask(RTask task, PrismObject object) {

    }
}
