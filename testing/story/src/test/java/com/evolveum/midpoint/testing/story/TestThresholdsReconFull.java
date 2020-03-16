/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.File;

import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author katka
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestThresholdsReconFull extends TestThresholds {

    private static final File TASK_RECONCILE_OPENDJ_FULL_FILE = new File(TEST_DIR, "task-opendj-reconcile-full.xml");
    private static final String TASK_RECONCILE_OPENDJ_FULL_OID = "20335c7c-838f-11e8-93a6-4b1dd0ab58e4";

    private static final File ROLE_POLICY_RULE_DELETE_FILE = new File(TEST_DIR, "role-policy-rule-delete.xml");
    private static final String ROLE_POLICY_RULE_DELETE_OID = "00000000-role-0000-0000-888111111112";

    private static final File TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_FILE = new File(TEST_DIR, "task-opendj-reconcile-simulate-execute.xml");
    private static final String TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID = "00000000-838f-11e8-93a6-4b1dd0ab58e4";

    @Override
    protected File getTaskFile() {
        return TASK_RECONCILE_OPENDJ_FULL_FILE;
    }

    @Override
    protected String getTaskOid() {
        return TASK_RECONCILE_OPENDJ_FULL_OID;
    }

    @Override
    protected int getProcessedUsers() {
        return 4;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(ROLE_POLICY_RULE_DELETE_FILE, initResult);
        repoAddObjectFromFile(TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_FILE, initResult);
    }

    @Test
    public void test600ChangeTaskPolicyRule() throws Exception {
        //WHEN
        Task task = createPlainTask();
        OperationResult result = task.getResult();
        assignRole(TaskType.class, TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID, ROLE_POLICY_RULE_DELETE_OID, task, result);

        //THEN
        PrismObject<TaskType> taskAfter = getObject(TaskType.class, TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID);
        display("Task after:", taskAfter);
        assertAssignments(taskAfter, 1);
        assertAssigned(taskAfter, ROLE_POLICY_RULE_DELETE_OID, RoleType.COMPLEX_TYPE);
        assertTaskExecutionStatus(TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID, TaskExecutionStatus.SUSPENDED);
    }

    @Test
    public void test610TestFullRecon() throws Exception {
        OperationResult result = createOperationResult();

        //WHEN
        when();
        OperationResult reconResult = resumeTaskAndWaitForNextFinish(TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID, true, 20000);
        assertSuccess(reconResult);

        //THEN

        Task taskAfter = taskManager.getTaskWithResult(TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID, result);

        assertTaskExecutionStatus(TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID, TaskExecutionStatus.RUNNABLE);
        assertSynchronizationStatisticsFull(taskAfter);

    }

    @Test
    public void test611TestFullRecon() throws Exception {
        OperationResult result = createOperationResult();

        openDJController.delete("uid=user10,ou=People,dc=example,dc=com");
        openDJController.delete("uid=user11,ou=People,dc=example,dc=com");
        openDJController.delete("uid=user12,ou=People,dc=example,dc=com");
        openDJController.delete("uid=user13,ou=People,dc=example,dc=com");
        openDJController.delete("uid=user14,ou=People,dc=example,dc=com");
        openDJController.delete("uid=user15,ou=People,dc=example,dc=com");

        //WHEN
        when();
        OperationResult reconResult = waitForTaskNextRun(TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID, true, 20000, false);
        assertSuccess(reconResult);

        //THEN

        Task taskAfter = taskManager.getTaskWithResult(TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID, result);

        assertTaskExecutionStatus(TASK_RECONCILE_OPENDJ_SIMULATE_EXECUTE_OID, TaskExecutionStatus.RUNNABLE);

        IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
        assertEquals(infoType.getTotalFailureCount(), 0);

        PrismObject<UserType> user10 = findUserByUsername("user10");
        assertNull(user10);

        PrismObject<UserType> user11 = findUserByUsername("user11");
        assertNull(user11);

        PrismObject<UserType> user12 = findUserByUsername("user12");
        assertNull(user12);

        PrismObject<UserType> user13 = findUserByUsername("user13");
        assertNull(user13);

        PrismObject<UserType> user14 = findUserByUsername("user14");
        assertNull(user14);

        PrismObject<UserType> user15 = findUserByUsername("user15");
        assertNull(user15);

    }

    @Override
    protected void assertSynchronizationStatisticsAfterImport(Task taskAfter) {
        IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
        assertEquals(infoType.getTotalFailureCount(), 1);

        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();

        assertEquals(syncInfo.getCountUnmatched(), 5);
        assertEquals(syncInfo.getCountDeleted(), 0);
        assertEquals(syncInfo.getCountLinked(), getDefaultUsers());
        assertEquals(syncInfo.getCountUnlinked(), 0);

        assertEquals(syncInfo.getCountUnmatchedAfter(), 1);  // There is 1 unmatched because it's recorded after "stop" policy rule triggered
        assertEquals(syncInfo.getCountDeleted(), 0);
        assertEquals(syncInfo.getCountLinkedAfter(), getDefaultUsers() + getProcessedUsers());
        assertEquals(syncInfo.getCountUnlinked(), 0);
    }

    private void assertSynchronizationStatisticsFull(Task taskAfter) {
        IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
        assertEquals(infoType.getTotalFailureCount(), 0);
        assertNull(taskAfter.getWorkState(), "Unexpected work state in task.");

    }

    @Override
    protected void assertSynchronizationStatisticsAfterSecondImport(Task taskAfter) {
        IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
        assertEquals(infoType.getTotalFailureCount(), 1);

        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();

        assertEquals(syncInfo.getCountUnmatched(), 5);
        assertEquals(syncInfo.getCountDeleted(), 0);
        assertEquals(syncInfo.getCountLinked(), getDefaultUsers() + getProcessedUsers());
        assertEquals(syncInfo.getCountUnlinked(), 0);

        assertEquals(syncInfo.getCountUnmatchedAfter(), 1);  // There is 1 unmatched because it's recorded after "stop" policy rule triggered
        assertEquals(syncInfo.getCountDeleted(), 0);
        assertEquals(syncInfo.getCountLinkedAfter(), getDefaultUsers() + getProcessedUsers() * 2);
        assertEquals(syncInfo.getCountUnlinked(), 0);
    }

    protected void assertSynchronizationStatisticsActivation(Task taskAfter) {
        IterativeTaskInformationType infoType = taskAfter.getStoredOperationStats().getIterativeTaskInformation();
        assertEquals(infoType.getTotalFailureCount(), 1);
        displayValue("Iterative task information", IterativeTaskInformation.format(infoType));

        SynchronizationInformationType synchronizationInformation = taskAfter.getStoredOperationStats().getSynchronizationInformation();
        dumpSynchronizationInformation(synchronizationInformation);

        assertEquals(synchronizationInformation.getCountUnmatched(), 0);
        assertEquals(synchronizationInformation.getCountDeleted(), 0);
        // 1. gibbs, 2. barbossa, 3. beckett (unchanged), 4. user1, 5. user2 (disabled), 6. user3 (tried to be disabled but failed because of the rule)
        assertEquals(synchronizationInformation.getCountLinked(), 6);
        assertEquals(synchronizationInformation.getCountUnlinked(), 0);

        assertEquals(synchronizationInformation.getCountUnmatchedAfter(), 0);
        assertEquals(synchronizationInformation.getCountDeleted(), 0);
        assertEquals(synchronizationInformation.getCountLinked(), 6);
        assertEquals(synchronizationInformation.getCountUnlinked(), 0);
    }

}
