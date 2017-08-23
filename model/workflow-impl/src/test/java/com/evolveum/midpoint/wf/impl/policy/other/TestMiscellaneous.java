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

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.wf.api.WorkflowConstants;
import com.evolveum.midpoint.wf.impl.policy.AbstractWfTestPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestMiscellaneous extends AbstractWfTestPolicy {

	@Override
	protected PrismObject<UserType> getDefaultActor() {
		return userAdministrator;
	}

	@Test
	public void test100RequesterComment() throws Exception {
		final String TEST_NAME = "test100RequesterComment";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// GIVEN

		dummyAuditService.clear();

		OperationBusinessContextType businessContext = new OperationBusinessContextType();
		final String REQUESTER_COMMENT = "req.comment";
		businessContext.setComment(REQUESTER_COMMENT);

		ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userJackOid, roleRole2Oid, RoleType.COMPLEX_TYPE, null, null, null, true);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		modelService.executeChanges(deltas, ModelExecuteOptions.createRequestBusinessContext(businessContext), task, result);

		assertNotAssignedRole(userJackOid, roleRole2Oid, task, result);

		WorkItemType workItem = getWorkItem(task, result);
		display("Work item", workItem);

		// WHEN
		workflowManager.completeWorkItem(workItem.getExternalId(), true, "OK", null, null, result);

		// THEN
		TaskType wfTask = getTask(WfContextUtil.getTask(workItem).getOid()).asObjectable();
		display("workflow context", wfTask.getWorkflowContext());
		List<? extends CaseEventType> events = wfTask.getWorkflowContext().getEvent();
		assertEquals("Wrong # of events", 2, events.size());

		CaseCreationEventType event1 = (CaseCreationEventType) events.get(0);
		display("Event 1", event1);
		assertEquals("Wrong requester comment", REQUESTER_COMMENT, WfContextUtil.getBusinessContext(wfTask.getWorkflowContext()).getComment());

		WorkItemEventType event2 = (WorkItemEventType) events.get(1);
		display("Event 2", event2);

		assertNotNull("Original assignee is null", event2.getOriginalAssigneeRef());
		assertEquals("Wrong original assignee OID", userLead2Oid, event2.getOriginalAssigneeRef().getOid());

		display("audit", dummyAuditService);
		List<AuditEventRecord> records = dummyAuditService.getRecordsOfType(AuditEventType.WORKFLOW_PROCESS_INSTANCE);
		assertEquals("Wrong # of process instance audit records", 2, records.size());
		for (int i = 0; i < records.size(); i++) {
			AuditEventRecord record = records.get(i);
			assertEquals("Wrong requester comment in audit record #" + i, Collections.singleton(REQUESTER_COMMENT),
					record.getPropertyValues(WorkflowConstants.AUDIT_REQUESTER_COMMENT));
		}

		Task parent = taskManager.createTaskInstance(wfTask.asPrismObject(), result).getParentTask(result);
		waitForTaskFinish(parent.getOid(), false);

		assertAssignedRole(userJackOid, roleRole2Oid, task, result);
	}

	@Test
	public void test110RequestPrunedRole() throws Exception {
		final String TEST_NAME = "test110RequestPrunedRole";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// GIVEN

		assignRole(RoleType.class, roleRole2Oid, metarolePruneTest2xRolesOid, task, result);
		assignRole(RoleType.class, roleRole2aOid, metarolePruneTest2xRolesOid, task, result);
		assignRole(RoleType.class, roleRole2bOid, metarolePruneTest2xRolesOid, task, result);

		assignRole(RoleType.class, roleRole2Oid, metaroleApproveUnassign, task, result);

		//display("lead2", getUser(userLead2Oid));

		// WHEN

		assignRole(userJackOid, roleRole2aOid, task, result);

		// THEN
		result.computeStatus();
		TestUtil.assertInProgress("Operation NOT in progress", result);

		assertNotAssignedRole(userJackOid, roleRole2aOid, task, result);

		// complete the work item related to assigning role-2a
		WorkItemType workItem = getWorkItem(task, result);
		display("Work item", workItem);
		workflowManager.completeWorkItem(workItem.getExternalId(), true, null, null, null, result);
		TaskType wfTask = getTask(WfContextUtil.getTask(workItem).getOid()).asObjectable();
		Task parent = taskManager.createTaskInstance(wfTask.asPrismObject(), result).getParentTask(result);
		waitForTaskFinish(parent.getOid(), false);

		assertNotAssignedRole(userJackOid, roleRole2Oid, task, result);			// should be pruned without approval
	}

	@Test
	public void test200GetRoleByTemplate() throws Exception {
		final String TEST_NAME = "test200GetRoleByTemplate";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// GIVEN
		setDefaultUserTemplate(userTemplateAssigningRole1aOid);
		unassignAllRoles(userJackOid);

		// WHEN
		// some innocent change
		modifyUserChangePassword(userJackOid, "PaSsWoRd123", task, result);

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertAssignedRole(userJackOid, roleRole1aOid, task, result);
	}

	@Test
	public void test210GetRoleByTemplateAfterAssignments() throws Exception {
		final String TEST_NAME = "test210GetRoleByTemplateAfterAssignments";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// GIVEN
		setDefaultUserTemplate(userTemplateAssigningRole1aOidAfter);
		unassignAllRoles(userJackOid);

		// WHEN
		// some innocent change
		modifyUserChangePassword(userJackOid, "PaSsWoRd123", task, result);
		// here the role1a appears in evaluatedAssignmentsTriple only in secondary phase; so no approvals are triggered

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertAssignedRole(userJackOid, roleRole1aOid, task, result);
	}

	@Test
	public void test220GetRoleByFocusMappings() throws Exception {
		final String TEST_NAME = "test220GetRoleByFocusMappings";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// GIVEN
		setDefaultUserTemplate(null);
		unassignAllRoles(userJackOid);

		// WHEN
		assignRole(userJackOid, roleFocusAssignmentMapping, task, result);

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertAssignedRole(userJackOid, roleRole1aOid, task, result);
	}

	@Test
	public void test250SkippingApprovals() throws Exception {
		final String TEST_NAME = "test250SkippingApprovals";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// GIVEN
		setDefaultUserTemplate(null);
		unassignAllRoles(userJackOid);

		// WHEN
		@SuppressWarnings({"raw", "unchecked"})
		ObjectDelta<? extends ObjectType> delta =
				(ObjectDelta<? extends ObjectType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT)
						.add(ObjectTypeUtil.createAssignmentTo(roleRole1aOid, ObjectTypes.ROLE, prismContext))
				.asObjectDelta(userJackOid);
		ModelExecuteOptions options = ModelExecuteOptions.createPartialProcessing(
				new PartialProcessingOptionsType().approvals(PartialProcessingTypeType.SKIP));
		modelService.executeChanges(Collections.singletonList(delta), options, task, result);

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertAssignedRole(userJackOid, roleRole1aOid, task, result);
	}

}
