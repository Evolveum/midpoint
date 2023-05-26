/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.tasks;

import java.io.File;
import java.util.List;
import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.repo.common.AbstractRepoCommonTest;
import com.evolveum.midpoint.repo.common.activity.run.buckets.BucketingConfigurationOverrides;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.tasks.handlers.iterative.IterativeMockActivityHandler;
import com.evolveum.midpoint.repo.common.tasks.handlers.search.SearchIterativeMockActivityHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Tests bucketing using live (running) tasks.
 *
 * These tests should not simply mirror {@link TestBucketingStatic}. Instead we should concentrate on checking whether
 * bucketing works in live tasks. Basic aspects are covered in {@link TestActivities}. Things covered there are:
 *
 * 1. detailed checking of bucketing state and statistics (test100),
 * 2. explicitly suspending a worker task during processing (test220), - NOT ENABLED YET
 * 3. deleting suspended worker task, assuming others will carry out the work (test220), - NOT ENABLED YET
 * 4. implicitly suspending a worker task (by throwing an exception) during processing (test230), - NOT ENABLED YET
 * 5. closing suspended worker task, assuming others will carry out the work (test230), - NOT ENABLED YET
 * 6. checking that queries are correctly narrowed within a task (test300). - NOT ENABLED YET
 *
 * Majority of this test class is commented out also because some required components (namely old bucketed mock task handler)
 * are no longer present. The bucketed mock task handler could be replaced by either {@link IterativeMockActivityHandler}
 * or {@link SearchIterativeMockActivityHandler} - it has to be determined yet.
 *
 * @see TestBucketingStatic
 */
@ContextConfiguration(locations = "classpath:ctx-repo-common-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestBucketingLive extends AbstractRepoCommonTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/bucketing-live");

    private static final TestResource<TaskType> TASK_100_SINGLE_WORKER_FOUR_BUCKETS = new TestResource<>(TEST_DIR, "task-100-single-worker-four-buckets.xml", "4e09a632-f2c7-4285-9204-c02e7c39ae04");

    private static final long DEFAULT_SLEEP_INTERVAL = 250L;
    private static final long DEFAULT_TIMEOUT = 30000L;

    @PostConstruct
    public void initialize() throws Exception {
        BucketingConfigurationOverrides.setFreeBucketWaitIntervalOverride(1000L);
        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
    }

    /**
     * Checks the execution of four numbered buckets in a single worker.
     */
    @Test
    public void test100SingleWorkerFourBuckets() throws Exception {
        given();
        OperationResult result = createOperationResult();

        mockRecorder.reset();

        List<RoleType> roles = repoObjectCreatorFor(RoleType.class)
                .withObjectCount(4)
                .withNamePattern("test-role-100-%d")
                .withCustomizer(this::setDiscriminator)
                .execute(result);

        when();
        Task root = taskAdd(TASK_100_SINGLE_WORKER_FOUR_BUCKETS, result);

        then();
        try {
            waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, DEFAULT_TIMEOUT);

            root.refresh(result);
            assertWorkerAfter("after 1st run", root, result);
            assertExecutions(roles, 1);

            when("second run");
            taskManager.scheduleTasksNow(List.of(root.getOid()), result);

            when("second run");
            waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, DEFAULT_TIMEOUT);

            root.refresh(result);
            assertWorkerAfter("after 2nd run", root, result);
            assertExecutions(roles, 2);
        } finally {
            suspendAndDeleteTasks(root.getOid());
        }
    }

    private void assertWorkerAfter(String message, Task root, OperationResult result) throws SchemaException {
        // @formatter:off
        assertTask(root, message)
                .display()
                .loadSubtasksDeeply(result)
                .progressInformation() // this is for the whole tree
                    .display()
                    .assertBuckets(4, 4)
                    .assertItems(4, null) // expected is null because we have workers
                .end()
                .rootActivityState()
                    .bucketManagementStatistics()
                        .assertEntries(0) // everything should be in the worker
                    .end()
                .end()
                .assertSubtasks(1)
                .subtask(0)
                    .display()
                    .rootActivityState()
                        .bucketManagementStatistics()
                            .assertEntries(3)
                            .assertCreatedNew(4)
                            .assertComplete(4)
                            .assertNoMoreBucketsDefinite(1)
                        .end()
                    .end()
                    .getObjectable();
        // @formatter:on
    }

    // TODO enable
