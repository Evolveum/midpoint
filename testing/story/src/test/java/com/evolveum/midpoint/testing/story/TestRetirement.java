/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRetirement extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "retirement");

    protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
    protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
    protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;

    public static final File ORG_TOP_FILE = new File(TEST_DIR, "org-top.xml");
    public static final String ORG_TOP_OID = "00000000-8888-6666-0000-100000000001";

    public static final File ORG_RETIRED_FILE = new File(TEST_DIR, "org-retired.xml");
    public static final String ORG_RETIRED_OID = "00000000-8888-6666-0000-100000ffffff";

    public static final File ROLE_META_ORG_FILE = new File(TEST_DIR, "role-meta-org.xml");
    public static final String ROLE_META_ORG_OID = "10000000-0000-0000-0000-000000006601";

    protected static final String ORG_ROYULA_CARPATHIA_NAME = "Royula Carpathia";
    protected static final String ORG_CORTUV_HRAD_NAME = "Čortův hrád";
    protected static final String ORG_VYSNE_VLKODLAKY_NAME = "Vyšné Vlkodlaky";

    protected static final String ORG_TYPE_FUNCTIONAL = "functional";

    protected static final String LDAP_GROUP_INTENT = "group";
    protected static final String LDAP_OU_INTENT = "ou";

    protected static final String USER_TELEKE_USERNAME = "teleke";
    protected static final String USER_TELEKE_GIVEN_NAME = "Felix";
    protected static final String USER_TELEKE_FAMILY_NAME = "Teleke z Tölökö";

    protected static final String USER_GORC_USERNAME = "gorc";
    protected static final String USER_GORC_GIVEN_NAME = "Robert";
    protected static final String USER_GORC_FAMILY_NAME = "Gorc z Gorců";

    protected static final String USER_DEZI_USERNAME = "dezi";
    protected static final String USER_DEZI_GIVEN_NAME = "Vilja";
    protected static final String USER_DEZI_FAMILY_NAME = "Dézi";

    protected ResourceType resourceOpenDjType;
    protected PrismObject<ResourceType> resourceOpenDj;

    protected String orgRolyulaCarpathiaOid;
    protected String orgCortuvHradOid;
    protected String orgVysneVlkodlakyOid;
    protected String userGorcOid;

    @Override
    protected String getTopOrgOid() {
        return ORG_TOP_OID;
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
        resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
        resourceOpenDjType = resourceOpenDj.asObjectable();
        openDJController.setResource(resourceOpenDj);

        // Org
        importObjectFromFile(ORG_TOP_FILE, initResult);

        // Role
        importObjectFromFile(ROLE_META_ORG_FILE, initResult);
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
    public void test050AddOrgRetired() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = PrismTestUtil.parseObject(ORG_RETIRED_FILE);

        // WHEN
        when();
        display("Adding org", orgBefore);
        addObject(orgBefore, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<OrgType> org = getObject(OrgType.class, ORG_RETIRED_OID);
        display("org", org);
        PrismAsserts.assertPropertyValue(org, OrgType.F_ORG_TYPE, ORG_TYPE_FUNCTIONAL);

        String ouShadowOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.GENERIC, LDAP_OU_INTENT);
        PrismObject<ShadowType> ouShadow = getShadowModel(ouShadowOid);
        display("Org retired OU shadow", ouShadow);
        // TODO assert shadow content

        Entry ouEntry = openDJController.fetchEntry(ouShadow.getName().getOrig());
        assertNotNull("No ou LDAP entry for retirement (" + ouShadow.getName().getOrig() + ")", ouEntry);
        displayValue("OU retirement entry", openDJController.toHumanReadableLdifoid(ouEntry));
        OpenDJController.assertObjectClass(ouEntry, "organizationalUnit");

        assertSubOrgs(org, 0);
    }

    @Test
    public void test100AddOrgRoyulaCarpathia() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = createOrg(ORG_ROYULA_CARPATHIA_NAME, ORG_TOP_OID);

        // WHEN
        when();
        display("Adding org", orgBefore);
        addObject(orgBefore, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<OrgType> orgAfter = getAndAssertFunctionalOrg(ORG_ROYULA_CARPATHIA_NAME, ORG_TOP_OID);
        orgRolyulaCarpathiaOid = orgAfter.getOid();

        assertSubOrgs(orgAfter, 0);
        assertSubOrgs(ORG_TOP_OID, 1);
    }

    @Test
    public void test110AddUserTeleke() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = createUser(USER_TELEKE_USERNAME,
                USER_TELEKE_GIVEN_NAME, USER_TELEKE_FAMILY_NAME, orgRolyulaCarpathiaOid);

        // WHEN
        when();
        display("Adding user", userBefore);
        addObject(userBefore, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        getAndAssertUser(USER_TELEKE_USERNAME);

        PrismObject<OrgType> orgAfter = getAndAssertFunctionalOrg(ORG_ROYULA_CARPATHIA_NAME, ORG_TOP_OID);

        dumpOrgTree();
        dumpLdap();

        assertSubOrgs(orgAfter, 0);
        assertSubOrgs(ORG_TOP_OID, 1);
    }

    @Test
    public void test200AddOrgCortuvHrad() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = createOrg(ORG_CORTUV_HRAD_NAME, orgRolyulaCarpathiaOid);

        // WHEN
        when();
        display("Adding org", orgBefore);
        addObject(orgBefore, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<OrgType> orgAfter = getAndAssertFunctionalOrg(ORG_CORTUV_HRAD_NAME, orgRolyulaCarpathiaOid);
        orgCortuvHradOid = orgAfter.getOid();

        dumpOrgTree();
        dumpLdap();

        assertSubOrgs(orgAfter, 0);
        assertSubOrgs(orgRolyulaCarpathiaOid, 1);
        assertSubOrgs(ORG_TOP_OID, 1);
    }

    @Test
    public void test210AddUserGorc() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = createUser(USER_GORC_USERNAME,
                USER_GORC_GIVEN_NAME, USER_GORC_FAMILY_NAME, orgCortuvHradOid);

        // WHEN
        when();
        display("Adding user", userBefore);
        addObject(userBefore, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getAndAssertUser(USER_GORC_USERNAME);
        userGorcOid = userAfter.getOid();

        dumpOrgTree();
        dumpLdap();
    }

    @Test
    public void test220AddOrgVysneVlkodlaky() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<OrgType> orgBefore = createOrg(ORG_VYSNE_VLKODLAKY_NAME, orgCortuvHradOid);

        // WHEN
        when();
        display("Adding org", orgBefore);
        addObject(orgBefore, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<OrgType> orgAfter = getAndAssertFunctionalOrg(ORG_VYSNE_VLKODLAKY_NAME, orgCortuvHradOid);
        orgVysneVlkodlakyOid = orgAfter.getOid();

        dumpOrgTree();
        dumpLdap();

        assertSubOrgs(orgAfter, 0);
        assertSubOrgs(orgRolyulaCarpathiaOid, 1);
        assertSubOrgs(ORG_TOP_OID, 1);
    }

    @Test
    public void test230AddUserViljaDezi() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = createUser(USER_DEZI_USERNAME,
                USER_DEZI_GIVEN_NAME, USER_DEZI_FAMILY_NAME, orgVysneVlkodlakyOid);

        // WHEN
        when();
        display("Adding user", userBefore);
        addObject(userBefore, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        getAndAssertUser(USER_DEZI_USERNAME);

        dumpOrgTree();
        dumpLdap();
    }

    @Test
    public void test300RetireUserGorc() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        modifications.add(createAssignmentModification(orgCortuvHradOid, OrgType.COMPLEX_TYPE, null, null, null, false));
        modifications.add(createAssignmentModification(ORG_RETIRED_OID, OrgType.COMPLEX_TYPE, null, null, null, true));
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModifyDelta(userGorcOid, modifications, UserType.class);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<UserType> userAfter = getAndAssertRetiredUser(USER_GORC_USERNAME);
        userGorcOid = userAfter.getOid();
    }

    @Test
    public void test302ReconcileUserGorc() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        reconcileUser(userGorcOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<UserType> userAfter = getAndAssertRetiredUser(USER_GORC_USERNAME);
        userGorcOid = userAfter.getOid();
    }

    @Test
    public void test303ReconcileUserGorcAgain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        reconcileUser(userGorcOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpOrgTree();
        dumpLdap();

        PrismObject<UserType> userAfter = getAndAssertRetiredUser(USER_GORC_USERNAME);
        userGorcOid = userAfter.getOid();
    }

    private PrismObject<UserType> createUser(String username, String givenName,
            String familyName, String parentOrgOid) throws SchemaException {
        PrismObject<UserType> user = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class).instantiate();
        UserType userType = user.asObjectable();
        userType.setName(new PolyStringType(username));
        userType.setGivenName(new PolyStringType(givenName));
        userType.setFamilyName(new PolyStringType(familyName));
        userType.setFullName(new PolyStringType(givenName + " " + familyName));
        if (parentOrgOid != null) {
            AssignmentType parentAssignment = new AssignmentType();
            ObjectReferenceType parentAssignmentTargetRef = new ObjectReferenceType();
            parentAssignmentTargetRef.setOid(parentOrgOid);
            parentAssignmentTargetRef.setType(OrgType.COMPLEX_TYPE);
            parentAssignment.setTargetRef(parentAssignmentTargetRef);
            userType.getAssignment().add(parentAssignment);
        }
        return user;
    }

    private PrismObject<OrgType> createOrg(String name, String parentOrgOid) throws SchemaException {
        PrismObject<OrgType> org = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(OrgType.class).instantiate();
        OrgType orgType = org.asObjectable();
        orgType.setName(new PolyStringType(name));
        orgType.getOrgType().add(ORG_TYPE_FUNCTIONAL);
        AssignmentType metaRoleAssignment = new AssignmentType();
        ObjectReferenceType metaRoleAssignmentTargetRef = new ObjectReferenceType();
        metaRoleAssignmentTargetRef.setOid(ROLE_META_ORG_OID);
        metaRoleAssignmentTargetRef.setType(RoleType.COMPLEX_TYPE);
        metaRoleAssignment.setTargetRef(metaRoleAssignmentTargetRef);
        orgType.getAssignment().add(metaRoleAssignment);
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

    private PrismObject<UserType> getAndAssertUser(String username)
            throws CommonException, DirectoryException {
        PrismObject<UserType> user = findUserByUsername(username);
        display("user", user);

        String shadowOid = getLinkRefOid(user, RESOURCE_OPENDJ_OID, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);
        PrismObject<ShadowType> accountShadow = getShadowModel(shadowOid);
        display("Account " + username + " shadow", accountShadow);
        // TODO assert shadow content

        Entry accountEntry = openDJController.searchSingle("uid=" + username);
        assertNotNull("No account LDAP entry for " + username, accountEntry);
        displayValue("account entry", openDJController.toHumanReadableLdifoid(accountEntry));
        OpenDJController.assertObjectClass(accountEntry, "inetOrgPerson");

        return user;
    }

    private PrismObject<UserType> getAndAssertRetiredUser(String username) throws CommonException, DirectoryException {
        PrismObject<UserType> user = findUserByUsername(username);
        display("user", user);

        String shadowOid = getLinkRefOid(user, RESOURCE_OPENDJ_OID, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);
        PrismObject<ShadowType> accountShadow = getShadowModel(shadowOid);
        display("Account " + username + " shadow", accountShadow);
        // TODO assert shadow content

        String dn = "uid=RRR-" + username + ",ou=RETIRED,dc=example,dc=com";
        Entry accountEntry = openDJController.fetchEntry(dn);
        assertNotNull("No account LDAP entry for " + username + " (" + dn + ")", accountEntry);
        displayValue("account entry", openDJController.toHumanReadableLdifoid(accountEntry));
        OpenDJController.assertObjectClass(accountEntry, "inetOrgPerson");

        return user;
    }

    private PrismObject<OrgType> getAndAssertFunctionalOrg(String orgName, String directParentOrgOid) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException, ExpressionEvaluationException {
        PrismObject<OrgType> org = getOrg(orgName);
        display("org", org);
        PrismAsserts.assertPropertyValue(org, OrgType.F_ORG_TYPE, ORG_TYPE_FUNCTIONAL);
        assertAssignedRole(org, ROLE_META_ORG_OID);

        String groupOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, LDAP_GROUP_INTENT);
        PrismObject<ShadowType> groupShadow = getShadowModel(groupOid);
        display("Org " + orgName + " group shadow", groupShadow);
        // TODO assert shadow content

        Entry groupEntry = openDJController.searchSingle("cn=" + orgName);
        assertNotNull("No group LDAP entry for " + orgName, groupEntry);
        displayValue("OU GROUP entry", openDJController.toHumanReadableLdifoid(groupEntry));
        OpenDJController.assertObjectClass(groupEntry, "groupOfUniqueNames");

        assertHasOrg(org, directParentOrgOid);
        assertAssignedOrg(org, directParentOrgOid);

        return org;
    }

    private void dumpLdap() throws DirectoryException {
        displayValue("LDAP server tree", openDJController.dumpTree());
        displayValue("LDAP server content", openDJController.dumpEntries());
    }
}
