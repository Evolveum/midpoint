/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.intest.rbac;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyExceptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSegregationOfDuties extends AbstractInitializedModelIntegrationTest {
	
	protected static final File TEST_DIR = new File("src/test/resources", "rbac");
	
	// Gold, silver and bronze: mutual exclusion (prune), directly in the roles
	
	protected static final File ROLE_PRIZE_GOLD_FILE = new File(TEST_DIR, "role-prize-gold.xml");
	protected static final String ROLE_PRIZE_GOLD_OID = "bbc22f82-df21-11e6-aa6b-4b1408befd10";
	protected static final String ROLE_PRIZE_GOLD_SHIP = "Gold";
	
	protected static final File ROLE_PRIZE_SILVER_FILE = new File(TEST_DIR, "role-prize-silver.xml");
	protected static final String ROLE_PRIZE_SILVER_OID = "dfb5fffe-df21-11e6-bb4f-ef02bdbc9d71";
	protected static final String ROLE_PRIZE_SILVER_SHIP = "Silver";
	
	protected static final File ROLE_PRIZE_BRONZE_FILE = new File(TEST_DIR, "role-prize-bronze.xml");
	protected static final String ROLE_PRIZE_BRONZE_OID = "19f11686-df22-11e6-b0e9-835ed7ca08a5";
	protected static final String ROLE_PRIZE_BRONZE_SHIP = "Bronze";
	
	// Red, green and blue: mutual exclusion (prune) in the metarole

	protected static final File ROLE_META_COLOR_FILE = new File(TEST_DIR, "role-meta-color.xml");
	protected static final String ROLE_META_COLOR_OID = "0b759ce2-df29-11e6-a84c-9b213183a815";
	
	protected static final File ROLE_COLOR_RED_FILE = new File(TEST_DIR, "role-color-red.xml");
	protected static final String ROLE_COLOR_RED_OID = "eaa4ec3e-df28-11e6-9cca-336e0346d5cc";
	protected static final String ROLE_COLOR_RED_SHIP = "Red";
	
	protected static final File ROLE_COLOR_GREEN_FILE = new File(TEST_DIR, "role-color-green.xml");
	protected static final String ROLE_COLOR_GREEN_OID = "2fd9e8f4-df29-11e6-9605-cfcedd703b9e";
	protected static final String ROLE_COLOR_GREEN_SHIP = "Green";
	
	protected static final File ROLE_COLOR_BLUE_FILE = new File(TEST_DIR, "role-color-blue.xml");
	protected static final String ROLE_COLOR_BLUE_OID = "553e8df2-df29-11e6-a7ca-cb7c1f38d89f";
	protected static final String ROLE_COLOR_BLUE_SHIP = "Blue";
	
	protected static final File ROLE_COLOR_NONE_FILE = new File(TEST_DIR, "role-color-none.xml");
	protected static final String ROLE_COLOR_NONE_OID = "662a997e-df2b-11e6-9bb3-5f235d1a8e60";
	
	// Executive / controlling exclusion roles
	
	protected static final File ROLE_META_EXECUTIVE_FILE = new File(TEST_DIR, "role-meta-executive.xml");
	protected static final String ROLE_META_EXECUTIVE_OID = "d20aefe6-3ecf-11e7-8068-5f346db1ee00";
	
	protected static final File ROLE_EXECUTIVE_1_FILE = new File(TEST_DIR, "role-executive-1.xml");
	protected static final String ROLE_EXECUTIVE_1_OID = "d20aefe6-3ecf-11e7-8068-5f346db1ee01";
	
	protected static final File ROLE_EXECUTIVE_2_FILE = new File(TEST_DIR, "role-executive-2.xml");
	protected static final String ROLE_EXECUTIVE_2_OID = "d20aefe6-3ecf-11e7-8068-5f346db1ee02";
	
	protected static final File ROLE_META_CONTROLLING_FILE = new File(TEST_DIR, "role-meta-controlling.xml");
	protected static final String ROLE_META_CONTROLLING_OID = "d20aefe6-3ecf-11e7-8068-5f346db1cc00";
	
	protected static final File ROLE_CONTROLLING_1_FILE = new File(TEST_DIR, "role-controlling-1.xml");
	protected static final String ROLE_CONTROLLING_1_OID = "d20aefe6-3ecf-11e7-8068-5f346db1cc01";
	
	protected static final File ROLE_CONTROLLING_2_FILE = new File(TEST_DIR, "role-controlling-2.xml");
	protected static final String ROLE_CONTROLLING_2_OID = "d20aefe6-3ecf-11e7-8068-5f346db1cc02";
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		repoAddObjectFromFile(ROLE_PRIZE_GOLD_FILE, initResult);
		repoAddObjectFromFile(ROLE_PRIZE_SILVER_FILE, initResult);
		repoAddObjectFromFile(ROLE_PRIZE_BRONZE_FILE, initResult);
		
		repoAddObjectFromFile(ROLE_META_COLOR_FILE, initResult);
		repoAddObjectFromFile(ROLE_COLOR_RED_FILE, initResult);
		repoAddObjectFromFile(ROLE_COLOR_GREEN_FILE, initResult);
		repoAddObjectFromFile(ROLE_COLOR_BLUE_FILE, initResult);
		repoAddObjectFromFile(ROLE_COLOR_NONE_FILE, initResult);
		
		repoAddObjectFromFile(ROLE_META_EXECUTIVE_FILE, initResult);
		repoAddObjectFromFile(ROLE_EXECUTIVE_1_FILE, initResult);
		repoAddObjectFromFile(ROLE_EXECUTIVE_2_FILE, initResult);
		repoAddObjectFromFile(ROLE_META_CONTROLLING_FILE, initResult);
		repoAddObjectFromFile(ROLE_CONTROLLING_1_FILE, initResult);
		repoAddObjectFromFile(ROLE_CONTROLLING_2_FILE, initResult);
		
	}
		
	@Test
    public void test110SimpleExclusion1() throws Exception {
		final String TEST_NAME = "test110SimpleExclusion1";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // This should go well
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        assertSuccess(result);
        
        try {
	        // This should die
	        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
	        
	        fail("Expected policy violation after adding judge role, but it went well");
        } catch (PolicyViolationException e) {
        	// This is expected
        	result.computeStatus();
        	assertFailure(result);
        }
        
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
	}
	
	@Test
    public void test112SimpleExclusion1Deprecated() throws Exception {
		final String TEST_NAME = "test112SimpleExclusion1Deprecated";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // This should go well
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        try {
	        // This should die
	        assignRole(USER_JACK_OID, ROLE_JUDGE_DEPRECATED_OID, task, result);
	        
	        AssertJUnit.fail("Expected policy violation after adding judge role, but it went well");
        } catch (PolicyViolationException e) {
        	// This is expected
        }
        
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
	}
	
	/**
	 * Same thing as before but other way around 
	 */
	@Test
    public void test120SimpleExclusion2() throws Exception {
		final String TEST_NAME = "test120SimpleExclusion2";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // This should go well
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        
        try {
	        // This should die
	        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
	        
	        AssertJUnit.fail("Expected policy violation after adding pirate role, but it went well");
        } catch (PolicyViolationException e) {
        	// This is expected
        }
        
        unassignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        assertAssignedNoRole(USER_JACK_OID, task, result);
	}
	
	/**
	 * Same thing as before but other way around 
	 */
	@Test
    public void test122SimpleExclusion2Deprecated() throws Exception {
		final String TEST_NAME = "test122SimpleExclusion2Deprecated";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // This should go well
        assignRole(USER_JACK_OID, ROLE_JUDGE_DEPRECATED_OID, task, result);
        
        try {
	        // This should die
	        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
	        
	        AssertJUnit.fail("Expected policy violation after adding pirate role, but it went well");
        } catch (PolicyViolationException e) {
        	// This is expected
        }
        
        unassignRole(USER_JACK_OID, ROLE_JUDGE_DEPRECATED_OID, task, result);
        assertAssignedNoRole(USER_JACK_OID, task, result);
	}
	
	@Test
    public void test130SimpleExclusionBoth1() throws Exception {
		final String TEST_NAME = "test130SimpleExclusionBoth1";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add((createAssignmentModification(ROLE_JUDGE_OID, RoleType.COMPLEX_TYPE, null, null, null, true)));
		modifications.add((createAssignmentModification(ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, true)));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        
        
        try {
        	modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
	        
	        AssertJUnit.fail("Expected policy violation, but it went well");
        } catch (PolicyViolationException e) {
        	// This is expected
        }
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
	}
	
	@Test
    public void test132SimpleExclusionBoth1Deprecated() throws Exception {
		final String TEST_NAME = "test132SimpleExclusionBoth1Deprecated";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add((createAssignmentModification(ROLE_JUDGE_DEPRECATED_OID, RoleType.COMPLEX_TYPE, null, null, null, true)));
		modifications.add((createAssignmentModification(ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, true)));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        
        
        try {
        	modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
	        
	        AssertJUnit.fail("Expected policy violation, but it went well");
        } catch (PolicyViolationException e) {
        	// This is expected
        }
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
	}
	
	@Test
    public void test140SimpleExclusionBoth2() throws Exception {
		final String TEST_NAME = "test140SimpleExclusionBoth2";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add((createAssignmentModification(ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, true)));
		modifications.add((createAssignmentModification(ROLE_JUDGE_OID, RoleType.COMPLEX_TYPE, null, null, null, true)));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        
        
        try {
        	modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
	        
	        AssertJUnit.fail("Expected policy violation, but it went well");
        } catch (PolicyViolationException e) {
        	// This is expected
        }
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
	}
	
	@Test
    public void test142SimpleExclusionBoth2Deprecated() throws Exception {
		final String TEST_NAME = "test142SimpleExclusionBoth2Deprecated";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add((createAssignmentModification(ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, true)));
		modifications.add((createAssignmentModification(ROLE_JUDGE_DEPRECATED_OID, RoleType.COMPLEX_TYPE, null, null, null, true)));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        
        
        try {
        	modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
	        
	        AssertJUnit.fail("Expected policy violation, but it went well");
        } catch (PolicyViolationException e) {
        	// This is expected
        }
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
	}
	
	@Test
    public void test150SimpleExclusionBothBidirectional1() throws Exception {
		final String TEST_NAME = "test150SimpleExclusionBothBidirectional1";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add((createAssignmentModification(ROLE_THIEF_OID, RoleType.COMPLEX_TYPE, null, null, null, true)));
		modifications.add((createAssignmentModification(ROLE_JUDGE_OID, RoleType.COMPLEX_TYPE, null, null, null, true)));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        
        try {
        	modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
	        
	        AssertJUnit.fail("Expected policy violation, but it went well");
        } catch (PolicyViolationException e) {
        	// This is expected
        }
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
	}
	
	@Test
    public void test160SimpleExclusionBothBidirectional2() throws Exception {
		final String TEST_NAME = "test160SimpleExclusionBothBidirectional2";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		modifications.add((createAssignmentModification(ROLE_JUDGE_OID, RoleType.COMPLEX_TYPE, null, null, null, true)));
		modifications.add((createAssignmentModification(ROLE_THIEF_OID, RoleType.COMPLEX_TYPE, null, null, null, true)));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
        
        try {
        	modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);
	        
	        AssertJUnit.fail("Expected policy violation, but it went well");
        } catch (PolicyViolationException e) {
        	// This is expected
        }
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
	}
	
	@Test
    public void test171SimpleExclusion1WithPolicyException() throws Exception {
		final String TEST_NAME = "test171SimpleExclusion1WithPolicyException";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, null, getJudgeExceptionBlock(), task, result);
        
        PrismObject<UserType> userJackIn = getUser(USER_JACK_OID);
        assertAssignedRoles(userJackIn, ROLE_JUDGE_OID, ROLE_PIRATE_OID);
        
        unassignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, null, getJudgeExceptionBlock(), task, result);
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
	}
	
	@Test
    public void test172SimpleExclusion2WithPolicyException() throws Exception {
		final String TEST_NAME = "test172SimpleExclusion2WithPolicyException";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, null, getJudgeExceptionBlock(), task, result);

        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        
        PrismObject<UserType> userJackIn = getUser(USER_JACK_OID);
        assertAssignedRoles(userJackIn, ROLE_JUDGE_OID, ROLE_PIRATE_OID);
                
        unassignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, null, getJudgeExceptionBlock(), task, result);
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
	}
	
	@Test
    public void test173SimpleExclusion3WithPolicyException() throws Exception {
		final String TEST_NAME = "test173SimpleExclusion3WithPolicyException";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, null, getJudgeExceptionBlock(), task, result);
        
        PrismObject<UserType> userJackIn = getUser(USER_JACK_OID);
        assertAssignedRoles(userJackIn, ROLE_JUDGE_OID, ROLE_PIRATE_OID);
                
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        unassignRole(USER_JACK_OID, ROLE_JUDGE_OID, null, getJudgeExceptionBlock(), task, result);
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
	}

	@Test
    public void test174SimpleExclusion4WithPolicyException() throws Exception {
		final String TEST_NAME = "test174SimpleExclusion4WithPolicyException";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, null, getJudgeExceptionBlock(), task, result);

        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        PrismObject<UserType> userJackIn = getUser(USER_JACK_OID);
        assertAssignedRoles(userJackIn, ROLE_JUDGE_OID, ROLE_PIRATE_OID);
                
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        unassignRole(USER_JACK_OID, ROLE_JUDGE_OID, null, getJudgeExceptionBlock(), task, result);
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
	}
	
	/**
	 * Add pirate role to judge. But include policy exception in the pirate assignment, so it
	 * should go OK. The assign thief (without exception). The exception in the pirate assignment
	 * should only apply to that assignment. The assignment of thief should fail.
	 */
	@Test
    public void test180JudgeExceptionalPirateAndThief() throws Exception {
		final String TEST_NAME = "test180JudgeExceptionalPirateAndThief";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, null, getJudgeExceptionBlock(), task, result);
        
        PrismObject<UserType> userJackIn = getUser(USER_JACK_OID);
        assertAssignedRoles(userJackIn, ROLE_JUDGE_OID, ROLE_PIRATE_OID);
        
        try {
	        // This should die
	        assignRole(USER_JACK_OID, ROLE_THIEF_OID, task, result);
	        
	        AssertJUnit.fail("Expected policy violation after adding thief role, but it went well");
        } catch (PolicyViolationException e) {
        	// This is expected
        }
        
        // Cleanup
        
        unassignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, null, getJudgeExceptionBlock(), task, result);
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
	}

	Consumer<AssignmentType> getJudgeExceptionBlock() {
		return assignment -> {
			PolicyExceptionType policyException = new PolicyExceptionType();
			policyException.setRuleName(ROLE_JUDGE_POLICY_RULE_EXCLUSION_NAME);
			assignment.getPolicyException().add(policyException);
		};
	}
		
	/**
	 * MID-3685
	 */
	@Test
    public void test200GuybrushAssignRoleGold() throws Exception {
		final String TEST_NAME = "test200GuybrushAssignRoleGold";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_PRIZE_GOLD_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_PRIZE_GOLD_OID);
        assertNotAssignedRole(userAfter, ROLE_PRIZE_SILVER_OID);
        assertNotAssignedRole(userAfter, ROLE_PRIZE_BRONZE_OID);
        
        assertDummyAccount(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, ROLE_PRIZE_GOLD_SHIP);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK);
	}

	/**
	 * MID-3685
	 */
	@Test
    public void test202GuybrushAssignRoleSilver() throws Exception {
		final String TEST_NAME = "test202GuybrushAssignRoleSilver";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_PRIZE_SILVER_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertNotAssignedRole(userAfter, ROLE_PRIZE_GOLD_OID);
        assertAssignedRole(userAfter, ROLE_PRIZE_SILVER_OID);
        assertNotAssignedRole(userAfter, ROLE_PRIZE_BRONZE_OID);
        
        assertDummyAccount(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, ROLE_PRIZE_SILVER_SHIP);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK);
	}
	
	/**
	 * Mix in ordinary role to check for interferences.
	 * MID-3685
	 */
	@Test
    public void test204GuybrushAssignRoleSailor() throws Exception {
		final String TEST_NAME = "test204GuybrushAssignRoleSailor";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_SAILOR_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertNotAssignedRole(userAfter, ROLE_PRIZE_GOLD_OID);
        assertAssignedRole(userAfter, ROLE_PRIZE_SILVER_OID);
        assertNotAssignedRole(userAfter, ROLE_PRIZE_BRONZE_OID);
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);
        
        assertDummyAccount(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, ROLE_PRIZE_SILVER_SHIP);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
	}
	
	
	/**
	 * MID-3685
	 */
	@Test
    public void test206GuybrushAssignRoleBronze() throws Exception {
		final String TEST_NAME = "test206GuybrushAssignRoleBronze";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_PRIZE_BRONZE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertNotAssignedRole(userAfter, ROLE_PRIZE_GOLD_OID);
        assertNotAssignedRole(userAfter, ROLE_PRIZE_SILVER_OID);
        assertAssignedRole(userAfter, ROLE_PRIZE_BRONZE_OID);
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);
        
        assertDummyAccount(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, ROLE_PRIZE_BRONZE_SHIP);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
	}
	
	/**
	 * MID-3685
	 */
	@Test
    public void test208GuybrushUnassignRoleBronze() throws Exception {
		final String TEST_NAME = "test209GuybrushUnassignRoleSilver";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(USER_GUYBRUSH_OID, ROLE_PRIZE_BRONZE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertNotAssignedRole(userAfter, ROLE_PRIZE_GOLD_OID);
        assertNotAssignedRole(userAfter, ROLE_PRIZE_SILVER_OID);
        assertNotAssignedRole(userAfter, ROLE_PRIZE_BRONZE_OID);
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);

        assertDummyAccount(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
	}

	/**
	 * MID-3685
	 */
	@Test
    public void test209GuybrushUnassignRoleSailor() throws Exception {
		final String TEST_NAME = "test209GuybrushUnassignRoleSailor";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(USER_GUYBRUSH_OID, ROLE_SAILOR_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignedNoRole(userAfter);

        assertNoDummyAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
	}

	/**
	 * MID-3685
	 */
	@Test
    public void test210GuybrushAssignRoleRed() throws Exception {
		final String TEST_NAME = "test210GuybrushAssignRoleRed";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_COLOR_RED_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_COLOR_RED_OID);
        assertNotAssignedRole(userAfter, ROLE_COLOR_GREEN_OID);
        assertNotAssignedRole(userAfter, ROLE_COLOR_BLUE_OID);
        assertNotAssignedRole(userAfter, ROLE_COLOR_NONE_OID);
        
        assertDummyAccount(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, ROLE_COLOR_RED_SHIP);
	}
	
	/**
	 * MID-3685
	 */
	@Test
    public void test212GuybrushAssignRoleGreen() throws Exception {
		final String TEST_NAME = "test212GuybrushAssignRoleGreen";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_COLOR_GREEN_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertNotAssignedRole(userAfter, ROLE_COLOR_RED_OID);
        assertAssignedRole(userAfter, ROLE_COLOR_GREEN_OID);
        assertNotAssignedRole(userAfter, ROLE_COLOR_BLUE_OID);
        assertNotAssignedRole(userAfter, ROLE_COLOR_NONE_OID);
        
        assertDummyAccount(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, ROLE_COLOR_GREEN_SHIP);
	}
	
	/**
	 * MID-3685
	 */
	@Test
    public void test214GuybrushAssignRoleColorNone() throws Exception {
		final String TEST_NAME = "test214GuybrushAssignRoleColorNone";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_COLOR_NONE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertNotAssignedRole(userAfter, ROLE_COLOR_RED_OID);
        assertNotAssignedRole(userAfter, ROLE_COLOR_GREEN_OID);
        assertNotAssignedRole(userAfter, ROLE_COLOR_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_COLOR_NONE_OID);
        
        assertNoDummyAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
	}
	
	/**
	 * MID-3685
	 */
	@Test
    public void test216GuybrushAssignRoleBlue() throws Exception {
		final String TEST_NAME = "test216GuybrushAssignRoleBlue";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_COLOR_BLUE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertNotAssignedRole(userAfter, ROLE_COLOR_RED_OID);
        assertNotAssignedRole(userAfter, ROLE_COLOR_GREEN_OID);
        assertAssignedRole(userAfter, ROLE_COLOR_BLUE_OID);
        assertNotAssignedRole(userAfter, ROLE_COLOR_NONE_OID);
        
        assertDummyAccount(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, ROLE_COLOR_BLUE_SHIP);
	}
	
	
	/**
	 * MID-3694
	 */
	@Test
    public void test220GuybrushAssignRoleExecutiveOne() throws Exception {
		final String TEST_NAME = "test220GuybrushAssignRoleExecutiveOne";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_EXECUTIVE_1_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_EXECUTIVE_1_OID);        
	}
	
	/**
	 * MID-3694
	 */
	@Test
    public void test222GuybrushAssignRoleControllingOne() throws Exception {
		final String TEST_NAME = "test222GuybrushAssignRoleControllingOne";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        try {
	        // WHEN
        	
	        displayWhen(TEST_NAME);
	        assignRole(USER_GUYBRUSH_OID, ROLE_CONTROLLING_1_OID, task, result);
        
	        assertNotReached();
	        
        } catch (PolicyViolationException e) {
        	
        	// THEN
        	displayThen(TEST_NAME);
        	assertFailure(result);
        }
	                
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_EXECUTIVE_1_OID);   
        assertNotAssignedRole(userAfter, ROLE_CONTROLLING_1_OID);
	}
	
	/**
	 * MID-3694
	 */
	@Test
    public void test224GuybrushAssignRoleExecutiveTwo() throws Exception {
		final String TEST_NAME = "test224GuybrushAssignRoleExecutiveTwo";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_EXECUTIVE_2_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_EXECUTIVE_1_OID);
        assertAssignedRole(userAfter, ROLE_EXECUTIVE_2_OID);
	}
	
	/**
	 * MID-3694
	 */
	@Test
    public void test225GuybrushAssignRoleControllingTwo() throws Exception {
		final String TEST_NAME = "test225GuybrushAssignRoleControllingTwo";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        try {
	        // WHEN
        	
	        displayWhen(TEST_NAME);
	        assignRole(USER_GUYBRUSH_OID, ROLE_CONTROLLING_2_OID, task, result);
        
	        assertNotReached();
	        
        } catch (PolicyViolationException e) {
        	
        	// THEN
        	displayThen(TEST_NAME);
        	assertFailure(result);
        }
	                
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_EXECUTIVE_1_OID);
        assertAssignedRole(userAfter, ROLE_EXECUTIVE_2_OID);
        assertNotAssignedRole(userAfter, ROLE_CONTROLLING_1_OID);
        assertNotAssignedRole(userAfter, ROLE_CONTROLLING_2_OID);
	}

	/**
	 * MID-3694
	 */
	@Test
    public void test226GuybrushUnassignRoleExecutiveOne() throws Exception {
		final String TEST_NAME = "test226GuybrushUnassignRoleExecutiveOne";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_GUYBRUSH_OID, ROLE_EXECUTIVE_1_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_EXECUTIVE_2_OID);        
	}
	
	/**
	 * MID-3694
	 */
	@Test
    public void test227GuybrushAssignRoleControllingOne() throws Exception {
		final String TEST_NAME = "test227GuybrushAssignRoleControllingOne";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        try {
	        // WHEN
        	
	        displayWhen(TEST_NAME);
	        assignRole(USER_GUYBRUSH_OID, ROLE_CONTROLLING_1_OID, task, result);
        
	        assertNotReached();
	        
        } catch (PolicyViolationException e) {
        	
        	// THEN
        	displayThen(TEST_NAME);
        	assertFailure(result);
        }
	                
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertNotAssignedRole(userAfter, ROLE_EXECUTIVE_1_OID);
        assertAssignedRole(userAfter, ROLE_EXECUTIVE_2_OID);
        assertNotAssignedRole(userAfter, ROLE_CONTROLLING_1_OID);
        assertNotAssignedRole(userAfter, ROLE_CONTROLLING_2_OID);
	}
	
	/**
	 * MID-3694
	 */
	@Test
    public void test229GuybrushUnassignRoleExecutiveTwo() throws Exception {
		final String TEST_NAME = "test229GuybrushUnassignRoleExecutiveTwo";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_GUYBRUSH_OID, ROLE_EXECUTIVE_2_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertNotAssignedRole(userAfter, ROLE_EXECUTIVE_1_OID);
        assertNotAssignedRole(userAfter, ROLE_EXECUTIVE_2_OID);
        assertNotAssignedRole(userAfter, ROLE_CONTROLLING_1_OID);
        assertNotAssignedRole(userAfter, ROLE_CONTROLLING_2_OID);
	}
	
	/**
	 * MID-3694
	 */
	@Test
    public void test230GuybrushAssignRoleControllingOne() throws Exception {
		final String TEST_NAME = "test230GuybrushAssignRoleControllingOne";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_CONTROLLING_1_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_CONTROLLING_1_OID);        
	}
	
	/**
	 * MID-3694
	 */
	@Test
    public void test232GuybrushAssignRoleExecutiveOne() throws Exception {
		final String TEST_NAME = "test232GuybrushAssignRoleExecutiveOne";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        try {
	        // WHEN
        	
	        displayWhen(TEST_NAME);
	        assignRole(USER_GUYBRUSH_OID, ROLE_EXECUTIVE_1_OID, task, result);
        
	        assertNotReached();
	        
        } catch (PolicyViolationException e) {
        	
        	// THEN
        	displayThen(TEST_NAME);
        	assertFailure(result);
        }
	                
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_CONTROLLING_1_OID);   
        assertNotAssignedRole(userAfter, ROLE_EXECUTIVE_1_OID);
	}
	
	/**
	 * MID-3694
	 */
	@Test
    public void test239GuybrushUnassignRoleControllingOne() throws Exception {
		final String TEST_NAME = "test239GuybrushUnassignRoleControllingOne";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_GUYBRUSH_OID, ROLE_CONTROLLING_1_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertNotAssignedRole(userAfter, ROLE_EXECUTIVE_1_OID);
        assertNotAssignedRole(userAfter, ROLE_EXECUTIVE_2_OID);
        assertNotAssignedRole(userAfter, ROLE_CONTROLLING_1_OID);
        assertNotAssignedRole(userAfter, ROLE_CONTROLLING_2_OID);
	}
}
