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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.policy.AbstractWfTestPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestEscalation extends AbstractWfTestPolicy {

	@Override
	protected PrismObject<UserType> getDefaultActor() {
		return userAdministrator;
	}

	@Autowired
	private ActivitiEngine activitiEngine;

	@Autowired
	private WorkflowService workflowService;

	protected static final File TEST_ESCALATION_RESOURCE_DIR = new File("src/test/resources/policy/escalation");
	protected static final File METAROLE_ESCALATED_FILE = new File(TEST_ESCALATION_RESOURCE_DIR, "metarole-escalated.xml");
	protected static final File ROLE_E1_FILE = new File(TEST_ESCALATION_RESOURCE_DIR, "role-e1.xml");

	protected String metaroleEscalatedOid;
	protected String roleE1Oid;
	private PrismObject<UserType> userLead1, userLead3;
	private String workItemId;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		metaroleEscalatedOid = repoAddObjectFromFile(METAROLE_ESCALATED_FILE, initResult).getOid();
		roleE1Oid = repoAddObjectFromFile(ROLE_E1_FILE, initResult).getOid();

		userLead1 = getUser(userLead1Oid);
		userLead3 = getUser(userLead3Oid);
	}

	@Test
	public void test100CreateTask() throws Exception {
		final String TEST_NAME = "test100CreateTask";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		assignRole(userJackOid, roleE1Oid, task, result);				// should start approval process
		assertNotAssignedRole(userJackOid, roleE1Oid, task, result);

		WorkItemType workItem = getWorkItem(task, result);
		workItemId = workItem.getWorkItemId();

		String wfTaskOid = workItem.getTaskRef().getOid();
		PrismObject<TaskType> wfTask = getTask(wfTaskOid);

		IntegrationTestTools.display("work item", workItem);
		IntegrationTestTools.display("workflow task", wfTask);

		//PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), userLead1Oid);
	}

}
