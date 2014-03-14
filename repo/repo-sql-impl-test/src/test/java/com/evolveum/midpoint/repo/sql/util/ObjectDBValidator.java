package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.hibernate.Query;
import org.hibernate.Session;
import static org.testng.AssertJUnit.*;

import javax.xml.namespace.QName;
import java.util.Set;

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

    private static void assertEqualsPolyString(RPolyString s1, PolyString s2, QName qname) {
        if (s1 == null && s2 == null) {
            return;
        }
        if ((s1 == null && s2 != null) || (s1 != null && s2 == null)) {
            fail("Not equal " + qname);
        }

    }

    private static void validateConnectorHost(RConnectorHost host, PrismObject object) {

    }

    private static void validateFocus(RFocus focus, PrismObject object) {


        if (AbstractRoleType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateAbstractRole((RAbstractRole) focus, object);
        } else if (UserType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateUser((RUser) focus, object);
        }
    }

    private static void validateAbstractRole(RAbstractRole role, PrismObject object) {


        if (RoleType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateRole((RRole) role, object);
        } else if (OrgType.class.isAssignableFrom(object.getCompileTimeClass())) {
            validateOrg((ROrg) role, object);
        }
    }

    private static void validateRole(RRole role, PrismObject object) {

    }

    private static void validateOrg(ROrg org, PrismObject object) {

    }

    private static void validateUser(RUser user, PrismObject object) {

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
        assertEqualsPolyString(rObject.getName(), (PolyString) object.getPropertyRealValue(ObjectType.F_NAME, PolyString.class), ObjectType.F_NAME);
        assertEquals(rObject.getOid(), object.getOid());
        assertEquals(Integer.toString(rObject.getVersion()), object.getVersion());

        validateMetadata(rObject, object.findContainer(ObjectType.F_METADATA));
        validateReference(rObject.getTenantRef(), object.findReference(ObjectType.F_TENANT_REF));
        validateReferences(rObject.getParentOrgRef(), object.findReference(ObjectType.F_PARENT_ORG_REF));

        //todo
        rObject.getExtension();
        rObject.getTrigger();
    }

    private static void validateReferences(Set<ObjectReference> set, PrismReference reference) {

    }

    private static void validateReference(ObjectReference ref, PrismReference reference) {

    }

    private static void validateMetadata(Metadata metadata, PrismContainer container) {

    }

    private static void validateSystemConfiguration(RSystemConfiguration config, PrismObject object) {

    }

    private static void validateTask(RTask task, PrismObject object) {

    }
}
