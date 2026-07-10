/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.tasks;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.io.File;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.TaskAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestTasks extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/misc");

    private static final TestObject<TaskType> TASK_10496 =
            TestObject.file(TEST_DIR, "task-10496.xml", "6ea1fcdf-f388-43ff-8c31-43ee2f3909fb");

    /**
     * These tests add tasks with fixed OIDs against a shared repository. We remove them (together
     * with their worker subtasks) after every method - including on failure - so repeated runs
     * start from a clean state. Otherwise, a worker that is slow to die leaks into the next run and
     * collides with a freshly created one, producing two worker instances for a single bucket and
     * a stuck coordinator (see MID-10496).
     */
    @AfterMethod
    public void deleteTasks() {
        try {
            suspendAndDeleteTasks(TASK_10496.oid, TASK_NOOP_RECURRENT.oid);
        } catch (Exception e) {
            logger.warn("Task cleanup failed", e);
        }
    }

    /**
     * MID-10496 task not starting after suspend/resume based on cron schedule. Only start once (executed immediately).
     */
    @Test
    public void testSuspendResumeCron() throws Exception {
        OperationResult result = createOperationResult();
        PrismObject<TaskType> task = TASK_10496.get();

        logger.info("Adding task");
        taskManager.addTask(task, result);

        logger.info("Waiting for task to start");
        // task should start in <= 10 seconds, and should run for ~9 seconds
        waitForTaskStart(task.getOid(), result, 12000L, 500);

        Thread.sleep(1000L);

        Task currentTask = taskManager.getTask(task.getOid(), GetOperationOptionsBuilder.create().build(), result);
        TaskAsserter.forTask(currentTask.getUpdatedTaskObject())
                .assertExecutionState(TaskExecutionStateType.RUNNING)
                .assertInProgress();

        long startBeforeSuspend = currentTask.getLastRunStartTimestamp();

        logger.info("Suspending task");
        // suspend the task
        taskManager.suspendTaskTree(task.getOid(), 2000L, result);

        Thread.sleep(1000L);

        currentTask = taskManager.getTask(task.getOid(), GetOperationOptionsBuilder.create().build(), result);
        TaskAsserter.forTask(currentTask.getUpdatedTaskObject())
                .assertSuspended();

        // Baseline for detecting runs that complete after resume. We assert on run *finish*
        // timestamps rather than momentary execution state: this task is recurring (cron every
        // ~10s) and loosely bound, so once resumed it keeps running back-to-back and is almost
        // never observably idle. Waiting for a single "not in progress" sample (waitForTaskFinish)
        // is therefore inherently racy against the restored schedule - see MID-10496 / MID-10687.
        long finishBeforeResume = or0(currentTask.getLastRunFinishTimestamp());

        logger.info("Resuming task");
        // resume task
        taskManager.resumeTaskTree(currentTask.getOid(), result);

        logger.info("Waiting for a run to complete after resume");
        // the resumed run should finish within ~10s (8 steps * 1s delay + scheduling overhead)
        waitForTaskRunFinish(task.getOid(), result, 25000L, 500L, finishBeforeResume);

        currentTask = taskManager.getTask(task.getOid(), GetOperationOptionsBuilder.create().build(), result);
        Assertions.assertThat(currentTask).isNotNull();
        // a new run actually started after resume (not merely the pre-suspend one)
        Assertions.assertThat(currentTask.getLastRunStartTimestamp())
                .as("a new run started after resume")
                .isGreaterThan(startBeforeSuspend);

        long finishAfterResume = or0(currentTask.getLastRunFinishTimestamp());

        // MID-10496: the task must continue at its next scheduled time, not run only once.
        // Wait for a *further* run to complete (next scheduled run is about every 10 seconds).
        logger.info("Waiting for the next scheduled run to complete");
        waitForTaskRunFinish(task.getOid(), result, 20000L, 500L, finishAfterResume);
    }
}
