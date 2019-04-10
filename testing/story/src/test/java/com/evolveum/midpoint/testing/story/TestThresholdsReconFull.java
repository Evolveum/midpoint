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
import static org.testng.Assert.assertNull;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author katka
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestThresholdsReconFull extends TestThresholds {

	private static final File TASK_RECONCILE_OPENDJ_FULL_FILE = new File(TEST_DIR, "task-opendj-reconcile-full.xml");
	private static final String TASK_RECONCILE_OPENDJ_FULL_OID = "20335c7c-838f-11e8-93a6-4b1dd0ab58e4";
	
	private static final File ROLE_POLICY_RULE_DELETE_FILE = new File(TEST_DIR, "role-policy-rule-delete.xml");
	private static final String ROLE_POLICY_RULE_DELETE_OID = "00000000-role-0000-0000-888111111112";
	
	private static final File TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_FILE = new File(TEST_DIR, "task-opendj-reconcile-simulate-execute.xml");
	private static final String TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID = "00000000-838f-11e8-93a6-4b1dd0ab58e4";
	
	@Override
	protected File getTaskFile() {
		return TASK_RECONCILE_OPENDJ_FULL_FILE;
	}
	
	@Override
	protected String getTaskOid() {
		return TASK_RECONCILE_OPENDJ_FULL_OID;
	}

	@Override
	protected int getProcessedUsers() {
		return 4;
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		repoAddObjectFromFile(ROLE_POLICY_RULE_DELETE_FILE, initResult);
		repoAddObjectFromFile(TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_FILE, initResult);
	}
	
	@Test
	public void test600chageTaskPolicyRule() throws Exception {
		final String TEST_NAME = "test600chageTaskPolicyRule";
		displayTestTitle(TEST_NAME);
		
		//WHEN
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();
		assignRole(TaskType.class, TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID, ROLE_POLICY_RULE_DELETE_OID, task, result);
		
		//THEN
		PrismObject<TaskType> taskAfter = getObject(TaskType.class, TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID);
		display("Task after:", taskAfter);
		assertAssignments(taskAfter, 1);
		assertAssigned(taskAfter, ROLE_POLICY_RULE_DELETE_OID, RoleType.COMPLEX_TYPE);
		assertTaskExecutionStatus(TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID, TaskExecutionStatus.SUSPENDED);
	}
	
	
	@Test
	public void test610testFullRecon() throws Exception {
		final String TEST_NAME = "test610testFullRecon";
		displayTestTitle(TEST_NAME);
		OperationResult result = new OperationResult(TEST_NAME);
		
		//WHEN
		displayWhen(TEST_NAME);
		OperationResult reconResult = waitForTaskResume(TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID, true, 20000);
		assertSuccess(reconResult);
		
		//THEN
		
		Task taskAfter = taskManager.getTaskWithResult(TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID, result);
		
		assertTaskExecutionStatus(TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID, TaskExecutionStatus.RUNNABLE);
//		assertUsers(getNumberOfUsers() + getProcessedUsers()*2);
		
		assertSynchronizationStatisticsFull(taskAfter);
		
	}
	

	@Override
	protected void assertSynchronizationStatisticsAfterImport(Task taskAfter) throws Exception {
		IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
		assertEquals(infoType.getTotalFailureCount(), 1);
		
		SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
		
		assertEquals(syncInfo.getCountUnmatched(), 5);
		assertEquals(syncInfo.getCountDeleted(), 0);
		assertEquals(syncInfo.getCountLinked(), getDefaultUsers());
		assertEquals(syncInfo.getCountUnlinked(), 0);
		
		assertEquals(syncInfo.getCountUnmatchedAfter(), 0);
		assertEquals(syncInfo.getCountDeleted(), 0);
		assertEquals(syncInfo.getCountLinkedAfter(), getDefaultUsers() + getProcessedUsers());
		assertEquals(syncInfo.getCountUnlinked(), 0);
	}
	
	private void assertSynchronizationStatisticsFull(Task taskAfter) throws Exception {
		IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
		assertEquals(infoType.getTotalFailureCount(), 0);
		assertNull(taskAfter.getWorkState(), "Unexpected work state in task.");
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.testing.story.TestThresholds#assertSynchronizationStatisticsAfterSecondImport(com.evolveum.midpoint.task.api.Task)
	 */
	@Override
	protected void assertSynchronizationStatisticsAfterSecondImport(Task taskAfter) throws Exception {
		IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
		assertEquals(infoType.getTotalFailureCount(), 1);
		
		SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
		
		assertEquals(syncInfo.getCountUnmatched(), 5);
		assertEquals(syncInfo.getCountDeleted(), 0);
		assertEquals(syncInfo.getCountLinked(), getDefaultUsers()+getProcessedUsers());
		assertEquals(syncInfo.getCountUnlinked(), 0);
		
		assertEquals(syncInfo.getCountUnmatchedAfter(), 0);
		assertEquals(syncInfo.getCountDeleted(), 0);
		assertEquals(syncInfo.getCountLinkedAfter(), getDefaultUsers() + getProcessedUsers()*2);
		assertEquals(syncInfo.getCountUnlinked(), 0);
	}
	
	protected void assertSynchronizationStatisticsActivation(Task taskAfter) {
		IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
		assertEquals(infoType.getTotalFailureCount(), 1);
		
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnmatched(), 3);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountDeleted(), 0);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountLinked(), 14);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnlinked(), 0);
		
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnmatchedAfter(), 0);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountDeleted(), 0);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountLinked(), 14);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnlinked(), 0);
	}
	
}
