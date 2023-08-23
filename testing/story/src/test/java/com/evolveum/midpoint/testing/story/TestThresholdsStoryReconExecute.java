/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author katka
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestThresholdsStoryReconExecute extends TestThresholdsStoryRecon {

    /**
     * This task is used for common import tests drive by the superclass.
     */
    private static final TestObject<TaskType> TASK_RECONCILE_OPENDJ_EXECUTE = TestObject.file(TEST_DIR,
            "task-opendj-reconcile-execute.xml", "20335c7c-838f-11e8-93a6-4b1dd0ab58e4");

    /**
     * This task is used in test600+test610 that check for obeying user deletion policy rule.
     * The mode is simulate + execute, so no deletion will take place.
     */
    private static final TestObject<TaskType> TASK_RECONCILE_OPENDJ_DELETED_SIMULATE_EXECUTE = TestObject.file(TEST_DIR,
            "task-opendj-reconcile-deleted-simulate-execute.xml", "449c3ab7-d50b-4ffa-b72f-9ea238fe2553");

    /**
     * This task is used in test620 that checks for obeying user deletion policy rule.
     * The mode is plain execute, so first two objects will be deleted.
     */
    private static final TestObject<TaskType> TASK_RECONCILE_OPENDJ_DELETED_EXECUTE = TestObject.file(TEST_DIR,
            "task-opendj-reconcile-deleted-execute.xml", "2ae8a1ef-c9f4-4312-a1d1-9b04805b1953");

    /**
     * This role is assigned to the two "deleted" tasks above.
     */
    private static final TestObject<TaskType> ROLE_STOP_ON_3RD_USER_DELETION = TestObject.file(TEST_DIR,
            "role-stop-on-3rd-user-deletion.xml", "ad3b7db8-15ea-4b47-87bc-3e75ff949a0f");

    @Override
    protected TestObject<TaskType> getTaskTestResource() {
        return TASK_RECONCILE_OPENDJ_EXECUTE;
    }

    @Override
    protected int getWorkerThreads() {
        return 0;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(ROLE_STOP_ON_3RD_USER_DELETION, initResult);
        addObject(TASK_RECONCILE_OPENDJ_DELETED_SIMULATE_EXECUTE, initTask, initResult);
        addObject(TASK_RECONCILE_OPENDJ_DELETED_EXECUTE, initTask, initResult);
    }

    @Test
    public void test600TestFullRecon() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        rerunTask(TASK_RECONCILE_OPENDJ_DELETED_SIMULATE_EXECUTE.oid, result);

        then();
        assertTask(TASK_RECONCILE_OPENDJ_DELETED_SIMULATE_EXECUTE.oid, "after")
                .display()
                .assertClosed()
                .assertSuccess();
    }

    /**
     * Disabled because policy rules are ignored on focus deletion.
     */
    @Test(enabled = false)
    public void test610TestDeleteAndRecon() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        openDJController.delete("uid=user10,ou=People,dc=example,dc=com");
        openDJController.delete("uid=user11,ou=People,dc=example,dc=com");
        openDJController.delete("uid=user12,ou=People,dc=example,dc=com");
        openDJController.delete("uid=user13,ou=People,dc=example,dc=com");
        openDJController.delete("uid=user14,ou=People,dc=example,dc=com");
        openDJController.delete("uid=user15,ou=People,dc=example,dc=com");

