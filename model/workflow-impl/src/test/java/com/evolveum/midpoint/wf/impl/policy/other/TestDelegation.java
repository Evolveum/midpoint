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

import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.policy.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemDelegationEventType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.util.TestUtil.assertSuccess;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemDelegationMethodType.ADD_ASSIGNEES;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemDelegationMethodType.REPLACE_ASSIGNEES;
import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestDelegation extends AbstractWfTestPolicy {

	@Override
	protected PrismObject<UserType> getDefaultActor() {
		return userAdministrator;
	}

	@Autowired
	private ActivitiEngine activitiEngine;

	@Autowired
	private WorkflowService workflowService;

	private PrismObject<UserType> userLead1, userLead3;
	private String workItemId;
	private String taskOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
	}

	@Test
	public void test100CreateTask() throws Exception {
		final String TEST_NAME = "test100CreateTask";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		userLead1 = getUser(userLead1Oid);
		userLead3 = getUser(userLead3Oid);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		assignRole(userJackOid, roleRole1aOid, task, result);				// should start approval process
		assertNotAssignedRole(userJackOid, roleRole1aOid, task, result);

		WorkItemType workItem = getWorkItem(task, result);
		workItemId = workItem.getExternalId();
		taskOid = WfContextUtil.getTask(workItem).getOid();

		display("work item", workItem);
		display("task", getObjectViaRepo(TaskType.class, taskOid));

		PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), userLead1Oid);
	}

	@Test
	public void test110DelegateToUser2Unauthorized() throws Exception {
		final String TEST_NAME = "test110DelegateToUser2Unauthorized";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userLead3);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		try {
			workflowService.delegateWorkItem(workItemId, Collections.singletonList(ort(userLead2Oid)), ADD_ASSIGNEES, result);
			fail("delegate succeeded even if it shouldn't");
		} catch (SecurityViolationException e) {
			// ok
		}

		WorkItemType workItem = getWorkItem(task, result);
		PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), userLead1Oid);
	}

	@Test
	public void test120DelegateToUser2() throws Exception {
		final String TEST_NAME = "test120DelegateToUser2";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userLead1);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		workflowService.delegateWorkItem(workItemId, Collections.singletonList(ort(userLead2Oid)), ADD_ASSIGNEES, result);

		result.computeStatus();
		assertSuccess(result);

		WorkItemType workItem = getWorkItem(task, result);
		display("work item", workItem);

		PrismObject<TaskType> wfTask = getObjectViaRepo(TaskType.class, taskOid);
		display("task", wfTask);

		PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), userLead1Oid, userLead2Oid);
		assertRefEquals("Wrong originalAssigneeRef", ort(userLead1Oid), workItem.getOriginalAssigneeRef());

		org.activiti.engine.task.Task activitiTask = activitiEngine.getTaskService().createTaskQuery()
				.taskId(workItem.getExternalId())
				.singleResult();
		System.out.println("Activiti task: " + activitiTask);
		assertEquals("Wrong activiti assignee", "UserType:"+userLead1Oid, activitiTask.getAssignee());
		List<String> assignees = getAssignees(activitiTask);
		assertEquals("Wrong midpoint-assignee values", new HashSet<>(Arrays.asList("UserType:" + userLead1Oid, "UserType:" + userLead2Oid)),
				new HashSet<>(assignees));

		List<WorkItemDelegationEventType> events = WfContextUtil.getWorkItemEvents(wfTask.asObjectable().getWorkflowContext(), workItemId, WorkItemDelegationEventType.class);
		assertEquals("Wrong # of delegation events", 1, events.size());
		// TODO check content
	}

	private List<String> getAssignees(org.activiti.engine.task.Task activitiTask) {
		return activitiEngine.getTaskService().getIdentityLinksForTask(activitiTask.getId()).stream()
						.filter(i -> CommonProcessVariableNames.MIDPOINT_ASSIGNEE.equals(i.getType()))
						.map(i -> i.getUserId()).collect(Collectors.toList());
	}

	@Test
	public void test130DelegateToUser3ByReplace() throws Exception {
		final String TEST_NAME = "test130DelegateToUser3ByReplace";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userLead1);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		workflowService.delegateWorkItem(workItemId, Collections.singletonList(ort(userLead3Oid)), REPLACE_ASSIGNEES, result);

		result.computeStatus();
		assertSuccess(result);

		WorkItemType workItem = getWorkItem(task, result);
		display("work item", workItem);

		PrismObject<TaskType> wfTask = getObjectViaRepo(TaskType.class, taskOid);
		display("task", wfTask);

		PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), userLead3Oid);
		assertRefEquals("Wrong originalAssigneeRef", ort(userLead1Oid), workItem.getOriginalAssigneeRef());

		org.activiti.engine.task.Task activitiTask = activitiEngine.getTaskService().createTaskQuery()
				.taskId(workItem.getExternalId())
				.singleResult();
		System.out.println("Activiti task: " + activitiTask);
		assertEquals("Wrong activiti assignee", "UserType:"+userLead3Oid, activitiTask.getAssignee());
		List<String> assignees = getAssignees(activitiTask);
		assertEquals("Wrong midpoint-assignee values", Collections.singleton("UserType:" + userLead3Oid),
				new HashSet<>(assignees));

		List<WorkItemDelegationEventType> events = WfContextUtil.getWorkItemEvents(wfTask.asObjectable().getWorkflowContext(), workItemId, WorkItemDelegationEventType.class);
		assertEquals("Wrong # of delegation events", 2, events.size());
		// TODO check content
	}

	@Test
	public void test140DelegateToNoneByReplace() throws Exception {
		final String TEST_NAME = "test140DelegateToNoneByReplace";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userLead3);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		workflowService.delegateWorkItem(workItemId, Collections.emptyList(), REPLACE_ASSIGNEES, result);

		result.computeStatus();
		assertSuccess(result);

		WorkItemType workItem = getWorkItem(task, result);
		display("work item", workItem);

		PrismObject<TaskType> wfTask = getObjectViaRepo(TaskType.class, taskOid);
		display("task", wfTask);

		assertEquals("Wrong assigneeRef count", 0, workItem.getAssigneeRef().size());
		assertRefEquals("Wrong originalAssigneeRef", ort(userLead1Oid), workItem.getOriginalAssigneeRef());

		org.activiti.engine.task.Task activitiTask = activitiEngine.getTaskService().createTaskQuery()
				.taskId(workItem.getExternalId())
				.singleResult();
		System.out.println("Activiti task: " + activitiTask);
		assertEquals("Wrong activiti assignee", null, activitiTask.getAssignee());
		assertEquals("Wrong # of assignees", 0, getAssignees(activitiTask).size());

		List<WorkItemDelegationEventType> events = WfContextUtil.getWorkItemEvents(wfTask.asObjectable().getWorkflowContext(), workItemId, WorkItemDelegationEventType.class);
		assertEquals("Wrong # of delegation events", 3, events.size());
		// TODO check content
	}
}
