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
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
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
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.api.Constants;
import com.evolveum.midpoint.wf.api.ProcessInstance;
import com.evolveum.midpoint.wf.processes.general.ApprovalRequest;
import com.evolveum.midpoint.wf.processes.general.ProcessVariableNames;
import com.evolveum.midpoint.wf.taskHandlers.WfProcessShadowTaskHandler;
import com.evolveum.midpoint.wf.taskHandlers.WfPrepareRootOperationTaskHandler;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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
//@DependsOn("workflowServiceImpl")
public class TestUserChangeApproval extends AbstractInternalModelIntegrationTest {

    protected static final Trace LOGGER = TraceManager.getTrace(TestUserChangeApproval.class);

    private static final String TEST_RESOURCE_DIR_NAME = "src/test/resources";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1 = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-role1.xml";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE2_CHANGE_GN = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-role2-change-gn.xml";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE3_CHANGE_GN2 = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-role3-change-gn2.xml";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLES2_3_4 = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-roles2-3-4.xml";
    private static final String REQ_USER_JACK_MODIFY_ACTIVATION_DISABLE = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-activation-disable.xml";
    private static final String REQ_USER_JACK_MODIFY_ACTIVATION_ENABLE = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-activation-enable.xml";
    private static final String REQ_USER_JACK_MODIFY_CHANGE_PASSWORD = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-change-password.xml";
    private static final String REQ_USER_JACK_MODIFY_CHANGE_PASSWORD_2 = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-change-password-2.xml";

    protected static final String USER_BILL_FILENAME = COMMON_DIR_NAME + "/user-bill.xml";
    protected static final String USER_BILL_OID = "c0c010c0-d34d-b33f-f00d-11111111111a";
    private static final String DONT_CHECK = "dont-check";

    @Autowired(required = true)
	private Clockwork clockwork;

	@Autowired(required = true)
	private TaskManager taskManager;

