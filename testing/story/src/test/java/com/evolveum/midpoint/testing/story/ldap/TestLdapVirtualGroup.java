/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.ldap;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.*;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;

import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Testing virtual groups in openDJ. Group membership is based on attributes of user object.
 * When a role is assigned midpoint should just write the correct user attribute values
 * instead of managing the group membership by associations.
 * <p>
 * in this test attribute description is used to do this magic (in real life this should be a custom attribute).
 * Object of class groupOfURLs is looking for description in users, object of class ds-virtual-static-group is
 * evaluating object of class groupOfURLs in OpenDj set ds-cfg-allow-retrieving-membership: true to show uniqueMember
 * in objects of class ds-virtual-static-group
 * <p>
 * dumpLdap()  does not show uniqueMember but when looking to DJ uniqueMember-attributes do exist.
 * (Seems uniqueMember must be defined explicitly in returnAttributes which is not the case)
 * <p>
 * Primary intent of test was to find replacement for $thisObject assignment variable. Tests are succesfull without using thisObject, though it
 * seems to be necessary to use different assignment variables. (depends on order of inducement)
 * <p>
 * Setting useThisObject = true will import IT-Role-HR and role-meta-ldap using "thisObject" assignment variable.
 * <p>
 * used roles:
 * IT-Role-HR: contains assignments for LDAP-Objects and inducement for setting description to user-account
 * role-meta-ldap: same as in IT-Role-HR but desinged as meta-role: creating LDAP-Objects is induced, inducement for setting description to user-account is order 2
 * IT-Role-Dev and IT-Role-Op: have assigend role-meta-ldap
 * Job-Role-DevOps: induces IT-Role-Dev and IT-Role-Op
 * Job-Role-MultiJobs: induces  Job-Role-DevOps and IT-Role-HR
 * <p>
 * Users and the roles being assigned in tests:
 * User0: IT-Role-HR
 * User1: IT-Role-Dev
 * User2: Job-Role-DevOps
 * User3: Job-Role-MultiJobs
 *
 * @author michael gruber
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapVirtualGroup extends AbstractLdapTest {

    private static boolean useThisObject = false;

    public static final File TEST_DIR = new File(LDAP_TEST_DIR, "virtualgroup");

    private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";

    public static final String ORG_TOP_OID = "00000000-8888-6666-0000-100000000001";

    public static final String ROLE_META_LDAP_OID = "10000000-0000-0000-0000-000000006601";
    public static final String ROLE_JOB_DEVOPS_OID = "10000000-0000-0000-0000-000000006602";
    public static final String ROLE_JOB_MULTIJOBS_OID = "10000000-0000-0000-0000-000000006603";
    public static final String ROLE_IT_HR_OID = "10000000-0000-0000-0000-000000006604";

    private static final String ROLE_IT_HR_NAME = "IT-Role-HR";
    private static final String ROLE_IT_DEV_NAME = "IT-Role-Dev";
    private static final String ROLE_IT_OP_NAME = "IT-Role-Op";

    private static String roleItDevOid;
    private static String roleItOpOid;

    private static final String USER_0_NAME = "User0";
    private static final String USER_1_NAME = "User1";
    private static final String USER_2_NAME = "User2";
    private static final String USER_3_NAME = "User3";

    private static final String LDAP_INTENT_DEFAULT = "default";
    private static final String LDAP_INTENT_VIRTUALSTATIC = "virtualstatic";
    private static final String LDAP_INTENT_DYNAMIC = "dynamic";

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

    private File getRoleMetaLdapFile() {
        String file = useThisObject ? "role-meta-ldap-thisobject.xml" : "role-meta-ldap.xml";
        return new File(getTestDir(), file);
    }

    private File getRoleItHrFile() {
        String file = useThisObject ? "role-it-hr-thisobject.xml" : "role-it-hr.xml";
        return new File(getTestDir(), file);
    }

    private File getRoleJobRoleDevOpsFile() {
        return new File(getTestDir(), "role-jobrole-devops.xml");
    }

    private File getRoleJobRoleMultiJobsFile() {
        return new File(getTestDir(), "role-jobrole-multijobs.xml");
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
        PrismObject<ResourceType> resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, getResourceOpenDjFile(), RESOURCE_OPENDJ_OID,
                initTask, initResult);
        openDJController.setResource(resourceOpenDj);

        // Org
        importObjectFromFile(getOrgTopFile(), initResult);

        // Roles
        importObjectFromFile(getRoleItHrFile(), initResult);
        importObjectFromFile(getRoleMetaLdapFile(), initResult);

        // set ds-cfg-allow-retrieving-membership: true to show uniqueMember in
        // ds-virtual-static-group
        String ldif = "dn: cn=Virtual Static uniqueMember,cn=Virtual Attributes,cn=config\n" + "changetype: modify\n"
                + "replace: ds-cfg-allow-retrieving-membership\n" + "ds-cfg-allow-retrieving-membership: true\n";
        openDJController.executeLdifChange(ldif);
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        TestUtil.assertSuccess(testResultOpenDj);

        dumpOrgTree();
        dumpLdap();
        display("FINISHED: test000Sanity");
    }

    @Test
    public void test090AddItRoleHR() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        display("recompute role " + ROLE_IT_HR_NAME);

        modelService.recompute(RoleType.class, ROLE_IT_HR_OID, ModelExecuteOptions.createReconcile(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_IT_HR_OID);
        roleItDevOid = roleAfter.getOid();

        assertLdapObject(roleAfter, ShadowKindType.ENTITLEMENT, LDAP_INTENT_VIRTUALSTATIC);
        assertLdapObject(roleAfter, ShadowKindType.GENERIC, LDAP_INTENT_DYNAMIC);
    }

    @Test
    public void test100AddItDevRole() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<RoleType> roleBefore = createLdapRole(ROLE_IT_DEV_NAME);

        // WHEN
        when();
        display("Adding role", roleBefore);
        addObject(roleBefore, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<RoleType> roleAfter = getObjectByName(RoleType.class, ROLE_IT_DEV_NAME);
        roleItDevOid = roleAfter.getOid();

        assertLdapObject(roleAfter, ShadowKindType.ENTITLEMENT, LDAP_INTENT_VIRTUALSTATIC);
        assertLdapObject(roleAfter, ShadowKindType.GENERIC, LDAP_INTENT_DYNAMIC);
    }

    @Test
    public void test110AddItOpRole() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<RoleType> roleBefore = createLdapRole(ROLE_IT_OP_NAME);

        // WHEN
        when();
        display("Adding role", roleBefore);
        addObject(roleBefore, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<RoleType> roleAfter = getObjectByName(RoleType.class, ROLE_IT_OP_NAME);
        roleItOpOid = roleAfter.getOid();

        assertLdapObject(roleAfter, ShadowKindType.ENTITLEMENT, LDAP_INTENT_VIRTUALSTATIC);
        assertLdapObject(roleAfter, ShadowKindType.GENERIC, LDAP_INTENT_DYNAMIC);
    }

    @Test
    public void test200CreateUsers() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user0Before = createUser(USER_0_NAME, "givenName0", "familyName0", true);
        PrismObject<UserType> user1Before = createUser(USER_1_NAME, "givenName1", "familyName1", true);
        PrismObject<UserType> user2Before = createUser(USER_2_NAME, "givenName2", "familyName2", true);
        PrismObject<UserType> user3Before = createUser(USER_3_NAME, "givenName3", "familyName3", true);

        // WHEN
        when();
        display("Adding user0", user0Before);
        addObject(user0Before, task, result);
        display("Adding user1", user1Before);
        addObject(user1Before, task, result);
        display("Adding user2", user2Before);
        addObject(user2Before, task, result);
        display("Adding user3", user3Before);
        addObject(user3Before, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user0After = getObjectByName(UserType.class, USER_0_NAME);
        display("user0 after", user0After);

        PrismObject<UserType> user1After = getObjectByName(UserType.class, USER_1_NAME);
        display("user1 after", user1After);

        PrismObject<UserType> user2After = getObjectByName(UserType.class, USER_2_NAME);
        display("user2 after", user2After);

        PrismObject<UserType> user3After = getObjectByName(UserType.class, USER_3_NAME);
        display("user3 after", user3After);

        dumpOrgTree();

    }

    @Test
    public void test210AssignItHrRoleToUser0() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user0Before = getObjectByName(UserType.class, USER_0_NAME);

        // WHEN
        when();
        display("user0Before: ", user0Before);
        assignRole(user0Before.getOid(), ROLE_IT_HR_OID);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<UserType> user0After = getObjectByName(UserType.class, USER_0_NAME);
        display("AFTER Assigning it-role hr ", user0After);
        assertRoleMembershipRef(user0After, ROLE_IT_HR_OID);

        assertLdapUserObject(user0After, ShadowKindType.ACCOUNT, LDAP_INTENT_DEFAULT);
        assertShadowAttribute(user0After, ShadowKindType.ACCOUNT, LDAP_INTENT_DEFAULT, "description",
                "dyngrp_" + ROLE_IT_HR_NAME);
    }

    @Test
    public void test220AssignItDevRoleToUser1() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user1Before = getObjectByName(UserType.class, USER_1_NAME);

        // WHEN
        when();
        display("user1Before: ", user1Before);
        assignRole(user1Before.getOid(), roleItDevOid);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<UserType> user1After = getObjectByName(UserType.class, USER_1_NAME);
        display("AFTER Assigning it-role dev ", user1After);
        assertRoleMembershipRef(user1After, roleItDevOid);

        assertLdapUserObject(user1After, ShadowKindType.ACCOUNT, LDAP_INTENT_DEFAULT);
        assertShadowAttribute(user1After, ShadowKindType.ACCOUNT, LDAP_INTENT_DEFAULT, "description",
                "dyngrp_" + ROLE_IT_DEV_NAME);
    }

    @Test
    public void test230AssignJobRoleToUser2() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        importObjectFromFile(getRoleJobRoleDevOpsFile(), result);

        PrismObject<UserType> user2Before = getObjectByName(UserType.class, USER_2_NAME);

        // WHEN
        when();
        display("user2Before: ", user2Before);
        assignRole(user2Before.getOid(), ROLE_JOB_DEVOPS_OID);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
        dumpLdap();
        //
        PrismObject<UserType> user2After = getObjectByName(UserType.class, USER_2_NAME);
        display("AFTER Assigning job-role ", user2After);

        // user must have jobrole and induced it-roles
        assertRoleMembershipRef(user2After, roleItDevOid, roleItOpOid, ROLE_JOB_DEVOPS_OID);

        assertLdapUserObject(user2After, ShadowKindType.ACCOUNT, LDAP_INTENT_DEFAULT);
        assertShadowAttribute(user2After, ShadowKindType.ACCOUNT, LDAP_INTENT_DEFAULT, "description",
                "dyngrp_" + ROLE_IT_DEV_NAME, "dyngrp_" + ROLE_IT_OP_NAME);
    }

    @Test
    public void test240AssignJobRoleMultiJobToUser3() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        importObjectFromFile(getRoleJobRoleMultiJobsFile(), result);

        PrismObject<UserType> user3Before = getObjectByName(UserType.class, USER_3_NAME);

        // WHEN
        when();
        display("user3Before: ", user3Before);
        assignRole(user3Before.getOid(), ROLE_JOB_MULTIJOBS_OID);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
        dumpLdap();
        //
        PrismObject<UserType> user3After = getObjectByName(UserType.class, USER_3_NAME);
        display("AFTER Assigning multi job-role ", user3After);

        // user must have jobrole multe and induced jobrole and  it-roles
        assertRoleMembershipRef(user3After, roleItDevOid, roleItOpOid, ROLE_JOB_DEVOPS_OID, ROLE_IT_HR_OID, ROLE_JOB_MULTIJOBS_OID);

        assertLdapUserObject(user3After, ShadowKindType.ACCOUNT, LDAP_INTENT_DEFAULT);
        assertShadowAttribute(user3After, ShadowKindType.ACCOUNT, LDAP_INTENT_DEFAULT, "description",
                "dyngrp_" + ROLE_IT_DEV_NAME, "dyngrp_" + ROLE_IT_OP_NAME, "dyngrp_" + ROLE_IT_HR_NAME);

    }

    private PrismObject<RoleType> createLdapRole(String name) throws SchemaException {
        PrismObject<RoleType> role = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(RoleType.class).instantiate();
        RoleType roleType = role.asObjectable();
        roleType.setName(new PolyStringType(name));
        AssignmentType roleAssignment = new AssignmentType();
        ObjectReferenceType roleAssignmentTargetRef = new ObjectReferenceType();
        roleAssignmentTargetRef.setOid(ROLE_META_LDAP_OID);
        roleAssignmentTargetRef.setType(RoleType.COMPLEX_TYPE);
        roleAssignment.setTargetRef(roleAssignmentTargetRef);
        roleType.getAssignment().add(roleAssignment);
        return role;
    }

    private void assertLdapUserObject(PrismObject<UserType> user, ShadowKindType kind, String intent)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, DirectoryException, ExpressionEvaluationException {
        String userName = user.getName().toString();
        displayValue("assert user", userName);

        String objOid = getLinkRefOid(user, RESOURCE_OPENDJ_OID, kind, intent);
        PrismObject<ShadowType> objShadow = getShadowModel(objOid);
        display("User " + userName + " kind " + kind + " intent " + intent + " shadow", objShadow);
        // TODO assert shadow content

        String search = "";
        if (kind.equals(ShadowKindType.ACCOUNT)) {
            if (LDAP_INTENT_DEFAULT.equals(intent)) { search = "uid=" + userName; }
        }
        Entry objEntry = openDJController.searchSingle(search);
        assertNotNull("No LDAP entry for " + userName + ", kind " + kind + ", intent " + intent, objEntry);
        display("LDAP entry kind " + kind + " inten " + intent + " ldapObj", objEntry);
    }

    private void assertLdapObject(PrismObject<RoleType> role, ShadowKindType kind, String intent)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, DirectoryException, ExpressionEvaluationException {
        String roleName = role.getName().toString();
        displayValue("assert role", roleName);

        String objOid = getLinkRefOid(role, RESOURCE_OPENDJ_OID, kind, intent);
        PrismObject<ShadowType> objShadow = getShadowModel(objOid);
        display("Role " + roleName + " kind " + kind + " intent " + intent + " shadow", objShadow);
        // TODO assert shadow content

        String search = "";
        if (kind.equals(ShadowKindType.ENTITLEMENT)) {
            if (LDAP_INTENT_VIRTUALSTATIC.equals(intent)) { search = "cn=vrtgrp-" + roleName; }
        }
        if (kind.equals(ShadowKindType.GENERIC)) {
            if (LDAP_INTENT_DYNAMIC.equals(intent)) { search = "cn=dyngrp-" + roleName; }
        }
        Entry objEntry = openDJController.searchSingle(search);
        assertNotNull("No LDAP entry for " + roleName + ", kind " + kind + ", intent " + intent, objEntry);
        display("LDAP entry kind " + kind + " inten " + intent + " ldapObj", objEntry);
    }

    private void assertShadowAttribute(PrismObject focus,
            ShadowKindType kind, String intent, String attribute, String... values)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        String focusName = focus.getName().toString();
        displayValue("assert focus " + focus.getCompileTimeClass(), focusName);

        String objOid = getLinkRefOid(focus, RESOURCE_OPENDJ_OID, kind, intent);
        PrismObject<ShadowType> objShadow = getShadowModel(objOid);
        display("Focus " + focusName + " kind " + kind + " intent " + intent + " shadow", objShadow);

        List<String> valuesList = new ArrayList<>(Arrays.asList(values));

        for (Object att : objShadow.asObjectable().getAttributes().asPrismContainerValue().getItems()) {
            if (att instanceof ResourceAttribute) {
                Collection propVals = ((ResourceAttribute) att).getRealValues();

                if (attribute.equals(((ResourceAttribute) att).getNativeAttributeName())) {

                    List<String> propValsString = new ArrayList<>(propVals.size());
                    for (Object pval : propVals) {
                        propValsString.add(pval.toString());
                    }

                    Collections.sort(propValsString);
                    Collections.sort(valuesList);

                    assertEquals(propValsString, valuesList);
                }
            }
        }
    }

    protected <F extends com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType> PrismObject<F> getObjectByName(
            Class clazz, String name)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<F> object = (PrismObject<F>) findObjectByName(clazz, name);
        assertNotNull("The object " + name + " of type " + clazz + " is missing!", object);
        display(clazz + " " + name, object);
        PrismAsserts.assertPropertyValue(object, F.F_NAME, PrismTestUtil.createPolyString(name));
        return object;
    }
}
