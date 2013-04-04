/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.model.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRecurrence;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.api.ProcessInstance;
import com.evolveum.midpoint.wf.processes.general.ApprovalRequest;
import com.evolveum.midpoint.wf.processes.general.ProcessVariableNames;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.Iterator;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@DependsOn("workflowManager")
public class TestUserChangeApproval extends AbstractInternalModelIntegrationTest {

    private static final String TEST_RESOURCE_DIR_NAME = "src/test/resources";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1 = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-role1.xml";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE2_CHANGE_GN = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-role2-change-gn.xml";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE3_CHANGE_GN2 = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-role3-change-gn2.xml";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLES2_3_4 = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-roles2-3-4.xml";

    protected static final String USER_BILL_FILENAME = COMMON_DIR_NAME + "/user-bill.xml";
    protected static final String USER_BILL_OID = "c0c010c0-d34d-b33f-f00d-11111111111a";

    @Autowired(required = true)
	private Clockwork clockwork;

	@Autowired(required = true)
	private TaskManager taskManager;

    @Autowired(required = true)
    private WorkflowManager workflowManager;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    @Autowired
    private ActivitiEngine activitiEngine;

    public static final String USERS_AND_ROLES_FILENAME = COMMON_DIR_NAME + "/users-and-roles.xml";
    public static final String ROLE_R1_OID = "00000001-d34d-b33f-f00d-000000000001";
    public static final String ROLE_R2_OID = "00000001-d34d-b33f-f00d-000000000002";
    public static final String ROLE_R3_OID = "00000001-d34d-b33f-f00d-000000000003";
    public static final String ROLE_R4_OID = "00000001-d34d-b33f-f00d-000000000004";

	public TestUserChangeApproval() throws JAXBException {
		super();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
        addObjectsFromFile(USERS_AND_ROLES_FILENAME, RoleType.class, initResult);
	}

