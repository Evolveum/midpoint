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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.internals.TestingPaths;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentValidity extends AbstractRbacTest {
		
	private XMLGregorianCalendar jackPirateValidTo;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
//		InternalsConfig.setTestingPaths(TestingPaths.REVERSED);
	}
	
	/**
	 * MID-4110
	 */
	@Test
    public void test100JackAssignRolePirateValidTo() throws Exception {
		final String TEST_NAME = "test100JackAssignRolePirateValidTo";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);
        
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, activationType, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.ENABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummyPirateAccount();
	}

	/**
	 * Assignment expires.
	 * MID-4110, MID-4114
	 */
	@Test
    public void test102Forward15min() throws Exception {
		final String TEST_NAME = "test102Forward15min";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        clockForward("PT15M");
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * New assignment. No time validity.
	 * MID-4110
	 */
	@Test
    public void test104JackAssignRolePirateAgain() throws Exception {
		final String TEST_NAME = "test104JackAssignRolePirateAgain";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummyPirateAccount();
	}
	
	/**
	 * Unassign valid assignment. Only invalid assignment remains.
	 * MID-4110
	 */
	@Test
    public void test106JackUnassignRolePirateValid() throws Exception {
		final String TEST_NAME = "test106JackUnassignRolePirateValid";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * MID-4110
	 */
	@Test
    public void test109JackUnassignAll() throws Exception {
		unassignAll("test109JackUnassignAll");
	}
	
	/**
	 * Raw modification of assignment. The assignment is not effective immediately,
	 * as this is raw operation. So, nothing much happens. Yet.
	 * MID-4110
	 */
	@Test
    public void test110JackAssignRolePirateValidToRaw() throws Exception {
		final String TEST_NAME = "test110JackAssignRolePirateValidToRaw";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);
        
        ModelExecuteOptions options = ModelExecuteOptions.createRaw();
        
        // WHEN
        displayWhen(TEST_NAME);
		modifyUserAssignment(USER_JACK_OID, ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, 
        		task, null, activationType, true, options, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentTypeAfter, null);
        assertRoleMembershipRef(userAfter);
		assertDelegatedRef(userAfter);
		
		assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

	/**
	 * MID-4110, MID-4114
	 */
	@Test
    public void test111RecomputeJack() throws Exception {
		final String TEST_NAME = "test111RecomputeJack";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.ENABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummyPirateAccount();
	}

	/**
	 * Assignment expires.
	 * MID-4110, MID-4114
	 */
	@Test
    public void test112Forward15min() throws Exception {
		final String TEST_NAME = "test102Forward15min";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        clockForward("PT15M");
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * New assignment. No time validity.
	 * MID-4110
	 */
	@Test
    public void test114JackAssignRolePirateAgain() throws Exception {
		final String TEST_NAME = "test114JackAssignRolePirateAgain";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummyPirateAccount();
	}
	
	/**
	 * MID-4110
	 */
	@Test
    public void test119JackUnassignAll() throws Exception {
		unassignAll("test119JackUnassignAll");
	}

	/**
	 * Sailor is an idempotent(conservative) role.
	 * MID-4110
	 */
	@Test
    public void test120JackAssignRoleSailorValidTo() throws Exception {
		final String TEST_NAME = "test120JackAssignRoleSailorValidTo";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);
        
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, activationType, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.ENABLED);
        assertRoleMembershipRef(userAfter, ROLE_STRONG_SAILOR_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummySailorAccount();
	}
	
	/**
	 * Assignment expires.
	 * MID-4110, MID-4114
	 */
	@Test
    public void test122Forward15min() throws Exception {
		final String TEST_NAME = "test122Forward15min";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        clockForward("PT15M");
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * New assignment. No time validity.
	 * MID-4110
	 */
	@Test
    public void test124JackAssignRoleSailorAgain() throws Exception {
		final String TEST_NAME = "test124JackAssignRoleSailorAgain";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_STRONG_SAILOR_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummySailorAccount();
	}
	
	/**
	 * MID-4110
	 */
	@Test
    public void test129JackUnassignAll() throws Exception {
		unassignAll("test129JackUnassignAll");
	}
	
	/**
	 * Raw modification of assignment. The assignment is not effective immediately,
	 * as this is raw operation. So, nothing much happens. Yet.
	 * MID-4110
	 */
	@Test
    public void test130JackAssignRoleSailorValidToRaw() throws Exception {
		final String TEST_NAME = "test130JackAssignRoleSailorValidToRaw";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);
        
        ModelExecuteOptions options = ModelExecuteOptions.createRaw();
        
        // WHEN
        displayWhen(TEST_NAME);
		modifyUserAssignment(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, RoleType.COMPLEX_TYPE, null, 
        		task, null, activationType, true, options, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, null);
        assertRoleMembershipRef(userAfter);
		assertDelegatedRef(userAfter);
		
		assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * MID-4110, MID-4114
	 */
	@Test
    public void test131RecomputeJack() throws Exception {
		final String TEST_NAME = "test131RecomputeJack";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.ENABLED);
        assertRoleMembershipRef(userAfter, ROLE_STRONG_SAILOR_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummySailorAccount();
	}
	
	/**
	 * Assignment expires.
	 * MID-4110, MID-4114
	 */
	@Test
    public void test132Forward15min() throws Exception {
		final String TEST_NAME = "test132Forward15min";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        clockForward("PT15M");
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * New assignment. No time validity.
	 * MID-4110
	 */
	@Test
    public void test134JackAssignRoleSailorAgain() throws Exception {
		final String TEST_NAME = "test134JackAssignRoleSailorAgain";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_STRONG_SAILOR_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummySailorAccount();
	}
	
	/**
	 * MID-4110
	 */
	@Test
    public void test139JackUnassignAll() throws Exception {
		unassignAll("test139JackUnassignAll");
	}
	
	/**
	 * This time do not recompute. Just set everything up, let the assignment expire
	 * and assign the role again.
	 * MID-4110
	 */
	@Test
    public void test140JackAssignRoleSailorValidToRaw() throws Exception {
		final String TEST_NAME = "test140JackAssignRoleSailorValidToRaw";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);
        
        ModelExecuteOptions options = ModelExecuteOptions.createRaw();
        
        // WHEN
        displayWhen(TEST_NAME);
		modifyUserAssignment(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, RoleType.COMPLEX_TYPE, null, 
        		task, null, activationType, true, options, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, null);
        assertRoleMembershipRef(userAfter);
		assertDelegatedRef(userAfter);
		
		assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
		
	/**
	 * Assignment expires. BUt do NOT recompute.
	 * MID-4110
	 */
	@Test
    public void test142Forward15min() throws Exception {
		final String TEST_NAME = "test142Forward15min";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        
        // WHEN
        displayWhen(TEST_NAME);
        clockForward("PT15M");
        // do NOT recompute
        
        // THEN
        displayThen(TEST_NAME);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, null); // Not recomputed
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * New assignment. No time validity.
	 * MID-4110
	 */
	@Test
    public void test144JackAssignRoleSailorAgain() throws Exception {
		final String TEST_NAME = "test144JackAssignRoleSailorAgain";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_STRONG_SAILOR_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummySailorAccount();
	}
	
	/**
	 * MID-4110
	 */
	@Test
    public void test149JackUnassignAll() throws Exception {
		unassignAll("test149JackUnassignAll");
	}
	
	/**
	 * MID-4110
	 */
	@Test
    public void test150JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test150JackAssignRolePirate";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignments(userAfter, 1);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);
        
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummyPirateAccount();
	}
	
	/**
	 * Sailor is an idempotent(conservative) role.
	 * MID-4110
	 */
	@Test
    public void test151JackAssignRoleSailorValidTo() throws Exception {
		final String TEST_NAME = "test151JackAssignRoleSailorValidTo";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, activationType, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignments(userAfter, 2);
        AssignmentType assignmentSailorTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentSailorTypeAfter, ActivationStatusType.ENABLED);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);
        
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID, ROLE_STRONG_SAILOR_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummyPirateSailorAccount();
	}
	
	/**
	 * Assignment expires.
	 * MID-4110, MID-4114
	 */
	@Test
    public void test153Forward15min() throws Exception {
		final String TEST_NAME = "test153Forward15min";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        clockForward("PT15M");
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);
        AssignmentType assignmentSailorTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentSailorTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);

        assertJackDummyPirateAccount();
	}
	
	/**
	 * New assignment. No time validity.
	 * MID-4110
	 */
	@Test
    public void test154JackAssignRoleSailorAgain() throws Exception {
		final String TEST_NAME = "test154JackAssignRoleSailorAgain";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 3);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID, ROLE_STRONG_SAILOR_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummyPirateSailorAccount();
	}
	
	/**
	 * MID-4110
	 */
	@Test
    public void test159JackUnassignAll() throws Exception {
		unassignAll("test159JackUnassignAll");
	}
	
	/**
	 * MID-4110
	 */
	@Test
    public void test160JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test160JackAssignRolePirate";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignments(userAfter, 1);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);
        
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummyPirateAccount();
	}
	
	/**
	 * MID-4110
	 */
	@Test
    public void test161JackAssignRoleSailorValidToRaw() throws Exception {
		final String TEST_NAME = "test161JackAssignRoleSailorValidToRaw";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);
        
        ModelExecuteOptions options = ModelExecuteOptions.createRaw();
        
        // WHEN
        displayWhen(TEST_NAME);
		modifyUserAssignment(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, RoleType.COMPLEX_TYPE, null, 
        		task, null, activationType, true, options, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignments(userAfter, 2);
        AssignmentType assignmentSailorTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentSailorTypeAfter, null);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);
        
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID); // SAILOR is not here, we are raw
		assertDelegatedRef(userAfter);

		
		assertJackDummyPirateAccount();
	}
	
	/**
	 * Recompute should fix it all.
	 * MID-4110
	 */
	@Test
    public void test162RecomputeJack() throws Exception {
		final String TEST_NAME = "test162RecomputeJack";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);
        
        // WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignments(userAfter, 2);
        AssignmentType assignmentSailorTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentSailorTypeAfter, ActivationStatusType.ENABLED);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);
        
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID, ROLE_STRONG_SAILOR_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummyPirateSailorAccount();
	}
		
	/**
	 * Assignment expires.
	 * MID-4110, MID-4114
	 */
	@Test
    public void test163Forward15min() throws Exception {
		final String TEST_NAME = "test163Forward15min";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        clockForward("PT15M");
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);
        AssignmentType assignmentSailorTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentSailorTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);

        assertJackDummyPirateAccount();
	}
	
	/**
	 * New assignment. No time validity.
	 * MID-4110
	 */
	@Test
    public void test164JackAssignRoleSailorAgain() throws Exception {
		final String TEST_NAME = "test164JackAssignRoleSailorAgain";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 3);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID, ROLE_STRONG_SAILOR_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummyPirateSailorAccount();
	}
	
	/**
	 * MID-4110
	 */
	@Test
    public void test169JackUnassignAll() throws Exception {
		unassignAll("test169JackUnassignAll");
	}
	
	/**
	 * MID-4110
	 */
	@Test
    public void test170JackAssignRolePirate() throws Exception {
		final String TEST_NAME = "test170JackAssignRolePirate";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignments(userAfter, 1);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);
        
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummyPirateAccount();
	}
	
	/**
	 * MID-4110
	 */
	@Test
    public void test171JackAssignRoleWeakSingerValidTo() throws Exception {
		final String TEST_NAME = "test171JackAssignRoleWeakSingerValidTo";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_WEAK_SINGER_OID, activationType, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        
        assertAssignments(userAfter, 2);
        AssignmentType assignmentSingerTypeAfter = assertAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);
        assertEffectiveActivation(assignmentSingerTypeAfter, ActivationStatusType.ENABLED);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);
        
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID, ROLE_WEAK_SINGER_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummyPirateSingerAccount();
	}
	
	/**
	 * Assignment expires.
	 * MID-4110, MID-4114
	 */
	@Test
    public void test173Forward15min() throws Exception {
		final String TEST_NAME = "test173Forward15min";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        clockForward("PT15M");
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);
        AssignmentType assignmentSailorTypeAfter = assertAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);
        assertEffectiveActivation(assignmentSailorTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);

        // Dummy attribute "title" is tolerant, so the singer value remains
        assertJackDummyPirateSingerAccount();
	}
	
	/**
	 * New assignment. No time validity.
	 * MID-4110
	 */
	@Test
    public void test174JackAssignRoleSingerAgain() throws Exception {
		final String TEST_NAME = "test174JackAssignRoleSingerAgain";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_WEAK_SINGER_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 3);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID, ROLE_WEAK_SINGER_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummyPirateSingerAccount();
	}
	
	/**
	 * MID-4110
	 */
	@Test
    public void test179JackUnassignAll() throws Exception {
		unassignAll("test179JackUnassignAll");
	}
	
	/**
	 * This time do both assigns as raw. And do NOT recompute until everything is set up.
	 * MID-4110
	 */
	@Test
    public void test180JackAssignRoleSailorValidToRaw() throws Exception {
		final String TEST_NAME = "test180JackAssignRoleSailorValidToRaw";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);
        
        // WHEN
        displayWhen(TEST_NAME);
		modifyUserAssignment(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, RoleType.COMPLEX_TYPE, null, 
        		task, null, activationType, true, ModelExecuteOptions.createRaw(), result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, null);
        assertRoleMembershipRef(userAfter);
		assertDelegatedRef(userAfter);
		
		assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
		
	/**
	 * MID-4110
	 */
	@Test
    public void test182Forward15minAndAssignRaw() throws Exception {
		final String TEST_NAME = "test142Forward15min";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        clockForward("PT15M");
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyUserAssignment(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, RoleType.COMPLEX_TYPE, null, 
        		task, null, null, true, ModelExecuteOptions.createRaw(), result);
        
        // THEN
        displayThen(TEST_NAME);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * MID-4110, MID-4114
	 */
	@Test
    public void test184RecomputeJack() throws Exception {
		final String TEST_NAME = "test184RecomputeJack";
        displayTestTile(TEST_NAME);

        Task task =  createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_STRONG_SAILOR_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummySailorAccount();
	}
	
	/**
	 * MID-4110
	 */
	@Test
    public void test189JackUnassignAll() throws Exception {
		unassignAll("test189JackUnassignAll");
	}

	
	private void assertJackDummyPirateAccount() throws Exception {
		assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Caribbean has ever seen");
	}
	
	private void assertJackDummySailorAccount() throws Exception {
		assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
	}
	
	private void assertJackDummyPirateSailorAccount() throws Exception {
		assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Caribbean has ever seen");
	}
	
	private void assertJackDummyPirateSingerAccount() throws Exception {
		assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_PIRATE_TITLE, ROLE_WEAK_SINGER_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it 
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, 
        		"Jack Sparrow is the best pirate Caribbean has ever seen");
	}
	
	private void unassignAll(final String TEST_NAME) throws Exception {
		displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignAll(userBefore, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 0);
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	

}
