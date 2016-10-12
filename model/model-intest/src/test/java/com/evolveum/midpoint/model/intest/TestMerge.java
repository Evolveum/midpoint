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
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMerge extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/merge");
	
	public static final String MERGE_CONFIG_DEFAULT_NAME = "default";

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		modifyUserAdd(USER_GUYBRUSH_OID, UserType.F_EMPLOYEE_TYPE, initTask, initResult, 
				"SAILOR", "PIRATE WANNABE");
		modifyUserAdd(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, initTask, initResult, 
				createPolyString("Pirate Wannabes"), createPolyString("Scurvy Seadogs"));
		assignRole(USER_GUYBRUSH_OID, ROLE_SAILOR_OID, initTask, initResult);
		assignRole(USER_GUYBRUSH_OID, ROLE_EMPTY_OID, initTask, initResult);
		assignRole(USER_GUYBRUSH_OID, ROLE_THIEF_OID, initTask, initResult);
		
		modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATION, initTask, initResult, 
				createPolyString("Pirate Brethren"), createPolyString("Scurvy Seadogs"));
		assignRole(USER_JACK_OID, ROLE_SAILOR_OID, initTask, initResult);
		assignRole(USER_JACK_OID, ROLE_EMPTY_OID, initTask, initResult);
		assignRole(USER_JACK_OID, ROLE_PIRATE_OID, initTask, initResult);
		assignRole(USER_JACK_OID, ROLE_NICE_PIRATE_OID, initTask, initResult);
	}

	@Test
    public void test100MergeJackGuybrushPreviewDelta() throws Exception {
		final String TEST_NAME = "test100MergeJackGuybrushPreviewDelta";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TestMerge.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
       
        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        display("Jack before", userJackBefore);
        
        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("Guybrush before", userGuybrushBefore);
               
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        ObjectDelta<UserType> delta = 
        		modelInteractionService.mergeObjectsPreviewDelta(UserType.class, 
        				USER_JACK_OID, USER_GUYBRUSH_OID, MERGE_CONFIG_DEFAULT_NAME, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        display("Delta", delta);
        
        PrismAsserts.assertIsModify(delta);
        assertEquals("Wrong delta OID", USER_JACK_OID, delta.getOid());
        PrismAsserts.assertNoItemDelta(delta, UserType.F_NAME);
        PrismAsserts.assertNoItemDelta(delta, UserType.F_GIVEN_NAME);
        PrismAsserts.assertPropertyReplace(delta, UserType.F_FAMILY_NAME);
        PrismAsserts.assertPropertyReplace(delta, UserType.F_FULL_NAME, 
        		createPolyString(USER_GUYBRUSH_FULL_NAME));
        PrismAsserts.assertPropertyReplace(delta, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertPropertyReplace(delta, UserType.F_LOCALITY, 
        		createPolyString(USER_GUYBRUSH_LOCALITY));
        PrismAsserts.assertPropertyAdd(delta, UserType.F_EMPLOYEE_TYPE, 
        		"SAILOR", "PIRATE WANNABE");
        PrismAsserts.assertPropertyAdd(delta, UserType.F_ORGANIZATION, 
        		createPolyString("Pirate Wannabes"));
        PrismAsserts.assertNoItemDelta(delta, UserType.F_ACTIVATION);
        PrismAsserts.assertNoItemDelta(delta, 
        		new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS));
        PrismAsserts.assertNoItemDelta(delta, UserType.F_ROLE_MEMBERSHIP_REF);
        
        PrismAsserts.assertContainerAdd(delta, UserType.F_ASSIGNMENT, 
        		FocusTypeUtil.createRoleAssignment(ROLE_THIEF_OID));
        
	}
	
	@Test
    public void test102MergeJackGuybrushPreviewObject() throws Exception {
		final String TEST_NAME = "test102MergeJackGuybrushPreviewObject";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TestMerge.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        PrismObject<UserType> object = 
        		modelInteractionService.mergeObjectsPreviewObject(UserType.class, 
        				USER_JACK_OID, USER_GUYBRUSH_OID, MERGE_CONFIG_DEFAULT_NAME, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        display("Object", object);
        
        assertEquals("Wrong object OID", USER_JACK_OID, object.getOid());
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_NAME, createPolyString(USER_JACK_USERNAME));
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_GIVEN_NAME, createPolyString(USER_JACK_GIVEN_NAME));
        PrismAsserts.assertNoItem(object, UserType.F_FAMILY_NAME);
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_FULL_NAME, createPolyString(USER_GUYBRUSH_FULL_NAME));
        PrismAsserts.assertNoItem(object, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_LOCALITY, createPolyString(USER_GUYBRUSH_LOCALITY));
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_EMPLOYEE_TYPE, USER_JACK_EMPLOYEE_TYPE, "SAILOR", "PIRATE WANNABE");
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_ORGANIZATION, 
        		createPolyString("Pirate Brethren"), createPolyString("Scurvy Seadogs"), createPolyString("Pirate Wannabes"));
        
        assertAssignedRoles(object, ROLE_SAILOR_OID, ROLE_EMPTY_OID, ROLE_THIEF_OID, 
        		ROLE_PIRATE_OID, ROLE_NICE_PIRATE_OID);        
	}
	
	@Test
    public void test110MergeGuybrushJackPreviewDelta() throws Exception {
		final String TEST_NAME = "test110MergeGuybrushJackPreviewDelta";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TestMerge.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                
        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("Guybrush before", userGuybrushBefore);
        
        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        display("Jack before", userJackBefore);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        ObjectDelta<UserType> delta = 
        		modelInteractionService.mergeObjectsPreviewDelta(UserType.class, 
        				USER_GUYBRUSH_OID, USER_JACK_OID, MERGE_CONFIG_DEFAULT_NAME, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        display("Delta", delta);
        
        PrismAsserts.assertIsModify(delta);
        assertEquals("Wrong delta OID", USER_GUYBRUSH_OID, delta.getOid());
        PrismAsserts.assertNoItemDelta(delta, UserType.F_NAME);
        PrismAsserts.assertNoItemDelta(delta, UserType.F_GIVEN_NAME);
        PrismAsserts.assertPropertyReplace(delta, UserType.F_FAMILY_NAME);
        PrismAsserts.assertPropertyReplace(delta, UserType.F_FULL_NAME, 
        		createPolyString(USER_JACK_FULL_NAME));
        PrismAsserts.assertPropertyReplace(delta, UserType.F_ADDITIONAL_NAME,
        		createPolyString(USER_JACK_ADDITIONAL_NAME));
        PrismAsserts.assertPropertyReplace(delta, UserType.F_LOCALITY, 
        		createPolyString(USER_JACK_LOCALITY));
        PrismAsserts.assertPropertyAdd(delta, UserType.F_EMPLOYEE_TYPE, 
        		USER_JACK_EMPLOYEE_TYPE);
        PrismAsserts.assertPropertyAdd(delta, UserType.F_ORGANIZATION, 
        		createPolyString("Pirate Brethren"));
        PrismAsserts.assertNoItemDelta(delta, UserType.F_ACTIVATION);
        PrismAsserts.assertNoItemDelta(delta, 
        		new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS));
        PrismAsserts.assertNoItemDelta(delta, UserType.F_ROLE_MEMBERSHIP_REF);
        
        PrismAsserts.assertContainerAdd(delta, UserType.F_ASSIGNMENT, 
        		FocusTypeUtil.createRoleAssignment(ROLE_PIRATE_OID),
        		FocusTypeUtil.createRoleAssignment(ROLE_NICE_PIRATE_OID));

        
	}
	
	@Test
    public void test112MergeGuybrushJackPreviewObject() throws Exception {
		final String TEST_NAME = "test112MergeGuybrushJackPreviewObject";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TestMerge.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        PrismObject<UserType> object = 
        		modelInteractionService.mergeObjectsPreviewObject(UserType.class, 
        				USER_GUYBRUSH_OID, USER_JACK_OID, MERGE_CONFIG_DEFAULT_NAME, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        display("Object", object);
        
        assertEquals("Wrong object OID", USER_GUYBRUSH_OID, object.getOid());
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_NAME, createPolyString(USER_GUYBRUSH_USERNAME));
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_GIVEN_NAME, createPolyString(USER_GUYBRUSH_GIVEN_NAME));
        PrismAsserts.assertNoItem(object, UserType.F_FAMILY_NAME);
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_FULL_NAME, createPolyString(USER_JACK_FULL_NAME));
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_ADDITIONAL_NAME, createPolyString(USER_JACK_ADDITIONAL_NAME));
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_LOCALITY, createPolyString(USER_JACK_LOCALITY));
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_EMPLOYEE_TYPE, USER_JACK_EMPLOYEE_TYPE, "SAILOR", "PIRATE WANNABE");
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_ORGANIZATION, 
        		createPolyString("Pirate Brethren"), createPolyString("Scurvy Seadogs"), createPolyString("Pirate Wannabes"));

        assertAssignedRoles(object, ROLE_SAILOR_OID, ROLE_EMPTY_OID, ROLE_THIEF_OID, 
        		ROLE_PIRATE_OID, ROLE_NICE_PIRATE_OID);
        
	}
	
	@Test
    public void test200MergeJackGuybrush() throws Exception {
		final String TEST_NAME = "test200MergeJackGuybrush";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TestMerge.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.mergeObjects(UserType.class, 
        		USER_JACK_OID, USER_GUYBRUSH_OID, MERGE_CONFIG_DEFAULT_NAME, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> object = getObject(UserType.class, USER_JACK_OID);
        display("Object", object);
        
        assertEquals("Wrong object OID", USER_JACK_OID, object.getOid());
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_NAME, createPolyString(USER_JACK_USERNAME));
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_GIVEN_NAME, createPolyString(USER_JACK_GIVEN_NAME));
        PrismAsserts.assertNoItem(object, UserType.F_FAMILY_NAME);
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_FULL_NAME, createPolyString(USER_GUYBRUSH_FULL_NAME));
        PrismAsserts.assertNoItem(object, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_LOCALITY, createPolyString(USER_GUYBRUSH_LOCALITY));
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_EMPLOYEE_TYPE, USER_JACK_EMPLOYEE_TYPE, "SAILOR", "PIRATE WANNABE");
        
        assertAssignedRoles(object, ROLE_SAILOR_OID, ROLE_EMPTY_OID, ROLE_THIEF_OID, 
        		ROLE_PIRATE_OID, ROLE_NICE_PIRATE_OID);
        
        assertNoObject(UserType.class, USER_GUYBRUSH_OID);
        
	}
}
