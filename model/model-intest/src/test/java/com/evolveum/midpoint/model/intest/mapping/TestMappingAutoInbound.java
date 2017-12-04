/*
 * Copyright (c) 2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.intest.mapping;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectionPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

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
		final String TEST_NAME = "test100ImportFromResource";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        DummyAccount accountHerman = new DummyAccount(USER_HERMAN_USERNAME);
        accountHerman.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, USER_HERMAN_FULL_NAME);
        accountHerman.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "matic");
		getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).addAccount(accountHerman);

        // Preconditions
        assertUsers(getNumberOfUsers());

		// WHEN
        displayWhen(TEST_NAME);
        modelService.importFromResource(RESOURCE_DUMMY_AUTOGREEN_OID, new QName(MidPointConstants.NS_RI, "AccountObjectClass"), task, result);

        // THEN
        displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        waitForTaskFinish(task, true, 40000);

        // THEN
        displayThen(TEST_NAME);
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
		final String TEST_NAME = "test110ModifyAccountTitleCraticAndReconcile";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        DummyAccount accountHerman = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getAccountByUsername(USER_HERMAN_USERNAME);
        accountHerman.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "cratic");

		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(userHermanOid, task, result);

        // THEN
        displayThen(TEST_NAME);
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
		final String TEST_NAME = "test112ModifyAccountTitleDidacticGraphicAndReconcile";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        DummyAccount accountHerman = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getAccountByUsername(USER_HERMAN_USERNAME);
        accountHerman.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "didactic", "graphic");

		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(userHermanOid, task, result);

        // THEN
        displayThen(TEST_NAME);
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
		final String TEST_NAME = "test200ImportFromResourceAssociations";
        displayTestTitle(TEST_NAME);

        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_AUTOGREEN_OID, AssignmentPolicyEnforcementType.RELATIVE, false);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        DummyGroup dummyGroup = new DummyGroup(GROUP_DUMMY_TESTERS_NAME);
        getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).addGroup(dummyGroup);
        dummyGroup.addMember(USER_HERMAN_USERNAME);
        
        dummyGroup = new DummyGroup(GROUP_DUMMY_CRATIC_NAME);
        getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).addGroup(dummyGroup);
        
        dummyGroup.addMember(USER_HERMAN_USERNAME);
		
		// WHEN
        displayWhen(TEST_NAME);
        modelService.importFromResource(RESOURCE_DUMMY_AUTOGREEN_OID, new QName(MidPointConstants.NS_RI, "AccountObjectClass"), task, result);

        // THEN
        displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        waitForTaskFinish(task, true, 40000);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(task.getResult());

        SearchResultList<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);

        PrismObject<UserType> userHermanAfter = findUserByUsername(USER_HERMAN_USERNAME);
        display("User after", userHermanAfter);
        userHermanOid = userHermanAfter.getOid();
        assertUser(userHermanAfter, userHermanAfter.getOid(), USER_HERMAN_USERNAME, USER_HERMAN_FULL_NAME, null, null);
        assertAssignedRole(userHermanAfter, ROLE_AUTODIDACTIC_OID);
        assertAssignedRole(userHermanAfter, ROLE_AUTOGRAPHIC_OID);
        assertAssignedRole(userHermanAfter, ROLE_AUTOTESTERS_OID);
        assertAssignedRole(userHermanAfter, ROLE_AUTOCRATIC_OID);
        assertAssignments(userHermanAfter, 4);

        assertEquals("Unexpected number of users", getNumberOfUsers() + 1, users.size());
	}
    
    @Test
    public void test300ModifyAccountDirectAssign() throws Exception {
		final String TEST_NAME = "test300ModifyAccountDirectAssign";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(userHermanOid, ROLE_ADMINS_OID);
        reconcileUser(userHermanOid, task, result);

        // THEN
        displayThen(TEST_NAME);
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
		final String TEST_NAME = "test301removeUserFromAutoGroup";
        displayTestTitle(TEST_NAME);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        DummyGroup craticGroup = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getGroupByName(GROUP_DUMMY_CRATIC_NAME);
        craticGroup.removeMember(USER_HERMAN_USERNAME);
        
        DummyAccount hermanAccount = getDummyAccount(RESOURCE_DUMMY_AUTOGREEN_NAME, USER_HERMAN_USERNAME);
        hermanAccount.removeAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, Arrays.asList("didactic"));
        
        assertNoDummyGroupMember(RESOURCE_DUMMY_AUTOGREEN_NAME, GROUP_DUMMY_CRATIC_NAME, USER_HERMAN_USERNAME);
        
        
        // WHEN
        displayWhen(TEST_NAME);
        modelService.importFromResource(RESOURCE_DUMMY_AUTOGREEN_OID, new QName(MidPointConstants.NS_RI, "AccountObjectClass"), task, result);

        // THEN
        displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        waitForTaskFinish(task, true, 70000);
        
        // THEN
        displayThen(TEST_NAME);
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
		final String TEST_NAME = "test402assignAutoGroupDirectly";
        displayTestTitle(TEST_NAME);

        DummyGroup craticGroup = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getGroupByName(GROUP_DUMMY_CRATIC_NAME);
        craticGroup.removeMember(USER_HERMAN_USERNAME);
        
        DummyGroup testersGroup = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getGroupByName(GROUP_DUMMY_TESTERS_NAME);
        testersGroup.addMember(USER_HERMAN_USERNAME);
        
        assertDummyGroupMember(RESOURCE_DUMMY_AUTOGREEN_NAME, GROUP_DUMMY_TESTERS_NAME, USER_HERMAN_USERNAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(userHermanOid, ROLE_AUTOCRATIC_OID);

        // THEN
        displayThen(TEST_NAME);
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
		final String TEST_NAME = "test403removeAllAssignments";
        displayTestTitle(TEST_NAME);

        DummyGroup testersGroup = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getGroupByName(GROUP_DUMMY_TESTERS_NAME);
        testersGroup.removeMember(USER_HERMAN_USERNAME);
        
        DummyAccount hermanAccount = getDummyAccount(RESOURCE_DUMMY_AUTOGREEN_NAME, USER_HERMAN_USERNAME);
        hermanAccount.removeAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, Arrays.asList("graphic", "cratic"));
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(userHermanOid, ROLE_ADMINS_OID);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(userHermanOid);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignments(userAfter, 1);
	}
    
    
    @Test
    public void test404importAssociationAutotesters() throws Exception {
		final String TEST_NAME = "test404importAssociationAutotesters";
        displayTestTitle(TEST_NAME);
        
        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_AUTOGREEN_OID, AssignmentPolicyEnforcementType.FULL, true);

        DummyGroup craticGroup = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getGroupByName(GROUP_DUMMY_CRATIC_NAME);
        craticGroup.removeMember(USER_HERMAN_USERNAME);
        
        DummyGroup testersGroup = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getGroupByName(GROUP_DUMMY_TESTERS_NAME);
        testersGroup.addMember(USER_HERMAN_USERNAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        modelService.importFromResource(RESOURCE_DUMMY_AUTOGREEN_OID, new QName(MidPointConstants.NS_RI, "AccountObjectClass"), task, result);

        // THEN
        displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        waitForTaskFinish(task, true, 40000);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(task.getResult());

        PrismObject<UserType> userAfter = getUser(userHermanOid);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_AUTOTESTERS_OID);
        assertAssignments(userAfter, 1);
	}
    
    @Test
    public void test405assignRoleAutocraticDirectly() throws Exception {
		final String TEST_NAME = "test405assignRoleAutocraticDirectly";
        displayTestTitle(TEST_NAME);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(userHermanOid, ROLE_AUTOCRATIC_OID);

        // THEN
        displayThen(TEST_NAME);
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
		final String TEST_NAME = "test406unassignRoleAutocraticAutotestersDirectly";
        displayTestTitle(TEST_NAME);
        
        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_AUTOGREEN_OID, AssignmentPolicyEnforcementType.FULL, true);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(userHermanOid, ROLE_AUTOCRATIC_OID);

        // THEN
        displayThen(TEST_NAME);
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
		final String TEST_NAME = "test407addHermanToTestersReconcile";
        displayTestTitle(TEST_NAME);
        
        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_AUTOGREEN_OID, AssignmentPolicyEnforcementType.FULL, true);

        DummyGroup craticGroup = getDummyResource(RESOURCE_DUMMY_AUTOGREEN_NAME).getGroupByName(GROUP_DUMMY_CRATIC_NAME);
        craticGroup.addMember(USER_HERMAN_USERNAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        reconcileUser(userHermanOid, task, result);
        
        // THEN
        displayThen(TEST_NAME);
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
