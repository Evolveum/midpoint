/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.io.File;
import java.util.Collection;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMultiResource extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/multi");
	
	// YELLOW dummy resource has a STRICT dependency on default dummy resource
	protected static final File RESOURCE_DUMMY_YELLOW_FILE = new File(TEST_DIR, "resource-dummy-yellow.xml");
	protected static final String RESOURCE_DUMMY_YELLOW_OID = "10000000-0000-0000-0000-000000000504";
	protected static final String RESOURCE_DUMMY_YELLOW_NAME = "yellow";
	protected static final String RESOURCE_DUMMY_YELLOW_NAMESPACE = MidPointConstants.NS_RI;
	
	// IVORY dummy resource has a LAX dependency on default dummy resource
	protected static final File RESOURCE_DUMMY_IVORY_FILE = new File(TEST_DIR, "resource-dummy-ivory.xml");
	protected static final String RESOURCE_DUMMY_IVORY_OID = "10000000-0000-0000-0000-000000011504";
	protected static final String RESOURCE_DUMMY_IVORY_NAME = "ivory";
	protected static final String RESOURCE_DUMMY_IVORY_NAMESPACE = MidPointConstants.NS_RI;
	
	// Assigns default dummy resource and red dummy resource
	protected static final File ROLE_DUMMIES_FILE = new File(TEST_DIR, "role-dummies.xml");
	protected static final String ROLE_DUMMIES_OID = "12345678-d34d-b33f-f00d-55555555dddd";
	
	protected static DummyResource dummyResourceYellow;
	protected static DummyResourceContoller dummyResourceCtlYellow;
	protected ResourceType resourceDummyYellowType;
	protected PrismObject<ResourceType> resourceDummyYellow;
	
	protected static DummyResource dummyResourceIvory;
	protected static DummyResourceContoller dummyResourceCtlIvory;
	protected ResourceType resourceDummyIvoryType;
	protected PrismObject<ResourceType> resourceDummyIvory;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		dummyResourceCtlYellow = DummyResourceContoller.create(RESOURCE_DUMMY_YELLOW_NAME, resourceDummyYellow);
		dummyResourceCtlYellow.extendDummySchema();
		dummyResourceYellow = dummyResourceCtlYellow.getDummyResource();
		resourceDummyYellow = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_YELLOW_FILE, RESOURCE_DUMMY_YELLOW_OID, initTask, initResult);
		resourceDummyYellowType = resourceDummyYellow.asObjectable();
		dummyResourceCtlYellow.setResource(resourceDummyYellow);
		
		dummyResourceCtlIvory = DummyResourceContoller.create(RESOURCE_DUMMY_IVORY_NAME, resourceDummyIvory);
		dummyResourceCtlIvory.extendDummySchema();
		dummyResourceIvory = dummyResourceCtlIvory.getDummyResource();
		resourceDummyIvory = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_IVORY_FILE, RESOURCE_DUMMY_IVORY_OID, initTask, initResult);
		resourceDummyIvoryType = resourceDummyIvory.asObjectable();
		dummyResourceCtlIvory.setResource(resourceDummyIvory);
		
		repoAddObjectFromFile(ROLE_DUMMIES_FILE, RoleType.class, initResult);
	}

	@Test
    public void test110JackAssignRoleDummiesFull() throws Exception {
		final String TEST_NAME = "test110JackAssignRoleDummiesFull";
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		jackAssignRoleDummies(TEST_NAME);
	}
	
	@Test
    public void test119JackUnAssignRoleDummiesFull() throws Exception {
		final String TEST_NAME = "test119JackUnAssignRoleDummiesFull";
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
		jackUnAssignRoleDummies(TEST_NAME);
	}
	
	@Test
    public void test120JackAssignRoleDummiesRelative() throws Exception {
		final String TEST_NAME = "test120JackAssignRoleDummiesRelative";
		
		// Clean up user
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
		modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		jackAssignRoleDummies(TEST_NAME);
	}
	
	@Test
    public void test129JackUnAssignRoleDummiesRelative() throws Exception {
		final String TEST_NAME = "test129JackUnAssignRoleDummiesRelative";
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		jackUnAssignRoleDummies(TEST_NAME);
	}
	
	/**
	 * Ivory resource has a lax dependency. The provisioning should go OK.
	 */
	@Test
    public void test200JackAssignDummyIvory() throws Exception {
		final String TEST_NAME = "test200JackAssignDummyIvory";
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        // Clean up user
		modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);
		
		// WHEN
		assignAccount(USER_JACK_OID, RESOURCE_DUMMY_IVORY_OID, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertAccounts(userJack, 1);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        
        assertDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        // No value for ship ... no place to get it from
        assertDummyAccountAttribute(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "ship");
	}
	
	/**
	 * Ivory resource has a lax dependency. The provisioning should go OK.
	 */
	@Test
    public void test209JackUnAssignDummyIvory() throws Exception {
		final String TEST_NAME = "test209JackUnAssignDummyIvory";
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        // Clean up user
		modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);
		
		// WHEN
		unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_IVORY_OID, null, task, result);
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertAccounts(userJack, 0);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Yellow resource has a lax dependency. The provisioning should go fail.
	 */
	@Test
    public void test210JackAssignDummyYellow() throws Exception {
		final String TEST_NAME = "test210JackAssignDummyYellow";
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        // Clean up user
		modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);
		
		try {
			// WHEN
			assignAccount(USER_JACK_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);
			
			AssertJUnit.fail("Unexpected success");
		} catch (PolicyViolationException e) {
			// this is expected
		}
		
		// THEN
		result.computeStatus();
        IntegrationTestTools.assertFailure(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertAccounts(userJack, 0);
        assertAssignments(userJack, 0);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);       
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * The "dummies" role assigns two dummy resources that are in a dependency. The value of "ship" is propagated from one
	 * resource through the user to the other resource. If dependency does not work then no value is propagated.
	 */
    public void jackAssignRoleDummies(final String TEST_NAME) throws Exception {
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_DUMMIES_OID, task, result);
        
        // THEN
        result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertAssignedRole(USER_JACK_OID, ROLE_DUMMIES_OID, task, result);
        assertAccounts(userJack, 3);
        
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "The Great Voodoo Master");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "ship", "The Lost Souls");
        
        // This is set up by "feedback" using an inbound expression. It has nothing with dependencies yet.
        assertUserProperty(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, PrismTestUtil.createPolyString("The crew of The Lost Souls"));
        
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        // This is set by red's outbound from user's organizationalUnit. If dependencies work this outbound is processed
        // after user's organizationUnit is set and it will have the same value as above.
        assertDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "ship", "The crew of The Lost Souls");
        
        assertDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        // This is set by red's outbound from user's organizationalUnit. If dependencies work this outbound is processed
        // after user's organizationUnit is set and it will have the same value as above.
        assertDummyAccountAttribute(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "ship", "The crew of The Lost Souls");
	}
	
    public void jackUnAssignRoleDummies(final String TEST_NAME) throws Exception {
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        unassignRole(USER_JACK_OID, ROLE_DUMMIES_OID, task, result);
        
        // THEN
        result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignedNoRole(user);
        assertAccounts(user, 0);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        
        assertUserProperty(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, PrismTestUtil.createPolyString("The crew of The Lost Souls"));        
	}
	
    @Test
    public void test300AddAndAssignRelative() throws Exception {
		final String TEST_NAME = "test300AddAndAssignRelative";
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // Add default dummy account to jack without assigning it.
        // In relative mode this account should shay untouched while we play with assignments and
        // unsassignements of other accounts
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
        modelService.executeChanges(deltas, null, task, result);
        result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after default dummy account add", userJack);
		assertUserJack(userJack);
		assertAccount(userJack, RESOURCE_DUMMY_OID);
        assertDummyAccount("jack", "Jack Sparrow", true);
		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
        userJack = getUser(USER_JACK_OID);
		display("User after red dummy assignment", userJack);
		assertUserJack(userJack);
		assertAccount(userJack, RESOURCE_DUMMY_OID);
		assertAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
		assertAccounts(userJack, 2);
        String accountOid = getAccountRef(userJack, RESOURCE_DUMMY_BLUE_OID);
        
        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertShadowRepo(accountShadow, accountOid, "jack", resourceDummyBlueType);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertShadowModel(accountModel, accountOid, "jack", resourceDummyBlueType);
        
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, "jack", "Jack Sparrow", true);
        
	}
    
    @Test
    public void test310AddedAccountAndUnassignRelative() throws Exception {
		final String TEST_NAME = "test310AddedAccountAndUnassignRelative";
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
		
		Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        IntegrationTestTools.assertSuccess(result);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after red dummy unassignment", userJack);
		assertUserJack(userJack);
		assertAccount(userJack, RESOURCE_DUMMY_OID);
		assertAccounts(userJack, 1);
        String accountOid = getAccountRef(userJack, RESOURCE_DUMMY_OID);
        
        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, result);
        assertDummyShadowRepo(accountShadow, accountOid, "jack");
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        
        assertDummyAccount("jack", "Jack Sparrow", true);
        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, "jack");
        
	}
    
}
