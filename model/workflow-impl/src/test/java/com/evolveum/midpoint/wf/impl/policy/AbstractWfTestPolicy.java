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

package com.evolveum.midpoint.wf.impl.policy;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.AbstractModelImplementationIntegrationTest;
import com.evolveum.midpoint.model.impl.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.processes.common.WorkflowResult;
import com.evolveum.midpoint.wf.impl.processors.general.GeneralChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskUtil;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.util.*;

import static com.evolveum.midpoint.schema.GetOperationOptions.createRetrieve;
import static com.evolveum.midpoint.schema.GetOperationOptions.resolveItemsNamed;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.wf.impl.legacy.AbstractWfTestLegacy.ROLE_R1_OID;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_PROCESSOR_SPECIFIC_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_REQUESTER_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_WORK_ITEM;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfPrimaryChangeProcessorStateType.F_DELTAS_TO_PROCESS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType.*;
import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class AbstractWfTestPolicy extends AbstractModelImplementationIntegrationTest {

    protected static final File TEST_RESOURCE_DIR = new File("src/test/resources/policy");
    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_RESOURCE_DIR, "system-configuration.xml");
    public static final File ROLE_SUPERUSER_FILE = new File(TEST_RESOURCE_DIR, "role-superuser.xml");
    public static final File USER_ADMINISTRATOR_FILE = new File(TEST_RESOURCE_DIR, "user-administrator.xml");

	protected static final File USER_JACK_FILE = new File(TEST_RESOURCE_DIR, "user-jack.xml");
	protected static final File USER_LEAD1_FILE = new File(TEST_RESOURCE_DIR, "user-lead1.xml");
	protected static final File ROLE_ROLE1_FILE = new File(TEST_RESOURCE_DIR, "role-role1.xml");
	protected static final String USER_ADMINISTRATOR_OID = SystemObjectsType.USER_ADMINISTRATOR.value();

	// practically final
	protected static String USER_JACK_OID;
	protected static String USER_LEAD1_OID;
	protected static String ROLE_ROLE1_OID;

	@Autowired
	protected Clockwork clockwork;

	@Autowired
    protected TaskManager taskManager;

    @Autowired
    protected WorkflowManager workflowManager;

    @Autowired
    protected WfTaskUtil wfTaskUtil;

    @Autowired
    protected ActivitiEngine activitiEngine;

    @Autowired
    protected MiscDataUtil miscDataUtil;

    @Autowired
    protected PrimaryChangeProcessor primaryChangeProcessor;

    @Autowired
    protected GeneralChangeProcessor generalChangeProcessor;

	protected PrismObject<UserType> userAdministrator;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
        modelService.postInit(initResult);

        repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
        userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
        login(userAdministrator);

		USER_JACK_OID = repoAddObjectFromFile(USER_JACK_FILE, initResult).getOid();
		ROLE_ROLE1_OID = repoAddObjectFromFile(ROLE_ROLE1_FILE, initResult).getOid();
		USER_LEAD1_OID = repoAddObjectFromFile(USER_LEAD1_FILE, initResult).getOid();
		recomputeUser(USER_LEAD1_OID, initTask, initResult);			// to compute membership data
		display("Lead1", getUser(USER_LEAD1_OID));
    }

    protected Map<String, WorkflowResult> createResultMap(String oid, WorkflowResult result) {
        Map<String,WorkflowResult> retval = new HashMap<String,WorkflowResult>();
        retval.put(oid, result);
        return retval;
    }

    protected Map<String, WorkflowResult> createResultMap(String oid, WorkflowResult approved, String oid2, WorkflowResult approved2) {
        Map<String,WorkflowResult> retval = new HashMap<String,WorkflowResult>();
        retval.put(oid, approved);
        retval.put(oid2, approved2);
        return retval;
    }

    protected Map<String, WorkflowResult> createResultMap(String oid, WorkflowResult approved, String oid2, WorkflowResult approved2, String oid3, WorkflowResult approved3) {
        Map<String,WorkflowResult> retval = new HashMap<String,WorkflowResult>();
        retval.put(oid, approved);
        retval.put(oid2, approved2);
        retval.put(oid3, approved3);
        return retval;
    }

    protected void checkAuditRecords(Map<String, WorkflowResult> expectedResults) {
        checkWorkItemAuditRecords(expectedResults);
        checkWfProcessAuditRecords(expectedResults);
    }

    protected void checkWorkItemAuditRecords(Map<String, WorkflowResult> expectedResults) {
        List<AuditEventRecord> workItemRecords = dummyAuditService.getRecordsOfType(AuditEventType.WORK_ITEM);
        assertEquals("Unexpected number of work item audit records", expectedResults.size()*2, workItemRecords.size());
        for (AuditEventRecord record : workItemRecords) {
            if (record.getEventStage() != AuditEventStage.EXECUTION) {
                continue;
            }
            if (record.getDeltas().size() != 1) {
                fail("Wrong # of deltas in work item audit record: " + record.getDeltas().size());
            }
            ObjectDelta<? extends ObjectType> delta = record.getDeltas().iterator().next().getObjectDelta();
            Containerable valueToAdd = ((PrismContainerValue) delta.getModifications().iterator().next().getValuesToAdd().iterator().next()).asContainerable();
            String oid;
            if (valueToAdd instanceof AssignmentType) {
                oid = ((AssignmentType) valueToAdd).getTargetRef().getOid();
            } else if (valueToAdd instanceof ShadowAssociationType) {
                oid = ((ShadowAssociationType) valueToAdd).getShadowRef().getOid();
            } else {
                continue;
            }
            assertNotNull("Unexpected target to approve: " + oid, expectedResults.containsKey(oid));
            assertEquals("Unexpected result for " + oid, expectedResults.get(oid), WorkflowResult.fromStandardWfAnswer(record.getResult()));
        }
    }

    protected void checkWfProcessAuditRecords(Map<String, WorkflowResult> expectedResults) {
        List<AuditEventRecord> records = dummyAuditService.getRecordsOfType(AuditEventType.WORKFLOW_PROCESS_INSTANCE);
        assertEquals("Unexpected number of workflow process instance audit records", expectedResults.size() * 2, records.size());
        for (AuditEventRecord record : records) {
            if (record.getEventStage() != AuditEventStage.EXECUTION) {
                continue;
            }
            ObjectDelta<? extends ObjectType> delta = record.getDeltas().iterator().next().getObjectDelta();
            if (!delta.getModifications().isEmpty()) {
                AssignmentType assignmentType = (AssignmentType) ((PrismContainerValue) delta.getModifications().iterator().next().getValuesToAdd().iterator().next()).asContainerable();
                String oid = assignmentType.getTargetRef().getOid();
                assertNotNull("Unexpected role to approve: " + oid, expectedResults.containsKey(oid));
                assertEquals("Unexpected result for " + oid, expectedResults.get(oid), WorkflowResult.fromStandardWfAnswer(record.getResult()));
            }
        }
    }

    protected void removeAllAssignments(String oid, OperationResult result) throws Exception {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, oid, null, result);
        for (AssignmentType at : user.asObjectable().getAssignment()) {
            ObjectDelta delta = ObjectDelta.createModificationDeleteContainer(UserType.class, oid, UserType.F_ASSIGNMENT, prismContext, at.asPrismContainerValue().clone());
            repositoryService.modifyObject(UserType.class, oid, delta.getModifications(), result);
            display("Removed assignment " + at + " from " + user);
        }
    }

    protected abstract class TestDetails {
        protected LensContext createModelContext(OperationResult result) throws Exception { return null; }
		protected void afterFirstClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception { }
		protected void afterTask0Finishes(Task task, OperationResult result) throws Exception { }
		protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception { }
		protected boolean executeImmediately() { return false; }
		protected boolean decideOnApproval(String executionId) throws Exception { return true; }
    }

    protected <F extends FocusType> void executeTest(String testName, TestDetails testDetails, int expectedSubTaskCount) throws Exception {

		// GIVEN
        prepareNotifications();
        dummyAuditService.clear();

        Task modelTask = taskManager.createTaskInstance(AbstractWfTestPolicy.class.getName() + "." + testName);
        modelTask.setOwner(userAdministrator);
        OperationResult result = new OperationResult("execution");

        LensContext<F> modelContext = testDetails.createModelContext(result);
        display("Model context at test start", modelContext);

        assertFocusModificationSanity(modelContext);

        // WHEN

        HookOperationMode mode = clockwork.run(modelContext, modelTask, result);

        // THEN

        display("Model context after first clockwork.run", modelContext);
        assertEquals("Unexpected state of the context", ModelState.PRIMARY, modelContext.getState());
        assertEquals("Wrong mode after clockwork.run in " + modelContext.getState(), HookOperationMode.BACKGROUND, mode);
        modelTask.refresh(result);
        display("Model task after first clockwork.run", modelTask);

        String rootTaskOid = wfTaskUtil.getRootTaskOid(modelTask);
        assertNotNull("Root task OID is not set in model task", rootTaskOid);

        Task rootTask = taskManager.getTask(rootTaskOid, result);
        assertTrue("Root task is not persistent", rootTask.isPersistent());

        UriStack uriStack = rootTask.getOtherHandlersUriStack();
        if (!testDetails.executeImmediately()) {
            assertEquals("Invalid handler at stack position 0", ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, uriStack.getUriStackEntry().get(0).getHandlerUri());
        } else {
            assertTrue("There should be no handlers for root tasks with immediate execution mode", uriStack == null || uriStack.getUriStackEntry().isEmpty());
        }

        ModelContext rootModelContext = testDetails.executeImmediately() ? null : wfTaskUtil.getModelContext(rootTask, result);
        if (!testDetails.executeImmediately()) {
            assertNotNull("Model context is not present in root task", rootModelContext);
        } else {
            assertNull("Model context is present in root task (execution mode = immediate)", rootModelContext);
        }

        List<Task> subtasks = rootTask.listSubtasks(result);
        assertEquals("Incorrect number of subtasks", expectedSubTaskCount, subtasks.size());

        Task task0 = findAndRemoveTask0(subtasks, testDetails);

        testDetails.afterFirstClockworkRun(rootTask, subtasks, result);

        if (testDetails.executeImmediately()) {
            waitForTaskClose(task0, 20000);
            testDetails.afterTask0Finishes(rootTask, result);
        }

        for (int i = 0; i < subtasks.size(); i++) {
            Task subtask = subtasks.get(i);
            PrismProperty<ObjectTreeDeltasType> deltas = subtask.getTaskPrismObject().findProperty(new ItemPath(F_WORKFLOW_CONTEXT, F_PROCESSOR_SPECIFIC_STATE, F_DELTAS_TO_PROCESS));
            assertNotNull("There are no modifications in subtask #" + i + ": " + subtask, deltas);
            assertEquals("Incorrect number of modifications in subtask #" + i + ": " + subtask, 1, deltas.getRealValues().size());
            // todo check correctness of the modification?

            // now check the workflow state
            String pid = wfTaskUtil.getProcessId(subtask);
            assertNotNull("Workflow process instance id not present in subtask " + subtask, pid);

//                WfProcessInstanceType processInstance = workflowServiceImpl.getProcessInstanceById(pid, false, true, result);
//                assertNotNull("Process instance information cannot be retrieved", processInstance);
//                assertEquals("Incorrect number of work items", 1, processInstance.getWorkItems().size());

            //String taskId = processInstance.getWorkItems().get(0).getWorkItemId();
            //WorkItemDetailed workItemDetailed = wfDataAccessor.getWorkItemDetailsById(taskId, result);

            org.activiti.engine.task.Task t = activitiEngine.getTaskService().createTaskQuery().processInstanceId(pid).singleResult();
            assertNotNull("activiti task not found", t);

            String executionId = t.getExecutionId();
            display("Execution id = " + executionId);

            boolean approve = testDetails.decideOnApproval(executionId);

            workflowManager.approveOrRejectWorkItem(t.getId(), approve, null, result);
            login(userAdministrator);
        }

        waitForTaskClose(rootTask, 60000);

        subtasks = rootTask.listSubtasks(result);
        findAndRemoveTask0(subtasks, testDetails);
        testDetails.afterRootTaskFinishes(rootTask, subtasks, result);

        notificationManager.setDisabled(true);

        // Check audit
        display("Audit", dummyAuditService);
        display("Output context", modelContext);
	}

    private Task findAndRemoveTask0(List<Task> subtasks, TestDetails testDetails) {
        Task task0 = null;

        for (Task subtask : subtasks) {
			if (subtask.getTaskPrismObject().asObjectable().getWorkflowContext() == null || subtask.getTaskPrismObject().asObjectable().getWorkflowContext().getProcessInstanceId() == null) {
				assertNull("More than one non-wf-monitoring subtask", task0);
				task0 = subtask;
			}
		}

        if (testDetails.executeImmediately()) {
			assertNotNull("Subtask for immediate execution was not found", task0);
			subtasks.remove(task0);
		} else {
			assertNull("Subtask for immediate execution was found even if it shouldn't be there", task0);
		}
        return task0;
    }

    protected void assertObjectInTaskTree(Task rootTask, String oid, boolean checkObjectOnSubtasks, OperationResult result) throws SchemaException {
        assertObjectInTask(rootTask, oid);
        if (checkObjectOnSubtasks) {
            for (Task task : rootTask.listSubtasks(result)) {
                assertObjectInTask(task, oid);
            }
        }
    }

    protected void assertObjectInTask(Task task, String oid) {
        assertEquals("Missing or wrong object OID in task " + task, oid, task.getObjectOid());
    }

    protected void waitForTaskClose(final Task task, final int timeout) throws Exception {
        final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskClose");
        Checker checker = new Checker() {
            @Override
            public boolean check() throws CommonException {
                task.refresh(waitResult);
                OperationResult result = task.getResult();
                if (verbose) display("Check result", result);
                return task.getExecutionStatus() == TaskExecutionStatus.CLOSED;
            }
            @Override
            public void timeout() {
                try {
                    task.refresh(waitResult);
                } catch (Throwable e) {
                    display("Exception during task refresh", e);
                }
				OperationResult result = task.getResult();
                display("Result of timed-out task", result);
                assert false : "Timeout ("+timeout+") while waiting for "+task+" to finish. Last result "+result;
            }
        };
        IntegrationTestTools.waitFor("Waiting for "+task+" finish", checker, timeout, 1000);
    }

	protected void assertWfContextAfterClockworkRun(Task rootTask, List<Task> subtasks, OperationResult result,
			String objectOid,
			String[] targetOids,
			String[] assigneeOids,

			String... processNames) throws Exception {

		final Collection<SelectorOptions<GetOperationOptions>> options =
				SelectorOptions.createCollection(new ItemPath(F_WORKFLOW_CONTEXT, F_WORK_ITEM), createRetrieve());

		Task opTask = taskManager.createTaskInstance();
		TaskType rootTaskType = modelService.getObject(TaskType.class, rootTask.getOid(), options, opTask, result).asObjectable();
		display("rootTask", rootTaskType);
		assertTrue("unexpected process instance id in root task", rootTaskType.getWorkflowContext() == null || rootTaskType.getWorkflowContext().getProcessInstanceId() == null);

		assertEquals("Wrong # of wf subtasks w.r.t processNames (" + Arrays.asList(processNames) + ")", processNames.length, subtasks.size());
		int i = 0;
		for (Task subtask : subtasks) {
			TaskType subtaskType = modelService.getObject(TaskType.class, subtask.getOid(), options, opTask, result).asObjectable();
			display("Subtask #"+(i+1)+": ", subtaskType);
			checkTask(subtaskType, subtask.toString(), processNames[i++]);
			assertRef("requester ref", subtaskType.getWorkflowContext().getRequesterRef(), USER_ADMINISTRATOR_OID, false, false);
		}

		final Collection<SelectorOptions<GetOperationOptions>> options1 = resolveItemsNamed(
				F_OBJECT_REF,
				F_TARGET_REF,
				F_ASSIGNEE_REF,
				new ItemPath(F_TASK_REF, F_WORKFLOW_CONTEXT, F_REQUESTER_REF));

		List<WorkItemType> workItems = modelService.searchContainers(WorkItemType.class, null, options1, opTask, result);
		assertEquals("Wrong # of work items", processNames.length, workItems.size());
		i = 0;
		for (WorkItemType workItem : workItems) {
			display("Work item #"+(i+1)+": ", workItem);
			display("Task ref", workItem.getTaskRef() != null ? workItem.getTaskRef().asReferenceValue().debugDump(0, true) : null);
			assertRef("object reference", workItem.getObjectRef(), objectOid, true, true);
			assertRef("target reference", workItem.getTargetRef(), targetOids[i], true, true);
			assertRef("assignee reference", workItem.getAssigneeRef(), assigneeOids[i], false, true);     // name is not known, as it is not stored in activiti (only OID is)
			assertRef("task reference", workItem.getTaskRef(), null, false, true);
			final TaskType subtaskType = (TaskType) ObjectTypeUtil.getObjectFromReference(workItem.getTaskRef());
			checkTask(subtaskType, "task in workItem", processNames[i++]);
			assertRef("requester ref", subtaskType.getWorkflowContext().getRequesterRef(), USER_ADMINISTRATOR_OID, false, true);
		}
	}

	private void assertRef(String what, ObjectReferenceType ref, String oid, boolean targetName, boolean fullObject) {
		assertNotNull(what + " is null", ref);
		assertNotNull(what + " contains no OID", ref.getOid());
		if (oid != null) {
			assertEquals(what + " contains wrong OID", oid, ref.getOid());
		}
		if (targetName) {
			assertNotNull(what + " contains no target name", ref.getTargetName());
		}
		if (fullObject) {
			assertNotNull(what + " contains no object", ref.asReferenceValue().getObject());
		}
	}


	private void checkTask(TaskType subtaskType, String subtaskName, String processName) {
		assertNull("Unexpected fetch result in wf subtask: " + subtaskName, subtaskType.getFetchResult());
		WfContextType wfc = subtaskType.getWorkflowContext();
		assertNotNull("Missing workflow context in wf subtask: " + subtaskName, wfc);
		assertNotNull("No process ID in wf subtask: " + subtaskName, wfc.getProcessInstanceId());
		assertEquals("Wrong process ID name in subtask: " + subtaskName, processName, wfc.getProcessInstanceName());
		assertNotNull("Missing process start time in subtask: " + subtaskName, wfc.getStartTimestamp());
		assertNull("Unexpected process end time in subtask: " + subtaskName, wfc.getEndTimestamp());
		assertEquals("Wrong 'approved' state", null, wfc.isApproved());
		assertEquals("Wrong answer", null, wfc.getAnswer());
		//assertEquals("Wrong state", null, wfc.getState());
	}

	protected void assertWfContextAfterRootTaskFinishes(Task rootTask, List<Task> subtasks, OperationResult result, String... processNames) throws Exception {

		final Collection<SelectorOptions<GetOperationOptions>> options =
				SelectorOptions.createCollection(new ItemPath(F_WORKFLOW_CONTEXT, F_WORK_ITEM), createRetrieve());

		Task opTask = taskManager.createTaskInstance();
		TaskType rootTaskType = modelService.getObject(TaskType.class, rootTask.getOid(), options, opTask, result).asObjectable();
		assertTrue("unexpected process instance id in root task", rootTaskType.getWorkflowContext() == null || rootTaskType.getWorkflowContext().getProcessInstanceId() == null);

		assertEquals("Wrong # of wf subtasks w.r.t processNames (" + Arrays.asList(processNames) + ")", processNames.length, subtasks.size());
		int i = 0;
		for (Task subtask : subtasks) {
			TaskType subtaskType = modelService.getObject(TaskType.class, subtask.getOid(), options, opTask, result).asObjectable();
			display("Subtask #"+(i+1)+": ", subtaskType);
			assertNull("Unexpected fetch result in wf subtask: " + subtask, subtaskType.getFetchResult());
			WfContextType wfc = subtaskType.getWorkflowContext();
			assertNotNull("Missing workflow context in wf subtask: " + subtask, wfc);
			assertNotNull("No process ID in wf subtask: " + subtask, wfc.getProcessInstanceId());
			assertEquals("Wrong process ID name in subtask: " + subtask, processNames[i++], wfc.getProcessInstanceName());
			assertNotNull("Missing process start time in subtask: " + subtask, wfc.getStartTimestamp());
			assertNotNull("Missing process end time in subtask: " + subtask, wfc.getEndTimestamp());
			assertEquals("Wrong 'approved' state", Boolean.TRUE, wfc.isApproved());
			assertEquals("Wrong answer", ApprovalUtils.DECISION_APPROVED, wfc.getAnswer());
			assertEquals("Wrong state", "Final decision is APPROVED", wfc.getState());
		}
	}

}
