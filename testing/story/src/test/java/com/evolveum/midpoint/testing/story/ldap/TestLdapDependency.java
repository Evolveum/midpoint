/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.ldap;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Testing dependencies:
 * There are two meta-roles for orgs.
 * Org Metarole contains two inducements, one for creating organizationalUnit (intent=ou) and one for creating groupOfUniqueNames (intent=group) in ldap.
 * group depends on ou (since it is created in ou)
 * <p>
 * Org Metarole VIP is very similar it also contains two inducements, one for creating (intent=ou-vip) and one for creating groupOfUniqueNames (intent=group-vip) in ldap.
 * group-vip depends on ou-cip (since it is created in ou-vip)
 *
 * @author michael gruber
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapDependency extends AbstractLdapTest {

    public static final File TEST_DIR = new File(LDAP_TEST_DIR, "dependency");

    private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";

    public static final String ORG_TOP_OID = "00000000-8888-6666-0000-100000000001";

    public static final String ROLE_META_ORG_OID = "10000000-0000-0000-0000-000000006601";
    public static final String ROLE_META_ORG_VIP_OID = "10000000-0000-0000-0000-000000006602";
    public static final String ROLE_META_ORG_SUPERVIP_OID = "10000000-0000-0000-0000-000000006603";

    private static final String ORG_IT_NAME = "IT";
    private static final String ORG_HR_NAME = "HR";
    private static String orgItOid;
    private static String orgHrOid;

    private static final String ORG_TYPE_FUNCTIONAL = "functional";

    private static final String LDAP_GROUP_INTENT = "group";
    private static final String LDAP_GROUP_VIP_INTENT = "group-vip";
    private static final String LDAP_GROUP_SUPERVIP_INTENT = "group-supervip";

    private static final String LDAP_OU_INTENT = "ou";
    private static final String LDAP_OU_VIP_INTENT = "ou-vip";

    @Override
    protected String getLdapResourceOid() {
        return RESOURCE_OPENDJ_OID;
    }

    @Override
    protected String getTopOrgOid() {
        return ORG_TOP_OID;
    }

    private File getTestDir() {
        return TEST_DIR;
    }

    private File getResourceOpenDjFile() {
        return new File(getTestDir(), "resource-opendj.xml");
    }

    private File getOrgTopFile() {
        return new File(getTestDir(), "org-top.xml");
    }

    private File getRoleMetaOrgFile() {
        return new File(getTestDir(), "role-meta-org.xml");
    }

    private File getRoleMetaOrgVipFile() {
        return new File(getTestDir(), "role-meta-org-vip.xml");
    }

    private File getRoleMetaOrgSuperVipFile() {
        return new File(getTestDir(), "role-meta-org-supervip.xml");
    }

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServerRI();
    }

    @AfterClass
    public static void stopResources() {
        openDJController.stop();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Resources
        PrismObject<ResourceType> resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, getResourceOpenDjFile(), RESOURCE_OPENDJ_OID, initTask, initResult);
        openDJController.setResource(resourceOpenDj);

        // Org
        importObjectFromFile(getOrgTopFile(), initResult);

        // Roles
        importObjectFromFile(getRoleMetaOrgFile(), initResult);
        importObjectFromFile(getRoleMetaOrgVipFile(), initResult);
        importObjectFromFile(getRoleMetaOrgSuperVipFile(), initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        TestUtil.assertSuccess(testResultOpenDj);

        dumpOrgTree();
        dumpLdap();
    }

    @Test
    public void test100AddOrgIT() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = createOrg(ORG_IT_NAME, ORG_TOP_OID);

        // WHEN
        when();
        display("Adding org", orgBefore);
        addObject(orgBefore, task, result);

        // THEN
        then();
        assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<OrgType> orgAfter = getOrg(ORG_IT_NAME);
        orgItOid = orgAfter.getOid();

        assertSubOrgs(orgAfter, 0);
        assertSubOrgs(ORG_TOP_OID, 1);
    }

    @Test
    public void test150AssignFunctionalRoleToITOrg() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = getOrg(ORG_IT_NAME);

        // WHEN
        when();
        display("orgBefore: ", orgBefore);
        assignRoleToOrg(orgItOid, ROLE_META_ORG_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<OrgType> orgAfter = getOrg(ORG_IT_NAME);
        display("AFTER Assigning functional role org", orgAfter);
        assertSubOrgs(orgAfter, 0);
        assertSubOrgs(ORG_TOP_OID, 1);
        assertRoleMembershipRef(orgAfter, ROLE_META_ORG_OID, ORG_TOP_OID);
        assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_INTENT);
        assertLdapObject(orgAfter, ShadowKindType.GENERIC, LDAP_OU_INTENT);
    }

    @Test
    public void test170UnassignFunctionalRoleFromITOrg() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = getOrg(ORG_IT_NAME);

        // WHEN
        when();
        display("unassigning vip role org", orgBefore);
        unassignRoleFromOrg(orgItOid, ROLE_META_ORG_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<OrgType> orgAfter = getOrg(ORG_IT_NAME);
        display("AFTER unassigning functional role org", orgAfter);
        assertSubOrgs(orgAfter, 0);
        assertSubOrgs(ORG_TOP_OID, 1);
        assertRoleMembershipRef(orgAfter, ORG_TOP_OID);
        assertNotAssignedRole(orgAfter, ROLE_META_ORG_OID);
        //TODO: assert ldap objects deleted...
    }

    @Test
    public void test200AddOrgHR() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = createOrg(ORG_HR_NAME, ORG_TOP_OID);

        // WHEN
        when();
        display("Adding org", orgBefore);
        addObject(orgBefore, task, result);

        // THEN
        then();
        assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<OrgType> orgAfter = getOrg(ORG_HR_NAME);
        orgHrOid = orgAfter.getOid();

        assertSubOrgs(orgAfter, 0);
        assertSubOrgs(ORG_TOP_OID, 2);
    }

    @Test
    public void test250AssignFunctionalAndVipRoleToHROrg() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = getOrg(ORG_HR_NAME);

        // WHEN
        when();
        display("orgBefore: ", orgBefore);
        assignRoleToOrg(orgHrOid, ROLE_META_ORG_OID, task, result);
        assignRoleToOrg(orgHrOid, ROLE_META_ORG_VIP_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<OrgType> orgAfter = getOrg(ORG_HR_NAME);
        display("AFTER Assigning functional and vip role org", orgAfter);
        assertSubOrgs(orgAfter, 0);
        assertSubOrgs(ORG_TOP_OID, 2);
        assertRoleMembershipRef(orgAfter, ROLE_META_ORG_OID, ROLE_META_ORG_VIP_OID, ORG_TOP_OID);
        assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_INTENT);
        assertLdapObject(orgAfter, ShadowKindType.GENERIC, LDAP_OU_INTENT);
        assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_VIP_INTENT);
        assertLdapObject(orgAfter, ShadowKindType.GENERIC, LDAP_OU_VIP_INTENT);
    }

    @Test
    public void test270UnassignVipRoleFromHROrg() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = getOrg(ORG_HR_NAME);

        // WHEN
        when();
        display("unassigning vip role org", orgBefore);
        unassignRoleFromOrg(orgHrOid, ROLE_META_ORG_VIP_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<OrgType> orgAfter = getOrg(ORG_HR_NAME);
        display("AFTER unassigning vip role org", orgAfter);
        assertSubOrgs(orgAfter, 0);
        assertSubOrgs(ORG_TOP_OID, 2);
        assertRoleMembershipRef(orgAfter, ROLE_META_ORG_OID, ORG_TOP_OID);
        assertNotAssignedRole(orgAfter, ROLE_META_ORG_VIP_OID);
        assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_INTENT);
        assertLdapObject(orgAfter, ShadowKindType.GENERIC, LDAP_OU_INTENT);
        //TODO: assert ldap vip objects deleted...
    }

    //test280AssignVipAndSuperVipRoleToHROrg required for  test290UnassignVipRoleFromHROrg
    @Test
    public void test280AssignVipAndSuperVipRoleToHROrg() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = getOrg(ORG_HR_NAME);

        // WHEN
        when();
        display("orgBefore: ", orgBefore);
        assignRoleToOrg(orgHrOid, ROLE_META_ORG_VIP_OID, task, result);
        assignRoleToOrg(orgHrOid, ROLE_META_ORG_SUPERVIP_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<OrgType> orgAfter = getOrg(ORG_HR_NAME);
        display("AFTER Assigning supervip role org", orgAfter);
        assertSubOrgs(orgAfter, 0);
        assertSubOrgs(ORG_TOP_OID, 2);
        assertRoleMembershipRef(orgAfter, ROLE_META_ORG_OID, ROLE_META_ORG_VIP_OID, ROLE_META_ORG_SUPERVIP_OID, ORG_TOP_OID);
        assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_INTENT);
        assertLdapObject(orgAfter, ShadowKindType.GENERIC, LDAP_OU_INTENT);
        assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_VIP_INTENT);
        assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_SUPERVIP_INTENT);
        assertLdapObject(orgAfter, ShadowKindType.GENERIC, LDAP_OU_VIP_INTENT);
    }

    @Test
    public void test290UnassignVipRoleFromHROrg() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = getOrg(ORG_HR_NAME);

        display("unassigning vip role org", orgBefore);

        try {
            // WHEN
            when();
            unassignRoleFromOrg(orgHrOid, ROLE_META_ORG_VIP_OID, task, result);

            assertNotReached();

        } catch (PolicyViolationException e) {
            // this is expected
        }

        // THEN
        then();
        assertFailure(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<OrgType> orgAfter = getOrg(ORG_HR_NAME);
        display("AFTER unassigning vip role org", orgAfter);
        assertSubOrgs(orgAfter, 0);
        assertSubOrgs(ORG_TOP_OID, 2);
        assertRoleMembershipRef(orgAfter, ROLE_META_ORG_OID, ROLE_META_ORG_VIP_OID, ROLE_META_ORG_SUPERVIP_OID, ORG_TOP_OID);
        assertAssignedRole(orgAfter, ROLE_META_ORG_VIP_OID);
        assertLdapObject(orgAfter, ShadowKindType.ENTITLEMENT, LDAP_GROUP_INTENT);
        assertLdapObject(orgAfter, ShadowKindType.GENERIC, LDAP_OU_INTENT);
        //TODO: assert ldap vip objects deleted...
    }

    private PrismObject<OrgType> createOrg(String name, String parentOrgOid) throws SchemaException {
        PrismObject<OrgType> org = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(OrgType.class).instantiate();
        OrgType orgType = org.asObjectable();
        orgType.setName(new PolyStringType(name));
        orgType.getOrgType().add(ORG_TYPE_FUNCTIONAL);
        if (parentOrgOid != null) {
            AssignmentType parentAssignment = new AssignmentType();
            ObjectReferenceType parentAssignmentTargetRef = new ObjectReferenceType();
            parentAssignmentTargetRef.setOid(parentOrgOid);
            parentAssignmentTargetRef.setType(OrgType.COMPLEX_TYPE);
            parentAssignment.setTargetRef(parentAssignmentTargetRef);
            orgType.getAssignment().add(parentAssignment);
        }
        return org;
    }

    private void assertLdapObject(PrismObject<OrgType> org, ShadowKindType kind, String intent)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, DirectoryException, ExpressionEvaluationException {
        String orgName = org.getName().toString();
        displayValue("assert org", orgName);

        String objOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, kind, intent);
        PrismObject<ShadowType> objShadow = getShadowModel(objOid);
        display("Org " + orgName + " kind " + kind + " intent " + intent + " shadow", objShadow);
        // TODO assert shadow content

        String search = "";
        if (kind.equals(ShadowKindType.ENTITLEMENT)) {
            if (LDAP_GROUP_INTENT.equals(intent)) { search = "cn=" + orgName; }
            if (LDAP_GROUP_VIP_INTENT.equals(intent)) { search = "cn=" + orgName + "-vip"; }
            if (LDAP_GROUP_SUPERVIP_INTENT.equals(intent)) { search = "cn=" + orgName + "-supervip"; }
        }
        if (kind.equals(ShadowKindType.GENERIC)) {
            if (LDAP_OU_INTENT.equals(intent)) { search = "ou=" + orgName; }
            if (LDAP_OU_VIP_INTENT.equals(intent)) { search = "ou=" + orgName + "-vip"; }
        }
        Entry objEntry = openDJController.searchSingle(search);
        assertNotNull("No LDAP entry for " + orgName, objEntry);
        display("LDAP entry kind " + kind + " intent " + intent + " ldapObj", objEntry);

        if (kind.equals(ShadowKindType.ENTITLEMENT)) {
            OpenDJController.assertObjectClass(objEntry, "groupOfUniqueNames");
        }
        if (kind.equals(ShadowKindType.GENERIC)) {
            OpenDJController.assertObjectClass(objEntry, "organizationalUnit");
        }
    }
}
