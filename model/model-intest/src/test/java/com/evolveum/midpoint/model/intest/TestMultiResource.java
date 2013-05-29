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

import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMultiResource extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/multiresource");
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
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
	 * The "dummies" role assigns two dummy resources that are in a dependency. The value of "ship" is propagated from one
	 * resource through the user to the other resource. If dependency does not work then no value is propagated.
	 */
    public void jackAssignRoleDummies(final String TEST_NAME) throws Exception {
        displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_DUMMIES_OID, task, result);
        
        // THEN
        assertAssignedRole(USER_JACK_OID, ROLE_DUMMIES_OID, task, result);
        
        assertDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "title", "The Great Voodoo Master");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME, "ship", "The Lost Souls");
        
        // This is set up by "feedback" using an inbound expression. It has nothing with dependencies yet.
        assertUserProperty(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, PrismTestUtil.createPolyString("The crew of The Lost Souls"));
        
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        // This is set by red's outbound from user's organizationalUnit. If dependencies work this outbound is processed
        // after user's organizationUnit is set and it will have the same value as above.
        assertDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "ship", "The crew of The Lost Souls");
	}
	
	@Test
    public void jackUnAssignRoleDummies(final String TEST_NAME) throws Exception {
        displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        unassignRole(USER_JACK_OID, ROLE_DUMMIES_OID, task, result);
        
        // THEN
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignedNoRole(user);
        
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        
        assertUserProperty(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, PrismTestUtil.createPolyString("The crew of The Lost Souls"));        
	}
	
}
