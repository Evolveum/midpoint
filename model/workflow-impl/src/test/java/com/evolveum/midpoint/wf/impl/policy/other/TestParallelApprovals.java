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
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.model.api.ModelExecuteOptions.createExecuteImmediatelyAfterApproval;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType.CLOSED;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestParallelApprovals extends AbstractWfTestPolicy {

	private static final File ROLE_ROLE51A_FILE = new File(TEST_RESOURCE_DIR, "role-role51a-slow.xml");
	private static final File ROLE_ROLE52A_FILE = new File(TEST_RESOURCE_DIR, "role-role52a-slow.xml");
	private static final File ROLE_ROLE53A_FILE = new File(TEST_RESOURCE_DIR, "role-role53a-slow.xml");

	private String roleRole51aOid, roleRole52aOid, roleRole53aOid;

	@Override
	protected PrismObject<UserType> getDefaultActor() {
		return userAdministrator;
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		roleRole51aOid = repoAddObjectFromFile(ROLE_ROLE51A_FILE, initResult).getOid();
		roleRole52aOid = repoAddObjectFromFile(ROLE_ROLE52A_FILE, initResult).getOid();
		roleRole53aOid = repoAddObjectFromFile(ROLE_ROLE53A_FILE, initResult).getOid();

		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
	}

	@Override
	protected void updateSystemConfiguration(SystemConfigurationType systemConfiguration) {
		super.updateSystemConfiguration(systemConfiguration);
		systemConfiguration.getWorkflowConfiguration()
			.beginExecutionTasksSerialization()
				.retryInterval(XmlTypeConverter.createDuration(1000));      // makes tests run faster
	}

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

		CheckingTaskListener listener = new CheckingTaskListener(rootTaskOid);
		taskManager.registerTaskListener(listener);

		List<WorkItemType> workItems = getWorkItems(task, result);
		display("work items", workItems);
		display("approving work items");
		for (WorkItemType workItem : workItems) {
			workflowManager.completeWorkItem(workItem.getExternalId(), true, null, null, null, result);
		}

		waitForTaskCloseOrSuspend(rootTaskOid, 120000, 1000);

		// THEN

		PrismObject<TaskType> rootTask = getTask(rootTaskOid);
		if (listener.getException() != null || rootTask.asObjectable().getExecutionStatus() != CLOSED) {
			fail("root task has not completed; recorded exception = " + listener.getException());
		}

		PrismObject<UserType> jack = getUser(userJackOid);
		assertAssignedRole(jack, roleRole51aOid);
		assertAssignedRole(jack, roleRole52aOid);
		assertAssignedRole(jack, roleRole53aOid);
	}

	private class CheckingTaskListener implements TaskListener {

		private String rootTaskOid;
		private Task executing;
		private RuntimeException exception;

		public CheckingTaskListener(String rootTaskOid) {
			this.rootTaskOid = rootTaskOid;
		}

		public RuntimeException getException() {
			return exception;
		}

		@Override
		public synchronized void onTaskStart(Task task) {
			if (!ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI.equals(task.getHandlerUri())) {
				return;
			}
			System.out.println("Starting " + task + ", handler uri " + task.getHandlerUri());
			if (executing != null) {
				exception = new IllegalStateException("Started task " + task + " but another one is already executing: " + executing);
				System.out.println(exception.getMessage());
				// suspend root task in order to fail faster
				taskManager.suspendTasks(Collections.singleton(rootTaskOid), TaskManager.DO_NOT_WAIT, new OperationResult("dummy"));
			}
			executing = task;
		}

		@Override
		public synchronized void onTaskFinish(Task task, TaskRunResult runResult) {
			if (!ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI.equals(task.getHandlerUri())) {
				return;
			}
			System.out.println("Finishing " + task + ", handler uri " + task.getHandlerUri());
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
