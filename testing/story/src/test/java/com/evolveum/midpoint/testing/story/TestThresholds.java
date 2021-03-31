/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;
import com.evolveum.midpoint.schema.util.task.TaskOperationStatsUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.TaskAsserter;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author katka
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class TestThresholds extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "thresholds");

    private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
    private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";

    private static final File LDIF_CREATE_BASE_USERS_FILE = new File(TEST_DIR, "users-base.ldif");
    private static final File LDIF_CREATE_USERS_FILE = new File(TEST_DIR, "users.ldif");
    private static final File LDIF_CREATE_USERS_NEXT_FILE = new File(TEST_DIR, "users-next.ldif");
    private static final File LDIF_CHANGE_ACTIVATION_FILE = new File(TEST_DIR, "users-activation.ldif");

    private static final File ROLE_POLICY_RULE_CREATE_FILE = new File(TEST_DIR, "role-policy-rule-create.xml");
    private static final String ROLE_POLICY_RULE_CREATE_OID = "00000000-role-0000-0000-999111111112";

    private static final File ROLE_POLICY_RULE_CHANGE_ACTIVATION_FILE = new File(TEST_DIR, "role-policy-rule-change-activation.xml");
    private static final String ROLE_POLICY_RULE_CHANGE_ACTIVATION_OID = "00000000-role-0000-0000-999111111223";

    private static final File TASK_IMPORT_BASE_USERS_FILE = new File(TEST_DIR, "task-opendj-import-base-users.xml");
    private static final String TASK_IMPORT_BASE_USERS_OID = "fa25e6dc-a858-11e7-8ebc-eb2b71ecce1d";

    protected static final int TASK_TIMEOUT = 60000;

    public static final int RULE_CREATE_WATERMARK = 5;
    public static final int RULE_ACTIVATION_WATERMARK = 3;

    int getDefaultUsers() {
        return 6;
    }

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServer();
    }

    @AfterClass
    public static void stopResources() {
        openDJController.stop();
    }

    protected abstract File getTaskFile();
    protected abstract String getTaskOid();
    protected abstract int getWorkerThreads();
    protected abstract int getProcessedUsers();
    protected abstract void assertSynchronizationStatisticsAfterImport(Task taskAfter) throws Exception;
    protected abstract void assertSynchronizationStatisticsAfterSecondImport(Task taskAfter);
    protected abstract void assertSynchronizationStatisticsActivation(Task taskAfter);

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        //Resources
        PrismObject<ResourceType> resourceOpenDj = importAndGetObjectFromFile(
                ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
        openDJController.setResource(resourceOpenDj);

        repoAddObjectFromFile(ROLE_POLICY_RULE_CREATE_FILE, initResult);
        repoAddObjectFromFile(ROLE_POLICY_RULE_CHANGE_ACTIVATION_FILE, initResult);

        repoAddObjectFromFile(getTaskFile(), initResult);
        adapTaskConfig(initTask, initResult);
    }

    /**
     * Imports base users in an auxiliary task ("import base users").
     */
    @Test
    public void test001testImportBaseUsers() throws Exception {
        OperationResult result = createOperationResult();

        importObjectFromFile(TASK_IMPORT_BASE_USERS_FILE);

        openDJController.addEntriesFromLdifFile(LDIF_CREATE_BASE_USERS_FILE);

        waitForTaskFinish(TASK_IMPORT_BASE_USERS_OID, true, TASK_TIMEOUT);

        Task taskAfter = taskManager.getTaskWithResult(TASK_IMPORT_BASE_USERS_OID, result);
        display("Task after test001testImportBaseUsers:", taskAfter);

        OperationStatsType stats = taskAfter.getStoredOperationStatsOrClone();
        assertNotNull(stats, "No statistics in task");

        SynchronizationInformationType syncInfo = stats.getSynchronizationInformation();
        assertNotNull(syncInfo, "No sync info in task");

//        assertEquals((Object) syncInfo.getCountUnmatched(), getDefaultUsers());
//        assertEquals((Object) syncInfo.getCountDeleted(), 0);
//        assertEquals((Object) syncInfo.getCountLinked(), 0);
//        assertEquals((Object) syncInfo.getCountUnlinked(), 0);

//        assertEquals((Object) syncInfo.getCountUnmatchedAfter(), 0);
//        assertEquals((Object) syncInfo.getCountDeletedAfter(), 0);
//        assertEquals((Object) syncInfo.getCountLinkedAfter(), getDefaultUsers());
//        assertEquals((Object) syncInfo.getCountUnlinkedAfter(), 0);

        assertUsers(getNumberOfUsers());
    }

    @Override
    protected int getNumberOfUsers() {
        return super.getNumberOfUsers() + getDefaultUsers();
    }

    /**
     * Assigns the rule of "Stop after 4 created users" to the main task.
     */
    @Test
    public void test100AssignCreationLimitToTask() throws Exception {

        Task task = createPlainTask();
        OperationResult result = task.getResult();

        when();
        assignRole(TaskType.class, getTaskOid(), ROLE_POLICY_RULE_CREATE_OID, task, result);

        then();
        PrismObject<TaskType> taskAfter = getObject(TaskType.class, getTaskOid());
        display("Task after:", taskAfter);
        assertAssignments(taskAfter, 1);
        assertAssigned(taskAfter, ROLE_POLICY_RULE_CREATE_OID, RoleType.COMPLEX_TYPE);
        assertTaskExecutionStatus(getTaskOid(), TaskExecutionStateType.SUSPENDED);

    }

    /**
     * Runs the main task and checks if it stopped after processing expected number of users.
     */
    @Test
    public void test110ImportAccounts() throws Exception {
        Task task = createPlainTask();
        OperationResult result = task.getResult();

        openDJController.addEntriesFromLdifFile(LDIF_CREATE_USERS_FILE);

        assertUsers(getNumberOfUsers());

        when();
        OperationResult reconResult = resumeTaskAndWaitForNextFinish(getTaskOid(), false, TASK_TIMEOUT);
        assertFailure(reconResult);

        then();
        assertUsers(getProcessedUsers() + getNumberOfUsers());
        assertTaskExecutionStatus(getTaskOid(), TaskExecutionStateType.SUSPENDED);

        Task taskAfter = taskManager.getTaskWithResult(getTaskOid(), result);
        assertSynchronizationStatisticsAfterImport(taskAfter);
    }

    /**
     * Adds more users. Runs the main task again and checks if it again stopped after processing expected number of users.
     */
    @Test
    public void test111ImportAccountsAgain() throws Exception {
        Task task = createPlainTask();
        OperationResult result = task.getResult();

        given();
        openDJController.addEntriesFromLdifFile(LDIF_CREATE_USERS_NEXT_FILE);
        assertUsers(getNumberOfUsers() + getProcessedUsers());
        clearTaskOperationalStats(result);

        when();
        OperationResult reconResult = resumeTaskAndWaitForNextFinish(getTaskOid(), false, TASK_TIMEOUT);
        assertFailure(reconResult);

        then();
        assertUsers(getProcessedUsers() * 2 + getNumberOfUsers());
        assertTaskExecutionStatus(getTaskOid(), TaskExecutionStateType.SUSPENDED);

        Task taskAfter = taskManager.getTaskWithResult(getTaskOid(), result);
        assertSynchronizationStatisticsAfterSecondImport(taskAfter);
    }

    private void clearTaskOperationalStats(OperationResult result) throws SchemaException, ObjectAlreadyExistsException,
            ObjectNotFoundException {
        List<ItemDelta<?, ?>> modifications = deltaFor(TaskType.class)
                .item(TaskType.F_OPERATION_STATS).replace()
                .asItemDeltas();
        repositoryService.modifyObject(TaskType.class, getTaskOid(), modifications, result);
    }

    /**
     * Changes the rule to "Stop after having 2 users with changed activation/administrativeStatus".
     */
    @Test
    public void test500AssignModificationLimitToTask() throws Exception {
        Task task = createPlainTask();
        OperationResult result = task.getResult();

        when();
        unassignRole(TaskType.class, getTaskOid(), ROLE_POLICY_RULE_CREATE_OID, task, result);
        assignRole(TaskType.class, getTaskOid(), ROLE_POLICY_RULE_CHANGE_ACTIVATION_OID, task, result);

        then();
        PrismObject<TaskType> taskAfter = getObject(TaskType.class, getTaskOid());
        display("Task after:", taskAfter);
        assertAssignments(taskAfter, 1);
        assertAssigned(taskAfter, ROLE_POLICY_RULE_CHANGE_ACTIVATION_OID, RoleType.COMPLEX_TYPE);
        assertTaskExecutionStatus(getTaskOid(), TaskExecutionStateType.SUSPENDED);
    }

    /**
     * Disables accounts for users1..6 on the resource. Expects to stop after processing 2 of them.
     */
    @Test
    public void test520ImportDisabledAccounts() throws Exception {
        OperationResult result = createOperationResult();

        given();
        openDJController.executeLdifChanges(LDIF_CHANGE_ACTIVATION_FILE);
        clearTaskOperationalStats(result);

        when();
        OperationResult reconResult = resumeTaskAndWaitForNextFinish(getTaskOid(), false, TASK_TIMEOUT);
        assertFailure(reconResult);

        then();
        Task taskAfter = taskManager.getTaskWithResult(getTaskOid(), result);

        assertTaskExecutionStatus(getTaskOid(), TaskExecutionStateType.SUSPENDED);

        assertSynchronizationStatisticsActivation(taskAfter);
    }

    void dumpSynchronizationInformation(SynchronizationInformationType synchronizationInformation) {
        displayValue("Synchronization information", SynchronizationInformation.format(synchronizationInformation));
    }

    protected void adapTaskConfig(Task task, OperationResult result) throws Exception {
        if (getWorkerThreads() == 0) {
            return;
        }

        ObjectDelta<TaskType> delta = prismContext.deltaFor(TaskType.class)
                .item(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_WORKER_THREADS))
                .add(getWorkerThreads()).asObjectDelta(getTaskOid());

        executeChanges(delta, null, task, result);

        PrismObject<TaskType> taskAfter = getTask(getTaskOid());

        TaskAsserter.forTask(taskAfter)
                .extension()
                .assertPropertyValuesEqual(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS, getWorkerThreads());
    }

    int getFailureCount(Task taskAfter) {
        // TODO separate the statistics dump
        OperationStatsType stats = taskAfter.getStoredOperationStatsOrClone();
        displayValue("Iterative statistics", IterativeTaskInformation.format(stats.getIterativeTaskInformation()));
        return TaskOperationStatsUtil.getItemsProcessedWithFailure(stats);
    }
}