    @Autowired(required = true)
    private WorkflowServiceImpl workflowServiceImpl;

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
       	executeTest("test010UserModifyAddRole", USER_JACK_OID, 1, false, true, new ContextCreator() {
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

               @Override
               boolean decideOnApproval(String executionId) throws Exception {
                   return decideOnRoleApproval(executionId);
               }
           });
	}

    private void removeAllRoles(String oid, OperationResult result) throws Exception {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, oid, result);
        for (AssignmentType at : user.asObjectable().getAssignment()) {
            ObjectDelta delta = ObjectDelta.createModificationDeleteContainer(UserType.class, oid, UserType.F_ASSIGNMENT, prismContext, at.asPrismContainerValue().clone());
            repositoryService.modifyObject(UserType.class, oid, delta.getModifications(), result);
            LOGGER.info("Removed assignment " + at + " from " + user);
        }
    }

    /**
     * User modification with one security-sensitive role and other (unrelated) change - e.g. change of the given name.
     * Aggregated execution.
     */

    @Test(enabled = true)
    public void test011UserModifyAddRoleChangeGivenName() throws Exception {
        displayTestTile(this, "test011UserModifyAddRoleChangeGivenName");
        executeTest("test011UserModifyAddRoleChangeGivenName", USER_JACK_OID, 1, false, true, new ContextCreator() {
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

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return decideOnRoleApproval(executionId);
            }

        });
    }

    @Test(enabled = true)
    public void test012UserModifyAddRoleChangeGivenNameImmediate() throws Exception {
        displayTestTile(this, "test012UserModifyAddRoleChangeGivenNameImmediate");
        executeTest("test012UserModifyAddRoleChangeGivenNameImmediate", USER_JACK_OID, 2, true, true, new ContextCreator() {
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

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return decideOnRoleApproval(executionId);
            }

        });
    }


    @Test(enabled = true)
    public void test020UserModifyAddThreeRoles() throws Exception {
        displayTestTile(this, "test020UserModifyAddThreeRoles");
        executeTest("test020UserModifyAddThreeRoles", USER_JACK_OID, 2, false, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType, ShadowType> context = createUserAccountContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLES2_3_4);
                addModificationToContext(context, REQ_USER_JACK_MODIFY_ACTIVATION_DISABLE);
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

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = getUserFromRepo(USER_JACK_OID, result);
                assertNotAssignedRole(jack, ROLE_R1_OID);
                assertNotAssignedRole(jack, ROLE_R2_OID);
                assertAssignedRole(jack, ROLE_R3_OID);
                assertAssignedRole(jack, ROLE_R4_OID);
                assertEquals("activation has not been changed", ActivationStatusType.DISABLED, jack.asObjectable().getActivation().getAdministrativeStatus());
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return decideOnRoleApproval(executionId);
            }

        });
    }

    @Test(enabled = true)
    public void test021UserModifyAddThreeRolesImmediate() throws Exception {
        displayTestTile(this, "test021UserModifyAddThreeRolesImmediate");
        executeTest("test021UserModifyAddThreeRolesImmediate", USER_JACK_OID, 3, true, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType, ShadowType> context = createUserAccountContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLES2_3_4);
                addModificationToContext(context, REQ_USER_JACK_MODIFY_ACTIVATION_ENABLE);
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
                assertNotAssignedRole(jack, ROLE_R1_OID);
                assertNotAssignedRole(jack, ROLE_R2_OID);
                assertNotAssignedRole(jack, ROLE_R3_OID);
                assertAssignedRole(jack, ROLE_R4_OID);
                assertEquals("activation has not been changed", ActivationStatusType.ENABLED, jack.asObjectable().getActivation().getAdministrativeStatus());
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = getUserFromRepo(USER_JACK_OID, result);
                assertNotAssignedRole(jack, ROLE_R1_OID);
                assertNotAssignedRole(jack, ROLE_R2_OID);
                assertAssignedRole(jack, ROLE_R3_OID);
                assertAssignedRole(jack, ROLE_R4_OID);
                assertEquals("activation has not been changed", ActivationStatusType.ENABLED, jack.asObjectable().getActivation().getAdministrativeStatus());
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return decideOnRoleApproval(executionId);
            }

        });
    }

    @Test(enabled = true)
    public void test030UserAdd() throws Exception {
        displayTestTile(this, "test030UserAdd");
        executeTest("test030UserAdd", null, 2, false, false, new ContextCreator() {
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
                PrismObject<UserType> objectToAdd = taskModelContext.getFocusContext().getPrimaryDelta().getObjectToAdd();
                assertNotNull("There is no object to add left in primary focus delta", objectToAdd);
                assertFalse("There is assignment of R1 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), ROLE_R1_OID));
                assertFalse("There is assignment of R2 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), ROLE_R2_OID));
                assertFalse("There is assignment of R3 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), ROLE_R3_OID));
                assertTrue("There is no assignment of R4 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), ROLE_R4_OID));
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> bill = findUserInRepo("bill", result);
                assertAssignedRole(bill, ROLE_R1_OID);
                assertNotAssignedRole(bill, ROLE_R2_OID);
                assertNotAssignedRole(bill, ROLE_R3_OID);
                assertAssignedRole(bill, ROLE_R4_OID);
                //assertEquals("Wrong number of assignments for bill", 4, bill.asObjectable().getAssignment().size());
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return decideOnRoleApproval(executionId);
            }

            @Override
            String getObjectOid(Task task, OperationResult result) throws SchemaException {
                //return findUserInRepo("bill", result).getOid();
                return DONT_CHECK;        // don't check in this case
            }
        });
    }

    @Test(enabled = true)
    public void test031UserAddImmediate() throws Exception {
        displayTestTile(this, "test031UserAddImmediate");

        deleteUserFromModel("bill");

        executeTest("test031UserAddImmediate", null, 3, true, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType, ShadowType> context = createUserAccountContext();
                PrismObject<UserType> bill = prismContext.parseObject(new File(USER_BILL_FILENAME));
                fillContextWithAddUserDelta(context, bill);
                context.setOptions(ModelExecuteOptions.createExecuteImmediatelyAfterApproval());
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(Task task, OperationResult result) throws Exception {
                assertFalse("There is model context in the root task (it should not be there)", wfTaskUtil.hasModelContext(task));
            }

            @Override
            void assertsAfterImmediateExecutionFinished(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> bill = findUserInRepo("bill", result);
                assertNotAssignedRole(bill, ROLE_R1_OID);
                assertNotAssignedRole(bill, ROLE_R2_OID);
                assertNotAssignedRole(bill, ROLE_R3_OID);
                assertAssignedRole(bill, ROLE_R4_OID);
                //assertEquals("Wrong number of assignments for bill", 3, bill.asObjectable().getAssignment().size());
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> bill = findUserInRepo("bill", result);
                assertAssignedRole(bill, ROLE_R1_OID);
                assertNotAssignedRole(bill, ROLE_R2_OID);
                assertNotAssignedRole(bill, ROLE_R3_OID);
                assertAssignedRole(bill, ROLE_R4_OID);
                //assertEquals("Wrong number of assignments for bill", 4, bill.asObjectable().getAssignment().size());
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return decideOnRoleApproval(executionId);
            }

            @Override
            String getObjectOid(Task task, OperationResult result) throws SchemaException {
                return findUserInRepo("bill", result).getOid();
            }
        });
    }

    @Test(enabled = true)
    public void test040UserModifyPasswordChangeBlocked() throws Exception {
        displayTestTile(this, "test040UserModifyPasswordChangeBlocked");

        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        final ProtectedStringType originalPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
        LOGGER.trace("password before test = " + originalPasswordValue);

        executeTest("test040UserModifyPasswordChangeBlocked", USER_JACK_OID, 1, false, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType, ShadowType> context = createUserAccountContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addModificationToContext(context, REQ_USER_JACK_MODIFY_CHANGE_PASSWORD);
                context.setOptions(ModelExecuteOptions.createNoCrypt());
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(Task task, OperationResult result) throws Exception {
                ModelContext taskModelContext = wfTaskUtil.retrieveModelContext(task, result);
                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = getUser(USER_JACK_OID);
                ProtectedStringType afterTestPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
                LOGGER.trace("password after test = " + afterTestPasswordValue);

                //assertNotNull("password was not set", afterTestPasswordValue.getEncryptedData());
                assertTrue("password was changed", originalPasswordValue.getEncryptedData().equals(afterTestPasswordValue.getEncryptedData()));
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return false;
            }
        });
    }

    @Test(enabled = true)
    public void test041UserModifyPasswordChange() throws Exception {
        displayTestTile(this, "test041UserModifyPasswordChange");

        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        final ProtectedStringType originalPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
        LOGGER.trace("password before test = " + originalPasswordValue);

        executeTest("test041UserModifyPasswordChange", USER_JACK_OID, 1, false, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType, ShadowType> context = createUserAccountContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addModificationToContext(context, REQ_USER_JACK_MODIFY_CHANGE_PASSWORD);
                context.setOptions(ModelExecuteOptions.createNoCrypt());
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(Task task, OperationResult result) throws Exception {
                ModelContext taskModelContext = wfTaskUtil.retrieveModelContext(task, result);
                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = getUser(USER_JACK_OID);
                ProtectedStringType afterTestPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
                LOGGER.trace("password after test = " + afterTestPasswordValue);

                //assertNotNull("password was not set", afterTestPasswordValue.getEncryptedData());
                assertFalse("password was not changed", originalPasswordValue.getEncryptedData().equals(afterTestPasswordValue.getEncryptedData()));
            }
        });
    }

    @Test(enabled = true)
    public void test050UserModifyAddRoleAndPasswordChange() throws Exception {
        displayTestTile(this, "test050UserModifyAddRoleAndPasswordChange");

        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        final ProtectedStringType originalPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
        LOGGER.trace("password before test = " + originalPasswordValue);

        executeTest("test050UserModifyAddRoleAndPasswordChange", USER_JACK_OID, 2, false, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType, ShadowType> context = createUserAccountContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addModificationToContext(context, REQ_USER_JACK_MODIFY_CHANGE_PASSWORD_2);
                addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1);
                context.setOptions(ModelExecuteOptions.createNoCrypt());
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(Task task, OperationResult result) throws Exception {
                ModelContext taskModelContext = wfTaskUtil.retrieveModelContext(task, result);
                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = getUser(USER_JACK_OID);
                ProtectedStringType afterTestPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
                LOGGER.trace("password after test = " + afterTestPasswordValue);

                // todo why is password value not set?
                //assertNotNull("password was not set", afterTestPasswordValue.getEncryptedData());
                //assertFalse("password was not changed", originalPasswordValue.getEncryptedData().equals(afterTestPasswordValue.getEncryptedData()));
                assertAssignedRole(jack, ROLE_R1_OID);
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

    private boolean decideOnRoleApproval(String executionId) {
        ApprovalRequest<AssignmentType> approvalRequest = (ApprovalRequest<AssignmentType>)
                activitiEngine.getRuntimeService().getVariable(executionId, ProcessVariableNames.APPROVAL_REQUEST);
        assertNotNull("approval request not found", approvalRequest);

        String roleOid = approvalRequest.getItemToApprove().getTargetRef().getOid();
        assertNotNull("requested role OID not found", roleOid);

        if (ROLE_R1_OID.equals(roleOid) || ROLE_R3_OID.equals(roleOid)) {
            return true;
        } else if (ROLE_R2_OID.equals(roleOid)) {
            return false;
        } else {
            throw new AssertionError("Unknown role OID in assignment to be approved: " + roleOid);
        }
    }
	
	private void executeTest(String testName, String oid, int sensitiveRolesAdded, boolean immediate, boolean checkObjectOnSubtasks, ContextCreator contextCreator) throws Exception {

		// GIVEN
        Task rootTask = taskManager.createTaskInstance(TestUserChangeApproval.class.getName() + "."+testName);

        OperationResult result = new OperationResult("execution");

        rootTask.setOwner(repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, result));

        if (oid != null) {
            removeAllRoles(oid, result);
        }

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
            assertEquals("Invalid current handler", WfPrepareRootOperationTaskHandler.HANDLER_URI, rootTask.getHandlerUri());
        } else {
            assertTrue("There should be no handlers for root tasks with immediate execution mode", uriStack == null || uriStack.getUriStackEntry().isEmpty());
        }

        ModelContext taskModelContext = immediate ? null : wfTaskUtil.retrieveModelContext(rootTask, result);
        if (!immediate) {
            assertNotNull("Model context is not present in root task", taskModelContext);
        } else {
            assertFalse("Model context is present in root task (execution mode = immediate)", wfTaskUtil.hasModelContext(rootTask));
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
            //assertEquals("Subtask #" + i + " is not recurring: " + subtask, TaskRecurrence.RECURRING, subtask.getRecurrenceStatus());
            //assertEquals("Incorrect execution status of subtask #" + i + ": " + subtask, TaskExecutionStatus.RUNNABLE, subtask.getExecutionStatus());
            PrismProperty<ObjectDelta> deltas = subtask.getExtension(Constants.WFDELTA_TO_PROCESS_PROPERTY_NAME);
            assertNotNull("There are no modifications in subtask #" + i + ": " + subtask, deltas);
            assertEquals("Incorrect number of modifications in subtask #" + i + ": " + subtask, 1, deltas.getRealValues().size());
            // todo check correctness of the modification?

            // now check the workflow state

            String pid = wfTaskUtil.getProcessId(subtask);
            assertNotNull("Workflow process instance id not present in subtask " + subtask, pid);

            ProcessInstance processInstance = workflowServiceImpl.getProcessInstanceByInstanceId(pid, false, true, result);
            assertNotNull("Process instance information cannot be retrieved", processInstance);
            assertEquals("Incorrect number of work items", 1, processInstance.getWorkItems().size());

            String taskId = processInstance.getWorkItems().get(0).getTaskId();
            //WorkItemDetailed workItemDetailed = wfDataAccessor.getWorkItemDetailsByTaskId(taskId, result);

            org.activiti.engine.task.Task t = activitiEngine.getTaskService().createTaskQuery().taskId(taskId).singleResult();
            assertNotNull("activiti task not found", t);

            String executionId = t.getExecutionId();
            LOGGER.trace("Task id = " + taskId + ", execution id = " + executionId);

            boolean approve = contextCreator.decideOnApproval(executionId);

            workflowServiceImpl.approveOrRejectWorkItem(taskId, approve, result);
        }

        waitForTaskClose(rootTask, 60000);
        contextCreator.assertsRootTaskFinishes(rootTask, result);

        if (oid == null) {
            oid = contextCreator.getObjectOid(rootTask, result);
        }
        assertNotNull("object oid is null after operation", oid);
        if (!oid.equals(DONT_CHECK)) {
            assertObjectInTaskTree(rootTask, oid, checkObjectOnSubtasks, result);
        }

        display("Output context", context);
	}

    private void assertObjectInTaskTree(Task rootTask, String oid, boolean checkObjectOnSubtasks, OperationResult result) throws SchemaException {
        assertObjectInTask(rootTask, oid);
        if (checkObjectOnSubtasks) {
            for (Task task : rootTask.listSubtasks(result)) {
                assertObjectInTask(task, oid);
            }
        }
    }

    private void assertObjectInTask(Task task, String oid) {
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
                LOGGER.debug("Result of timed-out task:\n{}", result.dump());
                assert false : "Timeout ("+timeout+") while waiting for "+task+" to finish. Last result "+result;
            }
        };
        IntegrationTestTools.waitFor("Waiting for "+task+" finish", checker, timeout, 1000);
    }

    private PrismObject<UserType> getUserFromRepo(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return repositoryService.getObject(UserType.class, oid, result);
    }

    private boolean assignmentExists(List<AssignmentType> assignmentList, String targetOid) {
        for (AssignmentType assignmentType : assignmentList) {
            if (assignmentType.getTargetRef() != null && targetOid.equals(assignmentType.getTargetRef().getOid())) {
                return true;
            }
        }
        return false;
    }

    private PrismObject<UserType> findUserInRepo(String name, OperationResult result) throws SchemaException {
        List<PrismObject<UserType>> users = findUserInRepoUnchecked(name, result);
        assertEquals("Didn't find exactly 1 user object with name " + name, 1, users.size());
        return users.get(0);
    }

    private List<PrismObject<UserType>> findUserInRepoUnchecked(String name, OperationResult result) throws SchemaException {
        ObjectQuery q = ObjectQuery.createObjectQuery(EqualsFilter.createEqual(UserType.class, prismContext, UserType.F_NAME, name));
        return repositoryService.searchObjects(UserType.class, q, result);
    }

    private void deleteUserFromModel(String name) throws SchemaException, ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

        OperationResult result = new OperationResult("dummy");
        Task t = taskManager.createTaskInstance();
        t.setOwner(repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, result));

        if (!findUserInRepoUnchecked(name, result).isEmpty()) {

            PrismObject<UserType> user = findUserInRepo(name, result);

            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
            deltas.add(ObjectDelta.createDeleteDelta(UserType.class, user.getOid(), prismContext));
            modelService.executeChanges(deltas, new ModelExecuteOptions(), t, result);

            LOGGER.info("User " + name + " was deleted");
        }
    }

}
