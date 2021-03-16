/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.util.task.TaskWorkStateUtil.sortBucketsBySequentialNumber;
import static com.evolveum.midpoint.test.IntegrationTestTools.waitFor;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ItemProcessingOutcomeType.SUCCESS;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;

import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;

import com.evolveum.midpoint.schema.util.task.TaskProgressUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.StructuredTaskProgress;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.quartzimpl.work.WorkStateManager;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

/**
 * Tests basic features of work state management:
 *
 * - basic creation of work buckets
 * - allocation, completion, release of buckets
 * - allocation of buckets when some workers are suspended
 * - basic propagation of buckets into bucket-aware task handler
 *
 * Both in coordinator-worker and standalone tasks.
 */

@ContextConfiguration(locations = { "classpath:ctx-task-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestWorkDistribution extends AbstractTaskManagerTest {

    public static final long DEFAULT_TIMEOUT = 30000L;

    @Autowired private WorkStateManager workStateManager;

    private static final File TEST_DIR = new File("src/test/resources/work-distribution");

    private static final TestResource<TaskType> TASK_100_COORDINATOR = new TestResource<>(TEST_DIR, "task-100-c.xml", "44444444-2222-2222-2222-100c00000000");
    private static final TestResource<TaskType> TASK_100_WORKER = new TestResource<>(TEST_DIR, "task-100-w.xml", "44444444-2222-2222-2222-100w00000000");
    private static final TestResource<TaskType> TASK_105 = new TestResource<>(TEST_DIR, "task-105.xml", "44444444-2222-2222-2222-105000000000");
    private static final TestResource<TaskType> TASK_107 = new TestResource<>(TEST_DIR, "task-107.xml", "44444444-2222-2222-2222-107000000000");
    private static final TestResource<TaskType> TASK_110 = new TestResource<>(TEST_DIR, "task-110.xml", "44444444-2222-2222-2222-110000000000");
    private static final TestResource<TaskType> TASK_120 = new TestResource<>(TEST_DIR, "task-120.xml", "44444444-2222-2222-2222-120000000000");
    private static final TestResource<TaskType> TASK_130_COORDINATOR = new TestResource<>(TEST_DIR, "task-130-c.xml", "44444444-2222-2222-2222-130c00000000");
    private static final TestResource<TaskType> TASK_130_WORKER_1 = new TestResource<>(TEST_DIR, "task-130-1.xml", "44444444-2222-2222-2222-130100000000");
    private static final TestResource<TaskType> TASK_130_WORKER_2 = new TestResource<>(TEST_DIR, "task-130-2.xml", "44444444-2222-2222-2222-130200000000");
    private static final TestResource<TaskType> TASK_130_WORKER_3 = new TestResource<>(TEST_DIR, "task-130-3.xml", "44444444-2222-2222-2222-130300000000");
    private static final TestResource<TaskType> TASK_130_WORKER_4 = new TestResource<>(TEST_DIR, "task-130-4.xml", "44444444-2222-2222-2222-130400000000");
    private static final TestResource<TaskType> TASK_130_WORKER_5 = new TestResource<>(TEST_DIR, "task-130-5.xml", "44444444-2222-2222-2222-130500000000");
    private static final TestResource<TaskType> TASK_200_COORDINATOR = new TestResource<>(TEST_DIR, "task-200-c.xml", "44444444-2222-2222-2222-200c00000000");
    private static final TestResource<TaskType> TASK_200_WORKER = new TestResource<>(TEST_DIR, "task-200-w.xml", "44444444-2222-2222-2222-200w00000000");
    private static final TestResource<TaskType> TASK_210_COORDINATOR = new TestResource<>(TEST_DIR, "task-210-c.xml", "44444444-2222-2222-2222-210c00000000");
    private static final TestResource<TaskType> TASK_210_WORKER_1 = new TestResource<>(TEST_DIR, "task-210-1.xml", "44444444-2222-2222-2222-210100000000");
    private static final TestResource<TaskType> TASK_210_WORKER_2 = new TestResource<>(TEST_DIR, "task-210-2.xml", "44444444-2222-2222-2222-210200000000");
    private static final TestResource<TaskType> TASK_210_WORKER_3 = new TestResource<>(TEST_DIR, "task-210-3.xml", "44444444-2222-2222-2222-210300000000");
    private static final TestResource<TaskType> TASK_220_COORDINATOR = new TestResource<>(TEST_DIR, "task-220-c.xml", "44444444-2222-2222-2222-220c00000000");
    private static final TestResource<TaskType> TASK_220_WORKER_1 = new TestResource<>(TEST_DIR, "task-220-1.xml", "44444444-2222-2222-2222-220100000000");
    private static final TestResource<TaskType> TASK_220_WORKER_2 = new TestResource<>(TEST_DIR, "task-220-2.xml", "44444444-2222-2222-2222-220200000000");
    private static final TestResource<TaskType> TASK_220_WORKER_3 = new TestResource<>(TEST_DIR, "task-220-3.xml", "44444444-2222-2222-2222-220300000000");
    private static final TestResource<TaskType> TASK_230_COORDINATOR = new TestResource<>(TEST_DIR, "task-230-c.xml", "44444444-2222-2222-2222-230c00000000");
    private static final TestResource<TaskType> TASK_230_WORKER_1 = new TestResource<>(TEST_DIR, "task-230-1.xml", "44444444-2222-2222-2222-230100000000");
    private static final TestResource<TaskType> TASK_230_WORKER_2 = new TestResource<>(TEST_DIR, "task-230-2.xml", "44444444-2222-2222-2222-230200000000");
    private static final TestResource<TaskType> TASK_230_WORKER_3 = new TestResource<>(TEST_DIR, "task-230-3.xml", "44444444-2222-2222-2222-230300000000");
    private static final TestResource<TaskType> TASK_300_COORDINATOR = new TestResource<>(TEST_DIR, "task-300-c.xml", "44444444-2222-2222-2222-300c00000000");
    private static final TestResource<TaskType> TASK_300_WORKER = new TestResource<>(TEST_DIR, "task-300-w.xml", "44444444-2222-2222-2222-300w00000000");

    @PostConstruct
    public void initialize() throws Exception {
        displayTestTitle("Initializing TEST CLASS: " + getClass().getName());
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
    public void test100AllocateBucket() throws Exception {
        given();
        OperationResult result = createOperationResult();
        add(TASK_100_COORDINATOR, result); // suspended
        add(TASK_100_WORKER, result); // suspended

        try {
            TaskQuartzImpl worker = taskManager.getTaskPlain(TASK_100_WORKER.oid, result);

            when();

            WorkBucketType bucket = workStateManager.getWorkBucket(worker.getOid(), 0, null, null, result);

            then();

            displayValue("allocated bucket", bucket);
            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(TASK_100_COORDINATOR.oid, result);
            TaskQuartzImpl workerAfter = taskManager.getTaskPlain(worker.getOid(), result);
            displayDumpable("coordinator task after", coordinatorAfter);
            displayDumpable("worker task after", workerAfter);

            assertNumericBucket(bucket, null, 1, 0, 1000);
            List<WorkBucketType> wBuckets = workerAfter.getWorkState().getBucket();
            assertNumericBucket(wBuckets.get(0), WorkBucketStateType.READY, 1, 0, 1000);
            List<WorkBucketType> cBuckets = coordinatorAfter.getWorkState().getBucket();
            assertNumericBucket(cBuckets.get(0), WorkBucketStateType.DELEGATED, 1, 0, 1000);
            assertNumberOfBuckets(coordinatorAfter, 100);

            assertOptimizedCompletedBuckets(coordinatorAfter);
        } finally {
            suspendAndDeleteTasks(TASK_100_COORDINATOR.oid);
        }
    }

    @Test
    public void test105AllocateBucketStandalone() throws Exception {
        given();

        OperationResult result = createOperationResult();

        add(TASK_105, result); // suspended

        TaskQuartzImpl standalone = taskManager.getTaskPlain(TASK_105.oid, result);
        try {

            when();

            WorkBucketType bucket = workStateManager.getWorkBucket(standalone.getOid(), 0, null, null, result);

            then();

            displayValue("allocated bucket", bucket);
            TaskQuartzImpl standaloneAfter = taskManager.getTaskPlain(standalone.getOid(), result);
            displayDumpable("task after", standaloneAfter);

            List<WorkBucketType> wBuckets = standaloneAfter.getWorkState().getBucket();
            assertEquals("Wrong # of buckets", 1, wBuckets.size());
            assertBucket(wBuckets.get(0), WorkBucketStateType.READY, 1);
            assertNull(wBuckets.get(0).getContent());
            assertNumberOfBuckets(standaloneAfter, 1);
        } finally {
            suspendAndDeleteTasks(standalone.getOid());
        }
    }

    @Test
    public void test107AllocateBucketStandaloneBatched() throws Exception {
        given();

        OperationResult result = createOperationResult();
        add(TASK_107, result); // suspended

        TaskQuartzImpl standalone = taskManager.getTaskPlain(TASK_107.oid, result);
        try {

            when();

            WorkBucketType bucket = workStateManager.getWorkBucket(standalone.getOid(), 0, null, null, result);

            then();

            displayValue("allocated bucket", bucket);
            TaskQuartzImpl standaloneAfter = taskManager.getTaskPlain(standalone.getOid(), result);
            displayDumpable("task after", standaloneAfter);

            List<WorkBucketType> wBuckets = standaloneAfter.getWorkState().getBucket();
            assertEquals("Wrong # of buckets", 7, wBuckets.size());
            assertBucket(wBuckets.get(0), WorkBucketStateType.READY, 1);
            assertNumberOfBuckets(standaloneAfter, 1000);
        } finally {
            suspendAndDeleteTasks(standalone.getOid());
        }
    }

    @Test
    public void test110AllocateTwoBucketsStandalone() throws Exception {
        given();

        OperationResult result = createOperationResult();
        add(TASK_110, result);

        TaskQuartzImpl standalone = taskManager.getTaskPlain(TASK_110.oid, result);
        try {

            when();

            WorkBucketType bucket1 = workStateManager.getWorkBucket(standalone.getOid(), 0, null, null, result);
            WorkBucketType bucket2 = workStateManager.getWorkBucket(standalone.getOid(), 0, null, null, result);

            then();

            displayValue("1st obtained bucket", bucket1);
            displayValue("2nd obtained bucket", bucket2);
            standalone = taskManager.getTaskPlain(standalone.getOid(), result);
            displayDumpable("task after 2x get", standalone);

            assertNumericBucket(bucket1, WorkBucketStateType.READY, 1, 0, 100);
            assertNumericBucket(bucket2, WorkBucketStateType.READY, 1, 0, 100);     // should be the same

            List<WorkBucketType> buckets = new ArrayList<>(standalone.getWorkState().getBucket());
            sortBucketsBySequentialNumber(buckets);
            assertEquals(1, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 1, 0, 100);

            when("complete");

            workStateManager.completeWorkBucket(standalone.getOid(), 1, null, result);
            WorkBucketType bucket3 = workStateManager.getWorkBucket(standalone.getOid(), 0, null, null, result);

            then("complete");

            displayValue("bucket obtained after complete", bucket3);
            standalone = taskManager.getTaskPlain(standalone.getOid(), result);
            displayDumpable("task after complete+get", standalone);
            assertOptimizedCompletedBuckets(standalone);

            assertNumericBucket(bucket3, WorkBucketStateType.READY, 2, 100, 200);

            buckets = new ArrayList<>(standalone.getWorkState().getBucket());
            sortBucketsBySequentialNumber(buckets);
            assertEquals(2, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 1, 0, 100);
            assertNumericBucket(buckets.get(1), WorkBucketStateType.READY, 2, 100, 200);

            when("complete 2");

            workStateManager.completeWorkBucket(standalone.getOid(), 2, null, result);
            WorkBucketType bucket4 = workStateManager.getWorkBucket(standalone.getOid(), 0, null, null, result);

            then("complete 2");

            displayValue("bucket obtained after 2nd complete", bucket4);
            standalone = taskManager.getTaskPlain(standalone.getOid(), result);
            displayDumpable("task after complete+get+complete+get", standalone);

            assertNumericBucket(bucket4, WorkBucketStateType.READY, 3, 200, 300);

            buckets = new ArrayList<>(standalone.getWorkState().getBucket());
            sortBucketsBySequentialNumber(buckets);
            assertEquals(2, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 2, 100, 200);
            assertNumericBucket(buckets.get(1), WorkBucketStateType.READY, 3, 200, 300);
            assertOptimizedCompletedBuckets(standalone);
        } finally {
            suspendAndDeleteTasks(standalone.getOid());
        }
    }

    @Test
    public void test120UnspecifiedBuckets() throws Exception {
        given();

        OperationResult result = createOperationResult();
        add(TASK_120, result); // suspended

        TaskQuartzImpl task = taskManager.getTaskPlain(TASK_120.oid, result);

        try {
            when();

            workStateManager.getWorkBucket(task.getOid(), 0, null, null, result);
            fail("unexpected success");
        } catch (IllegalStateException e) {

            then();

            System.out.println("Got expected exception: " + e.getMessage());
        }
    }

    @Test
    public void test130AllocateReleaseCompleteSequence() throws Exception {
        OperationResult result = createOperationResult();
        add(TASK_130_COORDINATOR, result); // suspended
        add(TASK_130_WORKER_1, result); // suspended
        add(TASK_130_WORKER_2, result); // suspended
        add(TASK_130_WORKER_3, result); // suspended
        add(TASK_130_WORKER_4, result); // suspended
        add(TASK_130_WORKER_5, result); // suspended

        try {
            TaskQuartzImpl worker1 = taskManager.getTaskPlain(TASK_130_WORKER_1.oid, result);
            TaskQuartzImpl worker2 = taskManager.getTaskPlain(TASK_130_WORKER_2.oid, result);
            TaskQuartzImpl worker3 = taskManager.getTaskPlain(TASK_130_WORKER_3.oid, result);
            TaskQuartzImpl worker4 = taskManager.getTaskPlain(TASK_130_WORKER_4.oid, result);
            TaskQuartzImpl worker5 = taskManager.getTaskPlain(TASK_130_WORKER_5.oid, result);

            when();

            WorkBucketType bucket1 = workStateManager.getWorkBucket(worker1.getOid(), 0, null, null, result);
            WorkBucketType bucket2 = workStateManager.getWorkBucket(worker2.getOid(), 0, null, null, result);
            WorkBucketType bucket3 = workStateManager.getWorkBucket(worker3.getOid(), 0, null, null, result);
            WorkBucketType bucket4 = workStateManager.getWorkBucket(worker4.getOid(), 0, null, null, result);
            WorkBucketType bucket4a = workStateManager
                    .getWorkBucket(worker4.getOid(), 0, null, null, result);     // should be the same as bucket4

            then();

            displayValue("1st allocated bucket", bucket1);
            displayValue("2nd allocated bucket", bucket2);
            displayValue("3rd allocated bucket", bucket3);
            displayValue("4th allocated bucket", bucket4);
            displayValue("4+th allocated bucket", bucket4a);
            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
            worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
            worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
            worker4 = taskManager.getTaskPlain(worker4.getOid(), result);
            Task coordinator = taskManager.getTaskPlain(TASK_130_COORDINATOR.oid, result);
            displayDumpable("coordinator task after 4+1x allocation", coordinator);
            displayDumpable("worker1 task after 4+1x allocation", worker1);
            displayDumpable("worker2 task after 4+1x allocation", worker2);
            displayDumpable("worker3 task after 4+1x allocation", worker3);
            displayDumpable("worker4 task after 4+1x allocation", worker4);

            assertNumericBucket(bucket1, null, 1, 0, 1);
            assertNumericBucket(bucket2, null, 2, 1, 2);
            assertNumericBucket(bucket3, null, 3, 2, 3);
            assertNumericBucket(bucket4, null, 4, 3, 4);
            assertNumericBucket(bucket4a, null, 4, 3, 4);
            List<WorkBucketType> buckets = new ArrayList<>(coordinator.getWorkState().getBucket());
            sortBucketsBySequentialNumber(buckets);
            assertEquals(5, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.DELEGATED, 1, 0, 1);
            assertNumericBucket(buckets.get(1), WorkBucketStateType.DELEGATED, 2, 1, 2);
            assertNumericBucket(buckets.get(2), WorkBucketStateType.DELEGATED, 3, 2, 3);
            assertNumericBucket(buckets.get(3), WorkBucketStateType.DELEGATED, 4, 3, 4);
            assertNumericBucket(buckets.get(4), WorkBucketStateType.READY, 5, 4, 5);        // pre-created

            buckets = new ArrayList<>(worker1.getWorkState().getBucket());
            assertEquals(1, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 1, 0, 1);
            buckets = new ArrayList<>(worker2.getWorkState().getBucket());
            assertEquals(1, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 2, 1, 2);
            buckets = new ArrayList<>(worker3.getWorkState().getBucket());
            assertEquals(1, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 3, 2, 3);
            buckets = new ArrayList<>(worker4.getWorkState().getBucket());
            assertEquals(1, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 4, 3, 4);

            when("complete bucket #2");

            workStateManager.completeWorkBucket(worker2.getOid(), 2, null, result);

            then("complete bucket #2");

            worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
            displayDumpable("worker2 after completion of 2nd bucket", worker2);
            coordinator = taskManager.getTaskPlain(coordinator.getOid(), result);
            displayDumpable("coordinator after completion of 2nd bucket", coordinator);

            buckets = new ArrayList<>(coordinator.getWorkState().getBucket());
            sortBucketsBySequentialNumber(buckets);

            assertEquals(5, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.DELEGATED, 1, 0, 1);
            assertNumericBucket(buckets.get(1), WorkBucketStateType.COMPLETE, 2, 1, 2);
            assertNumericBucket(buckets.get(2), WorkBucketStateType.DELEGATED, 3, 2, 3);
            assertNumericBucket(buckets.get(3), WorkBucketStateType.DELEGATED, 4, 3, 4);
            assertNumericBucket(buckets.get(4), WorkBucketStateType.READY, 5, 4, 5);        // pre-created

            assertNoWorkBuckets(worker2.getWorkState());

            when("complete bucket #1");

            workStateManager.completeWorkBucket(worker1.getOid(), 1, null, result);
            WorkBucketType bucket = workStateManager.getWorkBucket(worker1.getOid(), 0, null, null, result);

            then("complete bucket #1");

            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
            displayDumpable("worker1 after completion of 1st bucket and fetching next one", worker1);
            coordinator = taskManager.getTaskPlain(coordinator.getOid(), result);
            displayDumpable("coordinator after completion of 1st bucket and fetching next one", coordinator);

            assertNumericBucket(bucket, null, 5, 4, 5);

            buckets = new ArrayList<>(coordinator.getWorkState().getBucket());
            sortBucketsBySequentialNumber(buckets);

            assertEquals(4, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 2, 1, 2);
            assertNumericBucket(buckets.get(1), WorkBucketStateType.DELEGATED, 3, 2, 3);
            assertNumericBucket(buckets.get(2), WorkBucketStateType.DELEGATED, 4, 3, 4);
            assertNumericBucket(buckets.get(3), WorkBucketStateType.DELEGATED, 5, 4, 5);

            buckets = new ArrayList<>(worker1.getWorkState().getBucket());
            assertEquals(1, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 5, 4, 5);

            when("no more buckets");

            WorkBucketType nothing = workStateManager.getWorkBucket(worker5.getOid(), 0, null, null, result);

            then("no more buckets");

            assertNull("Found bucket even if none should be found", nothing);

            when("release bucket #4");

            // TODO set some state here and check its transfer to coordinator task
            workStateManager.releaseWorkBucket(worker4.getOid(), 4, null, result);

            then("release bucket #4");

            worker4 = taskManager.getTaskPlain(worker4.getOid(), result);
            displayDumpable("worker4 after releasing of 4th bucket", worker4);
            coordinator = taskManager.getTaskPlain(coordinator.getOid(), result);
            displayDumpable("coordinator after releasing of 4th bucket", coordinator);

            buckets = new ArrayList<>(coordinator.getWorkState().getBucket());
            sortBucketsBySequentialNumber(buckets);

            assertEquals(4, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 2, 1, 2);
            assertNumericBucket(buckets.get(1), WorkBucketStateType.DELEGATED, 3, 2, 3);
            assertNumericBucket(buckets.get(2), WorkBucketStateType.READY, 4, 3, 4);
            assertNumericBucket(buckets.get(3), WorkBucketStateType.DELEGATED, 5, 4, 5);

            assertNoWorkBuckets(worker4.getWorkState());

            when("complete bucket #3");

            workStateManager.completeWorkBucket(worker3.getOid(), 3, null, result);
            bucket = workStateManager.getWorkBucket(worker5.getOid(), 0, null, null, result);

            then("complete bucket #3");

            worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
            displayDumpable("worker3 after completion of 3rd bucket and getting next one", worker3);
            worker5 = taskManager.getTaskPlain(worker5.getOid(), result);
            displayDumpable("worker5 after completion of 3rd bucket and getting next one", worker5);
            coordinator = taskManager.getTaskPlain(coordinator.getOid(), result);
            displayDumpable("coordinator after completion of 3rd bucket and getting next one", coordinator);

            assertNumericBucket(bucket, null, 4, 3, 4);

            buckets = new ArrayList<>(coordinator.getWorkState().getBucket());
            sortBucketsBySequentialNumber(buckets);
            assertEquals(3, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 3, 2, 3);
            assertNumericBucket(buckets.get(1), WorkBucketStateType.DELEGATED, 4, 3, 4);
            assertNumericBucket(buckets.get(2), WorkBucketStateType.DELEGATED, 5, 4, 5);

            assertNoWorkBuckets(worker3.getWorkState());

            buckets = new ArrayList<>(worker5.getWorkState().getBucket());
            assertEquals(1, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 4, 3, 4);

            when("complete bucket #5");

            workStateManager.completeWorkBucket(worker1.getOid(), 5, null, result);
            taskManager.closeTask(worker5, result);

            then("complete bucket #5");

            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
            displayDumpable("worker1 after completion of 5th bucket and closing worker5", worker1);
            worker5 = taskManager.getTaskPlain(worker5.getOid(), result);
            displayDumpable("worker5 after completion of 5th bucket and closing worker5", worker5);
            coordinator = taskManager.getTaskPlain(coordinator.getOid(), result);
            displayDumpable("coordinator after completion of 5th bucket and closing worker5", coordinator);

            buckets = new ArrayList<>(coordinator.getWorkState().getBucket());
            assertEquals(2, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.DELEGATED, 4, 3, 4);
            assertNumericBucket(buckets.get(1), WorkBucketStateType.COMPLETE, 5, 4, 5);

            assertNoWorkBuckets(worker1.getWorkState());

            when("reclaiming mis-allocated bucket");

            bucket = workStateManager.getWorkBucket(worker1.getOid(), 100, null, null, result);

            then("reclaiming mis-allocated bucket");

            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
            displayDumpable("worker1 after reclaiming mis-allocated bucket", worker1);
            coordinator = taskManager.getTaskPlain(coordinator.getOid(), result);
            displayDumpable("coordinator after reclaiming mis-allocated bucket", coordinator);

            assertNumericBucket(bucket, null, 4, 3, 4);

            buckets = new ArrayList<>(coordinator.getWorkState().getBucket());
            assertEquals(2, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.DELEGATED, 4, 3, 4);
            assertNumericBucket(buckets.get(1), WorkBucketStateType.COMPLETE, 5, 4, 5);

            buckets = new ArrayList<>(worker1.getWorkState().getBucket());
            assertEquals(1, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 4, 3, 4);

            when("complete bucket #4");

            workStateManager.completeWorkBucket(worker1.getOid(), 4, null, result);

            then("complete bucket #4");

            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
            displayDumpable("worker1 after completion of 4th bucket", worker1);
            coordinator = taskManager.getTaskPlain(coordinator.getOid(), result);
            displayDumpable("coordinator after completion of 4th bucket", coordinator);

            buckets = new ArrayList<>(coordinator.getWorkState().getBucket());
            assertEquals(1, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 5, 4, 5);

            assertNoWorkBuckets(worker1.getWorkState());
        } finally {
            suspendAndDeleteTasks(TASK_130_COORDINATOR.oid);
        }
    }

    @Test
    public void test200OneWorkerTask() throws Exception {
        given();

        OperationResult result = createOperationResult();
        add(TASK_200_COORDINATOR, result); // waiting; 3 buckets per 10 objects, single
        add(TASK_200_WORKER, result); // suspended

        TaskQuartzImpl worker = taskManager.getTaskPlain(TASK_200_WORKER.oid, result);

        try {
            when();

            taskManager.resumeTask(worker, result);

            then();
            String coordinatorTaskOid = TASK_200_COORDINATOR.oid;
            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);

            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
            TaskQuartzImpl workerAfter = taskManager.getTaskPlain(worker.getOid(), result);
            displayDumpable("coordinator task after", coordinatorAfter);
            displayDumpable("worker task after", workerAfter);
            displayIterativeStatisticsAndProgress(workerAfter);

            assertTotalSuccessCountInIterativeInfo(30, singleton(workerAfter));
            assertTotalSuccessCountInProgress(30, 0, singleton(workerAfter));
        } finally {
            suspendAndDeleteTasks(TASK_200_COORDINATOR.oid);
        }
    }

    @Test
    public void test210ThreeWorkersTask() throws Exception {
        given();

        OperationResult result = createOperationResult();
        add(TASK_210_COORDINATOR, result); // waiting, buckets sized 10, to 107
        add(TASK_210_WORKER_1, result); // suspended
        add(TASK_210_WORKER_2, result); // suspended
        add(TASK_210_WORKER_3, result); // suspended

        try {
            TaskQuartzImpl worker1 = taskManager.getTaskPlain(TASK_210_WORKER_1.oid, result);
            TaskQuartzImpl worker2 = taskManager.getTaskPlain(TASK_210_WORKER_2.oid, result);
            TaskQuartzImpl worker3 = taskManager.getTaskPlain(TASK_210_WORKER_3.oid, result);

            workBucketsTaskHandler.setDelayProcessor(50);

            when();

            taskManager.resumeTask(worker1, result);
            taskManager.resumeTask(worker2, result);
            taskManager.resumeTask(worker3, result);

            then();

            String coordinatorTaskOid = TASK_210_COORDINATOR.oid;
            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);

            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
            worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
            worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
            displayDumpable("coordinator task after", coordinatorAfter);
            displayWorkers(worker1, worker2, worker3);

            assertNumberOfBuckets(coordinatorAfter, 11);

            assertOptimizedCompletedBuckets(coordinatorAfter);

            assertTotalSuccessCountInIterativeInfo(107, Arrays.asList(worker1, worker2, worker3));
            assertTotalSuccessCountInProgress(107, 0, Arrays.asList(worker1, worker2, worker3));

            // WHEN
            //taskManager.resumeTask();

            // TODO other asserts
        } finally {
            suspendAndDeleteTasks(TASK_210_COORDINATOR.oid);
        }
    }

    private void displayWorkers(TaskQuartzImpl worker1, TaskQuartzImpl worker2, TaskQuartzImpl worker3) {
//        displayDumpable("worker1 task after", worker1);
//        displayDumpable("worker2 task after", worker2);
//        displayDumpable("worker3 task after", worker3);
        displayIterativeStatisticsAndProgress(worker1);
        displayIterativeStatisticsAndProgress(worker2);
        displayIterativeStatisticsAndProgress(worker3);
    }

    private void displayIterativeStatisticsAndProgress(TaskQuartzImpl task) {
        displayValue(task.getName() + " stats", IterativeTaskInformation.format(task.getStoredOperationStatsOrClone().getIterativeTaskInformation()));
        displayValue(task.getName() + " progress", StructuredTaskProgress.format(task.getStructuredProgressOrClone()));
    }

    @Test
    public void test220WorkerSuspend() throws Exception {
        given();

        OperationResult result = createOperationResult();

        add(TASK_220_COORDINATOR, result); // waiting, bucket size 10, up to 107
        add(TASK_220_WORKER_1, result); // suspended
        add(TASK_220_WORKER_2, result); // suspended
        add(TASK_220_WORKER_3, result); // suspended

        try {
            TaskQuartzImpl worker1 = taskManager.getTaskPlain(TASK_220_WORKER_1.oid, result);
            TaskQuartzImpl worker2 = taskManager.getTaskPlain(TASK_220_WORKER_2.oid, result);
            TaskQuartzImpl worker3 = taskManager.getTaskPlain(TASK_220_WORKER_3.oid, result);

            Holder<Task> suspensionVictim = new Holder<>();
            workBucketsTaskHandler.setProcessor((task, bucket, index) -> {
                if (index == 44) {
                    task.storeOperationStatsAndProgress(); // to store operational stats for this task
                    display("Going to suspend " + task);
                    new Thread(() -> {
                        taskStateManager.suspendTaskNoException((TaskQuartzImpl) task, TaskManager.DO_NOT_WAIT, new OperationResult("suspend"));
                        display("Suspended " + task);
                        suspensionVictim.setValue(task);
                    }).start();
                    sleepChecked(20000);
                } else {
                    sleepChecked(100);
                }
            });

            when();

            taskManager.resumeTask(worker1, result);
            taskManager.resumeTask(worker2, result);
            taskManager.resumeTask(worker3, result);

            then();

            String coordinatorTaskOid = TASK_220_COORDINATOR.oid;
            // We have to wait for success closed because that is updated after iterative item information.
            waitFor("waiting for all items to be processed", () -> getTotalSuccessClosed(coordinatorTaskOid) == 107 - 10,
                    DEFAULT_TIMEOUT, 500);

            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
            worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
            worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
            displayDumpable("coordinator task after unfinished run", coordinatorAfter);
            displayWorkers(worker1, worker2, worker3);

            assertTotalSuccessCountInIterativeInfo(107 - 6, Arrays.asList(worker1, worker2, worker3));
            assertTotalSuccessCountInProgress(107 - 10, 4, Arrays.asList(worker1, worker2, worker3));

            assertOptimizedCompletedBuckets(coordinatorAfter);

            // TODO other asserts

            when("delete victim");

            workBucketsTaskHandler.setDelayProcessor(50);

            TaskQuartzImpl deletedTask = taskManager.getTaskPlain(suspensionVictim.getValue().getOid(), null, result);
            display("Deleting task " + deletedTask);
            taskManager.deleteTask(deletedTask.getOid(), result);

            then("delete victim");

            display("Waiting for coordinator task close");
            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);

            coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
            displayDumpable("coordinator task after finished run", coordinatorAfter);
            displayWorkers(worker1, worker2, worker3);

            assertOptimizedCompletedBuckets(coordinatorAfter);

            // Some of the "closed" successes were counted in the task that is now removed.
            int missingClosed = TaskProgressUtil.getProgressForOutcome(deletedTask.getStructuredProgressOrClone(), SUCCESS, false);

            assertTotalSuccessCountInProgress(107 - missingClosed, 0, coordinatorAfter.listSubtasks(result));
        } finally {
            suspendAndDeleteTasks(TASK_220_COORDINATOR.oid);
        }
    }

    @Test
    public void test230WorkerException() throws Exception {
        given();

        OperationResult result = createOperationResult();
        add(TASK_230_COORDINATOR, result); // waiting, bucket size 10, up to 107
        add(TASK_230_WORKER_1, result); // suspended
        add(TASK_230_WORKER_2, result); // suspended
        add(TASK_230_WORKER_3, result); // suspended

        try {
            TaskQuartzImpl worker1 = taskManager.getTaskPlain(TASK_230_WORKER_1.oid, result);
            TaskQuartzImpl worker2 = taskManager.getTaskPlain(TASK_230_WORKER_2.oid, result);
            TaskQuartzImpl worker3 = taskManager.getTaskPlain(TASK_230_WORKER_3.oid, result);

            Holder<Task> exceptionVictim = new Holder<>();
            workBucketsTaskHandler.setProcessor((task, bucket, index) -> {
                if (index == 44) {
                    task.storeOperationStatsAndProgress(); // to store operational stats for this task
                    display("Going to explode in " + task);
                    exceptionVictim.setValue(task);
                    throw new IllegalStateException("Bum");
                } else {
                    sleepChecked(100);
                }
            });

            when();

            taskManager.resumeTask(worker1, result);
            taskManager.resumeTask(worker2, result);
            taskManager.resumeTask(worker3, result);

            then();

            String coordinatorTaskOid = TASK_230_COORDINATOR.oid;
            // We have to wait for success closed because that is updated after iterative item information.
            waitFor("waiting for all items to be processed", () -> getTotalSuccessClosed(coordinatorTaskOid) == 107 - 10,
                    DEFAULT_TIMEOUT, 500);

            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
            worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
            worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
            displayDumpable("coordinator task after unfinished run", coordinatorAfter);
            displayWorkers(worker1, worker2, worker3);

            assertTotalSuccessCountInIterativeInfo(107 - 6, Arrays.asList(worker1, worker2, worker3));
            assertTotalSuccessCountInProgress(107 - 10, 4, Arrays.asList(worker1, worker2, worker3));

            assertOptimizedCompletedBuckets(coordinatorAfter);

            // TODO other asserts

            when("close victim");

            workBucketsTaskHandler.setDelayProcessor(50);

            String oidToClose = exceptionVictim.getValue().getOid();
            display("Closing task " + oidToClose);
            taskManager.closeTask(taskManager.getTaskPlain(oidToClose, result), result);

            then("close victim");

            display("Waiting for coordinator task close");
            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);

            coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
            worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
            worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
            displayDumpable("coordinator task after", coordinatorAfter);
            displayWorkers(worker1, worker2, worker3);

            assertTotalSuccessCountInIterativeInfo(107 - 6 + 10, coordinatorAfter.listSubtasks(result));
            assertTotalSuccessCountInProgress(107, 4, coordinatorAfter.listSubtasks(result));

            assertOptimizedCompletedBuckets(coordinatorAfter);
        } finally {
            suspendAndDeleteTasks(TASK_230_COORDINATOR.oid);
        }
    }

    @Test
    public void test300NarrowQueryOneWorkerTask() throws Exception {
        given();

        OperationResult result = createOperationResult();
        add(TASK_300_COORDINATOR, result); // waiting; 3 buckets per 10 items
        add(TASK_300_WORKER, result); // suspended

        workBucketsTaskHandler.resetBeforeTest();
        workBucketsTaskHandler.setDefaultQuery(prismContext.queryFactory().createQuery());

        try {

            TaskQuartzImpl worker = taskManager.getTaskPlain(TASK_300_WORKER.oid, result);

            when();

            taskManager.resumeTask(worker, result);

            then();

            String coordinatorTaskOid = TASK_300_COORDINATOR.oid;
            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);

            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
            TaskQuartzImpl workerAfter = taskManager.getTaskPlain(worker.getOid(), result);
            displayDumpable("coordinator task after", coordinatorAfter);
            displayDumpable("worker task after", workerAfter);

            assertTotalSuccessCountInIterativeInfo(30, singleton(workerAfter));
            assertTotalSuccessCountInProgress(30, 0, singleton(workerAfter));

            List<ObjectQuery> qe = workBucketsTaskHandler.getQueriesExecuted();
            displayValue("Queries executed", qe);
            assertEquals("Wrong # of queries", 3, qe.size());
            ObjectQuery q1 = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ITERATION).ge(BigInteger.valueOf(0))
                    .and().item(UserType.F_ITERATION).lt(BigInteger.valueOf(10))
                    .build();
            ObjectQuery q2 = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ITERATION).ge(BigInteger.valueOf(10))
                    .and().item(UserType.F_ITERATION).lt(BigInteger.valueOf(20))
                    .build();
            ObjectQuery q3 = prismContext.queryFor(UserType.class)
                    .item(UserType.F_ITERATION).ge(BigInteger.valueOf(20))
                    .and().item(UserType.F_ITERATION).lt(BigInteger.valueOf(30))
                    .build();
            PrismAsserts.assertQueriesEquivalent("Wrong query #1", q1, qe.get(0));
            PrismAsserts.assertQueriesEquivalent("Wrong query #2", q2, qe.get(1));
            PrismAsserts.assertQueriesEquivalent("Wrong query #3", q3, qe.get(2));
        } finally {
            suspendAndDeleteTasks(TASK_300_COORDINATOR.oid);
        }
    }
}
