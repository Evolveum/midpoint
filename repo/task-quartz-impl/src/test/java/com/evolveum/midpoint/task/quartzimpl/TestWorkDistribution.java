/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil.sortBucketsBySequentialNumber;
import static com.evolveum.midpoint.test.IntegrationTestTools.waitFor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.quartzimpl.work.WorkStateManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

/**
 * Tests basic features of work state management:
 * - basic creation of work buckets
 * - allocation, completion, release of buckets
 * - allocation of buckets when some workers are suspended
 * - basic propagation of buckets into bucket-aware task handler
 * <p>
 * Both in coordinator-worker and standalone tasks.
 *
 * @author mederly
 */

@ContextConfiguration(locations = { "classpath:ctx-task-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestWorkDistribution extends AbstractTaskManagerTest {

    public static final long DEFAULT_TIMEOUT = 30000L;

    @Autowired private WorkStateManager workStateManager;

    private String taskFilename(String subId) {
        return "src/test/resources/work/task-" + getTestNumber() + "-" + subId + ".xml";
    }

    private String taskFilename() {
        return taskFilename("0");
    }

    private String taskOid(String subId) {
        return "44444444-2222-2222-2222-" + getTestNumber() + subId + "00000000";
    }

    private String taskOid() {
        return taskOid("0");
    }

    @NotNull
    protected String workerTaskFilename() {
        return taskFilename("w");
    }

    @NotNull
    protected String coordinatorTaskFilename() {
        return taskFilename("c");
    }

    @NotNull
    protected String workerTaskOid() {
        return taskOid("w");
    }

    @NotNull
    protected String coordinatorTaskOid() {
        return taskOid("c");
    }

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
        OperationResult result = createOperationResult();
        addObjectFromFile(coordinatorTaskFilename());
        addObjectFromFile(workerTaskFilename());

        try {
            TaskQuartzImpl worker = taskManager.getTaskPlain(workerTaskOid(), result);

            // WHEN
            WorkBucketType bucket = workStateManager.getWorkBucket(worker.getOid(), 0, null, null, result);

            // THEN
            displayValue("allocated bucket", bucket);
            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid(), result);
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
            suspendAndDeleteTasks(coordinatorTaskOid());
        }
    }

    @Test
    public void test105AllocateBucketStandalone() throws Exception {
        OperationResult result = createOperationResult();
        addObjectFromFile(taskFilename());

        TaskQuartzImpl standalone = taskManager.getTaskPlain(taskOid(), result);
        try {

            // WHEN
            WorkBucketType bucket = workStateManager.getWorkBucket(standalone.getOid(), 0, null, null, result);

            // THEN
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
        OperationResult result = createOperationResult();
        addObjectFromFile(taskFilename());

        TaskQuartzImpl standalone = taskManager.getTaskPlain(taskOid(), result);
        try {

            // WHEN
            WorkBucketType bucket = workStateManager.getWorkBucket(standalone.getOid(), 0, null, null, result);

            // THEN
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
        OperationResult result = createOperationResult();
        addObjectFromFile(taskFilename());

        TaskQuartzImpl standalone = taskManager.getTaskPlain(taskOid(), result);
        try {

            // WHEN
            WorkBucketType bucket1 = workStateManager.getWorkBucket(standalone.getOid(), 0, null, null, result);
            WorkBucketType bucket2 = workStateManager.getWorkBucket(standalone.getOid(), 0, null, null, result);

            // THEN
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

            // WHEN
            workStateManager.completeWorkBucket(standalone.getOid(), 1, null, result);
            WorkBucketType bucket3 = workStateManager.getWorkBucket(standalone.getOid(), 0, null, null, result);

            // THEN
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

            // WHEN
            workStateManager.completeWorkBucket(standalone.getOid(), 2, null, result);
            WorkBucketType bucket4 = workStateManager.getWorkBucket(standalone.getOid(), 0, null, null, result);

            // THEN
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
        OperationResult result = createOperationResult();
        addObjectFromFile(taskFilename());

        TaskQuartzImpl task = taskManager.getTaskPlain(taskOid(), result);

        // WHEN + THEN
        try {
            workStateManager.getWorkBucket(task.getOid(), 0, null, null, result);
            fail("unexpected success");
        } catch (IllegalStateException e) {
            System.out.println("Got expected exception: " + e.getMessage());
        }
    }

    @Test
    public void test130AllocateReleaseCompleteSequence() throws Exception {
        OperationResult result = createOperationResult();
        addObjectFromFile(coordinatorTaskFilename());
        addObjectFromFile(taskFilename("1"));
        addObjectFromFile(taskFilename("2"));
        addObjectFromFile(taskFilename("3"));
        addObjectFromFile(taskFilename("4"));
        addObjectFromFile(taskFilename("5"));

        try {
            TaskQuartzImpl worker1 = taskManager.getTaskPlain(taskOid("1"), result);
            TaskQuartzImpl worker2 = taskManager.getTaskPlain(taskOid("2"), result);
            TaskQuartzImpl worker3 = taskManager.getTaskPlain(taskOid("3"), result);
            TaskQuartzImpl worker4 = taskManager.getTaskPlain(taskOid("4"), result);
            TaskQuartzImpl worker5 = taskManager.getTaskPlain(taskOid("5"), result);

            // WHEN
            WorkBucketType bucket1 = workStateManager.getWorkBucket(worker1.getOid(), 0, null, null, result);
            WorkBucketType bucket2 = workStateManager.getWorkBucket(worker2.getOid(), 0, null, null, result);
            WorkBucketType bucket3 = workStateManager.getWorkBucket(worker3.getOid(), 0, null, null, result);
            WorkBucketType bucket4 = workStateManager.getWorkBucket(worker4.getOid(), 0, null, null, result);
            WorkBucketType bucket4a = workStateManager
                    .getWorkBucket(worker4.getOid(), 0, null, null, result);     // should be the same as bucket4

            // THEN
            displayValue("1st allocated bucket", bucket1);
            displayValue("2nd allocated bucket", bucket2);
            displayValue("3rd allocated bucket", bucket3);
            displayValue("4th allocated bucket", bucket4);
            displayValue("4+th allocated bucket", bucket4a);
            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
            worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
            worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
            worker4 = taskManager.getTaskPlain(worker4.getOid(), result);
            Task coordinator = taskManager.getTaskPlain(coordinatorTaskOid(), result);
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

            // WHEN
            workStateManager.completeWorkBucket(worker2.getOid(), 2, null, result);

            // THEN
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

            // WHEN
            workStateManager.completeWorkBucket(worker1.getOid(), 1, null, result);
            WorkBucketType bucket = workStateManager.getWorkBucket(worker1.getOid(), 0, null, null, result);

            // THEN
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

            // WHEN
            WorkBucketType nothing = workStateManager.getWorkBucket(worker5.getOid(), 0, null, null, result);

            // THEN
            assertNull("Found bucket even if none should be found", nothing);

            // WHEN
            // TODO set some state here and check its transfer to coordinator task
            workStateManager.releaseWorkBucket(worker4.getOid(), 4, null, result);

            // THEN
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

            // WHEN
            workStateManager.completeWorkBucket(worker3.getOid(), 3, null, result);
            bucket = workStateManager.getWorkBucket(worker5.getOid(), 0, null, null, result);

            // THEN
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

            // WHEN
            workStateManager.completeWorkBucket(worker1.getOid(), 5, null, result);
            taskManager.closeTask(worker5, result);

            // THEN
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

            // WHEN
            bucket = workStateManager.getWorkBucket(worker1.getOid(), 100, null, null, result);

            // THEN
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

            // WHEN
            workStateManager.completeWorkBucket(worker1.getOid(), 4, null, result);

            // THEN
            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
            displayDumpable("worker1 after completion of 4th bucket", worker1);
            coordinator = taskManager.getTaskPlain(coordinator.getOid(), result);
            displayDumpable("coordinator after completion of 4th bucket", coordinator);

            buckets = new ArrayList<>(coordinator.getWorkState().getBucket());
            assertEquals(1, buckets.size());
            assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 5, 4, 5);

            assertNoWorkBuckets(worker1.getWorkState());
        } finally {
            suspendAndDeleteTasks(coordinatorTaskOid());
        }
    }

    @Test
    public void test200OneWorkerTask() throws Exception {
        OperationResult result = createOperationResult();
        addObjectFromFile(coordinatorTaskFilename());
        addObjectFromFile(workerTaskFilename());

        TaskQuartzImpl worker = taskManager.getTaskPlain(workerTaskOid(), result);

        try {
            // WHEN
            taskManager.resumeTask(worker, result);

            // THEN
            String coordinatorTaskOid = coordinatorTaskOid();
            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);

            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
            TaskQuartzImpl workerAfter = taskManager.getTaskPlain(worker.getOid(), result);
            displayDumpable("coordinator task after", coordinatorAfter);
            displayDumpable("worker task after", workerAfter);

            assertTotalSuccessCount(30, singleton(workerAfter));
        } finally {
            suspendAndDeleteTasks(coordinatorTaskOid());
        }
    }

    @Test
    public void test210ThreeWorkersTask() throws Exception {
        OperationResult result = createOperationResult();
        addObjectFromFile(coordinatorTaskFilename());
        addObjectFromFile(taskFilename("1"));
        addObjectFromFile(taskFilename("2"));
        addObjectFromFile(taskFilename("3"));

        try {
            TaskQuartzImpl worker1 = taskManager.getTaskPlain(taskOid("1"), result);
            TaskQuartzImpl worker2 = taskManager.getTaskPlain(taskOid("2"), result);
            TaskQuartzImpl worker3 = taskManager.getTaskPlain(taskOid("3"), result);

            workBucketsTaskHandler.setDelayProcessor(50);

            // WHEN
            taskManager.resumeTask(worker1, result);
            taskManager.resumeTask(worker2, result);
            taskManager.resumeTask(worker3, result);

            // THEN
            String coordinatorTaskOid = coordinatorTaskOid();
            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);

            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
            worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
            worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
            displayDumpable("coordinator task after", coordinatorAfter);
            displayDumpable("worker1 task after", worker1);
            displayDumpable("worker2 task after", worker2);
            displayDumpable("worker3 task after", worker3);
            displayValue("worker1 op stats task after", PrismTestUtil.serializeAnyDataWrapped(worker1.getStoredOperationStats()));
            displayValue("worker2 op stats task after", PrismTestUtil.serializeAnyDataWrapped(worker2.getStoredOperationStats()));
            displayValue("worker3 op stats task after", PrismTestUtil.serializeAnyDataWrapped(worker3.getStoredOperationStats()));

            assertNumberOfBuckets(coordinatorAfter, 11);

            assertOptimizedCompletedBuckets(coordinatorAfter);

            assertTotalSuccessCount(107, Arrays.asList(worker1, worker2, worker3));

            // WHEN
            //taskManager.resumeTask();

            // TODO other asserts
        } finally {
            suspendAndDeleteTasks(coordinatorTaskOid());
        }
    }

    @Test
    public void test220WorkerSuspend() throws Exception {
        OperationResult result = createOperationResult();
        addObjectFromFile(coordinatorTaskFilename());
        addObjectFromFile(taskFilename("1"));
        addObjectFromFile(taskFilename("2"));
        addObjectFromFile(taskFilename("3"));
        try {
            TaskQuartzImpl worker1 = taskManager.getTaskPlain(taskOid("1"), result);
            TaskQuartzImpl worker2 = taskManager.getTaskPlain(taskOid("2"), result);
            TaskQuartzImpl worker3 = taskManager.getTaskPlain(taskOid("3"), result);

            Holder<Task> suspensionVictim = new Holder<>();
            workBucketsTaskHandler.setProcessor((task, bucket, index) -> {
                if (index == 44) {
                    task.storeOperationStats();         // to store operational stats for this task
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

            // WHEN
            when();
            taskManager.resumeTask(worker1, result);
            taskManager.resumeTask(worker2, result);
            taskManager.resumeTask(worker3, result);

            // THEN
            then();
            String coordinatorTaskOid = coordinatorTaskOid();
            waitFor("waiting for all items to be processed", () -> getTotalItemsProcessed(coordinatorTaskOid) == 107 - 6,
                    DEFAULT_TIMEOUT, 500);

            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
            worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
            worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
            displayDumpable("coordinator task after unfinished run", coordinatorAfter);
            displayDumpable("worker1 task after unfinished run", worker1);
            displayDumpable("worker2 task after unfinished run", worker2);
            displayDumpable("worker3 task after unfinished run", worker3);
            displayValue("worker1 op stats task after unfinished run",
                    PrismTestUtil.serializeAnyDataWrapped(worker1.getStoredOperationStats()));
            displayValue("worker2 op stats task after unfinished run",
                    PrismTestUtil.serializeAnyDataWrapped(worker2.getStoredOperationStats()));
            displayValue("worker3 op stats task after unfinished run",
                    PrismTestUtil.serializeAnyDataWrapped(worker3.getStoredOperationStats()));

            assertTotalSuccessCount(107 - 6, Arrays.asList(worker1, worker2, worker3));

            assertOptimizedCompletedBuckets(coordinatorAfter);

            // TODO other asserts

            // WHEN
            when();

            workBucketsTaskHandler.setDelayProcessor(50);

            String oidToDelete = suspensionVictim.getValue().getOid();
            display("Deleting task " + oidToDelete);
            taskManager.deleteTask(oidToDelete, result);

            // THEN
            then();
            display("Waiting for coordinator task close");
            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);

            coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
            displayDumpable("coordinator task after finished run", coordinatorAfter);

            assertOptimizedCompletedBuckets(coordinatorAfter);

            // TODO change after correct resuming
            // this does not work as processed items from deleted subtask are missing
            //assertTotalSuccessCount(107 - 6 + 10, coordinatorAfter.listSubtasks(result));
        } finally {
            suspendAndDeleteTasks(coordinatorTaskOid());
        }
    }

    @Test
    public void test230WorkerException() throws Exception {
        OperationResult result = createOperationResult();
        addObjectFromFile(coordinatorTaskFilename());
        addObjectFromFile(taskFilename("1"));
        addObjectFromFile(taskFilename("2"));
        addObjectFromFile(taskFilename("3"));
        try {
            TaskQuartzImpl worker1 = taskManager.getTaskPlain(taskOid("1"), result);
            TaskQuartzImpl worker2 = taskManager.getTaskPlain(taskOid("2"), result);
            TaskQuartzImpl worker3 = taskManager.getTaskPlain(taskOid("3"), result);

            Holder<Task> exceptionVictim = new Holder<>();
            workBucketsTaskHandler.setProcessor((task, bucket, index) -> {
                if (index == 44) {
                    task.storeOperationStats();         // to store operational stats for this task
                    display("Going to explode in " + task);
                    exceptionVictim.setValue(task);
                    throw new IllegalStateException("Bum");
                } else {
                    sleepChecked(100);
                }
            });

            // WHEN
            when();
            taskManager.resumeTask(worker1, result);
            taskManager.resumeTask(worker2, result);
            taskManager.resumeTask(worker3, result);

            // THEN
            then();
            String coordinatorTaskOid = coordinatorTaskOid();
            waitFor("waiting for all items to be processed", () -> getTotalItemsProcessed(coordinatorTaskOid) == 107 - 6,
                    DEFAULT_TIMEOUT, 500);

            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
            worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
            worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
            displayDumpable("coordinator task after unfinished run", coordinatorAfter);
            displayDumpable("worker1 task after unfinished run", worker1);
            displayDumpable("worker2 task after unfinished run", worker2);
            displayDumpable("worker3 task after unfinished run", worker3);
            displayValue("worker1 op stats task after unfinished run",
                    PrismTestUtil.serializeAnyDataWrapped(worker1.getStoredOperationStats()));
            displayValue("worker2 op stats task after unfinished run",
                    PrismTestUtil.serializeAnyDataWrapped(worker2.getStoredOperationStats()));
            displayValue("worker3 op stats task after unfinished run",
                    PrismTestUtil.serializeAnyDataWrapped(worker3.getStoredOperationStats()));

            assertTotalSuccessCount(107 - 6, Arrays.asList(worker1, worker2, worker3));

            assertOptimizedCompletedBuckets(coordinatorAfter);

            // TODO other asserts

            // WHEN
            when();

            workBucketsTaskHandler.setDelayProcessor(50);

            String oidToClose = exceptionVictim.getValue().getOid();
            display("Closing task " + oidToClose);
            taskManager.closeTask(taskManager.getTaskPlain(oidToClose, result), result);

            // THEN
            then();
            display("Waiting for coordinator task close");
            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);

            coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
            worker1 = taskManager.getTaskPlain(worker1.getOid(), result);
            worker2 = taskManager.getTaskPlain(worker2.getOid(), result);
            worker3 = taskManager.getTaskPlain(worker3.getOid(), result);
            displayDumpable("coordinator task after", coordinatorAfter);
            displayDumpable("worker1 task after", worker1);
            displayDumpable("worker2 task after", worker2);
            displayDumpable("worker3 task after", worker3);
            displayValue("worker1 op stats task after", PrismTestUtil.serializeAnyDataWrapped(worker1.getStoredOperationStats()));
            displayValue("worker2 op stats task after", PrismTestUtil.serializeAnyDataWrapped(worker2.getStoredOperationStats()));
            displayValue("worker3 op stats task after", PrismTestUtil.serializeAnyDataWrapped(worker3.getStoredOperationStats()));

            // TODO change after correct resuming
            assertTotalSuccessCount(107 - 6 + 10, coordinatorAfter.listSubtasks(result));

            assertOptimizedCompletedBuckets(coordinatorAfter);
        } finally {
            suspendAndDeleteTasks(coordinatorTaskOid());
        }
    }

    @Test
    public void test300NarrowQueryOneWorkerTask() throws Exception {
        OperationResult result = createOperationResult();
        addObjectFromFile(coordinatorTaskFilename());
        addObjectFromFile(workerTaskFilename());

        workBucketsTaskHandler.resetBeforeTest();
        workBucketsTaskHandler.setDefaultQuery(prismContext.queryFactory().createQuery());

        try {

            TaskQuartzImpl worker = taskManager.getTaskPlain(workerTaskOid(), result);

            // WHEN
            taskManager.resumeTask(worker, result);

            // THEN
            String coordinatorTaskOid = coordinatorTaskOid();
            waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);

            TaskQuartzImpl coordinatorAfter = taskManager.getTaskPlain(coordinatorTaskOid, result);
            TaskQuartzImpl workerAfter = taskManager.getTaskPlain(worker.getOid(), result);
            displayDumpable("coordinator task after", coordinatorAfter);
            displayDumpable("worker task after", workerAfter);

            assertTotalSuccessCount(30, singleton(workerAfter));

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
            suspendAndDeleteTasks(coordinatorTaskOid());
        }
    }
}
