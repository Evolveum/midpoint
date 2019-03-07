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
import static org.testng.Assert.assertNotNull;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author katka
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class TestThresholds extends AbstractStoryTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "thresholds");

	private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
 	
	private static final File LDIF_CREATE_BASE_USERS_FILE = new File(TEST_DIR, "users-base.ldif");
	private static final File LDIF_CREATE_USERS_FILE = new File(TEST_DIR, "users.ldif");
	private static final File LDIF_CHANGE_ACTIVATION_FILE = new File(TEST_DIR, "users-activation.ldif");
	
	
	private static final File ROLE_POLICY_RULE_CREATE_FILE = new File(TEST_DIR, "role-policy-rule-create.xml");
	private static final String ROLE_POLICY_RULE_CREATE_OID = "00000000-role-0000-0000-999111111112";
	
	private static final File ROLE_POLICY_RULE_CHANGE_ACTIVATION_FILE = new File(TEST_DIR, "role-policy-rule-change-activation.xml");
	private static final String ROLE_POLICY_RULE_CHANGE_ACTIVATION_OID = "00000000-role-0000-0000-999111111223";
	
	private static final File TASK_IMPORT_BASE_USERS_FILE = new File(TEST_DIR, "task-opendj-import-base-users.xml");
	private static final String TASK_IMPORT_BASE_USERS_OID = "fa25e6dc-a858-11e7-8ebc-eb2b71ecce1d";
	

	private PrismObject<ResourceType> resourceOpenDj;

	protected int getDefaultUsers() { 
		return 6;
	}
	
	
	@Override
	protected void startResources() throws Exception {
		openDJController.startCleanServer();
	}
	
	@AfterClass
	public static void stopResources() throws Exception {
		openDJController.stop();
	}
	
	protected abstract File getTaskFile();
	protected abstract String getTaskOid();
	protected abstract int getProcessedUsers();
	protected abstract void assertSynchronizationStatisticsAfterImport(Task syncInfo) throws Exception;
	
	
	protected void assertSynchronizationStatisticsActivation(Task taskAfter) {
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnmatched(), 5);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountDeleted(), 0);
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountLinked(), getDefaultUsers());
		assertEquals(taskAfter.getStoredOperationStats().getSynchronizationInformation().getCountUnlinked(), 0);
	}
	
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		//Resources
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
		openDJController.setResource(resourceOpenDj);
		
		repoAddObjectFromFile(ROLE_POLICY_RULE_CREATE_FILE, initResult);
		repoAddObjectFromFile(ROLE_POLICY_RULE_CHANGE_ACTIVATION_FILE, initResult);
	
		repoAddObjectFromFile(getTaskFile(), initResult);
	}
	
	
	@Test
	public void test001testImportBaseUsers() throws Exception {
		final String TEST_NAME = "test001testImportBaseUsers";
		displayTestTitle(TEST_NAME);
		OperationResult result = new OperationResult(TEST_NAME);
		
		importObjectFromFile(TASK_IMPORT_BASE_USERS_FILE);
		
		openDJController.addEntriesFromLdifFile(LDIF_CREATE_BASE_USERS_FILE);
		
		waitForTaskFinish(TASK_IMPORT_BASE_USERS_OID, true, 30000);
		
//		waitForTaskNextRun(TASK_IMPORT_BASE_USERS_OID, true, 20000, true);
		
		Task taskAfter = taskManager.getTaskWithResult(TASK_IMPORT_BASE_USERS_OID, result);
		display("Task after test001testImportBaseUsers:", taskAfter);
		
		OperationStatsType stats = taskAfter.getStoredOperationStats();
		assertNotNull(stats, "No statistics in task");
		
		SynchronizationInformationType syncInfo = stats.getSynchronizationInformation();
		assertNotNull(syncInfo, "No sync info in task");
		
		assertEquals(syncInfo.getCountUnmatched(), getDefaultUsers());
		assertEquals(syncInfo.getCountDeleted(), 0);
		assertEquals(syncInfo.getCountLinked(), 0);
		assertEquals(syncInfo.getCountUnlinked(), 0);
		
		assertEquals(syncInfo.getCountUnmatchedAfter(), 0);
		assertEquals(syncInfo.getCountDeletedAfter(), 0);
		assertEquals(syncInfo.getCountLinkedAfter(), getDefaultUsers());
		assertEquals(syncInfo.getCountUnlinkedAfter(), 0);
		
		assertUsers(getNumberOfUsers());
	}
	
	@Override
	protected int getNumberOfUsers() {
		return super.getNumberOfUsers() + getDefaultUsers();
	}
	
	@Test
	public void test100assignPolicyRuleCreateToTask() throws Exception {
		final String TEST_NAME = "test100assignPolicyRuleCreateToTask";
		displayTestTitle(TEST_NAME);

		// WHEN
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();
		assignRole(TaskType.class, getTaskOid(), ROLE_POLICY_RULE_CREATE_OID, task, result);
		
		//THEN
		PrismObject<TaskType> taskAfter = getObject(TaskType.class, getTaskOid());
		display("Task after:", taskAfter);
		assertAssignments(taskAfter, 1);
		assertAssigned(taskAfter, ROLE_POLICY_RULE_CREATE_OID, RoleType.COMPLEX_TYPE);
		assertTaskExecutionStatus(getTaskOid(), TaskExecutionStatus.SUSPENDED);		
		
	}
	
	@Test
	public void test110importAccounts() throws Exception {
		final String TEST_NAME = "test110importAccountsSimulate";
		displayTestTitle(TEST_NAME);

		Task task = taskManager.createTaskInstance(TEST_NAME);
	    OperationResult result = task.getResult();
		
		openDJController.addEntriesFromLdifFile(LDIF_CREATE_USERS_FILE);
		 
		
		
		assertUsers(getNumberOfUsers());
		//WHEN
		displayWhen(TEST_NAME);
		OperationResult reconResult = waitForTaskResume(getTaskOid(), false, 20000);
		assertFailure(reconResult);
		
		//THEN
		assertUsers(getProcessedUsers() + getNumberOfUsers());
		assertTaskExecutionStatus(getTaskOid(), TaskExecutionStatus.SUSPENDED);
		
		Task taskAfter = taskManager.getTaskWithResult(getTaskOid(), result);
		assertSynchronizationStatisticsAfterImport(taskAfter);
		
	}
	
	@Test
	public void test500chageTaskPolicyRule() throws Exception {
		final String TEST_NAME = "test500chageTaskPolicyRule";
		displayTestTitle(TEST_NAME);
		
		//WHEN
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();
		unassignRole(TaskType.class, getTaskOid(), ROLE_POLICY_RULE_CREATE_OID, task, result);
		assignRole(TaskType.class, getTaskOid(), ROLE_POLICY_RULE_CHANGE_ACTIVATION_OID, task, result);
		
		//THEN
		PrismObject<TaskType> taskAfter = getObject(TaskType.class, getTaskOid());
		display("Task after:", taskAfter);
		assertAssignments(taskAfter, 1);
		assertAssigned(taskAfter, ROLE_POLICY_RULE_CHANGE_ACTIVATION_OID, RoleType.COMPLEX_TYPE);
		assertTaskExecutionStatus(getTaskOid(), TaskExecutionStatus.SUSPENDED);
	}
	
	
	@Test
	public void test520changeActivationThreeAccounts() throws Exception {
		final String TEST_NAME = "test520changeActivationThreeAccounts";
		displayTestTitle(TEST_NAME);
		OperationResult result = new OperationResult(TEST_NAME);
		
		//GIVEN
		openDJController.executeLdifChange(LDIF_CHANGE_ACTIVATION_FILE);
		
		//WHEN
		displayWhen(TEST_NAME);
		OperationResult reconResult = waitForTaskNextRun(getTaskOid(), false, 20000, true);
		assertFailure(reconResult);
		
		//THEN
		
		Task taskAfter = taskManager.getTaskWithResult(getTaskOid(), result);
		
		assertTaskExecutionStatus(getTaskOid(), TaskExecutionStatus.SUSPENDED);
		assertUsers(getNumberOfUsers() + getProcessedUsers());
		
		assertSynchronizationStatisticsActivation(taskAfter);
		
	}
	
}
