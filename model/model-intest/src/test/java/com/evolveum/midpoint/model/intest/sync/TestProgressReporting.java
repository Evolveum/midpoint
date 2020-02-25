/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import java.io.File;
import java.util.Collection;

import com.evolveum.midpoint.model.test.TaskProgressExtract;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.task.api.TaskDebugUtil;
import com.evolveum.midpoint.test.TestResource;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import static org.testng.AssertJUnit.*;

/**
 * Tests for MID-6011. However, as this issue was put into backlog, these tests are not finished and they are not
 * run automatically.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestProgressReporting extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/sync");

    /**
     * We currently do not use slow-down nor error inducing behavior of this resource
     * but let's keep it here.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private DummyInterruptedSyncResource interruptedSyncResource;

    private static final TestResource TASK_RECONCILE_DUMMY = new TestResource(TEST_DIR, "task-reconcile-dummy-interrupted-partitioned.xml", "83eef280-d420-417a-929d-796eed202e02");
    private static final TestResource TASK_RECONCILE_DUMMY_MULTINODE = new TestResource(TEST_DIR, "task-reconcile-dummy-interrupted-partitioned-multinode.xml", "9a52b7a4-afda-4b22-932e-f45b9f90cf95");

    private static final TestResource TASK_RECOMPUTE_ROLES = new TestResource(TEST_DIR, "task-recompute-roles.xml", "42869247-9bf1-4198-acea-3326f5ab2c34");
    private static final TestResource TASK_RECOMPUTE_ROLES_MULTINODE = new TestResource(TEST_DIR, "task-recompute-roles-multinode.xml", "c8cfe559-3888-4b39-b835-3aead9a46581");

    private static final TestResource METAROLE_SLOWING_DOWN = new TestResource(TEST_DIR, "metarole-slowing-down.xml", "b7218b57-fb8a-4dfd-a4c0-976849a4640c");

    private static final int USERS = 1000;
    private static final int ROLES = 1000;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        interruptedSyncResource = new DummyInterruptedSyncResource();
        interruptedSyncResource.init(dummyResourceCollection, initTask, initResult);

        addObject(METAROLE_SLOWING_DOWN, initTask, initResult);
    }

    /**
     * Reconciliation suspend + resume - check for progress reporting issues.
     */
    @Test
    public void test100ReconciliationSuspensionSingleNode() throws Exception {
        executeReconciliation(TASK_RECONCILE_DUMMY, "u", 1);
    }

    @Test
    public void test110ReconciliationSuspensionMultiNode() throws Exception {
        executeReconciliation(TASK_RECONCILE_DUMMY_MULTINODE, "v", 2);
    }

    private void executeReconciliation(TestResource reconciliationTask, String accountPrefix, int workers) throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

        // GIVEN
        for (int i = 0; i < USERS; i++) {
            interruptedSyncResource.getController().addAccount(String.format("%s%03d", accountPrefix, i));
        }

        addObject(reconciliationTask.file, task, result);

        // WHEN
        System.out.println("Waiting before suspending task tree.");
        Thread.sleep(10000L);

        System.out.println("Suspending task tree.");
        boolean stopped = taskManager.suspendTaskTree(reconciliationTask.oid, 10000, result);
        assertTrue("Not all tasks were stopped", stopped);

        System.out.println("Task tree suspended.");
        Collection<SelectorOptions<GetOperationOptions>> getSubtasks = getOperationOptionsBuilder()
                .item(TaskType.F_SUBTASK).retrieve()
                .build();
        PrismObject<TaskType> rootAfterSuspension1 = taskManager.getObject(TaskType.class, reconciliationTask.oid, getSubtasks, result);
        display("Tree after suspension", TaskDebugUtil.dumpTaskTree(rootAfterSuspension1.asObjectable()));

        TaskProgressExtract progress1 = TaskProgressExtract.fromTask(rootAfterSuspension1.asObjectable());
        display("Progress after suspension", progress1);

        System.out.println("Resuming task tree.");
        taskManager.resumeTaskTree(reconciliationTask.oid, result);

        System.out.println("Waiting before suspending task tree second time...");
        Thread.sleep(3000L);

        System.out.println("Suspending task tree.");
        boolean stopped2 = taskManager.suspendTaskTree(reconciliationTask.oid, 10000, result);
        assertTrue("Not all tasks were stopped", stopped2);

        System.out.println("Task tree suspended.");
        PrismObject<TaskType> rootAfterSuspension2 = taskManager.getObject(TaskType.class, reconciliationTask.oid, getSubtasks, result);
        display("Tree after second suspension", TaskDebugUtil.dumpTaskTree(rootAfterSuspension2.asObjectable()));

        TaskProgressExtract progress2 = TaskProgressExtract.fromTask(rootAfterSuspension2.asObjectable());
        display("Progress after second suspension", progress2);

        // THEN

        int bucketSize = 10;

        assertEquals("Wrong # of partitions (1st)", 3, progress1.getPartitioningInformation().getAllPartitions());
        assertEquals("Wrong # of partitions (2nd)", 3, progress2.getPartitioningInformation().getAllPartitions());
        assertEquals("Wrong # of complete partitions (1st)", 1, progress1.getPartitioningInformation().getCompletePartitions());
        assertEquals("Wrong # of complete partitions (2nd)", 1, progress2.getPartitioningInformation().getCompletePartitions());
        assertEquals("Wrong id of first incomplete partition (1st)", (Integer) 2, progress1.getPartitioningInformation().getFirstIncompletePartitionNumber());
        assertEquals("Wrong id of first incomplete partition (2nd)", (Integer) 2, progress2.getPartitioningInformation().getFirstIncompletePartitionNumber());

        assertEquals("Expected bucket count changed", progress1.getExpectedBuckets(), progress2.getExpectedBuckets());
        assertEquals("Wrong # of expected buckets", (Integer) 100, progress2.getExpectedBuckets());
        assertFalse("Completed buckets went backwards", progress1.getCompletedBuckets() > progress2.getCompletedBuckets());
        assertProgressVsBuckets(progress1.getProgress(), progress1.getCompletedBuckets(), bucketSize, workers);

        // The following two tests currently fail. It is not currently not possible to treat progress precisely in bucketed tasks.
        //assertFalse("Progress went backwards", progress1.getProgress() > progress2.getProgress());
        //assertProgressVsBuckets(progress2.getProgress() - progress1.getProgress(), progress2.getCompletedBuckets() - progress1.getCompletedBuckets(), bucketSize, 2);
    }

    /**
     * Recomputation suspend + resume - check for progress reporting issues.
     */
    @Test
    public void test150RecomputationSuspensionSingleNode() throws Exception {
        executeRecomputation(TASK_RECOMPUTE_ROLES, "rx", 1);
    }

    @Test
    public void test160RecomputationSuspensionMultiNode() throws Exception {
        executeRecomputation(TASK_RECOMPUTE_ROLES_MULTINODE, "ry", 2);
    }

    private void executeRecomputation(TestResource recomputationTask, String rolePrefix, int workers) throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

        // GIVEN
        System.out.println("Importing roles.");
        for (int i = 0; i < ROLES; i++) {
            RoleType role = new RoleType(prismContext)
                    .name(String.format("%s%03d", rolePrefix, i))
                    .beginAssignment()
                        .targetRef(METAROLE_SLOWING_DOWN.oid, RoleType.COMPLEX_TYPE)
                    .end();
            repositoryService.addObject(role.asPrismObject(), null, result);
        }

        System.out.println("Importing recompute task.");
        addObject(recomputationTask.file, task, result);

        // WHEN
        System.out.println("Waiting before suspending task tree.");
        Thread.sleep(10000L);

        System.out.println("Suspending task tree.");
        boolean stopped = taskManager.suspendTaskTree(recomputationTask.oid, 10000, result);
        assertTrue("Not all tasks were stopped", stopped);

        System.out.println("Task tree suspended.");
        Collection<SelectorOptions<GetOperationOptions>> getSubtasks = getOperationOptionsBuilder()
                .item(TaskType.F_SUBTASK).retrieve()
                .build();
        PrismObject<TaskType> rootAfterSuspension1 = taskManager.getObject(TaskType.class, recomputationTask.oid, getSubtasks, result);
        display("Tree after suspension", TaskDebugUtil.dumpTaskTree(rootAfterSuspension1.asObjectable()));

        TaskProgressExtract progress1 = TaskProgressExtract.fromTask(rootAfterSuspension1.asObjectable());
        display("Progress after suspension", progress1);

        System.out.println("Resuming task tree.");
        taskManager.resumeTaskTree(recomputationTask.oid, result);

        System.out.println("Waiting before suspending task tree second time...");
        Thread.sleep(3000L);

        System.out.println("Suspending task tree.");
        boolean stopped2 = taskManager.suspendTaskTree(recomputationTask.oid, 10000, result);
        assertTrue("Not all tasks were stopped", stopped2);

        System.out.println("Task tree suspended.");
        PrismObject<TaskType> rootAfterSuspension2 = taskManager.getObject(TaskType.class, recomputationTask.oid, getSubtasks, result);
        display("Tree after second suspension", TaskDebugUtil.dumpTaskTree(rootAfterSuspension2.asObjectable()));

        TaskProgressExtract progress2 = TaskProgressExtract.fromTask(rootAfterSuspension2.asObjectable());
        display("Progress after second suspension", progress2);

        // THEN

        int bucketSize = 10;

        assertEquals("Expected bucket count changed", progress1.getExpectedBuckets(), progress2.getExpectedBuckets());
        assertEquals("Wrong # of expected buckets", (Integer) 100, progress2.getExpectedBuckets());
        assertFalse("Completed buckets went backwards", progress1.getCompletedBuckets() > progress2.getCompletedBuckets());
        assertProgressVsBuckets(progress1.getProgress(), progress1.getCompletedBuckets(), bucketSize, workers);

        // The following two tests currently fail. It is not currently not possible to treat progress precisely in bucketed tasks.
        //assertFalse("Progress went backwards", progress1.getProgress() > progress2.getProgress());
        //assertProgressVsBuckets(progress2.getProgress() - progress1.getProgress(), progress2.getCompletedBuckets() - progress1.getCompletedBuckets(), bucketSize, 2);
    }

    private void assertProgressVsBuckets(long progress, int completedBuckets, int bucketSize, int workers) {
        if (progress < completedBuckets*bucketSize) {
            fail("Progress (" + progress + ") is lower than buckets (" + completedBuckets + ") x size (" + bucketSize + ")");
        } else if (progress > completedBuckets*bucketSize + workers*bucketSize) {
            fail("Progress (" + progress + ") is greater than buckets (" + completedBuckets + ") x size (" + bucketSize
                    + ") + workers (" + workers + ") x size (" + bucketSize + ")");
        }
    }
}
