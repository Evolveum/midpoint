/*
 * Copyright (c) 2010-2018 Evolveum
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
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathImpl;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.test.asserter.FocusAsserter;
import com.evolveum.midpoint.test.asserter.PrismObjectAsserter;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author katka
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestThresholds extends AbstractStoryTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "thresholds");

	private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
 	
	private static final File LDIF_CREATE_USERS_FILE = new File(TEST_DIR, "users.ldif");
	private static final File LDIF_CHANGE_ACTIVATION_FILE = new File(TEST_DIR, "users-activation.ldif");
	
	private static final File TASK_RECONCILE_OPENDJ_FILE = new File(TEST_DIR, "task-opendj-reconcile.xml");
	private static final String TASK_RECONCILE_OPENDJ_OID = "10335c7c-838f-11e8-93a6-4b1dd0ab58e4";
	
	private static final File ROLE_POLICY_RULE_CREATE_FILE = new File(TEST_DIR, "role-policy-rule-create.xml");
	private static final String ROLE_POLICY_RULE_CREATE_OID = "00000000-role-0000-0000-999111111112";
	
	private static final File ROLE_POLICY_RULE_CHANGE_ACTIVATION_FILE = new File(TEST_DIR, "role-policy-rule-change-activation.xml");
	private static final String ROLE_POLICY_RULE_CHANGE_ACTIVATION_OID = "00000000-role-0000-0000-999111111223";

	private PrismObject<ResourceType> resourceOpenDj;

	
	private static int defaultLdapUsers = 3;
	
	@Override
	protected void startResources() throws Exception {
		openDJController.startCleanServer();
	}
	
	@AfterClass
	public static void stopResources() throws Exception {
		openDJController.stop();
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		//Resources
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
		openDJController.setResource(resourceOpenDj);
		
		repoAddObjectFromFile(ROLE_POLICY_RULE_CREATE_FILE, initResult);
		repoAddObjectFromFile(ROLE_POLICY_RULE_CHANGE_ACTIVATION_FILE, initResult);
		
		repoAddObjectFromFile(TASK_RECONCILE_OPENDJ_FILE, initResult);

	}
	
	@Test
	public void test100assignPolicyRuleCreateToTask() throws Exception {
		final String TEST_NAME = "test100assignPolicyRuleCreateToTask";
		displayTestTitle(TEST_NAME);

		// WHEN
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();
		assignRole(TaskType.class, TASK_RECONCILE_OPENDJ_OID, ROLE_POLICY_RULE_CREATE_OID, task, result);
		
		//THEN
		PrismObject<TaskType> taskAfter = getObject(TaskType.class, TASK_RECONCILE_OPENDJ_OID);
		display("Task after:", taskAfter);
		assertAssignments(taskAfter, 1);
		assertAssigned(taskAfter, ROLE_POLICY_RULE_CREATE_OID, RoleType.COMPLEX_TYPE);
		assertTaskExecutionStatus(TASK_RECONCILE_OPENDJ_OID, TaskExecutionStatus.SUSPENDED);		
		
	}
	
	@Test
	public void test110importAccountsSimulate() throws Exception {
		final String TEST_NAME = "test110importAccountsSimulate";
		displayTestTitle(TEST_NAME);
		
		
		openDJController.addEntriesFromLdifFile(LDIF_CREATE_USERS_FILE);
		 
		Task task = taskManager.createTaskInstance(TEST_NAME);
	    OperationResult result = task.getResult();
		executeChanges(DeltaBuilder.deltaFor(TaskType.class, prismContext)
						.item(TaskType.F_EXECUTION_STATUS)
							.replace(TaskExecutionStatusType.RUNNABLE)
							.asObjectDelta(TASK_RECONCILE_OPENDJ_OID), null, task, result);
		
		assertUsers(getNumberOfUsers());
		//WHEN
		displayWhen(TEST_NAME);
		OperationResult reconResult = waitForTaskNextRun(TASK_RECONCILE_OPENDJ_OID, false, 20000, true);
		assertFailure(reconResult);
		
		//THEN
		assertUsers(getNumberOfUsers());
		assertTaskExecutionStatus(TASK_RECONCILE_OPENDJ_OID, TaskExecutionStatus.SUSPENDED);
		
		Task taskAfter = taskManager.getTaskWithResult(TASK_RECONCILE_OPENDJ_OID, result);
		IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
		assertEquals(infoType.getTotalFailureCount(), 1);
		
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnmatched(), 5);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountDeleted(), 0);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountLinked(), 0);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnlinked(), 0);
	}
	
	
	@Test
	public void test120changeReconTaskFull() throws Exception {
		final String TEST_NAME = "test120changeReconTaskFull";
		displayTestTitle(TEST_NAME);
		
		assertUsers(getNumberOfUsers());
		
		// WHEN
        displayWhen(TEST_NAME);
        
        Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult result = task.getResult();
        PrismObject<TaskType> taskBefore = getObject(TaskType.class, TASK_RECONCILE_OPENDJ_OID);
        TaskType taskTypeBefore = taskBefore.asObjectable();
        List<TaskStageType> stages = taskTypeBefore.getStage();
        TaskStageType simulateStage = null;
        for (TaskStageType stage : stages) {
        	if (ReconciliationTaskHandler.SIMULATE_URI.equals(stage.getStage())) {
        		simulateStage  = stage;
        		break;
        	}
        }
        modifyObjectDeleteContainer(TaskType.class, TASK_RECONCILE_OPENDJ_OID, new ItemName(TaskType.F_STAGE), task, result, simulateStage.clone());

		// THEN
		displayThen(TEST_NAME);
		
		PrismObject<TaskType> taskAfter = getObject(TaskType.class, TASK_RECONCILE_OPENDJ_OID);		
		assertNotNull(taskAfter, "Task not found");
		
		TaskType taskTypeAfter = taskAfter.asObjectable();
		List<TaskStageType> stagesAfter = taskTypeAfter.getStage();
		assertEquals(stagesAfter.size(), 1, "Unexpected number of stages");
		
		TaskStageType stageAfter = stagesAfter.iterator().next();
		assertEquals(ReconciliationTaskHandler.EXECUTE_URI, stageAfter.getStage(), "Unexpected stage.");
		assertTaskExecutionStatus(TASK_RECONCILE_OPENDJ_OID, TaskExecutionStatus.SUSPENDED);
				
				
	}
		
	@Test
	public void test510importFourAccounts() throws Exception {
		final String TEST_NAME = "test510importFourAccounts";
		displayTestTitle(TEST_NAME);
		
		//GIVEN
		Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult result = task.getResult();
        modifyObjectReplaceProperty(TaskType.class, TASK_RECONCILE_OPENDJ_OID, TaskType.F_EXECUTION_STATUS, task, result, TaskExecutionStatusType.RUNNABLE);
		
		//WHEN
		displayWhen(TEST_NAME);
		OperationResult reconResult = waitForTaskNextRun(TASK_RECONCILE_OPENDJ_OID, false, 20000, true);
		assertFailure(reconResult);
		
		//THEN
		assertUsers(getNumberOfUsers() + 4);
		assertTaskExecutionStatus(TASK_RECONCILE_OPENDJ_OID, TaskExecutionStatus.SUSPENDED);
	}
	
	
	@Test
	public void test520changeActivationThreeAccounts() throws Exception {
		final String TEST_NAME = "test520changeActivationThreeAccounts";
		displayTestTitle(TEST_NAME);
		
		//GIVEN
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();
		unassignRole(TaskType.class, TASK_RECONCILE_OPENDJ_OID, ROLE_POLICY_RULE_CREATE_OID, task, result);
		assignRole(TaskType.class, TASK_RECONCILE_OPENDJ_OID, ROLE_POLICY_RULE_CHANGE_ACTIVATION_OID, task, result);
		
		openDJController.executeLdifChange(LDIF_CHANGE_ACTIVATION_FILE);
		
		//WHEN
		displayWhen(TEST_NAME);
		OperationResult reconResult = waitForTaskNextRun(TASK_RECONCILE_OPENDJ_OID, false, 20000, true);
		assertFailure(reconResult);
		
		//THEN
		
		Task taskAfter = taskManager.getTaskWithResult(TASK_RECONCILE_OPENDJ_OID, result);
//		recotaskAfternTask.getStoredOperationStats().getSynchronizationInformation().getCountLinked();	
		
		assertTaskExecutionStatus(TASK_RECONCILE_OPENDJ_OID, TaskExecutionStatus.SUSPENDED);
		assertUsers(getNumberOfUsers() + 4);
		
//		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnmatched(), 4);
//		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountDeleted(), 0);
//		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountLinked(), 0);
//		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnlinked(), 0);
		
	}
	
}
