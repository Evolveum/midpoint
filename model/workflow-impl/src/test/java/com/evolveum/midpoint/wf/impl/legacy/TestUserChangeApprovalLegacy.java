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
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfTestUtil;
import com.evolveum.midpoint.wf.impl.WorkflowResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.*;

import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;
import static com.evolveum.midpoint.schema.GetOperationOptions.createRetrieve;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType.*;
import static java.util.Collections.singletonList;
import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestUserChangeApprovalLegacy extends AbstractWfTestLegacy {

    protected static final Trace LOGGER = TraceManager.getTrace(TestUserChangeApprovalLegacy.class);

    private static final File REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1 = new File(TEST_RESOURCE_DIR,
            "user-jack-modify-add-assignment-role1.xml");
    private static final File REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE2_CHANGE_GN = new File(TEST_RESOURCE_DIR,
            "user-jack-modify-add-assignment-role2-change-gn.xml");
    private static final File REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE3_CHANGE_GN2 = new File(TEST_RESOURCE_DIR,
            "user-jack-modify-add-assignment-role3-change-gn2.xml");
    private static final File REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLES2_3_4 = new File(TEST_RESOURCE_DIR,
            "user-jack-modify-add-assignment-roles2-3-4.xml");
    private static final File REQ_USER_JACK_MODIFY_ACTIVATION_DISABLE = new File(TEST_RESOURCE_DIR,
            "user-jack-modify-activation-disable.xml");
    private static final File REQ_USER_JACK_MODIFY_ACTIVATION_ENABLE = new File(TEST_RESOURCE_DIR,
            "user-jack-modify-activation-enable.xml");
    private static final File REQ_USER_JACK_MODIFY_CHANGE_PASSWORD = new File(TEST_RESOURCE_DIR,
            "user-jack-modify-change-password.xml");
    private static final File REQ_USER_JACK_MODIFY_CHANGE_PASSWORD_2 = new File(TEST_RESOURCE_DIR,
            "user-jack-modify-change-password-2.xml");
    private static final File REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE10 = new File(TEST_RESOURCE_DIR,
            "user-jack-modify-add-assignment-role10.xml");
    private static final File REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_DUMMY = new File(TEST_RESOURCE_DIR,
            "user-jack-modify-add-assignment-dummy.xml");

    public TestUserChangeApprovalLegacy() throws JAXBException {
		super();
	}

    /**
     * The simplest case: user modification with one security-sensitive role.
     */
	@Test
    public void test010UserModifyAddRole() throws Exception {
        TestUtil.displayTestTitle(this, "test010UserModifyAddRole");
        login(userAdministrator);
       	executeTest("test010UserModifyAddRole", USER_JACK_OID, new TestDetails() {
            @Override
            int subcasesCount() {
                return 1;
            }

            @Override
            boolean immediate() {
                return false;
            }

            @Override
            boolean checkObjectOnSubtasks() {
                return true;
            }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subtasks,
                    Task opTask, OperationResult result) throws Exception {
//                ModelContext taskModelContext = temporaryHelper.getModelContext(rootCase, opTask, result);
//                assertTrue("There are modifications left in primary focus delta", ObjectDelta.isEmpty(taskModelContext.getFocusContext().getPrimaryDelta()));
                assertNotAssignedRole(USER_JACK_OID, ROLE_R1_OID, opTask, result);
                assertWfContextAfterClockworkRun(rootCase, subtasks, result, "Assigning role \"Role1\" to user \"jack\"");
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask, OperationResult result) throws Exception {
                assertAssignedRole(USER_JACK_OID, ROLE_R1_OID, opTask, result);
                checkDummyTransportMessages("simpleUserNotifier", 1);
                checkWorkItemAuditRecords(createResultMap(ROLE_R1_OID, WorkflowResult.APPROVED));
                checkUserApprovers(USER_JACK_OID, singletonList(R1BOSS_OID), result);
                assertWfContextAfterRootTaskFinishes(aCase, subcases, result, "Assigning role \"Role1\" to user \"jack\"");
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                return decideOnRoleApproval(subcase, wfContext);
            }
        });
        displayAllNotifications();
        checkDummyTransportMessages("simpleWorkflowNotifier-Processes", 2);       // start + end
	}

    protected void assertWfContextAfterClockworkRun(CaseType rootCase, List<CaseType> subcases, OperationResult result, String... processNames) throws Exception {

        Task opTask = taskManager.createTaskInstance();
        display("rootCase", rootCase);
        // TODO-WF
//        assertTrue("unexpected process instance id in root task", rootTaskType.getWorkflowContext() == null || rootTaskType.getWorkflowContext().getCaseOid() == null);

        assertEquals("Wrong # of wf subcases w.r.t processNames (" + Arrays.asList(processNames) + ")", processNames.length, subcases.size());
        int i = 0;
        for (CaseType subcase : subcases) {
            display("Subtask #"+(i+1)+": ", subcase);
            checkCase(subcase, subcase.toString(), processNames[i++]);
            WfTestUtil.assertRef("requester ref", subcase.getRequestorRef(), USER_ADMINISTRATOR_OID, false, false);
        }

        final Collection<SelectorOptions<GetOperationOptions>> options1 = schemaHelper.getOperationOptionsBuilder()
                .item(T_PARENT, F_OBJECT_REF).resolve()
                .item(T_PARENT, F_TARGET_REF).resolve()
                .item(F_ASSIGNEE_REF).resolve()
                .item(F_ORIGINAL_ASSIGNEE_REF).resolve()
                .item(T_PARENT, F_REQUESTOR_REF).resolve()
                .build();
        List<CaseWorkItemType> workItems = modelService.searchContainers(CaseWorkItemType.class, getOpenItemsQuery(), options1, opTask, result);
        assertEquals("Wrong # of work items", processNames.length, workItems.size());
        i = 0;
        for (CaseWorkItemType workItem : workItems) {
            display("Work item #"+(i+1)+": ", workItem);
            display("Case", CaseWorkItemUtil.getCase(workItem));
            WfTestUtil.assertRef("object reference", WfContextUtil.getObjectRef(workItem), USER_JACK_OID, true, true);
            WfTestUtil.assertRef("target reference", WfContextUtil.getTargetRef(workItem), ROLE_R1_OID, true, true);
            WfTestUtil.assertRef("assignee reference", workItem.getOriginalAssigneeRef(), R1BOSS_OID, false, true);     // name is not known
            //WfTestUtil.assertRef("task reference", workItem.getTaskRef(), null, false, true);
            final CaseType subcase = CaseWorkItemUtil.getCaseRequired(workItem);
            checkCase(subcase, "case in workItem", processNames[i++]);
            WfTestUtil.assertRef("requester ref", subcase.getRequestorRef(), USER_ADMINISTRATOR_OID, false, true);
        }
    }

    private void checkCase(CaseType subcase, String context, String processName) {
        WfContextType wfc = subcase.getWorkflowContext();
        assertNotNull("Missing workflow context in subcase: " + context, wfc);
        assertEquals("Wrong process ID name in subtask: " + context, processName, subcase.getName().getOrig());
        assertNotNull("Missing process start time in subtask: " + context, CaseTypeUtil.getStartTimestamp(subcase));
        assertNull("Unexpected process end time in subtask: " + context, subcase.getCloseTimestamp());
        assertEquals("Wrong outcome", null, subcase.getOutcome());
        //assertEquals("Wrong state", null, wfc.getState());
    }

    protected void assertWfContextAfterRootTaskFinishes(CaseType rootCase, List<CaseType> subcases, OperationResult result, String... processNames) throws Exception {

        assertEquals("Wrong # of wf subcases w.r.t processNames (" + Arrays.asList(processNames) + ")", processNames.length, subcases.size());
        int i = 0;
        for (CaseType subcase : subcases) {
            display("Subtask #"+(i+1)+": ", subcase);
            WfContextType wfc = subcase.getWorkflowContext();
            assertNotNull("Missing workflow context in wf subcase: " + subcase, wfc);
            // TODO-WF
//            assertNotNull("No process ID in wf subcase: " + subcase, wfc.getCaseOid());
            assertEquals("Wrong process ID name in subcase: " + subcase, processNames[i++], subcase.getName().getOrig());
            assertNotNull("Missing process start time in subcase: " + subcase, CaseTypeUtil.getStartTimestamp(subcase));
            assertNotNull("Missing process end time in subcase: " + subcase, subcase.getCloseTimestamp());
            assertEquals("Wrong outcome", SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE, subcase.getOutcome());
        }
    }

    /**
     * User modification with one security-sensitive role and other (unrelated) change - e.g. change of the given name.
     * Aggregated execution.
     */

    @Test(enabled = true)
    public void test011UserModifyAddRoleChangeGivenName() throws Exception {
        TestUtil.displayTestTitle(this, "test011UserModifyAddRoleChangeGivenName");
        login(userAdministrator);

        executeTest("test011UserModifyAddRoleChangeGivenName", USER_JACK_OID, new TestDetails() {
            @Override int subcasesCount() { return 2; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE2_CHANGE_GN);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase, CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
                Collection<? extends ItemDelta<?, ?>> modifications = pcpGeneralHelper.retrieveResultingDeltas(case0)
                        .getFocusChange().getModifications();
                assertEquals("There is wrong number of modifications left in primary focus delta", 1,
                        modifications.size());
                ItemDelta givenNameDelta = modifications.iterator().next();

                assertNotNull("givenName delta is incorrect (not a replace delta)", givenNameDelta.isReplace());
                assertEquals("givenName delta is incorrect (wrong value)", "JACK", ((PrismPropertyValue<PolyString>) givenNameDelta.getValuesToReplace().iterator().next()).getValue().getOrig());

                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
                assertNotAssignedRole(jack, ROLE_R2_OID);
                assertEquals("Wrong given name before change", "Jack", jack.asObjectable().getGivenName().getOrig());
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
                assertNotAssignedRole(jack, ROLE_R2_OID);
                assertEquals("Wrong given name after change", "JACK", jack.asObjectable().getGivenName().getOrig());

                checkDummyTransportMessages("simpleUserNotifier", 1);
                checkWorkItemAuditRecords(createResultMap(ROLE_R2_OID, WorkflowResult.REJECTED));
                checkUserApprovers(USER_JACK_OID, new ArrayList<>(), result);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                return decideOnRoleApproval(subcase, wfContext);
            }

        });
    }

    @Test(enabled = true)
    public void test012UserModifyAddRoleChangeGivenNameImmediate() throws Exception {
        TestUtil.displayTestTitle(this, "test012UserModifyAddRoleChangeGivenNameImmediate");
        login(userAdministrator);
        executeTest("test012UserModifyAddRoleChangeGivenNameImmediate", USER_JACK_OID, new TestDetails() {
            @Override int subcasesCount() { return 2; }
            @Override boolean immediate() { return true; }
            @Override boolean checkObjectOnSubtasks() { return true; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE3_CHANGE_GN2);
                context.setOptions(ModelExecuteOptions.createExecuteImmediatelyAfterApproval());
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
            }

            @Override
            void assertsAfterImmediateExecutionFinished(CaseType task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
                assertNotAssignedRole(jack, ROLE_R3_OID);
                assertEquals("Wrong given name after immediate execution", "J-A-C-K", jack.asObjectable().getGivenName().getOrig());
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
                assertAssignedRole(jack, ROLE_R3_OID);

                checkDummyTransportMessages("simpleUserNotifier", 2);
                checkWorkItemAuditRecords(createResultMap(ROLE_R3_OID, WorkflowResult.APPROVED));
                checkUserApprovers(USER_JACK_OID, Arrays.asList(R3BOSS_OID), result);         // given name is changed before role is added, so the approver should be recorded
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                return decideOnRoleApproval(subcase, wfContext);
            }

        });
    }


    @Test(enabled = true)
    public void test020UserModifyAddThreeRoles() throws Exception {
        TestUtil.displayTestTitle(this, "test020UserModifyAddThreeRoles");
        login(userAdministrator);
        executeTest("test020UserModifyAddThreeRoles", USER_JACK_OID, new TestDetails() {
            @Override int subcasesCount() { return 3; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLES2_3_4);
                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ACTIVATION_DISABLE);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
                Collection<? extends ItemDelta<?, ?>> modifications = pcpGeneralHelper.retrieveResultingDeltas(case0)
                        .getFocusChange().getModifications();
                assertEquals("There is wrong number of modifications left in case0", 2, modifications.size());
                Iterator<? extends ItemDelta> it = modifications.iterator();
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
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                PrismObject<UserType> jack = getUserFromRepo(USER_JACK_OID, result);
                assertNotAssignedRole(jack, ROLE_R1_OID);
                assertNotAssignedRole(jack, ROLE_R2_OID);
                assertAssignedRole(jack, ROLE_R3_OID);
                assertAssignedRole(jack, ROLE_R4_OID);
                assertEquals("activation has not been changed", ActivationStatusType.DISABLED, jack.asObjectable().getActivation().getAdministrativeStatus());

                checkDummyTransportMessages("simpleUserNotifier", 1);
                checkWorkItemAuditRecords(createResultMap(ROLE_R2_OID, WorkflowResult.REJECTED, ROLE_R3_OID, WorkflowResult.APPROVED));
                checkUserApprovers(USER_JACK_OID, Arrays.asList(R3BOSS_OID), result);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                return decideOnRoleApproval(subcase, wfContext);
            }

        });
    }

    @Test(enabled = true)
    public void test021UserModifyAddThreeRolesImmediate() throws Exception {
        TestUtil.displayTestTitle(this, "test021UserModifyAddThreeRolesImmediate");
        login(userAdministrator);
        executeTest("test021UserModifyAddThreeRolesImmediate", USER_JACK_OID, new TestDetails() {
            @Override int subcasesCount() { return 3; }
            @Override boolean immediate() { return true; }
            @Override boolean checkObjectOnSubtasks() { return true; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLES2_3_4);
                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ACTIVATION_ENABLE);
                context.setOptions(ModelExecuteOptions.createExecuteImmediatelyAfterApproval());
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
            }

            @Override
            void assertsAfterImmediateExecutionFinished(CaseType task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
                assertNotAssignedRole(jack, ROLE_R1_OID);
                assertNotAssignedRole(jack, ROLE_R2_OID);
                assertNotAssignedRole(jack, ROLE_R3_OID);
                assertAssignedRole(jack, ROLE_R4_OID);
                assertEquals("activation has not been changed", ActivationStatusType.ENABLED, jack.asObjectable().getActivation().getAdministrativeStatus());
                checkUserApprovers(USER_JACK_OID, new ArrayList<>(), result);
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                PrismObject<UserType> jack = getUserFromRepo(USER_JACK_OID, result);
                assertNotAssignedRole(jack, ROLE_R1_OID);
                assertNotAssignedRole(jack, ROLE_R2_OID);
                assertAssignedRole(jack, ROLE_R3_OID);
                assertAssignedRole(jack, ROLE_R4_OID);
                assertEquals("activation has not been changed", ActivationStatusType.ENABLED, jack.asObjectable().getActivation().getAdministrativeStatus());

                checkDummyTransportMessages("simpleUserNotifier", 2);
                checkWorkItemAuditRecords(createResultMap(ROLE_R2_OID, WorkflowResult.REJECTED, ROLE_R3_OID, WorkflowResult.APPROVED));
                checkUserApprovers(USER_JACK_OID, Arrays.asList(R3BOSS_OID), result);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                return decideOnRoleApproval(subcase, wfContext);
            }

        });
    }

    @Test(enabled = true)
    public void test030UserAdd() throws Exception {
        TestUtil.displayTestTitle(this, "test030UserAdd");
        login(userAdministrator);
        executeTest("test030UserAdd", null, new TestDetails() {
            @Override int subcasesCount() { return 3; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return false; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                PrismObject<UserType> bill = prismContext.parseObject(USER_BILL_FILE);
                fillContextWithAddUserDelta(context, bill);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
                //noinspection unchecked
                PrismObject<UserType> objectToAdd = (PrismObject<UserType>) pcpGeneralHelper.retrieveResultingDeltas(case0)
                        .getFocusChange().getObjectToAdd();
                assertNotNull("There is no object to add left in primary focus delta", objectToAdd);
                assertFalse("There is assignment of R1 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), ROLE_R1_OID));
                assertFalse("There is assignment of R2 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), ROLE_R2_OID));
                assertFalse("There is assignment of R3 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), ROLE_R3_OID));
                assertTrue("There is no assignment of R4 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), ROLE_R4_OID));
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                PrismObject<UserType> bill = findUserInRepo("bill", result);
                assertAssignedRole(bill, ROLE_R1_OID);
                assertNotAssignedRole(bill, ROLE_R2_OID);
                assertNotAssignedRole(bill, ROLE_R3_OID);
                assertAssignedRole(bill, ROLE_R4_OID);
                //assertEquals("Wrong number of assignments for bill", 4, bill.asObjectable().getAssignmentNew().size());

                checkDummyTransportMessages("simpleUserNotifier", 1);
                checkWorkItemAuditRecords(createResultMap(ROLE_R1_OID, WorkflowResult.APPROVED, ROLE_R2_OID, WorkflowResult.REJECTED));
                checkUserApproversForCreate(bill.getOid(), Arrays.asList(R1BOSS_OID), result);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                return decideOnRoleApproval(subcase, wfContext);
            }

            @Override
            String getObjectOid(CaseType task, OperationResult result) throws SchemaException {
                //return findUserInRepo("bill", result).getOid();
                return DONT_CHECK;        // don't check in this case
            }
        });
    }

    @Test(enabled = true)
    public void test031UserAddImmediate() throws Exception {
        TestUtil.displayTestTitle(this, "test031UserAddImmediate");
        login(userAdministrator);

        deleteUserFromModel("bill");

        executeTest("test031UserAddImmediate", null, new TestDetails() {
            @Override int subcasesCount() { return 3; }
            @Override boolean immediate() { return true; }
            @Override boolean checkObjectOnSubtasks() { return true; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                PrismObject<UserType> bill = prismContext.parseObject(USER_BILL_FILE);
                fillContextWithAddUserDelta(context, bill);
                context.setOptions(ModelExecuteOptions.createExecuteImmediatelyAfterApproval());
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
            }

            @Override
            void assertsAfterImmediateExecutionFinished(CaseType task, OperationResult result) throws Exception {
                PrismObject<UserType> bill = findUserInRepo("bill", result);
                assertNotAssignedRole(bill, ROLE_R1_OID);
                assertNotAssignedRole(bill, ROLE_R2_OID);
                assertNotAssignedRole(bill, ROLE_R3_OID);
                assertAssignedRole(bill, ROLE_R4_OID);
                //assertEquals("Wrong number of assignments for bill", 3, bill.asObjectable().getAssignmentNew().size());
                checkUserApproversForCreate(USER_JACK_OID, new ArrayList<>(), result);
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                PrismObject<UserType> bill = findUserInRepo("bill", result);
                assertAssignedRole(bill, ROLE_R1_OID);
                assertNotAssignedRole(bill, ROLE_R2_OID);
                assertNotAssignedRole(bill, ROLE_R3_OID);
                assertAssignedRole(bill, ROLE_R4_OID);
                //assertEquals("Wrong number of assignments for bill", 4, bill.asObjectable().getAssignmentNew().size());

                checkDummyTransportMessages("simpleUserNotifier", 2);
                checkWorkItemAuditRecords(createResultMap(ROLE_R1_OID, WorkflowResult.APPROVED, ROLE_R2_OID, WorkflowResult.REJECTED));
                checkUserApprovers(bill.getOid(), Arrays.asList(R1BOSS_OID), result);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                return decideOnRoleApproval(subcase, wfContext);
            }

            @Override
            String getObjectOid(CaseType task, OperationResult result) throws SchemaException {
                return findUserInRepo("bill", result).getOid();
            }
        });
    }

    @Test(enabled = false)      // password change aspect is not available any more
    public void test040UserModifyPasswordChangeBlocked() throws Exception {
        TestUtil.displayTestTitle(this, "test040UserModifyPasswordChangeBlocked");
        login(userAdministrator);

        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        final ProtectedStringType originalPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
        LOGGER.trace("password before test = " + originalPasswordValue);

        executeTest("test040UserModifyPasswordChangeBlocked", USER_JACK_OID, new TestDetails() {
            @Override int subcasesCount() { return 1; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                encryptAndAddFocusModificationToContext(context, REQ_USER_JACK_MODIFY_CHANGE_PASSWORD);
                //context.setOptions(ModelExecuteOptions.createNoCrypt());
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
//                ModelContext taskModelContext = temporaryHelper.getModelContext(rootCase, opTask, result);
//                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                PrismObject<UserType> jack = getUser(USER_JACK_OID);
                ProtectedStringType afterTestPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
                LOGGER.trace("password after test = " + afterTestPasswordValue);

                //assertNotNull("password was not set", afterTestPasswordValue.getEncryptedData());
                assertTrue("password was changed", originalPasswordValue.getEncryptedDataType().equals(afterTestPasswordValue.getEncryptedDataType()));

                checkDummyTransportMessages("simpleUserNotifier", 0);
                // we don't check for modifyApproverRef because in this test the value was not changed (no change was executed)
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                login(getUser(USER_ADMINISTRATOR_OID));
                return false;
            }
        });
    }

    @Test(enabled = false)      // password change aspect is not available any more
    public void test041UserModifyPasswordChange() throws Exception {
        TestUtil.displayTestTitle(this, "test041UserModifyPasswordChange");
        login(userAdministrator);
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        final ProtectedStringType originalPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
        LOGGER.trace("password before test = " + originalPasswordValue);

        executeTest("test041UserModifyPasswordChange", USER_JACK_OID, new TestDetails() {
            @Override int subcasesCount() { return 1; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                encryptAndAddFocusModificationToContext(context, REQ_USER_JACK_MODIFY_CHANGE_PASSWORD);
                //context.setOptions(ModelExecuteOptions.createNoCrypt());
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
//                ModelContext taskModelContext = temporaryHelper.getModelContext(rootCase, opTask, result);
//                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                PrismObject<UserType> jack = getUser(USER_JACK_OID);
                ProtectedStringType afterTestPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
                LOGGER.trace("password after test = " + afterTestPasswordValue);

                //assertNotNull("password was not set", afterTestPasswordValue.getEncryptedData());
                assertFalse("password was not changed", originalPasswordValue.getEncryptedDataType().equals(afterTestPasswordValue.getEncryptedDataType()));

                checkDummyTransportMessages("simpleUserNotifier", 1);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                login(getUser(USER_ADMINISTRATOR_OID));
                return true;
            }
        });
    }

    @Test(enabled = false)      // password change aspect is not available any more
    public void test050UserModifyAddRoleAndPasswordChange() throws Exception {
        TestUtil.displayTestTitle(this, "test050UserModifyAddRoleAndPasswordChange");
        login(userAdministrator);
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        final ProtectedStringType originalPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
        LOGGER.trace("password before test = " + originalPasswordValue);

        executeTest("test050UserModifyAddRoleAndPasswordChange", USER_JACK_OID, new TestDetails() {
            @Override int subcasesCount() { return 2; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                encryptAndAddFocusModificationToContext(context, REQ_USER_JACK_MODIFY_CHANGE_PASSWORD_2);
                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1);
                //context.setOptions(ModelExecuteOptions.createNoCrypt());
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
//                ModelContext taskModelContext = temporaryHelper.getModelContext(rootCase, opTask, result);
//                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                PrismObject<UserType> jack = getUser(USER_JACK_OID);
                ProtectedStringType afterTestPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
                LOGGER.trace("password after test = " + afterTestPasswordValue);

                // todo why is password value not set?
                //assertNotNull("password was not set", afterTestPasswordValue.getEncryptedData());
                //assertFalse("password was not changed", originalPasswordValue.getEncryptedData().equals(afterTestPasswordValue.getEncryptedData()));
                assertAssignedRole(jack, ROLE_R1_OID);

                checkDummyTransportMessages("simpleUserNotifier", 1);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                ObjectReferenceType targetRef = subcase.getTargetRef();
                if (targetRef != null && RoleType.COMPLEX_TYPE.equals(targetRef.getType())) {
                    return decideOnRoleApproval(subcase, wfContext);
                } else {
                    login(getUser(USER_ADMINISTRATOR_OID));
                    return true;
                }
            }

        });
    }

    @Test(enabled = true)
    public void test060UserModifyAddRoleAutoApproval() throws Exception {
        TestUtil.displayTestTitle(this, "test060UserModifyAddRoleAutoApproval");
        login(userAdministrator);
        executeTest("test060UserModifyAddRoleAutoApproval", USER_JACK_OID, new TestDetails() {
            @Override int subcasesCount() { return 1; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }

            @Override boolean approvedAutomatically() { return true; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE10);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
                // todo perhaps the role should be assigned even at this point?
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
                assertAssignedRole(jack, ROLE_R10_OID);

                checkDummyTransportMessages("simpleUserNotifier", 1);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                throw new AssertionError("Decision should not be acquired in this scenario.");
            }

        });
        displayAllNotifications();
        checkDummyTransportMessages("simpleWorkflowNotifier-Processes", 2);       // start + end
    }

    @Test(enabled = true)
    public void test061UserModifyAddRoleAutoSkip() throws Exception {
        final String TEST_NAME = "test061UserModifyAddRoleAutoSkip";
        TestUtil.displayTestTitle(this, TEST_NAME);
        login(userAdministrator);
        Task task = taskManager.createTaskInstance(TestUserChangeApprovalLegacy.class.getName() + "."+TEST_NAME);
        task.setOwner(userAdministrator);
        OperationResult result = task.getResult();

        // WHEN
        assignRole(USER_JACK_OID, ROLE_R10_SKIP_OID, task, result);

        // THEN
        PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
        assertAssignedRole(jack, ROLE_R10_OID, task, result);

        result.computeStatusIfUnknown();
        assertSuccess(result);
    }

    @Test
    public void test062UserModifyAddRoleAutoApprovalFirstDecides() throws Exception {
        TestUtil.displayTestTitle(this, "test062UserModifyAddRoleAutoApprovalFirstDecides");
        login(userAdministrator);
        executeTest("test062UserModifyAddRoleAutoApprovalFirstDecides", USER_JACK_OID, new TestDetails() {
            @Override int subcasesCount() { return 1; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }

            @Override boolean approvedAutomatically() { return true; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addFocusDeltaToContext(context,
                        (ObjectDelta) prismContext.deltaFor(UserType.class)
                                .item(UserType.F_ASSIGNMENT).add(
                                        ObjectTypeUtil.createAssignmentTo(ROLE_R11_OID, ObjectTypes.ROLE, prismContext).asPrismContainerValue())
                            .asObjectDelta(USER_JACK_OID));
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
                // todo perhaps the role should be assigned even at this point?
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
                assertAssignedRole(jack, ROLE_R11_OID);

                checkDummyTransportMessages("simpleUserNotifier", 1);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                throw new AssertionError("Decision should not be acquired in this scenario.");
            }

        });
        displayAllNotifications();
        checkDummyTransportMessages("simpleWorkflowNotifier-Processes", 2);       // start + end
    }

    @Test
    public void test064UserModifyAddRoleNoApproversAllMustAgree() throws Exception {
        TestUtil.displayTestTitle(this, "test064UserModifyAddRoleNoApproversAllMustAgree");
        login(userAdministrator);
        executeTest("test064UserModifyAddRoleNoApproversAllMustAgree", USER_JACK_OID, new TestDetails() {
            @Override int subcasesCount() { return 1; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }

            @Override boolean approvedAutomatically() { return true; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addFocusDeltaToContext(context,
                        (ObjectDelta) prismContext.deltaFor(UserType.class)
                                .item(UserType.F_ASSIGNMENT).add(
                                        ObjectTypeUtil.createAssignmentTo(ROLE_R12_OID, ObjectTypes.ROLE, prismContext).asPrismContainerValue())
                                .asObjectDelta(USER_JACK_OID));
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
                // todo perhaps the role should be assigned even at this point?
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
                assertAssignedRole(jack, ROLE_R12_OID);

                checkDummyTransportMessages("simpleUserNotifier", 1);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                throw new AssertionError("Decision should not be acquired in this scenario.");
            }

        });
    }

    @Test
    public void test065UserModifyAddRoleNoApproversFirstDecides() throws Exception {
        TestUtil.displayTestTitle(this, "test065UserModifyAddRoleNoApproversFirstDecides");
        login(userAdministrator);
        executeTest("test065UserModifyAddRoleNoApproversFirstDecides", USER_JACK_OID, new TestDetails() {
            @Override int subcasesCount() { return 1; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }

            @Override boolean approvedAutomatically() { return true; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addFocusDeltaToContext(context,
                        (ObjectDelta) prismContext.deltaFor(UserType.class)
                                .item(UserType.F_ASSIGNMENT).add(
                                        ObjectTypeUtil.createAssignmentTo(ROLE_R13_OID, ObjectTypes.ROLE, prismContext).asPrismContainerValue())
                                .asObjectDelta(USER_JACK_OID));
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
                // todo perhaps the role should be assigned even at this point?
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
                assertAssignedRole(jack, ROLE_R13_OID);

                checkDummyTransportMessages("simpleUserNotifier", 1);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                throw new AssertionError("Decision should not be acquired in this scenario.");
            }

        });
    }


    @Test(enabled = false)          // assignment modification approval is not enabled by default
    public void test070UserModifyAssignment() throws Exception {
        TestUtil.displayTestTitle(this, "test070UserModifyAssignment");
        login(userAdministrator);
        removeAllAssignments(USER_JACK_OID, new OperationResult("dummy"));
        assignRoleRaw(USER_JACK_OID, ROLE_R1_OID);

        final XMLGregorianCalendar validFrom = XmlTypeConverter.createXMLGregorianCalendar(2015, 2, 25, 10, 0, 0);
        final XMLGregorianCalendar validTo = XmlTypeConverter.createXMLGregorianCalendar(2015, 3, 25, 10, 0, 0);

        executeTest("test070UserModifyAssignment", USER_JACK_OID, new TestDetails() {
            @Override
            int subcasesCount() {
                return 1;
            }

            @Override
            boolean immediate() {
                return false;
            }

            @Override
            boolean checkObjectOnSubtasks() {
                return true;
            }

            @Override
            boolean removeAssignmentsBeforeTest() {
                return false;
            }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                UserType jack = context.getFocusContext().getObjectOld().asObjectable();
                modifyAssignmentValidity(context, jack, validFrom, validTo);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
//                ModelContext taskModelContext = temporaryHelper.getModelContext(rootCase, opTask, result);
//                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
                UserType jack = getUser(USER_JACK_OID).asObjectable();
                checkNoAssignmentValidity(jack);
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                UserType jack = getUser(USER_JACK_OID).asObjectable();
                checkAssignmentValidity(jack, validFrom, validTo);

                // TODO
                //checkDummyTransportMessages("simpleUserNotifier", 1);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                login(getUser(R1BOSS_OID));
                return true;
            }

        });
    }

    private void checkAssignmentValidity(UserType jack, XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo) {
        assertEquals("jack's assignments", 1, jack.getAssignment().size());
        AssignmentType assignmentType = jack.getAssignment().get(0);
        assertEquals("wrong validFrom", validFrom, assignmentType.getActivation().getValidFrom());
        assertEquals("wrong validTo", validTo, assignmentType.getActivation().getValidTo());
    }

    protected void checkNoAssignmentValidity(UserType jack) {
        assertEquals("jack's assignments", 1, jack.getAssignment().size());
        AssignmentType assignmentType = jack.getAssignment().get(0);
        if (assignmentType.getActivation() != null) {
            assertNull("validFrom already set", assignmentType.getActivation().getValidFrom());
            assertNull("validTo already set", assignmentType.getActivation().getValidTo());
        }
    }

    protected void modifyAssignmentValidity(LensContext<UserType> context, UserType jack, XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo) throws SchemaException {
        assertEquals("jack's assignments", 1, jack.getAssignment().size());
        PrismContainerDefinition<ActivationType> activationDef =
                prismContext.getSchemaRegistry()
                        .findObjectDefinitionByCompileTimeClass(UserType.class)
                        .findContainerDefinition(ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION));
        assertNotNull("no activationDef", activationDef);

        Long assignmentId = jack.getAssignment().get(0).getId();
        PrismPropertyDefinition<XMLGregorianCalendar> validFromDef = activationDef.findPropertyDefinition(ActivationType.F_VALID_FROM);
        PropertyDelta<XMLGregorianCalendar> validFromDelta = prismContext.deltaFactory().property().create(
                ItemPath.create(UserType.F_ASSIGNMENT, assignmentId, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                        validFromDef);
        validFromDelta.setRealValuesToReplace(validFrom);
        PrismPropertyDefinition<XMLGregorianCalendar> validToDef = activationDef.findPropertyDefinition(ActivationType.F_VALID_TO);
        PropertyDelta<XMLGregorianCalendar> validToDelta = prismContext.deltaFactory().property().create(
                ItemPath.create(UserType.F_ASSIGNMENT, assignmentId, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO),
                        validToDef);
        validToDelta.setRealValuesToReplace(validTo);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().create(UserType.class, ChangeType.MODIFY);
        userDelta.setOid(USER_JACK_OID);
        userDelta.addModification(validFromDelta);
        userDelta.addModification(validToDelta);
        addFocusDeltaToContext(context, userDelta);
    }

    private void assignRoleRaw(String userOid, String roleOid) throws SchemaException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userOid, roleOid, RoleType.COMPLEX_TYPE, null, null, null, true);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        repositoryService.modifyObject(UserType.class,
                userOid, userDelta.getModifications(), new OperationResult("dummy"));
    }

    /**
     * User modification: adding single security-sensitive resource assignment.
     */
    @Test(enabled = false)          // this is not enabled by default
    public void test080UserModifyAddResource() throws Exception {
        TestUtil.displayTestTitle(this, "test080UserModifyAddResource");
        login(userAdministrator);
        executeTest("test080UserModifyAddResource", USER_JACK_OID, new TestDetails() {
            @Override int subcasesCount() { return 1; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_DUMMY);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase, CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
//                ModelContext taskModelContext = temporaryHelper.getModelContext(rootCase, opTask, result);
//                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
                assertNotAssignedResource(USER_JACK_OID, RESOURCE_DUMMY_OID, opTask, result);
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                assertAssignedResource(UserType.class, USER_JACK_OID, RESOURCE_DUMMY_OID, opTask, result);
                checkDummyTransportMessages("simpleUserNotifier", 1);
                //checkWorkItemAuditRecords(createResultMap(ROLE_R1_OID, WorkflowResult.APPROVED));
                checkUserApprovers(USER_JACK_OID, Arrays.asList(DUMMYBOSS_OID), result);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                login(getUser(DUMMYBOSS_OID));
                return true;
            }
        });
    }

    /**
     * User modification: modifying validity of single security-sensitive resource assignment.
     */
    @Test(enabled = false)          // this is not enabled by default
    public void test090UserModifyModifyResourceAssignmentValidity() throws Exception {
        TestUtil.displayTestTitle(this, "test090UserModifyModifyResourceAssignmentValidity");
        login(userAdministrator);

        final XMLGregorianCalendar validFrom = XmlTypeConverter.createXMLGregorianCalendar(2015, 2, 25, 10, 0, 0);
        final XMLGregorianCalendar validTo = XmlTypeConverter.createXMLGregorianCalendar(2015, 3, 25, 10, 0, 0);

        executeTest("test090UserModifyModifyResourceAssignmentValidity", USER_JACK_OID, new TestDetails() {
            @Override int subcasesCount() { return 1; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }
            @Override boolean removeAssignmentsBeforeTest() { return false; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                UserType jack = context.getFocusContext().getObjectOld().asObjectable();
                modifyAssignmentValidity(context, jack, validFrom, validTo);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
//                ModelContext taskModelContext = temporaryHelper.getModelContext(rootCase, opTask, result);
//                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
                UserType jack = getUser(USER_JACK_OID).asObjectable();
                checkNoAssignmentValidity(jack);
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                assertAssignedResource(UserType.class, USER_JACK_OID, RESOURCE_DUMMY_OID, opTask, result);
                UserType jack = getUser(USER_JACK_OID).asObjectable();
                checkAssignmentValidity(jack, validFrom, validTo);
                checkDummyTransportMessages("simpleUserNotifier", 1);
                //checkWorkItemAuditRecords(createResultMap(ROLE_R1_OID, WorkflowResult.APPROVED));
                checkUserApprovers(USER_JACK_OID, Arrays.asList(DUMMYBOSS_OID), result);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                login(getUser(DUMMYBOSS_OID));
                return true;
            }
        });
    }

    /**
     * User modification: modifying attribute of single security-sensitive resource assignment.
     *
     * User primary delta:
     *  ObjectDelta<UserType>(UserType:377205db-33d1-47e5-bf96-bbe2a7d1222e,MODIFY):
     *  assignment/[1]/construction/attribute
     *  ADD: ResourceAttributeDefinitionType(ref=ItemPathType{itemPath=lastname}...)
     *  DELETE: ResourceAttributeDefinitionType(ref=ItemPathType{itemPath=lastname}...)
     *
     */
    @Test(enabled = false)          // this is not enabled by default
    public void test095UserModifyModifyResourceAssignmentConstruction() throws Exception {
        final String TEST_NAME = "test095UserModifyModifyResourceAssignmentConstruction";
        TestUtil.displayTestTitle(this, TEST_NAME);
        login(userAdministrator);

        executeTest(TEST_NAME, USER_JACK_OID, new TestDetails() {
            @Override int subcasesCount() { return 1; }
            @Override boolean immediate() { return false; }
            @Override boolean checkObjectOnSubtasks() { return true; }
            @Override boolean removeAssignmentsBeforeTest() { return false; }

            @Override
            public LensContext createModelContext(Task task, OperationResult result) throws Exception {
                LensContext<UserType> context = createUserLensContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                UserType jack = context.getFocusContext().getObjectOld().asObjectable();
                modifyAssignmentConstruction(context, jack,
                        DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "water", true);
                return context;
            }

            @Override
            public void assertsAfterClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    Task opTask, OperationResult result) throws Exception {
//                ModelContext taskModelContext = temporaryHelper.getModelContext(rootCase, opTask, result);
//                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
                UserType jack = getUser(USER_JACK_OID).asObjectable();
                checkNoAssignmentConstruction(jack, "drink");
            }

            @Override
            void assertsRootCaseFinishes(CaseType aCase, List<CaseType> subcases, Task opTask,
                    OperationResult result) throws Exception {
                assertAssignedResource(UserType.class, USER_JACK_OID, RESOURCE_DUMMY_OID, opTask, result);
                UserType jack = getUser(USER_JACK_OID).asObjectable();
                checkAssignmentConstruction(jack, "drink", "water");
                checkDummyTransportMessages("simpleUserNotifier", 1);
                //checkWorkItemAuditRecords(createResultMap(ROLE_R1_OID, WorkflowResult.APPROVED));
                checkUserApprovers(USER_JACK_OID, Arrays.asList(DUMMYBOSS_OID), result);
            }

            @Override
            boolean decideOnApproval(CaseType subcase,
                    WfContextType wfContext) throws Exception {
                login(getUser(DUMMYBOSS_OID));
                return true;
            }
        });
    }

    protected void modifyAssignmentConstruction(LensContext<UserType> context, UserType jack,
                                                String attributeName, String value, boolean add) throws SchemaException {
        assertEquals("jack's assignments", 1, jack.getAssignment().size());
        PrismContainerDefinition<ResourceAttributeDefinitionType> attributeDef =
                prismContext.getSchemaRegistry()
                        .findObjectDefinitionByCompileTimeClass(UserType.class)
                        .findContainerDefinition(ItemPath.create(UserType.F_ASSIGNMENT,
                                AssignmentType.F_CONSTRUCTION,
                                ConstructionType.F_ATTRIBUTE));
        assertNotNull("no attributeDef", attributeDef);

        Long assignmentId = jack.getAssignment().get(0).getId();
        ContainerDelta<ResourceAttributeDefinitionType> attributeDelta = prismContext.deltaFactory().container().create(
            ItemPath.create(UserType.F_ASSIGNMENT, assignmentId, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE),
            attributeDef);
        ResourceAttributeDefinitionType attributeDefinitionType = new ResourceAttributeDefinitionType();
        if (add) {
            attributeDelta.addValueToAdd(attributeDefinitionType.asPrismContainerValue());
        } else {
            attributeDelta.addValueToDelete(attributeDefinitionType.asPrismContainerValue());
        }
        attributeDefinitionType.setRef(new ItemPathType(ItemPath.create(new QName(RESOURCE_DUMMY_NAMESPACE, attributeName))));
        MappingType outbound = new MappingType();
        outbound.setStrength(MappingStrengthType.STRONG);       // to see changes on the resource
        ExpressionType expression = new ExpressionType();
        expression.getExpressionEvaluator().add(new ObjectFactory().createValue(value));
        outbound.setExpression(expression);
        attributeDefinitionType.setOutbound(outbound);


        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().create(UserType.class, ChangeType.MODIFY);
        userDelta.setOid(USER_JACK_OID);
        userDelta.addModification(attributeDelta);
        addFocusDeltaToContext(context, userDelta);
    }

    private void checkAssignmentConstruction(UserType jack, String attributeName, String value) throws SchemaException {
        assertEquals("jack's assignments", 1, jack.getAssignment().size());
        AssignmentType assignmentType = jack.getAssignment().get(0);
        ConstructionType constructionType = assignmentType.getConstruction();
        assertNotNull("construction is null", constructionType);
        boolean found = false;
        for (ResourceAttributeDefinitionType attributeDefinitionType : constructionType.getAttribute()) {
            if (attributeDefinitionType.getRef().equivalent(new ItemPathType(ItemPath.create(new QName(attributeName))))) {
                ExpressionType expressionType = attributeDefinitionType.getOutbound().getExpression();
                assertNotNull("no expression", expressionType);
                assertEquals("wrong # of expression evaluators", 1, expressionType.getExpressionEvaluator().size());
                JAXBElement<?> element = expressionType.getExpressionEvaluator().get(0);
                PrimitiveXNode valueXNode = (PrimitiveXNode) (((RawType) element.getValue()).serializeToXNode());
                assertEquals("wrong outbound value", value, valueXNode.getStringValue());
                found = true;
            }
        }
        assertTrue("attribute " + attributeName + " mapping not found", found);
    }

    private void checkNoAssignmentConstruction(UserType jack, String attributeName) {
        assertEquals("jack's assignments", 1, jack.getAssignment().size());
        AssignmentType assignmentType = jack.getAssignment().get(0);
        ConstructionType constructionType = assignmentType.getConstruction();
        assertNotNull("construction is null", constructionType);
        for (ResourceAttributeDefinitionType attributeDefinitionType : constructionType.getAttribute()) {
            if (attributeDefinitionType.getRef().equivalent(new ItemPathType(ItemPath.create(new QName(attributeName))))) {
                fail("Construction attribute " + attributeName + " present, although it shouldn't");
            }
        }
    }

    @Test
    public void zzzMarkAsNotInitialized() {
        display("Setting class as not initialized");
        unsetSystemInitialized();
    }
}
