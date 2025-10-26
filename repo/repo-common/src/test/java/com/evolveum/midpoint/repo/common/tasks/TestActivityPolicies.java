/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.tasks;

import java.io.File;

import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.common.AbstractRepoCommonTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestTask;

/**
 * This is to comprehensively test activity policies at the low level.
 * Notifications are not here, so we deal only with suspending tasks, restarting and skipping activities.
 *
 * - `test1xx` tests for halting activities when they exceed given execution time
 * - `test2xx` tests for halting activities when they exceed given number of errors (important because of irregular distribution)
 */
@ContextConfiguration(locations = "classpath:ctx-repo-common-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestActivityPolicies extends AbstractRepoCommonTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/activities/policies");

    private static final long DEFAULT_TIMEOUT = 20_000;
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

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        createServices(initResult);
    }

    /** Create objects used by mock search activities. */
    private void createServices(OperationResult result) throws CommonException {
        repoObjectCreatorFor(ServiceType.class)
                .withObjectCount(10)
                .withNamePattern("test-good-%03d")
                .execute(result);
        repoObjectCreatorFor(ServiceType.class)
                .withObjectCount(10)
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

        TASK_100_SIMPLE_SUSPEND_ON_EXECUTION_TIME.init(this, task, result);

        when("task is run until it's stopped");
        TASK_100_SIMPLE_SUSPEND_ON_EXECUTION_TIME.rerunErrorsOk(result);

        then("the task is suspended after exceeding execution time");
        TASK_100_SIMPLE_SUSPEND_ON_EXECUTION_TIME.assertAfter()
                .assertSuspended()
                .assertFatalError();
        // TODO more asserts
    }

    /**
     * A child activity that is suspended when it exceeds allowed execution time (specified on its own level).
     */
    @Test
    public void test110ChildSuspendOnOwnExecutionTime() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TASK_110_CHILD_SUSPEND_ON_OWN_EXECUTION_TIME.init(this, task, result);

        when("task is run until it's stopped");
        TASK_110_CHILD_SUSPEND_ON_OWN_EXECUTION_TIME.rerunErrorsOk(result);

        then("the task is suspended after exceeding execution time");
        TASK_110_CHILD_SUSPEND_ON_OWN_EXECUTION_TIME.assertAfter()
                .assertSuspended()
                .assertFatalError();
        // TODO more asserts
    }

    /**
     * A child activity that is suspended when it exceeds allowed execution time (specified on the parent level).
     */
    @Test(enabled = false) // FIXME we don't see parent policies
    public void test120ChildSuspendOnParentExecutionTime() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TASK_120_CHILD_SUSPEND_ON_PARENT_EXECUTION_TIME.init(this, task, result);

        when("task is run until it's stopped");
        TASK_120_CHILD_SUSPEND_ON_PARENT_EXECUTION_TIME.rerunErrorsOk(result);

        then("the task is suspended after exceeding execution time");
        TASK_120_CHILD_SUSPEND_ON_PARENT_EXECUTION_TIME.assertAfter()
                .assertSuspended()
                .assertFatalError();
        // TODO more asserts
    }

    /**
     * As {@link #test110ChildSuspendOnOwnExecutionTime()} but the activities run in subtasks.
     */
    @Test
    public void test130ChildSuspendOnOwnExecutionTimeWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TASK_130_CHILD_SUSPEND_ON_OWN_EXECUTION_TIME_WITH_SUBTASKS.init(this, task, result);

        when("task is run until it's stopped");
        TASK_130_CHILD_SUSPEND_ON_OWN_EXECUTION_TIME_WITH_SUBTASKS.rerunErrorsOk(
                checkerBuilder -> checkerBuilder.checkOnlySchedulingState(true),
                result);

        // @formatter:off
        then("the task tree is suspended after exceeding execution time");
        TASK_130_CHILD_SUSPEND_ON_OWN_EXECUTION_TIME_WITH_SUBTASKS.assertTree("after")
                .display()
                .assertSuspended()
                .assertSubtasks(2)
                .subtask("first", false)
                    .display()
                    .assertSuccess()
                    .assertClosed() // this activity finished before suspension
                    .end()
                .subtask("second", false)
                    .assertFatalError()
                    .assertSuspended() // but this was suspended
                    .display()
                    .end();
        // @formatter:on
        // TODO more asserts
    }

    /**
     * As {@link #test120ChildSuspendOnParentExecutionTime()} but the activities run in subtasks.
     */
    @Test(enabled = false) // FIXME we don't see parent policies
    public void test140ChildSuspendOnParentExecutionTimeWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TASK_140_CHILD_SUSPEND_ON_PARENT_EXECUTION_TIME_WITH_SUBTASKS.init(this, task, result);

        when("task is run until it's stopped");
        TASK_140_CHILD_SUSPEND_ON_PARENT_EXECUTION_TIME_WITH_SUBTASKS.rerunErrorsOk(
                checkerBuilder -> checkerBuilder.checkOnlySchedulingState(true),
                result);

        then("the task tree is suspended after exceeding execution time");
        TASK_140_CHILD_SUSPEND_ON_PARENT_EXECUTION_TIME_WITH_SUBTASKS.assertTree("after")
                .display()
                .assertSuspended()
                .assertSubtasks(2)
                .subtask("first", false)
                .display()
                .assertClosed() // this activity finished before suspension
                .end()
                .subtask("second", false)
                .display()
                .assertSuspended() // but this was suspended
                .end();
        // TODO more asserts
    }

    /**
     * A multi-node activity that is suspended when it exceeds allowed execution time.
     */
    @Test
    public void test150MultinodeSuspendOnExecutionTime() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TASK_150_MULTINODE_SUSPEND_ON_EXECUTION_TIME.init(this, task, result);

        when("task is run until it's stopped (some workers may continue for a little while)");
        TASK_150_MULTINODE_SUSPEND_ON_EXECUTION_TIME.rerunErrorsOk(
                checkerBuilder -> checkerBuilder.checkOnlySchedulingState(true),
                result);

        and("the task and ALL workers are suspended");
        waitForTaskTreeCloseOrCondition(
                TASK_150_MULTINODE_SUSPEND_ON_EXECUTION_TIME.oid,
                result,
                DEFAULT_TIMEOUT,
                DEFAULT_SLEEP_TIME,
                tasksSuspendedPredicate(3)); // main task + 2 workers

        then("everything is OK");
        TASK_150_MULTINODE_SUSPEND_ON_EXECUTION_TIME.assertTree("after")
                .display()
                .assertSuspended() // already checked above
                .assertSubtasks(2)
                .subtask(0).display().end()
                .subtask(1).display().end();
        // TODO more asserts
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
        testTask.rerunErrorsOk(
                checkerBuilder -> checkerBuilder.checkOnlySchedulingState(true),
                result);

        then("root task is suspended and the first activity task is closed");
        testTask.assertTree("")
                .assertSuspended()
                .subtask("first", false)
                .assertClosed()
                .assertSuccess();

        then("the second activity task and ALL its workers are suspended after exceeding execution time");
        var secondActivityTaskOid = testTask.assertTree("")
                .subtask("second", false)
                .getOid();

        waitForTaskTreeCloseOrCondition(
                secondActivityTaskOid,
                result,
                DEFAULT_TIMEOUT,
                DEFAULT_SLEEP_TIME,
                tasksSuspendedPredicate(3)); // coordinator + 2 workers

        assertTaskTree(secondActivityTaskOid, "second activity task after")
                .display()
                .assertSuspended()
                .assertSubtasks(2)
                .subtask(0).display().end()
                .subtask(1).display().end();
        // TODO more asserts
    }

    /**
     * A child multi-node activity that is suspended when it exceeds allowed execution time (specified on the root level).
     * Children have its own subtasks.
     *
     * As {@link #test160MultinodeSuspendOnOwnExecutionTimeWithSubtasks()} but the constraint is on the root level.
     */
    @Test(enabled = false) // FIXME we don't see ancestors' policies
    public void test170MultinodeSuspendOnRootExecutionTimeWithSubtasks() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        TestTask testTask = TASK_170_MULTINODE_CHILD_SUSPEND_ON_ROOT_EXECUTION_TIME_WITH_SUBTASKS; // too long to be typed
        testTask.init(this, task, result);

        when("task is run until it's stopped (some workers may continue for a little while)");
        testTask.rerunErrorsOk(
                checkerBuilder -> checkerBuilder.checkOnlySchedulingState(true),
                result);

        then("root task is suspended and the first activity task is closed");
        testTask.assertTree("")
                .assertSuspended()
                .subtask("first", false)
                .assertClosed()
                .assertSuccess();

        then("the second activity task and ALL its workers are suspended after exceeding execution time");
        var secondActivityTaskOid = testTask.assertTree("")
                .subtask("second", false)
                .getOid();

        waitForTaskTreeCloseOrCondition(
                secondActivityTaskOid,
                result,
                DEFAULT_TIMEOUT,
                DEFAULT_SLEEP_TIME,
                tasksSuspendedPredicate(3)); // coordinator + 2 workers

        assertTaskTree(secondActivityTaskOid, "second activity task after")
                .display()
                .assertSuspended()
                .assertSubtasks(2)
                .subtask(0).display().end()
                .subtask(1).display().end();
        // TODO more asserts
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

        then("the task is suspended after exceeding given number of errors");
        testTask.assertAfter()
                .assertSuspended()
                .assertFatalError();
        // TODO more asserts
    }
}
