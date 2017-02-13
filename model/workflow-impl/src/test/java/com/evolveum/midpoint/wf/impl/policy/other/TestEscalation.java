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
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

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

	protected static final File TASK_TRIGGER_SCANNER_FILE = new File(COMMON_DIR, "task-trigger-scanner.xml");
	protected static final String TASK_TRIGGER_SCANNER_OID = "00000000-0000-0000-0000-000000000007";

	protected static final File TEST_ESCALATION_RESOURCE_DIR = new File("src/test/resources/policy/escalation");
	protected static final File METAROLE_ESCALATED_FILE = new File(TEST_ESCALATION_RESOURCE_DIR, "metarole-escalated.xml");
	protected static final File ROLE_E1_FILE = new File(TEST_ESCALATION_RESOURCE_DIR, "role-e1.xml");

	protected String metaroleEscalatedOid;
	protected String roleE1Oid;
	private PrismObject<UserType> userLead1, userLead2;
	private String workItemId;
	private String approvalTaskOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

		metaroleEscalatedOid = repoAddObjectFromFile(METAROLE_ESCALATED_FILE, initResult).getOid();
		roleE1Oid = repoAddObjectFromFile(ROLE_E1_FILE, initResult).getOid();

		importObjectFromFile(TASK_TRIGGER_SCANNER_FILE);

		userLead1 = getUser(userLead1Oid);
		userLead2 = getUser(userLead2Oid);
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

		approvalTaskOid = workItem.getTaskRef().getOid();
		PrismObject<TaskType> wfTask = getTask(approvalTaskOid);

		display("work item", workItem);
		display("workflow task", wfTask);

		// 5 days: notification
		// D-2 days: escalate
		// D-0 days: approve
		assertEquals("Wrong # of triggers", 3, wfTask.asObjectable().getTrigger().size());

		PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), userLead1Oid);
		PrismAsserts.assertReferenceValue(ref(workItem.getOriginalAssigneeRef()), userLead1Oid);
	}

	@Test
	public void test110Notify() throws Exception {
		final String TEST_NAME = "test110Notify";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clock.overrideDuration("P6D");			// at P5D there's a notify action
		waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

		// TODO assert notifications

		WorkItemType workItem = getWorkItem(task, result);
		display("work item", workItem);
		String wfTaskOid = workItem.getTaskRef().getOid();
		PrismObject<TaskType> wfTask = getTask(wfTaskOid);
		display("task", wfTask);
		assertEquals("Wrong # of triggers", 2, wfTask.asObjectable().getTrigger().size());

		PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), userLead1Oid);
		PrismAsserts.assertReferenceValue(ref(workItem.getOriginalAssigneeRef()), userLead1Oid);
	}

	@Test
	public void test120Escalate() throws Exception {
		final String TEST_NAME = "test120Escalate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clock.resetOverride();
		clock.overrideDuration("P13D");		// at -P2D (i.e. P12D) there is a delegate action
		waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

		WorkItemType workItem = getWorkItem(task, result);
		display("work item", workItem);
		PrismObject<TaskType> wfTask = getTask(workItem.getTaskRef().getOid());
		display("task", wfTask);
		assertEquals("Wrong # of triggers", 1, wfTask.asObjectable().getTrigger().size());

		PrismAsserts.assertReferenceValues(ref(workItem.getAssigneeRef()), userLead1Oid, userLead2Oid);
		PrismAsserts.assertReferenceValue(ref(workItem.getOriginalAssigneeRef()), userLead1Oid);
		assertEquals("Wrong escalation level number", (Integer) 1, workItem.getEscalationLevelNumber());

	}

	@Test
	public void test130Complete() throws Exception {
		final String TEST_NAME = "test130Complete";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		clock.resetOverride();
		clock.overrideDuration("P15D");		// at 0 (i.e. P14D) there is a delegate action
		waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

		PrismObject<TaskType> wfTask = getTask(approvalTaskOid);
		display("task", wfTask);
		assertEquals("Wrong # of triggers", 0, wfTask.asObjectable().getTrigger().size());

		Task rootTask = taskManager.getTaskByIdentifier(wfTask.asObjectable().getParent(), result);
		display("rootTask", rootTask);
		waitForTaskClose(rootTask, 60000);

		assertAssignedRole(userJackOid, roleE1Oid, task, result);
	}

}
