/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.List;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.quartzimpl.work.WorkStateManager;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskSchedulingStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Tests task handlers for workers creation and for task partitioning.
 */
@ContextConfiguration(locations = { "classpath:ctx-task-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPartitioning extends AbstractTaskManagerTest {

    public static final long DEFAULT_SLEEP_INTERVAL = 250L;
    public static final long DEFAULT_TIMEOUT = 30000L;

    private static final File TEST_DIR = new File("src/test/resources/partitioning");
    private static final TestResource<TaskType> TASK_100_MASTER = new TestResource<>(TEST_DIR, "task-100-master.xml", "bcbbdc23-b205-4b90-8df9-4674432b863c");

    @Autowired private WorkStateManager workStateManager;

    @PostConstruct
    public void initialize() throws Exception {
        super.initialize();
        workStateManager.setFreeBucketWaitIntervalOverride(1000L);
        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
        singleHandler1.setDelay(1000);
    }

    @Test
    public void test000Integrity() {
        AssertJUnit.assertNotNull(repositoryService);
        AssertJUnit.assertNotNull(taskManager);
    }

    @Test
    public void test100DurableRecurring() throws Exception {
        given();

        OperationResult result = createOperationResult();

        when();

        add(TASK_100_MASTER, result);

        try {
            waitForTaskRunFinish(TASK_100_MASTER.oid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL, 0);
            TaskQuartzImpl masterTask = taskManager.getTaskPlain(TASK_100_MASTER.oid, result);
            long lastRunFinish = masterTask.getLastRunFinishTimestamp();

            // The task should be in this state for at least 3000 ms. Hopefully we catch it.
            waitForTaskWaiting(TASK_100_MASTER.oid, result, 10000, 100);

            then();

            masterTask = taskManager.getTaskPlain(TASK_100_MASTER.oid, result);
            List<? extends Task> partitions = masterTask.listSubtasks(result);

            displayDumpable("master task", masterTask);
            displayValue("partition tasks", partitions);

            assertEquals("Wrong # of partitions", 3, partitions.size());

            assertEquals("Wrong scheduling state", TaskSchedulingStateType.WAITING, masterTask.getSchedulingState());
            assertEquals("Wrong execution state", TaskExecutionStateType.RUNNING, masterTask.getExecutionState());

            when("wait for master finish");

            // Task is ready after partitions finish (because it is recurring task)
            waitForTaskReady(TASK_100_MASTER.oid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

            then("wait for master finish");

            assertEquals("Wrong # of handler executions", 3, singleHandler1.getExecutions());

            when("second run");

            taskManager.scheduleTaskNow(TASK_100_MASTER.oid, result);

            then("second run");

            waitForTaskRunFinish(TASK_100_MASTER.oid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL, lastRunFinish);
            waitForTaskReady(TASK_100_MASTER.oid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

            masterTask = taskManager.getTaskPlain(TASK_100_MASTER.oid, result);
            partitions = masterTask.listSubtasks(result);
            displayDumpable("master task (after 2nd run)", masterTask);
            displayValue("partition tasks (after 2nd run)", partitions);

            assertEquals("Wrong # of handler executions", 6, singleHandler1.getExecutions());
        } finally {
            suspendAndDeleteTasks(TASK_100_MASTER.oid);
        }
    }
}
