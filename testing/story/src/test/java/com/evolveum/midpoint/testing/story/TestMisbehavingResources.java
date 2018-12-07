/*
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.testing.story;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;

/**
 * Test for various resource-side errors, strange situations, timeouts
 * 
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMisbehavingResources extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "misbehaving-resources");
	
	protected static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
	protected static final String RESOURCE_DUMMY_OID = "5f9615a2-d05b-11e8-9dab-37186a8ab7ef";
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		initDummyResourcePirate(null, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);		
	}
	
	@Test
	public void test010SanityAssignJackDummyAccount() throws Exception {
		final String TEST_NAME = "test010SanityAssignJackDummyAccount";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertDummyAccountByUsername(null, USER_JACK_USERNAME)
			.assertFullName(USER_JACK_FULL_NAME);
	}
	
	@Test
	public void test019SanityUnassignJackDummyAccount() throws Exception {
		final String TEST_NAME = "test010SanityAssignJackDummyAccount";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertNoDummyAccount(USER_JACK_USERNAME);
	}

	/**
	 * MID-4773
	 */
	@Test
	public void test100AssignJackDummyAccountTimeout() throws Exception {
		final String TEST_NAME = "test100AssignJackDummyAccountTimeout";
		displayTestTitle(TEST_NAME);
		
		getDummyResource().setOperationDelayOffset(3000);
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertInProgress(result);
		
		// ConnId timeout is obviously not enforced. Therefore if the operation
		// does not fail by itself it is not forcibly stopped. The account is
		// created anyway.
		assertDummyAccountByUsername(null, USER_JACK_USERNAME)
			.assertFullName(USER_JACK_FULL_NAME);
	}
	
	@Test
	public void test102AssignJackDummyAccounRetry() throws Exception {
		final String TEST_NAME = "test102AssignJackDummyAccounRetry";
		displayTestTitle(TEST_NAME);
		
		getDummyResource().setOperationDelayOffset(0);
		clockForward("P1D");
		
		Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(USER_JACK_OID, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		assertDummyAccountByUsername(null, USER_JACK_USERNAME)
			.assertFullName(USER_JACK_FULL_NAME);
	}
}
