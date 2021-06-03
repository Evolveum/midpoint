/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.task.TaskOperationStatsUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.WorkDefinitionUtil;
import com.evolveum.midpoint.schema.util.task.WorkDefinitionWrapper;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * FIXME
 *
 * Tests basic features of work state management:
 *
 * - basic creation of work buckets
 * - allocation, completion, release of buckets
 * - allocation of buckets when some workers are suspended
 * - basic propagation of buckets into bucket-aware task handler
 *
 * Both in coordinator-worker and standalone tasks.
 */

@ContextConfiguration(locations = "classpath:ctx-repo-common-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestActivities extends AbstractRepoCommonTest {

    // TODO enable
//    private static final long DEFAULT_TIMEOUT = 30000L;
//
    private static final File TEST_DIR = new File("src/test/resources/tasks/activities");

    private static final TestResource<TaskType> TASK_MOCK_SIMPLE_LEGACY = new TestResource<>(TEST_DIR, "task-mock-simple-legacy.xml", "7523433a-a537-4943-96e9-58b6c57566e8");
    private static final TestResource<TaskType> TASK_MOCK_COMPOSITE_LEGACY = new TestResource<>(TEST_DIR, "task-mock-composite-legacy.xml", "b5fd4ecf-2163-4079-99ec-d56e8a96ca94");
    private static final TestResource<TaskType> TASK_MOCK_SIMPLE = new TestResource<>(TEST_DIR, "task-mock-simple.xml", "6a1a58fa-ce09-495d-893f-3093cdcc00b6");
    private static final TestResource<TaskType> TASK_MOCK_COMPOSITE = new TestResource<>(TEST_DIR, "task-mock-composite.xml", "14a41fca-a664-450c-bc5d-d4ce35045346");
    private static final TestResource<TaskType> TASK_PURE_COMPOSITE = new TestResource<>(TEST_DIR, "task-pure-composite.xml", "65866e01-73cd-4249-9b7b-03ebc4413bd0");
    private static final TestResource<TaskType> TASK_MOCK_ITERATIVE = new TestResource<>(TEST_DIR, "task-mock-iterative.xml", "c21785e9-1c67-492f-bc79-0c51f74561a1");
    private static final TestResource<TaskType> TASK_MOCK_SEARCH_ITERATIVE = new TestResource<>(TEST_DIR, "task-mock-search-iterative.xml", "9d8384b3-a007-44e2-a9f7-084a64bdc285");
    private static final TestResource<TaskType> TASK_MOCK_BUCKETED = new TestResource<>(TEST_DIR, "task-mock-bucketed.xml", "04e257d1-bb25-4675-8e00-f248f164fbc3");
    private static final TestResource<TaskType> TASK_BUCKETED_TREE = new TestResource<>(TEST_DIR, "task-bucketed-tree.xml", "ac3220c5-6ded-4b94-894e-9ed39c05db66");

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
    @Autowired private MockRecorder recorder;

    private static final int ROLES = 100;
    private static final String ROLE_NAME_PATTERN = "r%02d";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        createRoles(initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
    }

    private void createRoles(OperationResult result) throws SchemaException, ObjectAlreadyExistsException {
        for (int i = 0; i < ROLES; i++) {
            RoleType role = new RoleType(prismContext)
                    .name(String.format(ROLE_NAME_PATTERN, i));
            repositoryService.addObject(role.asPrismObject(), null, result);
        }
    }

    @Test
    public void test000Sanity() throws Exception {
        when();

        PrismObject<TaskType> task = prismContext.parserFor(TASK_MOCK_COMPOSITE.file).parse();
        ActivityDefinitionType activityDefinition = task.asObjectable().getActivity();

        then();

        List<WorkDefinitionWrapper> values = WorkDefinitionUtil.getWorkDefinitions(activityDefinition.getWork());
        displayValue("Work definitions found", values);

        Collection<QName> types = WorkDefinitionUtil.getWorkDefinitionTypeNames(activityDefinition.getWork());
        displayValue("Actions types found", types);

        // TODO asserts
    }

    @Test
    public void test100RunSimpleLegacyTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_MOCK_SIMPLE_LEGACY, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        display("task after", task1);
        assertSuccess(task1.getResult());

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("executions").containsExactly("msg1");
    }

    @Test
    public void test110RunCompositeLegacyTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_MOCK_COMPOSITE_LEGACY, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        display("task after", task1);
        assertSuccess(task1.getResult());

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("executions").containsExactly("id1:opening", "id1:closing");
    }

    @Test
    public void test130RunSimpleTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_MOCK_SIMPLE, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        display("task after", task1);
        assertSuccess(task1.getResult());

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("executions").containsExactly("msg1");
    }

    @Test
    public void test140RunCompositeTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_MOCK_COMPOSITE, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        display("task after", task1);
        assertSuccess(task1.getResult());

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("executions").containsExactly("id1:opening", "id1:closing");
    }

    @Test
    public void test140RunPureCompositeTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_PURE_COMPOSITE, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        display("task after", task1);
        assertSuccess(task1.getResult());

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("recorder")
                .containsExactly("A:opening", "A:closing", "Hello", "B:opening", "B:closing", "C:closing");
    }

    @Test
    public void test150RunMockIterativeTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_MOCK_ITERATIVE, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        display("task after", task1);
        assertSuccess(task1.getResult());

        OperationStatsType stats = task1.getStoredOperationStatsOrClone();
        displayValue("statistics", TaskOperationStatsUtil.format(stats));
        assertThat(stats.getIterationInformation().getProcessed().get(0).getCount())
                .as("count of processed items in first activity")
                .isEqualTo(5);

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("recorder")
                .containsExactly("Item: 1", "Item: 2", "Item: 3", "Item: 4", "Item: 5");
    }

    @Test
    public void test160RunMockSearchIterativeTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_MOCK_SEARCH_ITERATIVE, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        display("task after", task1);
        assertSuccess(task1.getResult());

        OperationStatsType stats = task1.getStoredOperationStatsOrClone();
        displayValue("statistics", TaskOperationStatsUtil.format(stats));
        assertThat(stats.getIterationInformation().getProcessed().get(0).getCount())
                .as("count of processed items in first activity")
                .isEqualTo(100);

        displayDumpable("recorder", recorder);
        Set<String> messages = IntStream.range(0, 100)
                .mapToObj(i -> String.format("Role: " + ROLE_NAME_PATTERN, i))
                .collect(Collectors.toSet());
        assertThat(recorder.getExecutions()).as("recorder")
                .containsExactlyInAnyOrderElementsOf(messages);
    }

    @Test
    public void test170RunBucketedTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_MOCK_BUCKETED, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        display("task after", task1);
        assertSuccess(task1.getResult());

        OperationStatsType stats = task1.getStoredOperationStatsOrClone();
        displayValue("statistics", TaskOperationStatsUtil.format(stats));
        assertThat(stats.getIterationInformation().getProcessed().get(0).getCount())
                .as("count of processed items in first activity")
                .isEqualTo(100);

        displayDumpable("recorder", recorder);
        Set<String> messages = IntStream.range(0, 100)
                .mapToObj(i -> String.format("Role: " + ROLE_NAME_PATTERN, i))
                .collect(Collectors.toSet());
        assertThat(recorder.getExecutions()).as("recorder")
                .containsExactlyInAnyOrderElementsOf(messages);

        // TODO assert the bucketing
    }

    @Test
    public void test180RunBucketedTree() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_BUCKETED_TREE, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        display("task after", task1);
        assertSuccess(task1.getResult());

        OperationStatsType stats = task1.getStoredOperationStatsOrClone();
        displayValue("statistics", TaskOperationStatsUtil.format(stats));
//        assertThat(stats.getIterativeTaskInformation().getPart().get(0).getProcessed().get(0).getCount())
//                .as("count of processed items in first activity")
//                .isEqualTo(100);

        displayDumpable("recorder", recorder);
        Set<String> messages = new HashSet<>();
        // Nothing from the first activity (no system config in repo)
        messages.add("Second (user): administrator");
        messages.addAll(
                IntStream.range(10, 20)
                        .mapToObj(i -> String.format("Third-A (role): " + ROLE_NAME_PATTERN, i))
                        .collect(Collectors.toSet()));
        messages.add("Third-B (user): administrator");
        messages.addAll(
                IntStream.range(0, 100)
                        .mapToObj(i -> String.format("Fourth (role): " + ROLE_NAME_PATTERN, i))
                        .collect(Collectors.toSet()));

        assertThat(recorder.getExecutions()).as("recorder")
                .containsExactlyInAnyOrderElementsOf(messages);

        task1.setResult(null);
        displayValue("task after (XML)", prismContext.xmlSerializer().serialize(task1.getRawTaskObjectClone()));

        // TODO assert the bucketing
    }

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
