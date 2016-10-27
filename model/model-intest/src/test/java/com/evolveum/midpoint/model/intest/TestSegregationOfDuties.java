/*
 * Copyright (c) 2010-2015 Evolveum
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
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
		
	@Test
    public void test110SimpleExclusion1() throws Exception {
		final String TEST_NAME = "test110SimpleExclusion1";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // This should go well
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        try {
	        // This should die
	        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
	        
	        AssertJUnit.fail("Expected policy violation after adding judge role, but it went well");
        } catch (PolicyViolationException e) {
        	// This is expected
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

}
