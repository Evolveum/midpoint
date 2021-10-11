/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.mapping;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.assertEquals;

@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMappingAutoInbound extends AbstractMappingTest {

    protected static final File RESOURCE_DUMMY_AUTOGREEN_FILE = new File(TEST_DIR, "resource-dummy-autogreen.xml");
    protected static final String RESOURCE_DUMMY_AUTOGREEN_OID = "10000000-0000-0000-0000-00000000a404";
    protected static final String RESOURCE_DUMMY_AUTOGREEN_NAME = "autogreen";

    private static final String GROUP_DUMMY_CRATIC_NAME = "cratic";

    private String userHermanOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        initDummyResourcePirate(RESOURCE_DUMMY_AUTOGREEN_NAME,
                RESOURCE_DUMMY_AUTOGREEN_FILE, RESOURCE_DUMMY_AUTOGREEN_OID, initTask, initResult);

        repoAddObjectFromFile(ROLE_AUTOMATIC_FILE, initResult);
        repoAddObjectFromFile(ROLE_AUTOCRATIC_FILE, initResult);
        repoAddObjectFromFile(ROLE_AUTODIDACTIC_FILE, initResult);
        repoAddObjectFromFile(ROLE_AUTOGRAPHIC_FILE, initResult);
        repoAddObjectFromFile(ROLE_AUTOTESTERS_FILE, initResult);
        repoAddObjectFromFile(ROLE_ADMINS_FILE, initResult);
    }

    /**
     * MID-2104
     */
    @Test
    public void test100ImportFromResource() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount accountHerman = new DummyAccount(USER_HERMAN_USERNAME);
        accountHerman.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, USER_HERMAN_FULL_NAME);
        accountHerman.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "matic");
        getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).addAccount(accountHerman);

        // Preconditions
        assertUsers(getNumberOfUsers());

        // WHEN
        when();
        modelService.importFromResource(RESOURCE_DUMMY_AUTOGREEN_OID, new QName(MidPointConstants.NS_RI, SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), task, result);

        // THEN
        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        waitForTaskFinish(task, true, 40000);

        // THEN
        then();
        assertSuccess(task.getResult());

        SearchResultList<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);

        PrismObject<UserType> userHermanAfter = findUserByUsername(USER_HERMAN_USERNAME);
        display("User after", userHermanAfter);
        userHermanOid = userHermanAfter.getOid();
        assertUser(userHermanAfter, userHermanAfter.getOid(), USER_HERMAN_USERNAME, USER_HERMAN_FULL_NAME, null, null);
        assertAssignedRole(userHermanAfter, ROLE_AUTOMATIC_OID);
        assertAssignments(userHermanAfter, 1);

        assertEquals("Unexpected number of users", getNumberOfUsers() + 1, users.size());
    }


    /**
     * MID-2104
     */
    @Test
    public void test110ModifyAccountTitleCraticAndReconcile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount accountHerman = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getAccountByUsername(USER_HERMAN_USERNAME);
        accountHerman.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "cratic");

        // WHEN
        when();
        reconcileUser(userHermanOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(userHermanOid);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignments(userAfter, 1);
    }

    /**
     * MID-2104
     */
    @Test
    public void test112ModifyAccountTitleDidacticGraphicAndReconcile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount accountHerman = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getAccountByUsername(USER_HERMAN_USERNAME);
        accountHerman.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "didactic", "graphic");

        // WHEN
        when();
        reconcileUser(userHermanOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(userHermanOid);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_AUTODIDACTIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTOGRAPHIC_OID);
        assertAssignments(userAfter, 2);
    }

    // TODO: tests with range (other role assignments present)

    // TODO: associations

    @Test
    public void test200ImportFromResourceAssociations() throws Exception {
        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_AUTOGREEN_OID, AssignmentPolicyEnforcementType.RELATIVE, false);

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyGroup dummyGroup = new DummyGroup(GROUP_DUMMY_TESTERS_NAME);
        getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).addGroup(dummyGroup);
        dummyGroup.addMember(USER_HERMAN_USERNAME);

        dummyGroup = new DummyGroup(GROUP_DUMMY_CRATIC_NAME);
        getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).addGroup(dummyGroup);

        dummyGroup.addMember(USER_HERMAN_USERNAME);

        // WHEN
        when();
        modelService.importFromResource(RESOURCE_DUMMY_AUTOGREEN_OID, new QName(MidPointConstants.NS_RI, "AccountObjectClass"), task, result);

        // THEN
        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        waitForTaskFinish(task, true, 40000);

        // THEN
        then();
        assertSuccess(task.getResult());

        SearchResultList<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);

        UserAsserter<Void> userAsserter = assertUserAfterByUsername(USER_HERMAN_USERNAME);
        userHermanOid = userAsserter.getOid();
        PrismObject<UserType> userAfter = userAsserter.getObject();
        assertUser(userAfter, userHermanOid, USER_HERMAN_USERNAME, USER_HERMAN_FULL_NAME, null, null);
        userAsserter
                .assignments()
                    .forRole(ROLE_AUTODIDACTIC_OID)
                        .assertOriginMappingName("Assignment from title") // MID-5846
                        .end()
                    .forRole(ROLE_AUTOGRAPHIC_OID)
                        .assertOriginMappingName("Assignment from title") // MID-5846
                        .end()
                    .forRole(ROLE_AUTOTESTERS_OID)
                        .assertOriginMappingName("Assignment from group") // MID-5846
                        .end()
                    .forRole(ROLE_AUTOCRATIC_OID)
                        .assertOriginMappingName("Assignment from group") // MID-5846
                        .end()
                    .assertAssignments(4);

        assertEquals("Unexpected number of users", getNumberOfUsers() + 1, users.size());
    }

    @Test
    public void test300ModifyAccountDirectAssign() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(userHermanOid, ROLE_ADMINS_OID);
        reconcileUser(userHermanOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(userHermanOid);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_AUTODIDACTIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTOGRAPHIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTOTESTERS_OID);
        assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignedRole(userAfter, ROLE_ADMINS_OID);
        assertAssignments(userAfter, 5);
    }

    @Test
    public void test301removeUserFromAutoGroup() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyGroup craticGroup = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getGroupByName(GROUP_DUMMY_CRATIC_NAME);
        craticGroup.removeMember(USER_HERMAN_USERNAME);

        DummyAccount hermanAccount = getDummyAccount(RESOURCE_DUMMY_AUTOGREEN_NAME, USER_HERMAN_USERNAME);
        hermanAccount.removeAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, Collections.singletonList("didactic"));

        assertNoDummyGroupMember(RESOURCE_DUMMY_AUTOGREEN_NAME, GROUP_DUMMY_CRATIC_NAME, USER_HERMAN_USERNAME);


        // WHEN
        when();
        modelService.importFromResource(RESOURCE_DUMMY_AUTOGREEN_OID, new QName(MidPointConstants.NS_RI, "AccountObjectClass"), task, result);

        // THEN
        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        waitForTaskFinish(task, true, 70000);

        // THEN
        then();
        assertSuccess(task.getResult());

        PrismObject<UserType> userAfter = getUser(userHermanOid);
        display("User after", userAfter);

        assertNotAssignedRole(userAfter, ROLE_AUTODIDACTIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTOGRAPHIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTOTESTERS_OID);
        assertNotAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignedRole(userAfter, ROLE_ADMINS_OID);
        assertAssignments(userAfter, 3);
    }


    @Test
    public void test402assignAutoGroupDirectly() throws Exception {
        DummyGroup craticGroup = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getGroupByName(GROUP_DUMMY_CRATIC_NAME);
        craticGroup.removeMember(USER_HERMAN_USERNAME);

        DummyGroup testersGroup = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getGroupByName(GROUP_DUMMY_TESTERS_NAME);
        testersGroup.addMember(USER_HERMAN_USERNAME);

        assertDummyGroupMember(RESOURCE_DUMMY_AUTOGREEN_NAME, GROUP_DUMMY_TESTERS_NAME, USER_HERMAN_USERNAME);

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(userHermanOid, ROLE_AUTOCRATIC_OID);

        // THEN
        then();
        assertSuccess(result);

        assertDummyGroupMember(RESOURCE_DUMMY_AUTOGREEN_NAME, GROUP_DUMMY_TESTERS_NAME, USER_HERMAN_USERNAME);

        PrismObject<UserType> userAfter = getUser(userHermanOid);
        display("User after", userAfter);
//        assertAssignedRole(userAfter, ROLE_AUTODIDACTIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTOGRAPHIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTOTESTERS_OID);
        assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignedRole(userAfter, ROLE_ADMINS_OID);
        assertAssignments(userAfter, 4);
    }

    @Test
    public void test403removeAllAssignments() throws Exception {
        DummyGroup testersGroup = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getGroupByName(GROUP_DUMMY_TESTERS_NAME);
        testersGroup.removeMember(USER_HERMAN_USERNAME);

        DummyAccount hermanAccount = getDummyAccount(RESOURCE_DUMMY_AUTOGREEN_NAME, USER_HERMAN_USERNAME);
        hermanAccount.removeAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, Arrays.asList("graphic", "cratic"));
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignRole(userHermanOid, ROLE_ADMINS_OID);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(userHermanOid);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignments(userAfter, 1);
    }


    @Test
    public void test404importAssociationAutotesters() throws Exception {
        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_AUTOGREEN_OID, AssignmentPolicyEnforcementType.FULL, true);

        DummyGroup craticGroup = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getGroupByName(GROUP_DUMMY_CRATIC_NAME);
        craticGroup.removeMember(USER_HERMAN_USERNAME);

        DummyGroup testersGroup = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getGroupByName(GROUP_DUMMY_TESTERS_NAME);
        testersGroup.addMember(USER_HERMAN_USERNAME);

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modelService.importFromResource(RESOURCE_DUMMY_AUTOGREEN_OID, new QName(MidPointConstants.NS_RI, "AccountObjectClass"), task, result);

        // THEN
        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        waitForTaskFinish(task, true, 40000);

        // THEN
        then();
        assertSuccess(task.getResult());

        PrismObject<UserType> userAfter = getUser(userHermanOid);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_AUTOTESTERS_OID);
        assertAssignments(userAfter, 1);
    }

    @Test
    public void test405assignRoleAutocraticDirectly() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(userHermanOid, ROLE_AUTOCRATIC_OID);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(userHermanOid);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_AUTOTESTERS_OID);
        assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignments(userAfter, 2);

        assertDummyGroupMember(RESOURCE_DUMMY_AUTOGREEN_NAME, GROUP_DUMMY_CRATIC_NAME, USER_HERMAN_USERNAME);
        assertDummyGroupMember(RESOURCE_DUMMY_AUTOGREEN_NAME, GROUP_DUMMY_TESTERS_NAME, USER_HERMAN_USERNAME);
    }

    @Test
    public void test406unassignRoleAutocraticDirectly() throws Exception {
        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_AUTOGREEN_OID, AssignmentPolicyEnforcementType.FULL, true);

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignRole(userHermanOid, ROLE_AUTOCRATIC_OID);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(userHermanOid);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_AUTOTESTERS_OID);
        assertNotAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignments(userAfter, 1);

        assertNoDummyGroupMember(RESOURCE_DUMMY_AUTOGREEN_NAME, GROUP_DUMMY_CRATIC_NAME, USER_HERMAN_USERNAME);
        assertDummyGroupMember(RESOURCE_DUMMY_AUTOGREEN_NAME, GROUP_DUMMY_TESTERS_NAME, USER_HERMAN_USERNAME);
    }

    @Test
    public void test407addHermanToTestersReconcile() throws Exception {
        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_AUTOGREEN_OID, AssignmentPolicyEnforcementType.FULL, true);

        DummyGroup craticGroup = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getGroupByName(GROUP_DUMMY_CRATIC_NAME);
        craticGroup.addMember(USER_HERMAN_USERNAME);

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        reconcileUser(userHermanOid, task, result);

        // THEN
        then();
        assertSuccess(task.getResult());

        PrismObject<UserType> userAfter = getUser(userHermanOid);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_AUTOTESTERS_OID);
        assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignments(userAfter, 2);

        assertDummyGroupMember(RESOURCE_DUMMY_AUTOGREEN_NAME, GROUP_DUMMY_TESTERS_NAME, USER_HERMAN_USERNAME);
        assertDummyGroupMember(RESOURCE_DUMMY_AUTOGREEN_NAME, GROUP_DUMMY_CRATIC_NAME, USER_HERMAN_USERNAME);
    }

}
