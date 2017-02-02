/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.policy.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.TaskQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestActivitiQuery extends AbstractWfTestPolicy {

	@Override
	protected PrismObject<UserType> getDefaultActor() {
		return userAdministrator;
	}

	@Autowired
	private ActivitiEngine activitiEngine;

	@Test
	public void test100SearchByMoreAssignees() throws Exception {
		final String TEST_NAME = "test100SearchByMoreAssignees";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		assignRole(userJackOid, roleRole1aOid, task, result);				// should start approval process
		assertNotAssignedRole(userJackOid, roleRole1aOid, task, result);

		{
			SearchResultList<WorkItemType> itemsAll = modelService.searchContainers(WorkItemType.class, null, null, task, result);
			assertEquals("Wrong # of total work items", 1, itemsAll.size());
		}

		{
			ObjectQuery query2 = QueryBuilder.queryFor(WorkItemType.class, prismContext)
					.item(WorkItemType.F_ASSIGNEE_REF).ref(userLead1Oid)
					.build();
			SearchResultList<WorkItemType> items2 = modelService.searchContainers(WorkItemType.class, query2, null, task, result);
			assertEquals("Wrong # of work items found using single-assignee query", 1, items2.size());
		}

		{
			List<PrismReferenceValue> refs = new ArrayList<>();
			refs.add(prv("oid-number-1"));
			refs.add(prv(userLead1Oid));
			refs.add(prv("oid-number-3"));
			ObjectQuery query3 = QueryBuilder.queryFor(WorkItemType.class, prismContext)
					.item(WorkItemType.F_ASSIGNEE_REF).ref(refs)
					.build();
			SearchResultList<WorkItemType> items3 = modelService
					.searchContainers(WorkItemType.class, query3, null, task, result);
			assertEquals("Wrong # of work items found using multi-assignee query", 1, items3.size());
		}
	}

	@Test
	public void test200TestQueryByTaskVariable() throws Exception {
		final String TEST_NAME = "test200TestQueryByTaskVariable";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		TaskService taskService = activitiEngine.getTaskService();
		TaskQuery tq = taskService.createTaskQuery();
		org.activiti.engine.task.Task task = tq.singleResult();
		System.out.println("Task = " + task);
		assertNotNull("No task", task);

		final String TASK_NAME = "Approve assigning Role1a to jack";
		final String VAR = CommonProcessVariableNames.VARIABLE_ADDITIONAL_ASSIGNEES;
		taskService.setVariableLocal(task.getId(), VAR, "[:abc];[:def];[UserType:"+userLead1Oid+"]");
		TaskQuery tq1 = taskService.createTaskQuery().includeTaskLocalVariables()
				.taskVariableValueLike(VAR, "%:def]%")
				.taskName(TASK_NAME);
		org.activiti.engine.task.Task task1 = tq1.singleResult();
		System.out.println("Task1 = " + task1);
		assertNotNull("No task1", task1);

		TaskQuery tq2 = taskService.createTaskQuery().includeTaskLocalVariables().taskVariableValueLike(VAR, "%:xyz]%");
		org.activiti.engine.task.Task task2 = tq2.singleResult();
		System.out.println("Task2 = " + task2);
		assertNull("Found task2 even if it shouldn't", task2);

		TaskQuery tq3 = taskService.createTaskQuery().includeTaskLocalVariables()
				.taskName(TASK_NAME)
				.or()
					.taskVariableValueLike(VAR, "%:ghi]%")
					.taskVariableValueLike(VAR, "%:xxx]%")
					.taskVariableValueLike(VAR, "%:"+userLead1Oid+"]%")
				.endOr();
		org.activiti.engine.task.Task task3 = tq3.singleResult();
		System.out.println("Task3 = " + task3);
		assertNotNull("No task3", task3);

		TaskQuery tq4 = taskService.createTaskQuery().includeTaskLocalVariables()
				.taskName(TASK_NAME)
				.or()
					.taskVariableValueLike(VAR, "%:"+userLead1Oid+"]%")
					.taskVariableValueLike(VAR, "%:xxx]%")
					.taskAssignee(userLead1Oid)
				.endOr();
		org.activiti.engine.task.Task task4 = tq4.singleResult();
		System.out.println("Task4 = " + task4);
		assertNotNull("No task4", task4);

		TaskQuery tq5 = taskService.createTaskQuery().includeTaskLocalVariables()
				.taskName(TASK_NAME)
				.or()
					.taskVariableValueLike(VAR, "%:"+userLead1Oid+"]%")
					.taskVariableValueLike(VAR, "%:xxx]%")
					.taskAssignee("xxx;" + userLead1Oid)
				.endOr();
		org.activiti.engine.task.Task task5 = tq5.singleResult();
		System.out.println("Task5 = " + task5);
		assertNotNull("No task5", task5);

		TaskQuery tq6 = taskService.createTaskQuery().includeTaskLocalVariables()
				.taskName(TASK_NAME)
				.or()
					.taskVariableValueLike(VAR, "%:xxx]%")
					.taskVariableValueLike(VAR, "%:"+userLead1Oid+"]%")
					.taskAssignee("xxx;yyy")
				.endOr();
		org.activiti.engine.task.Task task6 = tq6.singleResult();
		System.out.println("Task6 = " + task6);
		assertNotNull("No task6", task6);

		TaskQuery tq7 = taskService.createTaskQuery().includeTaskLocalVariables()
				.taskName(TASK_NAME)
				.or()
					.taskVariableValueLike(VAR, "%:xxx]%")
					.taskVariableValueLike(VAR, "%:yyy]%")
					.taskAssignee("xxx;" + userLead1Oid)
				.endOr();
		org.activiti.engine.task.Task task7 = tq7.singleResult();
		System.out.println("Task7 = " + task7);
		assertNotNull("No task7", task7);

	}

	@Test
	public void test210SearchByAssignee() throws Exception {
		final String TEST_NAME = "test210SearchByAssignee";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		List<PrismReferenceValue> assigneeRefs = new ArrayList<>();
		assigneeRefs.add(prv("oid-number-1"));
		assigneeRefs.add(prv(userLead1Oid));
		assigneeRefs.add(prv("oid-number-3"));
		assigneeRefs.add(prv("oid-number-4"));
		assigneeRefs.add(prv("oid-number-5"));
		assigneeRefs.add(prv("oid-number-6"));

		ObjectQuery query = QueryBuilder.queryFor(WorkItemType.class, prismContext)
				.item(WorkItemType.F_ASSIGNEE_REF).ref(assigneeRefs)
				.build();
		SearchResultList<WorkItemType> items = modelService
				.searchContainers(WorkItemType.class, query, null, task, result);
		assertEquals("Wrong # of work items found using multi-assignee/multi-delegate query", 1, items.size());
	}

}
