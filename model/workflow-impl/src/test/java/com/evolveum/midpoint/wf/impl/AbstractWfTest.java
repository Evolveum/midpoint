/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.jobs.WfProcessInstanceShadowTaskHandler;
import com.evolveum.midpoint.wf.impl.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.impl.processes.common.WorkflowResult;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequestImpl;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processors.general.GeneralChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.wf.processors.primary.PcpTaskExtensionItemsNames;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBusinessConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UriStack;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfProcessInstanceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
//@DependsOn("workflowServiceImpl")
public class AbstractWfTest extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_RESOURCE_DIR = new File("src/test/resources");
    protected static final String DONT_CHECK = "dont-check";

    @Autowired
	protected Clockwork clockwork;

	@Autowired
    protected TaskManager taskManager;

    @Autowired
    protected WorkflowManagerImpl workflowServiceImpl;

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

    public static final String USERS_AND_ROLES_FILENAME = AbstractIntegrationTest.COMMON_DIR_PATH + "/users-and-roles.xml";
    public static final String ROLE_R1_OID = "00000001-d34d-b33f-f00d-000000000001";
    public static final String ROLE_R2_OID = "00000001-d34d-b33f-f00d-000000000002";
    public static final String ROLE_R3_OID = "00000001-d34d-b33f-f00d-000000000003";
    public static final String ROLE_R4_OID = "00000001-d34d-b33f-f00d-000000000004";
    public static final String USER_BILL_FILENAME = AbstractIntegrationTest.COMMON_DIR_PATH + "/user-bill.xml";
    public static final String USER_BILL_OID = "c0c010c0-d34d-b33f-f00d-11111111111a";
    public static final String ROLE_R10_OID = "00000001-d34d-b33f-f00d-000000000010";

    public static final String R1BOSS_OID = "00000000-d34d-b33f-f00d-111111111111";
    public static final String R2BOSS_OID = "00000000-d34d-b33f-f00d-111111111112";
    public static final String R3BOSS_OID = "00000000-d34d-b33f-f00d-111111111113";
    public static final String DUMMYBOSS_OID = "00000000-d34d-b33f-f00d-111111111333";

    public AbstractWfTest() throws JAXBException {
		super();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {

		super.initSystem(initTask, initResult);
        importObjectFromFile(USERS_AND_ROLES_FILENAME, initResult);

        // add dummyboss as approver for Dummy Resource
        ResourceBusinessConfigurationType businessConfigurationType = new ResourceBusinessConfigurationType(prismContext);
        ObjectReferenceType dummyApproverRef = new ObjectReferenceType();
        dummyApproverRef.setType(UserType.COMPLEX_TYPE);
        dummyApproverRef.setOid(DUMMYBOSS_OID);
        businessConfigurationType.getApproverRef().add(dummyApproverRef);
        ObjectDelta objectDelta = ObjectDelta.createModificationAddContainer(ResourceType.class, RESOURCE_DUMMY_OID, new ItemPath(ResourceType.F_BUSINESS), prismContext, businessConfigurationType);
        repositoryService.modifyObject(ResourceType.class, RESOURCE_DUMMY_OID, objectDelta.getModifications(), initResult);

        // check Role2 approver OID (it is filled-in using search filter)
        List<PrismObject<RoleType>> roles = findRoleInRepoUnchecked("Role2", initResult);
        assertEquals("Wrong number of Role2 objects found in repo", 1, roles.size());
        RoleType role2 = roles.get(0).asObjectable();

//        could be done also like this
//        RoleType role2 = repositoryService.getObject(RoleType.class, ROLE_R2_OID, null, initResult).asObjectable();

        ObjectReferenceType approver = role2.getApprovalSchema().getLevel().get(0).getApproverRef().get(0);
        assertEquals("Wrong OID of Role2's approver", R2BOSS_OID, approver.getOid());
	}

    protected void checkUserApprovers(String oid, List<String> expectedApprovers, OperationResult result) throws SchemaException, ObjectNotFoundException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, oid, null, result);
        checkApprovers(user, expectedApprovers, user.asObjectable().getMetadata().getModifyApproverRef(), result);
    }

    protected void checkUserApproversForCreate(String oid, List<String> expectedApprovers, OperationResult result) throws SchemaException, ObjectNotFoundException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, oid, null, result);
        checkApprovers(user, expectedApprovers, user.asObjectable().getMetadata().getCreateApproverRef(), result);
    }

    protected void checkApproversForCreate(Class<? extends ObjectType> clazz, String oid, List<String> expectedApprovers, OperationResult result) throws SchemaException, ObjectNotFoundException {
        PrismObject<? extends ObjectType> object = repositoryService.getObject(clazz, oid, null, result);
        checkApprovers(object, expectedApprovers, object.asObjectable().getMetadata().getCreateApproverRef(), result);
    }

    protected void checkApprovers(PrismObject<? extends ObjectType> object, List<String> expectedApprovers, List<ObjectReferenceType> realApprovers, OperationResult result) throws SchemaException, ObjectNotFoundException {
        HashSet<String> realApproversSet = new HashSet<String>();
        for (ObjectReferenceType approver : realApprovers) {
            realApproversSet.add(approver.getOid());
            assertEquals("Unexpected target type in approverRef", UserType.COMPLEX_TYPE, approver.getType());
        }
        assertEquals("Mismatch in approvers in metadata", new HashSet(expectedApprovers), realApproversSet);
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
            ObjectDelta<? extends ObjectType> delta = record.getDeltas().iterator().next().getObjectDelta();
            AssignmentType assignmentType = (AssignmentType) ((PrismContainerValue) delta.getModifications().iterator().next().getValuesToAdd().iterator().next()).asContainerable();
            String oid = assignmentType.getTargetRef().getOid();
            assertNotNull("Unexpected role to approve: " + oid, expectedResults.containsKey(oid));
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
            LOGGER.info("Removed assignment " + at + " from " + user);
        }
    }

    protected abstract class TestDetails {
        abstract int subtaskCount();
        abstract boolean immediate();
        abstract boolean checkObjectOnSubtasks();
        boolean approvedAutomatically() { return false; }
        LensContext createModelContext(OperationResult result) throws Exception { return null; }
        void assertsAfterClockworkRun(Task task, OperationResult result) throws Exception { }
        void assertsAfterImmediateExecutionFinished(Task task, OperationResult result) throws Exception { }
        void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception { }
        boolean decideOnApproval(String executionId) throws Exception { return true; }
        String getObjectOid(Task task, OperationResult result) throws SchemaException { return null; };
        boolean removeAssignmentsBeforeTest() { return true; }
    }

    protected boolean decideOnRoleApproval(String executionId) throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException, SecurityViolationException {
        ApprovalRequestImpl<AssignmentType> approvalRequest = (ApprovalRequestImpl<AssignmentType>)
                activitiEngine.getRuntimeService().getVariable(executionId, ProcessVariableNames.APPROVAL_REQUEST);
        assertNotNull("approval request not found", approvalRequest);

        approvalRequest.setPrismContext(prismContext);

        String roleOid = approvalRequest.getItemToApprove().getTargetRef().getOid();
        assertNotNull("requested role OID not found", roleOid);

        if (ROLE_R1_OID.equals(roleOid)) {
            login(getUser(R1BOSS_OID));
            return true;
        } else if (ROLE_R2_OID.equals(roleOid)) {
            login(getUser(R2BOSS_OID));
            return false;
        } else if (ROLE_R3_OID.equals(roleOid)) {
            login(getUser(R3BOSS_OID));
            return true;
        } else {
            throw new AssertionError("Unknown role OID in assignment to be approved: " + roleOid);
        }
    }

    protected void executeTest(String testName, String oid, TestDetails testDetails) throws Exception {

        int workflowSubtaskCount = testDetails.immediate() ? testDetails.subtaskCount()-1 : testDetails.subtaskCount();

		// GIVEN
        prepareNotifications();
        dummyAuditService.clear();

        Task modelTask = taskManager.createTaskInstance(AbstractWfTest.class.getName() + "."+testName);

        OperationResult result = new OperationResult("execution");

        modelTask.setOwner(repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, null, result));

        if (oid != null && testDetails.removeAssignmentsBeforeTest()) {
            removeAllAssignments(oid, result);
        }

        LensContext<UserType> context = (LensContext<UserType>) testDetails.createModelContext(result);

        display("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN

        HookOperationMode mode = clockwork.run(context, modelTask, result);

        // THEN

        assertEquals("Unexpected state of the context", ModelState.PRIMARY, context.getState());
        assertEquals("Wrong mode after clockwork.run in " + context.getState(), HookOperationMode.BACKGROUND, mode);
        modelTask.refresh(result);

        String rootTaskOid = wfTaskUtil.getRootTaskOid(modelTask);
        assertNotNull("Root task OID is not set in model task", rootTaskOid);

        Task rootTask = taskManager.getTask(rootTaskOid, result);
        assertTrue("Root task is not persistent", rootTask.isPersistent());          // trivial ;)

        if (!testDetails.approvedAutomatically()) {

            UriStack uriStack = rootTask.getOtherHandlersUriStack();
            if (!testDetails.immediate()) {
                assertEquals("Invalid handler at stack position 0", ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, uriStack.getUriStackEntry().get(0).getHandlerUri());
            } else {
                assertTrue("There should be no handlers for root tasks with immediate execution mode", uriStack == null || uriStack.getUriStackEntry().isEmpty());
            }

            ModelContext taskModelContext = testDetails.immediate() ? null : wfTaskUtil.retrieveModelContext(rootTask, result);
            if (!testDetails.immediate()) {
                assertNotNull("Model context is not present in root task", taskModelContext);
            } else {
                assertFalse("Model context is present in root task (execution mode = immediate)", wfTaskUtil.hasModelContext(rootTask));
            }

            List<Task> subtasks = rootTask.listSubtasks(result);
            assertEquals("Incorrect number of subtasks", testDetails.subtaskCount(), subtasks.size());

            Task task0 = null;

            for (Task subtask : subtasks) {
                if (!WfProcessInstanceShadowTaskHandler.HANDLER_URI.equals(subtask.getHandlerUri())) {
                    assertNull("More than one non-wf-monitoring subtask", task0);
                    task0 = subtask;
                }
            }

            if (testDetails.immediate()) {
                assertNotNull("Subtask for immediate execution was not found", task0);
                subtasks.remove(task0);
            }

            testDetails.assertsAfterClockworkRun(rootTask, result);

            // ZZZ TEMPORARY
//            checkDummyTransportMessages("simpleWorkflowNotifier-Processes", workflowSubtaskCount);
//            checkDummyTransportMessages("simpleWorkflowNotifier-WorkItems", workflowSubtaskCount);

            if (testDetails.immediate()) {
                waitForTaskClose(task0, 20000);
                //TestUtil.assertSuccess(task0.getResult());            // todo enable this
                testDetails.assertsAfterImmediateExecutionFinished(rootTask, result);
            }

            for (int i = 0; i < subtasks.size(); i++) {
                Task subtask = subtasks.get(i);
                //assertEquals("Subtask #" + i + " is not recurring: " + subtask, TaskRecurrence.RECURRING, subtask.getRecurrenceStatus());
                //assertEquals("Incorrect execution status of subtask #" + i + ": " + subtask, TaskExecutionStatus.RUNNABLE, subtask.getExecutionStatus());
                PrismProperty<ObjectDelta> deltas = subtask.getExtensionProperty(PcpTaskExtensionItemsNames.WFDELTA_TO_PROCESS_PROPERTY_NAME);
                assertNotNull("There are no modifications in subtask #" + i + ": " + subtask, deltas);
                assertEquals("Incorrect number of modifications in subtask #" + i + ": " + subtask, 1, deltas.getRealValues().size());
                // todo check correctness of the modification?

                // now check the workflow state

                String pid = wfTaskUtil.getProcessId(subtask);
                assertNotNull("Workflow process instance id not present in subtask " + subtask, pid);

                WfProcessInstanceType processInstance = workflowServiceImpl.getProcessInstanceById(pid, false, true, result);
                assertNotNull("Process instance information cannot be retrieved", processInstance);
                assertEquals("Incorrect number of work items", 1, processInstance.getWorkItems().size());

                String taskId = processInstance.getWorkItems().get(0).getWorkItemId();
                //WorkItemDetailed workItemDetailed = wfDataAccessor.getWorkItemDetailsById(taskId, result);

                org.activiti.engine.task.Task t = activitiEngine.getTaskService().createTaskQuery().taskId(taskId).singleResult();
                assertNotNull("activiti task not found", t);

                String executionId = t.getExecutionId();
                LOGGER.trace("Task id = " + taskId + ", execution id = " + executionId);

                boolean approve = testDetails.decideOnApproval(executionId);

                workflowServiceImpl.approveOrRejectWorkItem(taskId, approve, result);
                login(userAdministrator);
            }
        }

        waitForTaskClose(rootTask, 60000);
        //TestUtil.assertSuccess(rootTask.getResult());
        testDetails.assertsRootTaskFinishes(rootTask, result);

        if (oid == null) {
            oid = testDetails.getObjectOid(rootTask, result);
        }
        assertNotNull("object oid is null after operation", oid);
        if (!oid.equals(DONT_CHECK)) {
            assertObjectInTaskTree(rootTask, oid, testDetails.checkObjectOnSubtasks(), result);
        }

        if (!testDetails.approvedAutomatically()) {
            // ZZZ temporarily disabled
//            checkDummyTransportMessages("simpleWorkflowNotifier-Processes", workflowSubtaskCount * 2);
//            checkDummyTransportMessages("simpleWorkflowNotifier-WorkItems", workflowSubtaskCount * 2);
        }
        notificationManager.setDisabled(true);

        // Check audit
        display("Audit", dummyAuditService);

        display("Output context", context);
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
            public boolean check() throws Exception {
                task.refresh(waitResult);
                OperationResult result = task.getResult();
                if (verbose) display("Check result", result);
                return task.getExecutionStatus() == TaskExecutionStatus.CLOSED;
            }
            @Override
            public void timeout() {
                try {
                    task.refresh(waitResult);
                } catch (ObjectNotFoundException e) {
                    LOGGER.error("Exception during task refresh: {}", e,e);
                } catch (SchemaException e) {
                    LOGGER.error("Exception during task refresh: {}", e,e);
                }
                OperationResult result = task.getResult();
                LOGGER.debug("Result of timed-out task:\n{}", result.debugDump());
                assert false : "Timeout ("+timeout+") while waiting for "+task+" to finish. Last result "+result;
            }
        };
        IntegrationTestTools.waitFor("Waiting for "+task+" finish", checker, timeout, 1000);
    }

    protected PrismObject<UserType> getUserFromRepo(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return repositoryService.getObject(UserType.class, oid, null, result);
    }

    protected boolean assignmentExists(List<AssignmentType> assignmentList, String targetOid) {
        for (AssignmentType assignmentType : assignmentList) {
            if (assignmentType.getTargetRef() != null && targetOid.equals(assignmentType.getTargetRef().getOid())) {
                return true;
            }
        }
        return false;
    }

    protected PrismObject<UserType> findUserInRepo(String name, OperationResult result) throws SchemaException {
        List<PrismObject<UserType>> users = findUserInRepoUnchecked(name, result);
        assertEquals("Didn't find exactly 1 user object with name " + name, 1, users.size());
        return users.get(0);
    }

    protected List<PrismObject<UserType>> findUserInRepoUnchecked(String name, OperationResult result) throws SchemaException {
        ObjectQuery q = ObjectQuery.createObjectQuery(EqualFilter.createEqual(UserType.F_NAME, UserType.class, prismContext, PolyStringOrigMatchingRule.NAME, new PolyString(name)));
        return repositoryService.searchObjects(UserType.class, q, null, result);
    }

    protected List<PrismObject<RoleType>> findRoleInRepoUnchecked(String name, OperationResult result) throws SchemaException {
        ObjectQuery q = ObjectQuery.createObjectQuery(EqualFilter.createEqual(RoleType.F_NAME, UserType.class, prismContext, PolyStringOrigMatchingRule.NAME, new PolyString(name)));
        return repositoryService.searchObjects(RoleType.class, q, null, result);
    }

    protected void deleteUserFromModel(String name) throws SchemaException, ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

        OperationResult result = new OperationResult("dummy");
        Task t = taskManager.createTaskInstance();
        t.setOwner(repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, null, result));

        if (!findUserInRepoUnchecked(name, result).isEmpty()) {

            PrismObject<UserType> user = findUserInRepo(name, result);

            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
            deltas.add(ObjectDelta.createDeleteDelta(UserType.class, user.getOid(), prismContext));
            modelService.executeChanges(deltas, new ModelExecuteOptions(), t, result);

            LOGGER.info("User " + name + " was deleted");
        }
    }

    <O extends ObjectType> ObjectDelta<O> encryptAndAddFocusModificationToContext(
            LensContext<O> context, File file)
            throws JAXBException, SchemaException, IOException {
        ObjectModificationType modElement = PrismTestUtil.parseAtomicValue(
                file, ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<O> focusDelta = DeltaConvertor.createObjectDelta(
                modElement, context.getFocusClass(), prismContext);
        Utils.encrypt((Collection) Arrays.asList(focusDelta), protector, null, new OperationResult("dummy"));
        return addFocusDeltaToContext(context, focusDelta);
    }

}
