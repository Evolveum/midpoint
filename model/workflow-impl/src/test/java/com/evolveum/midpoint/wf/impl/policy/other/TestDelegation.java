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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.policy.AbstractWfTestPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.Collections;

import static com.evolveum.midpoint.test.util.TestUtil.assertSuccess;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemDelegationMethodType.ADD_DELEGATES;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

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
		workItemId = workItem.getWorkItemId();

		assertRefEquals("Wrong assigneeRef", ort(userLead1Oid), workItem.getAssigneeRef());
		assertEquals("Wrong # of delegates", 0, workItem.getDelegateRef().size());
	}

	@Test
	public void test110DelegateToUser2Unauthorized() throws Exception {
		final String TEST_NAME = "test110DelegateToUser2Unauthorized";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userLead3);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		try {
			workflowService.delegateWorkItem(workItemId, Collections.singletonList(ort(userLead2Oid)), ADD_DELEGATES, result);
			fail("delegate succeeded even if it shouldn't");
		} catch (SecurityViolationException e) {
			// ok
		}

		WorkItemType workItem = getWorkItem(task, result);
		assertRefEquals("Wrong assigneeRef", ort(userLead1Oid), workItem.getAssigneeRef());
		assertEquals("Wrong # of delegates", 0, workItem.getDelegateRef().size());
	}

	@Test
	public void test120DelegateToUser2() throws Exception {
		final String TEST_NAME = "test120DelegateToUser2";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userLead1);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		workflowService.delegateWorkItem(workItemId, Collections.singletonList(ort(userLead2Oid)), ADD_DELEGATES, result);

		result.computeStatus();
		assertSuccess(result);

		WorkItemType workItem = getWorkItem(task, result);
		assertRefEquals("Wrong assigneeRef", ort(userLead1Oid), workItem.getAssigneeRef());
		assertEquals("Wrong # of delegates", 1, workItem.getDelegateRef().size());
		assertRefEquals("Wrong delegateRef", ort(userLead2Oid), workItem.getDelegateRef().get(0));

	}

	private WorkItemType getWorkItem(Task task, OperationResult result)
			throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
		SearchResultList<WorkItemType> itemsAll = modelService.searchContainers(WorkItemType.class, null, null, task, result);
		assertEquals("Wrong # of total work items", 1, itemsAll.size());
		return itemsAll.get(0);
	}

	private ObjectReferenceType ort(String oid) {
		return ObjectTypeUtil.createObjectRef(oid, ObjectTypes.USER);
	}

	private PrismReferenceValue prv(String oid) {
		return ObjectTypeUtil.createObjectRef(oid, ObjectTypes.USER).asReferenceValue();
	}

}
