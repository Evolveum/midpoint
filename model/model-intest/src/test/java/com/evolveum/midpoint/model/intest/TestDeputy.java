/*
 * Copyright (c) 2016 Evolveum
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

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Test for deputy (delegation) mechanism.
 * 
 * MID-3472
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestDeputy extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/deputy");
		
	@Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }
	
	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User Jack", userJack);
        assertNoAssignments(userJack);
        assertLinks(userJack, 0);
        assertNoAuthorizations(userJack);
        
        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        display("User Barbossa", userBarbossa);
        assertNoAssignments(userBarbossa);
        assertLinks(userBarbossa, 0);
        assertNoAuthorizations(userBarbossa);
	}

	/**
	 * Jack and Barbossa does not have any accounts or roles.
	 * Assign Barbossa as Jack's deputy. Not much should happen.
	 */
    @Test
    public void test100AssignDeputyNoBigDeal() throws Exception {
		final String TEST_NAME = "test100AssignDeputyNoBigDeal";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestDeputy.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignments(userBarbossaAfter, 1);
        assertLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);
        
        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertNoAssignments(userJackAfter);
        assertLinks(userJackAfter, 0);
        assertNoAuthorizations(userJackAfter);
        
    }
    
    /**
	 * Jack and Barbossa does not have any accounts or roles.
	 * Unassign Barbossa as Jack's deputy. Not much should happen.
	 */
    @Test
    public void test109UnassignDeputyNoBigDeal() throws Exception {
		final String TEST_NAME = "test109UnassignDeputyNoBigDeal";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestDeputy.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);        
        assertNoAssignments(userBarbossaAfter);
        assertLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);
        
        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertNoAssignments(userJackAfter);
        assertLinks(userJackAfter, 0);
        assertNoAuthorizations(userJackAfter);
        
    }
    
    /**
	 * Still not much here. Just preparing Jack.
	 * Make sure that Barbossa is not affected though.
	 */
    @Test
    public void test110AssignJackPirate() throws Exception {
		final String TEST_NAME = "test110AssignJackPirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestDeputy.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignedRole(userJackAfter, ROLE_PIRATE_OID);
        assertAssignments(userJackAfter, 1);
        assertAccount(userJackAfter, RESOURCE_DUMMY_OID);
        assertLinks(userJackAfter, 1);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL);
        
        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);        
        
    }
    
    /**
	 * Assign Barbossa as Jack's deputy. Barbossa should get equivalent
	 * accounts and authorizations as Jack.
	 */
    @Test
    public void test112AssignDeputyPirate() throws Exception {
		final String TEST_NAME = "test112AssignDeputyPirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestDeputy.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_OID);
        assertLinks(userBarbossaAfter, 1);
        assertAuthorizations(userBarbossaAfter, AUTZ_LOOT_URL);
        
        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignedRole(userJackAfter, ROLE_PIRATE_OID);
        assertAssignments(userJackAfter, 1);
        assertAccount(userJackAfter, RESOURCE_DUMMY_OID);
        assertLinks(userJackAfter, 1);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL);
        
    }
    
    // TODO: recompute barbossa, recompute jack
    
    /**
	 * Unassign Barbossa as Jack's deputy. Barbossa should get 
	 * back to emptiness.
	 */
    @Test
    public void test119UnassignDeputyPirate() throws Exception {
		final String TEST_NAME = "test119UnassignDeputyPirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TestDeputy.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);        
        assertNoAssignments(userBarbossaAfter);
        assertLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);
        
        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignedRole(userJackAfter, ROLE_PIRATE_OID);
        assertAssignments(userJackAfter, 1);
        assertAccount(userJackAfter, RESOURCE_DUMMY_OID);
        assertLinks(userJackAfter, 1);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL);
        
    }
    
    // TODO: assign deputy to empty jack, assign role to jack, recompute barbossa
	
}
