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

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContainerValue;
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
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.activiti.TestAuthenticationInfoHolder;
import com.evolveum.midpoint.wf.api.Constants;
import com.evolveum.midpoint.wf.api.ProcessInstance;
import com.evolveum.midpoint.wf.processes.WorkflowResult;
import com.evolveum.midpoint.wf.processes.itemApproval.ApprovalRequestImpl;
import com.evolveum.midpoint.wf.processes.itemApproval.ProcessVariableNames;
import com.evolveum.midpoint.wf.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.jobs.WfProcessInstanceShadowTaskHandler;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.*;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
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

    @Autowired
    private MiscDataUtil miscDataUtil;

    public TestUserChangeApproval() throws JAXBException {
		super();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
        importObjectFromFile(TestConstants.USERS_AND_ROLES_FILENAME, initResult);

        // check Role2 approver OID (it is filled-in using search filter)
        List<PrismObject<RoleType>> roles = findRoleInRepoUnchecked("Role2", initResult);
        assertEquals("Wrong number of Role2 objects found in repo", 1, roles.size());
        RoleType role2 = roles.get(0).asObjectable();

//        could be done also like this
//        RoleType role2 = repositoryService.getObject(RoleType.class, TestConstants.ROLE_R2_OID, null, initResult).asObjectable();

        ObjectReferenceType approver = role2.getApprovalSchema().getLevel().get(0).getApproverRef().get(0);
        assertEquals("Wrong OID of Role2's approver", TestConstants.R2BOSS_OID, approver.getOid());
	}

    /**
     * The simplest case: user modification with one security-sensitive role.
     */
	@Test(enabled = true)
    public void test010UserModifyAddRole() throws Exception {
        TestUtil.displayTestTile(this, "test010UserModifyAddRole");
       	executeTest("test010UserModifyAddRole", USER_JACK_OID, 1, false, true, new ContextCreator() {
               @Override
               public LensContext createModelContext(OperationResult result) throws Exception {
                   LensContext<UserType> context = createUserAccountContext();
                   fillContextWithUser(context, USER_JACK_OID, result);
                   addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1);
                   return context;
               }

               @Override
               public void assertsAfterClockworkRun(Task task, OperationResult result) throws Exception {
                   ModelContext taskModelContext = wfTaskUtil.retrieveModelContext(task, result);
                   assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
                   assertNotAssignedRole(USER_JACK_OID, TestConstants.ROLE_R1_OID, task, result);
               }

               @Override
               void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                   assertAssignedRole(USER_JACK_OID, TestConstants.ROLE_R1_OID, task, result);
                   checkDummyTransportMessages("simpleUserNotifier", 1);
                   checkWorkItemAuditRecords(createResultMap(TestConstants.ROLE_R1_OID, WorkflowResult.APPROVED));
                   checkUserApprovers(USER_JACK_OID, Arrays.asList(TestConstants.R1BOSS_OID), result);
               }

               @Override
               boolean decideOnApproval(String executionId) throws Exception {
                   return decideOnRoleApproval(executionId);
               }
           });
	}

    private void checkUserApprovers(String oid, List<String> expectedApprovers, OperationResult result) throws SchemaException, ObjectNotFoundException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, oid, null, result);
        checkUserApprovers(user, expectedApprovers, user.asObjectable().getMetadata().getModifyApproverRef(), result);
    }

    private void checkUserApproversForCreate(String oid, List<String> expectedApprovers, OperationResult result) throws SchemaException, ObjectNotFoundException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, oid, null, result);
        checkUserApprovers(user, expectedApprovers, user.asObjectable().getMetadata().getCreateApproverRef(), result);
    }

    private void checkUserApprovers(PrismObject<UserType> user, List<String> expectedApprovers, List<ObjectReferenceType> realApprovers, OperationResult result) throws SchemaException, ObjectNotFoundException {
        HashSet<String> realApproversSet = new HashSet<String>();
        for (ObjectReferenceType approver : realApprovers) {
            realApproversSet.add(approver.getOid());
        }
        assertEquals("Mismatch in modifyApproverRef in metadata", new HashSet(expectedApprovers), realApproversSet);
    }

    private Map<String, WorkflowResult> createResultMap(String oid, WorkflowResult result) {
        Map<String,WorkflowResult> retval = new HashMap<String,WorkflowResult>();
        retval.put(oid, result);
        return retval;
    }

    private Map<String, WorkflowResult> createResultMap(String oid, WorkflowResult approved, String oid2, WorkflowResult approved2) {
        Map<String,WorkflowResult> retval = new HashMap<String,WorkflowResult>();
        retval.put(oid, approved);
        retval.put(oid2, approved2);
        return retval;
    }

    private Map<String, WorkflowResult> createResultMap(String oid, WorkflowResult approved, String oid2, WorkflowResult approved2, String oid3, WorkflowResult approved3) {
        Map<String,WorkflowResult> retval = new HashMap<String,WorkflowResult>();
        retval.put(oid, approved);
        retval.put(oid2, approved2);
        retval.put(oid3, approved3);
        return retval;
    }

    private void checkAuditRecords(Map<String, WorkflowResult> expectedResults) {
        checkWorkItemAuditRecords(expectedResults);
        checkWfProcessAuditRecords(expectedResults);
    }

    private void checkWorkItemAuditRecords(Map<String, WorkflowResult> expectedResults) {
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

    private void checkWfProcessAuditRecords(Map<String, WorkflowResult> expectedResults) {
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

    private void removeAllRoles(String oid, OperationResult result) throws Exception {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, oid, null, result);
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
        TestUtil.displayTestTile(this, "test011UserModifyAddRoleChangeGivenName");
        executeTest("test011UserModifyAddRoleChangeGivenName", USER_JACK_OID, 1, false, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType> context = createUserAccountContext();
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

                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
                assertNotAssignedRole(jack, TestConstants.ROLE_R2_OID);
                assertEquals("Wrong given name before change", "Jack", jack.asObjectable().getGivenName().getOrig());
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
                assertNotAssignedRole(jack, TestConstants.ROLE_R2_OID);
                assertEquals("Wrong given name after change", "JACK", jack.asObjectable().getGivenName().getOrig());

                checkDummyTransportMessages("simpleUserNotifier", 1);
                checkWorkItemAuditRecords(createResultMap(TestConstants.ROLE_R2_OID, WorkflowResult.REJECTED));
                checkUserApprovers(USER_JACK_OID, new ArrayList<String>(), result);
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return decideOnRoleApproval(executionId);
            }

        });
    }

    @Test(enabled = true)
    public void test012UserModifyAddRoleChangeGivenNameImmediate() throws Exception {
        TestUtil.displayTestTile(this, "test012UserModifyAddRoleChangeGivenNameImmediate");
        executeTest("test012UserModifyAddRoleChangeGivenNameImmediate", USER_JACK_OID, 2, true, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType> context = createUserAccountContext();
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
                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
                assertNotAssignedRole(jack, TestConstants.ROLE_R3_OID);
                assertEquals("Wrong given name after immediate execution", "J-A-C-K", jack.asObjectable().getGivenName().getOrig());
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
                assertAssignedRole(jack, TestConstants.ROLE_R3_OID);

                checkDummyTransportMessages("simpleUserNotifier", 2);
                checkWorkItemAuditRecords(createResultMap(TestConstants.ROLE_R3_OID, WorkflowResult.APPROVED));
                checkUserApprovers(USER_JACK_OID, Arrays.asList(TestConstants.R3BOSS_OID), result);         // given name is changed before role is added, so the approver should be recorded
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return decideOnRoleApproval(executionId);
            }

        });
    }


    @Test(enabled = true)
    public void test020UserModifyAddThreeRoles() throws Exception {
        TestUtil.displayTestTile(this, "test020UserModifyAddThreeRoles");
        executeTest("test020UserModifyAddThreeRoles", USER_JACK_OID, 2, false, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType> context = createUserAccountContext();
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
                assertNotAssignedRole(jack, TestConstants.ROLE_R1_OID);
                assertNotAssignedRole(jack, TestConstants.ROLE_R2_OID);
                assertAssignedRole(jack, TestConstants.ROLE_R3_OID);
                assertAssignedRole(jack, TestConstants.ROLE_R4_OID);
                assertEquals("activation has not been changed", ActivationStatusType.DISABLED, jack.asObjectable().getActivation().getAdministrativeStatus());

                checkDummyTransportMessages("simpleUserNotifier", 1);
                checkWorkItemAuditRecords(createResultMap(TestConstants.ROLE_R2_OID, WorkflowResult.REJECTED, TestConstants.ROLE_R3_OID, WorkflowResult.APPROVED));
                checkUserApprovers(USER_JACK_OID, Arrays.asList(TestConstants.R3BOSS_OID), result);
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return decideOnRoleApproval(executionId);
            }

        });
    }

    @Test(enabled = true)
    public void test021UserModifyAddThreeRolesImmediate() throws Exception {
        TestUtil.displayTestTile(this, "test021UserModifyAddThreeRolesImmediate");
        executeTest("test021UserModifyAddThreeRolesImmediate", USER_JACK_OID, 3, true, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType> context = createUserAccountContext();
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
                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
                assertNotAssignedRole(jack, TestConstants.ROLE_R1_OID);
                assertNotAssignedRole(jack, TestConstants.ROLE_R2_OID);
                assertNotAssignedRole(jack, TestConstants.ROLE_R3_OID);
                assertAssignedRole(jack, TestConstants.ROLE_R4_OID);
                assertEquals("activation has not been changed", ActivationStatusType.ENABLED, jack.asObjectable().getActivation().getAdministrativeStatus());
                checkUserApprovers(USER_JACK_OID, new ArrayList<String>(), result);
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = getUserFromRepo(USER_JACK_OID, result);
                assertNotAssignedRole(jack, TestConstants.ROLE_R1_OID);
                assertNotAssignedRole(jack, TestConstants.ROLE_R2_OID);
                assertAssignedRole(jack, TestConstants.ROLE_R3_OID);
                assertAssignedRole(jack, TestConstants.ROLE_R4_OID);
                assertEquals("activation has not been changed", ActivationStatusType.ENABLED, jack.asObjectable().getActivation().getAdministrativeStatus());

                checkDummyTransportMessages("simpleUserNotifier", 2);
                checkWorkItemAuditRecords(createResultMap(TestConstants.ROLE_R2_OID, WorkflowResult.REJECTED, TestConstants.ROLE_R3_OID, WorkflowResult.APPROVED));
                checkUserApprovers(USER_JACK_OID, Arrays.asList(TestConstants.R3BOSS_OID), result);
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                return decideOnRoleApproval(executionId);
            }

        });
    }

    @Test(enabled = true)
    public void test030UserAdd() throws Exception {
        TestUtil.displayTestTile(this, "test030UserAdd");
        executeTest("test030UserAdd", null, 2, false, false, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType> context = createUserAccountContext();
                PrismObject<UserType> bill = prismContext.parseObject(new File(TestConstants.USER_BILL_FILENAME));
                fillContextWithAddUserDelta(context, bill);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(Task task, OperationResult result) throws Exception {
                ModelContext taskModelContext = wfTaskUtil.retrieveModelContext(task, result);
                PrismObject<UserType> objectToAdd = taskModelContext.getFocusContext().getPrimaryDelta().getObjectToAdd();
                assertNotNull("There is no object to add left in primary focus delta", objectToAdd);
                assertFalse("There is assignment of R1 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), TestConstants.ROLE_R1_OID));
                assertFalse("There is assignment of R2 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), TestConstants.ROLE_R2_OID));
                assertFalse("There is assignment of R3 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), TestConstants.ROLE_R3_OID));
                assertTrue("There is no assignment of R4 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), TestConstants.ROLE_R4_OID));
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> bill = findUserInRepo("bill", result);
                assertAssignedRole(bill, TestConstants.ROLE_R1_OID);
                assertNotAssignedRole(bill, TestConstants.ROLE_R2_OID);
                assertNotAssignedRole(bill, TestConstants.ROLE_R3_OID);
                assertAssignedRole(bill, TestConstants.ROLE_R4_OID);
                //assertEquals("Wrong number of assignments for bill", 4, bill.asObjectable().getAssignment().size());

                checkDummyTransportMessages("simpleUserNotifier", 1);
                checkWorkItemAuditRecords(createResultMap(TestConstants.ROLE_R1_OID, WorkflowResult.APPROVED, TestConstants.ROLE_R2_OID, WorkflowResult.REJECTED));
                checkUserApproversForCreate(bill.getOid(), Arrays.asList(TestConstants.R1BOSS_OID), result);
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
        TestUtil.displayTestTile(this, "test031UserAddImmediate");

        deleteUserFromModel("bill");

        executeTest("test031UserAddImmediate", null, 3, true, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType> context = createUserAccountContext();
                PrismObject<UserType> bill = prismContext.parseObject(new File(TestConstants.USER_BILL_FILENAME));
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
                assertNotAssignedRole(bill, TestConstants.ROLE_R1_OID);
                assertNotAssignedRole(bill, TestConstants.ROLE_R2_OID);
                assertNotAssignedRole(bill, TestConstants.ROLE_R3_OID);
                assertAssignedRole(bill, TestConstants.ROLE_R4_OID);
                //assertEquals("Wrong number of assignments for bill", 3, bill.asObjectable().getAssignment().size());
                checkUserApproversForCreate(USER_JACK_OID, new ArrayList<String>(), result);
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> bill = findUserInRepo("bill", result);
                assertAssignedRole(bill, TestConstants.ROLE_R1_OID);
                assertNotAssignedRole(bill, TestConstants.ROLE_R2_OID);
                assertNotAssignedRole(bill, TestConstants.ROLE_R3_OID);
                assertAssignedRole(bill, TestConstants.ROLE_R4_OID);
                //assertEquals("Wrong number of assignments for bill", 4, bill.asObjectable().getAssignment().size());

                checkDummyTransportMessages("simpleUserNotifier", 2);
                checkWorkItemAuditRecords(createResultMap(TestConstants.ROLE_R1_OID, WorkflowResult.APPROVED, TestConstants.ROLE_R2_OID, WorkflowResult.REJECTED));
                checkUserApprovers(bill.getOid(), Arrays.asList(TestConstants.R1BOSS_OID), result);
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
        TestUtil.displayTestTile(this, "test040UserModifyPasswordChangeBlocked");

        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        final ProtectedStringType originalPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
        LOGGER.trace("password before test = " + originalPasswordValue);

        executeTest("test040UserModifyPasswordChangeBlocked", USER_JACK_OID, 1, false, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType> context = createUserAccountContext();
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

                checkDummyTransportMessages("simpleUserNotifier", 0);
                // we don't check for modifyApproverRef because in this test the value was not changed (no change was executed)
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                TestAuthenticationInfoHolder.setUserType(getUser(USER_ADMINISTRATOR_OID).asObjectable());
                return false;
            }
        });
    }

    @Test(enabled = true)
    public void test041UserModifyPasswordChange() throws Exception {
        TestUtil.displayTestTile(this, "test041UserModifyPasswordChange");

        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        final ProtectedStringType originalPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
        LOGGER.trace("password before test = " + originalPasswordValue);

        executeTest("test041UserModifyPasswordChange", USER_JACK_OID, 1, false, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType> context = createUserAccountContext();
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

                checkDummyTransportMessages("simpleUserNotifier", 1);
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                TestAuthenticationInfoHolder.setUserType(getUser(USER_ADMINISTRATOR_OID).asObjectable());
                return true;
            }
        });
    }

    @Test(enabled = true)
    public void test050UserModifyAddRoleAndPasswordChange() throws Exception {
        TestUtil.displayTestTile(this, "test050UserModifyAddRoleAndPasswordChange");

        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        final ProtectedStringType originalPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
        LOGGER.trace("password before test = " + originalPasswordValue);

        executeTest("test050UserModifyAddRoleAndPasswordChange", USER_JACK_OID, 2, false, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType> context = createUserAccountContext();
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
                assertAssignedRole(jack, TestConstants.ROLE_R1_OID);

                checkDummyTransportMessages("simpleUserNotifier", 1);
            }

            @Override
            boolean decideOnApproval(String executionId) throws Exception {
                ApprovalRequestImpl approvalRequest = (ApprovalRequestImpl)
                        activitiEngine.getRuntimeService().getVariable(executionId, ProcessVariableNames.APPROVAL_REQUEST);
                if (approvalRequest.getItemToApprove() instanceof AssignmentType) {
                    return decideOnRoleApproval(executionId);
                } else {
                    TestAuthenticationInfoHolder.setUserType(getUser(USER_ADMINISTRATOR_OID).asObjectable());
                    return true;
                }
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

    private boolean decideOnRoleApproval(String executionId) throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException, SecurityViolationException {
        ApprovalRequestImpl<AssignmentType> approvalRequest = (ApprovalRequestImpl<AssignmentType>)
                activitiEngine.getRuntimeService().getVariable(executionId, ProcessVariableNames.APPROVAL_REQUEST);
        assertNotNull("approval request not found", approvalRequest);

        approvalRequest.setPrismContext(prismContext);

        String roleOid = approvalRequest.getItemToApprove().getTargetRef().getOid();
        assertNotNull("requested role OID not found", roleOid);

        if (TestConstants.ROLE_R1_OID.equals(roleOid)) {
            TestAuthenticationInfoHolder.setUserType(getUser(TestConstants.R1BOSS_OID).asObjectable());
            return true;
        } else if (TestConstants.ROLE_R2_OID.equals(roleOid)) {
            TestAuthenticationInfoHolder.setUserType(getUser(TestConstants.R2BOSS_OID).asObjectable());
            return false;
        } else if (TestConstants.ROLE_R3_OID.equals(roleOid)) {
            TestAuthenticationInfoHolder.setUserType(getUser(TestConstants.R3BOSS_OID).asObjectable());
            return true;
        } else {
            throw new AssertionError("Unknown role OID in assignment to be approved: " + roleOid);
        }
    }
	
	private void executeTest(String testName, String oid, int subtaskCount, boolean immediate, boolean checkObjectOnSubtasks, ContextCreator contextCreator) throws Exception {

        int workflowSubtaskCount = immediate ? subtaskCount-1 : subtaskCount;

		// GIVEN
        prepareNotifications();
        dummyAuditService.clear();

        Task modelTask = taskManager.createTaskInstance(TestUserChangeApproval.class.getName() + "."+testName);

        OperationResult result = new OperationResult("execution");

        modelTask.setOwner(repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, null, result));

        if (oid != null) {
            removeAllRoles(oid, result);
        }

        LensContext<UserType> context = (LensContext<UserType>) contextCreator.createModelContext(result);

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN

       	HookOperationMode mode = clockwork.run(context, modelTask, result);

        // THEN

        assertEquals("Unexpected state of the context", ModelState.PRIMARY, context.getState());
        assertEquals("Wrong mode after clockwork.run in "+context.getState(), HookOperationMode.BACKGROUND, mode);
        modelTask.refresh(result);

        String rootTaskOid = wfTaskUtil.getRootTaskOid(modelTask);
        assertNotNull("Root task OID is not set in model task", rootTaskOid);

        Task rootTask = taskManager.getTask(rootTaskOid, result);
        assertTrue("Root task is not persistent", rootTask.isPersistent());          // trivial ;)

        UriStack uriStack = rootTask.getOtherHandlersUriStack();
        if (!immediate) {
            assertEquals("Invalid handler at stack position 0", ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, uriStack.getUriStackEntry().get(0).getHandlerUri());
//            assertEquals("Invalid current handler", WfPrepareRootOperationTaskHandler.HANDLER_URI, rootTask.getHandlerUri());
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
        assertEquals("Incorrect number of subtasks", subtaskCount, subtasks.size());

        Task task0 = null;

        for (Task subtask : subtasks) {
            if (!WfProcessInstanceShadowTaskHandler.HANDLER_URI.equals(subtask.getHandlerUri())) {
                assertNull("More than one non-wf-monitoring subtask", task0);
                task0 = subtask;
            }
        }

        if (immediate) {
            assertNotNull("Subtask for immediate execution was not found", task0);
            subtasks.remove(task0);
        }

        contextCreator.assertsAfterClockworkRun(rootTask, result);

        checkDummyTransportMessages("simpleWorkflowNotifier-Processes", workflowSubtaskCount);
        checkDummyTransportMessages("simpleWorkflowNotifier-WorkItems", workflowSubtaskCount);

        if (immediate) {
            waitForTaskClose(task0, 20000);
            //TestUtil.assertSuccess(task0.getResult());            // todo enable this
            contextCreator.assertsAfterImmediateExecutionFinished(rootTask, result);
        }

        for (int i = 0; i < subtasks.size(); i++) {
            Task subtask = subtasks.get(i);
            //assertEquals("Subtask #" + i + " is not recurring: " + subtask, TaskRecurrence.RECURRING, subtask.getRecurrenceStatus());
            //assertEquals("Incorrect execution status of subtask #" + i + ": " + subtask, TaskExecutionStatus.RUNNABLE, subtask.getExecutionStatus());
            PrismProperty<ObjectDelta> deltas = subtask.getExtensionProperty(Constants.WFDELTA_TO_PROCESS_PROPERTY_NAME);
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
        //TestUtil.assertSuccess(rootTask.getResult());
        contextCreator.assertsRootTaskFinishes(rootTask, result);

        if (oid == null) {
            oid = contextCreator.getObjectOid(rootTask, result);
        }
        assertNotNull("object oid is null after operation", oid);
        if (!oid.equals(DONT_CHECK)) {
            assertObjectInTaskTree(rootTask, oid, checkObjectOnSubtasks, result);
        }

        checkDummyTransportMessages("simpleWorkflowNotifier-Processes", workflowSubtaskCount*2);
        checkDummyTransportMessages("simpleWorkflowNotifier-WorkItems", workflowSubtaskCount*2);
        notificationManager.setDisabled(true);

        // Check audit
        display("Audit", dummyAuditService);

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
        return repositoryService.getObject(UserType.class, oid, null, result);
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
        ObjectQuery q = ObjectQuery.createObjectQuery(EqualsFilter.createPolyStringOrigEqual(UserType.class, prismContext, UserType.F_NAME, new PolyStringType(name)));
        return repositoryService.searchObjects(UserType.class, q, null, result);
    }

    private List<PrismObject<RoleType>> findRoleInRepoUnchecked(String name, OperationResult result) throws SchemaException {
        ObjectQuery q = ObjectQuery.createObjectQuery(EqualsFilter.createPolyStringOrigEqual(UserType.class, prismContext, RoleType.F_NAME, new PolyStringType(name)));
        return repositoryService.searchObjects(RoleType.class, q, null, result);
    }

    private void deleteUserFromModel(String name) throws SchemaException, ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {

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

}
