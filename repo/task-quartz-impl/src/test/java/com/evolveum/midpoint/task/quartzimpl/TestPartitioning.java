/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.quartzimpl.work.WorkStateManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.annotation.PostConstruct;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Tests task handlers for workers creation and for task partitioning.
 *
 * @author mederly
 */

@ContextConfiguration(locations = {"classpath:ctx-task-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPartitioning extends AbstractTaskManagerTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestPartitioning.class);
    public static final long DEFAULT_SLEEP_INTERVAL = 250L;
    public static final long DEFAULT_TIMEOUT = 30000L;

    @Autowired private WorkStateManager workStateManager;

    private static String taskFilename(String testName, String subId) {
        return "src/test/resources/partitioning/task-" + testNumber(testName) + "-" + subId + ".xml";
    }

    private static String taskFilename(String testName) {
        return taskFilename(testName, "0");
    }

    private static String taskOid(String testName, String subId) {
        return "44444444-2222-2222-8888-" + testNumber(testName) + subId + "00000000";
    }

    private static String taskOid(String test) {
        return taskOid(test, "0");
    }

    private static String testNumber(String test) {
        return test.substring(4, 7);
    }

    @NotNull
    protected String workerTaskFilename(String TEST_NAME) {
        return taskFilename(TEST_NAME, "w");
    }

    @NotNull
    protected String coordinatorTaskFilename(String TEST_NAME) {
        return taskFilename(TEST_NAME, "c");
    }

    @NotNull
    protected String workerTaskOid(String TEST_NAME) {
        return taskOid(TEST_NAME, "w");
    }

    @NotNull
    protected String coordinatorTaskOid(String TEST_NAME) {
        return taskOid(TEST_NAME, "c");
    }

    @PostConstruct
    public void initialize() throws Exception {
        super.initialize();
        workStateManager.setFreeBucketWaitIntervalOverride(1000L);
        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
    }

    @Test
    public void test000Integrity() {
        AssertJUnit.assertNotNull(repositoryService);
        AssertJUnit.assertNotNull(taskManager);
    }

    @Test
    public void test100DurableRecurring() throws Exception {
        final String TEST_NAME = "test100DurableRecurring";
        OperationResult result = createResult(TEST_NAME);

        // WHEN
        addObjectFromFile(taskFilename(TEST_NAME, "m"));

        // THEN
        String masterTaskOid = taskOid(TEST_NAME, "m");
        try {
            waitForTaskProgress(masterTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL, 1);

            TaskQuartzImpl masterTask = taskManager.getTask(masterTaskOid, result);
            List<Task> partitions = masterTask.listSubtasks(result);

            display("master task", masterTask);
            display("partition tasks", partitions);

            assertEquals("Wrong # of partitions", 3, partitions.size());

            waitForTaskRunnable(masterTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

            assertEquals("Wrong # of handler executions", 3, singleHandler1.getExecutions());

            // WHEN
            taskManager.scheduleTasksNow(singleton(masterTaskOid), result);

            // THEN
            waitForTaskProgress(masterTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL, 2);
            waitForTaskRunnable(masterTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

            masterTask = taskManager.getTask(masterTaskOid, result);
            partitions = masterTask.listSubtasks(result);
            display("master task (after 2nd run)", masterTask);
            display("partition tasks (after 2nd run)", partitions);

            assertEquals("Wrong # of handler executions", 6, singleHandler1.getExecutions());
        } finally {
            suspendAndDeleteTasks(masterTaskOid);
        }
    }
}
