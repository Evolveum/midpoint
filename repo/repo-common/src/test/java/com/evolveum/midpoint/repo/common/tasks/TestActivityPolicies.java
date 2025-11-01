/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.tasks;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.common.AbstractRepoCommonTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

/**
 * This is to comprehensively test activity policies at the low level.
 * Notifications are not here, so we deal only with suspending tasks, restarting and skipping activities.
 *
 * - `test1xx` tests for halting activities when they exceed given execution time
 * - `test2xx` tests for halting activities when they exceed given number of errors (important because of irregular distribution)
 * - `test3xx` tests for skipping activities when they exceed given execution time
 */
@ContextConfiguration(locations = "classpath:ctx-repo-common-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestActivityPolicies extends AbstractRepoCommonTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/activities/policies");

    private static final long DEFAULT_TIMEOUT = 60_000;
    private static final long DEFAULT_SLEEP_TIME = 500;

    private static final TestTask TASK_100_SIMPLE_SUSPEND_ON_EXECUTION_TIME = new TestTask(
            TEST_DIR,
            "task-100-simple-suspend-on-execution-time.xml",
            "18bf2508-4582-4fab-99d4-964fd6d23858",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_110_CHILD_SUSPEND_ON_OWN_EXECUTION_TIME = new TestTask(
            TEST_DIR,
            "task-110-child-suspend-on-own-execution-time.xml",
            "10703f89-f904-443d-9b6b-5f8d7d7264b5",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_120_CHILD_SUSPEND_ON_PARENT_EXECUTION_TIME = new TestTask(
            TEST_DIR,
            "task-120-child-suspend-on-parent-execution-time.xml",
            "1e6d612d-4ae9-4024-a9f9-fe8ed79d9c1f",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_130_CHILD_SUSPEND_ON_OWN_EXECUTION_TIME_WITH_SUBTASKS = new TestTask(
            TEST_DIR,
            "task-130-child-suspend-on-own-execution-time-with-subtasks.xml",
            "234c3ee7-8093-44b9-896d-e2fe2e0b4e47",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_140_CHILD_SUSPEND_ON_PARENT_EXECUTION_TIME_WITH_SUBTASKS = new TestTask(
            TEST_DIR,
            "task-140-child-suspend-on-parent-execution-time-with-subtasks.xml",
            "0dffbdc6-ece5-4504-be5b-455aefb61ff3",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_150_MULTINODE_SUSPEND_ON_EXECUTION_TIME = new TestTask(
            TEST_DIR,
            "task-150-multinode-suspend-on-execution-time.xml",
            "5b5132c5-0c06-4363-8b85-106a92a665d5",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_160_MULTINODE_CHILD_SUSPEND_ON_OWN_EXECUTION_TIME_WITH_SUBTASKS = new TestTask(
            TEST_DIR,
            "task-160-multinode-child-suspend-on-own-execution-time-with-subtasks.xml",
            "c2887c31-43b7-4e0f-85ef-16c2c7ed7f69",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_170_MULTINODE_CHILD_SUSPEND_ON_ROOT_EXECUTION_TIME_WITH_SUBTASKS = new TestTask(
            TEST_DIR,
            "task-170-multinode-child-suspend-on-root-execution-time-with-subtasks.xml",
            "82281150-8f95-4889-82c5-2b9c75abeae0",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_200_SIMPLE_SUSPEND_ON_ITEM_ERRORS = new TestTask(
            TEST_DIR,
            "task-200-simple-suspend-on-item-errors.xml",
            "8f949395-2af0-4728-82e3-a079f1128bcb",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_210_CHILD_SUSPEND_ON_OWN_ITEM_ERRORS = new TestTask(
            TEST_DIR,
            "task-210-child-suspend-on-own-item-errors.xml",
            "7c377ab5-22dc-4c4a-9ee4-6b58224bfffe",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_220_CHILD_SUSPEND_ON_PARENT_ITEM_ERRORS = new TestTask(
            TEST_DIR,
            "task-220-child-suspend-on-parent-item-errors.xml",
            "9bd88e33-7aae-4ebf-bb9e-d940d4bb6bb4",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_230_CHILD_SUSPEND_ON_OWN_ITEM_ERRORS_WITH_SUBTASKS = new TestTask(
            TEST_DIR,
            "task-230-child-suspend-on-own-item-errors-with-subtasks.xml",
            "4fdf1e66-f520-4689-ada7-d19f4441bd2c",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_240_CHILD_SUSPEND_ON_PARENT_ITEM_ERRORS_WITH_SUBTASKS = new TestTask(
            TEST_DIR,
            "task-240-child-suspend-on-parent-item-errors-with-subtasks.xml",
            "82da7afe-3b01-4ecc-9691-34b6a760afae",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_250_MULTINODE_SUSPEND_ON_ITEM_ERRORS = new TestTask(
            TEST_DIR,
            "task-250-multinode-suspend-on-item-errors.xml",
            "7b046dca-ab34-49c4-ae67-f693e7874cde",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_255_MULTINODE_SUSPEND_ON_ITEM_ERRORS_UNBALANCED = new TestTask(
            TEST_DIR,
            "task-255-multinode-suspend-on-item-errors-unbalanced.xml",
            "2e45c709-1cbe-4030-84a6-1e25e37e3846",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_300_SIMPLE_SKIP_ON_EXECUTION_TIME = new TestTask(
            TEST_DIR,
            "task-300-simple-skip-on-execution-time.xml",
            "83a0d9cc-6460-4c64-a047-08da9801f364",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_310_CHILD_SKIP_ON_OWN_EXECUTION_TIME = new TestTask(
            TEST_DIR,
            "task-310-child-skip-on-own-execution-time.xml",
            "e6582e03-d575-4af1-add5-084058eef2d5",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_320_CHILD_SKIP_ON_PARENT_EXECUTION_TIME = new TestTask(
            TEST_DIR,
            "task-320-child-skip-on-parent-execution-time.xml",
            "ee54d77d-4a7c-4279-8b7c-01a4895be46c",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_330_CHILD_SKIP_ON_OWN_EXECUTION_TIME_WITH_SUBTASKS = new TestTask(
            TEST_DIR,
            "task-330-child-skip-on-own-execution-time-with-subtasks.xml",
            "29b26fcd-f6d5-4e5b-a97e-4c0a7c103040",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_340_CHILD_SKIP_ON_PARENT_EXECUTION_TIME_WITH_SUBTASKS = new TestTask(
            TEST_DIR,
            "task-340-child-skip-on-parent-execution-time-with-subtasks.xml",
            "857ee252-8e3a-47b7-bd7a-c9dc40cfc45d",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_350_MULTINODE_SKIP_ON_EXECUTION_TIME = new TestTask(
            TEST_DIR,
            "task-350-multinode-skip-on-execution-time.xml",
            "462daf2c-638f-4208-a6c8-fb015f59e500",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_360_MULTINODE_CHILD_SKIP_ON_OWN_EXECUTION_TIME_WITH_SUBTASKS = new TestTask(
            TEST_DIR,
            "task-360-multinode-child-skip-on-own-execution-time-with-subtasks.xml",
            "3417555b-0af2-483f-8baa-ae392039ebe3",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_370_MULTINODE_CHILD_SKIP_ON_ROOT_EXECUTION_TIME_WITH_SUBTASKS = new TestTask(
            TEST_DIR,
            "task-370-multinode-child-skip-on-root-execution-time-with-subtasks.xml",
            "f64f27d8-55a2-4c7f-8ae9-83af13238fb5",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_400_SIMPLE_RESTART_ON_EXECUTION_TIME = new TestTask(
            TEST_DIR,
            "task-400-simple-restart-on-execution-time.xml",
            "ec8896c5-236b-4f66-a0de-9a35c040b490",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_410_CHILD_RESTART_ON_OWN_EXECUTION_TIME = new TestTask(
            TEST_DIR,
            "task-410-child-restart-on-own-execution-time.xml",
            "8b04e48a-362d-4ec0-b09c-ad5e25115055",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_420_CHILD_RESTART_ON_PARENT_EXECUTION_TIME = new TestTask(
            TEST_DIR,
            "task-420-child-restart-on-parent-execution-time.xml",
            "0efc26ef-3f3b-4113-a1be-a1392fbb79eb",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_430_CHILD_RESTART_ON_OWN_EXECUTION_TIME_WITH_SUBTASKS = new TestTask(
            TEST_DIR,
            "task-430-child-restart-on-own-execution-time-with-subtasks.xml",
            "0dc350ce-6232-4861-81d8-c422726e9fa7",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_440_CHILD_RESTART_ON_PARENT_EXECUTION_TIME_WITH_SUBTASKS = new TestTask(
            TEST_DIR,
            "task-440-child-restart-on-parent-execution-time-with-subtasks.xml",
            "5a7cad09-f7f6-4c55-8714-09b5c73f9a99",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_450_MULTINODE_RESTART_ON_EXECUTION_TIME = new TestTask(
            TEST_DIR,
            "task-450-multinode-restart-on-execution-time.xml",
            "0e8b3300-0e0e-49bd-84c8-c4c9e5c30976",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_460_MULTINODE_CHILD_RESTART_ON_OWN_EXECUTION_TIME_WITH_SUBTASKS = new TestTask(
            TEST_DIR,
            "task-460-multinode-child-restart-on-own-execution-time-with-subtasks.xml",
            "5c7cb832-95d9-48fe-b86b-0278a43b21ee",
            DEFAULT_TIMEOUT);
    private static final TestTask TASK_470_MULTINODE_CHILD_RESTART_ON_ROOT_EXECUTION_TIME_WITH_SUBTASKS = new TestTask(
            TEST_DIR,
            "task-470-multinode-child-restart-on-root-execution-time-with-subtasks.xml",
            "dc89a2c8-7d9b-46be-90a7-9be690ec2faf",
            DEFAULT_TIMEOUT);

    /** Good objects on which we test "fail on error" policies. These complete without failures. */
    private static final int NUMBER_OF_GOOD_OBJECTS = 50;

    /** Bad objects on which we test "fail on error" policies. These fail when processed. */
    private static final int NUMBER_OF_BAD_OBJECTS = 50;

    private static final ActivityPath PATH_FIRST = ActivityPath.fromId("first");
    private static final ActivityPath PATH_SECOND = ActivityPath.fromId("second");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        createTestObjects(initResult);
    }

    /** Create objects used by mock search activities. */
    private void createTestObjects(OperationResult result) throws CommonException {
        repoObjectCreatorFor(ServiceType.class)
                .withObjectCount(NUMBER_OF_GOOD_OBJECTS)
                .withNamePattern("test-good-%03d")
                .execute(result);
        repoObjectCreatorFor(ServiceType.class)
                .withObjectCount(NUMBER_OF_BAD_OBJECTS)
                .withNamePattern("test-bad-%03d")
                .execute(result);
    }

    /**
     * A simple activity that is suspended when it exceeds allowed execution time.
     */
    @Test
    public void test100SimpleSuspendOnExecutionTime() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_100_SIMPLE_SUSPEND_ON_EXECUTION_TIME;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunErrorsOk(result);

        then("the task is suspended after exceeding execution time");
        // @formatter:off
        testTask.assertAfter()
                .display()
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .assertExecutionAttempts(1)
                    .assertFatalError()
                    .assertInProgressLocal()
                    .assertNoCounters()
                    .policies()
                        .assertPolicyCount(1)
                        .policy("Execution time")
                            .assertTriggerCount(1)
                        .end()
                    .end()
                    .itemProcessingStatistics()
                        .assertRunTimeBetween(2000L, 5000L) // limit is 2 seconds, 10 seconds planned
                    .end();
        // @formatter:on
    }

    /**
     * A child activity that is suspended when it exceeds allowed execution time (specified on its own level).
     */
    @Test
    public void test110ChildSuspendOnOwnExecutionTime() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_110_CHILD_SUSPEND_ON_OWN_EXECUTION_TIME;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunErrorsOk(result);

        then("the task is suspended after exceeding execution time");
        // @formatter:off
        testTask.assertAfter()
                .assertSuspended()
                .assertFatalError()
                .activityState(PATH_FIRST)
                    .assertComplete()
                    .assertSuccess()
                    .assertNoCounters()
                    .itemProcessingStatistics()
                        .assertRunTimeBetween(4000L, 5000L) // no limit, 4 seconds planned
                    .end()
                .end()
                .activityState(PATH_SECOND)
                    .assertInProgressLocal()
                    .assertFatalError()
                    .assertNoCounters()
                    .itemProcessingStatistics()
                        .assertRunTimeBetween(8000L, 10000L); // limit is 8 seconds here, 20 seconds planned
        // @formatter:on
    }

    /**
     * A child activity that is suspended when it exceeds allowed execution time (specified on the parent level).
     */
    @Test
    public void test120ChildSuspendOnParentExecutionTime() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_120_CHILD_SUSPEND_ON_PARENT_EXECUTION_TIME;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunErrorsOk(result);

        then("the task is suspended after exceeding execution time");
        // @formatter:off
        testTask.assertAfter()
                .display()
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .assertFatalError()
                    .assertInProgressLocal()
                    .child("first")
                        .assertExecutionAttempts(1)
                        .assertComplete()
                        .assertSuccess()
                        .assertNoCounters()
                        .assertNoPolicies()
                        .itemProcessingStatistics()
                            .assertRunTimeBetween(4000L, 5000L) // 4 seconds planned (under limit of 8 seconds total)
                        .end()
                    .end()
                    .child("second")
                        .assertExecutionAttempts(1)
                        .assertInProgressLocal()
                        .assertFatalError()
                        .assertNoCounters()
                        .policies()
                            .assertPolicyCount(1)
                            .policy("Execution time")
                                .assertTriggerCount(1)
                                .end()
                            .end()
                            .itemProcessingStatistics()
                                .assertRunTimeBetween(3000L, 5000L) // 20 seconds planned, but limit is 8 seconds total
                            .end()
                        .end()
                    .child("third")
                        .assertNotStarted();
    }

    /**
     * As {@link #test110ChildSuspendOnOwnExecutionTime()} but the activities run in subtasks.
     */
    @Test
    public void test130ChildSuspendOnOwnExecutionTimeWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_130_CHILD_SUSPEND_ON_OWN_EXECUTION_TIME_WITH_SUBTASKS;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunTreeErrorsOk(result);

        then("the task tree is suspended after exceeding execution time");
        // @formatter:off
        testTask.assertTreeAfter()
                .assertSuspended()
                // we don't assert FATAL_ERROR here, because the root task itself may have different operationResultStatus
                .rootActivityState()
                    .assertInProgressLocal()
                    // not sure about OperationResult here
                    .child("first")
                        .assertExecutionAttempts(1)
                        .assertComplete()
                        .assertSuccess()
                        .end()
                    .child("second")
                        .assertExecutionAttempts(1)
                        .assertInProgressDelegated()
                        .assertStatusInProgress()
                        .end()
                    .child("third")
                        .assertNotStarted()
                        .end()
                    .end()
                .assertSubtasks(2)
                .subtask("first", false)
                    .display()
                    .assertSuccess()
                    .assertClosed() // this activity finished before suspension
                    .activityState(ActivityPath.fromId("first"))
                        .assertSuccess()
                        .assertNoPolicies()
                        .end()
                    .end()
                .subtask("second", false)
                    .display()
                    .assertFatalError()
                    .assertSuspended() // but this was suspended
                    .activityState(ActivityPath.fromId("second"))
                        .assertFatalError()
                        .policies()
                        .assertPolicyCount(1)
                        .policy("Execution time")
                            .assertTriggerCount(1);
        // @formatter:on
    }

    /**
     * As {@link #test120ChildSuspendOnParentExecutionTime()} but the activities run in subtasks.
     */
    @Test
    public void test140ChildSuspendOnParentExecutionTimeWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_140_CHILD_SUSPEND_ON_PARENT_EXECUTION_TIME_WITH_SUBTASKS;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunTreeErrorsOk(result);

        then("the task tree is suspended after exceeding execution time");
        // @formatter:off
        testTask.assertTreeAfter()
                .assertSuspended()
                .assertSubtasks(2)
                .subtask("first", false)
                    .display()
                    .assertClosed() // this activity finished before suspension
                    .activityState(ActivityPath.fromId("first"))
                        .assertNoCounters()
                        .assertNoPolicies()
                        .end()
                    .end()
                .subtask("second", false)
                    .display()
                    .assertSuspended() // but this was suspended
                    .activityState(ActivityPath.fromId("second"))
                        .assertNoCounters()
                        .policies()
                            .assertPolicyCount(1)
                            .policy("Execution time")
                                .assertTriggerCount(1);
        // @formatter:on
    }

    /**
     * A multi-node activity that is suspended when it exceeds allowed execution time.
     */
    @Test
    public void test150MultinodeSuspendOnExecutionTime() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_150_MULTINODE_SUSPEND_ON_EXECUTION_TIME;
        testTask.init(this, task, result);

        when("task is run until it's stopped (some workers may continue for a little while)");
        testTask.rerunTreeErrorsOk(result);

        and("the task and ALL workers are suspended");
        waitForTaskTreeCloseOrCondition(
                testTask.oid,
                result,
                DEFAULT_TIMEOUT,
                DEFAULT_SLEEP_TIME,
                tasksSuspendedPredicate(3)); // main task + 2 workers

        then("everything is OK");
        // @formatter:off
        testTask.assertTreeAfter()
                .display()
                .assertSuspended() // already checked above
                .assertSubtasks(2)
                .rootActivityState()
                    .assertStatusInProgress()
                    .assertNoCounters()
                    .policies()
                        .assertPolicyCount(1)
                        .policy("Execution time")
                            .assertTriggerCount(1)
                            .end()
                        .end()
                    .end()
                .subtask(0)
                    .display()
                    .assertSuspended()
                    .assertFatalError()
                    .end()
                .subtask(1)
                    .display()
                    .assertSuspended()
                    .assertFatalError();
        // @formatter:on
    }

    /**
     * A child multi-node activity that is suspended when it exceeds allowed execution time (specified on its own level).
     * Children have its own subtasks.
     *
     * It's a combination of {@link #test130ChildSuspendOnOwnExecutionTimeWithSubtasks()}
     * and {@link #test150MultinodeSuspendOnExecutionTime()}.
     */
    @Test
    public void test160MultinodeSuspendOnOwnExecutionTimeWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TestTask testTask = TASK_160_MULTINODE_CHILD_SUSPEND_ON_OWN_EXECUTION_TIME_WITH_SUBTASKS; // too long to be typed
        testTask.init(this, task, result);

        when("task is run until it's stopped (some workers may continue for a little while)");
        testTask.rerunTreeErrorsOk(result);

        then("root task is suspended and the first activity task is closed");
        // @formatter:off
        testTask.assertTree("")
                .display()
                .assertSuspended()
                .rootActivityState()
                    .child("first")
                        .assertComplete()
                        .assertSuccess()
                        .assertNoCounters()
                        .assertNoPolicies()
                        .end()
                    .end()
                .subtask("first", false)
                    .display()
                    .assertClosed()
                    .assertSuccess()
                    .activityState(ActivityPath.fromId("first"))
                        .assertComplete()
                        .assertSuccess()
                        .assertNoPolicies()
                        .assertNoCounters();
        // @formatter:on

        then("the second activity task and ALL its workers are suspended after exceeding execution time");
        // @formatter:off
        var secondActivityTaskOid =
                testTask.assertTree("")
                    .display()
                    .subtask("second", false)
                    .getOid();
        // @formatter:on

        waitForTaskTreeCloseOrCondition(
                secondActivityTaskOid,
                result,
                DEFAULT_TIMEOUT,
                DEFAULT_SLEEP_TIME,
                tasksSuspendedPredicate(3)); // coordinator + 2 workers

        // @formatter:off
        assertTaskTree(secondActivityTaskOid, "second activity task after")
                .display()
                .assertSuspended()
                .assertInProgress()
                .activityState(ActivityPath.fromId("second"))
                    .assertStatusInProgress()
                    .policies()
                        .assertPolicyCount(1)
                        .policy("Execution time")
                            .assertTriggerCount(1)
                            .end()
                        .end()
                    .end()
                .assertSubtasks(2)
                .subtask(0)
                    .display()
                    .assertFatalError()
                    .assertSuspended()
                    .activityState(ActivityPath.fromId("second")) // worker state for second activity
                        .assertFatalError()
                        .assertInProgressLocal()
                        .assertNoCounters()
                        .assertNoPolicies()
                        .end()
                    .end()
                .subtask(0)
                    .display()
                    .assertFatalError()
                    .assertSuspended()
                    .activityState(ActivityPath.fromId("second")) // worker state for second activity
                        .assertFatalError()
                        .assertInProgressLocal()
                        .assertNoCounters()
                        .assertNoPolicies();
        // @formatter:on
    }

    /**
     * A child multi-node activity that is suspended when it exceeds allowed execution time (specified on the root level).
     * Children have its own subtasks.
     *
     * As {@link #test160MultinodeSuspendOnOwnExecutionTimeWithSubtasks()} but the constraint is on the root level.
     */
    @Test
    public void test170MultinodeSuspendOnRootExecutionTimeWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TestTask testTask = TASK_170_MULTINODE_CHILD_SUSPEND_ON_ROOT_EXECUTION_TIME_WITH_SUBTASKS; // too long to be typed
        testTask.init(this, task, result);

        when("task is run until it's stopped (some workers may continue for a little while)");
        testTask.rerunTreeErrorsOk(result);

        then("root task is suspended and the first activity task is closed");
        // @formatter:off
        testTask.assertTree("")
                .display()
                .assertSuspended()
                .rootActivityState()
                    .child("first")
                        .assertComplete()
                        .assertSuccess()
                        .assertNoCounters()
                        .assertNoPolicies()
                        .end()
                    .end()
                .subtask("first", false)
                    .display()
                    .assertClosed()
                    .assertSuccess()
                    .activityState(ActivityPath.fromId("first"))
                        .assertComplete()
                        .assertSuccess()
                        .assertNoPolicies()
                        .assertNoCounters();
        // @formatter:on

        then("the second activity task and ALL its workers are suspended after exceeding execution time");
        // @formatter:off
        var secondActivityTaskOid = testTask.assertTree("")
                .subtask("second", false)
                .getOid();
        // @formatter:on

        waitForTaskTreeCloseOrCondition(
                secondActivityTaskOid,
                result,
                DEFAULT_TIMEOUT,
                DEFAULT_SLEEP_TIME,
                tasksSuspendedPredicate(3)); // coordinator + 2 workers

        // @formatter:off
        assertTaskTree(secondActivityTaskOid, "second activity task after")
                .display()
                .assertSuspended()
                .assertInProgress()
                .activityState(ActivityPath.fromId("second"))
                    .assertStatusInProgress()
                    .policies()
                        .assertPolicyCount(1)
                        .policy("Execution time")
                            .assertTriggerCount(1)
                            .end()
                        .end()
                    .end()
                .assertSubtasks(2)
                .subtask(0)
                    .display()
                    .assertFatalError()
                    .assertSuspended()
                    .activityState(ActivityPath.fromId("second")) // worker state for second activity
                        .assertFatalError()
                        .assertInProgressLocal()
                        .assertNoCounters()
                        .assertNoPolicies()
                        .end()
                    .end()
                .subtask(0)
                    .display()
                    .assertFatalError()
                    .assertSuspended()
                    .activityState(ActivityPath.fromId("second")) // worker state for second activity
                        .assertFatalError()
                        .assertInProgressLocal()
                        .assertNoCounters()
                        .assertNoPolicies();
        // @formatter:on
    }

    /**
     * A simple activity that is suspended when it exceeds given number of errors.
     */
    @Test
    public void test200SimpleSuspendOnErrors() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TestTask testTask = TASK_200_SIMPLE_SUSPEND_ON_ITEM_ERRORS;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunErrorsOk(result);

        var stopCounterIdentifier =
                testTask.buildPolicyIdentifier(
                        ActivityPath.empty(),
                        "Stop after");

        then("the task is suspended after exceeding given number of errors");
        // @formatter:off
        testTask.assertAfter()
                .display()
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .assertInProgressLocal()
                    .assertFatalError()
                    .fullExecutionModePolicyRulesCounters()
                        .assertCounter(stopCounterIdentifier, 5)
                        .end()
                    .policies()
                        .assertPolicyCount(1)
                        .policy("Stop after")
                            .assertTriggerCount(1);
        // @formatter:on
    }

    /**
     * A child activity that is suspended when it exceeds given number of errors (specified on its own level).
     */
    @Test
    public void test210ChildSuspendOnOwnErrors() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_210_CHILD_SUSPEND_ON_OWN_ITEM_ERRORS;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunErrorsOk(result);

        var stopCounterIdentifier =
                testTask.buildPolicyIdentifier(
                        PATH_SECOND,
                        "Stop after");

        then("the task is suspended after exceeding number of errors");
        // @formatter:off
        testTask.assertAfter()
                .display()
                .assertSuspended()
                .assertFatalError()
                .activityState(PATH_FIRST)
                    .assertComplete()
                    .assertPartialError()
                    .assertNoCounters()
                    .assertNoPolicies()
                    .itemProcessingStatistics()
                        .assertTotalCounts(9,1,0)
                        .end()
                    .end()
                .activityState(PATH_SECOND)
                    .assertInProgressLocal()
                    .assertFatalError()
                    .fullExecutionModePolicyRulesCounters()
                        .assertCounter(stopCounterIdentifier,5)
                            .end()
                    .policies()
                        .assertPolicyCount(1)
                        .policy("Stop after")
                            .assertTriggerCount(1)
                            .end()
                        .end()
                    .itemProcessingStatistics()
                        .assertFailureCount(5);
        // @formatter:on
    }

    /**
     * A child activity that is suspended when it exceeds given number of errors (specified on the parent level).
     */
    @Test
    public void test220ChildSuspendOnParentErrors() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_220_CHILD_SUSPEND_ON_PARENT_ITEM_ERRORS;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunErrorsOk(result);

        var stopCounterIdentifier =
                testTask.buildPolicyIdentifier(
                        ActivityPath.empty(),
                        "Stop after");

        then("the task is suspended after exceeding execution time");
        // @formatter:off
        testTask.assertAfter()
                .display()
                .assertSuspended()
                .assertFatalError()
                .activityState(PATH_FIRST)
                    .assertComplete()
                    .assertPartialError()
                    .fullExecutionModePolicyRulesCounters()
                        .assertCounter(stopCounterIdentifier, 1)
                        .end()
                    .assertNoPolicies()
                    .itemProcessingStatistics()
                        .assertTotalCounts(9,1,0)
                        .end()
                    .end()
                .activityState(PATH_SECOND)
                    .assertInProgressLocal()
                    .assertFatalError()
                    .fullExecutionModePolicyRulesCounters()
                        .assertCounter(stopCounterIdentifier,4)
                        .end()
                    .policies()
                        .assertPolicyCount(1)
                        .policy("Stop after")
                            .assertTriggerCount(1)
                            .end()
                        .end()
                    .itemProcessingStatistics()
                        .assertFailureCount(4);
        // @formatter:on
    }

    /**
     * As {@link #test210ChildSuspendOnOwnErrors()} but the activities run in subtasks.
     */
    @Test
    public void test230ChildSuspendOnOwnErrorsWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_230_CHILD_SUSPEND_ON_OWN_ITEM_ERRORS_WITH_SUBTASKS;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunTreeErrorsOk(result);

        final String stopAfterPolicyCounterIdentifier =
                testTask.buildPolicyIdentifier(
                        ActivityPath.fromId("second"),
                        "Stop after");

        then("the task tree is suspended after exceeding given error count");
        // @formatter:off
        testTask.assertTreeAfter()
                .display()
                .assertSuspended()
                .assertSubtasks(2)
                .subtask("first", false)
                    .display()
                    .assertPartialError() // there is one error
                    .assertClosed() // this activity finished before suspension
                    .activityState(PATH_FIRST)
                        .assertPartialError()
                        .assertComplete()
                        .assertNoCounters()
                        .assertNoPolicies()
                        .itemProcessingStatistics()
                            .assertTotalCounts(9,1,0)
                            .end()
                        .end()
                    .end()
                .subtask("second", false)
                    .display()
                    .assertFatalError()
                    .assertSuspended() // but this was suspended
                    .activityState(PATH_SECOND)
                        .assertFatalError()
                        .itemProcessingStatistics()
                            .assertFailureCount(5)
                            .end()
                        .fullExecutionModePolicyRulesCounters()
                            .assertCounter(stopAfterPolicyCounterIdentifier,5)
                            .end()
                        .policies()
                            .assertPolicyCount(1)
                            .policy("Stop after")
                                .assertTriggerCount(1);
        // @formatter:on
    }

    /**
     * As {@link #test220ChildSuspendOnParentErrors()} but the activities run in subtasks.
     */
    @Test
    public void test240ChildSuspendOnParentErrorsWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_240_CHILD_SUSPEND_ON_PARENT_ITEM_ERRORS_WITH_SUBTASKS;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunTreeErrorsOk(result);

        final String stopCounterIdentifier =
                testTask.buildPolicyIdentifier(
                        ActivityPath.empty(),
                        "Stop after");

        then("the task tree is suspended after exceeding given error count");
        // @formatter:off
        testTask.assertTreeAfter()
                .display()
                .assertSuspended()
                .assertSubtasks(2)
                .subtask("first", false)
                    .display()
                    .assertPartialError()
                    .assertClosed() // this activity finished before suspension
                    .activityState(ActivityPath.fromId("first"))
                        .fullExecutionModePolicyRulesCounters()
                            .assertCounter(stopCounterIdentifier,1)
                            .end()
                        .assertNoPolicies()
                        .end()
                    .end()
                .subtask("second", false)
                    .display()
                    .assertSuspended() // but this was suspended
                    .activityState(ActivityPath.fromId("second"))
                        .assertFatalError()
                        .assertInProgressLocal()
                        .fullExecutionModePolicyRulesCounters()
                            .assertCounter(stopCounterIdentifier,4)
                            .end()
                        .policies()
                            .policy("Stop after")
                                .assertTriggerCount(1);
        // @formatter:on
    }

    /**
     * A multinode activity that is suspended when it exceeds given number of errors.
     */
    @Test
    public void test250MultinodeSuspendOnErrors() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TestTask testTask = TASK_250_MULTINODE_SUSPEND_ON_ITEM_ERRORS;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunTreeErrorsOk(result);

        // 50 bad objects (~ 25 per task), delay is 400 milliseconds, so each task would normally run for about 10 seconds.

        and("giving both workers a chance to stop");
        Thread.sleep(5000);

        then("the task is suspended after exceeding given number of errors");
        // @formatter:off
        testTask.assertTreeAfter()
                .assertSuspended()
                .assertSubtasks(2)
                .subtask(0)
                    .display()
                    .assertSuspended()
                    .end()
                .subtask(1)
                    .display()
                    .assertSuspended()
                    .end();
        // @formatter:on
        // TODO more asserts
    }

    /**
     * As {@link #test250MultinodeSuspendOnErrors()} but the workers are unbalanced: one worker processes only good objects,
     * the other only bad objects. They both should stop when the error threshold is reached.
     */
    // FIXME currently failing, because we don't have the mechanism to notify other workers when one worker suspends the task
    @Test(enabled = false)
    public void test255MultinodeSuspendOnErrorsUnbalanced() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TestTask testTask = TASK_255_MULTINODE_SUSPEND_ON_ITEM_ERRORS_UNBALANCED;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunTreeErrorsOk(result);

        // 50 good objects, 50 bad objects, delay is 200 milliseconds, so the tasks would normally run for about 10 seconds.

        and("giving a chance to the other worker to stop as well");
        Thread.sleep(5000);

        then("all tasks are suspended after exceeding given number of errors");
        // @formatter:off
        testTask.assertTreeAfter()
                .assertSuspended()
                .assertSubtasks(2)
                .subtask(0)
                    .display()
                    .assertSuspended()
                .end()
                .subtask(1)
                    .display()
                    .assertSuspended()
                .end();
        // @formatter:on
        // TODO more asserts
    }

    /**
     * A simple activity that is skipped when it exceeds allowed execution time.
     */
    @Test
    public void test300SimpleSkipOnExecutionTime() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TASK_300_SIMPLE_SKIP_ON_EXECUTION_TIME.init(this, task, result);

        when("task is run until it's stopped");
        TASK_300_SIMPLE_SKIP_ON_EXECUTION_TIME.rerunErrorsOk(result);

        then("the task is suspended after exceeding execution time");
        // @formatter:off
        TASK_300_SIMPLE_SKIP_ON_EXECUTION_TIME.assertAfter()
                .display()
                .assertClosed()
                //.assertFatalError()
                .rootActivityState()
                    .assertExecutionAttempts(1)
                    .assertAborted()
                    .assertFatalError()
                    .policies()
                        .assertPolicyCount(1)
                        .policy("Execution time")
                            .assertTriggerCount(1)
                            .end()
                        .end()
                    .end();
        // @formatter:on
        // TODO more asserts
    }

    /**
     * A child activity that is skipped when it exceeds allowed execution time (specified on its own level).
     */
    @Test
    public void test310ChildSkipOnOwnExecutionTime() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_310_CHILD_SKIP_ON_OWN_EXECUTION_TIME;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunErrorsOk(result);

        then("the task is finished and second activity skipped after exceeding execution time");
        // @formatter:off
        testTask.assertAfter()
                .display()
                .assertClosed()
                .assertFatalError()
                .rootActivityState()
                    .assertExecutionAttempts(1)
                    .assertComplete()
                    .assertFatalError()
                    .assertNoPolicies()
                    .child("first")
                        .assertExecutionAttempts(1)
                        .assertComplete()
                        .assertSuccess()
                        .assertNoPolicies()
                        .end()
                    .child("second")
                        .assertExecutionAttempts(1)
                        .assertAborted()
                        .assertFatalError()
                        .policies()
                            .assertPolicyCount(1)
                            .policy("Execution time")
                                .assertTriggerCount(1)
                                .end()
                            .end()
                        .end()
                    .child("third")
                        .assertExecutionAttempts(1)
                        .assertComplete()
                        .assertSuccess()
                        .assertNoPolicies()
                        .end()
                    .end()
                .end();
        // @formatter:on
    }

    /**
     * A child activity that is skipped when it exceeds allowed execution time (specified on the parent level).
     */
    @Test
    public void test320ChildSkipOnParentExecutionTime() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_320_CHILD_SKIP_ON_PARENT_EXECUTION_TIME;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunErrorsOk(result);

        then("the task is finished and second activity skipped after exceeding execution time; no third activity run");
        // @formatter:off
        testTask.assertAfter()
                .display()
                .assertClosed()
                .assertFatalError()
                .rootActivityState()
                    .assertExecutionAttempts(1)
                    .assertAborted()
                    .assertFatalError()
                    .assertNoPolicies()
                    .assertChildren(3)
                    .child("first")
                        .assertExecutionAttempts(1)
                        .assertComplete()
                        .assertSuccess()
                        .assertNoPolicies()
                        .end()
                    .child("second")
                        .assertExecutionAttempts(1)
                        .assertAborted()
                        .assertFatalError()
                        .policies() // FIXME this should be moved to root activity
                            .assertPolicyCount(1)
                            .policy("Execution time")
                                .assertTriggerCount(1)
                                .assertTriggerCount(1)
                            .end()
                        .end()
                    .end()
                    .child("third")
                        .assertNotStarted()
                    .end()
                .end();
        // @formatter:on
    }

    /**
     * As {@link #test310ChildSkipOnOwnExecutionTime()} but the activities run in subtasks.
     */
    @Test
    public void test330ChildSkipOnOwnExecutionTimeWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_330_CHILD_SKIP_ON_OWN_EXECUTION_TIME_WITH_SUBTASKS;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunTreeErrorsOk(result);

        then("the task tree is finished, second activity skipped after exceeding execution time");
        // @formatter:off
        testTask.assertTreeAfter()
                .display()
                .assertClosed()
                .assertSubtasks(3)
                .subtask("first", false)
                    .display()
                    .assertSuccess()
                    .assertClosed()
                .end()
                .subtask("second", false)
                    .display()
                    .assertFatalError()
                    .assertClosed() // this
                .end()
                .subtask("third", false)
                    .display()
                    .assertSuccess()
                    .assertClosed() // this one continues after second is skipped
                .end();
        // @formatter:on
        // TODO more asserts
    }

    /**
     * As {@link #test320ChildSkipOnParentExecutionTime()} but the activities run in subtasks.
     */
    @Test
    public void test340ChildSkipOnParentExecutionTimeWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_340_CHILD_SKIP_ON_PARENT_EXECUTION_TIME_WITH_SUBTASKS;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunTreeErrorsOk(result);

        then("the task tree is suspended after exceeding execution time");
        // @formatter:off
        testTask.assertTreeAfter()
                .display()
                .assertClosed()
                .assertSubtasks(2)
                .subtask("first", false)
                    .display()
                    .assertSuccess()
                    .assertClosed() // this activity finished before suspension
                    .end()
                .subtask("second", false)
                    .display()
                    .assertFatalError()
                    .assertClosed() // but this was suspended
                    .end();
        // @formatter:on
        // TODO more asserts
    }

    /**
     * A multi-node activity that is skipped when it exceeds allowed execution time.
     */
    @Test
    public void test350MultinodeSkipOnExecutionTime() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TASK_350_MULTINODE_SKIP_ON_EXECUTION_TIME.init(this, task, result);

        when("task is run until it's stopped (some workers may continue for a little while)");
        TASK_350_MULTINODE_SKIP_ON_EXECUTION_TIME.rerunTreeErrorsOk(result);

        and("the task and ALL workers are suspended");
        waitForTaskTreeCloseOrCondition(
                TASK_350_MULTINODE_SKIP_ON_EXECUTION_TIME.oid,
                result,
                DEFAULT_TIMEOUT,
                DEFAULT_SLEEP_TIME,
                tasksSuspendedPredicate(3)); // main task + 2 workers

        then("everything is OK");
        // @formatter:off
        TASK_350_MULTINODE_SKIP_ON_EXECUTION_TIME.assertTreeAfter()
                .assertClosed() // already checked above
                .assertSubtasks(2)
                .subtask(0)
                    .display()
                    .end()
                .subtask(1)
                    .display()
                    .end();
        // @formatter:on
        // TODO more asserts
    }

    /**
     * A child multi-node activity that is skipped when it exceeds allowed execution time (specified on its own level).
     * Children have its own subtasks.
     *
     * It's a combination of {@link #test330ChildSkipOnOwnExecutionTimeWithSubtasks()}
     * and {@link #test350MultinodeSkipOnExecutionTime()}.
     */
    @Test
    public void test360MultinodeSkipOnOwnExecutionTimeWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TestTask testTask = TASK_360_MULTINODE_CHILD_SKIP_ON_OWN_EXECUTION_TIME_WITH_SUBTASKS; // too long to be typed
        testTask.init(this, task, result);

        when("task is run until it's stopped (some workers may continue for a little while)");
        testTask.rerunTreeErrorsOk(result);

        then("root task is closed");
        // @formatter:off
        testTask.assertTree("")
                .assertClosed()
                .subtask("first", false)
                    .assertClosed()
                    .assertSuccess()
                .end()
                .subtask("second", false)
                    .assertClosed()
                    .assertSubtasks(2)
                        .subtask(0)
                        .display()
                    .end()
                        .subtask(1)
                        .display()
                    .end()
                .end()
                .subtask("third", false)
                    .assertClosed()
                .end();
        // @formatter:on
        // TODO more asserts
    }

    /**
     * A child multi-node activity that is skipped when it exceeds allowed execution time (specified on the root level).
     * Children have its own subtasks.
     *
     * As {@link #test360MultinodeSkipOnOwnExecutionTimeWithSubtasks()}} but the constraint is on the root level.
     */
    @Test
    public void test370MultinodeSkipOnRootExecutionTimeWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TestTask testTask = TASK_370_MULTINODE_CHILD_SKIP_ON_ROOT_EXECUTION_TIME_WITH_SUBTASKS; // too long to be typed
        testTask.init(this, task, result);

        when("task is run until it's stopped (some workers may continue for a little while)");
        testTask.rerunTreeErrorsOk(result);

        then("root task is closed");
        // @formatter:off
        testTask.assertTree("")
                .assertClosed()
                .subtask("first", false)
                    .assertClosed()
                    .assertSuccess()
                .end()
                .subtask("second", false)
                    .assertClosed()
                    .assertSubtasks(2)
                        .subtask(0)
                        .display()
                    .end()
                        .subtask(1)
                        .display()
                    .end()
                .end();
                // there is no third subtask, because the parent activity was aborted when second one was executing
        // @formatter:on
        // TODO more asserts
    }

    /**
     * A simple activity that is restarted when it exceeds allowed execution time.
     */
    @Test
    public void test400SimpleRestartOnExecutionTime() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TASK_400_SIMPLE_RESTART_ON_EXECUTION_TIME.init(this, task, result);

        when("task is run until it's stopped");
        TASK_400_SIMPLE_RESTART_ON_EXECUTION_TIME.rerunErrorsOk(result);

        then("the task finished after one restart after exceeding execution time");
        // @formatter:off
        TASK_400_SIMPLE_RESTART_ON_EXECUTION_TIME.assertAfter()
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .assertExecutionAttempts(2)
                    .assertComplete()
                    .assertSuccess()
                    .itemProcessingStatistics()
                        .assertTotalCounts(10, 0, 0)
                        .end()
                    // after first restart there, task should finish successfully, policy not activated
                    .assertNoPolicies()
                    .end();
        // @formatter:on
        // TODO more asserts
    }

    /**
     * A child activity that is skipped when it exceeds allowed execution time (specified on its own level).
     */
    @Test
    public void test410ChildRestartOnOwnExecutionTime() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TASK_410_CHILD_RESTART_ON_OWN_EXECUTION_TIME.init(this, task, result);

        when("task is run until it's stopped");
        TASK_410_CHILD_RESTART_ON_OWN_EXECUTION_TIME.rerunErrorsOk(result);

        then("the task is finished and second activity was restarted once after exceeding execution time");
        // @formatter:off
        TASK_410_CHILD_RESTART_ON_OWN_EXECUTION_TIME.assertAfter()
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .assertExecutionAttempts(1)
                    .assertComplete()
                    .assertSuccess()
                    .assertNoPolicies()
                    .child("first")
                        .assertExecutionAttempts(1)
                        .assertComplete()
                        .assertSuccess()
                        .assertNoPolicies()
                        .end()
                    .child("second")
                        .assertExecutionAttempts(2)
                        .assertComplete()
                        .assertSuccess()
                        .assertNoPolicies()
                        .end()
                    .child("third")
                        .assertExecutionAttempts(1)
                        .assertComplete()
                        .assertSuccess()
                        .assertNoPolicies()
                        .end()
                    .end()
                .end();
        // @formatter:on
        // TODO more asserts
    }

    /**
     * A child activity that is skipped when it exceeds allowed execution time (specified on the parent level).
     */
    @Test
    public void test420ChildRestartOnParentExecutionTime() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TASK_420_CHILD_RESTART_ON_PARENT_EXECUTION_TIME.init(this, task, result);

        when("task is run until it's stopped");
        TASK_420_CHILD_RESTART_ON_PARENT_EXECUTION_TIME.rerunErrorsOk(result);

        then("the task finish, second activity restarted once after exceeding execution time");
        // @formatter:off
        TASK_420_CHILD_RESTART_ON_PARENT_EXECUTION_TIME.assertAfter()
                .display()
                .assertClosed()
                //.assertFatalError()
                .rootActivityState()
                    .assertExecutionAttempts(2)
                    .assertComplete()
                    .assertSuccess()
                    .assertNoPolicies()
                    .child("first")
                        .assertExecutionAttempts(1)
                        .assertComplete()
                        .assertSuccess()
                        .assertNoPolicies()
                        .end()
                    .child("second")
                        .assertExecutionAttempts(1)
                        .assertComplete()
                        .assertSuccess()
                        .assertNoPolicies()
                        .end()
                    .child("third")
                        .assertExecutionAttempts(1)
                        .assertComplete()
                        .assertSuccess()
                        .assertNoPolicies()
                        .end()
                    .end()
                .end();
        // @formatter:on
        // TODO more asserts
    }

    /**
     * As {@link #test410ChildRestartOnOwnExecutionTime()} but the activities run in subtasks.
     */
    @Test
    public void test430ChildRestartOnOwnExecutionTimeWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_430_CHILD_RESTART_ON_OWN_EXECUTION_TIME_WITH_SUBTASKS;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunTreeErrorsOk(result);

        Thread.sleep(10000); // FIXME we need to give tasks a time to restart

        // @formatter:off
        then("the task tree is finished, second activity (subtask) restarted once after exceeding execution time");
        testTask.assertTreeAfter()
                .display()
                .assertClosed()
                .assertSubtasks(3)
                .subtask("first", false)
                    .display()
                    .assertSuccess()
                    .assertClosed() // this activity finished before suspension
                    .end()
                .subtask("second", false)
                    .assertSuccess()
                    .assertClosed() // this one was restarted
                    .display()
                    .end()
                .subtask("third", false)
                    .display()
                    .assertSuccess()
                    .assertClosed()
                    .end();
                    // todo where to assert executionAttempts? should it be in child task or parent tree?
        // @formatter:on
        // TODO more asserts
    }

    /**
     * As {@link #test420ChildRestartOnParentExecutionTime()} but the activities run in subtasks.
     */
    @Test
    public void test440ChildRestartOnParentExecutionTimeWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var testTask = TASK_440_CHILD_RESTART_ON_PARENT_EXECUTION_TIME_WITH_SUBTASKS;
        testTask.init(this, task, result);

        when("task is run until it's stopped");
        testTask.rerunTreeErrorsOk(result);

        then("the task finishes, task (root activity) is restarted after exceeding execution time");  // todo fix
        // @formatter:off
        testTask.assertTreeAfter()
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .assertExecutionAttempts(2)
                    .end()
                .assertSubtasks(3)
                .subtask("first", false)
                    .display()
                    .assertSuccess()
                    .assertClosed() // this activity finished before suspension
                    .end()
                .subtask("second", false)
                    .display()
                    .assertSuccess()
                    .assertClosed() // but this was restarted
                    .end()
                .subtask("third", false)
                    .display()
                    .assertSuccess()
                    .assertClosed()
                    .end();
            // todo how to assert executionAttempts for child tasks?
        // @formatter:on
        // TODO more asserts
    }

    /**
     * A multi-node activity that is skipped when it exceeds allowed execution time.
     */
    @Test
    public void test450MultinodeRestartOnExecutionTime() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TASK_450_MULTINODE_RESTART_ON_EXECUTION_TIME.init(this, task, result);

        when("task is run until it's restarted");
        TASK_450_MULTINODE_RESTART_ON_EXECUTION_TIME.rerunTreeErrorsOk(result);

        and("the task and ALL workers are closed"); // they should finish successfully after restart
        waitForTaskTreeCloseOrCondition(
                TASK_450_MULTINODE_RESTART_ON_EXECUTION_TIME.oid,
                result,
                DEFAULT_TIMEOUT,
                DEFAULT_SLEEP_TIME,
                tasksClosedPredicate(3)); // main task + 2 workers

        then("everything is OK");
        // @formatter:off
        TASK_450_MULTINODE_RESTART_ON_EXECUTION_TIME.assertTreeAfter()// todo fix
                .assertClosed() // already checked above
                .rootActivityState()
                    .assertExecutionAttempts(2)
                    .end()
                .assertSubtasks(2)
                .subtask(0)
                    .display()
                    .end()
                .subtask(1)
                    .display()
                    .end();
        // @formatter:on
        // TODO more asserts
    }

    /**
     * A child multi-node activity that is restarted once when it exceeds allowed execution time (specified on its own level).
     * Children have its own subtasks.
     *
     * It's a combination of {@link #test430ChildRestartOnOwnExecutionTimeWithSubtasks()}
     * and {@link #test450MultinodeRestartOnExecutionTime()}.
     */
    @Test
    public void test460MultinodeRestartOnOwnExecutionTimeWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TestTask testTask = TASK_460_MULTINODE_CHILD_RESTART_ON_OWN_EXECUTION_TIME_WITH_SUBTASKS; // too long to be typed
        testTask.init(this, task, result);

        when("task is run until it's stopped (some workers may continue for a little while)");
        testTask.rerunTreeErrorsOk(result);

        then("root task is closed");
        // @formatter:off
        testTask.assertTree("")
                .assertClosed()
                .subtask("first", false)
                    .assertClosed()
                    .assertSuccess()
                .end()
                .subtask("second", false)
                    .assertClosed()
                    .assertSubtasks(2)
                        .subtask(0)
                        .display()
                    .end()
                        .subtask(1)
                        .display()
                    .end()
                .end()
                .subtask("third", false)
                    .assertClosed()
                .end();
        // @formatter:on
        // TODO more asserts
    }

    /**
     * A child multi-node activity that is restarted when it exceeds allowed execution time (specified on the root level).
     * Children have its own subtasks.
     *
     * As {@link #test460MultinodeRestartOnOwnExecutionTimeWithSubtasks()} but the constraint is on the root level.
     */
    @Test
    public void test470MultinodeRestartOnRootExecutionTimeWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TestTask testTask = TASK_470_MULTINODE_CHILD_RESTART_ON_ROOT_EXECUTION_TIME_WITH_SUBTASKS; // too long to be typed
        testTask.init(this, task, result);

        when("task is run until it's stopped (some workers may continue for a little while)");
        testTask.rerunTreeErrorsOk(result);

        then("root task is closed");
        // @formatter:off
        testTask.assertTree("")
                .assertClosed()
                .subtask("first", false)
                    .assertClosed()
                    .assertSuccess()
                .end()
                .subtask("second", false)
                    .assertClosed()
                    .assertSubtasks(2)
                        .subtask(0)
                        .display()
                    .end()
                        .subtask(1)
                        .display()
                    .end()
                .end()
                .subtask("third", false)
                    .assertClosed()
                .end();
        // @formatter:on
        // TODO more asserts
    }
}
