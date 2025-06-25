/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.repo.common.util.OperationExecutionWriter;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskRunHistoryType;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
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

    private static final TestObject<TaskType> TASK_NOOP_RECURRENT =
            TestObject.file(TEST_DIR, "task-noop-recurrent.xml", "b1701de1-6e3f-441a-accf-c853b1e41fe0");

    /**
     * MID-10496 task not starting after suspend/resume based on cron schedule. Only start once (executed immediately).
     *
     * TODO MID-10687 !!!!!
     *  Causes performance problems on master. Oracle job fails with OOM, sqlserver job timeouts.
     *  TO BE INVESTIGATED as part of MID-10687
     */
    @Test(enabled = false)
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

        long lastRun = currentTask.getLastRunStartTimestamp();

        logger.info("Suspending task");
        // suspend the task
        taskManager.suspendTaskTree(task.getOid(), 2000L, result);

        Thread.sleep(1000L);

        currentTask = taskManager.getTask(task.getOid(), GetOperationOptionsBuilder.create().build(), result);
        TaskAsserter.forTask(currentTask.getUpdatedTaskObject())
                .assertSuspended();

        logger.info("Resuming task");
        // resume task
        taskManager.resumeTaskTree(currentTask.getOid(), result);

        currentTask = taskManager.getTask(task.getOid(), GetOperationOptionsBuilder.create().build(), result);
        TaskAsserter.forTask(currentTask.getUpdatedTaskObject())
                .assertExecutionState(TaskExecutionStateType.RUNNING)
                .assertInProgress();

        logger.info("Waiting for task to finish");
        // task should resume immediately
        waitForTaskFinish(currentTask, 20000, 500L);

        // task should be finished with success, 9 items processed
        currentTask = taskManager.getTask(task.getOid(), GetOperationOptionsBuilder.create().build(), result);
        Assertions.assertThat(currentTask).isNotNull();
        TaskAsserter.forTask(currentTask.getUpdatedTaskObject())
                .assertSuccess()
                .rootActivityState()
                .assertComplete();

        Assertions.assertThat(currentTask.getLastRunStartTimestamp()).isGreaterThan(lastRun);

        lastRun = currentTask.getLastRunStartTimestamp();

        // once more wait - whether next scheduled run is executed (about every 10 seconds)
        Thread.sleep(15000L);

        currentTask = taskManager.getTask(task.getOid(), GetOperationOptionsBuilder.create().build(), result);
        Assertions.assertThat(currentTask).isNotNull();

        Assertions.assertThat(currentTask.getLastRunStartTimestamp()).isGreaterThan(lastRun);
    }

    @Test
    public void test100TaskRunHistoryCleanup() throws Exception{
        OperationResult result = createOperationResult();
        PrismObject<TaskType> task = TASK_NOOP_RECURRENT.get();

        logger.info("Adding task");
        taskManager.addTask(task, result);

        logger.info("Waiting for task to start");
        waitForTaskStart(task.getOid(), result, 5000L, 500);

        // let's wait for the task to run for 4-5 times
        Thread.sleep(5000L);

        taskManager.suspendTaskTree(task.getOid(), 1000L, result);

        task = getTask(TASK_NOOP_RECURRENT.oid);
        TaskType taskType = task.asObjectable();

        Assertions.assertThat(taskType)
                .isNotNull();

        List<TaskRunHistoryType> history = taskType.getTaskRunHistory();

        Assertions.assertThat(history)
                .hasSize(OperationExecutionWriter.DEFAUL_NUMBER_OF_RESULTS_TO_KEEP_PER_TASK);

        for (TaskRunHistoryType item : history) {
            Assertions.assertThat(item.getTaskRunIdentifier()).isNotEmpty();
            Assertions.assertThat(item.getRunStartTimestamp()).isNotNull();
            Assertions.assertThat(item.getRunEndTimestamp()).isNotNull();
        }
    }
}
