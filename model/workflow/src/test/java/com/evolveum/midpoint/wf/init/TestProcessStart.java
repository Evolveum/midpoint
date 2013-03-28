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
package com.evolveum.midpoint.wf.init;

import com.evolveum.midpoint.model.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.model.lens.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.wf.WfRootTaskHandler;
import com.evolveum.midpoint.wf.WfTaskUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
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
public class TestProcessStart extends AbstractInternalModelIntegrationTest {

    private static final String TEST_RESOURCE_DIR_NAME = "src/test/resources";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE_R1 = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-role-r1.xml";

    @Autowired(required = true)
	private Clockwork clockwork;

	@Autowired(required = true)
	private TaskManager taskManager;

    @Autowired(required = true)
    private WfTaskUtil wfTaskUtil;

    public static final String ROLES_FILENAME = COMMON_DIR_NAME + "/roles.xml";
    public static final String ROLE_R1_OID = "00000001-d34d-b33f-f00d-123000000001";

	public TestProcessStart() throws JAXBException {
		super();
	}
		
	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
        addObjectsFromFile(ROLES_FILENAME, RoleType.class, initResult);
	}

	@Test(enabled = true)
    public void test001StartAddRoleProcess() throws Exception {
        displayTestTile(this, "test001StartAddRoleProcess");
       	assignAccountToJackAsync("test001StartAddRoleProcess");
	}
	
	private void assignAccountToJackAsync(String testName) throws SchemaException, ObjectNotFoundException, JAXBException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, IOException, ClassNotFoundException, RewindException {
		displayTestTile(this, testName);
		
		// GIVEN
        Task task = taskManager.createTaskInstance(TestProcessStart.class.getName() + "."+testName);

        OperationResult result = task.getResult();

        task.setOwner(repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, result));

        LensContext<UserType, AccountShadowType> context = createJackAssignAccountContext(result);

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        while(context.getState() != ModelState.FINAL) {

        	HookOperationMode mode = clockwork.click(context, task, result);
        	assertTrue("Unexpected INITIAL state of the context", context.getState() != ModelState.INITIAL);

            if (context.getState() == ModelState.PRIMARY) {

        	    assertEquals("Wrong mode after click in "+context.getState(), HookOperationMode.BACKGROUND, mode);
                task.refresh(result);

                assertTrue("Task is not persistent", task.isPersistent());

                UriStack uriStack = task.getOtherHandlersUriStack();
                assertEquals("Invalid handler at stack position 0", ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, uriStack.getUriStackEntry().get(0).getHandlerUri());
                assertEquals("Invalid handler at stack position 1", WfRootTaskHandler.HANDLER_URI, uriStack.getUriStackEntry().get(1).getHandlerUri());
                ModelContext taskModelContext = wfTaskUtil.retrieveModelContext(task, result);
                assertNotNull("Model context is not present in root task", taskModelContext);

                // delta existence etc etc

                //assertEquals("Invalid current task handler", Wait, uriStack.getUriStackEntry().get(1).getHandlerUri());

                List<Task> subtasks = task.listSubtasks(result);
                assertEquals("Incorrect number of subtasks", 1, subtasks.size());

                //waitForTaskClose(subtasks.get(0), 20000L);
            }

            display("Context", context);
        }
        
        // THEN
        mockClockworkHook.setRecord(false);
        display("Output context", context);
	}
	
	private LensContext<UserType, AccountShadowType> createJackAssignAccountContext(OperationResult result) throws SchemaException, ObjectNotFoundException, FileNotFoundException, JAXBException {
		LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE_R1);
        return context;
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
        IntegrationTestTools.waitFor("Waiting for "+task+" finish", checker, timeout, DEFAULT_TASK_SLEEP_TIME);
    }


}
