/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.quartzimpl.work.WorkStateManager;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.evolveum.midpoint.task.quartzimpl.work.WorkBucketUtil.sortBucketsBySequentialNumber;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.waitFor;
import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.*;

/**
 * Tests basic features of work state management:
 * - basic creation of work buckets
 * - allocation, completion, release of buckets
 * - allocation of buckets when some workers are suspended
 * - basic propagation of buckets into bucket-aware task handler
 *
 * Both in coordinator-worker and standalone tasks.
 *
 * @author mederly
 */

@ContextConfiguration(locations = {"classpath:ctx-task-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestWorkDistribution extends AbstractTaskManagerTest {

	private static final transient Trace LOGGER = TraceManager.getTrace(TestWorkDistribution.class);

	public static final long DEFAULT_TIMEOUT = 30000L;

	@Autowired private WorkStateManager workStateManager;

	private static String taskFilename(String testName, String subId) {
		return "src/test/resources/work/task-" + testNumber(testName) + "-" + subId + ".xml";
	}

	private static String taskFilename(String testName) {
		return taskFilename(testName, "0");
	}

	private static String taskOid(String testName, String subId) {
		return "44444444-2222-2222-2222-" + testNumber(testName) + subId + "00000000";
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
		workStateManager.setFreeBucketWaitInterval(1000L);
		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
	}

    @Test
    public void test000Integrity() {
        AssertJUnit.assertNotNull(repositoryService);
        AssertJUnit.assertNotNull(taskManager);
    }

    @Test
    public void test100AllocateBucket() throws Exception {
        final String TEST_NAME = "test100AllocateBucket";
        OperationResult result = createResult(TEST_NAME, LOGGER);
        addObjectFromFile(coordinatorTaskFilename(TEST_NAME));
        addObjectFromFile(workerTaskFilename(TEST_NAME));

        try {
	        TaskQuartzImpl worker = taskManager.getTask(workerTaskOid(TEST_NAME), result);

	        // WHEN
	        WorkBucketType bucket = workStateManager.getWorkBucket(worker.getOid(), 0, null, result);

	        // THEN
	        display("allocated bucket", bucket);
	        TaskQuartzImpl coordinatorAfter = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
	        TaskQuartzImpl workerAfter = taskManager.getTask(worker.getOid(), result);
	        display("coordinator task after", coordinatorAfter);
	        display("worker task after", workerAfter);

	        assertNumericBucket(bucket, null, 1, 0, 1000);
	        List<WorkBucketType> wBuckets = workerAfter.getTaskType().getWorkState().getBucket();
	        assertNumericBucket(wBuckets.get(0), WorkBucketStateType.READY, 1, 0, 1000);
	        List<WorkBucketType> cBuckets = coordinatorAfter.getTaskType().getWorkState().getBucket();
	        assertNumericBucket(cBuckets.get(0), WorkBucketStateType.DELEGATED, 1, 0, 1000);
	        assertNumberOfBuckets(coordinatorAfter, 100);

	        assertOptimizedCompletedBuckets(coordinatorAfter);
        } finally {
	        suspendAndDeleteTasks(coordinatorTaskOid(TEST_NAME));
        }
    }

	@Test
    public void test105AllocateBucketStandalone() throws Exception {
        final String TEST_NAME = "test105AllocateBucketStandalone";
        OperationResult result = createResult(TEST_NAME, LOGGER);
        addObjectFromFile(taskFilename(TEST_NAME));

		TaskQuartzImpl standalone = taskManager.getTask(taskOid(TEST_NAME), result);
		try {

	        // WHEN
	        WorkBucketType bucket = workStateManager.getWorkBucket(standalone.getOid(), 0, null, result);

	        // THEN
	        display("allocated bucket", bucket);
	        TaskQuartzImpl standaloneAfter = taskManager.getTask(standalone.getOid(), result);
	        display("task after", standaloneAfter);

	        List<WorkBucketType> wBuckets = standaloneAfter.getTaskType().getWorkState().getBucket();
	        assertEquals("Wrong # of buckets", 1, wBuckets.size());
	        assertBucket(wBuckets.get(0), WorkBucketStateType.READY, 1);
	        assertNull(wBuckets.get(0).getContent());
	        assertNumberOfBuckets(standaloneAfter, 1);
        } finally {
	        suspendAndDeleteTasks(standalone.getOid());
        }
    }

	@Test
    public void test110AllocateTwoBucketsStandalone() throws Exception {
        final String TEST_NAME = "test110AllocateTwoBucketsStandalone";

        OperationResult result = createResult(TEST_NAME, LOGGER);
		addObjectFromFile(taskFilename(TEST_NAME));

        TaskQuartzImpl standalone = taskManager.getTask(taskOid(TEST_NAME), result);
        try {

	        // WHEN
	        WorkBucketType bucket1 = workStateManager.getWorkBucket(standalone.getOid(), 0, null, result);
	        WorkBucketType bucket2 = workStateManager.getWorkBucket(standalone.getOid(), 0, null, result);

	        // THEN
	        display("1st obtained bucket", bucket1);
	        display("2nd obtained bucket", bucket2);
	        standalone = taskManager.getTask(standalone.getOid(), result);
	        display("task after 2xget", standalone);

	        assertNumericBucket(bucket1, WorkBucketStateType.READY, 1, 0, 100);
	        assertNumericBucket(bucket2, WorkBucketStateType.READY, 1, 0, 100);     // should be the same

	        List<WorkBucketType> buckets = new ArrayList<>(standalone.getWorkState().getBucket());
	        sortBucketsBySequentialNumber(buckets);
	        assertEquals(1, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 1, 0, 100);

	        // WHEN
	        workStateManager.completeWorkBucket(standalone.getOid(), 1, result);
	        WorkBucketType bucket3 = workStateManager.getWorkBucket(standalone.getOid(), 0, null, result);

	        // THEN
	        display("bucket obtained after complete", bucket3);
	        standalone = taskManager.getTask(standalone.getOid(), result);
	        display("task after complete+get", standalone);
	        assertOptimizedCompletedBuckets(standalone);

	        assertNumericBucket(bucket3, WorkBucketStateType.READY, 2, 100, 200);

	        buckets = new ArrayList<>(standalone.getWorkState().getBucket());
	        sortBucketsBySequentialNumber(buckets);
	        assertEquals(2, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 1, 0, 100);
	        assertNumericBucket(buckets.get(1), WorkBucketStateType.READY, 2, 100, 200);

	        // WHEN
	        workStateManager.completeWorkBucket(standalone.getOid(), 2, result);
	        WorkBucketType bucket4 = workStateManager.getWorkBucket(standalone.getOid(), 0, null, result);

	        // THEN
	        display("bucket obtained after 2nd complete", bucket4);
	        standalone = taskManager.getTask(standalone.getOid(), result);
	        display("task after complete+get+complete+get", standalone);

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
        final String TEST_NAME = "test120UnspecifiedBuckets";
        OperationResult result = createResult(TEST_NAME, LOGGER);
        addObjectFromFile(taskFilename(TEST_NAME));

        TaskQuartzImpl task = taskManager.getTask(taskOid(TEST_NAME), result);

        // WHEN + THEN
		try {
			workStateManager.getWorkBucket(task.getOid(), 0, null, result);
			fail("unexpected success");
		} catch (IllegalStateException e) {
			System.out.println("Got expected exception: " + e.getMessage());
		}
    }

	@Test
    public void test130AllocateReleaseCompleteSequence() throws Exception {
        final String TEST_NAME = "test130AllocateReleaseCompleteSequence";
        OperationResult result = createResult(TEST_NAME, LOGGER);
        addObjectFromFile(coordinatorTaskFilename(TEST_NAME));
        addObjectFromFile(taskFilename(TEST_NAME, "1"));
        addObjectFromFile(taskFilename(TEST_NAME, "2"));
        addObjectFromFile(taskFilename(TEST_NAME, "3"));
        addObjectFromFile(taskFilename(TEST_NAME, "4"));
        addObjectFromFile(taskFilename(TEST_NAME, "5"));

        try {
	        TaskQuartzImpl worker1 = taskManager.getTask(taskOid(TEST_NAME, "1"), result);
	        TaskQuartzImpl worker2 = taskManager.getTask(taskOid(TEST_NAME, "2"), result);
	        TaskQuartzImpl worker3 = taskManager.getTask(taskOid(TEST_NAME, "3"), result);
	        TaskQuartzImpl worker4 = taskManager.getTask(taskOid(TEST_NAME, "4"), result);
	        TaskQuartzImpl worker5 = taskManager.getTask(taskOid(TEST_NAME, "5"), result);

	        // WHEN
	        WorkBucketType bucket1 = workStateManager.getWorkBucket(worker1.getOid(), 0, null, result);
	        WorkBucketType bucket2 = workStateManager.getWorkBucket(worker2.getOid(), 0, null, result);
	        WorkBucketType bucket3 = workStateManager.getWorkBucket(worker3.getOid(), 0, null, result);
	        WorkBucketType bucket4 = workStateManager.getWorkBucket(worker4.getOid(), 0, null, result);
	        WorkBucketType bucket4a = workStateManager
			        .getWorkBucket(worker4.getOid(), 0, null, result);     // should be the same as bucket4

	        // THEN
	        display("1st allocated bucket", bucket1);
	        display("2nd allocated bucket", bucket2);
	        display("3rd allocated bucket", bucket3);
	        display("4th allocated bucket", bucket4);
	        display("4+th allocated bucket", bucket4a);
	        worker1 = taskManager.getTask(worker1.getOid(), result);
	        worker2 = taskManager.getTask(worker2.getOid(), result);
	        worker3 = taskManager.getTask(worker3.getOid(), result);
	        worker4 = taskManager.getTask(worker4.getOid(), result);
	        Task coordinator = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
	        display("coordinator task after 4+1x allocation", coordinator);
	        display("worker1 task after 4+1x allocation", worker1);
	        display("worker2 task after 4+1x allocation", worker2);
	        display("worker3 task after 4+1x allocation", worker3);
	        display("worker4 task after 4+1x allocation", worker4);

	        assertNumericBucket(bucket1, null, 1, 0, 1);
	        assertNumericBucket(bucket2, null, 2, 1, 2);
	        assertNumericBucket(bucket3, null, 3, 2, 3);
	        assertNumericBucket(bucket4, null, 4, 3, 4);
	        assertNumericBucket(bucket4a, null, 4, 3, 4);
	        List<WorkBucketType> buckets = new ArrayList<>(coordinator.getTaskType().getWorkState().getBucket());
	        sortBucketsBySequentialNumber(buckets);
	        assertEquals(4, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.DELEGATED, 1, 0, 1);
	        assertNumericBucket(buckets.get(1), WorkBucketStateType.DELEGATED, 2, 1, 2);
	        assertNumericBucket(buckets.get(2), WorkBucketStateType.DELEGATED, 3, 2, 3);
	        assertNumericBucket(buckets.get(3), WorkBucketStateType.DELEGATED, 4, 3, 4);

	        buckets = new ArrayList<>(worker1.getTaskType().getWorkState().getBucket());
	        assertEquals(1, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 1, 0, 1);
	        buckets = new ArrayList<>(worker2.getTaskType().getWorkState().getBucket());
	        assertEquals(1, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 2, 1, 2);
	        buckets = new ArrayList<>(worker3.getTaskType().getWorkState().getBucket());
	        assertEquals(1, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 3, 2, 3);
	        buckets = new ArrayList<>(worker4.getTaskType().getWorkState().getBucket());
	        assertEquals(1, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 4, 3, 4);

	        // WHEN
	        workStateManager.completeWorkBucket(worker2.getOid(), 2, result);

	        // THEN
	        worker2 = taskManager.getTask(worker2.getOid(), result);
	        display("worker2 after completion of 2nd bucket", worker2);
	        coordinator = taskManager.getTask(coordinator.getOid(), result);
	        display("coordinator after completion of 2nd bucket", coordinator);

	        buckets = new ArrayList<>(coordinator.getTaskType().getWorkState().getBucket());
	        sortBucketsBySequentialNumber(buckets);

	        assertEquals(4, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.DELEGATED, 1, 0, 1);
	        assertNumericBucket(buckets.get(1), WorkBucketStateType.COMPLETE, 2, 1, 2);
	        assertNumericBucket(buckets.get(2), WorkBucketStateType.DELEGATED, 3, 2, 3);
	        assertNumericBucket(buckets.get(3), WorkBucketStateType.DELEGATED, 4, 3, 4);

	        assertNoWorkBuckets(worker2.getTaskType().getWorkState());

	        // WHEN
	        workStateManager.completeWorkBucket(worker1.getOid(), 1, result);
	        WorkBucketType bucket = workStateManager.getWorkBucket(worker1.getOid(), 0, null, result);

	        // THEN
	        worker1 = taskManager.getTask(worker1.getOid(), result);
	        display("worker1 after completion of 1st bucket and fetching next one", worker1);
	        coordinator = taskManager.getTask(coordinator.getOid(), result);
	        display("coordinator after completion of 1st bucket and fetching next one", coordinator);

	        assertNumericBucket(bucket, null, 5, 4, 5);

	        buckets = new ArrayList<>(coordinator.getWorkState().getBucket());
	        sortBucketsBySequentialNumber(buckets);

	        assertEquals(4, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 2, 1, 2);
	        assertNumericBucket(buckets.get(1), WorkBucketStateType.DELEGATED, 3, 2, 3);
	        assertNumericBucket(buckets.get(2), WorkBucketStateType.DELEGATED, 4, 3, 4);
	        assertNumericBucket(buckets.get(3), WorkBucketStateType.DELEGATED, 5, 4, 5);

	        buckets = new ArrayList<>(worker1.getTaskType().getWorkState().getBucket());
	        assertEquals(1, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 5, 4, 5);

	        // WHEN
	        WorkBucketType nothing = workStateManager.getWorkBucket(worker5.getOid(), 0, null, result);

	        // THEN
	        assertNull("Found bucket even if none should be found", nothing);

	        // WHEN
	        // TODO set some state here and check its transfer to coordinator task
	        workStateManager.releaseWorkBucket(worker4.getOid(), 4, result);

	        // THEN
	        worker4 = taskManager.getTask(worker4.getOid(), result);
	        display("worker4 after releasing of 4th bucket", worker4);
	        coordinator = taskManager.getTask(coordinator.getOid(), result);
	        display("coordinator after releasing of 4th bucket", coordinator);

	        buckets = new ArrayList<>(coordinator.getTaskType().getWorkState().getBucket());
	        sortBucketsBySequentialNumber(buckets);

	        assertEquals(4, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 2, 1, 2);
	        assertNumericBucket(buckets.get(1), WorkBucketStateType.DELEGATED, 3, 2, 3);
	        assertNumericBucket(buckets.get(2), WorkBucketStateType.READY, 4, 3, 4);
	        assertNumericBucket(buckets.get(3), WorkBucketStateType.DELEGATED, 5, 4, 5);

	        assertNoWorkBuckets(worker4.getTaskType().getWorkState());

	        // WHEN
	        workStateManager.completeWorkBucket(worker3.getOid(), 3, result);
	        bucket = workStateManager.getWorkBucket(worker5.getOid(), 0, null, result);

	        // THEN
	        worker3 = taskManager.getTask(worker3.getOid(), result);
	        display("worker3 after completion of 3rd bucket and getting next one", worker3);
	        worker5 = taskManager.getTask(worker5.getOid(), result);
	        display("worker5 after completion of 3rd bucket and getting next one", worker5);
	        coordinator = taskManager.getTask(coordinator.getOid(), result);
	        display("coordinator after completion of 3rd bucket and getting next one", coordinator);

	        assertNumericBucket(bucket, null, 4, 3, 4);

	        buckets = new ArrayList<>(coordinator.getWorkState().getBucket());
	        sortBucketsBySequentialNumber(buckets);
	        assertEquals(3, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 3, 2, 3);
	        assertNumericBucket(buckets.get(1), WorkBucketStateType.DELEGATED, 4, 3, 4);
	        assertNumericBucket(buckets.get(2), WorkBucketStateType.DELEGATED, 5, 4, 5);

	        assertNoWorkBuckets(worker3.getTaskType().getWorkState());

	        buckets = new ArrayList<>(worker5.getWorkState().getBucket());
	        assertEquals(1, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 4, 3, 4);

	        // WHEN
	        workStateManager.completeWorkBucket(worker1.getOid(), 5, result);
	        taskManager.closeTask(worker5, result);

	        // THEN
	        worker1 = taskManager.getTask(worker1.getOid(), result);
	        display("worker1 after completion of 5th bucket and closing worker5", worker1);
	        worker5 = taskManager.getTask(worker5.getOid(), result);
	        display("worker5 after completion of 5th bucket and closing worker5", worker5);
	        coordinator = taskManager.getTask(coordinator.getOid(), result);
	        display("coordinator after completion of 5th bucket and closing worker5", coordinator);

	        buckets = new ArrayList<>(coordinator.getTaskType().getWorkState().getBucket());
	        assertEquals(2, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.DELEGATED, 4, 3, 4);
	        assertNumericBucket(buckets.get(1), WorkBucketStateType.COMPLETE, 5, 4, 5);

	        assertNoWorkBuckets(worker1.getTaskType().getWorkState());

	        // WHEN
	        bucket = workStateManager.getWorkBucket(worker1.getOid(), 100, null, result);

	        // THEN
	        worker1 = taskManager.getTask(worker1.getOid(), result);
	        display("worker1 after reclaiming mis-allocated bucket", worker1);
	        coordinator = taskManager.getTask(coordinator.getOid(), result);
	        display("coordinator after reclaiming mis-allocated bucket", coordinator);

	        assertNumericBucket(bucket, null, 4, 3, 4);

	        buckets = new ArrayList<>(coordinator.getTaskType().getWorkState().getBucket());
	        assertEquals(2, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.DELEGATED, 4, 3, 4);
	        assertNumericBucket(buckets.get(1), WorkBucketStateType.COMPLETE, 5, 4, 5);

	        buckets = new ArrayList<>(worker1.getWorkState().getBucket());
	        assertEquals(1, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.READY, 4, 3, 4);

	        // WHEN
	        workStateManager.completeWorkBucket(worker1.getOid(), 4, result);

	        // THEN
	        worker1 = taskManager.getTask(worker1.getOid(), result);
	        display("worker1 after completion of 4th bucket", worker1);
	        coordinator = taskManager.getTask(coordinator.getOid(), result);
	        display("coordinator after completion of 4th bucket", coordinator);

	        buckets = new ArrayList<>(coordinator.getTaskType().getWorkState().getBucket());
	        assertEquals(1, buckets.size());
	        assertNumericBucket(buckets.get(0), WorkBucketStateType.COMPLETE, 5, 4, 5);

	        assertNoWorkBuckets(worker1.getTaskType().getWorkState());
        } finally {
	        suspendAndDeleteTasks(coordinatorTaskOid(TEST_NAME));
        }
	}

	@Test
	public void test200OneWorkerTask() throws Exception {
		final String TEST_NAME = "test200OneWorkerTask";
		OperationResult result = createResult(TEST_NAME, LOGGER);
		addObjectFromFile(coordinatorTaskFilename(TEST_NAME));
		addObjectFromFile(workerTaskFilename(TEST_NAME));

		TaskQuartzImpl worker = taskManager.getTask(workerTaskOid(TEST_NAME), result);

		try {
			// WHEN
			taskManager.resumeTask(worker, result);

			// THEN
			String coordinatorTaskOid = coordinatorTaskOid(TEST_NAME);
			waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);

			TaskQuartzImpl coordinatorAfter = taskManager.getTask(coordinatorTaskOid, result);
			TaskQuartzImpl workerAfter = taskManager.getTask(worker.getOid(), result);
			display("coordinator task after", coordinatorAfter);
			display("worker task after", workerAfter);

			assertTotalSuccessCount(30, singleton(workerAfter));
		} finally {
			suspendAndDeleteTasks(coordinatorTaskOid(TEST_NAME));
		}
	}

	@Test
	public void test210ThreeWorkersTask() throws Exception {
		final String TEST_NAME = "test210ThreeWorkersTask";
		OperationResult result = createResult(TEST_NAME, LOGGER);
		addObjectFromFile(coordinatorTaskFilename(TEST_NAME));
		addObjectFromFile(taskFilename(TEST_NAME, "1"));
		addObjectFromFile(taskFilename(TEST_NAME, "2"));
		addObjectFromFile(taskFilename(TEST_NAME, "3"));

		try {
			TaskQuartzImpl worker1 = taskManager.getTask(taskOid(TEST_NAME, "1"), result);
			TaskQuartzImpl worker2 = taskManager.getTask(taskOid(TEST_NAME, "2"), result);
			TaskQuartzImpl worker3 = taskManager.getTask(taskOid(TEST_NAME, "3"), result);

			workBucketsTaskHandler.setDelayProcessor(50);

			// WHEN
			taskManager.resumeTask(worker1, result);
			taskManager.resumeTask(worker2, result);
			taskManager.resumeTask(worker3, result);

			// THEN
			String coordinatorTaskOid = coordinatorTaskOid(TEST_NAME);
			waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);

			TaskQuartzImpl coordinatorAfter = taskManager.getTask(coordinatorTaskOid, result);
			worker1 = taskManager.getTask(worker1.getOid(), result);
			worker2 = taskManager.getTask(worker2.getOid(), result);
			worker3 = taskManager.getTask(worker3.getOid(), result);
			display("coordinator task after", coordinatorAfter);
			display("worker1 task after", worker1);
			display("worker2 task after", worker2);
			display("worker3 task after", worker3);
			display("worker1 op stats task after", PrismTestUtil.serializeAnyDataWrapped(worker1.getStoredOperationStats()));
			display("worker2 op stats task after", PrismTestUtil.serializeAnyDataWrapped(worker2.getStoredOperationStats()));
			display("worker3 op stats task after", PrismTestUtil.serializeAnyDataWrapped(worker3.getStoredOperationStats()));

			assertNumberOfBuckets(coordinatorAfter, 11);

			assertOptimizedCompletedBuckets(coordinatorAfter);

			assertTotalSuccessCount(107, Arrays.asList(worker1, worker2, worker3));

			// WHEN
			//taskManager.resumeTask();

			// TODO other asserts
		} finally {
			suspendAndDeleteTasks(coordinatorTaskOid(TEST_NAME));
		}
	}

	@Test
	public void test220WorkerSuspend() throws Exception {
		final String TEST_NAME = "test220WorkerSuspend";
		OperationResult result = createResult(TEST_NAME, LOGGER);
		addObjectFromFile(coordinatorTaskFilename(TEST_NAME));
		addObjectFromFile(taskFilename(TEST_NAME, "1"));
		addObjectFromFile(taskFilename(TEST_NAME, "2"));
		addObjectFromFile(taskFilename(TEST_NAME, "3"));
		try {
			TaskQuartzImpl worker1 = taskManager.getTask(taskOid(TEST_NAME, "1"), result);
			TaskQuartzImpl worker2 = taskManager.getTask(taskOid(TEST_NAME, "2"), result);
			TaskQuartzImpl worker3 = taskManager.getTask(taskOid(TEST_NAME, "3"), result);

			Holder<Task> suspensionVictim = new Holder<>();
			workBucketsTaskHandler.setProcessor((task, bucket, index) -> {
				if (index == 44) {
					task.storeOperationStats();         // to store operational stats for this task
					display("Going to suspend " + task);
					new Thread(() -> {
						taskManager.suspendTask(task, TaskManager.DO_NOT_WAIT, new OperationResult("suspend"));
						display("Suspended " + task);
						suspensionVictim.setValue(task);
					}).start();
					sleepChecked(20000);
				} else {
					sleepChecked(100);
				}
			});

			// WHEN
			TestUtil.displayWhen(TEST_NAME);
			taskManager.resumeTask(worker1, result);
			taskManager.resumeTask(worker2, result);
			taskManager.resumeTask(worker3, result);

			// THEN
			TestUtil.displayThen(TEST_NAME);
			String coordinatorTaskOid = coordinatorTaskOid(TEST_NAME);
			waitFor("waiting for all items to be processed", () -> getTotalItemsProcessed(coordinatorTaskOid) == 107 - 6,
					DEFAULT_TIMEOUT, 500);

			TaskQuartzImpl coordinatorAfter = taskManager.getTask(coordinatorTaskOid, result);
			worker1 = taskManager.getTask(worker1.getOid(), result);
			worker2 = taskManager.getTask(worker2.getOid(), result);
			worker3 = taskManager.getTask(worker3.getOid(), result);
			display("coordinator task after unfinished run", coordinatorAfter);
			display("worker1 task after unfinished run", worker1);
			display("worker2 task after unfinished run", worker2);
			display("worker3 task after unfinished run", worker3);
			display("worker1 op stats task after unfinished run",
					PrismTestUtil.serializeAnyDataWrapped(worker1.getStoredOperationStats()));
			display("worker2 op stats task after unfinished run",
					PrismTestUtil.serializeAnyDataWrapped(worker2.getStoredOperationStats()));
			display("worker3 op stats task after unfinished run",
					PrismTestUtil.serializeAnyDataWrapped(worker3.getStoredOperationStats()));

			assertTotalSuccessCount(107 - 6, Arrays.asList(worker1, worker2, worker3));

			assertOptimizedCompletedBuckets(coordinatorAfter);

			// TODO other asserts

			// WHEN
			TestUtil.displayWhen(TEST_NAME);

			workBucketsTaskHandler.setDelayProcessor(50);

			String oidToDelete = suspensionVictim.getValue().getOid();
			display("Deleting task " + oidToDelete);
			taskManager.deleteTask(oidToDelete, result);

			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("Waiting for coordinator task close");
			waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);

			coordinatorAfter = taskManager.getTask(coordinatorTaskOid, result);
			display("coordinator task after finished run", coordinatorAfter);

			assertOptimizedCompletedBuckets(coordinatorAfter);

			// TODO change after correct resuming
			// this does not work as processed items from deleted subtask are missing
			//assertTotalSuccessCount(107 - 6 + 10, coordinatorAfter.listSubtasks(result));
		} finally {
			suspendAndDeleteTasks(coordinatorTaskOid(TEST_NAME));
		}
	}

	@Test
	public void test230WorkerException() throws Exception {
		final String TEST_NAME = "test230WorkerException";
		OperationResult result = createResult(TEST_NAME, LOGGER);
		addObjectFromFile(coordinatorTaskFilename(TEST_NAME));
		addObjectFromFile(taskFilename(TEST_NAME, "1"));
		addObjectFromFile(taskFilename(TEST_NAME, "2"));
		addObjectFromFile(taskFilename(TEST_NAME, "3"));
		try {
			TaskQuartzImpl worker1 = taskManager.getTask(taskOid(TEST_NAME, "1"), result);
			TaskQuartzImpl worker2 = taskManager.getTask(taskOid(TEST_NAME, "2"), result);
			TaskQuartzImpl worker3 = taskManager.getTask(taskOid(TEST_NAME, "3"), result);

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
			TestUtil.displayWhen(TEST_NAME);
			taskManager.resumeTask(worker1, result);
			taskManager.resumeTask(worker2, result);
			taskManager.resumeTask(worker3, result);

			// THEN
			TestUtil.displayThen(TEST_NAME);
			String coordinatorTaskOid = coordinatorTaskOid(TEST_NAME);
			waitFor("waiting for all items to be processed", () -> getTotalItemsProcessed(coordinatorTaskOid) == 107 - 6,
					DEFAULT_TIMEOUT, 500);

			TaskQuartzImpl coordinatorAfter = taskManager.getTask(coordinatorTaskOid, result);
			worker1 = taskManager.getTask(worker1.getOid(), result);
			worker2 = taskManager.getTask(worker2.getOid(), result);
			worker3 = taskManager.getTask(worker3.getOid(), result);
			display("coordinator task after unfinished run", coordinatorAfter);
			display("worker1 task after unfinished run", worker1);
			display("worker2 task after unfinished run", worker2);
			display("worker3 task after unfinished run", worker3);
			display("worker1 op stats task after unfinished run",
					PrismTestUtil.serializeAnyDataWrapped(worker1.getStoredOperationStats()));
			display("worker2 op stats task after unfinished run",
					PrismTestUtil.serializeAnyDataWrapped(worker2.getStoredOperationStats()));
			display("worker3 op stats task after unfinished run",
					PrismTestUtil.serializeAnyDataWrapped(worker3.getStoredOperationStats()));

			assertTotalSuccessCount(107 - 6, Arrays.asList(worker1, worker2, worker3));

			assertOptimizedCompletedBuckets(coordinatorAfter);

			// TODO other asserts

			// WHEN
			TestUtil.displayWhen(TEST_NAME);

			workBucketsTaskHandler.setDelayProcessor(50);

			String oidToClose = exceptionVictim.getValue().getOid();
			display("Closing task " + oidToClose);
			taskManager.closeTask(taskManager.getTask(oidToClose, result), result);

			// THEN
			TestUtil.displayThen(TEST_NAME);
			display("Waiting for coordinator task close");
			waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);

			coordinatorAfter = taskManager.getTask(coordinatorTaskOid, result);
			worker1 = taskManager.getTask(worker1.getOid(), result);
			worker2 = taskManager.getTask(worker2.getOid(), result);
			worker3 = taskManager.getTask(worker3.getOid(), result);
			display("coordinator task after", coordinatorAfter);
			display("worker1 task after", worker1);
			display("worker2 task after", worker2);
			display("worker3 task after", worker3);
			display("worker1 op stats task after", PrismTestUtil.serializeAnyDataWrapped(worker1.getStoredOperationStats()));
			display("worker2 op stats task after", PrismTestUtil.serializeAnyDataWrapped(worker2.getStoredOperationStats()));
			display("worker3 op stats task after", PrismTestUtil.serializeAnyDataWrapped(worker3.getStoredOperationStats()));

			// TODO change after correct resuming
			assertTotalSuccessCount(107 - 6 + 10, coordinatorAfter.listSubtasks(result));

			assertOptimizedCompletedBuckets(coordinatorAfter);
		} finally {
			suspendAndDeleteTasks(coordinatorTaskOid(TEST_NAME));
		}
	}

	@Test
	public void test300NarrowQueryOneWorkerTask() throws Exception {
		final String TEST_NAME = "test300NarrowQueryOneWorkerTask";
		OperationResult result = createResult(TEST_NAME, LOGGER);
		addObjectFromFile(coordinatorTaskFilename(TEST_NAME));
		addObjectFromFile(workerTaskFilename(TEST_NAME));

		workBucketsTaskHandler.resetBeforeTest();
		workBucketsTaskHandler.setDefaultQuery(new ObjectQuery());

		try {

			TaskQuartzImpl worker = taskManager.getTask(workerTaskOid(TEST_NAME), result);

			// WHEN
			taskManager.resumeTask(worker, result);

			// THEN
			String coordinatorTaskOid = coordinatorTaskOid(TEST_NAME);
			waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, 200);

			TaskQuartzImpl coordinatorAfter = taskManager.getTask(coordinatorTaskOid, result);
			TaskQuartzImpl workerAfter = taskManager.getTask(worker.getOid(), result);
			display("coordinator task after", coordinatorAfter);
			display("worker task after", workerAfter);

			assertTotalSuccessCount(30, singleton(workerAfter));

			List<ObjectQuery> qe = workBucketsTaskHandler.getQueriesExecuted();
			display("Queries executed", qe);
			assertEquals("Wrong # of queries", 3, qe.size());
			ObjectQuery q1 = QueryBuilder.queryFor(UserType.class, prismContext)
					.item(UserType.F_ITERATION).ge(BigInteger.valueOf(0))
					.and().item(UserType.F_ITERATION).lt(BigInteger.valueOf(10))
					.build();
			ObjectQuery q2 = QueryBuilder.queryFor(UserType.class, prismContext)
					.item(UserType.F_ITERATION).ge(BigInteger.valueOf(10))
					.and().item(UserType.F_ITERATION).lt(BigInteger.valueOf(20))
					.build();
			ObjectQuery q3 = QueryBuilder.queryFor(UserType.class, prismContext)
					.item(UserType.F_ITERATION).ge(BigInteger.valueOf(20))
					.and().item(UserType.F_ITERATION).lt(BigInteger.valueOf(30))
					.build();
			PrismAsserts.assertQueriesEquivalent("Wrong query #1", q1, qe.get(0));
			PrismAsserts.assertQueriesEquivalent("Wrong query #2", q2, qe.get(1));
			PrismAsserts.assertQueriesEquivalent("Wrong query #3", q3, qe.get(2));
		} finally {
			suspendAndDeleteTasks(coordinatorTaskOid(TEST_NAME));
		}
	}
}
