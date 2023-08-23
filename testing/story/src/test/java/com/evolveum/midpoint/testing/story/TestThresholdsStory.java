/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationExclusionReasonType.PROTECTED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.LINKED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.UNMATCHED;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.function.Consumer;

import com.evolveum.midpoint.repo.common.activity.run.buckets.BucketingConfigurationOverrides;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Tests the thresholds.
 *
 * General schema:
 *
 * 1. A set of starting accounts (user1 - user3) is created, and all accounts (these three + jgibbs, hbarbossa, jbeckett)
 * are imported. See {@link #test001ImportBaseUsers()}.
 * 2. The _first import_ is run: accounts user4 - user9 are created, and the task is executed under "stop on 5th add" rule.
 * See {@link #test110ImportAccountsFirst()}. (There are variants according to task type, distribution and execution mode.)
 * 3. The _second import_ is run: accounts user10 - user 15 are created, and the same task is re-executed.
 * See {@link #test111ImportAccountsSecond()}.
 * 4. The _disabled accounts import_ is run: Accounts for users1..6 are disabled on the resource. Task is modified to stop
 * on disabling 3rd user, and executed. See {@link #test520ImportDisabledAccounts()}.
 *
 * There are some extensions in {@link TestThresholdsStoryReconExecute}.
 *
 * @author katka
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class TestThresholdsStory extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "thresholds");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

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

    private static final TestObject<RoleType> ROLE_STOP_ON_5TH_USER_CREATION = TestObject.file(TEST_DIR, "role-stop-on-5th-user-creation.xml", "71478881-f19b-4e8d-a574-76187ee861b2");
    private static final TestObject<RoleType> ROLE_STOP_ON_3RD_STATUS_CHANGE = TestObject.file(TEST_DIR, "role-stop-on-3rd-status-change.xml", "fdd65b29-d892-452a-9260-f26c7f20e507");
    private static final TestObject<TaskType> TASK_IMPORT_BASE_USERS = TestObject.file(TEST_DIR, "task-opendj-import-base-users.xml", "fa25e6dc-a858-11e7-8ebc-eb2b71ecce1d");

    private static final int TASK_TIMEOUT = 60000;

    public static final int RULE_CREATE_WATERMARK = 5;
    public static final int RULE_ACTIVATION_WATERMARK = 3;

    @Autowired CommonTaskBeans commonTaskBeans;

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    protected boolean isAvoidLoggingChange() {
        return false; // we want our own logging
    }

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

    protected abstract TestObject<TaskType> getTaskTestResource();
    protected abstract int getWorkerThreads();
    protected boolean isMultiNode() {
        return false;
    }
    protected abstract void assertAfterFirstImport(TaskType taskAfter) throws Exception;
    protected abstract void assertAfterSecondImport(TaskType taskAfter);
    protected abstract void assertAfterDisablingAccounts(TaskType taskAfter);

    protected boolean isPreview() {
        return false;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        BucketingConfigurationOverrides.setFreeBucketWaitIntervalOverride(3000L); // experimental

        PrismObject<ResourceType> resourceOpenDj = importAndGetObjectFromFile(
                ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
        openDJController.setResource(resourceOpenDj);

        repoAdd(ROLE_STOP_ON_5TH_USER_CREATION, initResult);
        repoAdd(ROLE_STOP_ON_3RD_STATUS_CHANGE, initResult);
    }

    abstract Consumer<PrismObject<TaskType>> getWorkerThreadsCustomizer();

    /**
     * Imports base users in an auxiliary task ("import base users").
     */
    @Test
    public void test001ImportBaseUsers() throws Exception {
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

    String getTaskOid() {
        return getTaskTestResource().oid;
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

        addObject(getTaskTestResource(), task, result,
                aggregateCustomizer(
                        roleAssignmentCustomizer(ROLE_STOP_ON_5TH_USER_CREATION.oid),
                        getWorkerThreadsCustomizer()));

        when();
        runTask(result);

        then();
        TaskType taskAfter = assertTaskAfter();

        int expectedUsersImported = isPreview() ? 0 : RULE_CREATE_WATERMARK-1;
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

        int expectedUsersImported = isPreview() ? 0 : RULE_CREATE_WATERMARK-1;
        assertUsers(getNumberOfInitialUsers() + expectedUsersImported);

        taskManager.deleteTaskTree(getTaskOid(), result);
        addObject(getTaskTestResource(), task, result,
                aggregateCustomizer(
                        roleAssignmentCustomizer(ROLE_STOP_ON_5TH_USER_CREATION.oid),
                        getWorkerThreadsCustomizer()));

        when();
        runTask(result);

        then();
        TaskType taskAfter = assertTaskAfter();

        assertUsers(getNumberOfInitialUsers() + 2*expectedUsersImported);

        assertAfterSecondImport(taskAfter);
    }

//    private void clearCountersAndStatistics(OperationResult result) throws SchemaException, ObjectAlreadyExistsException,
//            ObjectNotFoundException, ActivityRunException {
//        Collection<ActivityState> activityStates = getExecutionStates(result);
//        for (ActivityState activityState : activityStates) {
//            activityState.setItemRealValues(ActivityStateType.F_COUNTERS);
//            activityState.setItemRealValues(ActivityStateType.F_STATISTICS);
//            activityState.setItemRealValues(ActivityStateType.F_PROGRESS);
//            activityState.setItemRealValues(ItemPath.create(ActivityStateType.F_BUCKETING, ActivityBucketingStateType.F_BUCKET));
//            activityState.setItemRealValues(ItemPath.create(ActivityStateType.F_BUCKETING, ActivityBucketingStateType.F_WORK_COMPLETE));
//            activityState.flushPendingModifications(result);
//        }
//    }

    /**
     * Returns the state(s) of activity/activities where the "real execution" takes place: it is the root activity in LS tasks
     * and children activities in reconciliation ones.
     */
    protected abstract Collection<ActivityState> getExecutionStates(OperationResult result) throws SchemaException, ObjectNotFoundException;

    /**
     * Disables accounts for users1..6 on the resource. Expects to stop after processing 2 of them.
     */
    @Test
    public void test520ImportDisabledAccounts() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        openDJController.executeLdifChanges(LDIF_CHANGE_ACTIVATION_FILE);

        taskManager.deleteTaskTree(getTaskOid(), result);
        addObject(getTaskTestResource(), task, result,
                aggregateCustomizer(
                        roleAssignmentCustomizer(ROLE_STOP_ON_3RD_STATUS_CHANGE.oid),
                        getWorkerThreadsCustomizer()));

        when();
        runTask(result);

        then();
        TaskType taskAfter = assertTaskAfter();

        assertAfterDisablingAccounts(taskAfter);
    }

    private void runTask(OperationResult result) throws Exception {
        if (isMultiNode()) {
            runTaskTreeAndWaitForFinish(getTaskOid(), TASK_TIMEOUT);
        } else {
            rerunTaskErrorsOk(getTaskOid(), result);
        }
    }

    /**
     * Special for multi-node reconciliation.
     */
    TaskType assertTaskAfter() throws SchemaException, ObjectNotFoundException {
        return assertTask(getTaskOid(), "after")
                .display()
                .assertSuspended()
                .assertFatalError()
                .getObjectable();
    }
}
