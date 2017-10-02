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

package com.evolveum.midpoint.wf.impl.policy.other;

import com.evolveum.midpoint.model.impl.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskListener;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.wf.impl.policy.AbstractWfTestPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.model.api.ModelExecuteOptions.createExecuteImmediatelyAfterApproval;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType.CLOSED;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestParallelApprovals extends AbstractWfTestPolicy {

	private static final File ROLE_ROLE50A_FILE = new File(TEST_RESOURCE_DIR, "role-role50a-slow.xml");
	private static final File ROLE_ROLE51A_FILE = new File(TEST_RESOURCE_DIR, "role-role51a-slow.xml");
	private static final File ROLE_ROLE52A_FILE = new File(TEST_RESOURCE_DIR, "role-role52a-slow.xml");
	private static final File ROLE_ROLE53A_FILE = new File(TEST_RESOURCE_DIR, "role-role53a-slow.xml");

	private String roleRole50aOid, roleRole51aOid, roleRole52aOid, roleRole53aOid;

	@Override
	protected PrismObject<UserType> getDefaultActor() {
		return userAdministrator;
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		roleRole50aOid = repoAddObjectFromFile(ROLE_ROLE50A_FILE, initResult).getOid();
		roleRole51aOid = repoAddObjectFromFile(ROLE_ROLE51A_FILE, initResult).getOid();
		roleRole52aOid = repoAddObjectFromFile(ROLE_ROLE52A_FILE, initResult).getOid();
		roleRole53aOid = repoAddObjectFromFile(ROLE_ROLE53A_FILE, initResult).getOid();

		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
	}

	@Override
	protected void updateSystemConfiguration(SystemConfigurationType systemConfiguration) {
		super.updateSystemConfiguration(systemConfiguration);
		systemConfiguration.getWorkflowConfiguration()
				.beginExecutionTasks()
						.beginSerialization()
								.retryAfter(XmlTypeConverter.createDuration(1000));      // makes tests run faster
	}

	private CheckingTaskListener listener;

	@Test
	public void test100ParallelApprovals() throws Exception {
		final String TEST_NAME = "test100ParallelApprovals";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<UserType> assignDelta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(
						ObjectTypeUtil.createAssignmentTo(roleRole50aOid, ObjectTypes.ROLE, prismContext),
						ObjectTypeUtil.createAssignmentTo(roleRole51aOid, ObjectTypes.ROLE, prismContext),
						ObjectTypeUtil.createAssignmentTo(roleRole52aOid, ObjectTypes.ROLE, prismContext),
						ObjectTypeUtil.createAssignmentTo(roleRole53aOid, ObjectTypes.ROLE, prismContext))
				.asObjectDeltaCast(userJackOid);
		executeChanges(assignDelta, createExecuteImmediatelyAfterApproval(), task, result); // should start approval processes
		assertNotAssignedRole(userJackOid, roleRole51aOid, task, result);
		assertNotAssignedRole(userJackOid, roleRole52aOid, task, result);
		assertNotAssignedRole(userJackOid, roleRole53aOid, task, result);

		display("Task after operation", task);
		String rootTaskOid = wfTaskUtil.getRootTaskOid(task);
		display("root task", getTask(rootTaskOid));

		if (listener != null) {
			taskManager.unregisterTaskListener(listener);
		}
		listener = new CheckingTaskListener(singleton(rootTaskOid));
		taskManager.registerTaskListener(listener);

		approveAllWorkItems(task, result);

		waitForTaskCloseOrSuspend(rootTaskOid, 120000, 1000);

		// THEN

		PrismObject<TaskType> rootTask = getTask(rootTaskOid);
		assertNull("Exception has occurred " + listener.getException(), listener.getException());
		assertEquals("Wrong root task1 status", CLOSED, rootTask.asObjectable().getExecutionStatus());

		PrismObject<UserType> jack = getUser(userJackOid);
		assertAssignedRole(jack, roleRole50aOid);
		assertAssignedRole(jack, roleRole51aOid);
		assertAssignedRole(jack, roleRole52aOid);
		assertAssignedRole(jack, roleRole53aOid);
	}

	@Test
	public void test110ParallelApprovalsAdd() throws Exception {
		final String TEST_NAME = "test110ParallelApprovalsAdd";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		if (listener != null) {
			taskManager.unregisterTaskListener(listener);
		}
		listener = new CheckingTaskListener();
		taskManager.registerTaskListener(listener);

		// WHEN
		displayWhen(TEST_NAME);
		UserType alice = prismContext.createObjectable(UserType.class)
				.name("alice")
				.assignment(ObjectTypeUtil.createAssignmentTo(roleRole50aOid, ObjectTypes.ROLE, prismContext))
				.assignment(ObjectTypeUtil.createAssignmentTo(roleRole51aOid, ObjectTypes.ROLE, prismContext))
				.assignment(ObjectTypeUtil.createAssignmentTo(roleRole52aOid, ObjectTypes.ROLE, prismContext))
				.assignment(ObjectTypeUtil.createAssignmentTo(roleRole53aOid, ObjectTypes.ROLE, prismContext));
		executeChanges(ObjectDelta.createAddDelta(alice.asPrismObject()), createExecuteImmediatelyAfterApproval(), task, result); // should start approval processes

		display("Task after operation", task);
		String rootTaskOid = wfTaskUtil.getRootTaskOid(task);
		display("root task", getTask(rootTaskOid));

		listener.setTasksToSuspendOnError(singleton(rootTaskOid));

		approveAllWorkItems(task, result);
		waitForTaskCloseOrSuspend(rootTaskOid, 120000, 1000);

		// THEN

		PrismObject<TaskType> rootTask = getTask(rootTaskOid);
		assertNull("Exception has occurred " + listener.getException(), listener.getException());
		assertEquals("Wrong root task1 status", CLOSED, rootTask.asObjectable().getExecutionStatus());

		PrismObject<UserType> aliceAfter = findUserByUsername("alice");
		assertAssignedRole(aliceAfter, roleRole50aOid);
		assertAssignedRole(aliceAfter, roleRole51aOid);
		assertAssignedRole(aliceAfter, roleRole52aOid);
		assertAssignedRole(aliceAfter, roleRole53aOid);
	}

	public void approveAllWorkItems(Task task, OperationResult result) throws Exception {
		List<WorkItemType> workItems = getWorkItems(task, result);
		display("work items", workItems);
		display("approving work items");
		for (WorkItemType workItem : workItems) {
			workflowManager.completeWorkItem(workItem.getExternalId(), true, null, null, null, result);
		}
	}

	@Test
	public void test120ParallelApprovalsInTwoOperations() throws Exception {
		final String TEST_NAME = "test120ParallelApprovalsInTwoOperations";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		Task task1 = createTask(TEST_NAME);
		Task task2 = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		if (listener != null) {
			taskManager.unregisterTaskListener(listener);
		}
		listener = new CheckingTaskListener();
		taskManager.registerTaskListener(listener);

		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<UserType> assignDelta1 = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(
						ObjectTypeUtil.createAssignmentTo(roleRole50aOid, ObjectTypes.ROLE, prismContext),
						ObjectTypeUtil.createAssignmentTo(roleRole51aOid, ObjectTypes.ROLE, prismContext))
				.asObjectDeltaCast(userBobOid);
		executeChanges(assignDelta1, createExecuteImmediatelyAfterApproval(), task1, result); // should start approval processes
		ObjectDelta<UserType> assignDelta2 = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(
						ObjectTypeUtil.createAssignmentTo(roleRole50aOid, ObjectTypes.ROLE, prismContext),
						ObjectTypeUtil.createAssignmentTo(roleRole52aOid, ObjectTypes.ROLE, prismContext),
						ObjectTypeUtil.createAssignmentTo(roleRole53aOid, ObjectTypes.ROLE, prismContext))
				.asObjectDeltaCast(userBobOid);
		executeChanges(assignDelta2, createExecuteImmediatelyAfterApproval(), task2, result); // should start approval processes
		assertNotAssignedRole(userBobOid, roleRole51aOid, task, result);
		assertNotAssignedRole(userBobOid, roleRole52aOid, task, result);
		assertNotAssignedRole(userBobOid, roleRole53aOid, task, result);

		display("Task1 after operation", task1);
		display("Task2 after operation", task2);
		String rootTask1Oid = wfTaskUtil.getRootTaskOid(task1);
		String rootTask2Oid = wfTaskUtil.getRootTaskOid(task2);
		display("root task 1", getTask(rootTask1Oid));
		display("root task 2", getTask(rootTask2Oid));

		assertNull("Exception has occurred " + listener.getException(), listener.getException());
		listener.setTasksToSuspendOnError(Arrays.asList(rootTask1Oid, rootTask2Oid));

		approveAllWorkItems(task, result);

		waitForTaskCloseOrSuspend(rootTask1Oid, 120000, 1000);
		waitForTaskCloseOrSuspend(rootTask2Oid, 120000, 1000);

		// THEN

		PrismObject<TaskType> rootTask1 = getTask(rootTask1Oid);
		PrismObject<TaskType> rootTask2 = getTask(rootTask2Oid);
		assertNull("Exception has occurred " + listener.getException(), listener.getException());
		assertEquals("Wrong root task1 status", CLOSED, rootTask1.asObjectable().getExecutionStatus());
		assertEquals("Wrong root task2 status", CLOSED, rootTask2.asObjectable().getExecutionStatus());

		PrismObject<UserType> bob = getUser(userBobOid);
		assertAssignedRole(bob, roleRole50aOid);
		assertAssignedRole(bob, roleRole51aOid);
		assertAssignedRole(bob, roleRole52aOid);
		assertAssignedRole(bob, roleRole53aOid);
	}

	@Test
	public void test130ParallelApprovalsInThreeSummarizingOperations() throws Exception {
		final String TEST_NAME = "test130ParallelApprovalsInThreeSummarizingOperations";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		Task task1 = createTask(TEST_NAME);
		Task task2 = createTask(TEST_NAME);
		Task task3 = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		if (listener != null) {
			taskManager.unregisterTaskListener(listener);
		}
		listener = new CheckingTaskListener();
		taskManager.registerTaskListener(listener);

		// WHEN
		displayWhen(TEST_NAME);
		// three separate approval contexts, "summarizing" as the deltas are executed after all approvals
		assignRole(userChuckOid, roleRole51aOid, task1, result);
		assignRole(userChuckOid, roleRole52aOid, task2, result);
		assignRole(userChuckOid, roleRole53aOid, task3, result);
		assertNotAssignedRole(userChuckOid, roleRole51aOid, task, result);
		assertNotAssignedRole(userChuckOid, roleRole52aOid, task, result);
		assertNotAssignedRole(userChuckOid, roleRole53aOid, task, result);

		display("Task1 after operation", task1);
		display("Task2 after operation", task2);
		display("Task3 after operation", task3);
		String rootTask1Oid = wfTaskUtil.getRootTaskOid(task1);
		String rootTask2Oid = wfTaskUtil.getRootTaskOid(task2);
		String rootTask3Oid = wfTaskUtil.getRootTaskOid(task3);
		display("root task 1", getTask(rootTask1Oid));
		display("root task 2", getTask(rootTask2Oid));
		display("root task 3", getTask(rootTask3Oid));

		assertNull("Exception has occurred " + listener.getException(), listener.getException());
		listener.setTasksToSuspendOnError(Arrays.asList(rootTask1Oid, rootTask2Oid, rootTask3Oid));

		approveAllWorkItems(task, result);

		waitForTaskCloseOrSuspend(rootTask1Oid, 120000, 1000);
		waitForTaskCloseOrSuspend(rootTask2Oid, 120000, 1000);
		waitForTaskCloseOrSuspend(rootTask3Oid, 120000, 1000);

		// THEN

		PrismObject<TaskType> rootTask1 = getTask(rootTask1Oid);
		PrismObject<TaskType> rootTask2 = getTask(rootTask2Oid);
		PrismObject<TaskType> rootTask3 = getTask(rootTask3Oid);
		assertNull("Exception has occurred " + listener.getException(), listener.getException());
		assertEquals("Wrong root task1 status", CLOSED, rootTask1.asObjectable().getExecutionStatus());
		assertEquals("Wrong root task2 status", CLOSED, rootTask2.asObjectable().getExecutionStatus());
		assertEquals("Wrong root task3 status", CLOSED, rootTask3.asObjectable().getExecutionStatus());

		PrismObject<UserType> chuck = getUser(userChuckOid);
		assertAssignedRole(chuck, roleRole51aOid);
		assertAssignedRole(chuck, roleRole52aOid);
		assertAssignedRole(chuck, roleRole53aOid);
	}

	private class CheckingTaskListener implements TaskListener {

		private Collection<String> tasksToSuspendOnError;
		private Task executing;
		private RuntimeException exception;

		public CheckingTaskListener() {
			this.tasksToSuspendOnError = emptySet();
		}

		public CheckingTaskListener(Collection<String> tasksToSuspendOnError) {
			this.tasksToSuspendOnError = tasksToSuspendOnError;
		}

		public RuntimeException getException() {
			return exception;
		}

		public void setTasksToSuspendOnError(Collection<String> tasksToSuspendOnError) {
			this.tasksToSuspendOnError = tasksToSuspendOnError;
			if (exception != null) {
				suspendTasks();
			}
		}

		@Override
		public synchronized void onTaskStart(Task task) {
			if (!ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI.equals(task.getHandlerUri())) {
				return;
			}
			System.out.println(Thread.currentThread().getName() + ": Starting " + task + ", handler uri " + task.getHandlerUri() + ", group " + task.getGroup());
			if (executing != null) {
				exception = new IllegalStateException("Started task " + task + " but another one is already executing: " + executing);
				System.out.println(exception.getMessage());
				display("newly started task", task);
				display("already executing task", executing);
				suspendTasks();
			}
			executing = task;
		}

		public void suspendTasks() {
			// suspend root task in order to fail faster
			taskManager.suspendTasks(tasksToSuspendOnError, TaskManager.DO_NOT_WAIT, new OperationResult("dummy"));
		}

		@Override
		public synchronized void onTaskFinish(Task task, TaskRunResult runResult) {
			if (!ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI.equals(task.getHandlerUri())) {
				return;
			}
			System.out.println(Thread.currentThread().getName() + ": Finishing " + task + ", handler uri " + task.getHandlerUri());
			assert executing.getOid().equals(task.getOid());
			executing = null;
		}

		@Override
		public void onTaskThreadStart(Task task, boolean isRecovering) {
			// ignoring
		}

		@Override
		public void onTaskThreadFinish(Task task) {
			// ignoring
		}
	}
}
