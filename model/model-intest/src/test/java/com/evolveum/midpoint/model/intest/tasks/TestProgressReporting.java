/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.tasks;

import static com.evolveum.midpoint.schema.util.task.ActivityProgressInformationBuilder.InformationSource.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertTrue;

import static com.evolveum.midpoint.model.api.ModelPublicConstants.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.List;

import com.evolveum.icf.dummy.resource.ObjectDoesNotExistException;
import com.evolveum.midpoint.schema.util.task.ActivityBasedTaskInformation;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformationBuilder.InformationSource;
import com.evolveum.midpoint.schema.util.task.TaskInformation;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityProgressInformation;
import com.evolveum.midpoint.schema.util.task.TaskTreeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskDebugUtil;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests for progress-related issues, like MID-6011.
 *
 * Normally, such tests should be part of test classes devoted to specific activity handlers,
 * but some generic ones can be also here.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestProgressReporting extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/reporting"); // TODO move

    /**
     * We currently do not use slow-down nor error inducing behavior of this resource
     * but let's keep it here.
     *
     * BEWARE: It is "imported" from `sync` directory.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private DummyInterruptedSyncResource interruptedSyncResource;

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<TaskType> TASK_RECONCILE_DUMMY_0T_NB_NP =
            TestObject.file(TEST_DIR, "task-reconcile-dummy-0t-nb-np.xml", "a9b042dd-0a67-4829-b0c4-a51026af0070");
    private static final TestObject<TaskType> TASK_RECONCILE_DUMMY_2T_NB_NP =
            TestObject.file(TEST_DIR, "task-reconcile-dummy-2t-nb-np.xml", "944f7dbd-b221-4df3-b975-781c7794af6e");
    private static final TestObject<TaskType> TASK_RECONCILE_DUMMY_PARTITIONED =
            TestObject.file(TEST_DIR, "task-reconcile-dummy-interrupted-partitioned.xml", "83eef280-d420-417a-929d-796eed202e02");
    private static final TestObject<TaskType> TASK_RECONCILE_DUMMY_PARTITIONED_MULTINODE
            = TestObject.file(TEST_DIR, "task-reconcile-dummy-interrupted-partitioned-multinode.xml", "9a52b7a4-afda-4b22-932e-f45b9f90cf95");

    private static final TestObject<TaskType> TASK_RECOMPUTE_ROLES =
            TestObject.file(TEST_DIR, "task-recompute-roles.xml", "42869247-9bf1-4198-acea-3326f5ab2c34");
    private static final TestObject<TaskType> TASK_RECOMPUTE_ROLES_MULTINODE =
            TestObject.file(TEST_DIR, "task-recompute-roles-multinode.xml", "c8cfe559-3888-4b39-b835-3aead9a46581");

    private static final TestObject<TaskType> METAROLE_SLOWING_DOWN =
            TestObject.file(TEST_DIR, "metarole-slowing-down.xml", "b7218b57-fb8a-4dfd-a4c0-976849a4640c");

    private static final TestObject<TaskType> TASK_WITH_ERRORS =
            TestObject.file(TEST_DIR, "task-with-errors.xml", "cfcfa2d8-ba8a-45b3-af94-3a4e5774768a");
    private static final TestObject<TaskType> TASK_WITH_ERRORS_CHILD_1 =
            TestObject.file(TEST_DIR, "task-with-errors-child-1.xml", "3d288cce-898d-49b9-8987-ce9e6116bb0b");
    private static final TestObject<TaskType> TASK_WITH_ERRORS_CHILD_2 =
            TestObject.file(TEST_DIR, "task-with-errors-child-2.xml", "8d5909f6-7d8b-4c01-913b-42ba0bcf6ef7");

    private static final int USERS = 1000;
    private static final int ROLES = 1000;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        interruptedSyncResource = new DummyInterruptedSyncResource();
        interruptedSyncResource.init(dummyResourceCollection, initTask, initResult);

        addObject(METAROLE_SLOWING_DOWN, initTask, initResult);
        createShadowWithPendingOperation();

        repoAdd(TASK_WITH_ERRORS, initResult);
        repoAdd(TASK_WITH_ERRORS_CHILD_1, initResult);
        repoAdd(TASK_WITH_ERRORS_CHILD_2, initResult);
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
        UserType user = new UserType()
                .name("pending")
                .beginAssignment()
                    .beginConstruction()
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                        .<AssignmentType>end()
                    .end();
        addObject(user, task, result);

        turnMaintenanceModeOn(resourceOid, result);
        deleteObject(UserType.class, user.getOid(), task, result);
        turnMaintenanceModeOff(resourceOid, result);
    }

    @Test
    public void test100PlainCompleteReconciliation() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        int users = 10;
        createAccounts("a", users);

        addObject(TASK_RECONCILE_DUMMY_0T_NB_NP, task, result);

        when();

        waitForTaskFinish(TASK_RECONCILE_DUMMY_0T_NB_NP.oid);

        then();

        assertTask(TASK_RECONCILE_DUMMY_0T_NB_NP.oid, "after")
                .display()
                .assertClosed()
                .rootActivityState()
                    .child(RECONCILIATION_OPERATION_COMPLETION_ID)
                        .progress()
                            .assertSuccessCount(0, 1)
                        .end()
                        .itemProcessingStatistics().display().end()
                    .end()
                    .child(RECONCILIATION_RESOURCE_OBJECTS_ID)
                        .progress()
                            .assertSuccessCount(0, 10)
                        .end()
                        .itemProcessingStatistics().display().end()
                    .end()
                    .child(RECONCILIATION_REMAINING_SHADOWS_ID)
                        .progress()
                            .assertCommitted(1, 0, 0) // "pending" is processed because of death timestamp
                            .assertUncommitted(0, 0, 0)
                        .end()
                        .itemProcessingStatistics().display().end()
                    .end()
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
    private void executePlainReconciliation(TestObject<TaskType> reconciliationTask, String accountPrefix)
            throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();
        createAccounts(accountPrefix, USERS);

        addObject(reconciliationTask, task, result);

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
                .display();

        assertProgress(reconciliationTask.oid, "after 1st run")
                .display()
                .assertChildren(3)
                .checkConsistence()
                .child(RECONCILIATION_OPERATION_COMPLETION_ID)
                    .assertBuckets(1, 1)
                    .assertItems(1, 1)
                    .assertComplete()
                    .end()
                .child(RECONCILIATION_RESOURCE_OBJECTS_ID)
                    .assertBuckets(0, 1)
                    .items()
                        .assertProgressGreaterThanZero()
                        .assertNoExpectedTotal();

        assertPerformance(reconciliationTask.oid, "1st")
                .display();

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
                .display();

        assertProgress(reconciliationTask.oid, "after 2nd run")
                .display()
                .assertChildren(3)
                .checkConsistence()
                .child(RECONCILIATION_OPERATION_COMPLETION_ID)
                    .assertBuckets(1, 1)
                    .assertItems(1, 1)
                    .assertComplete()
                    .end()
                .child(RECONCILIATION_RESOURCE_OBJECTS_ID)
                    .assertBuckets(0, 1)
                    .items()
                        .assertProgressGreaterThanZero()
                        .assertNoExpectedTotal();


        assertPerformance(reconciliationTask.oid, "2nd")
                .display();
    }

    private void createAccounts(String accountPrefix, int users) throws ObjectAlreadyExistsException, SchemaViolationException,
            ConnectException, FileNotFoundException, ConflictException, InterruptedException, ObjectDoesNotExistException {
        for (int i = 0; i < users; i++) {
            interruptedSyncResource.getController().addAccount(String.format("%s%03d", accountPrefix, i));
        }
    }

    private void executePartitionedBucketedReconciliation(TestObject<TaskType> reconciliationTask, String accountPrefix,
            int workers) throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();
        createAccounts(accountPrefix, USERS);

        addObject(reconciliationTask, task, result);

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
                .display()
                .subtaskForPath(RECONCILIATION_OPERATION_COMPLETION_PATH)
                    .display()
                .end()
                .subtaskForPath(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .display();

        ActivityProgressInformation info1 = assertProgress(reconciliationTask.oid, "after 1st run")
                .display()
                .assertChildren(3)
                .checkConsistence()
                .child(RECONCILIATION_OPERATION_COMPLETION_ID)
                    .assertBuckets(1, 1)
                    .assertItems(1, 1)
                    .assertComplete()
                    .end()
                .child(RECONCILIATION_RESOURCE_OBJECTS_ID)
                    .assertExpectedBuckets(100)
                    .assertBucketsItemsConsistency(bucketSize, workers)
                    .items().display().end()
                    .get();

        assertPerformance(reconciliationTask.oid, "1st")
                .display();

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
                .subtaskForPath(RECONCILIATION_OPERATION_COMPLETION_PATH)
                    .display()
                .end()
                .subtaskForPath(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .display();

        assertProgress(reconciliationTask.oid, "after 2nd run")
                .display()
                .assertChildren(3)
                .checkConsistence()
                .child(RECONCILIATION_OPERATION_COMPLETION_ID)
                    .assertBuckets(1, 1)
                    .assertItems(1, 1)
                    .assertComplete()
                    .end()
                .child(RECONCILIATION_RESOURCE_OBJECTS_ID)
                    .assertExpectedBuckets(100)
                    .assertCompletedBucketsAtLeast(info1.getBucketsProgress().getCompleteBuckets())
                    .assertItemsProgressAtLeast(info1.getItemsProgress().getProgress())
                    .assertBucketsItemsConsistency(bucketSize, workers)
                    .items().display().end()
                    .get();

        assertPerformance(reconciliationTask.oid, "2nd")
                .display();

        PrismObject<TaskType> subtask2 = assertTask(rootAfterSuspension2.asObjectable(), "after 2nd run")
                .subtaskForPath(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .getObject();
        List<TaskType> children2 = TaskTreeUtil.getResolvedSubtasks(subtask2.asObjectable());
        if (!children2.isEmpty()) {
            TaskType worker = children2.get(0);
            assertTask(worker, "worker")
                    .display();
        }
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

    private void executeRecomputation(TestObject<TaskType> recomputationTask, String rolePrefix, int workers) throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();
        System.out.println("Importing roles.");
        for (int i = 0; i < ROLES; i++) {
            RoleType role = new RoleType()
                    .name(String.format("%s%03d", rolePrefix, i))
                    .beginAssignment()
                        .targetRef(METAROLE_SLOWING_DOWN.oid, RoleType.COMPLEX_TYPE)
                    .end();
            repositoryService.addObject(role.asPrismObject(), null, result);
        }

        System.out.println("Importing recompute task.");
        addObject(recomputationTask, task, result);

        int bucketSize = 10;

        when("1st run");

        System.out.println("Waiting before suspending task tree.");
        Thread.sleep(5000L);

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
                .display()
                .rootActivityStateOverview()
                    .display()
                .end();

        ActivityProgressInformation info1 = assertProgress(recomputationTask.oid, "1st")
                .display()
                .assertChildren(0)
                .assertExpectedBuckets(100)
                .assertExpectedItems(workers <= 1 ? ROLES : null)
                .assertBucketsItemsConsistency(bucketSize, workers)
                .get();

        assertPerformance(recomputationTask.oid, "1st")
                .display();

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
                .rootActivityStateOverview()
                    .display()
                .end();

        var treePreferred = assertProgress(recomputationTask.oid, TREE_OVERVIEW_PREFERRED, "2nd-tree-preferred")
                .display()
                .assertChildren(0)
                .assertExpectedBuckets(100)
                .assertBucketsItemsConsistency(bucketSize, workers)
                .assertCompletedBucketsAtLeast(info1.getBucketsProgress().getCompleteBuckets())
                .assertItemsProgressAtLeast(info1.getItemsProgress().getProgress())
                .get();

        var fullPreferred = assertProgress(recomputationTask.oid, FULL_STATE_PREFERRED, "2nd-full-preferred")
                .display()
                .get();

        assertThat(treePreferred.getBucketsProgress())
                .as("bucket progress in tree-preferred")
                .isEqualTo(fullPreferred.getBucketsProgress());
        assertThat(treePreferred.getItemsProgress())
                .as("items progress in tree-preferred")
                .isEqualTo(fullPreferred.getItemsProgress());
    }

    /**
     * Let us check whether we correctly determine the error counters from stored tasks.
     *
     * Checking both at the level of {@link TaskInformation} and {@link ActivityProgressInformation}.
     * (The latter can be used to specify various {@link InformationSource} options.)
     *
     * MID-7339
     */
    @Test
    public void test200ErrorCounters() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TaskType rootOnlyBean = taskManager
                .getTask(TASK_WITH_ERRORS.oid, null, result)
                .getRawTaskObjectClonedIfNecessary().asObjectable();

        TaskType fullTreeBean = taskManager
                .getTaskTree(TASK_WITH_ERRORS.oid, result)
                .getRawTaskObjectClonedIfNecessary().asObjectable();

        assertThat(fullTreeBean.getSubtaskRef()).as("subtask refs").hasSize(2);
        assertThat(TaskTreeUtil.allSubtasksAreResolved(fullTreeBean)).as("all subtasks resolved").isTrue();

        // ------------------------------------------------------------- Checking TaskInformation when only root task is fetched.
        when("TI - root only");
        var taskInfoOnlyRoot = TaskInformation.createForTask(rootOnlyBean, null);

        then("TI - root only");
        displayDumpable("task info (root only)", taskInfoOnlyRoot);
        assertProgress(((ActivityBasedTaskInformation) taskInfoOnlyRoot).getProgressInformation(), "only root")
                .display()
                .assertItems(46, 10, null);

        // -------------------------------------------------------------- Checking TaskInformation when the full tree is fetched.
        when("TI - full tree");
        var taskInfoFullTree = TaskInformation.createForTask(fullTreeBean, null);

        then("TI - full tree");
        displayDumpable("task info (full tree)", taskInfoFullTree);
        assertProgress(((ActivityBasedTaskInformation) taskInfoFullTree).getProgressInformation(), "full tree")
                .display()
                .assertItems(46, 10, null);

        // ------------------------------------------------- Checking ActivityProgressInformation when only root task is fetched.
        when("API - root only - tree overview only");
        var apiRootOnlyOverviewOnly =
                ActivityProgressInformation.fromRootTask(rootOnlyBean, TREE_OVERVIEW_ONLY);

        then("API - root only - tree overview only");
        assertProgress(apiRootOnlyOverviewOnly, "root only - tree overview only")
                .display()
                .assertItems(46, 10, null);

        when("API - root only - tree overview preferred");
        var apiRootOnlyOverviewPreferred =
                ActivityProgressInformation.fromRootTask(rootOnlyBean, TREE_OVERVIEW_PREFERRED);

        then("API - root only - tree overview preferred");
        assertProgress(apiRootOnlyOverviewPreferred, "root only - tree overview preferred")
                .display()
                .assertItems(46, 10, null);

        when("API - root only - full state preferred");
        var apiRootOnlyFullPreferred =
                ActivityProgressInformation.fromRootTask(rootOnlyBean, FULL_STATE_PREFERRED);

        then("API - root only - full state preferred");
        assertProgress(apiRootOnlyFullPreferred, "root only - full state preferred")
                .display()
                .assertItems(46, 10, null);

        when("API - root only - full state only");
        try {
            ActivityProgressInformation.fromRootTask(rootOnlyBean, FULL_STATE_ONLY);
        } catch (IllegalStateException e) {
            displayExpectedException(e);
            assertThat(e).hasMessageContaining("Full activity state is not present");
        }

        // ------------------------------------------------- Checking ActivityProgressInformation when the whole tree is fetched.
        when("API - full tree - tree overview only");
        var apiFullTreeOverviewOnly =
                ActivityProgressInformation.fromRootTask(fullTreeBean, TREE_OVERVIEW_ONLY);

        then("API - full tree - tree overview only");
        assertProgress(apiFullTreeOverviewOnly, "full tree - tree overview only")
                .display()
                .assertItems(46, 10, null);

        when("API - full tree - tree overview preferred");
        var apiFullTreeOverviewPreferred =
                ActivityProgressInformation.fromRootTask(fullTreeBean, TREE_OVERVIEW_PREFERRED);

        then("API - full tree - tree overview preferred");
        assertProgress(apiFullTreeOverviewPreferred, "full tree - tree overview preferred")
                .display()
                .assertItems(46, 10, null);

        when("API - full tree - full state preferred");
        var apiFullTreeFullPreferred =
                ActivityProgressInformation.fromRootTask(fullTreeBean, FULL_STATE_PREFERRED);

        then("API - full tree - full state preferred");
        assertProgress(apiFullTreeFullPreferred, "full tree - full state preferred")
                .display()
                .assertItems(46, 10, null);

        when("API - full tree - full state only");
        var apiFullTreeFullOnly =
                ActivityProgressInformation.fromRootTask(fullTreeBean, FULL_STATE_ONLY);

        then("API - full tree - full state only");
        assertProgress(apiFullTreeFullOnly, "full tree - full state preferred")
                .display()
                .assertItems(46, 10, null);
    }
}