//    private static final long DEFAULT_TIMEOUT = 30000L;
//
//    private static final File TEST_DIR = new File("src/test/resources/work-distribution");
//
//    private static final TestResource<TaskType> TASK_200_COORDINATOR = new TestResource<>(TEST_DIR, "task-200-c.xml", "44444444-2222-2222-2222-200c00000000");
//    private static final TestResource<TaskType> TASK_200_WORKER = new TestResource<>(TEST_DIR, "task-200-w.xml", "44444444-2222-2222-2222-200w00000000");
//    private static final TestResource<TaskType> TASK_210_COORDINATOR = new TestResource<>(TEST_DIR, "task-210-c.xml", "44444444-2222-2222-2222-210c00000000");
//    private static final TestResource<TaskType> TASK_210_WORKER_1 = new TestResource<>(TEST_DIR, "task-210-1.xml", "44444444-2222-2222-2222-210100000000");
//    private static final TestResource<TaskType> TASK_210_WORKER_2 = new TestResource<>(TEST_DIR, "task-210-2.xml", "44444444-2222-2222-2222-210200000000");
//    private static final TestResource<TaskType> TASK_210_WORKER_3 = new TestResource<>(TEST_DIR, "task-210-3.xml", "44444444-2222-2222-2222-210300000000");
//    private static final TestResource<TaskType> TASK_220_COORDINATOR = new TestResource<>(TEST_DIR, "task-220-c.xml", "44444444-2222-2222-2222-220c00000000");
//    private static final TestResource<TaskType> TASK_220_WORKER_1 = new TestResource<>(TEST_DIR, "task-220-1.xml", "44444444-2222-2222-2222-220100000000");
//    private static final TestResource<TaskType> TASK_220_WORKER_2 = new TestResource<>(TEST_DIR, "task-220-2.xml", "44444444-2222-2222-2222-220200000000");
//    private static final TestResource<TaskType> TASK_220_WORKER_3 = new TestResource<>(TEST_DIR, "task-220-3.xml", "44444444-2222-2222-2222-220300000000");
//    private static final TestResource<TaskType> TASK_230_COORDINATOR = new TestResource<>(TEST_DIR, "task-230-c.xml", "44444444-2222-2222-2222-230c00000000");
//    private static final TestResource<TaskType> TASK_230_WORKER_1 = new TestResource<>(TEST_DIR, "task-230-1.xml", "44444444-2222-2222-2222-230100000000");
//    private static final TestResource<TaskType> TASK_230_WORKER_2 = new TestResource<>(TEST_DIR, "task-230-2.xml", "44444444-2222-2222-2222-230200000000");
//    private static final TestResource<TaskType> TASK_230_WORKER_3 = new TestResource<>(TEST_DIR, "task-230-3.xml", "44444444-2222-2222-2222-230300000000");
//    private static final TestResource<TaskType> TASK_300_COORDINATOR = new TestResource<>(TEST_DIR, "task-300-c.xml", "44444444-2222-2222-2222-300c00000000");
//    private static final TestResource<TaskType> TASK_300_WORKER = new TestResource<>(TEST_DIR, "task-300-w.xml", "44444444-2222-2222-2222-300w00000000");
//
//    @Override
//    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
//        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
//    }
//
//    @Test
//    public void test200OneWorkerTask() throws Exception {
//        given();
//
//        OperationResult result = createOperationResult();
//        repoAdd(TASK_200_COORDINATOR, result); // waiting; 3 buckets per 10 objects, single
//        repoAdd(TASK_200_WORKER, result); // suspended
//
//        Task worker = taskManager.getTaskPlain(TASK_200_WORKER.oid, result);
//
//        try {
//            when();
//
//            taskManager.resumeTask(worker, result);
//
//            then();
//            String coordinatorTaskOid = TASK_200_COORDINATOR.oid;
//            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);
//
//            Task coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
//            Task workerAfter = taskManager.getTaskPlain(worker.getOid(), result);
//            displayDumpable("coordinator task after", coordinatorAfter);
//            displayDumpable("worker task after", workerAfter);
//            displayIterativeStatisticsAndProgress(workerAfter);
//
//            assertTotalSuccessCountInIterativeInfo(30, singleton(workerAfter));
//            assertTotalSuccessCountInProgress(30, 0, singleton(workerAfter));
//        } finally {
//            suspendAndDeleteTasks(TASK_200_COORDINATOR.oid);
//        }
//    }
//
//    @Test
//    public void test210ThreeWorkersTask() throws Exception {
//        given();
//
//        OperationResult result = createOperationResult();
//        taskAdd(TASK_210_COORDINATOR, result); // waiting, buckets sized 10, to 107
//        taskAdd(TASK_210_WORKER_1, result); // suspended
//        taskAdd(TASK_210_WORKER_2, result); // suspended
//        taskAdd(TASK_210_WORKER_3, result); // suspended
//
//        try {
//            Task worker1 = taskManager.getTaskPlain(TASK_210_WORKER_1.oid, result);
//            Task worker2 = taskManager.getTaskPlain(TASK_210_WORKER_2.oid, result);
//            Task worker3 = taskManager.getTaskPlain(TASK_210_WORKER_3.oid, result);
//
////            workBucketsTaskHandler.setDelayProcessor(50);
//
//            when();
//
//            taskManager.resumeTask(worker1, result);
//            taskManager.resumeTask(worker2, result);
//            taskManager.resumeTask(worker3, result);
//
//            then();
//
//            String coordinatorTaskOid = TASK_210_COORDINATOR.oid;
//            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);
//
//            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
//            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
//            worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
//            worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
//            displayDumpable("coordinator task after", coordinatorAfter);
//            displayWorkers(worker1, worker2, worker3);
//
//            assertNumberOfBuckets(coordinatorAfter, 11);
//
//            assertOptimizedCompletedBuckets(coordinatorAfter);
//
//            assertTotalSuccessCountInIterativeInfo(107, Arrays.asList(worker1, worker2, worker3));
//            assertTotalSuccessCountInProgress(107, 0, Arrays.asList(worker1, worker2, worker3));
//
//            // WHEN
//            //taskManager.resumeTask();
//
//            // TODO other asserts
//        } finally {
//            suspendAndDeleteTasks(TASK_210_COORDINATOR.oid);
//        }
//    }
//
//    private void taskAdd(TestResource<TaskType> testResource, OperationResult result) {
//        throw new UnsupportedOperationException();
//    }
//
//    private void displayWorkers(TaskQuartzImpl worker1, TaskQuartzImpl worker2, TaskQuartzImpl worker3) {
////        displayDumpable("worker1 task after", worker1);
////        displayDumpable("worker2 task after", worker2);
////        displayDumpable("worker3 task after", worker3);
//        displayIterativeStatisticsAndProgress(worker1);
//        displayIterativeStatisticsAndProgress(worker2);
//        displayIterativeStatisticsAndProgress(worker3);
//    }
//
//    private void displayIterativeStatisticsAndProgress(Task task) {
//        displayValue(task.getName() + " stats", IterativeTaskInformation.format(task.getStoredOperationStatsOrClone().getIterativeTaskInformation()));
//        displayValue(task.getName() + " progress", StructuredTaskProgress.format(task.getStructuredProgressOrClone()));
//    }
//
//    @Test
//    public void test220WorkerSuspend() throws Exception {
//        given();
//
//        OperationResult result = createOperationResult();
//
//        taskAdd(TASK_220_COORDINATOR, result); // waiting, bucket size 10, up to 107
//        taskAdd(TASK_220_WORKER_1, result); // suspended
//        taskAdd(TASK_220_WORKER_2, result); // suspended
//        taskAdd(TASK_220_WORKER_3, result); // suspended
//
//        try {
//            Task worker1 = taskManager.getTaskPlain(TASK_220_WORKER_1.oid, result);
//            Task worker2 = taskManager.getTaskPlain(TASK_220_WORKER_2.oid, result);
//            Task worker3 = taskManager.getTaskPlain(TASK_220_WORKER_3.oid, result);
//
//            Holder<Task> suspensionVictim = new Holder<>();
//            workBucketsTaskHandler.setProcessor((task, bucket, index) -> {
//                if (index == 44) {
//                    task.updateAndStoreStatisticsIntoRepository(true, new OperationResult("storeStats"));
//                    display("Going to suspend " + task);
//                    new Thread(() -> {
//                        taskStateManager.suspendTaskNoException((TaskQuartzImpl) task, TaskManager.DO_NOT_WAIT, new OperationResult("suspend"));
//                        display("Suspended " + task);
//                        suspensionVictim.setValue(task);
//                    }).start();
//                    sleepChecked(20000);
//                } else {
//                    sleepChecked(100);
//                }
//            });
//
//            when();
//
//            taskManager.resumeTask(worker1, result);
//            taskManager.resumeTask(worker2, result);
//            taskManager.resumeTask(worker3, result);
//
//            then();
//
//            String coordinatorTaskOid = TASK_220_COORDINATOR.oid;
//            // We have to wait for success closed because that is updated after iterative item information.
//            waitFor("waiting for all items to be processed", () -> getTotalSuccessClosed(coordinatorTaskOid) == 107 - 10,
//                    DEFAULT_TIMEOUT, 500);
//
//            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
//            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
//            worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
//            worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
//            displayDumpable("coordinator task after unfinished run", coordinatorAfter);
//            displayWorkers(worker1, worker2, worker3);
//
//            assertTotalSuccessCountInIterativeInfo(107 - 6, Arrays.asList(worker1, worker2, worker3));
//            assertTotalSuccessCountInProgress(107 - 10, 4, Arrays.asList(worker1, worker2, worker3));
//
//            assertOptimizedCompletedBuckets(coordinatorAfter);
//
//            // TODO other asserts
//
//            when("delete victim");
//
//            workBucketsTaskHandler.setDelayProcessor(50);
//
//            TaskQuartzImpl deletedTask = taskManager.getTaskPlain(suspensionVictim.getValue().getOid(), null, result);
//            display("Deleting task " + deletedTask);
//            taskManager.deleteTask(deletedTask.getOid(), result);
//
//            then("delete victim");
//
//            display("Waiting for coordinator task close");
//            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);
//
//            coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
//            displayDumpable("coordinator task after finished run", coordinatorAfter);
//            displayWorkers(worker1, worker2, worker3);
//
//            assertOptimizedCompletedBuckets(coordinatorAfter);
//
//            // Some of the "closed" successes were counted in the task that is now removed.
//            int missingClosed = TaskProgressUtil.getProgressForOutcome(deletedTask.getStructuredProgressOrClone(), SUCCESS, false);
//
//            assertTotalSuccessCountInProgress(107 - missingClosed, 0, coordinatorAfter.listSubtasks(result));
//        } finally {
//            suspendAndDeleteTasks(TASK_220_COORDINATOR.oid);
//        }
//    }
//
//    @Test
//    public void test230WorkerException() throws Exception {
//        given();
//
//        OperationResult result = createOperationResult();
//        add(TASK_230_COORDINATOR, result); // waiting, bucket size 10, up to 107
//        add(TASK_230_WORKER_1, result); // suspended
//        add(TASK_230_WORKER_2, result); // suspended
//        add(TASK_230_WORKER_3, result); // suspended
//
//        try {
//            TaskQuartzImpl worker1 = taskManager.getTaskPlain(TASK_230_WORKER_1.oid, result);
//            TaskQuartzImpl worker2 = taskManager.getTaskPlain(TASK_230_WORKER_2.oid, result);
//            TaskQuartzImpl worker3 = taskManager.getTaskPlain(TASK_230_WORKER_3.oid, result);
//
//            Holder<Task> exceptionVictim = new Holder<>();
//            workBucketsTaskHandler.setProcessor((task, bucket, index) -> {
//                if (index == 44) {
//                    task.updateAndStoreStatisticsIntoRepository(true, new OperationResult("storeStats"));
//                    display("Going to explode in " + task);
//                    exceptionVictim.setValue(task);
//                    throw new IllegalStateException("Bum");
//                } else {
//                    sleepChecked(100);
//                }
//            });
//
//            when();
//
//            taskManager.resumeTask(worker1, result);
//            taskManager.resumeTask(worker2, result);
//            taskManager.resumeTask(worker3, result);
//
//            then();
//
//            String coordinatorTaskOid = TASK_230_COORDINATOR.oid;
//            // We have to wait for success closed because that is updated after iterative item information.
//            waitFor("waiting for all items to be processed", () -> getTotalSuccessClosed(coordinatorTaskOid) == 107 - 10,
//                    DEFAULT_TIMEOUT, 500);
//
//            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
//            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
//            worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
//            worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
//            displayDumpable("coordinator task after unfinished run", coordinatorAfter);
//            displayWorkers(worker1, worker2, worker3);
//
//            assertTotalSuccessCountInIterativeInfo(107 - 6, Arrays.asList(worker1, worker2, worker3));
//            assertTotalSuccessCountInProgress(107 - 10, 4, Arrays.asList(worker1, worker2, worker3));
//
//            assertOptimizedCompletedBuckets(coordinatorAfter);
//
//            // TODO other asserts
//
//            when("close victim");
//
//            workBucketsTaskHandler.setDelayProcessor(50);
//
//            String oidToClose = exceptionVictim.getValue().getOid();
//            display("Closing task " + oidToClose);
//            taskManager.closeTask(taskManager.getTaskPlain(oidToClose, result), result);
//
//            then("close victim");
//
//            display("Waiting for coordinator task close");
//            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);
//
//            coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
//            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
//            worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
//            worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
//            displayDumpable("coordinator task after", coordinatorAfter);
//            displayWorkers(worker1, worker2, worker3);
//
//            assertTotalSuccessCountInIterativeInfo(107 - 6 + 10, coordinatorAfter.listSubtasks(result));
//            assertTotalSuccessCountInProgress(107, 4, coordinatorAfter.listSubtasks(result));
//
//            assertOptimizedCompletedBuckets(coordinatorAfter);
//        } finally {
//            suspendAndDeleteTasks(TASK_230_COORDINATOR.oid);
//        }
//    }
//
//    @Test
//    public void test300NarrowQueryOneWorkerTask() throws Exception {
//        given();
//
//        OperationResult result = createOperationResult();
//        add(TASK_300_COORDINATOR, result); // waiting; 3 buckets per 10 items
//        add(TASK_300_WORKER, result); // suspended
//
//        workBucketsTaskHandler.resetBeforeTest();
//        workBucketsTaskHandler.setDefaultQuery(prismContext.queryFactory().createQuery());
//
//        try {
//
//            TaskQuartzImpl worker = taskManager.getTaskPlain(TASK_300_WORKER.oid, result);
//
//            when();
//
//            taskManager.resumeTask(worker, result);
//
//            then();
//
//            String coordinatorTaskOid = TASK_300_COORDINATOR.oid;
//            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);
//
//            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
//            TaskQuartzImpl workerAfter = taskManager.getTaskPlain(worker.getOid(), result);
//            displayDumpable("coordinator task after", coordinatorAfter);
//            displayDumpable("worker task after", workerAfter);
//
//            assertTotalSuccessCountInIterativeInfo(30, singleton(workerAfter));
//            assertTotalSuccessCountInProgress(30, 0, singleton(workerAfter));
//
//            List<ObjectQuery> qe = workBucketsTaskHandler.getQueriesExecuted();
//            displayValue("Queries executed", qe);
//            assertEquals("Wrong # of queries", 3, qe.size());
//            ObjectQuery q1 = prismContext.queryFor(UserType.class)
//                    .item(UserType.F_ITERATION).ge(BigInteger.valueOf(0))
//                    .and().item(UserType.F_ITERATION).lt(BigInteger.valueOf(10))
//                    .build();
//            ObjectQuery q2 = prismContext.queryFor(UserType.class)
//                    .item(UserType.F_ITERATION).ge(BigInteger.valueOf(10))
//                    .and().item(UserType.F_ITERATION).lt(BigInteger.valueOf(20))
//                    .build();
//            ObjectQuery q3 = prismContext.queryFor(UserType.class)
//                    .item(UserType.F_ITERATION).ge(BigInteger.valueOf(20))
//                    .and().item(UserType.F_ITERATION).lt(BigInteger.valueOf(30))
//                    .build();
//            PrismAsserts.assertQueriesEquivalent("Wrong query #1", q1, qe.get(0));
//            PrismAsserts.assertQueriesEquivalent("Wrong query #2", q2, qe.get(1));
//            PrismAsserts.assertQueriesEquivalent("Wrong query #3", q3, qe.get(2));
//        } finally {
//            suspendAndDeleteTasks(TASK_300_COORDINATOR.oid);
//        }
//    }
}
