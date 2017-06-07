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

package com.evolveum.midpoint.wf.impl.legacy;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.WfTestUtil;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRef;
import com.evolveum.midpoint.wf.impl.WorkflowResult;
import com.evolveum.midpoint.wf.impl.processors.general.GeneralChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskUtil;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.*;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_PROCESSOR_SPECIFIC_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfPrimaryChangeProcessorStateType.F_DELTAS_TO_PROCESS;
import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
//@DependsOn("workflowServiceImpl")
public class AbstractWfTestLegacy extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_RESOURCE_DIR = new File("src/test/resources/legacy");
    protected static final String DONT_CHECK = "dont-check";

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

    public static final File USERS_AND_ROLES_FILE = new File(TEST_RESOURCE_DIR, "users-and-roles.xml");

    public static final String ROLE_R1_OID = "00000001-d34d-b33f-f00d-000000000001";
    public static final String ROLE_R2_OID = "00000001-d34d-b33f-f00d-000000000002";
    public static final String ROLE_R3_OID = "00000001-d34d-b33f-f00d-000000000003";
    public static final String ROLE_R4_OID = "00000001-d34d-b33f-f00d-000000000004";
    public static final File USER_BILL_FILE = new File(TEST_RESOURCE_DIR, "user-bill.xml");
    public static final String USER_BILL_OID = "c0c010c0-d34d-b33f-f00d-11111111111a";
    public static final String ROLE_R10_OID = "00000001-d34d-b33f-f00d-000000000010";
    public static final String ROLE_R10_SKIP_OID = "00000001-d34d-b33f-f00d-000000000S10";

    public static final File ROLE_R11_FILE = new File(TEST_RESOURCE_DIR, "role11.xml");
    public static final String ROLE_R11_OID = "00000001-d34d-b33f-f00d-000000000011";
    public static final File ROLE_R12_FILE = new File(TEST_RESOURCE_DIR, "role12.xml");
    public static final String ROLE_R12_OID = "00000001-d34d-b33f-f00d-000000000012";
    public static final File ROLE_R13_FILE = new File(TEST_RESOURCE_DIR, "role13.xml");
    public static final String ROLE_R13_OID = "00000001-d34d-b33f-f00d-000000000013";

    public static final String R1BOSS_OID = "00000000-d34d-b33f-f00d-111111111111";
    public static final String R2BOSS_OID = "00000000-d34d-b33f-f00d-111111111112";
    public static final String R3BOSS_OID = "00000000-d34d-b33f-f00d-111111111113";
    public static final String DUMMYBOSS_OID = "00000000-d34d-b33f-f00d-111111111333";

    public static final String GROUP_TESTERS_OID = "20000000-0000-0000-3333-000000000002";
    public static final File GROUP_TESTERS_FILE = new File(TEST_RESOURCE_DIR, "group-testers-dummy.xml");
    public static final String GROUP_TESTERS_NAME = "testers";

    public static final String GROUP_GUESTS_OID = "20000000-0000-0000-3333-000000000072";
    public static final File GROUP_GUESTS_FILE = new File(TEST_RESOURCE_DIR, "/group-guests-dummy.xml");
    public static final String GROUP_GUESTS_NAME = "guests";

    public static final File USER_ELISABETH_FILE = new File(TEST_RESOURCE_DIR, "user-elisabeth.xml");
    public static final String USER_ELISABETH_OID = "c0c010c0-d34d-b33f-f00d-111111112222";

    public static final File ACCOUNT_SHADOW_ELISABETH_DUMMY_FILE = new File(TEST_RESOURCE_DIR, "account-shadow-elisabeth-dummy.xml");

    public AbstractWfTestLegacy() throws JAXBException {
		super();
	}

	protected boolean enablePolicyRuleBasedAspect;

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {

		super.initSystem(initTask, initResult);
        importObjectFromFile(USERS_AND_ROLES_FILE, initResult);
        importObjectFromFile(ROLE_R11_FILE, initResult);
        importObjectFromFile(ROLE_R12_FILE, initResult);
        importObjectFromFile(ROLE_R13_FILE, initResult);

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

        ObjectReferenceType approver = role2.getApprovalSchema().getStage().get(0).getApproverRef().get(0);
        assertEquals("Wrong OID of Role2's approver", R2BOSS_OID, approver.getOid());

        importObjectFromFile(GROUP_TESTERS_FILE, initResult);
        importObjectFromFile(GROUP_GUESTS_FILE, initResult);

        getDummyResourceController().addGroup(GROUP_TESTERS_NAME);
        getDummyResourceController().addGroup(GROUP_GUESTS_NAME);

        display("setting policyRuleBasedAspect.enabled to", enablePolicyRuleBasedAspect);
        List<ItemDelta<?, ?>> deltas =
                DeltaBuilder.deltaFor(SystemConfigurationType.class, prismContext)
                        .item(SystemConfigurationType.F_WORKFLOW_CONFIGURATION, WfConfigurationType.F_PRIMARY_CHANGE_PROCESSOR,
                                PrimaryChangeProcessorConfigurationType.F_POLICY_RULE_BASED_ASPECT, PcpAspectConfigurationType.F_ENABLED)
                        .replace(enablePolicyRuleBasedAspect)
                        .asItemDeltas();
        repositoryService.modifyObject(SystemConfigurationType.class, SYSTEM_CONFIGURATION_OID, deltas, initResult);
        display("policyRuleBasedAspect.enabled was set to", enablePolicyRuleBasedAspect);

        systemObjectCache.invalidateCaches();
    }

    @BeforeClass
    @Parameters({ "enablePolicyRuleBasedAspect" })
    public void temp(@org.testng.annotations.Optional Boolean enablePolicyRuleBasedAspect) {
        this.enablePolicyRuleBasedAspect = BooleanUtils.isNotFalse(enablePolicyRuleBasedAspect);
        System.out.println("Testing with policy rule based aspect = " + this.enablePolicyRuleBasedAspect);
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

    void checkWorkItemAuditRecords(Map<String, WorkflowResult> expectedResults) {
        WfTestUtil.checkWorkItemAuditRecords(expectedResults, dummyAuditService);
    }

    protected void checkWfProcessAuditRecords(Map<String, WorkflowResult> expectedResults) {
	    WfTestUtil.checkWfProcessAuditRecords(expectedResults, dummyAuditService);
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
        LensContext createModelContext(Task task, OperationResult result) throws Exception { return null; }
        void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception { }
        void assertsAfterImmediateExecutionFinished(Task task, OperationResult result) throws Exception { }
        void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception { }
        boolean decideOnApproval(String executionId) throws Exception { return true; }
        String getObjectOid(Task task, OperationResult result) throws SchemaException { return null; };
        boolean removeAssignmentsBeforeTest() { return true; }
    }

    protected boolean decideOnRoleApproval(String executionId) throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        LightweightObjectRef targetRef = (LightweightObjectRef) activitiEngine.getRuntimeService().getVariable(executionId, CommonProcessVariableNames.VARIABLE_TARGET_REF);
        assertNotNull("targetRef not found", targetRef);
        String roleOid = targetRef.getOid();
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

		// GIVEN
        prepareNotifications();
        dummyAuditService.clear();

        Task modelTask = taskManager.createTaskInstance(AbstractWfTestLegacy.class.getName() + "."+testName);

        OperationResult result = new OperationResult("execution");

        modelTask.setOwner(repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, null, result));

        if (oid != null && testDetails.removeAssignmentsBeforeTest()) {
            removeAllAssignments(oid, result);
        }

        LensContext<UserType> context = (LensContext<UserType>) testDetails.createModelContext(modelTask, result);

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

            ModelContext taskModelContext = testDetails.immediate() ? null : wfTaskUtil.getModelContext(rootTask, result);
            if (!testDetails.immediate()) {
                assertNotNull("Model context is not present in root task", taskModelContext);
            } else {
                assertFalse("Model context is present in root task (execution mode = immediate)", wfTaskUtil.hasModelContext(rootTask));
            }

            List<Task> subtasks = rootTask.listSubtasks(result);
            assertEquals("Incorrect number of subtasks", testDetails.subtaskCount(), subtasks.size());

            Task task0 = extractTask0(subtasks, testDetails);

            testDetails.assertsAfterClockworkRun(rootTask, subtasks, result);

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
                LOGGER.trace("Execution id = {}", executionId);

                boolean approve = testDetails.decideOnApproval(executionId);

                workflowManager.completeWorkItem(t.getId(), approve, null, null, null, result);
                login(userAdministrator);
            }
        }

        waitForTaskClose(rootTask, 60000);

        List<Task> subtasks = rootTask.listSubtasks(result);
        extractTask0(subtasks, testDetails);
        //TestUtil.assertSuccess(rootTask.getResult());
        testDetails.assertsRootTaskFinishes(rootTask, subtasks, result);

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

    private Task extractTask0(List<Task> subtasks, TestDetails testDetails) {
        Task task0 = null;

        for (Task subtask : subtasks) {
			if (subtask.getTaskPrismObject().asObjectable().getWorkflowContext() == null || subtask.getTaskPrismObject().asObjectable().getWorkflowContext().getProcessInstanceId() == null) {
				assertNull("More than one non-wf-monitoring subtask", task0);
				task0 = subtask;
			}
		}

        if (testDetails.immediate()) {
			assertNotNull("Subtask for immediate execution was not found", task0);
			subtasks.remove(task0);
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
        } else {
            LOGGER.info("User {} was not found", name);
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