//        setGlobalTracingOverride(createModelLoggingTracingProfile());

        when();
        rerunTaskErrorsOk(TASK_RECONCILE_OPENDJ_DELETED_SIMULATE_EXECUTE.oid, result);

        then();
        assertTask(TASK_RECONCILE_OPENDJ_DELETED_SIMULATE_EXECUTE.oid, "after")
                .display()
                .assertSuspended()
                .assertFatalError();

        assertObjectByName(UserType.class, "user10", task, result);
        assertObjectByName(UserType.class, "user11", task, result);
        assertObjectByName(UserType.class, "user12", task, result);
        assertObjectByName(UserType.class, "user13", task, result);
        assertObjectByName(UserType.class, "user14", task, result);
        assertObjectByName(UserType.class, "user15", task, result);
    }

    /**
     * Disabled because policy rules are ignored on focus deletion.
     */
    @Test(enabled = false)
    public void test620ReconFull() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        rerunTaskErrorsOk(TASK_RECONCILE_OPENDJ_DELETED_EXECUTE.oid, result);

        then();
        assertTask(TASK_RECONCILE_OPENDJ_DELETED_EXECUTE.oid, "after")
                .display()
                .assertSuspended()
                .assertFatalError();

        assertNoObjectByName(UserType.class, "user10", task, result);
        assertNoObjectByName(UserType.class, "user11", task, result);
        assertObjectByName(UserType.class, "user12", task, result);
        assertObjectByName(UserType.class, "user13", task, result);
        assertObjectByName(UserType.class, "user14", task, result);
        assertObjectByName(UserType.class, "user15", task, result);
    }

    @Override
    protected void assertAfterFirstImport(TaskType taskAfter) {
//        assertEquals(getReconFailureCount(taskAfter), 1);

//        ActivitySynchronizationStatisticsType syncInfo = getReconSyncStats(taskAfter);
//        dumpSynchronizationInformation(syncInfo);

//        assertEquals((int) syncInfo.getCountUnmatched(), 5);
//        assertEquals((int) syncInfo.getCountDeleted(), 0);
//        assertEquals((int) syncInfo.getCountLinked(), getDefaultUsers());
//        assertEquals((int) syncInfo.getCountUnlinked(), 0);
//
//        assertEquals((int) syncInfo.getCountUnmatchedAfter(), 1);  // There is 1 unmatched because it's recorded after "stop" policy rule triggered
//        assertEquals((int) syncInfo.getCountDeleted(), 0);
//        assertEquals((int) syncInfo.getCountLinkedAfter(), getDefaultUsers() + getProcessedUsers());
//        assertEquals((int) syncInfo.getCountUnlinked(), 0);
    }

    @Override
    protected void assertAfterSecondImport(TaskType taskAfter) {
//        assertEquals(getReconFailureCount(taskAfter), 1);
//
//        ActivitySynchronizationStatisticsType syncInfo = getReconSyncStats(taskAfter);
//        dumpSynchronizationInformation(syncInfo);

//        assertEquals((int) syncInfo.getCountUnmatched(), 5);
//        assertEquals((int) syncInfo.getCountDeleted(), 0);
//        assertEquals((int) syncInfo.getCountLinked(), getDefaultUsers() + getProcessedUsers());
//        assertEquals((int) syncInfo.getCountUnlinked(), 0);
//
//        assertEquals((int) syncInfo.getCountUnmatchedAfter(), 1);  // There is 1 unmatched because it's recorded after "stop" policy rule triggered
//        assertEquals((int) syncInfo.getCountDeleted(), 0);
//        assertEquals((int) syncInfo.getCountLinkedAfter(), getDefaultUsers() + getProcessedUsers() * 2);
//        assertEquals((int) syncInfo.getCountUnlinked(), 0);
    }

    protected void assertAfterDisablingAccounts(TaskType taskAfter) {
//        assertEquals(getReconFailureCount(taskAfter), 1);
//
//        ActivitySynchronizationStatisticsType syncInfo = getReconSyncStats(taskAfter);
//        dumpSynchronizationInformation(syncInfo);

//        assertEquals((int) synchronizationInformation.getCountUnmatched(), 0);
//        assertEquals((int) synchronizationInformation.getCountDeleted(), 0);
//        // 1. gibbs, 2. barbossa, 3. beckett (unchanged), 4. user1, 5. user2 (disabled), 6. user3 (tried to be disabled but failed because of the rule)
//        assertEquals((int) synchronizationInformation.getCountLinked(), 6);
//        assertEquals((int) synchronizationInformation.getCountUnlinked(), 0);
//
//        assertEquals((int) synchronizationInformation.getCountUnmatchedAfter(), 0);
//        assertEquals((int) synchronizationInformation.getCountDeleted(), 0);
//        assertEquals((int) synchronizationInformation.getCountLinked(), 6);
//        assertEquals((int) synchronizationInformation.getCountUnlinked(), 0);
    }

}
