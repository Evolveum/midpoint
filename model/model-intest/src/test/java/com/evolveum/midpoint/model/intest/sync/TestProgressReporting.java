/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Collection;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.task.TaskPartProgressInformation;
import com.evolveum.midpoint.schema.util.task.TaskPerformanceInformation;
import com.evolveum.midpoint.schema.util.task.TaskProgressInformation;
import com.evolveum.midpoint.task.api.TaskDebugUtil;
import com.evolveum.midpoint.test.TestResource;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import static org.testng.AssertJUnit.*;

/**
 * Tests for MID-6011.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestProgressReporting extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/reporting");

    /**
     * We currently do not use slow-down nor error inducing behavior of this resource
     * but let's keep it here.
     *
     * BEWARE: It is "imported" from `sync` directory.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private DummyInterruptedSyncResource interruptedSyncResource;

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestResource<TaskType> TASK_RECONCILE_DUMMY_0T_NB_NP =
            new TestResource<>(TEST_DIR, "task-reconcile-dummy-0t-nb-np.xml", "a9b042dd-0a67-4829-b0c4-a51026af0070");
    private static final TestResource<TaskType> TASK_RECONCILE_DUMMY_2T_NB_NP =
            new TestResource<>(TEST_DIR, "task-reconcile-dummy-2t-nb-np.xml", "944f7dbd-b221-4df3-b975-781c7794af6e");
    private static final TestResource<TaskType> TASK_RECONCILE_DUMMY_PARTITIONED =
            new TestResource<>(TEST_DIR, "task-reconcile-dummy-interrupted-partitioned.xml", "83eef280-d420-417a-929d-796eed202e02");
    private static final TestResource<TaskType> TASK_RECONCILE_DUMMY_PARTITIONED_MULTINODE
            = new TestResource<>(TEST_DIR, "task-reconcile-dummy-interrupted-partitioned-multinode.xml", "9a52b7a4-afda-4b22-932e-f45b9f90cf95");

    private static final TestResource<TaskType> TASK_RECOMPUTE_ROLES
            = new TestResource<>(TEST_DIR, "task-recompute-roles.xml", "42869247-9bf1-4198-acea-3326f5ab2c34");
    private static final TestResource<TaskType> TASK_RECOMPUTE_ROLES_MULTINODE
            = new TestResource<>(TEST_DIR, "task-recompute-roles-multinode.xml", "c8cfe559-3888-4b39-b835-3aead9a46581");

    private static final TestResource<TaskType> METAROLE_SLOWING_DOWN
            = new TestResource<>(TEST_DIR, "metarole-slowing-down.xml", "b7218b57-fb8a-4dfd-a4c0-976849a4640c");

    private static final int USERS = 400;
    private static final int ROLES = 400;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        interruptedSyncResource = new DummyInterruptedSyncResource();
        interruptedSyncResource.init(dummyResourceCollection, initTask, initResult);

        addObject(METAROLE_SLOWING_DOWN, initTask, initResult);
        createShadowWithPendingOperation();
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    protected boolean isAvoidLoggingChange() {
        return false; // We need custom logging.
    }

    private void createShadowWithPendingOperation() throws CommonException {
        Task task = createTask();
        OperationResult result = task.getResult(); // Must be separate from initResult because of IN_PROGRESS status

        String resourceOid = interruptedSyncResource.getController().getResource().getOid();
        UserType user = new UserType(prismContext)
                .name("pending")
                .beginAssignment()
                    .beginConstruction()
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                        .<AssignmentType>end()
                    .end();
        addObject(user.asPrismObject(), task, result);

        modifyResourceMaintenance(resourceOid, AdministrativeAvailabilityStatusType.MAINTENANCE, task, result);
        deleteObject(UserType.class, user.getOid(), task, result);

        modifyResourceMaintenance(resourceOid, AdministrativeAvailabilityStatusType.OPERATIONAL, task, result);
    }

    @Test
    public void test100PlainCompleteReconciliation() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        int users = 10;
        createAccounts("a", users);

        addObject(TASK_RECONCILE_DUMMY_0T_NB_NP.file, task, result);

        when();

        waitForTaskFinish(TASK_RECONCILE_DUMMY_0T_NB_NP.oid, false);

        then();

        assertTask(TASK_RECONCILE_DUMMY_0T_NB_NP.oid, "after")
                .display()
                .assertClosed()
                .structuredProgress()
                    .display()
                    .part(ModelPublicConstants.RECONCILIATION_OPERATION_COMPLETION_PART_URI)
                        .assertSuccessCount(0, 1)
                        .end()
                    .part(ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_PART_URI)
                        .assertSuccessCount(0, 10)
                        .end()
                    .part(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_PART_URI)
                        .assertSuccessCount(0, 1)
                        .end()
                    .end()
                .iterativeTaskInformation()
                    .display()
                    .end();

        deleteObject(TaskType.class, TASK_RECONCILE_DUMMY_0T_NB_NP.oid);
    }

    /**
     * Reconciliation suspend + resume - check for progress reporting issues.
     */
    @Test
    public void test110Reconcile_0T_NB_NP() throws Exception {
        executePlainReconciliation(TASK_RECONCILE_DUMMY_0T_NB_NP, "b");
    }

    @Test
    public void test120Reconcile_2T_NB_NP() throws Exception {
        executePlainReconciliation(TASK_RECONCILE_DUMMY_2T_NB_NP, "c");
    }

    /**
     * TODO
     */
    @Test
    public void test130ReconciliationSuspensionPartitioned() throws Exception {
        executePartitionedBucketedReconciliation(TASK_RECONCILE_DUMMY_PARTITIONED, "v", 1);
    }

    @Test
    public void test140ReconciliationSuspensionPartitionedMultiNode() throws Exception {
        executePartitionedBucketedReconciliation(TASK_RECONCILE_DUMMY_PARTITIONED_MULTINODE, "w", 2);
    }

    @SuppressWarnings("SameParameterValue")
    private void executePlainReconciliation(TestResource<TaskType> reconciliationTask, String accountPrefix)
            throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();
        createAccounts(accountPrefix, USERS);

        addObject(reconciliationTask.file, task, result);

        when("1st run");

        System.out.println("Waiting before suspending task tree.");
        Thread.sleep(6000L);

        System.out.println("Suspending task tree.");
        boolean stopped = taskManager.suspendTaskTree(reconciliationTask.oid, 10000, result);
        assertTrue("Not all tasks were stopped", stopped);

        then("1st run");

        System.out.println("Task tree suspended.");
        Collection<SelectorOptions<GetOperationOptions>> getSubtasks = getOperationOptionsBuilder()
                .item(TaskType.F_SUBTASK_REF).retrieve()
                .build();
        PrismObject<TaskType> rootAfterSuspension1 = taskManager.getObject(TaskType.class, reconciliationTask.oid, getSubtasks, result);
        displayValue("Tree after suspension", TaskDebugUtil.dumpTaskTree(rootAfterSuspension1.asObjectable()));

        assertTask(rootAfterSuspension1.asObjectable(), "1st")
                .display()
                .structuredProgress()
                    .display()
                    .end()
                .iterativeTaskInformation()
                    .display()
                    .end();

        TaskProgressInformation progress1 = TaskProgressInformation.fromTaskTree(rootAfterSuspension1.asObjectable());
        assertTaskProgress(progress1, "after 1st run")
                .display()
                .assertAllPartsCount(3)
                .assertCurrentPartNumber(2)
                .checkConsistence()
                .part(ModelPublicConstants.RECONCILIATION_OPERATION_COMPLETION_PART_URI)
                    .assertNoBucketInformation()
                    .assertItems(1, null)
                    .assertComplete()
                    .end()
                .currentPart()
                    .assertNoBucketInformation()
                    .items()
                        .assertProgressGreaterThanZero()
                        .assertNoExpectedTotal();

        TaskPerformanceInformation perf1 = TaskPerformanceInformation.fromTaskTree(rootAfterSuspension1.asObjectable());
        displayDumpable("perf after 1st run", perf1);

        when("2nd run");

        System.out.println("Resuming task tree.");
        taskManager.resumeTaskTree(reconciliationTask.oid, result);

        System.out.println("Waiting before suspending task tree second time...");
        Thread.sleep(3000L);

        System.out.println("Suspending task tree.");
        boolean stopped2 = taskManager.suspendTaskTree(reconciliationTask.oid, 10000, result);
        assertTrue("Not all tasks were stopped", stopped2);

        then("2nd run");

        System.out.println("Task tree suspended.");
        PrismObject<TaskType> rootAfterSuspension2 = taskManager.getObject(TaskType.class, reconciliationTask.oid, getSubtasks, result);
        displayValue("Tree after second suspension", TaskDebugUtil.dumpTaskTree(rootAfterSuspension2.asObjectable()));

        assertTask(rootAfterSuspension2.asObjectable(), "2nd")
                .display()
                .structuredProgress()
                    .display()
                    .end()
                .iterativeTaskInformation()
                    .display()
                    .end();

        TaskProgressInformation progress2 = TaskProgressInformation.fromTaskTree(rootAfterSuspension2.asObjectable());
        assertTaskProgress(progress2, "after 2nd suspension")
                .display()
                .assertAllPartsCount(3)
                .assertCurrentPartNumber(2)
                .currentPart()
                    .assertNoBucketInformation()
                    .items()
                        .assertProgressGreaterThanZero();

        TaskPerformanceInformation perf2 = TaskPerformanceInformation.fromTaskTree(rootAfterSuspension2.asObjectable());
        displayDumpable("perf after 2nd run", perf2);
    }

    private void createAccounts(String accountPrefix, int users) throws ObjectAlreadyExistsException, SchemaViolationException,
            ConnectException, FileNotFoundException, ConflictException, InterruptedException {
        for (int i = 0; i < users; i++) {
            interruptedSyncResource.getController().addAccount(String.format("%s%03d", accountPrefix, i));
        }
    }

    private void executePartitionedBucketedReconciliation(TestResource<TaskType> reconciliationTask, String accountPrefix,
            int workers) throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();
        createAccounts(accountPrefix, USERS);

        addObject(reconciliationTask.file, task, result);

        int bucketSize = 10;

        when("1st run");

        System.out.println("Waiting before suspending task tree.");
        Thread.sleep(6000L);

        System.out.println("Suspending task tree.");
        boolean stopped = taskManager.suspendTaskTree(reconciliationTask.oid, 10000, result);
        assertTrue("Not all tasks were stopped", stopped);

        then("1st run");

        System.out.println("Task tree suspended.");
        Collection<SelectorOptions<GetOperationOptions>> getSubtasks = getOperationOptionsBuilder()
                .item(TaskType.F_SUBTASK_REF).retrieve()
                .build();
        PrismObject<TaskType> rootAfterSuspension1 = taskManager.getObject(TaskType.class, reconciliationTask.oid, getSubtasks, result);
        displayValue("Tree after suspension", TaskDebugUtil.dumpTaskTree(rootAfterSuspension1.asObjectable()));

        assertTask(rootAfterSuspension1.asObjectable(), "after 1st run")
                .subtaskForPart(1)
                    .display()
                    .structuredProgress()
                        .display()
                        .assertSuccessCount(1, false)
                        .currentPart()
                            .assertComplete()
                            .end()
                        .end()
                    .end()
                .subtaskForPart(2)
                    .display()
                    .structuredProgress()
                        .display()
                        .end()
                    .iterativeTaskInformation()
                        .display()
                        .end();

        TaskProgressInformation progress1 = TaskProgressInformation.fromTaskTree(rootAfterSuspension1.asObjectable());
        TaskPartProgressInformation info1 = assertTaskProgress(progress1, "after 1st run")
                .display()
                .assertAllPartsCount(3)
                .assertCurrentPartNumber(2)
                .currentPart()
                    .assertExpectedBuckets(100)
                    .assertBucketsItemsConsistency(bucketSize, workers)
                    .get();

        TaskPerformanceInformation perf1 = TaskPerformanceInformation.fromTaskTree(rootAfterSuspension1.asObjectable());
        displayDumpable("perf after 1st run", perf1);

        when("2nd run");

        System.out.println("Resuming task tree.");
        taskManager.resumeTaskTree(reconciliationTask.oid, result);

        System.out.println("Waiting before suspending task tree second time...");
        Thread.sleep(3000L);

        System.out.println("Suspending task tree.");
        boolean stopped2 = taskManager.suspendTaskTree(reconciliationTask.oid, 10000, result);
        assertTrue("Not all tasks were stopped", stopped2);

        then("2nd run");

        System.out.println("Task tree suspended.");
        PrismObject<TaskType> rootAfterSuspension2 = taskManager.getObject(TaskType.class, reconciliationTask.oid, getSubtasks, result);
        displayValue("Tree after second suspension", TaskDebugUtil.dumpTaskTree(rootAfterSuspension2.asObjectable()));

        assertTask(rootAfterSuspension2.asObjectable(), "after 2nd run")
                .subtaskForPart(1)
                    .display()
                    .structuredProgress()
                        .display()
                        .assertSuccessCount(1, false)
                        .currentPart()
                            .assertComplete()
                            .end()
                        .end()
                    .end()
                .subtaskForPart(2)
                    .display()
                    .structuredProgress()
                        .display()
                        .end()
                    .iterativeTaskInformation()
                        .display()
                        .end();

        TaskProgressInformation progress2 = TaskProgressInformation.fromTaskTree(rootAfterSuspension2.asObjectable());
        assertTaskProgress(progress2, "after 2nd suspension")
                .display()
                .assertAllPartsCount(3)
                .assertCurrentPartNumber(2)
                .checkConsistence()
                .currentPart()
                    .assertExpectedBuckets(100)
                    .assertCompletedBucketsAtLeast(info1.getBucketsProgress().getCompletedBuckets())
                    .assertItemsProgressAtLeast(info1.getItemsProgress().getProgress())
                    .assertBucketsItemsConsistency(bucketSize, workers)
                    .get();

        TaskPerformanceInformation perf2 = TaskPerformanceInformation.fromTaskTree(rootAfterSuspension2.asObjectable());
        displayDumpable("perf after 2nd run", perf2);
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

    private void executeRecomputation(TestResource<TaskType> recomputationTask, String rolePrefix, int workers) throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();
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

        int bucketSize = 10;

        when("1st run");

        System.out.println("Waiting before suspending task tree.");
        Thread.sleep(6000L);

        System.out.println("Suspending task tree.");
        boolean stopped = taskManager.suspendTaskTree(recomputationTask.oid, 10000, result);
        assertTrue("Not all tasks were stopped", stopped);
        System.out.println("Task tree suspended.");

        then("1st run");

        Collection<SelectorOptions<GetOperationOptions>> getSubtasks = getOperationOptionsBuilder()
                .item(TaskType.F_SUBTASK_REF).retrieve()
                .build();
        PrismObject<TaskType> rootAfterSuspension1 = taskManager.getObject(TaskType.class, recomputationTask.oid, getSubtasks, result);
        displayValue("Tree after suspension", TaskDebugUtil.dumpTaskTree(rootAfterSuspension1.asObjectable()));

        assertTask(rootAfterSuspension1.asObjectable(), "1st")
                .display();

        TaskProgressInformation progress1 = TaskProgressInformation.fromTaskTree(rootAfterSuspension1.asObjectable());
        TaskPartProgressInformation info1 = assertTaskProgress(progress1, "after 1st suspension")
                .display()
                .assertAllPartsCount(1)
                .assertCurrentPartNumber(1)
                .currentPart()
                    .assertExpectedBuckets(100)
                    .assertBucketsItemsConsistency(bucketSize, workers)
                    .get();

        TaskPerformanceInformation perf1 = TaskPerformanceInformation.fromTaskTree(rootAfterSuspension1.asObjectable());
        displayDumpable("perf after 1st run", perf1);

        when("2nd run");

        System.out.println("Resuming task tree.");
        taskManager.resumeTaskTree(recomputationTask.oid, result);

        System.out.println("Waiting before suspending task tree second time...");
        Thread.sleep(3000L);

        System.out.println("Suspending task tree.");
        boolean stopped2 = taskManager.suspendTaskTree(recomputationTask.oid, 10000, result);
        assertTrue("Not all tasks were stopped", stopped2);
        System.out.println("Task tree suspended.");

        then("2nd run");

        PrismObject<TaskType> rootAfterSuspension2 = taskManager.getObject(TaskType.class, recomputationTask.oid, getSubtasks, result);
        displayValue("Tree after second suspension", TaskDebugUtil.dumpTaskTree(rootAfterSuspension2.asObjectable()));

        assertTask(rootAfterSuspension2.asObjectable(), "2nd")
                .display()
                .structuredProgress()
                    .display();

        TaskProgressInformation progress2 = TaskProgressInformation.fromTaskTree(rootAfterSuspension2.asObjectable());
        assertTaskProgress(progress2, "after 2nd suspension")
                .display()
                .assertAllPartsCount(1)
                .assertCurrentPartNumber(1)
                .currentPart()
                    .assertExpectedBuckets(100)
                    .assertBucketsItemsConsistency(bucketSize, workers)
                    .assertCompletedBucketsAtLeast(info1.getBucketsProgress().getCompletedBuckets())
                    .assertItemsProgressAtLeast(info1.getItemsProgress().getProgress())
                    .get();

        TaskPerformanceInformation perf2 = TaskPerformanceInformation.fromTaskTree(rootAfterSuspension2.asObjectable());
        displayDumpable("perf after 2nd run", perf2);
    }
}