    /**
     * The simplest case: user modification with one security-sensitive role.
     */
	@Test(enabled = true)
    public void test010UserModifyAddRole() throws Exception {
        displayTestTile(this, "test010UserModifyAddRole");
       	executeTest("test010UserModifyAddRole", 1, false, new ContextCreator() {
               @Override
               public LensContext createModelContext(OperationResult result) throws Exception {
                   LensContext<UserType, ShadowType> context = createUserAccountContext();
                   fillContextWithUser(context, USER_JACK_OID, result);
                   addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1);
                   return context;
               }

               @Override
               public void assertsAfterClockworkRun(Task task, OperationResult result) throws Exception {
                   ModelContext taskModelContext = wfTaskUtil.retrieveModelContext(task, result);
                   assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
                   assertNotAssignedRole(USER_JACK_OID, ROLE_R1_OID, task, result);
               }

               @Override
               void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                   assertAssignedRole(USER_JACK_OID, ROLE_R1_OID, task, result);
               }
           });
	}

    /**
     * User modification with one security-sensitive role and other (unrelated) change - e.g. change of the given name.
     * Aggregated execution.
     */

    @Test(enabled = true)
    public void test011UserModifyAddRoleChangeGivenName() throws Exception {
        displayTestTile(this, "test011UserModifyAddRoleChangeGivenName");
        executeTest("test011UserModifyAddRoleChangeGivenName", 1, false, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType, ShadowType> context = createUserAccountContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE2_CHANGE_GN);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(Task task, OperationResult result) throws Exception {
                ModelContext taskModelContext = wfTaskUtil.retrieveModelContext(task, result);
                assertEquals("There is wrong number of modifications left in primary focus delta", 1, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
                ItemDelta givenNameDelta = (ItemDelta) taskModelContext.getFocusContext().getPrimaryDelta().getModifications().iterator().next();

                assertNotNull("givenName delta is incorrect (not a replace delta)", givenNameDelta.isReplace());
                assertEquals("givenName delta is incorrect (wrong value)", "JACK", ((PrismPropertyValue<PolyString>) givenNameDelta.getValuesToReplace().iterator().next()).getValue().getOrig());

                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, result);
                assertNotAssignedRole(jack, ROLE_R2_OID);
                assertEquals("Wrong given name before change", "Jack", jack.asObjectable().getGivenName().getOrig());
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, result);
                assertNotAssignedRole(jack, ROLE_R2_OID);
                assertEquals("Wrong given name after change", "JACK", jack.asObjectable().getGivenName().getOrig());
            }
        });
    }

    @Test(enabled = true)
    public void test012UserModifyAddRoleChangeGivenNameImmediate() throws Exception {
        displayTestTile(this, "test012UserModifyAddRoleChangeGivenNameImmediate");
        executeTest("test012UserModifyAddRoleChangeGivenNameImmediate", 2, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType, ShadowType> context = createUserAccountContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE3_CHANGE_GN2);
                context.setOptions(ModelExecuteOptions.createExecuteImmediatelyAfterApproval());
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(Task task, OperationResult result) throws Exception {
                assertFalse("There is model context in the root task (it should not be there)", wfTaskUtil.hasModelContext(task));
            }

            @Override
            void assertsAfterImmediateExecutionFinished(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, result);
                assertNotAssignedRole(jack, ROLE_R3_OID);
                assertEquals("Wrong given name after immediate execution", "J-A-C-K", jack.asObjectable().getGivenName().getOrig());
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, result);
                assertAssignedRole(jack, ROLE_R3_OID);
            }
        });
    }


    @Test(enabled = false)
    public void test011UserModifyAddRoles() throws Exception {
        displayTestTile(this, "test011UserModifyAddRoles");
        executeTest("test011UserModifyAddRoles", 2, false, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType, ShadowType> context = createUserAccountContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLES2_3_4);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(Task task, OperationResult result) throws Exception {
                ModelContext taskModelContext = wfTaskUtil.retrieveModelContext(task, result);
                assertEquals("There is wrong number of modifications left in primary focus delta", 2, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
                Iterator<? extends ItemDelta> it = taskModelContext.getFocusContext().getPrimaryDelta().getModifications().iterator();
                ItemDelta addRoleDelta = null, activationChange = null;
                while (it.hasNext()) {
                    ItemDelta mod = it.next();
                    if (mod.isAdd()) {
                        addRoleDelta = mod;
                    } else if (mod.isReplace()) {
                        activationChange = mod;
                    }
                }
                assertNotNull("role add delta was not found", addRoleDelta);
                assertEquals("role add delta contains wrong number of values", 1, addRoleDelta.getValuesToAdd().size());
                assertNotNull("activation change delta was not found", activationChange);
            }
        });
    }

    @Test(enabled = false)
    public void test020UserAdd() throws Exception {
        displayTestTile(this, "test020UserAdd");
        executeTest("test020UserAdd", 2, false, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType, ShadowType> context = createUserAccountContext();
                PrismObject<UserType> bill = prismContext.parseObject(new File(USER_BILL_FILENAME));
                fillContextWithAddUserDelta(context, bill);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(Task task, OperationResult result) throws Exception {
                ModelContext taskModelContext = wfTaskUtil.retrieveModelContext(task, result);
                assertNotNull("There is no object to add left in primary focus delta", taskModelContext.getFocusContext().getPrimaryDelta().getObjectToAdd());
            }
        });
    }


    private abstract class ContextCreator {
        LensContext createModelContext(OperationResult result) throws Exception { return null; }
        void assertsAfterClockworkRun(Task task, OperationResult result) throws Exception { }
        void assertsAfterImmediateExecutionFinished(Task task, OperationResult result) throws Exception { }
        void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception { }
    }
	
	private void executeTest(String testName, int sensitiveRolesAdded, boolean immediate, ContextCreator contextCreator) throws Exception {

		// GIVEN
        Task rootTask = taskManager.createTaskInstance(TestUserChangeApproval.class.getName() + "."+testName);

        OperationResult result = new OperationResult("execution");

        rootTask.setOwner(repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, result));

        LensContext<UserType, ShadowType> context = (LensContext<UserType, ShadowType>) contextCreator.createModelContext(result);

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN

       	HookOperationMode mode = clockwork.run(context, rootTask, result);

        // THEN

        assertEquals("Unexpected state of the context", ModelState.PRIMARY, context.getState());
        assertEquals("Wrong mode after clockwork.run in "+context.getState(), HookOperationMode.BACKGROUND, mode);
        rootTask.refresh(result);

        assertTrue("Task is not persistent", rootTask.isPersistent());

        UriStack uriStack = rootTask.getOtherHandlersUriStack();
        if (!immediate) {
            assertEquals("Invalid handler at stack position 0", ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, uriStack.getUriStackEntry().get(0).getHandlerUri());
            assertEquals("Invalid handler at stack position 1", WfRootTaskHandler.HANDLER_URI, uriStack.getUriStackEntry().get(1).getHandlerUri());
        } else {
            assertTrue("There should be no handlers for root tasks with immediate execution mode", uriStack == null || uriStack.getUriStackEntry().isEmpty());
        }

        ModelContext taskModelContext = immediate ? null : wfTaskUtil.retrieveModelContext(rootTask, result);
        if (!immediate) {
            assertNotNull("Model context is not present in root task", taskModelContext);
        } else {
            assertFalse("Model context is present in root task (ex.mode = immediate)", wfTaskUtil.hasModelContext(rootTask));
        }

        //assertEquals("Invalid current task handler", Wait, uriStack.getUriStackEntry().get(1).getHandlerUri());

        List<Task> subtasks = rootTask.listSubtasks(result);
        assertEquals("Incorrect number of subtasks", sensitiveRolesAdded, subtasks.size());

        Task task0 = null;

        for (Task subtask : subtasks) {
            if (!WfProcessShadowTaskHandler.HANDLER_URI.equals(subtask.getHandlerUri())) {
                assertNull("More than one non-wf-monitoring subtask", task0);
                task0 = subtask;
            }
        }

        if (immediate) {
            assertNotNull("Subtask for immediate execution was not found", task0);
            subtasks.remove(task0);
        }

        contextCreator.assertsAfterClockworkRun(rootTask, result);

        if (immediate) {
            waitForTaskClose(task0, 20000);
            contextCreator.assertsAfterImmediateExecutionFinished(rootTask, result);
        }

        for (int i = 0; i < subtasks.size(); i++) {
            Task subtask = subtasks.get(i);
            assertEquals("Subtask #" + i + " is not recurring: " + subtask, TaskRecurrence.RECURRING, subtask.getRecurrenceStatus());
            assertEquals("Incorrect execution status of subtask #" + i + ": " + subtask, TaskExecutionStatus.RUNNABLE, subtask.getExecutionStatus());
            PrismProperty<ObjectDelta> deltas = subtask.getExtension(WfTaskUtil.WFDELTA_TO_PROCESS_PROPERTY_NAME);
            assertNotNull("There are no modifications in subtask #" + i + ": " + subtask, deltas);
            assertEquals("Incorrect number of modifications in subtask #" + i + ": " + subtask, 1, deltas.getRealValues().size());
            // todo check correctness of the modification?

            // now check the workflow state

            String pid = wfTaskUtil.getProcessId(subtask);
            assertNotNull("Workflow process instance id not present in subtask " + subtask, pid);

            WorkflowServiceImpl workflowServiceImpl = workflowManager.getDataAccessor();
            ProcessInstance processInstance = workflowServiceImpl.getProcessInstanceByInstanceId(pid, false, true, result);
            assertNotNull("Process instance information cannot be retrieved", processInstance);
            assertEquals("Incorrect number of work items", 1, processInstance.getWorkItems().size());

            String taskId = processInstance.getWorkItems().get(0).getTaskId();
            //WorkItemDetailed workItemDetailed = wfDataAccessor.getWorkItemByTaskId(taskId, result);

            org.activiti.engine.task.Task t = activitiEngine.getTaskService().createTaskQuery().taskId(taskId).singleResult();
            assertNotNull("activiti task not found", t);

            String executionId = t.getExecutionId();
            LOGGER.trace("Task id = " + taskId + ", execution id = " + executionId);

            ApprovalRequest<AssignmentType> approvalRequest = (ApprovalRequest<AssignmentType>)
                    activitiEngine.getRuntimeService().getVariable(executionId, ProcessVariableNames.APPROVAL_REQUEST);
            assertNotNull("approval request not found", approvalRequest);

            String roleOid = approvalRequest.getItemToApprove().getTargetRef().getOid();
            assertNotNull("requested role OID not found", roleOid);

            Boolean approve = null;
            if (ROLE_R1_OID.equals(roleOid) || ROLE_R3_OID.equals(roleOid)) {
                approve = true;
            } else if (ROLE_R2_OID.equals(roleOid)) {
                approve = false;
            } else {
                assertTrue("Unknown role OID in assignment to be approved: " + roleOid, false);
            }

            workflowServiceImpl.approveOrRejectWorkItem(taskId, approve, result);
        }

        waitForTaskClose(rootTask, 60000);
        contextCreator.assertsRootTaskFinishes(rootTask, result);

        display("Output context", context);
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
                LOGGER.debug("Result of timed-out task:\n{}", result.dump());
                assert false : "Timeout ("+timeout+") while waiting for "+task+" to finish. Last result "+result;
            }
        };
        IntegrationTestTools.waitFor("Waiting for "+task+" finish", checker, timeout, 1000);
    }


}
