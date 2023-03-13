/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.other;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskListener;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.WfTestHelper;
import com.evolveum.midpoint.wf.impl.execution.CaseOperationExecutionTaskHandler;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestParallelApprovals extends AbstractWfTestPolicy {

    @Autowired private WfTestHelper testHelper;

    protected static final File TEST_RESOURCE_DIR = new File("src/test/resources/parallel");

    private static final File ROLE_ROLE50A_FILE = new File(TEST_RESOURCE_DIR, "role-role50a-slow.xml");
    private static final File ROLE_ROLE51A_FILE = new File(TEST_RESOURCE_DIR, "role-role51a-slow.xml");
    private static final File ROLE_ROLE52A_FILE = new File(TEST_RESOURCE_DIR, "role-role52a-slow.xml");
    private static final File ROLE_ROLE53A_FILE = new File(TEST_RESOURCE_DIR, "role-role53a-slow.xml");

    private static final File USER_BOB_FILE = new File(TEST_RESOURCE_DIR, "user-bob.xml");
    private static final File USER_CHUCK_FILE = new File(TEST_RESOURCE_DIR, "user-chuck.xml");

    protected String userBobOid;
    protected String userChuckOid;

    private String roleRole50aOid, roleRole51aOid, roleRole52aOid, roleRole53aOid;

    public static boolean doSleep = false;

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        userBobOid = repoAddObjectFromFile(USER_BOB_FILE, initResult).getOid();
        userChuckOid = repoAddObjectFromFile(USER_CHUCK_FILE, initResult).getOid();

        roleRole50aOid = repoAddObjectFromFile(ROLE_ROLE50A_FILE, initResult).getOid();
        roleRole51aOid = repoAddObjectFromFile(ROLE_ROLE51A_FILE, initResult).getOid();
        roleRole52aOid = repoAddObjectFromFile(ROLE_ROLE52A_FILE, initResult).getOid();
        roleRole53aOid = repoAddObjectFromFile(ROLE_ROLE53A_FILE, initResult).getOid();

        assignRole(USER_ADMINISTRATOR_OID, roleRole51aOid, SchemaConstants.ORG_APPROVER, initTask, initResult);
        assignRole(USER_ADMINISTRATOR_OID, roleRole52aOid, SchemaConstants.ORG_APPROVER,  initTask, initResult);
        assignRole(USER_ADMINISTRATOR_OID, roleRole53aOid, SchemaConstants.ORG_APPROVER,  initTask, initResult);

        doSleep = true;
        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
    }

    @Override
    protected void updateSystemConfiguration(SystemConfigurationType systemConfiguration) throws SchemaException, IOException {
        super.updateSystemConfiguration(systemConfiguration);
        systemConfiguration.getWorkflowConfiguration()
                .beginExecutionTasks()
                        .beginSerialization()
                                .retryAfter(XmlTypeConverter.createDuration(1000));      // makes tests run faster
    }

    private CheckingTaskListener listener;

    @Test
    public void test100ParallelApprovals() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        when();
        ObjectDelta<UserType> assignDelta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(
                        ObjectTypeUtil.createAssignmentTo(roleRole50aOid, ObjectTypes.ROLE, prismContext),
                        ObjectTypeUtil.createAssignmentTo(roleRole51aOid, ObjectTypes.ROLE, prismContext),
                        ObjectTypeUtil.createAssignmentTo(roleRole52aOid, ObjectTypes.ROLE, prismContext),
                        ObjectTypeUtil.createAssignmentTo(roleRole53aOid, ObjectTypes.ROLE, prismContext))
                .asObjectDelta(USER_JACK.oid);
        executeChanges(assignDelta, executeOptions().executeImmediatelyAfterApproval(), task, result); // should start approval processes
        assertNotAssignedRole(USER_JACK.oid, roleRole51aOid, result);
        assertNotAssignedRole(USER_JACK.oid, roleRole52aOid, result);
        assertNotAssignedRole(USER_JACK.oid, roleRole53aOid, result);

        display("Task after operation", task);

        CaseType rootCase = testHelper.getRootCase(result);
        display("root case", rootCase);

        if (listener != null) {
            taskManager.unregisterTaskListener(listener);
        }
        listener = new CheckingTaskListener(singleton(rootCase.getOid()));
        taskManager.registerTaskListener(listener);

        approveAllWorkItems(task, result);

        rootCase = testHelper.waitForCaseClose(rootCase, 120000);

        // THEN

        assertNull("Exception has occurred " + listener.getException(), listener.getException());
        assertEquals("Wrong root case status", SchemaConstants.CASE_STATE_CLOSED, rootCase.getState());

        PrismObject<UserType> jack = getUser(USER_JACK.oid);
        assertAssignedRole(jack, roleRole50aOid);
        assertAssignedRole(jack, roleRole51aOid);
        assertAssignedRole(jack, roleRole52aOid);
        assertAssignedRole(jack, roleRole53aOid);
    }

    @Test
    public void test110ParallelApprovalsAdd() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        if (listener != null) {
            taskManager.unregisterTaskListener(listener);
        }
        listener = new CheckingTaskListener();
        taskManager.registerTaskListener(listener);

        // WHEN
        when();
        UserType alice = prismContext.createObjectable(UserType.class)
                .name("alice")
                .assignment(ObjectTypeUtil.createAssignmentTo(roleRole50aOid, ObjectTypes.ROLE, prismContext))
                .assignment(ObjectTypeUtil.createAssignmentTo(roleRole51aOid, ObjectTypes.ROLE, prismContext))
                .assignment(ObjectTypeUtil.createAssignmentTo(roleRole52aOid, ObjectTypes.ROLE, prismContext))
                .assignment(ObjectTypeUtil.createAssignmentTo(roleRole53aOid, ObjectTypes.ROLE, prismContext));
        executeChanges(DeltaFactory.Object.createAddDelta(alice.asPrismObject()), executeOptions().executeImmediatelyAfterApproval(), task, result); // should start approval processes

        display("Task after operation", task);
        CaseType rootCase = testHelper.getRootCase(result);
        display("root case", rootCase);

        listener.setCasesToCloseOnError(singleton(rootCase.getOid()));

        approveAllWorkItems(task, result);
        rootCase = testHelper.waitForCaseClose(rootCase, 120000);

        // THEN

        assertNull("Exception has occurred " + listener.getException(), listener.getException());
        assertEquals("Wrong root case status", SchemaConstants.CASE_STATE_CLOSED, rootCase.getState());

        PrismObject<UserType> aliceAfter = findUserByUsername("alice");
        assertAssignedRole(aliceAfter, roleRole50aOid);
        assertAssignedRole(aliceAfter, roleRole51aOid);
        assertAssignedRole(aliceAfter, roleRole52aOid);
        assertAssignedRole(aliceAfter, roleRole53aOid);
    }

    public void approveAllWorkItems(Task task, OperationResult result) throws Exception {
        List<CaseWorkItemType> workItems = getWorkItems(task, result);
        display("work items", workItems);
        display("approving work items");
        for (CaseWorkItemType workItem : workItems) {
            caseManager.completeWorkItem(WorkItemId.of(workItem),
                    ApprovalUtils.createApproveOutput(),
                    null, task, result);
        }
    }

    @Test
    public void test120ParallelApprovalsInTwoOperations() throws Exception {
        login(userAdministrator);

        Task task0 = createPlainTask("task0");
        Task task1 = createPlainTask("task1");
        Task task2 = createPlainTask("task2");
        OperationResult result0 = task0.getResult();
        OperationResult result1 = task1.getResult();
        OperationResult result2 = task2.getResult();

        if (listener != null) {
            taskManager.unregisterTaskListener(listener);
        }
        listener = new CheckingTaskListener();
        taskManager.registerTaskListener(listener);

        // WHEN
        when();
        ObjectDelta<UserType> assignDelta1 = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(
                        ObjectTypeUtil.createAssignmentTo(roleRole50aOid, ObjectTypes.ROLE, prismContext),
                        ObjectTypeUtil.createAssignmentTo(roleRole51aOid, ObjectTypes.ROLE, prismContext))
                .asObjectDelta(userBobOid);
        executeChanges(assignDelta1, executeOptions().executeImmediatelyAfterApproval(), task1, result1); // should start approval processes
        ObjectDelta<UserType> assignDelta2 = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(
                        ObjectTypeUtil.createAssignmentTo(roleRole50aOid, ObjectTypes.ROLE, prismContext),
                        ObjectTypeUtil.createAssignmentTo(roleRole52aOid, ObjectTypes.ROLE, prismContext),
                        ObjectTypeUtil.createAssignmentTo(roleRole53aOid, ObjectTypes.ROLE, prismContext))
                .asObjectDelta(userBobOid);
        executeChanges(assignDelta2, executeOptions().executeImmediatelyAfterApproval(), task2, result2); // should start approval processes
        assertNotAssignedRole(userBobOid, roleRole51aOid, result0);
        assertNotAssignedRole(userBobOid, roleRole52aOid, result0);
        assertNotAssignedRole(userBobOid, roleRole53aOid, result0);

        display("Task1 after operation", task1);
        display("Task2 after operation", task2);
        CaseType rootCase1 = testHelper.getRootCase(result1);
        display("root case1", rootCase1);
        CaseType rootCase2 = testHelper.getRootCase(result2);
        display("root case2", rootCase2);

        assertNull("Exception has occurred " + listener.getException(), listener.getException());
        listener.setCasesToCloseOnError(Arrays.asList(rootCase1.getOid(), rootCase2.getOid()));

        approveAllWorkItems(task0, result0);

        rootCase1 = testHelper.waitForCaseClose(rootCase1, 120000);
        rootCase2 = testHelper.waitForCaseClose(rootCase2, 120000);

        // THEN

        assertNull("Exception has occurred " + listener.getException(), listener.getException());
        assertEquals("Wrong root case1 status", SchemaConstants.CASE_STATE_CLOSED, rootCase1.getState());
        assertEquals("Wrong root case2 status", SchemaConstants.CASE_STATE_CLOSED, rootCase2.getState());

        PrismObject<UserType> bob = getUser(userBobOid);
        assertAssignedRole(bob, roleRole50aOid);
        assertAssignedRole(bob, roleRole51aOid);
        assertAssignedRole(bob, roleRole52aOid);
        assertAssignedRole(bob, roleRole53aOid);
    }

    @Test
    public void test130ParallelApprovalsInThreeSummarizingOperations() throws Exception {
        login(userAdministrator);

        Task task0 = createPlainTask("task0");
        Task task1 = createPlainTask("task1");
        Task task2 = createPlainTask("task2");
        Task task3 = createPlainTask("task3");
        OperationResult result0 = task0.getResult();
        OperationResult result1 = task1.getResult();
        OperationResult result2 = task2.getResult();
        OperationResult result3 = task3.getResult();

        if (listener != null) {
            taskManager.unregisterTaskListener(listener);
        }
        listener = new CheckingTaskListener();
        taskManager.registerTaskListener(listener);

        // WHEN
        when();
        // three separate approval contexts, "summarizing" as the deltas are executed after all approvals
        assignRole(userChuckOid, roleRole51aOid, task1, result1);
        assignRole(userChuckOid, roleRole52aOid, task2, result2);
        assignRole(userChuckOid, roleRole53aOid, task3, result3);
        assertNotAssignedRole(userChuckOid, roleRole51aOid, result0);
        assertNotAssignedRole(userChuckOid, roleRole52aOid, result0);
        assertNotAssignedRole(userChuckOid, roleRole53aOid, result0);

        display("Task1 after operation", task1);
        display("Task2 after operation", task2);
        display("Task3 after operation", task3);
        CaseType rootCase1 = testHelper.getRootCase(result1);
        CaseType rootCase2 = testHelper.getRootCase(result2);
        CaseType rootCase3 = testHelper.getRootCase(result3);
        display("root case1", rootCase1);
        display("root case2", rootCase2);
        display("root case3", rootCase3);

        assertNull("Exception has occurred " + listener.getException(), listener.getException());
        listener.setCasesToCloseOnError(Arrays.asList(rootCase1.getOid(), rootCase2.getOid(), rootCase3.getOid()));

        approveAllWorkItems(task0, result0);

        rootCase1 = testHelper.waitForCaseClose(rootCase1, 120000);
        rootCase2 = testHelper.waitForCaseClose(rootCase2, 120000);
        rootCase3 = testHelper.waitForCaseClose(rootCase3, 120000);

        // THEN

        assertNull("Exception has occurred " + listener.getException(), listener.getException());
        assertEquals("Wrong root case1 status", SchemaConstants.CASE_STATE_CLOSED, rootCase1.getState());
        assertEquals("Wrong root case2 status", SchemaConstants.CASE_STATE_CLOSED, rootCase2.getState());
        assertEquals("Wrong root case3 status", SchemaConstants.CASE_STATE_CLOSED, rootCase3.getState());

        PrismObject<UserType> chuck = getUser(userChuckOid);
        assertAssignedRole(chuck, roleRole51aOid);
        assertAssignedRole(chuck, roleRole52aOid);
        assertAssignedRole(chuck, roleRole53aOid);
    }

    private class CheckingTaskListener implements TaskListener {

        private Collection<String> casesToCloseOnError;
        private Task executing;
        private RuntimeException exception;

        public CheckingTaskListener() {
            this.casesToCloseOnError = emptySet();
        }

        public CheckingTaskListener(Collection<String> casesToCloseOnError) {
            this.casesToCloseOnError = casesToCloseOnError;
        }

        public RuntimeException getException() {
            return exception;
        }

        public void setCasesToCloseOnError(Collection<String> casesToCloseOnError) {
            this.casesToCloseOnError = casesToCloseOnError;
            if (exception != null) {
                closeCases();
            }
        }

        @Override
        public synchronized void onTaskStart(Task task, OperationResult result) {
            if (!CaseOperationExecutionTaskHandler.HANDLER_URI.equals(task.getHandlerUri())) {
                return;
            }
            System.out.println(Thread.currentThread().getName() + ": Starting " + task + ", handler uri " + task.getHandlerUri() + ", groups " + task.getGroups());
            if (executing != null) {
                exception = new IllegalStateException("Started task " + task + " but another one is already executing: " + executing);
                System.out.println(exception.getMessage());
                display("newly started task", task);
                display("already executing task", executing);
                closeCases();
            }
            executing = task;
        }

        void closeCases() {
            // suspend root task in order to fail faster
            try {
                List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(CaseType.class)
                        .item(CaseType.F_STATE).replace(SchemaConstants.CASE_STATE_CLOSED)
                        .asItemDeltas();
                for (String oid : casesToCloseOnError) {
                    repositoryService.modifyObject(CaseType.class, oid, modifications, new OperationResult("dummy"));
                }
            } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
                throw new SystemException(e);
            }
        }

        @Override
        public synchronized void onTaskFinish(Task task, TaskRunResult runResult, OperationResult result) {
            if (!CaseOperationExecutionTaskHandler.HANDLER_URI.equals(task.getHandlerUri())) {
                return;
            }
            System.out.println(Thread.currentThread().getName() + ": Finishing " + task + ", handler uri " + task.getHandlerUri());
            assert executing.getOid().equals(task.getOid());
            executing = null;
        }

        @Override
        public void onTaskThreadStart(Task task, boolean isRecovering, OperationResult result) {
            // ignoring
        }

        @Override
        public void onTaskThreadFinish(Task task, OperationResult result) {
            // ignoring
        }
    }
}
