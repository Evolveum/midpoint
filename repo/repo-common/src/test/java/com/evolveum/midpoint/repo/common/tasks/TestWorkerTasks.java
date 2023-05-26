/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.repo.common.AbstractRepoCommonTest;
import com.evolveum.midpoint.repo.common.activity.run.buckets.BucketingConfigurationOverrides;
import com.evolveum.midpoint.test.asserter.TaskAsserter;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests creation and management of worker tasks, including workers reconciliation.
 */
public class TestWorkerTasks extends AbstractRepoCommonTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/worker-tasks");

    private static final TestResource<TaskType> TASK_100_SINGLE_WORKER = new TestResource<>(TEST_DIR, "task-100-single-worker.xml", "4e09a632-f2c7-4285-9204-c02e7c39ae04");
    private static final TestResource<TaskType> TASK_110_FOUR_WORKERS = new TestResource<>(TEST_DIR, "task-110-four-workers.xml", "33b0f9bb-15bd-4f64-bd08-11aad034e77f");
    private static final TestResource<TaskType> TASK_120_TWO_WORKERS_PER_NODE = new TestResource<>(TEST_DIR, "task-120-two-workers-per-node.xml", "76d0c81d-759d-4e71-b2d7-04443032a0e4");
    private static final TestResource<TaskType> TASK_130_NO_OP_BUCKETING_SANITY = new TestResource<>(TEST_DIR, "task-130-no-op-bucketing-sanity.xml", "9e8972ad-8c86-42ca-8690-d5959c3a64a8");
    private static final TestResource<TaskType> TASK_140_WORKERS_UPDATE = new TestResource<>(TEST_DIR, "task-140-workers-update.xml", "8ef8e606-3c3e-45c7-bca7-e64eb47de1e4");
    private static final TestResource<TaskType> TASK_150_WORKERS_MOVE = new TestResource<>(TEST_DIR, "task-150-workers-move.xml", "f3efb438-c573-4631-bbff-ba9e09b3ae03");
    private static final TestResource<TaskType> TASK_160_WORKERS_ADD_DELETE = new TestResource<>(TEST_DIR, "task-160-workers-add-delete.xml", "9e94e921-d319-422a-b9d6-9e98d9034975");
    private static final TestResource<TaskType> TASK_170_NUMBER_SEGMENTATION_NUMBER_OF_BUCKETS = new TestResource<>(TEST_DIR, "task-170-num-seg-num-of-buckets.xml", "33b0f9bb-15bd-4f64-bd08-11aad034e77e");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final long DEFAULT_SLEEP_INTERVAL = 250L;
    private static final long DEFAULT_TIMEOUT = 30000L;

    private static final int ROLES = 100;
    private static final String ROLE_NAME_PATTERN = "test-role-%02d";

    private static final String DN = "DefaultNode";
    private static final String NA = "NodeA";
    private static final String NB = "NodeB";

    private final List<RoleType> allRoles = new ArrayList<>();

    @Autowired private CacheConfigurationManager cacheConfigurationManager;

    @Autowired private ClusterManager clusterManager;

    @PostConstruct
    public void initialize() throws Exception {
        OperationResult result = new OperationResult("initialize");

        BucketingConfigurationOverrides.setFreeBucketWaitIntervalOverride(1000L);
        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

        // Needed for caching profiles
        cacheConfigurationManager.applyCachingConfiguration(
                (SystemConfigurationType) prismContext.parseObject(SYSTEM_CONFIGURATION_FILE).asObjectable());

        allRoles.addAll(
                repoObjectCreatorFor(RoleType.class)
                        .withObjectCount(ROLES)
                        .withNamePattern(ROLE_NAME_PATTERN)
                        .withCustomizer(this::setDiscriminator)
                        .execute(result));

        clusterManager.startClusterManagerThread();
    }

    /**
     * Checks the creation of a single worker task. The worker processes roles (in 4 buckets).
     *
     * The second run should start "from scratch", initializing statistics and everything else.
     * Currently the worker task is deleted and re-created.
     */
    @Test
    public void test100SingleWorker() throws Exception {
        given();
        OperationResult result = createOperationResult();

        mockRecorder.reset();

        // Although we have a lot of roles, buckets for this task cover only first 4 ones.
        List<RoleType> roles = allRoles.subList(0, 4);

        when();
        Task root = taskAdd(TASK_100_SINGLE_WORKER, result);

        then();
        try {
            waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, DEFAULT_TIMEOUT);

            root.refresh(result);
            String workerOid1 = assertTaskTreeAfter100("after 1st run", root, result);
            assertExecutions(roles, 1);

            when("second run");
            taskManager.scheduleTasksNow(List.of(root.getOid()), result);

            then("second run");
            waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, DEFAULT_TIMEOUT);

            root.refresh(result);
            String workerOid2 = assertTaskTreeAfter100("after 2nd run", root, result);
            assertExecutions(roles, 2);
            assertWorkersDiffer(workerOid1, workerOid2);

        } finally {
            suspendAndDeleteTasks(root.getOid());
        }
    }

    private void assertWorkersDiffer(String workerOid1, String workerOid2) {
        assertThat(workerOid2)
                .withFailMessage("Worker OID was not changed between runs: %s", workerOid1)
                .isNotEqualTo(workerOid1);
    }

    /** @return Worker task OID */
    private String assertTaskTreeAfter100(String message, Task root, OperationResult result) throws SchemaException {
        // @formatter:off
        return assertTask(root, message)
                .display()
                .assertClosed()
                .assertSuccess()
                .loadSubtasksDeeply(result)
                .progressInformation() // this is for the whole tree
                    .display()
                    .assertBuckets(4, 4)
                    .assertItems(4, null) // null because of workers
                .end()
                .assertCachingProfiles("profile1")
                .assertSubtasks(1)
                .subtask("Worker DefaultNode:1 for root activity in task-100")
                    .display()
                    .assertClosed()
                    .assertSuccess()
                    .assertCachingProfiles("profile1")
                    .rootItemProcessingInformation()
                        .display()
                        .assertTotalCounts(4, 0, 0)
                    .end()
                    .getOid();
        // @formatter:on
    }

    /**
     * Checks the creation of four worker tasks. The workers process roles (in 20 buckets).
     */
    @Test
    public void test110FourWorkers() throws Exception {
        given();
        OperationResult result = createOperationResult();

        mockRecorder.reset();

        // Although we have a lot of roles, buckets for this task cover only first 80 ones.
        List<RoleType> roles = allRoles.subList(0, 80);

        when();
        Task root = taskAdd(TASK_110_FOUR_WORKERS, result);

        then();
        try {
            waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, DEFAULT_TIMEOUT);

            root.refresh(result);
            assertTaskTreeAfter110("after 1st run", root, result);
            assertExecutions(roles, 1);

            when("second run");
            taskManager.scheduleTasksNow(List.of(root.getOid()), result);

            then("second run");
            waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, DEFAULT_TIMEOUT);

            root.refresh(result);
            assertTaskTreeAfter110("after 2nd run", root, result);
            assertExecutions(roles, 2);

        } finally {
            suspendAndDeleteTasks(root.getOid());
        }
    }

    private void assertTaskTreeAfter110(String message, Task root, OperationResult result) throws SchemaException {
        // @formatter:off
        assertTask(root, message)
                .display()
                .assertClosed()
                .assertSuccess()
                .loadSubtasksDeeply(result)
                .progressInformation() // this is for the whole tree
                    .display()
                    .assertBuckets(20, 20)
                    .assertItems(80, null)
                .end()
                .assertSubtasks(4)
                .subtask("Worker DefaultNode:1 for root activity in task-110")
                    .assertClosed()
                    .assertSuccess()
                    .rootItemProcessingInformation()
                        .display()
                    .end()
                .end()
                .subtask("Worker DefaultNode:2 for root activity in task-110")
                    .assertClosed()
                    .assertSuccess()
                    .rootItemProcessingInformation()
                        .display()
                    .end()
                .end()
                .subtask("Worker DefaultNode:3 for root activity in task-110")
                    .assertClosed()
                    .assertSuccess()
                    .rootItemProcessingInformation()
                        .display()
                    .end()
                .end()
                .subtask("Worker DefaultNode:4 for root activity in task-110")
                    .assertClosed()
                    .assertSuccess()
                    .rootItemProcessingInformation()
                        .display()
                    .end()
                .end();
        // @formatter:on
    }

    /**
     * Checks the creation of two worker tasks on each of two cluster nodes.
     */
    @Test
    public void test120TwoNodesWithTwoWorkersEach() throws Exception {
        given();
        OperationResult result = createOperationResult();

        assumeExtraClusterNodes(List.of("NodeA"), result);

        mockRecorder.reset();

        // Although we have a lot of roles, buckets for this task cover only first 80 ones.
        List<RoleType> roles = allRoles.subList(0, 80);

        when();
        Task root = taskAdd(TASK_120_TWO_WORKERS_PER_NODE, result);

        then();
        try {
            waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, DEFAULT_TIMEOUT);

            root.refresh(result);
            assertTaskTreeAfter120("after 1st run", root, result);
            assertExecutions(roles, 1);

            when("second run");
            taskManager.scheduleTasksNow(List.of(root.getOid()), result);

            then("second run");
            waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, DEFAULT_TIMEOUT);

            root.refresh(result);
            assertTaskTreeAfter120("after 2nd run", root, result);
            assertExecutions(roles, 2);

        } finally {
            suspendAndDeleteTasks(root.getOid());
        }
    }

    private void assertTaskTreeAfter120(String message, Task root, OperationResult result) throws SchemaException {
        // @formatter:off
        assertTask(root, message)
                .display()
                .assertClosed()
                .assertSuccess()
                .loadSubtasksDeeply(result)
                .progressInformation() // this is for the whole tree
                    .display()
                    .assertBuckets(20, 20)
                    .assertItems(80, null)
                .end()
                .assertSubtasks(4)
                .subtask("Worker DefaultNode:1 for root activity in task-120")
                    .assertClosed()
                    .assertSuccess()
                    .assertExecutionGroup("DefaultNode")
                    .rootItemProcessingInformation()
                        .display()
                    .end()
                .end()
                .subtask("Worker DefaultNode:2 for root activity in task-120")
                    .assertClosed()
                    .assertSuccess()
                    .assertExecutionGroup("DefaultNode")
                    .rootItemProcessingInformation()
                        .display()
                    .end()
                .end()
                .subtask("Worker NodeA:1 for root activity in task-120")
                    .assertClosed()
                    .assertSuccess()
                    .assertExecutionGroup("NodeA")
                    .rootItemProcessingInformation()
                        .display()
                    .end()
                .end()
                .subtask("Worker NodeA:2 for root activity in task-120")
                    .assertClosed()
                    .assertSuccess()
                    .assertExecutionGroup("NodeA")
                    .rootItemProcessingInformation()
                        .display()
                    .end()
                .end();
        // @formatter:on
    }

    /**
     * Checks if NoOp bucketing works well.
     *
     * (Or should we move this test to {@link TestActivities} or {@link TestBucketingLive}?)
     */
    @Test
    public void test130NoOpBucketingSanity() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        Task task1 = taskAdd(TASK_130_NO_OP_BUCKETING_SANITY, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000);

        then();

        task1.refresh(result);
        assertNoOpTaskAfter130(task1);
    }

    private void assertNoOpTaskAfter130(Task task1) {
        // @formatter:off
        assertTask(task1, "after")
                .display()
                .assertClosed()
                .assertSuccess()
                .assertProgress(1000)
                .progressInformation()
                    .display()
                    .assertComplete()
                    .assertBuckets(20, 20)
                    .assertItems(1000, 1000)
                .end();
        // @formatter:on
    }

    /**
     * Checks if worker tasks are correctly updated by the reconciliation.
     *
     * Scenario:
     *
     * 1. four worker tasks are created and started
     * 2. some workers are broken: #2 is renamed, #3 is renamed + made scavenger, #4 is made scavenger
     * 3. reconciliation is started, fixing the things
     */
    @Test
    public void test140UpdateWorkerTasks() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        assumeNoExtraClusterNodes(result);

        when("starting worker tasks");

        Task root = taskAdd(TASK_140_WORKERS_UPDATE, result);

        try {
            waitForChildrenBeRunning(root, 4, result);

            then("starting worker tasks");

            String w1correct = "Worker DefaultNode:1 for root activity in task-140";
            String w2correct = "Worker DefaultNode:2 for root activity in task-140";
            String w3correct = "Worker DefaultNode:3 for root activity in task-140";
            String w4correct = "Worker DefaultNode:4 for root activity in task-140";
            assertFourWorkers(root, "after starting worker", w1correct, w2correct, w3correct, w4correct,
                    true, false, false, false, DN, DN, DN, DN, 4, result);

            // ---------------------------------------------------------------------------------------- rename worker explicitly
            when("change workers explicitly");

            List<? extends Task> subtasks = root.listSubtasks(result);
            assertThat(subtasks).as("subtasks").hasSize(4);
            Task w2 = findTaskByName(subtasks, w2correct);
            Task w3 = findTaskByName(subtasks, w3correct);
            Task w4 = findTaskByName(subtasks, w4correct);

            String w2broken = "Broken 2";
            String w3broken = "Broken 3";

            w2.setName(w2broken);
            w2.flushPendingModifications(result);

            w3.setName(w3broken);
            setScavenger(w3, true);
            w3.flushPendingModifications(result);

            setScavenger(w4, true);
            w4.flushPendingModifications(result);

            then("change workers explicitly");
            assertFourWorkers(root, "after changing workers", w1correct, w2broken, w3broken, w4correct,
                    true, false, true, true,  DN, DN, DN, DN, 4, result);

            // ---------------------------------------------------------------------------------------- reconcile workers
            when("reconcile workers");
            Map<ActivityPath, WorkersReconciliationResultType> resultMap =
                    activityManager.reconcileWorkers(root.getOid(), result);

            then("reconcile workers");
            assertFourWorkers(root, "after reconciliation", w1correct, w2correct, w3correct, w4correct,
                    true, false, false, false,  DN, DN, DN, DN, 4, result);
            assertReconResult("after reconciliation", resultMap,
                    1, 1, 2, 0, 0, 0);

        } finally {
            taskManager.suspendTaskTree(root.getOid(), TaskManager.WAIT_INDEFINITELY, result);
            taskManager.deleteTaskTree(root.getOid(), result);
        }
    }

    /**
     * Checks if worker tasks are correctly treated when cluster node is changed (brought down + another node starts).
     *
     * Scenario:
     *
     * 1. Cluster has DefaultNode + NodeA. Then two worker tasks are created and started on each node.
     * 2. NodeA is removed from cluster and NodeB is added.
     * 3. Workers reconciliation is started, suspending tasks on NodeA and starting two worker tasks to NodeB.
     */
    @Test
    public void test150ChangeClusterNode() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        assumeExtraClusterNodes(List.of("NodeA"), result);

        when("starting worker tasks");

        Task root = taskAdd(TASK_150_WORKERS_MOVE, result);

        try {
            waitForChildrenBeRunning(root, 4, result);

            then("starting worker tasks");

            String w1correct = "Worker DefaultNode:1 for root activity in task-150";
            String w2correct = "Worker DefaultNode:2 for root activity in task-150";
            String w3nodeA = "Worker NodeA:1 for root activity in task-150";
            String w4nodeA = "Worker NodeA:2 for root activity in task-150";
            String w3nodeB = "Worker NodeB:1 for root activity in task-150";
            String w4nodeB = "Worker NodeB:2 for root activity in task-150";
            assertFourWorkers(root, "after starting workers", w1correct, w2correct, w3nodeA, w4nodeA,
                    true, false, true, false, DN, DN, NA, NA, 4, result);

            // ---------------------------------------------------------------------------------------- reconfigure cluster
            when("reconfigure cluster");

            assumeExtraClusterNodes(List.of("NodeB"), result);

            // ---------------------------------------------------------------------------------------- reconcile workers
            when("reconcile workers");
            Map<ActivityPath, WorkersReconciliationResultType> resultMap =
                    activityManager.reconcileWorkers(root.getOid(), result);

            then("reconcile workers");

            waitForChildrenBeRunning(root, 4, result);
            then("children are running");
            var asserter= assertFourWorkers(root, "after reconciliation",
                    w1correct, w2correct, w3nodeB, w4nodeB,
                    true, false, true, false,
                    DN, DN, NB, NB, 6, result);
            then("four workers present, two suspended");
            assertTwoWorkersSuspended(asserter, w3nodeA, w4nodeA, true, false, NA, NA);
            then("reconciliation");
            assertReconResult("after reconciliation", resultMap,
                    2, 0, 0, 2, 2, 0);

        } finally {
            taskManager.suspendTaskTree(root.getOid(), TaskManager.WAIT_INDEFINITELY, result);
            taskManager.deleteTaskTree(root.getOid(), result);
        }
    }

    /**
     * Checks if worker tasks are correctly created, suspended, and re-started by the reconciliation.
     *
     * Scenario:
     *
     * 1. Cluster has DefaultNode. Then two worker tasks are created and started on that node.
     * 2. NodeA is added.
     * 3. Workers reconciliation is started, adding two worker tasks for NodeA.
     * 4. NodeA is removed.
     * 5. Workers reconciliation is started, suspending two worker tasks for NodeA.
     * 6. NodeA is re-added.
     * 7. Workers reconciliation is started, starting two worker tasks for NodeA again.
     */
    @Test
    public void test160AddAndDeleteWorkerTasks() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        assumeNoExtraClusterNodes(result);

        when("starting worker tasks");

        Task root = taskAdd(TASK_160_WORKERS_ADD_DELETE, result);

        try {
            waitForChildrenBeRunning(root, 2, result);

            then("starting worker tasks");

            String w1default = "Worker DefaultNode:1 for root activity in task-160";
            String w2default = "Worker DefaultNode:2 for root activity in task-160";
            assertTwoWorkers(root, "after starting workers", w1default, w2default,
                    true, false, DN, DN, 2, result);

            // ---------------------------------------------------------------------------------------- add node A
            when("add node A");

            assumeExtraClusterNodes(List.of("NodeA"), result);

            // ---------------------------------------------------------------------------------------- reconcile workers
            when("reconcile workers");
            Map<ActivityPath, WorkersReconciliationResultType> resultMap1 =
                    activityManager.reconcileWorkers(root.getOid(), result);

            then("reconcile workers");

            waitForChildrenBeRunning(root, 4, result);

            String w3nodeA = "Worker NodeA:1 for root activity in task-160";
            String w4nodeA = "Worker NodeA:2 for root activity in task-160";

            assertFourWorkers(root, "after reconciliation", w1default, w2default, w3nodeA, w4nodeA,
                    true, false, true, false, DN, DN, NA, NA, 4, result);
            assertReconResult("after reconciliation 1", resultMap1,
                    2, 0, 0, 0, 2, 0);

            // ---------------------------------------------------------------------------------------- delete node A
            when("delete node A");

            assumeNoExtraClusterNodes(result);

            // ---------------------------------------------------------------------------------------- reconcile workers 2
            when("reconcile workers 2");
            Map<ActivityPath, WorkersReconciliationResultType> resultMap2 =
                    activityManager.reconcileWorkers(root.getOid(), result);

            then("reconcile workers 2");

            var asserter = assertTwoWorkers(root, "after reconciliation 2", w1default, w2default,
                    true, false, DN, DN, 4, result);
            assertTwoWorkersSuspended(asserter, w3nodeA, w4nodeA, true, false, NA, NA);

            assertReconResult("after reconciliation 2", resultMap2,
                    2, 0, 0, 2, 0, 0);

            // ---------------------------------------------------------------------------------------- re-add node A
            when("re-add node A");

            assumeExtraClusterNodes(List.of("NodeA"), result);

            // ---------------------------------------------------------------------------------------- reconcile workers 3
            when("reconcile workers 3");
            Map<ActivityPath, WorkersReconciliationResultType> resultMap3 =
                    activityManager.reconcileWorkers(root.getOid(), result);

            then("reconcile workers 3");

            waitForChildrenBeRunning(root, 4, result);

            assertFourWorkers(root, "after reconciliation", w1default, w2default, w3nodeA, w4nodeA,
                    true, false, true, false, DN, DN, NA, NA, 4, result);
            assertReconResult("after reconciliation 3", resultMap3,
                    4, 0, 0, 0, 0, 2);

        } finally {
            taskManager.suspendTaskTree(root.getOid(), TaskManager.WAIT_INDEFINITELY, result);
            taskManager.deleteTaskTree(root.getOid(), result);
        }
    }

    private void assertReconResult(String message, Map<ActivityPath, WorkersReconciliationResultType> resultMap,
            int matched, int renamed, int adapted, int suspended, int created, int resumed) {

        displayValue("recon result map " + message, DebugUtil.debugDump(resultMap, 1));

        assertThat(resultMap).as("reconciliation result map " + message).hasSize(1);
        WorkersReconciliationResultType result = resultMap.get(ActivityPath.empty());
        assertThat(result).as("recon result for root activity").isNotNull();
        assertThat(result.getMatched()).as("matched").isEqualTo(matched);
        assertThat(result.getRenamed()).as("renamed").isEqualTo(renamed);
        assertThat(result.getAdapted()).as("adapted").isEqualTo(adapted);
        assertThat(result.getSuspended()).as("suspended").isEqualTo(suspended);
        assertThat(result.getCreated()).as("created").isEqualTo(created);
        assertThat(result.getResumed()).as("resumed").isEqualTo(resumed);
        assertThat(result.getStatus()).as("status").isEqualTo(OperationResultStatusType.SUCCESS);
    }

    @SuppressWarnings("SameParameterValue")
    private TaskAsserter<Void> assertTwoWorkers(Task root, String message, String name1, String name2,
            boolean sc1, boolean sc2, String g1, String g2, int allWorkers, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        root.refresh(result);
        // @formatter:off
        return assertTask(root, message)
                .display()
                .assertExecutionState(TaskExecutionStateType.RUNNING)
                .assertSchedulingState(TaskSchedulingStateType.WAITING)
                .loadImmediateSubtasks(result)
                .assertSubtasks(allWorkers)
                .subtask(name1)
                    .display()
                    .assertExecutionState(TaskExecutionStateType.RUNNING)
                    .assertSchedulingState(TaskSchedulingStateType.READY)
                    .rootActivityState()
                        .assertScavenger(sc1)
                    .end()
                    .assertExecutionGroup(g1)
                .end()
                .subtask(name2)
                    .display()
                    .assertExecutionState(TaskExecutionStateType.RUNNING)
                    .assertSchedulingState(TaskSchedulingStateType.READY)
                    .rootActivityState()
                        .assertScavenger(sc2)
                    .end()
                    .assertExecutionGroup(g2)
                .end();
        // @formatter:on
    }

    @SuppressWarnings("SameParameterValue")
    private void assertTwoWorkersSuspended(TaskAsserter<Void> asserter, String name1, String name2,
            boolean sc1, boolean sc2, String g1, String g2) {
        // @formatter:off
        asserter
                .subtask(name1)
                    .display()
                    .assertExecutionState(TaskExecutionStateType.SUSPENDED)
                    .assertSchedulingState(TaskSchedulingStateType.SUSPENDED)
                    .rootActivityState()
                        .assertScavenger(sc1)
                        .assertNoReadyBuckets()
                    .end()
                    .assertExecutionGroup(g1)
                .end()
                .subtask(name2)
                    .display()
                    .assertExecutionState(TaskExecutionStateType.SUSPENDED)
                    .assertSchedulingState(TaskSchedulingStateType.SUSPENDED)
                    .rootActivityState()
                        .assertScavenger(sc2)
                        .assertNoReadyBuckets()
                    .end()
                    .assertExecutionGroup(g2)
                .end();
        // @formatter:on
    }

    @SuppressWarnings("SameParameterValue")
    private TaskAsserter<Void> assertFourWorkers(Task root, String message, String name1, String name2, String name3, String name4,
            boolean sc1, boolean sc2, boolean sc3, boolean sc4,
            String g1, String g2, String g3, String g4, int allWorkers, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        root.refresh(result);
        // @formatter:off
        return assertTask(root, message)
                .display()
                .assertExecutionState(TaskExecutionStateType.RUNNING)
                .assertSchedulingState(TaskSchedulingStateType.WAITING)
                .loadImmediateSubtasks(result)
                .assertSubtasks(allWorkers)
                .subtask(name1)
                    .display()
                    .assertExecutionState(TaskExecutionStateType.RUNNING)
                    .assertSchedulingState(TaskSchedulingStateType.READY)
                    .rootActivityState()
                        .assertScavenger(sc1)
                    .end()
                    .assertExecutionGroup(g1)
                .end()
                .subtask(name2)
                    .display()
                    .assertExecutionState(TaskExecutionStateType.RUNNING)
                    .assertSchedulingState(TaskSchedulingStateType.READY)
                    .rootActivityState()
                        .assertScavenger(sc2)
                    .end()
                    .assertExecutionGroup(g2)
                .end()
                .subtask(name3)
                    .display()
                    .assertExecutionState(TaskExecutionStateType.RUNNING)
                    .assertSchedulingState(TaskSchedulingStateType.READY)
                    .rootActivityState()
                        .assertScavenger(sc3)
                    .end()
                    .assertExecutionGroup(g3)
                .end()
                .subtask(name4)
                    .display()
                    .assertExecutionState(TaskExecutionStateType.RUNNING)
                    .assertSchedulingState(TaskSchedulingStateType.READY)
                    .rootActivityState()
                        .assertScavenger(sc4)
                    .end()
                    .assertExecutionGroup(g4)
                .end();
        // @formatter:on
    }

    @SuppressWarnings("SameParameterValue")
    private void setScavenger(Task task, boolean value) throws SchemaException {
        task.modify(prismContext.deltaFor(TaskType.class)
                .item(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY,
                        ActivityStateType.F_BUCKETING, ActivityBucketingStateType.F_SCAVENGER).replace(value)
                .asItemDeltas());
    }

    /**
     * Checks the generation number of buckets and real number of buckets. Exists definition for numberOfBuckets + from + to.
     */
    @Test
    public void test170NumberSegmentationNumberOfBuckets() throws Exception {
        given();
        OperationResult result = createOperationResult();

        mockRecorder.reset();

        assumeNoExtraClusterNodes(result);

        // Although we have a lot of roles, buckets for this task cover only from 5 to 100.
        List<RoleType> roles = allRoles.subList(5, 100);

        when();
        Task root = taskAdd(TASK_170_NUMBER_SEGMENTATION_NUMBER_OF_BUCKETS, result);

        then();
        try {
            waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, DEFAULT_TIMEOUT);

            root.refresh(result);
            assertTaskTreeAfter170(root, result);
            assertExecutions(roles, 1);
        } finally {
            suspendAndDeleteTasks(root.getOid());
        }
    }

    private void assertTaskTreeAfter170(Task root, OperationResult result) throws SchemaException {
        // @formatter:off
        assertTask(root, "after run")
                .display()
                .assertClosed()
                .assertSuccess()
                .loadSubtasksDeeply(result)
                .progressInformation() // this is for the whole tree
                    .display()
                    .assertBuckets(10, 10)
                    .assertItems(95, null)
                .end()
                .assertSubtasks(2)
                .subtask("Worker DefaultNode:1 for root activity in task-170")
                    .assertClosed()
                    .assertSuccess()
                    .rootItemProcessingInformation()
                        .display()
                    .end()
                .end()
                .subtask("Worker DefaultNode:2 for root activity in task-170")
                    .assertClosed()
                    .assertSuccess()
                    .rootItemProcessingInformation()
                        .display()
                    .end()
                .end();
        // @formatter:on
    }
}
