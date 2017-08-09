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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
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
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
		assertDelegatedRef(userAfter);
		
		assertJackDummyPirateAccount();
	}

	/**
	 * Assignment expires.
	 * MID-4110
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
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}
	
	/**
	 * Unassign valid assignment. Only invalid assignment remains.
	 * MID-4110
	 */
	@Test
    public void test109JackUnassignAll() throws Exception {
		final String TEST_NAME = "test109JackUnassignAll";
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
        // TODO: assert assignment effectiveStatus
        assertRoleMembershipRef(userAfter);
		assertDelegatedRef(userAfter);
		
		assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
	}

//	/**
//	 * MID-4110
//	 */
//	@Test
//    public void test111RecomputeJack() throws Exception {
//		final String TEST_NAME = "test111RecomputeJack";
//        displayTestTile(TEST_NAME);
//
//        Task task =  createTask(TEST_NAME);
//        OperationResult result = task.getResult();
//        
//        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
//        display("User jack before", userBefore);
//
//        // WHEN
//        displayWhen(TEST_NAME);
//        recomputeUser(USER_JACK_OID, task, result);
//
//        // THEN
//        displayThen(TEST_NAME);
//        assertSuccess(result);
//        
//        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
//        display("User jack after", userAfter);
//        assertAssignments(userAfter, 1);
//        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
//		assertDelegatedRef(userAfter);
//		
//		assertJackDummyPirateAccount();
//	}
//
//	/**
//	 * Assignment expires.
//	 * MID-4110
//	 */
//	@Test
//    public void test112Forward15min() throws Exception {
//		final String TEST_NAME = "test102Forward15min";
//        displayTestTile(TEST_NAME);
//
//        Task task = createTask(TEST_NAME);
//        OperationResult result = task.getResult();
//        
//        clockForward("PT15M");
//        
//        // WHEN
//        displayWhen(TEST_NAME);
//        recomputeUser(USER_JACK_OID, task, result);
//        
//        // THEN
//        displayThen(TEST_NAME);
//        assertSuccess(result);
//        
//        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
//        display("User jack after", userAfter);
//        assertAssignments(userAfter, 1);
//        assertRoleMembershipRef(userAfter);
//
//        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
//	}
	
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
}
