/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.tasks;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.test.asserter.predicates.AssertionPredicate;

import com.evolveum.midpoint.test.asserter.predicates.ExceptionBasedAssertionPredicate;

import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterStateType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import javax.xml.datatype.XMLGregorianCalendar;

import static com.evolveum.midpoint.test.asserter.predicates.TimeAssertionPredicates.timeBetween;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityAutoScalingWorkStateType.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskSchedulingStateType.READY;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the auto-scaling task functionality.
 *
 * We don't need to check details of workers reconciliation process. That is treated in `TestWorkerTasks` in `repo-common`.
 *
 * We just want to test the mechanics of auto-scaling activity - whether it
 *
 * 1. correctly detects cluster state changes,
 * 2. respects min/max intervals,
 * 3. respects `skipInitialReconciliation` parameter.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAutoScalingTask extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/auto-scaling");

    private static final TestObject<TaskType> TASK_AUTO_SCALING = TestObject.file(TEST_DIR, "task-auto-scaling.xml", "f7b42763-2553-469e-8bb9-df44498d4767");
    private static final TestObject<TaskType> TASK_AUTO_SCALING_SKIP_INITIAL = TestObject.file(TEST_DIR, "task-auto-scaling-skip-initial.xml", "8eda5dcd-2394-4ac2-a936-c32231ce6f22");

    private static final TestObject<TaskType> TASK_TWO_WORKERS_PER_NODE = TestObject.file(TEST_DIR, "task-two-workers-per-node.xml", "8e8b1c24-f512-467a-9c09-f1bc899b56bd");

    private static final TestObject<TaskType> TASK_NO_WORKERS = TestObject.file(TEST_DIR, "task-no-workers.xml", "4a56bee1-57e5-4575-9b05-0fb1ad6ad73e");
    private static final TestObject<TaskType> TASK_DISABLED_AUTO_SCALING = TestObject.file(TEST_DIR, "task-disabled-auto-scaling.xml", "e2d2012d-7940-48bb-9c27-dd26d20b68bc");

    private static final int TIMEOUT = 10000;

    private static final String DEFAULT_NODE = "DefaultNode";
    private static final String NODE1 = "Node1";

    private XMLGregorianCalendar lastReconciliationTimestamp;
    private XMLGregorianCalendar lastClusterStateChangeTimestamp;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Test
    public void test000StartWorkingTasks() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        addTask(TASK_TWO_WORKERS_PER_NODE, result);
        waitForChildrenBeRunning(TASK_TWO_WORKERS_PER_NODE.oid, 2, result);

        addTask(TASK_NO_WORKERS, result);
        addTask(TASK_DISABLED_AUTO_SCALING, result);
    }

    /**
     * After starting the auto scaling task the first time, the reconciliation is run.
     */
    @Test
    public void test100InitialAutoScaling() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        addTask(TASK_AUTO_SCALING, result);

        when();

        waitForTaskCloseOrSuspend(TASK_AUTO_SCALING.oid, TIMEOUT);

        then();

        long after = System.currentTimeMillis();

        // @formatter:off
        assertTask(TASK_AUTO_SCALING.oid, "after")
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertSuccessCount(1)
                        .assertLastSuccessObjectOid(TASK_TWO_WORKERS_PER_NODE.oid)
                    .end()
                    .assertPersistencePerpetual()
                    .workState()
                        .assertItemValueSatisfies(F_LAST_RECONCILIATION_TIMESTAMP, timeBetween(0, after))
                        .sendItemValue(F_LAST_RECONCILIATION_TIMESTAMP, this::setLastReconciliationTimestamp)
                        .assertItemValueSatisfies(F_LAST_CLUSTER_STATE_CHANGE_TIMESTAMP, timeBetween(0, after))
                        .sendItemValue(F_LAST_CLUSTER_STATE_CHANGE_TIMESTAMP, this::setLastClusterStateChangeTimestamp)
                        .assertItemValueSatisfies(F_LAST_CLUSTER_STATE, nodes(DEFAULT_NODE))
                        .assertPropertyEquals(F_RECONCILIATION_PENDING, false)
                    .end();
        // @formatter:on

        assertWorkerTaskTwoSubtasks();
    }

    private AssertionPredicate<ClusterStateType> nodes(String... identifiers) {
        return new ExceptionBasedAssertionPredicate<>(value -> {
            assertThat(value).as("cluster state").isNotNull();
            assertThat(value.getNodeUp()).as("nodes").containsExactlyInAnyOrder(identifiers);
        });
    }

    /**
     * Next run comes right after the previous one. No cluster change, no reconciliation.
     */
    @Test
    public void test110NextRunNoChange() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        restartTask(TASK_AUTO_SCALING.oid, result);
        waitForTaskCloseOrSuspend(TASK_AUTO_SCALING.oid, TIMEOUT);

        then();

        // @formatter:off
        assertTask(TASK_AUTO_SCALING.oid, "after")
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertSuccessCount(1) // should not increase
                        .assertLastSuccessObjectOid(TASK_TWO_WORKERS_PER_NODE.oid)
                    .end()
                    .assertPersistencePerpetual()
                    .workState()
                        .assertPropertyEquals(F_LAST_RECONCILIATION_TIMESTAMP, lastReconciliationTimestamp)
                        .assertPropertyEquals(F_LAST_CLUSTER_STATE_CHANGE_TIMESTAMP, lastClusterStateChangeTimestamp)
                        .assertItemValueSatisfies(F_LAST_CLUSTER_STATE, nodes(DEFAULT_NODE))
                        .assertPropertyEquals(F_RECONCILIATION_PENDING, false)
                    .end();
        // @formatter:on

        assertWorkerTaskTwoSubtasks();
    }

    /**
     * Node is added, but last reconciliation was just a moment ago. Not reconciling, but scheduling future reconciliation.
     */
    @Test
    public void test120NodeAdded() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assumeExtraClusterNodes(List.of(NODE1), result);

        long before = System.currentTimeMillis();

        when();

        restartTask(TASK_AUTO_SCALING.oid, result);
        waitForTaskCloseOrSuspend(TASK_AUTO_SCALING.oid, TIMEOUT);

        then();

        long after = System.currentTimeMillis();

        // @formatter:off
        assertTask(TASK_AUTO_SCALING.oid, "after")
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertSuccessCount(1) // should not increase
                        .assertLastSuccessObjectOid(TASK_TWO_WORKERS_PER_NODE.oid)
                    .end()
                    .assertPersistencePerpetual()
                    .workState()
                        .assertPropertyEquals(F_LAST_RECONCILIATION_TIMESTAMP, lastReconciliationTimestamp)
                        .assertItemValueSatisfies(F_LAST_CLUSTER_STATE_CHANGE_TIMESTAMP, timeBetween(before, after))
                        .sendItemValue(F_LAST_CLUSTER_STATE_CHANGE_TIMESTAMP, this::setLastClusterStateChangeTimestamp)
                        .assertItemValueSatisfies(F_LAST_CLUSTER_STATE, nodes(DEFAULT_NODE, NODE1))
                        .assertPropertyEquals(F_RECONCILIATION_PENDING, true)
                    .end();
        // @formatter:on

        assertWorkerTaskTwoSubtasks();
    }

    /**
     * Next run comes right after the previous one. No cluster change. Min interval still prevents [pending] reconciliation.
     */
    @Test
    public void test130NextRunNoChange() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        restartTask(TASK_AUTO_SCALING.oid, result);
        waitForTaskCloseOrSuspend(TASK_AUTO_SCALING.oid, TIMEOUT);

        then();

        // @formatter:off
        assertTask(TASK_AUTO_SCALING.oid, "after")
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertSuccessCount(1) // should not increase
                        .assertLastSuccessObjectOid(TASK_TWO_WORKERS_PER_NODE.oid)
                    .end()
                    .assertPersistencePerpetual()
                    .workState()
                        .assertPropertyEquals(F_LAST_RECONCILIATION_TIMESTAMP, lastReconciliationTimestamp)
                        .assertPropertyEquals(F_LAST_CLUSTER_STATE_CHANGE_TIMESTAMP, lastClusterStateChangeTimestamp)
                        .assertItemValueSatisfies(F_LAST_CLUSTER_STATE, nodes(DEFAULT_NODE, NODE1))
                        .assertPropertyEquals(F_RECONCILIATION_PENDING, true)
                    .end();
        // @formatter:on

        assertWorkerTaskTwoSubtasks();
    }

    /**
     * Min interval elapsed. Pending reconciliation should apply.
     */
    @Test
    public void test140AfterMinInterval() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clock.overrideDuration("PT2H"); // min = 1H

        long beforeShifted = clock.currentTimeMillis();

        when();

        restartTask(TASK_AUTO_SCALING.oid, result);
        waitForTaskCloseOrSuspend(TASK_AUTO_SCALING.oid, TIMEOUT);

        then();

        long afterShifted = clock.currentTimeMillis();

        // @formatter:off
        assertTask(TASK_AUTO_SCALING.oid, "after")
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertSuccessCount(2) // increased by 1
                        .assertLastSuccessObjectOid(TASK_TWO_WORKERS_PER_NODE.oid)
                    .end()
                    .assertPersistencePerpetual()
                    .workState()
                        .assertItemValueSatisfies(F_LAST_RECONCILIATION_TIMESTAMP, timeBetween(beforeShifted, afterShifted))
                        .sendItemValue(F_LAST_RECONCILIATION_TIMESTAMP, this::setLastReconciliationTimestamp)
                        .assertPropertyEquals(F_LAST_CLUSTER_STATE_CHANGE_TIMESTAMP, lastClusterStateChangeTimestamp)
                        .assertItemValueSatisfies(F_LAST_CLUSTER_STATE, nodes(DEFAULT_NODE, NODE1))
                        .assertPropertyEquals(F_RECONCILIATION_PENDING, false)
                    .end();
        // @formatter:on

        assertWorkerTaskFourSubtasks();
    }

    /**
     * Node removed after 3 hours. Reconciliation should start immediately.
     */
    @Test
    public void test150NodeRemovedAfter3Hours() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clock.overrideDuration("PT3H");
        assumeNoExtraClusterNodes(result);

        long beforeShifted = clock.currentTimeMillis();

        when();

        restartTask(TASK_AUTO_SCALING.oid, result);
        waitForTaskCloseOrSuspend(TASK_AUTO_SCALING.oid, TIMEOUT);

        then();

        long afterShifted = clock.currentTimeMillis();

        // @formatter:off
        assertTask(TASK_AUTO_SCALING.oid, "after")
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertSuccessCount(3) // increased by 1
                        .assertLastSuccessObjectOid(TASK_TWO_WORKERS_PER_NODE.oid)
                    .end()
                    .assertPersistencePerpetual()
                    .workState()
                        .assertItemValueSatisfies(F_LAST_RECONCILIATION_TIMESTAMP, timeBetween(beforeShifted, afterShifted))
                        .sendItemValue(F_LAST_RECONCILIATION_TIMESTAMP, this::setLastReconciliationTimestamp)
                        .assertItemValueSatisfies(F_LAST_CLUSTER_STATE_CHANGE_TIMESTAMP, timeBetween(beforeShifted, afterShifted))
                        .sendItemValue(F_LAST_CLUSTER_STATE_CHANGE_TIMESTAMP, this::setLastClusterStateChangeTimestamp)
                        .assertItemValueSatisfies(F_LAST_CLUSTER_STATE, nodes(DEFAULT_NODE))
                        .assertPropertyEquals(F_RECONCILIATION_PENDING, false)
                    .end();
        // @formatter:on

        // Note that tasks on Node 1 are NOT suspended. The reason is that they were not actually started (Node1 did not
        // exist in reality). So the reconciliator just lets them be.
        assertWorkerTaskFourSubtasks();
    }

    /**
     * No change for two days. Reconciliation should be triggered after 1 day.
     */
    @Test
    public void test160NoChangeForTwoDays() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clock.overrideDuration("P2D");

        long beforeShifted = clock.currentTimeMillis();

        when();

        restartTask(TASK_AUTO_SCALING.oid, result);
        waitForTaskCloseOrSuspend(TASK_AUTO_SCALING.oid, TIMEOUT);

        then();

        long afterShifted = clock.currentTimeMillis();

        // @formatter:off
        assertTask(TASK_AUTO_SCALING.oid, "after")
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertSuccessCount(4) // increased by 1
                        .assertLastSuccessObjectOid(TASK_TWO_WORKERS_PER_NODE.oid)
                    .end()
                    .assertPersistencePerpetual()
                    .workState()
                        .assertItemValueSatisfies(F_LAST_RECONCILIATION_TIMESTAMP, timeBetween(beforeShifted, afterShifted))
                        .sendItemValue(F_LAST_RECONCILIATION_TIMESTAMP, this::setLastReconciliationTimestamp)
                        .assertPropertyEquals(F_LAST_CLUSTER_STATE_CHANGE_TIMESTAMP, lastClusterStateChangeTimestamp)
                        .assertItemValueSatisfies(F_LAST_CLUSTER_STATE, nodes(DEFAULT_NODE))
                        .assertPropertyEquals(F_RECONCILIATION_PENDING, false)
                    .end();
        // @formatter:on

        assertWorkerTaskFourSubtasks();
    }

    /**
     * Initial auto scaling is turned off for this task.
     */
    @Test
    public void test900AutoScalingSkipInitial() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assumeNoExtraClusterNodes(result);
        clock.resetOverride();

        addTask(TASK_AUTO_SCALING_SKIP_INITIAL, result);

        when();

        waitForTaskCloseOrSuspend(TASK_AUTO_SCALING_SKIP_INITIAL.oid, TIMEOUT);

        then();

        long after = System.currentTimeMillis();

        // @formatter:off
        assertTask(TASK_AUTO_SCALING_SKIP_INITIAL.oid, "after")
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertSuccessCount(0)
                    .end()
                    .assertPersistencePerpetual()
                    .workState()
                        // Note the last reconciliation timestamp should be there even if the initial recon was skipped.
                        .assertItemValueSatisfies(F_LAST_RECONCILIATION_TIMESTAMP, timeBetween(0, after))
                        .assertItemValueSatisfies(F_LAST_CLUSTER_STATE_CHANGE_TIMESTAMP, timeBetween(0, after))
                        .assertItemValueSatisfies(F_LAST_CLUSTER_STATE, nodes(DEFAULT_NODE))
                        .assertItemsAbsent(F_RECONCILIATION_PENDING)
                    .end();
        // @formatter:on

        assertWorkerTaskFourSubtasks();
    }

    private void setLastReconciliationTimestamp(XMLGregorianCalendar lastReconciliationTimestamp) {
        this.lastReconciliationTimestamp = lastReconciliationTimestamp;
    }

    private void setLastClusterStateChangeTimestamp(XMLGregorianCalendar lastClusterStateChangeTimestamp) {
        this.lastClusterStateChangeTimestamp = lastClusterStateChangeTimestamp;
    }

    private void assertWorkerTaskTwoSubtasks() throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        assertTaskTree(TASK_TWO_WORKERS_PER_NODE.oid, "working task after")
                .assertSubtasks(2)
                .subtask("Worker DefaultNode:1 for root activity in task-two-workers-per-node")
                    .assertSchedulingState(READY)
                .end()
                .subtask("Worker DefaultNode:2 for root activity in task-two-workers-per-node")
                    .assertSchedulingState(READY)
                .end();
        // @formatter:on
    }

    private void assertWorkerTaskFourSubtasks() throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        assertTaskTree(TASK_TWO_WORKERS_PER_NODE.oid, "working task after")
                .assertSubtasks(4)
                .subtask("Worker DefaultNode:1 for root activity in task-two-workers-per-node")
                    .assertSchedulingState(READY)
                .end()
                .subtask("Worker DefaultNode:2 for root activity in task-two-workers-per-node")
                    .assertSchedulingState(READY)
                .end()
                .subtask("Worker Node1:1 for root activity in task-two-workers-per-node")
                    .assertSchedulingState(READY)
                .end()
                .subtask("Worker Node1:2 for root activity in task-two-workers-per-node")
                    .assertSchedulingState(READY)
                .end();
        // @formatter:on
    }
}
