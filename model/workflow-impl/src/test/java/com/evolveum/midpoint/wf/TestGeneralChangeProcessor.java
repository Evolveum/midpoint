/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.model.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.activiti.TestAuthenticationInfoHolder;
import com.evolveum.midpoint.wf.api.ProcessInstance;
import com.evolveum.midpoint.wf.api.WorkItemDetailed;
import com.evolveum.midpoint.wf.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WfProcessInstanceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WorkItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.util.List;

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
public class TestGeneralChangeProcessor extends AbstractInternalModelIntegrationTest {

    protected static final Trace LOGGER = TraceManager.getTrace(TestGeneralChangeProcessor.class);

    private static final String TEST_RESOURCE_DIR_NAME = "src/test/resources";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1 = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-role1.xml";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE2_CHANGE_GN = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-role2-change-gn.xml";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE3_CHANGE_GN2 = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-role3-change-gn2.xml";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLES2_3_4 = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-roles2-3-4.xml";
    private static final String REQ_USER_JACK_MODIFY_ACTIVATION_DISABLE = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-activation-disable.xml";
    private static final String REQ_USER_JACK_MODIFY_ACTIVATION_ENABLE = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-activation-enable.xml";
    private static final String REQ_USER_JACK_MODIFY_CHANGE_PASSWORD = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-change-password.xml";
    private static final String REQ_USER_JACK_MODIFY_CHANGE_PASSWORD_2 = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-change-password-2.xml";

    private static final String DONT_CHECK = "dont-check";

    @Autowired(required = true)
	private Clockwork clockwork;

	@Autowired(required = true)
	private TaskManager taskManager;

    @Autowired(required = true)
    private WorkflowManagerImpl workflowServiceImpl;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    @Autowired
    private ActivitiEngine activitiEngine;

    @Autowired
    private MiscDataUtil miscDataUtil;

    public TestGeneralChangeProcessor() throws JAXBException {
		super();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
        importObjectFromFile(TestConstants.USERS_AND_ROLES_FILENAME, initResult);

	}

	@Test(enabled = true)
    public void test010() throws Exception {
        TestUtil.displayTestTile(this, "test010UserModifyAddRole");
       	executeTest("test010UserModifyAddRole", USER_JACK_OID, 1, false, true, new ContextCreator() {
               @Override
               public LensContext createModelContext(OperationResult result) throws Exception {
                   LensContext<UserType, ShadowType> context = createUserAccountContext();
                   fillContextWithUser(context, USER_JACK_OID, result);
                   addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1);
                   return context;
               }
           });
	}

    private abstract class ContextCreator {
        LensContext createModelContext(OperationResult result) throws Exception { return null; }
        void assertsAfterClockworkRun(Task task, OperationResult result) throws Exception { }
        void assertsAfterImmediateExecutionFinished(Task task, OperationResult result) throws Exception { }
        void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception { }
        boolean decideOnApproval(String executionId) throws Exception { return true; }
        String getObjectOid(Task task, OperationResult result) throws SchemaException { return null; };
    }

	private void executeTest(String testName, String oid, int subtaskCount, boolean immediate, boolean checkObjectOnSubtasks, ContextCreator contextCreator) throws Exception {

        int workflowSubtaskCount = immediate ? subtaskCount-1 : subtaskCount;

		// GIVEN
        prepareNotifications();
        dummyAuditService.clear();

        Task rootTask = taskManager.createTaskInstance(TestGeneralChangeProcessor.class.getName() + "."+testName);

        OperationResult result = new OperationResult("execution");

        rootTask.setOwner(repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, null, result));

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

        assertEquals("Invalid current handler", ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, rootTask.getHandlerUri());

        ModelContext taskModelContext = immediate ? null : wfTaskUtil.retrieveModelContext(rootTask, result);
        assertNotNull("Model context is not present in root task", taskModelContext);

        //assertEquals("Invalid current task handler", Wait, uriStack.getUriStackEntry().get(1).getHandlerUri());

        List<Task> subtasks = rootTask.listSubtasks(result);
        assertEquals("Incorrect number of subtasks", subtaskCount, subtasks.size());

        for (int i = 0; i < subtasks.size(); i++) {
            Task subtask = subtasks.get(i);

            // now check the workflow state
            String pid = wfTaskUtil.getProcessId(subtask);
            assertNotNull("Workflow process instance id not present in subtask " + subtask, pid);

            WfProcessInstanceType processInstance = workflowServiceImpl.getProcessInstanceById(pid, false, true, result);
            assertNotNull("Process instance information cannot be retrieved", processInstance);
            assertEquals("Incorrect number of work items", 1, processInstance.getWorkItems().size());

            String taskId = processInstance.getWorkItems().get(0).getWorkItemId();
            WorkItemType workItem = workflowServiceImpl.getWorkItemDetailsById(taskId, result);

            org.activiti.engine.task.Task t = activitiEngine.getTaskService().createTaskQuery().taskId(taskId).singleResult();
            assertNotNull("activiti task not found", t);

            String executionId = t.getExecutionId();
            LOGGER.trace("Task id = " + taskId + ", execution id = " + executionId);

            boolean approve = contextCreator.decideOnApproval(executionId);

            PrismObject<? extends ObjectType> workItemObject = workItem.getRequestSpecificData().asPrismObject();
            LOGGER.trace("workItemObject = " + workItemObject.debugDump());

            // change role1 -> role2
            QName contextQName = new QName(SchemaConstants.NS_WFCF, "modelContext");
            PrismProperty<String> contextProperty = workItemObject.findProperty(contextQName);
            assertNotNull(contextQName + " not found among workItem specific properties", contextProperty);
            String xml = contextProperty.getRealValue();
            xml = xml.replaceAll("00000001-d34d-b33f-f00d-000000000001", "00000001-d34d-b33f-f00d-000000000002");
            contextProperty.setRealValue(xml);

            TestAuthenticationInfoHolder.setUserType(getUser(USER_ADMINISTRATOR_OID).asObjectable());
            workflowServiceImpl.completeWorkItemWithDetails(taskId, workItemObject, "approve", result);
        }

        waitForTaskClose(rootTask, 60000);
        contextCreator.assertsRootTaskFinishes(rootTask, result);
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
        IntegrationTestTools.waitFor("Waiting for " + task + " finish", checker, timeout, 1000);
    }

}
