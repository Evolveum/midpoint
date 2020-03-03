/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.quartzimpl.work.WorkStateManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests task handlers for workers creation and for task partitioning.
 *
 * @author mederly
 */

@ContextConfiguration(locations = { "classpath:ctx-task-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestWorkersManagement extends AbstractTaskManagerTest {

    private static final long DEFAULT_SLEEP_INTERVAL = 250L;
    private static final long DEFAULT_TIMEOUT = 30000L;

    @Autowired private WorkStateManager workStateManager;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;

    private static String taskFilename(String testName, String subId) {
        return "src/test/resources/workers/task-" + testNumber(testName) + "-" + subId + ".xml";
    }

    @SuppressWarnings("unused")
    private static String taskFilename(String testName) {
        return taskFilename(testName, "0");
    }

    private static String taskOid(String testName, String subId) {
        return "44444444-2222-2222-2223-" + testNumber(testName) + subId + "00000000";
    }

    @SuppressWarnings("unused")
    private static String taskOid(String test) {
        return taskOid(test, "0");
    }

    private static String testNumber(String test) {
        return test.substring(4, 7);
    }

    @SuppressWarnings("unused")
    @NotNull
    protected String workerTaskFilename(String TEST_NAME) {
        return taskFilename(TEST_NAME, "w");
    }

    @NotNull
    private String coordinatorTaskFilename(String TEST_NAME) {
        return taskFilename(TEST_NAME, "c");
    }

    @SuppressWarnings("unused")
    @NotNull
    protected String workerTaskOid(String TEST_NAME) {
        return taskOid(TEST_NAME, "w");
    }

    @NotNull
    private String coordinatorTaskOid(String TEST_NAME) {
        return taskOid(TEST_NAME, "c");
    }

    @PostConstruct
    public void initialize() throws Exception {
        super.initialize();
        workStateManager.setFreeBucketWaitIntervalOverride(1000L);
        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

        cacheConfigurationManager.applyCachingConfiguration(
                (SystemConfigurationType) prismContext.parseObject(SYSTEM_CONFIGURATION_FILE).asObjectable());
    }

    @Test
    public void test000Integrity() {
        AssertJUnit.assertNotNull(repositoryService);
        AssertJUnit.assertNotNull(taskManager);
    }

    @Test
    public void test100CreateWorkersSingle() throws Exception {
        final String TEST_NAME = "test100CreateWorkersSingle";
        OperationResult result = createResult(TEST_NAME);

        workBucketsTaskHandler.resetBeforeTest();
        workBucketsTaskHandler.setDelayProcessor(DEFAULT_SLEEP_INTERVAL);

        // WHEN
        addObjectFromFile(coordinatorTaskFilename(TEST_NAME));

        // THEN
        String coordinatorTaskOid = coordinatorTaskOid(TEST_NAME);
        try {
            waitForTaskProgress(coordinatorTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL, 1);

            TaskQuartzImpl coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
            List<Task> workers = coordinatorTask.listSubtasks(result);
            assertEquals("Wrong # of workers", 1, workers.size());

            display("coordinator task", coordinatorTask);
            display("worker task", workers.get(0));
            displayBucketOpStatistics("coordinator", coordinatorTask);
            displayBucketOpStatistics("worker", workers.get(0));
            assertCachingProfiles(coordinatorTask, "profile1");
            assertCachingProfiles(workers.get(0), "profile1");

            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

            Thread.sleep(1000);         // if workers would be started again, we would get some more processing here
            assertEquals("Wrong # of items processed", 4, workBucketsTaskHandler.getItemsProcessed());

            // TODO some asserts here

            // WHEN
            taskManager.scheduleTasksNow(singleton(coordinatorTaskOid), result);

            // THEN
            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

            coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
            workers = coordinatorTask.listSubtasks(result);
            assertEquals("Wrong # of workers", 1, workers.size());

            display("coordinator task after re-run", coordinatorTask);
            display("worker task after re-run", workers.get(0));

            Thread.sleep(1000);         // if workers would be started again, we would get some more processing here
            assertEquals("Wrong # of items processed", 8, workBucketsTaskHandler.getItemsProcessed());
        } finally {
            suspendAndDeleteTasks(coordinatorTaskOid);
        }
    }

    private void displayBucketOpStatistics(String label, Task task) throws SchemaException {
        OperationStatsType stats = task.getStoredOperationStats();
        WorkBucketManagementPerformanceInformationType bucketStats = stats != null ? stats.getWorkBucketManagementPerformanceInformation() : null;
        String text = bucketStats != null ? prismContext.yamlSerializer().root(new QName("stats")).serializeRealValue(bucketStats) : "(null)";
        display("Bucket op stats for " + label, text);
    }

    @Test
    public void test110CreateWorkersRecurring() throws Exception {
        final String TEST_NAME = "test110CreateWorkersRecurring";
        OperationResult result = createResult(TEST_NAME);

        workBucketsTaskHandler.resetBeforeTest();
        workBucketsTaskHandler.setDelayProcessor(DEFAULT_SLEEP_INTERVAL);

        // (1) ------------------------------------------------------------------------------------ WHEN (import task)
        when("1: import task");
        addObjectFromFile(coordinatorTaskFilename(TEST_NAME));
        String coordinatorTaskOid = coordinatorTaskOid(TEST_NAME);

        try {
            // THEN (worker is created and executed)
            then("1: import task");
            waitForTaskProgress(coordinatorTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL, 1);

            TaskQuartzImpl coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
            List<Task> workers = coordinatorTask.listSubtasks(result);
            assertEquals("Wrong # of workers", 1, workers.size());

            display("coordinator task", coordinatorTask);
            display("worker task", workers.get(0));

            waitForTaskClose(workers.get(0).getOid(), result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

            assertEquals("Wrong # of items processed", 4, workBucketsTaskHandler.getItemsProcessed());
            // TODO some asserts here

            // coordinator should run automatically in cca 15 seconds

            // (2) ------------------------------------------------------------------------------------ WHEN (wait for coordinator next run)
            when("2: wait for coordinator next run");
            // TODO adapt this when the coordinator progress will be reported in other ways
            waitForTaskProgress(coordinatorTaskOid, result, 30000, DEFAULT_SLEEP_INTERVAL, 2);

            // THEN (worker is still present and executed)
            then("2: wait for coordinator next run");
            coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
            workers = coordinatorTask.listSubtasks(result);
            assertEquals("Wrong # of workers", 1, workers.size());

            display("coordinator task after re-run", coordinatorTask);
            display("worker task after re-run", workers.get(0));

            waitForTaskClose(workers.get(0).getOid(), result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

            assertEquals("Wrong # of items processed", 8, workBucketsTaskHandler.getItemsProcessed());

            // (3) ------------------------------------------------------------------------------------  WHEN (suspend the tree while work is done)
            when("3: suspend the tree while work is done");
            boolean stopped = taskManager.suspendTaskTree(coordinatorTaskOid, DEFAULT_TIMEOUT, result);

            // THEN (tasks are suspended)
            then("3: suspend the tree while work is done");
            coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
            workers = coordinatorTask.listSubtasks(result);
            assertEquals("Wrong # of workers", 1, workers.size());
            Task worker = workers.get(0);

            assertTrue("tasks were not stopped", stopped);

            display("coordinator task after suspend-when-waiting", coordinatorTask);
            display("worker task after suspend-when-waiting", worker);

            assertEquals("Wrong execution status of coordinator", TaskExecutionStatus.SUSPENDED,
                    coordinatorTask.getExecutionStatus());
            // in very slow environments the coordinator could be started in the meanwhile, so here the state could be WAITING
            //assertEquals("Wrong state-before-suspend of coordinator", TaskExecutionStatusType.RUNNABLE,
            //        coordinatorTask.getStateBeforeSuspend());
            assertEquals("Wrong execution status of worker", TaskExecutionStatus.CLOSED, worker.getExecutionStatus());
            //noinspection SimplifiedTestNGAssertion
            assertEquals("Wrong state-before-suspend of worker", null, worker.getStateBeforeSuspend());

            // (4) ------------------------------------------------------------------------------------  WHEN (resume the tree)
            when("4: resume the tree");
            taskManager.resumeTaskTree(coordinatorTaskOid, result);

            // THEN (tasks are resumed)
            then("4: resume the tree");
            coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
            workers = coordinatorTask.listSubtasks(result);
            assertEquals("Wrong # of workers", 1, workers.size());
            worker = workers.get(0);

            display("coordinator task after resume-from-suspend-when-waiting", coordinatorTask);
            display("worker task after resume-from-suspend-when-waiting", worker);

            assertEquals("Wrong execution status of coordinator", TaskExecutionStatus.RUNNABLE,
                    coordinatorTask.getExecutionStatus());
            //noinspection SimplifiedTestNGAssertion
            assertEquals("Wrong state-before-suspend of coordinator", null, coordinatorTask.getStateBeforeSuspend());
            //noinspection SimplifiedTestNGAssertion
            assertEquals("Wrong state-before-suspend of worker", null, worker.getStateBeforeSuspend());

            // (5) ------------------------------------------------------------------------------------  WHEN (suspend the tree while worker is executing)
            when("5: suspend the tree while worker is executing");
            waitForTaskProgress(coordinatorTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL, 3);
            stopped = taskManager.suspendTaskTree(coordinatorTaskOid, DEFAULT_TIMEOUT, result);

            // THEN (tasks are suspended)
            then("5: suspend the tree while worker is executing");
            coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
            workers = coordinatorTask.listSubtasks(result);
            assertEquals("Wrong # of workers", 1, workers.size());
            worker = workers.get(0);

            display("coordinator task after suspend-when-running", coordinatorTask);
            display("worker task after suspend-when-running", worker);

            assertEquals("Wrong execution status of coordinator", TaskExecutionStatus.SUSPENDED,
                    coordinatorTask.getExecutionStatus());
            // in theory, the execution could be 'after' at this time; so this assertion might fail
            //assertEquals("Wrong state-before-suspend of coordinator", TaskExecutionStatusType.WAITING,
            //        coordinatorTask.getStateBeforeSuspend());
            assertEquals("Wrong execution status of worker", TaskExecutionStatus.SUSPENDED, worker.getExecutionStatus());
            assertEquals("Wrong state-before-suspend of worker", TaskExecutionStatusType.RUNNABLE,
                    worker.getStateBeforeSuspend());

            assertTrue("tasks were not stopped", stopped);

            // (6) ------------------------------------------------------------------------------------  WHEN (resume after 2nd suspend)
            when("6: resume after 2nd suspend");
            taskManager.resumeTaskTree(coordinatorTaskOid, result);

            // THEN (tasks are suspended)
            then("6: resume after 2nd suspend");
            coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
            workers = coordinatorTask.listSubtasks(result);
            assertEquals("Wrong # of workers", 1, workers.size());
            worker = workers.get(0);

            display("coordinator task after resume-after-2nd-suspend", coordinatorTask);
            display("worker task after resume-after-2nd-suspend", worker);

            displayBucketOpStatistics("coordinator", coordinatorTask);
            displayBucketOpStatistics("worker", worker);

            waitForTaskClose(worker.getOid(), result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

            // brittle - might fail
            assertEquals("Wrong # of items processed", 12, workBucketsTaskHandler.getItemsProcessed());

            // cleanup
        } finally {
            suspendAndDeleteTasks(coordinatorTaskOid);
        }
    }

    @Test
    public void test200SimplePartitioning() throws Exception {
        final String TEST_NAME = "test200SimplePartitioning";
        OperationResult result = createResult(TEST_NAME);

        partitionedWorkBucketsTaskHandler.resetBeforeTest();
        partitionedWorkBucketsTaskHandler.setEnsureSingleRunner(true);
        partitionedWorkBucketsTaskHandler.setDelayProcessor(1000L);

        // WHEN
        addObjectFromFile(taskFilename(TEST_NAME, "r"));

        // THEN
        String masterTaskOid = taskOid(TEST_NAME, "r");
        try {
            waitForTaskProgress(masterTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL, 1);

            TaskQuartzImpl masterTask = taskManager.getTask(masterTaskOid, result);
            List<Task> subtasks = masterTask.listSubtasks(result);

            display("master task", masterTask);
            display("subtasks", subtasks);

            assertEquals("Wrong task kind", TaskKindType.PARTITIONED_MASTER, masterTask.getWorkManagement().getTaskKind());
            assertEquals("Wrong # of partitions", 3, subtasks.size());

            assertCachingProfiles(masterTask, "profile1");
            assertCachingProfiles(subtasks.get(0), "profile1");
            assertCachingProfiles(subtasks.get(1), "profile1");
            assertCachingProfiles(subtasks.get(2), "profile1");

            waitForTaskCloseCheckingSubtasks(masterTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

            masterTask = taskManager.getTask(masterTaskOid, result);
            subtasks = masterTask.listSubtasksDeeply(result);
            displayBucketOpStatistics("master", masterTask);
            for (Task subtask : subtasks) {
                displayBucketOpStatistics(subtask.toString(), subtask);
            }

            //noinspection SimplifiedTestNGAssertion
            assertEquals("Unexpected failure", null, partitionedWorkBucketsTaskHandler.getFailure());

            // TODO some asserts here
            // TODO test suspend, resume here
        } finally {
            suspendAndDeleteTasks(masterTaskOid);
        }
    }

    @Test
    public void test210PartitioningToWorkersSingleBucket() throws Exception {
        final String TEST_NAME = "test210PartitioningToWorkersSingleBucket";
        OperationResult result = createResult(TEST_NAME);

        partitionedWorkBucketsTaskHandler.resetBeforeTest();
        partitionedWorkBucketsTaskHandler.setEnsureSingleRunner(true);
        partitionedWorkBucketsTaskHandler.setDelayProcessor(1000L);

        // WHEN
        addObjectFromFile(taskFilename(TEST_NAME, "r"));

        // THEN
        String masterTaskOid = taskOid(TEST_NAME, "r");
        try {
            waitForTaskProgress(masterTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL, 1);

            TaskQuartzImpl masterTask = taskManager.getTask(masterTaskOid, result);
            List<Task> subtasks = masterTask.listSubtasks(result);

            display("master task", masterTask);
            display("subtasks", subtasks);

            assertEquals("Wrong task kind", TaskKindType.PARTITIONED_MASTER, masterTask.getWorkManagement().getTaskKind());
            assertEquals("Wrong # of partitions", 3, subtasks.size());

            Task first = subtasks.stream().filter(t -> t.getName().getOrig().contains("(1)")).findFirst().orElse(null);
            Task second = subtasks.stream().filter(t -> t.getName().getOrig().contains("(2)")).findFirst().orElse(null);
            Task third = subtasks.stream().filter(t -> t.getName().getOrig().contains("(3)")).findFirst().orElse(null);
            assertNotNull("Second-phase task was not created", second);
            assertNotNull("Third-phase task was not created", third);

            waitForTaskCloseCheckingSubtasks(second.getOid(), result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);
            second = taskManager.getTask(second.getOid(), result);
            display("Second task after completion", second);
            List<Task> secondSubtasks = second.listSubtasks(result);
            display("Subtasks of second task after completion", secondSubtasks);
            assertEquals("Wrong # of second task's subtasks", 3, secondSubtasks.size());

            assertCachingProfiles(masterTask, "profile1");
            assertCachingProfiles(first, "profile1");
            assertCachingProfiles(second, "profile2");
            assertCachingProfiles(third, "profile1");
            secondSubtasks.forEach(t -> assertCachingProfiles(t, "profile2"));

            waitForTaskCloseCheckingSubtasks(third.getOid(), result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);
            third = taskManager.getTask(third.getOid(), result);
            display("Third task after completion", third);
            List<Task> thirdSubtasks = third.listSubtasks(result);
            display("Subtasks of third task after completion", thirdSubtasks);
            assertEquals("Wrong # of third task's subtasks", 2, thirdSubtasks.size());

            waitForTaskCloseCheckingSubtasks(masterTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

            //noinspection SimplifiedTestNGAssertion
            assertEquals("Unexpected failure", null, partitionedWorkBucketsTaskHandler.getFailure());

            // TODO some asserts here

            // TODO test suspend, resume here
        } finally {
            suspendAndDeleteTasks(masterTaskOid);
        }
    }

    @Test
    public void test220PartitioningToWorkersMoreBuckets() throws Exception {
        final String TEST_NAME = "test220PartitioningToWorkersMoreBuckets";
        OperationResult result = createResult(TEST_NAME);

        partitionedWorkBucketsTaskHandler.resetBeforeTest();
        partitionedWorkBucketsTaskHandler.setDelayProcessor(50L);

        // WHEN
        addObjectFromFile(taskFilename(TEST_NAME, "r"));

        // THEN
        String masterTaskOid = taskOid(TEST_NAME, "r");
        try {
            waitForTaskProgress(masterTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL, 1);

            TaskQuartzImpl masterTask = taskManager.getTask(masterTaskOid, result);
            List<Task> subtasks = masterTask.listSubtasks(result);

            display("master task", masterTask);
            display("subtasks", subtasks);

            assertEquals("Wrong task kind", TaskKindType.PARTITIONED_MASTER, masterTask.getWorkManagement().getTaskKind());
            assertEquals("Wrong # of partitions", 3, subtasks.size());

            Task first = subtasks.stream().filter(t -> t.getName().getOrig().contains("(1)")).findFirst().orElse(null);
            Task second = subtasks.stream().filter(t -> t.getName().getOrig().contains("(2)")).findFirst().orElse(null);
            Task third = subtasks.stream().filter(t -> t.getName().getOrig().contains("(3)")).findFirst().orElse(null);
            assertNotNull("Second-phase task was not created", second);
            assertNotNull("Third-phase task was not created", third);

            waitForTaskCloseCheckingSubtasks(second.getOid(), result, 30000L, DEFAULT_SLEEP_INTERVAL);
            second = taskManager.getTask(second.getOid(), result);
            display("Second task after completion", second);
            List<Task> secondSubtasks = second.listSubtasks(result);
            display("Subtasks of second task after completion", secondSubtasks);
            assertEquals("Wrong # of second task's subtasks", 3, secondSubtasks.size());

            waitForTaskCloseCheckingSubtasks(third.getOid(), result, 20000L, DEFAULT_SLEEP_INTERVAL);
            third = taskManager.getTask(third.getOid(), result);
            display("Third task after completion", third);
            List<Task> thirdSubtasks = third.listSubtasks(result);
            display("Subtasks of third task after completion", thirdSubtasks);
            assertEquals("Wrong # of third task's subtasks", 2, thirdSubtasks.size());

            assertCachingProfiles(masterTask, "profile1");
            assertCachingProfiles(first, "profile2");
            assertCachingProfiles(second, "profile2");
            assertCachingProfiles(third, "profile3");
            secondSubtasks.forEach(t -> assertCachingProfiles(t, "profile2"));
            thirdSubtasks.forEach(t -> assertCachingProfiles(t, "profile3"));

            waitForTaskCloseCheckingSubtasks(masterTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

            //noinspection SimplifiedTestNGAssertion
            assertEquals("Unexpected failure", null, partitionedWorkBucketsTaskHandler.getFailure());

            assertEquals("Wrong # of items processed", 41, partitionedWorkBucketsTaskHandler.getItemsProcessed());

            int totalItems2 = getTotalItemsProcessed(second.getOid());
            assertEquals("Wrong # of items processed in 2nd stage", 32, totalItems2);

            int totalItems3 = getTotalItemsProcessed(third.getOid());
            assertEquals("Wrong # of items processed in 3rd stage", 8, totalItems3);

            // TODO some asserts here

            // TODO test suspend, resume here
        } finally {
            suspendAndDeleteTasks(masterTaskOid);
        }
    }
}
