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

import com.evolveum.midpoint.model.api.util.MergeDeltas;
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
	public static final String MERGE_CONFIG_DEFAULT_SPECIFIC_NAME = "default-specific";
	public static final String MERGE_CONFIG_EXPRESSION_NAME = "expression";
	
	private String jackDummyAccountOid;
	private String jackDummyAccountRedOid;
	private String guybrushDummyAccountOid;
	private String guybrushDummyAccountCyanOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		modifyUserAdd(USER_GUYBRUSH_OID, UserType.F_EMPLOYEE_TYPE, initTask, initResult, 
				"SAILOR", "PIRATE WANNABE");
		modifyUserAdd(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, initTask, initResult, 
				createPolyString("Pirate Wannabes"), createPolyString("Sailors"), createPolyString("Rum Club"), createPolyString("Lovers"));
		assignRole(USER_GUYBRUSH_OID, ROLE_SAILOR_OID, initTask, initResult);
		assignRole(USER_GUYBRUSH_OID, ROLE_CYAN_SAILOR_OID, initTask, initResult);
		assignRole(USER_GUYBRUSH_OID, ROLE_EMPTY_OID, initTask, initResult);
		assignRole(USER_GUYBRUSH_OID, ROLE_THIEF_OID, initTask, initResult);
		
		modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATION, initTask, initResult, 
				createPolyString("Pirate Brethren"), createPolyString("Sailors"), createPolyString("Rum Club"), createPolyString("Drinkers"));
		assignRole(USER_JACK_OID, ROLE_SAILOR_OID, initTask, initResult);
		assignRole(USER_JACK_OID, ROLE_RED_SAILOR_OID, initTask, initResult);
		assignRole(USER_JACK_OID, ROLE_EMPTY_OID, initTask, initResult);
		assignRole(USER_JACK_OID, ROLE_PIRATE_OID, initTask, initResult);
		assignRole(USER_JACK_OID, ROLE_NICE_PIRATE_OID, initTask, initResult);
	}
	
	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		TestUtil.displayTestTitle(this, TEST_NAME);
		
        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        display("Jack before", userJackBefore);
        
        jackDummyAccountOid = assertAccount(userJackBefore, RESOURCE_DUMMY_OID);
        jackDummyAccountRedOid = assertAccount(userJackBefore, RESOURCE_DUMMY_RED_OID);
        assertLinks(userJackBefore, 2);
        
        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("Guybrush before", userGuybrushBefore);
        
        guybrushDummyAccountOid = assertAccount(userGuybrushBefore, RESOURCE_DUMMY_OID);
        guybrushDummyAccountCyanOid = assertAccount(userGuybrushBefore, RESOURCE_DUMMY_CYAN_OID);
        assertLinks(userGuybrushBefore, 2);
        
        display("Jack DUMMY account", jackDummyAccountOid);
        display("Jack RED account", jackDummyAccountRedOid);
        display("Guybrush DUMMY account", guybrushDummyAccountOid);
        display("Guybrush CYAN account", guybrushDummyAccountCyanOid);
	}

	/**
	 * MID-3460
	 */
	@Test
    public void test100MergeJackGuybrushPreviewDelta() throws Exception {
		final String TEST_NAME = "test100MergeJackGuybrushPreviewDelta";
		TestUtil.displayTestTitle(this, TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TestMerge.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
       
        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        display("Jack before", userJackBefore);
        
        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("Guybrush before", userGuybrushBefore);
               
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        MergeDeltas<UserType> deltas = 
        		modelInteractionService.mergeObjectsPreviewDeltas(UserType.class, 
        				USER_JACK_OID, USER_GUYBRUSH_OID, MERGE_CONFIG_DEFAULT_NAME, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        display("Deltas", deltas);
        
        ObjectDelta<UserType> leftObjectdelta = deltas.getLeftObjectDelta();
        PrismAsserts.assertIsModify(leftObjectdelta);
        assertEquals("Wrong delta OID", USER_JACK_OID, leftObjectdelta.getOid());
        PrismAsserts.assertNoItemDelta(leftObjectdelta, UserType.F_NAME);
        PrismAsserts.assertNoItemDelta(leftObjectdelta, UserType.F_GIVEN_NAME);
        PrismAsserts.assertPropertyReplace(leftObjectdelta, UserType.F_FAMILY_NAME);
        PrismAsserts.assertPropertyReplace(leftObjectdelta, UserType.F_FULL_NAME, 
        		createPolyString(USER_GUYBRUSH_FULL_NAME));
        PrismAsserts.assertPropertyReplace(leftObjectdelta, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertPropertyReplace(leftObjectdelta, UserType.F_LOCALITY, 
        		createPolyString(USER_GUYBRUSH_LOCALITY));
        PrismAsserts.assertPropertyAdd(leftObjectdelta, UserType.F_EMPLOYEE_TYPE, 
        		"SAILOR", "PIRATE WANNABE");
        PrismAsserts.assertPropertyAdd(leftObjectdelta, UserType.F_ORGANIZATION, 
        		createPolyString("Pirate Wannabes"), createPolyString("Lovers"));
        PrismAsserts.assertNoItemDelta(leftObjectdelta, UserType.F_ACTIVATION);
        PrismAsserts.assertNoItemDelta(leftObjectdelta, 
        		new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS));
        PrismAsserts.assertNoItemDelta(leftObjectdelta, UserType.F_ROLE_MEMBERSHIP_REF);
        PrismAsserts.assertNoItemDelta(leftObjectdelta, UserType.F_DELEGATED_REF);

        PrismAsserts.assertContainerAdd(leftObjectdelta, UserType.F_ASSIGNMENT, 
        		FocusTypeUtil.createRoleAssignment(ROLE_THIEF_OID));
        
        PrismAsserts.assertNoItemDelta(leftObjectdelta, UserType.F_LINK_REF);
        
        ObjectDelta<UserType> leftLinkDelta = deltas.getLeftLinkDelta();
        PrismAsserts.assertReferenceAdd(leftLinkDelta, UserType.F_LINK_REF, guybrushDummyAccountCyanOid);
        
        ObjectDelta<UserType> rightLinkDelta = deltas.getRightLinkDelta();
        PrismAsserts.assertReferenceDelete(rightLinkDelta, UserType.F_LINK_REF, guybrushDummyAccountCyanOid);
        
	}
	
	/**
	 * MID-3460
	 */
	@Test
    public void test102MergeJackGuybrushPreviewObject() throws Exception {
		final String TEST_NAME = "test102MergeJackGuybrushPreviewObject";
		TestUtil.displayTestTitle(this, TEST_NAME);
		
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
        		createPolyString("Pirate Brethren"), createPolyString("Sailors"), createPolyString("Rum Club"),
        		createPolyString("Pirate Wannabes"), createPolyString("Lovers"), createPolyString("Drinkers"));
        
        assertAssignedRoles(object, ROLE_SAILOR_OID, ROLE_RED_SAILOR_OID, ROLE_CYAN_SAILOR_OID, 
        		ROLE_EMPTY_OID, ROLE_THIEF_OID, ROLE_PIRATE_OID, ROLE_NICE_PIRATE_OID);
        
        assertLinked(object, jackDummyAccountOid);
        assertLinked(object, jackDummyAccountRedOid);
        assertLinked(object, guybrushDummyAccountCyanOid);
        assertLinks(object, 3);
	}
	
	/**
	 * MID-3460
	 */
	@Test
    public void test110MergeGuybrushJackPreviewDelta() throws Exception {
		final String TEST_NAME = "test110MergeGuybrushJackPreviewDelta";
		TestUtil.displayTestTitle(this, TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TestMerge.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                
        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("Guybrush before", userGuybrushBefore);
        
        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        display("Jack before", userJackBefore);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        MergeDeltas<UserType> deltas = 
        		modelInteractionService.mergeObjectsPreviewDeltas(UserType.class, 
        				USER_GUYBRUSH_OID, USER_JACK_OID, MERGE_CONFIG_DEFAULT_NAME, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        display("Deltas", deltas);
        
        ObjectDelta<UserType> delta = deltas.getLeftObjectDelta();
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
        		createPolyString("Pirate Brethren"), createPolyString("Drinkers"));
        PrismAsserts.assertNoItemDelta(delta, UserType.F_ACTIVATION);
        PrismAsserts.assertNoItemDelta(delta, 
        		new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS));
        PrismAsserts.assertNoItemDelta(delta, UserType.F_ROLE_MEMBERSHIP_REF);
        PrismAsserts.assertNoItemDelta(delta, UserType.F_DELEGATED_REF);

        PrismAsserts.assertContainerAdd(delta, UserType.F_ASSIGNMENT, 
        		FocusTypeUtil.createRoleAssignment(ROLE_PIRATE_OID),
        		FocusTypeUtil.createRoleAssignment(ROLE_NICE_PIRATE_OID));
        
        PrismAsserts.assertNoItemDelta(delta, UserType.F_LINK_REF);

        ObjectDelta<UserType> leftLinkDelta = deltas.getLeftLinkDelta();
        PrismAsserts.assertReferenceAdd(leftLinkDelta, UserType.F_LINK_REF, jackDummyAccountRedOid);
        
        ObjectDelta<UserType> rightLinkDelta = deltas.getRightLinkDelta();
        PrismAsserts.assertReferenceDelete(rightLinkDelta, UserType.F_LINK_REF, jackDummyAccountRedOid);
	}
	
	/**
	 * MID-3460
	 */
	@Test
    public void test112MergeGuybrushJackPreviewObject() throws Exception {
		final String TEST_NAME = "test112MergeGuybrushJackPreviewObject";
		TestUtil.displayTestTitle(this, TEST_NAME);
		
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
        		createPolyString("Pirate Brethren"), createPolyString("Sailors"), createPolyString("Rum Club"),
        		createPolyString("Pirate Wannabes"), createPolyString("Lovers"), createPolyString("Drinkers"));

        assertAssignedRoles(object, ROLE_SAILOR_OID, ROLE_RED_SAILOR_OID, ROLE_CYAN_SAILOR_OID,
        		ROLE_EMPTY_OID, ROLE_THIEF_OID, ROLE_PIRATE_OID, ROLE_NICE_PIRATE_OID);
        
	}

	/**
	 * MID-3460
	 */
	@Test
    public void test200MergeJackGuybrushExpressionPreviewDelta() throws Exception {
		final String TEST_NAME = "test200MergeJackGuybrushExpressionPreviewDelta";
		TestUtil.displayTestTitle(this, TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TestMerge.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
       
        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        display("Jack before", userJackBefore);
        
        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("Guybrush before", userGuybrushBefore);
               
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        MergeDeltas<UserType> deltas = 
        		modelInteractionService.mergeObjectsPreviewDeltas(UserType.class, 
        				USER_JACK_OID, USER_GUYBRUSH_OID, MERGE_CONFIG_EXPRESSION_NAME, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        display("Deltas", deltas);
        
        ObjectDelta<UserType> delta = deltas.getLeftObjectDelta();
        PrismAsserts.assertIsModify(delta);
        assertEquals("Wrong delta OID", USER_JACK_OID, delta.getOid());
        PrismAsserts.assertNoItemDelta(delta, UserType.F_NAME);
        PrismAsserts.assertNoItemDelta(delta, UserType.F_GIVEN_NAME);
        PrismAsserts.assertNoItemDelta(delta, UserType.F_FAMILY_NAME);
        PrismAsserts.assertNoItemDelta(delta, UserType.F_FULL_NAME);
        PrismAsserts.assertNoItemDelta(delta, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertPropertyAdd(delta, UserType.F_EMPLOYEE_TYPE, 
        		"SAILOR");
        PrismAsserts.assertPropertyAdd(delta, UserType.F_ORGANIZATION, 
        		createPolyString("Pirate Wannabes"));
        PrismAsserts.assertPropertyDelete(delta, UserType.F_ORGANIZATION, 
        		createPolyString("Sailors"), createPolyString("Drinkers"));
        PrismAsserts.assertNoItemDelta(delta, UserType.F_ACTIVATION);
        PrismAsserts.assertNoItemDelta(delta, 
        		new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS));
        PrismAsserts.assertNoItemDelta(delta, UserType.F_ROLE_MEMBERSHIP_REF);
        PrismAsserts.assertNoItemDelta(delta, UserType.F_DELEGATED_REF);

        PrismAsserts.assertContainerAdd(delta, UserType.F_ASSIGNMENT, 
        		FocusTypeUtil.createRoleAssignment(ROLE_THIEF_OID));        
	}
	
	/**
	 * MID-3460
	 */
	@Test
    public void test202MergeJackGuybrushExpressionPreviewObject() throws Exception {
		final String TEST_NAME = "test202MergeJackGuybrushExpressionPreviewObject";
		TestUtil.displayTestTitle(this, TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TestMerge.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        PrismObject<UserType> object = 
        		modelInteractionService.mergeObjectsPreviewObject(UserType.class, 
        				USER_JACK_OID, USER_GUYBRUSH_OID, MERGE_CONFIG_EXPRESSION_NAME, task, result);
        
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
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_FAMILY_NAME, createPolyString(USER_JACK_FAMILY_NAME));
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_FULL_NAME, createPolyString(USER_JACK_FULL_NAME));
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_ADDITIONAL_NAME, createPolyString(USER_JACK_ADDITIONAL_NAME));
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_LOCALITY, createPolyString(USER_JACK_LOCALITY));
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_EMPLOYEE_TYPE, USER_JACK_EMPLOYEE_TYPE, "SAILOR");
        PrismAsserts.assertPropertyValue(object, 
        		UserType.F_ORGANIZATION, 
        		createPolyString("Pirate Brethren"), createPolyString("Rum Club"),
        		createPolyString("Pirate Wannabes"));
        
        assertAssignedRoles(object, ROLE_SAILOR_OID, ROLE_RED_SAILOR_OID, ROLE_CYAN_SAILOR_OID,
        		ROLE_EMPTY_OID, ROLE_THIEF_OID, ROLE_PIRATE_OID, ROLE_NICE_PIRATE_OID);        
	}


	/**
	 * The default-specific config is almost the same as default (test1XX),
	 * just the projections are selected by specific resource.
	 * MID-3460
	 */
	@Test
    public void test300MergeJackGuybrushPreviewDeltaDefaultSpecific() throws Exception {
		final String TEST_NAME = "test300MergeJackGuybrushPreviewDeltaDefaultSpecific";
		TestUtil.displayTestTitle(this, TEST_NAME);
		
		Task task = taskManager.createTaskInstance(TestMerge.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
       
        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        display("Jack before", userJackBefore);
        
        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("Guybrush before", userGuybrushBefore);
               
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        MergeDeltas<UserType> deltas = 
        		modelInteractionService.mergeObjectsPreviewDeltas(UserType.class, 
        				USER_JACK_OID, USER_GUYBRUSH_OID, MERGE_CONFIG_DEFAULT_SPECIFIC_NAME, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        display("Deltas", deltas);
        
        ObjectDelta<UserType> leftObjectdelta = deltas.getLeftObjectDelta();
        PrismAsserts.assertIsModify(leftObjectdelta);
        assertEquals("Wrong delta OID", USER_JACK_OID, leftObjectdelta.getOid());
        PrismAsserts.assertNoItemDelta(leftObjectdelta, UserType.F_NAME);
        PrismAsserts.assertNoItemDelta(leftObjectdelta, UserType.F_GIVEN_NAME);
        PrismAsserts.assertPropertyReplace(leftObjectdelta, UserType.F_FAMILY_NAME);
        PrismAsserts.assertPropertyReplace(leftObjectdelta, UserType.F_FULL_NAME, 
        		createPolyString(USER_GUYBRUSH_FULL_NAME));
        PrismAsserts.assertPropertyReplace(leftObjectdelta, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertPropertyReplace(leftObjectdelta, UserType.F_LOCALITY, 
        		createPolyString(USER_GUYBRUSH_LOCALITY));
        PrismAsserts.assertPropertyAdd(leftObjectdelta, UserType.F_EMPLOYEE_TYPE, 
        		"SAILOR", "PIRATE WANNABE");
        PrismAsserts.assertPropertyAdd(leftObjectdelta, UserType.F_ORGANIZATION, 
        		createPolyString("Pirate Wannabes"), createPolyString("Lovers"));
        PrismAsserts.assertNoItemDelta(leftObjectdelta, UserType.F_ACTIVATION);
        PrismAsserts.assertNoItemDelta(leftObjectdelta, 
        		new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS));
        PrismAsserts.assertNoItemDelta(leftObjectdelta, UserType.F_ROLE_MEMBERSHIP_REF);
        PrismAsserts.assertNoItemDelta(leftObjectdelta, UserType.F_DELEGATED_REF);

        PrismAsserts.assertContainerAdd(leftObjectdelta, UserType.F_ASSIGNMENT, 
        		FocusTypeUtil.createRoleAssignment(ROLE_THIEF_OID));
        
        PrismAsserts.assertNoItemDelta(leftObjectdelta, UserType.F_LINK_REF);
        
        ObjectDelta<UserType> leftLinkDelta = deltas.getLeftLinkDelta();
        PrismAsserts.assertReferenceAdd(leftLinkDelta, UserType.F_LINK_REF, guybrushDummyAccountCyanOid);
        
        ObjectDelta<UserType> rightLinkDelta = deltas.getRightLinkDelta();
        PrismAsserts.assertReferenceDelete(rightLinkDelta, UserType.F_LINK_REF, guybrushDummyAccountCyanOid);
        
	}
	
	/**
	 * MID-3460
	 */
	@Test
    public void test500MergeJackGuybrush() throws Exception {
		final String TEST_NAME = "test500MergeJackGuybrush";
		TestUtil.displayTestTitle(this, TEST_NAME);
		
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
        
        assertAssignedRoles(object, ROLE_SAILOR_OID, ROLE_RED_SAILOR_OID, ROLE_CYAN_SAILOR_OID,
        		ROLE_EMPTY_OID, ROLE_THIEF_OID, ROLE_PIRATE_OID, ROLE_NICE_PIRATE_OID);
        
        assertNoObject(UserType.class, USER_GUYBRUSH_OID);
        
	}
}
