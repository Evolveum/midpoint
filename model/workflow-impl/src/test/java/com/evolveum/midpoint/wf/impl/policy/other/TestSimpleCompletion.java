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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.wf.api.WorkflowConstants;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.policy.AbstractWfTestPolicy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
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
public class TestSimpleCompletion extends AbstractWfTestPolicy {

	@Override
	protected PrismObject<UserType> getDefaultActor() {
		return userAdministrator;
	}

	@Autowired
	private ActivitiEngine activitiEngine;

	@Test
	public void test100SimpleApprove() throws Exception {
		final String TEST_NAME = "test100SimpleApprove";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// GIVEN

		dummyAuditService.clear();

		OperationBusinessContextType businessContext = new OperationBusinessContextType();
		final String REQUESTER_COMMENT = "req.comment";
		businessContext.setComment(REQUESTER_COMMENT);

		ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userJackOid, roleRole1aOid, RoleType.COMPLEX_TYPE, null, null, null, true);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		modelService.executeChanges(deltas, ModelExecuteOptions.createRequestBusinessContext(businessContext), task, result);

		assertNotAssignedRole(userJackOid, roleRole1aOid, task, result);

		WorkItemType workItem = getWorkItem(task, result);
		display("Work item", workItem);

		// WHEN
		workflowManager.completeWorkItem(workItem.getWorkItemId(), true, "OK", null, null, result);

		// THEN
		TaskType wfTask = getTask(workItem.getTaskRef().getOid()).asObjectable();
		display("workflow context", wfTask.getWorkflowContext());
		List<WfProcessEventType> events = wfTask.getWorkflowContext().getEvent();
		assertEquals("Wrong # of events", 2, events.size());

		WfProcessCreationEventType event1 = (WfProcessCreationEventType) events.get(0);
		display("Event 1", event1);
		assertEquals("Wrong requester comment", REQUESTER_COMMENT, WfContextUtil.getBusinessContext(wfTask.getWorkflowContext()).getComment());

		WorkItemEventType event2 = (WorkItemEventType) events.get(1);
		display("Event 2", event2);

		assertNotNull("Original assignee is null", event2.getOriginalAssigneeRef());
		assertEquals("Wrong original assignee OID", userLead1Oid, event2.getOriginalAssigneeRef().getOid());

		display("audit", dummyAuditService);
		List<AuditEventRecord> records = dummyAuditService.getRecordsOfType(AuditEventType.WORKFLOW_PROCESS_INSTANCE);
		assertEquals("Wrong # of process instance audit records", 2, records.size());
		for (int i = 0; i < records.size(); i++) {
			AuditEventRecord record = records.get(i);
			assertEquals("Wrong requester comment in audit record #" + i, Collections.singleton(REQUESTER_COMMENT),
					record.getPropertyValues(WorkflowConstants.AUDIT_REQUESTER_COMMENT));
		}
	}

}
