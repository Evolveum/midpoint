/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.test.TestResource;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ActivitySynchronizationStatisticsUtil;
import com.evolveum.midpoint.schema.util.task.ActivityItemProcessingStatisticsUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationExclusionReasonType.PROTECTED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.LINKED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.UNMATCHED;

/**
 * @author katka
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class TestThresholds extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "thresholds");

    private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
    private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";

    /**
     * Pre-existing users are: idm (protected), jgibbs, hbarbossa, jbeckett.
     * Base users in this file are: user1, user2, user3. These are imported at the beginning.
     */
    private static final File LDIF_USERS_BASE_FILE = new File(TEST_DIR, "users-base.ldif");

    /**
     * These are user4 - user9 (6 entries). They are imported under a policy rule in the first import.
     */
    private static final File LDIF_USERS_FIRST_IMPORT_FILE = new File(TEST_DIR, "users-first-import.ldif");

    /**
     * These are user10 - user15 (6 entries). They are imported under a policy rule in the second import.
     */
    private static final File LDIF_USERS_SECOND_IMPORT_FILE = new File(TEST_DIR, "users-second-import.ldif");

    /**
     * Disables users 1-6.
     */
    private static final File LDIF_CHANGE_ACTIVATION_FILE = new File(TEST_DIR, "users-change-activation.ldif");

    private static final TestResource<RoleType> ROLE_STOP_ON_5TH_USER_CREATION = new TestResource<>(TEST_DIR, "role-stop-on-5th-user-creation.xml", "71478881-f19b-4e8d-a574-76187ee861b2");
    private static final TestResource<RoleType> ROLE_STOP_ON_3RD_STATUS_CHANGE = new TestResource<>(TEST_DIR, "role-stop-on-3rd-status-change.xml", "fdd65b29-d892-452a-9260-f26c7f20e507");
    private static final TestResource<TaskType> TASK_IMPORT_BASE_USERS = new TestResource<>(TEST_DIR, "task-opendj-import-base-users.xml", "fa25e6dc-a858-11e7-8ebc-eb2b71ecce1d");

    static final int TASK_TIMEOUT = 60000;

    public static final int RULE_CREATE_WATERMARK = 5;
    public static final int RULE_ACTIVATION_WATERMARK = 3;

    @Override
    protected void importSystemTasks(OperationResult initResult) throws FileNotFoundException {
        // We don't want these.
    }

    private int getStartingAccounts() {
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

    protected abstract TestResource<TaskType> getTaskResource();
    protected abstract int getWorkerThreads();
    protected abstract int getProcessedUsers();
    protected abstract void assertAfterFirstImport(TaskType taskAfter) throws Exception;
    protected abstract void assertAfterSecondImport(TaskType taskAfter);
    protected abstract void assertAfterDisablingAccounts(TaskType taskAfter);

    protected boolean isSimulate() {
        return false;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        PrismObject<ResourceType> resourceOpenDj = importAndGetObjectFromFile(
                ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
        openDJController.setResource(resourceOpenDj);

        repoAdd(ROLE_STOP_ON_5TH_USER_CREATION, initResult);
        repoAdd(ROLE_STOP_ON_3RD_STATUS_CHANGE, initResult);

        addObject(getTaskResource().file, initTask, initResult, workerThreadsCustomizerNew(getWorkerThreads()));
    }

    /**
     * Imports base users in an auxiliary task ("import base users").
     */
    @Test
    public void test001testImportBaseUsers() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        openDJController.addEntriesFromLdifFile(LDIF_USERS_BASE_FILE);

        addObject(TASK_IMPORT_BASE_USERS, task, result);
        waitForTaskCloseOrSuspend(TASK_IMPORT_BASE_USERS.oid, TASK_TIMEOUT);

        int standardAccounts = getStartingAccounts();

        // @formatter:off
        assertTask(TASK_IMPORT_BASE_USERS.oid, "after")
                .display()
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(standardAccounts, 0, 1) // existing (jgibbs, hbarbossa, jbeckett + idm), new (user1-3)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(null, UNMATCHED, LINKED, null, standardAccounts, 0, 0)
                        .assertTransition(null, null, null, PROTECTED, 0, 0, 1)
                        .assertTransitions(2);
        // @formatter:on

        assertUsers(getNumberOfInitialUsers());
    }

    /**
     * Users from the superclass (administrator, jack) + users stemming from the
     * starting accounts (jgibbs, hbarbossa, jbeckett, user1-3).
     */
    private int getNumberOfInitialUsers() {
        return super.getNumberOfUsers() + getStartingAccounts();
    }

    /**
     * Assigns the rule of "Stop after 4 created users" to the main task.
     */
    @Test
    public void test100AssignCreationLimitToTask() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assignRole(TaskType.class, getTaskOid(), ROLE_STOP_ON_5TH_USER_CREATION.oid, task, result);

        then();
        assertTask(getTaskOid(), "after")
                .display()
                .assertSuspended()
                .assignments()
                    .assertRole(ROLE_STOP_ON_5TH_USER_CREATION.oid)
                    .assertAssignments(1);
    }

    private String getTaskOid() {
        return getTaskResource().oid;
    }

    /**
     * Runs the main task and checks if it stopped after processing expected number of users.
     */
    @Test
    public void test110ImportAccountsFirst() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        openDJController.addEntriesFromLdifFile(LDIF_USERS_FIRST_IMPORT_FILE);

        assertUsers(getNumberOfInitialUsers());

        clearCountersAndStatistics(result);

        when();
        rerunTaskErrorsOk(getTaskOid(), result);

        then();
        TaskType taskAfter = assertTask(getTaskOid(), "after")
                .display()
                .assertSuspended()
                .assertFatalError()
                .getObjectable();

        int expectedUsersImported = isSimulate() ? 0 : RULE_CREATE_WATERMARK-1;
        assertUsers(getNumberOfInitialUsers() + expectedUsersImported);

        assertAfterFirstImport(taskAfter);
    }

    /**
     * Adds more users. Runs the main task again and checks if it again stopped after processing expected number of users.
     */
    @Test
    public void test111ImportAccountsSecond() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        openDJController.addEntriesFromLdifFile(LDIF_USERS_SECOND_IMPORT_FILE);

        int expectedUsersImported = isSimulate() ? 0 : RULE_CREATE_WATERMARK-1;
        assertUsers(getNumberOfInitialUsers() + expectedUsersImported);

        clearCountersAndStatistics(result);

        when();
        rerunTaskErrorsOk(getTaskOid(), result);

        then();
        TaskType taskAfter = assertTask(getTaskOid(), "after")
                .display()
                .assertSuspended()
                .assertFatalError()
                .getObjectable();

        assertUsers(getNumberOfInitialUsers() + 2*expectedUsersImported);

        assertAfterSecondImport(taskAfter);
    }

    private void clearCountersAndStatistics(OperationResult result) throws SchemaException, ObjectAlreadyExistsException,
            ObjectNotFoundException {
        ItemPath activityStatePath = ItemPath.create(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY);
        List<ItemDelta<?, ?>> modifications = deltaFor(TaskType.class)
                .item(activityStatePath.append(ActivityStateType.F_COUNTERS)).replace()
                .item(activityStatePath.append(ActivityStateType.F_STATISTICS)).replace()
                .item(activityStatePath.append(ActivityStateType.F_PROGRESS)).replace()
                .asItemDeltas();
        repositoryService.modifyObject(TaskType.class, getTaskOid(), modifications, result);
    }

    /**
     * Changes the rule to "Stop after having 2 users with changed activation/administrativeStatus".
     */
    @Test
    public void test500AssignModificationLimitToTask() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        unassignRole(TaskType.class, getTaskOid(), ROLE_STOP_ON_5TH_USER_CREATION.oid, task, result);
        assignRole(TaskType.class, getTaskOid(), ROLE_STOP_ON_3RD_STATUS_CHANGE.oid, task, result);

        then();
        assertTask(getTaskOid(), "after")
                .display()
                .assertSuspended()
                .assignments()
                    .assertRole(ROLE_STOP_ON_3RD_STATUS_CHANGE.oid)
                    .assertAssignments(1);
    }

    /**
     * Disables accounts for users1..6 on the resource. Expects to stop after processing 2 of them.
     */
    @Test
    public void test520ImportDisabledAccounts() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        openDJController.executeLdifChanges(LDIF_CHANGE_ACTIVATION_FILE);

        clearCountersAndStatistics(result);

        when();
        rerunTaskErrorsOk(getTaskOid(), result);

        then();
        TaskType taskAfter = assertTask(getTaskOid(), "after")
                .display()
                .assertSuspended()
                .assertFatalError()
                .getObjectable();

        assertAfterDisablingAccounts(taskAfter);
    }

    void dumpSynchronizationInformation(ActivitySynchronizationStatisticsType synchronizationInformation) {
        displayValue("Synchronization information", ActivitySynchronizationStatisticsUtil.format(synchronizationInformation));
    }

    int getReconFailureCount(Task task) {
        return getFailureCount(task, ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_PATH);
    }

    int getRootFailureCount(Task task) {
        return getFailureCount(task, ActivityPath.empty());
    }

    ActivitySynchronizationStatisticsType getReconSyncStats(Task task) {
        return getSyncStats(task, ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_PATH);
    }

    ActivitySynchronizationStatisticsType getRootSyncStats(Task task) {
        return getSyncStats(task, ActivityPath.empty());
    }

    int getFailureCount(Task task, ActivityPath path) {
        ActivityStateType state = ActivityStateUtil.getActivityStateRequired(task.getActivitiesStateOrClone(), path);
        return ActivityItemProcessingStatisticsUtil.getErrorsShallow(state.getStatistics().getItemProcessing());
    }

    ActivitySynchronizationStatisticsType getSyncStats(Task task, ActivityPath path) {
        return ActivityStateUtil.getActivityStateRequired(task.getActivitiesStateOrClone(), path)
                .getStatistics()
                .getSynchronization();
    }
}
