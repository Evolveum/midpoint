/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.tasks;

import static com.evolveum.midpoint.repo.common.tasks.handlers.CommonMockActivityHelper.EXECUTION_COUNT_NAME;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.task.*;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper;
import com.evolveum.midpoint.task.api.TaskDebugUtil;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;
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

    private static final TestResource<TaskType> TASK_100_MOCK_SIMPLE_LEGACY = new TestResource<>(TEST_DIR, "task-100-mock-simple-legacy.xml", "7523433a-a537-4943-96e9-58b6c57566e8");
    private static final TestResource<TaskType> TASK_110_MOCK_COMPOSITE_LEGACY = new TestResource<>(TEST_DIR, "task-110-mock-composite-legacy.xml", "b5fd4ecf-2163-4079-99ec-d56e8a96ca94");
    private static final TestResource<TaskType> TASK_120_MOCK_SIMPLE = new TestResource<>(TEST_DIR, "task-120-mock-simple.xml", "6a1a58fa-ce09-495d-893f-3093cdcc00b6");
    private static final TestResource<TaskType> TASK_130_MOCK_COMPOSITE = new TestResource<>(TEST_DIR, "task-130-mock-composite.xml", "14a41fca-a664-450c-bc5d-d4ce35045346");
    private static final TestResource<TaskType> TASK_140_PURE_COMPOSITE = new TestResource<>(TEST_DIR, "task-140-pure-composite.xml", "65866e01-73cd-4249-9b7b-03ebc4413bd0");
    private static final TestResource<TaskType> TASK_150_MOCK_ITERATIVE = new TestResource<>(TEST_DIR, "task-150-mock-iterative.xml", "c21785e9-1c67-492f-bc79-0c51f74561a1");
    private static final TestResource<TaskType> TASK_160_MOCK_SEARCH_ITERATIVE = new TestResource<>(TEST_DIR, "task-160-mock-search-iterative.xml", "9d8384b3-a007-44e2-a9f7-084a64bdc285");
    private static final TestResource<TaskType> TASK_170_MOCK_BUCKETED = new TestResource<>(TEST_DIR, "task-170-mock-bucketed.xml", "04e257d1-bb25-4675-8e00-f248f164fbc3");
    private static final TestResource<TaskType> TASK_180_BUCKETED_TREE = new TestResource<>(TEST_DIR, "task-180-bucketed-tree.xml", "ac3220c5-6ded-4b94-894e-9ed39c05db66");
    private static final TestResource<TaskType> TASK_190_SUSPENDING_COMPOSITE = new TestResource<>(TEST_DIR, "task-190-suspending-composite.xml", "1e7cf975-7253-4991-a707-661d3c52f203");
    private static final TestResource<TaskType> TASK_200_SUBTASK = new TestResource<>(TEST_DIR, "task-200-subtask.xml", "ee60863e-ff77-4edc-9e4e-2e1ea7853478");
    private static final TestResource<TaskType> TASK_210_SUSPENDING_COMPOSITE_WITH_SUBTASKS = new TestResource<>(TEST_DIR, "task-210-suspending-composite-with-subtasks.xml", "cd36ca66-cd49-44cf-9eb2-36928acbe1fd");
    private static final TestResource<TaskType> TASK_220_MOCK_COMPOSITE_WITH_SUBTASKS = new TestResource<>(TEST_DIR, "task-220-mock-composite-with-subtasks.xml", "");
    private static final TestResource<TaskType> TASK_300_WORKERS_SIMPLE = new TestResource<>(TEST_DIR, "task-300-workers-simple.xml", "5cfa521a-a174-4254-a5cb-199189fe42d5");

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
        bucketingManager.setFreeBucketWaitIntervalOverride(100L);
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

        PrismObject<TaskType> task = prismContext.parserFor(TASK_130_MOCK_COMPOSITE.file).parse();
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

        Task task1 = taskAdd(TASK_100_MOCK_SIMPLE_LEGACY, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        assertTask(task1, "after")
                .display()
                .assertClosed()
                .assertSuccess()
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete();

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("executions").containsExactly("msg1");

        assertProgress(task1.getOid(), "after")
                .display()
                .assertRealizationState(ActivityProgressInformation.RealizationState.COMPLETE)
                .assertNoBucketInformation()
                .assertItems(1, null);

        assertPerformance(task1.getOid(), "after")
                .display()
                .assertItemsProcessed(1)
                .assertErrors(0)
                .assertProgress(1)
                .assertHasWallClockTime()
                .assertHasThroughput();
    }

    @Test
    public void test110RunCompositeLegacyTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_110_MOCK_COMPOSITE_LEGACY, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        assertTask(task1, "after")
                .display()
                .assertClosed()
                .assertSuccess()
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete();

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("executions").containsExactly("id1:opening", "id1:closing");

        assertProgress(task1.getOid(), "after")
                .display()
                .assertComplete()
                .assertNoBucketInformation()
                .assertNoItemsInformation()
                .assertChildren(2)
                .child("opening")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertItems(1, null)
                .end()
                .child("closing")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertItems(1, null);

        assertPerformance(task1.getOid(), "after")
                .display()
                .assertNotApplicable()
                .assertChildren(2)
                .child("opening")
                    .assertItemsProcessed(1)
                    .assertErrors(0)
                    .assertProgress(1)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end()
                .child("closing")
                    .assertItemsProcessed(1)
                    .assertErrors(0)
                    .assertProgress(1)
                    .assertHasWallClockTime()
                    .assertHasThroughput();
    }

    /**
     * Runs the task twice to check the purger.
     */
    @Test
    public void test120RunSimpleTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_120_MOCK_SIMPLE, result);

        when("run 1");

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then("run 1");

        task1.refresh(result);
        assertTask(task1, "after run 1")
                .display()
                .assertClosed()
                .assertSuccess()
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete();

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("executions").containsExactly("msg1");

        dumpProgressAndPerformanceInfo(task1.getOid(), result);

        when("run 2");

        restartTask(task1.getOid(), result);
        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then("run 2");

        task1.refresh(result);
        assertTask(task1, "after run 2")
                .display()
                .assertClosed()
                .assertSuccess()
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete();

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("executions").containsExactly("msg1", "msg1");

        dumpProgressAndPerformanceInfo(task1.getOid(), result);

    }

    @Test
    public void test130RunCompositeTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_130_MOCK_COMPOSITE, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        assertTask(task1, "after")
                .display()
                .assertClosed()
                .assertSuccess()
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete();

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("executions").containsExactly("id1:opening", "id1:closing");

    }

    /**
     * Run twice to check state purge.
     */
    @Test
    public void test140RunPureCompositeTask() throws Exception {

        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();
        Task root = taskAdd(TASK_140_PURE_COMPOSITE, result);
        List<String> expectedExecutions1 = List.of("A:opening", "A:closing", "Hello", "B:opening", "B:closing", "C:closing");
        List<String> expectedExecutions2 = ListUtils.union(expectedExecutions1, expectedExecutions1);

        execute140RunPureCompositeTaskOnce(root, "run 1", expectedExecutions1);
        execute140RunPureCompositeTaskOnce(root, "run 2", expectedExecutions2);
    }

    private void execute140RunPureCompositeTaskOnce(Task root, String label, List<String> expectedExecutions)
            throws CommonException {

        Task task = getTestTask();
        OperationResult result = task.getResult();

        when(label);

        waitForTaskClose(root.getOid(), result, 10000, 200);

        then(label);

        root.refresh(result);
        assertTask(root, label)
                .display()
                .assertClosed()
                .assertSuccess()
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete();

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("recorder")
                .containsExactlyElementsOf(expectedExecutions);

        // @formatter:off
        assertProgress( root.getOid(),label)
                .display()
                .assertComplete()
                .assertNoBucketInformation()
                .assertNoItemsInformation()
                .assertChildren(4)
                .child("mock-composite:1")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                    .assertChildren(2)
                    .child("opening")
                        .assertComplete()
                        .assertNoBucketInformation()
                        .assertItems(1, null)
                    .end()
                    .child("closing")
                        .assertComplete()
                        .assertNoBucketInformation()
                        .assertItems(1, null)
                    .end()
                .end()
                .child("mock-simple:1")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertItems(1, null)
                .end()
                .child("mock-composite:2")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                    .assertChildren(2)
                    .child("opening")
                        .assertComplete()
                        .assertNoBucketInformation()
                        .assertItems(1, null)
                    .end()
                    .child("closing")
                        .assertComplete()
                        .assertNoBucketInformation()
                        .assertItems(1, null)
                    .end()
                .end()
                .child("mock-composite:3")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                    .assertChildren(1)
                    .child("closing")
                        .assertComplete()
                        .assertNoBucketInformation()
                        .assertItems(1, null)
                    .end()
                .end();

        assertPerformance( root.getOid(),label)
                .display()
                .assertNotApplicable()
                .assertChildren(4)
                .child("mock-composite:1")
                    .assertNotApplicable()
                    .assertChildren(2)
                    .child("opening")
                        .assertItemsProcessed(1)
                        .assertErrors(0)
                        .assertProgress(1)
                        .assertHasWallClockTime()
                        .assertHasThroughput()
                    .end()
                    .child("closing")
                        .assertItemsProcessed(1)
                        .assertErrors(0)
                        .assertProgress(1)
                        .assertHasWallClockTime()
                        .assertHasThroughput()
                    .end()
                .end()
                .child("mock-simple:1")
                    .assertItemsProcessed(1)
                    .assertErrors(0)
                    .assertProgress(1)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end()
                .child("mock-composite:2")
                    .assertNotApplicable()
                    .assertChildren(2)
                    .child("opening")
                        .assertItemsProcessed(1)
                        .assertErrors(0)
                        .assertProgress(1)
                        .assertHasWallClockTime()
                        .assertHasThroughput()
                    .end()
                    .child("closing")
                        .assertItemsProcessed(1)
                        .assertErrors(0)
                        .assertProgress(1)
                        .assertHasWallClockTime()
                        .assertHasThroughput()
                    .end()
                .end()
                .child("mock-composite:3")
                    .assertNotApplicable()
                    .assertChildren(1)
                    .child("closing")
                        .assertItemsProcessed(1)
                        .assertErrors(0)
                        .assertProgress(1)
                        .assertHasWallClockTime()
                        .assertHasThroughput()
                    .end()
                .end();
        // @formatter:on
    }

    @Test
    public void test150RunMockIterativeTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_150_MOCK_ITERATIVE, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        assertTask(task1, "after")
                .display()
                .assertSuccess()
                .assertClosed()
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete()
                        .assertSuccess()
                        .progress()
                            .assertUncommitted(5, 0, 0) // maybe in the future we may move these to committed on activity close
                            .assertNoCommitted()
                        .end()
                        .itemProcessingStatistics()
                            .assertTotalCounts(5, 0, 0)
                            .assertLastSuccessObjectName("5")
                            .assertExecutions(1);

        OperationStatsType stats = task1.getStoredOperationStatsOrClone();
        displayValue("task statistics", TaskOperationStatsUtil.format(stats));

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("recorder")
                .containsExactly("Item: 1", "Item: 2", "Item: 3", "Item: 4", "Item: 5");

        assertProgress(task1.getOid(), "after")
                .display()
                .assertComplete()
                .assertItems(5, null);
        assertPerformance(task1.getOid(), "after")
                .display()
                .assertItemsProcessed(5)
                .assertErrors(0)
                .assertProgress(5)
                .assertHasWallClockTime();
    }

    @Test
    public void test160RunMockSearchIterativeTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_160_MOCK_SEARCH_ITERATIVE, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        assertTask(task1, "after")
                .display()
                .assertSuccess()
                .assertClosed()
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete()
                        .assertSuccess()
                        .progress()
                            .assertCommitted(100, 0, 0) // maybe in the future we may move these to committed on activity close
                            .assertNoUncommitted()
                        .end()
                        .itemProcessingStatistics()
                            .assertTotalCounts(100, 0, 0)
                            .assertExecutions(1)
                        .end()
                        .assertBucketManagementStatisticsOperations(3);

        OperationStatsType stats = task1.getStoredOperationStatsOrClone();
        displayValue("statistics", TaskOperationStatsUtil.format(stats));

        displayDumpable("recorder", recorder);
        Set<String> messages = IntStream.range(0, 100)
                .mapToObj(i -> String.format("Role: " + ROLE_NAME_PATTERN, i))
                .collect(Collectors.toSet());
        assertThat(recorder.getExecutions()).as("recorder")
                .containsExactlyInAnyOrderElementsOf(messages);

        assertProgress(task1.getOid(), "after")
                .display()
                .assertComplete()
                .assertBuckets(1, 1)
                .assertItems(100, null); // TODO expected
        assertPerformance(task1.getOid(), "after")
                .display()
                .assertItemsProcessed(100)
                .assertErrors(0)
                .assertProgress(100)
                .assertHasWallClockTime();
    }

    @Test
    public void test170RunBucketedTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_170_MOCK_BUCKETED, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        assertTask(task1, "after")
                .display()
                .assertSuccess()
                .assertClosed()
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete()
                        .assertSuccess()
                        .progress()
                            .assertCommitted(100, 0, 0) // maybe in the future we may move these to committed on activity close
                            .assertNoUncommitted()
                        .end()
                        .itemProcessingStatistics()
                            .assertTotalCounts(100, 0, 0)
                            .assertExecutions(1)
                        .end()
                        .assertBucketManagementStatisticsOperations(3);

        OperationStatsType stats = task1.getStoredOperationStatsOrClone();
        displayValue("statistics", TaskOperationStatsUtil.format(stats));

        displayDumpable("recorder", recorder);
        Set<String> messages = IntStream.range(0, 100)
                .mapToObj(i -> String.format("Role: " + ROLE_NAME_PATTERN, i))
                .collect(Collectors.toSet());
        assertThat(recorder.getExecutions()).as("recorder")
                .containsExactlyInAnyOrderElementsOf(messages);

        // TODO assert the bucketing

        assertProgress(task1.getOid(), "after")
                .display()
                .assertComplete()
                .assertBuckets(11, 11)
                .assertItems(100, null); // TODO expected
        assertPerformance(task1.getOid(), "after")
                .display()
                .assertItemsProcessed(100)
                .assertErrors(0)
                .assertProgress(100)
                .assertHasWallClockTime();
    }

    @Test
    public void test180RunBucketedTree() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_180_BUCKETED_TREE, result);

        when();

        waitForTaskClose(task1.getOid(), result, 20000, 1000);

        then();

        task1.refresh(result);
        assertTask(task1, "after")
                .display()
                .assertSuccess()
                .assertClosed()
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete()
                        .assertSuccess()
                        .assertNoProgress()
                        .assertChildren(4)
                        .child("first")
                            .assertComplete()
                            .assertSuccess()
                            .progress() // no objects here
                                .assertNoCommitted()
                                .assertNoUncommitted()
                            .end()
                            .itemProcessingStatistics()
                                .assertTotalCounts(0, 0, 0)
                                .assertExecutions(1)
                            .end()
                            .assertBucketManagementStatisticsOperations(3)
                        .end()
                        .child("second")
                            .assertComplete()
                            .assertSuccess()
                            .progress() // 1 user here
                                .assertCommitted(1, 0, 0)
                                .assertNoUncommitted()
                            .end()
                            .itemProcessingStatistics()
                                .assertTotalCounts(1, 0, 0)
                                .assertLastSuccessObjectName("administrator")
                                .assertExecutions(1)
                            .end()
                            .assertBucketManagementStatisticsOperations(3)
                        .end()
                        .child("composition:1")
                            .assertComplete()
                            .assertSuccess()
                            .assertNoProgress()
                            .assertChildren(2)
                            .child("third-A")
                                .assertComplete()
                                .assertSuccess()
                                .progress() // roles r1*
                                    .assertCommitted(10, 0, 0)
                                    .assertNoUncommitted()
                                .end()
                                .itemProcessingStatistics()
                                    .assertTotalCounts(10, 0, 0)
                                    .assertExecutions(1)
                                .end()
                                .assertBucketManagementStatisticsOperations(3)
                            .end()
                            .child("third-B")
                                .assertComplete()
                                .assertSuccess()
                                .progress() // users
                                    .assertCommitted(1, 0, 0)
                                    .assertNoUncommitted()
                                .end()
                                .itemProcessingStatistics()
                                    .assertTotalCounts(1, 0, 0)
                                    .assertLastSuccessObjectName("administrator")
                                    .assertExecutions(1)
                                .end()
                                .assertBucketManagementStatisticsOperations(3)
                            .end()
                        .end()
                        .child("fourth")
                            .assertComplete()
                            .assertSuccess()
                            .progress() // roles r* (100 buckets)
                                .assertCommitted(100, 0, 0)
                                .assertNoUncommitted()
                            .end()
                            .itemProcessingStatistics()
                                .assertTotalCounts(100, 0, 0)
                                .assertExecutions(1)
                            .end()
                            .assertBucketManagementStatisticsOperations(3)
                        .end();

        // TODO assert the bucketing

        OperationStatsType stats = task1.getStoredOperationStatsOrClone();
        displayValue("statistics", TaskOperationStatsUtil.format(stats));

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

        // @formatter:off
        assertProgress( task1.getOid(),"after")
                .display()
                .assertComplete()
                .assertNoBucketInformation()
                .assertNoItemsInformation()
                .assertChildren(4)
                .child("first") // 0 configs
                    .assertComplete()
                    .assertBuckets(1, 1)
                    .assertItems(0, null)
                    .assertNoChildren()
                .end()
                .child("second") // 1 user
                    .assertComplete()
                    .assertBuckets(1, 1)
                    .assertItems(1, null)
                .end()
                .child("composition:1")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                    .assertChildren(2)
                    .child("third-A")
                        .assertComplete()
                        .assertBuckets(11, 11)
                        .assertItems(10, null)
                    .end()
                    .child("third-B")
                        .assertComplete()
                        .assertBuckets(1, 1)
                        .assertItems(1, null)
                    .end()
                .end()
                .child("fourth")
                    .assertComplete()
                    .assertBuckets(101, 101)
                    .assertItems(100, null)
                    .assertNoChildren()
                .end();

        assertPerformance( task1.getOid(),"after")
                .display()
                .assertNotApplicable()
                .assertChildren(4)
                .child("first") // 0 configs
                    .assertItemsProcessed(0)
                    .assertErrors(0)
                    .assertProgress(0)
                    .assertHasWallClockTime()
                    .assertNoThroughput()
                .end()
                .child("second") // 1 user
                    .assertItemsProcessed(1)
                    .assertErrors(0)
                    .assertProgress(1)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end()
                .child("composition:1")
                    .assertNotApplicable()
                    .assertChildren(2)
                    .child("third-A")
                        .assertItemsProcessed(10)
                        .assertErrors(0)
                        .assertProgress(10)
                        .assertHasWallClockTime()
                        .assertHasThroughput()
                    .end()
                    .child("third-B")
                        .assertItemsProcessed(1)
                        .assertErrors(0)
                        .assertProgress(1)
                        .assertHasWallClockTime()
                        .assertHasThroughput()
                    .end()
                .end()
                .child("fourth")
                    .assertItemsProcessed(100)
                    .assertErrors(0)
                    .assertProgress(100)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end();
        // @formatter:on
    }

    @Test
    public void test190SuspendingComposite() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();
        List<String> expectedRecords = new ArrayList<>();

        Task task1 = taskAdd(TASK_190_SUSPENDING_COMPOSITE, result);

        // ------------------------------------------------------------------------------------ run 1

        when("run 1");

        waitForTaskCloseOrSuspend(task1.getOid(), 10000, 200);

        then("run 1");

        task1.refresh(result);

        // @formatter:off
        assertTask(task1.getOid(), "after run 1")
                .display()
                .assertFatalError()
                .assertExecutionStatus(TaskExecutionStateType.SUSPENDED)
                .activityState()
                    .assertTreeRealizationInProgress()
                    .rootActivity()
                        .assertInProgressLocal()
                        .assertFatalError()
                        .assertChildren(3)
                        .child("mock-simple:1")
                            .assertInProgressLocal()
                            .assertFatalError()
                            .workStateExtension()
                                .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 1)
                            .end()
                            .progress()
                                .assertUncommitted(0, 1, 0)
                                .assertNoCommitted()
                            .end()
                            .itemProcessingStatistics()
                                .assertTotalCounts(0, 1, 0)
                                .assertLastFailureObjectName("#1")
                                .assertExecutions(1)
                            .end()
                        .end()
                        .child("composition:1")
                            .assertStatusInProgress() // TODO
                            .assertChildren(0)
                        .end()
                        .child("mock-simple:2")
                            .assertStatusInProgress() // TODO
                        .end();
        // @formatter:on

        displayDumpable("recorder after run 1", recorder);

        expectedRecords.add("#1"); // 1st failed attempt
        assertThat(recorder.getExecutions()).as("recorder after run 1")
                .containsExactlyElementsOf(expectedRecords);

        // @formatter:off
        assertProgress( task1.getOid(),"after")
                .display()
                .assertInProgress()
                .assertNoBucketInformation()
                .assertNoItemsInformation()
                .assertChildren(3)
                .child("mock-simple:1")
                    .assertInProgress()
                    .assertNoBucketInformation()
                    .assertItems(1, null)
                    .assertNoChildren()
                .end()
                .child("composition:1") // 1 user
                    .assertNotStarted()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                    .assertNoChildren()
                .end()
                .child("mock-simple:2")
                    .assertNotStarted()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                .end();

        assertPerformance( task1.getOid(),"after")
                .display()
                .assertNotApplicable()
                .assertChildren(3)
                .child("mock-simple:1") // 0 configs
                    .assertItemsProcessed(1)
                    .assertErrors(1)
                    .assertProgress(1)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end()
                .child("composition:1") // 1 user
                    .assertNotApplicable()
                    .assertNoChildren()
                .end()
                .child("mock-simple:2")
                    .assertNotApplicable()
                .end();
        // @formatter:on

        // ------------------------------------------------------------------------------------ run 2

        when("run 2");

        activityManager.clearFailedActivityState(task1.getOid(), result);
        restartTask(task1.getOid(), result);
        waitForTaskCloseOrSuspend(task1.getOid(), 10000, 200);

        then("run 2");

        task1.refresh(result);

        // @formatter:off
        assertTask(task1.getOid(), "after run 2")
                .display()
                //.assertFatalError() // TODO
                .assertExecutionStatus(TaskExecutionStateType.SUSPENDED)
                .activityState()
                    .assertTreeRealizationInProgress()
                    .rootActivity()
                        .assertInProgressLocal()
                        .assertFatalError()
                        .assertChildren(3)
                        .child("mock-simple:1")
                            .assertInProgressLocal()
                            .assertFatalError()
                            .workStateExtension()
                                .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 2)
                            .end()
                            .progress()
                                .assertUncommitted(0, 1, 0)
                                .assertNoCommitted()
                            .end()
                            .itemProcessingStatistics()
                                .assertTotalCounts(0, 2, 0)
                                .assertLastFailureObjectName("#1")
                                .assertExecutions(2)
                            .end()
                        .end()
                        .child("composition:1")
                            .assertStatusInProgress() // TODO
                            .assertChildren(0)
                        .end()
                        .child("mock-simple:2")
                            .assertStatusInProgress() // TODO
                        .end();
        // @formatter:on

        displayDumpable("recorder after run 2", recorder);
        expectedRecords.add("#1"); // 2nd failed attempt
        assertThat(recorder.getExecutions()).as("recorder after run 2")
                .containsExactlyElementsOf(expectedRecords);

        // @formatter:off
        assertProgress( task1.getOid(),"after")
                .display()
                .assertInProgress()
                .assertNoBucketInformation()
                .assertNoItemsInformation()
                .assertChildren(3)
                .child("mock-simple:1")
                    .assertInProgress()
                    .assertNoBucketInformation()
                    .assertItems(1, null)
                    .assertNoChildren()
                .end()
                .child("composition:1") // 1 user
                    .assertNotStarted()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                    .assertNoChildren()
                .end()
                .child("mock-simple:2")
                    .assertNotStarted()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                .end();

        assertPerformance( task1.getOid(),"after")
                .display()
                .assertNotApplicable()
                .assertChildren(3)
                .child("mock-simple:1") // 0 configs
                    .assertItemsProcessed(2)
                    .assertErrors(2)
                    .assertProgress(1)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end()
                .child("composition:1") // 1 user
                    .assertNotApplicable()
                    .assertNoChildren()
                .end()
                .child("mock-simple:2")
                    .assertNotApplicable()
                .end();
        // @formatter:on

        // ------------------------------------------------------------------------------------ run 3

        when("run 3");

        activityManager.clearFailedActivityState(task1.getOid(), result);
        restartTask(task1.getOid(), result);
        waitForTaskCloseOrSuspend(task1.getOid(), 10000, 200);

        then("run 3");

        task1.refresh(result);

        // @formatter:off
        assertTask(task1.getOid(), "after run 3")
                .display()
                .assertFatalError()
                .assertExecutionStatus(TaskExecutionStateType.SUSPENDED)
                .activityState()
                    .assertTreeRealizationInProgress()
                    .rootActivity()
                        .assertInProgressLocal()
                        .assertFatalError()
                        .child("mock-simple:1")
                            .assertComplete()
                            .assertSuccess()
                            .workStateExtension()
                                .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 3)
                            .end()
                            .progress()
                                .assertUncommitted(1, 0, 0)
                                .assertNoCommitted()
                            .end()
                            .itemProcessingStatistics()
                                .assertTotalCounts(1, 2, 0)
                                .assertLastSuccessObjectName("#1")
                                .assertLastFailureObjectName("#1")
                                .assertExecutions(3)
                            .end()
                        .end()
                        .child("composition:1")
                            .assertInProgressLocal()
                            .assertFatalError()
                            .child("mock-simple:1")
                                .assertComplete()
                                .assertSuccess()
                                .workStateExtension()
                                    .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 1)
                                .end()
                            .end()
                            .child("mock-simple:2")
                                .assertInProgressLocal()
                                .assertFatalError()
                                .workStateExtension()
                                    .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 1)
                                .end()
                                .progress()
                                    .assertUncommitted(0, 1, 0)
                                    .assertNoCommitted()
                                .end()
                                .itemProcessingStatistics()
                                    .assertTotalCounts(0, 1, 0)
                                    .assertLastFailureObjectName("#2.2")
                                    .assertExecutions(1)
                                .end()
                            .end()
                        .end()
                        .child("mock-simple:2")
                            .assertNotStarted()
                            .assertStatusInProgress() // TODO
                        .end();
        // @formatter:on

        displayDumpable("recorder after run 3", recorder);
        expectedRecords.add("#1"); // success after 2 failures
        expectedRecords.add("#2.1"); // immediate success
        expectedRecords.add("#2.2"); // 1st failure
        assertThat(recorder.getExecutions()).as("recorder after run 3")
                .containsExactlyElementsOf(expectedRecords);

        dumpProgressAndPerformanceInfo(task1.getOid(), result);

        // ------------------------------------------------------------------------------------ run 4

        when("run 4");

        activityManager.clearFailedActivityState(task1.getOid(), result);
        restartTask(task1.getOid(), result);
        waitForTaskCloseOrSuspend(task1.getOid(), 10000, 200);

        then("run 4");

        task1.refresh(result);

        // @formatter:off
        assertTask(task1.getOid(), "after run 4")
                .display()
                .assertFatalError()
                .assertExecutionStatus(TaskExecutionStateType.SUSPENDED)
                .activityState()
                    .assertTreeRealizationInProgress()
                    .rootActivity()
                        .assertRealizationState(ActivityRealizationStateType.IN_PROGRESS_LOCAL)
                        .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                        .child("mock-simple:1")
                            .assertRealizationState(ActivityRealizationStateType.COMPLETE)
                            .assertResultStatus(OperationResultStatusType.SUCCESS)
                            .workStateExtension()
                                .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 3)
                            .end()
                        .end()
                        .child("composition:1")
                            .assertRealizationState(ActivityRealizationStateType.COMPLETE)
                            .assertResultStatus(OperationResultStatusType.SUCCESS)
                            .child("mock-simple:1")
                                .assertRealizationState(ActivityRealizationStateType.COMPLETE)
                                .assertResultStatus(OperationResultStatusType.SUCCESS)
                                .workStateExtension()
                                    .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 1)
                                .end()
                            .end()
                            .child("mock-simple:2")
                                .assertRealizationState(ActivityRealizationStateType.COMPLETE)
                                .assertResultStatus(OperationResultStatusType.SUCCESS)
                                .workStateExtension()
                                    .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 2)
                                .end()
                            .end()
                        .end()
                        .child("mock-simple:2") // this is not related to mock-simple:2 above!
                            .assertRealizationState(ActivityRealizationStateType.IN_PROGRESS_LOCAL)
                            .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                            .workStateExtension()
                                .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 1);
        // @formatter:on

        displayDumpable("recorder after run 4", recorder);
        expectedRecords.add("#2.2"); // success after 1 failure
        expectedRecords.add("#3"); // 1st failure
        assertThat(recorder.getExecutions()).as("recorder after run 4")
                .containsExactlyElementsOf(expectedRecords);

        dumpProgressAndPerformanceInfo(task1.getOid(), result);

        // ------------------------------------------------------------------------------------ run 5

        when("run 5");

        activityManager.clearFailedActivityState(task1.getOid(), result);
        restartTask(task1.getOid(), result);
        waitForTaskCloseOrSuspend(task1.getOid(), 10000, 200);

        then("run 5");

        task1.refresh(result);

        // @formatter:off
        assertTask(task1.getOid(), "after run 5")
                .display()
                .assertSuccess()
                .assertExecutionStatus(TaskExecutionStateType.CLOSED)
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertRealizationState(ActivityRealizationStateType.COMPLETE)
                        .assertResultStatus(OperationResultStatusType.SUCCESS)
                        .child("mock-simple:1")
                            .assertRealizationState(ActivityRealizationStateType.COMPLETE)
                            .assertResultStatus(OperationResultStatusType.SUCCESS)
                            .workStateExtension()
                                .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 3)
                            .end()
                        .end()
                        .child("composition:1")
                            .assertRealizationState(ActivityRealizationStateType.COMPLETE)
                            .assertResultStatus(OperationResultStatusType.SUCCESS)
                            .child("mock-simple:1")
                                .assertRealizationState(ActivityRealizationStateType.COMPLETE)
                                .assertResultStatus(OperationResultStatusType.SUCCESS)
                                .workStateExtension()
                                    .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 1)
                                .end()
                            .end()
                            .child("mock-simple:2")
                                .assertRealizationState(ActivityRealizationStateType.COMPLETE)
                                .assertResultStatus(OperationResultStatusType.SUCCESS)
                                .workStateExtension()
                                    .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 2)
                                .end()
                            .end()
                        .end()
                        .child("mock-simple:2") // this is not related to mock-simple:2 above!
                            .assertRealizationState(ActivityRealizationStateType.COMPLETE)
                            .assertResultStatus(OperationResultStatusType.SUCCESS)
                            .workStateExtension()
                                .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 2);
        // @formatter:on

        displayDumpable("recorder after run 5", recorder);
        expectedRecords.add("#3"); // success after 1 failure
        assertThat(recorder.getExecutions()).as("recorder after run 5")
                .containsExactlyElementsOf(expectedRecords);

        // @formatter:off
        assertProgress( task1.getOid(),"after")
                .display()
                .assertComplete()
                .assertNoBucketInformation()
                .assertNoItemsInformation()
                .assertChildren(3)
                .child("mock-simple:1")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertItems(1, null)
                    .assertNoChildren()
                .end()
                .child("composition:1") // 1 user
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                    .assertChildren(2)
                    .child("mock-simple:1")
                        .assertComplete()
                        .assertNoBucketInformation()
                        .assertItems(1, null)
                    .end()
                    .child("mock-simple:2")
                        .assertComplete()
                        .assertNoBucketInformation()
                        .assertItems(1, null)
                    .end()
                .end()
                .child("mock-simple:2")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertItems(1, null)
                    .assertNoChildren()
                .end();

        assertPerformance( task1.getOid(),"after")
                .display()
                .assertNotApplicable()
                .assertChildren(3)
                .child("mock-simple:1")
                    .assertItemsProcessed(3)
                    .assertErrors(2)
                    .assertProgress(1)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end()
                .child("composition:1")
                    .assertNotApplicable()
                    .assertChildren(2)
                    .child("mock-simple:1")
                        .assertItemsProcessed(1)
                        .assertErrors(0)
                        .assertProgress(1)
                        .assertHasWallClockTime()
                        .assertHasThroughput()
                    .end()
                    .child("mock-simple:2")
                        .assertItemsProcessed(2)
                        .assertErrors(1)
                        .assertProgress(1)
                        .assertHasWallClockTime()
                        .assertHasThroughput()
                    .end()
                .end()
                .child("mock-simple:2")
                    .assertItemsProcessed(2)
                    .assertErrors(1)
                    .assertProgress(1)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end();
        // @formatter:on
    }

    @Test
    public void test200Subtask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_200_SUBTASK, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);

        assertTaskTree(task1.getOid(), "after")
                .display("root")
                .assertSuccess()
                .assertClosed()
                .activityState()
                    .rootActivity()
                        .assertComplete()
                        .assertSuccess()
                        .assertDelegationWorkStateWithTaskRef()
                        .assertNoChildren()
                    .end()
                .end()
                .subtaskForPath(ActivityPath.empty())
                    .display("child")
                    .activityState()
                        .assertRole(ActivityExecutionRoleType.DELEGATE)
                        .assertLocalRoot(ActivityPath.empty())
                        .rootActivity()
                            .assertComplete()
                            .assertSuccess()
                            .progress()
                                .assertUncommitted(1, 0, 0)
                                .assertNoCommitted()
                            .end()
                            .itemProcessingStatistics()
                                .assertTotalCounts(1, 0, 0)
                                .assertExecutions(1)
                            .end()
                            .workStateExtension()
                                .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 1)
                            .end();

        OperationStatsType stats = task1.getStoredOperationStatsOrClone();
        displayValue("statistics", TaskOperationStatsUtil.format(stats));

        displayDumpable("recorder", recorder);

        assertProgress(task1.getOid(), "after") // This is derived from the subtask
                .display()
                .assertComplete()
                .assertNoBucketInformation()
                .assertItems(1, null);

        assertPerformance(task1.getOid(), "after") // This is derived from the subtask
                .display()
                .assertItemsProcessed(1)
                .assertErrors(0)
                .assertProgress(1)
                .assertHasWallClockTime()
                .assertHasThroughput();
    }

    @Test
    public void test210SuspendingCompositeWithSubtasks() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();
        List<String> expectedRecords = new ArrayList<>();

        Task root = taskAdd(TASK_210_SUSPENDING_COMPOSITE_WITH_SUBTASKS, result);

        // ------------------------------------------------------------------------------------ run 1

        when("run 1");

        waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, 10000, 500);

        then("run 1");

        root.refresh(result);
        displayValue("Task tree", TaskDebugUtil.dumpTaskTree(root, result));

        // @formatter:off
        String oidOfSubtask1 = assertTaskTree(root.getOid(), "after run 1")
                .display()
                // state?
                .assertExecutionStatus(TaskExecutionStateType.RUNNING)
                .assertSchedulingState(TaskSchedulingStateType.WAITING)
                .activityState()
                    .assertTreeRealizationInProgress()
                    .rootActivity()
                        .assertRealizationState(ActivityRealizationStateType.IN_PROGRESS_LOCAL)
                        .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                        .child("mock-simple:1")
                            .assertRealizationState(ActivityRealizationStateType.IN_PROGRESS_DELEGATED)
                            .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                        .end()
                    .end()
                .end()
                .subtask(0)
                    .display()
                    .assertExecutionStatus(TaskExecutionStateType.SUSPENDED)
                    .assertSchedulingState(TaskSchedulingStateType.SUSPENDED)
                    .assertFatalError()
                    .activityState()
                        .rootActivity()
                            .assertRealizationState(ActivityRealizationStateType.IN_PROGRESS_LOCAL)
                            .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                            .workStateExtension()
                                .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 1)
                            .end()
                        .end()
                    .end()
                    .getObject().getOid();
        // @formatter:on

        displayDumpable("recorder after run 1", recorder);

        expectedRecords.add("#1"); // 1st failed attempt
        assertThat(recorder.getExecutions()).as("recorder after run 1")
                .containsExactlyElementsOf(expectedRecords);

        // @formatter:off
        assertProgress( root.getOid(),"after")
                .display()
                .assertInProgress()
                .assertNoBucketInformation()
                .assertNoItemsInformation()
                .assertChildren(3)
                .child("mock-simple:1")
                    .assertInProgress()
                    .assertNoBucketInformation()
                    .assertItems(1, null)
                    .assertNoChildren()
                .end()
                .child("composition:1") // 1 user
                    .assertNotStarted()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                    .assertNoChildren()
                .end()
                .child("mock-simple:2")
                    .assertNotStarted()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                .end();

        assertPerformance( root.getOid(),"after")
                .display()
                .assertNotApplicable()
                .assertChildren(3)
                .child("mock-simple:1") // 0 configs
                    .assertItemsProcessed(1)
                    .assertErrors(1)
                    .assertProgress(1)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end()
                .child("composition:1") // 1 user
                    .assertNotApplicable()
                    .assertNoChildren()
                .end()
                .child("mock-simple:2")
                    .assertNotApplicable()
                .end();
        // @formatter:on

        // ------------------------------------------------------------------------------------ run 2

        when("run 2");

        activityManager.clearFailedActivityState(root.getOid(), result);
        taskManager.resumeTask(oidOfSubtask1, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, 10000, 500);

        then("run 2");

        root.refresh(result);
        displayValue("Task tree", TaskDebugUtil.dumpTaskTree(root, result));

        // @formatter:off
        assertTaskTree(root.getOid(), "after run 2")
                .display()
                // state?
                .assertExecutionStatus(TaskExecutionStateType.RUNNING)
                .assertSchedulingState(TaskSchedulingStateType.WAITING)
                .activityState()
                    .assertTreeRealizationInProgress()
                    .rootActivity()
                        .assertRealizationState(ActivityRealizationStateType.IN_PROGRESS_LOCAL)
                        .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                        .child("mock-simple:1")
                            .assertRealizationState(ActivityRealizationStateType.IN_PROGRESS_DELEGATED)
                            .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                        .end()
                    .end()
                .end()
                .subtask(0)
                    .display()
                    .assertExecutionStatus(TaskExecutionStateType.SUSPENDED)
                    .assertSchedulingState(TaskSchedulingStateType.SUSPENDED)
                    .assertFatalError()
                    .activityState()
                        .rootActivity()
                            .assertRealizationState(ActivityRealizationStateType.IN_PROGRESS_LOCAL)
                            .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                            .workStateExtension()
                                .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 2)
                            .end()
                        .end()
                    .end();
        // @formatter:on

        displayDumpable("recorder after run 2", recorder);
        expectedRecords.add("#1"); // 2nd failed attempt
        assertThat(recorder.getExecutions()).as("recorder after run 2")
                .containsExactlyElementsOf(expectedRecords);

        // @formatter:off
        assertProgress( root.getOid(),"after")
                .display()
                .assertInProgress()
                .assertNoBucketInformation()
                .assertNoItemsInformation()
                .assertChildren(3)
                .child("mock-simple:1")
                    .assertInProgress()
                    .assertNoBucketInformation()
                    .assertItems(1, null)
                    .assertNoChildren()
                .end()
                .child("composition:1") // 1 user
                    .assertNotStarted()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                    .assertNoChildren()
                .end()
                .child("mock-simple:2")
                    .assertNotStarted()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                .end();

        assertPerformance( root.getOid(),"after")
                .display()
                .assertNotApplicable()
                .assertChildren(3)
                .child("mock-simple:1") // 0 configs
                    .assertItemsProcessed(2)
                    .assertErrors(2)
                    .assertProgress(1)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end()
                .child("composition:1") // 1 user
                    .assertNotApplicable()
                    .assertNoChildren()
                .end()
                .child("mock-simple:2")
                    .assertNotApplicable()
                .end();
        // @formatter:on

        // ------------------------------------------------------------------------------------ run 3

        when("run 3");

        activityManager.clearFailedActivityState(root.getOid(), result);
        taskManager.resumeTask(oidOfSubtask1, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, 10000, 500);

        then("run 3");

        root.refresh(result);
        displayValue("Task tree", TaskDebugUtil.dumpTaskTree(root, result));

        // @formatter:off
        String oidOfSubtask22 = assertTaskTree(root.getOid(), "after run 3")
                .display()
                // state?
                .assertExecutionStatus(TaskExecutionStateType.RUNNING)
                .assertSchedulingState(TaskSchedulingStateType.WAITING)
                .activityState()
                    .assertTreeRealizationInProgress()
                    .rootActivity()
                        .assertRealizationState(ActivityRealizationStateType.IN_PROGRESS_LOCAL)
                        .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                        .child("mock-simple:1")
                            .assertRealizationState(ActivityRealizationStateType.COMPLETE)
                            .assertResultStatus(OperationResultStatusType.SUCCESS)
//                            .assertHasTaskRef()
                        .end()
                        .child("composition:1")
                            .assertRealizationState(ActivityRealizationStateType.IN_PROGRESS_DELEGATED)
                            .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
//                            .assertHasTaskRef()
                        .end()
                    .end()
                .end()
                .subtaskForPath(ActivityPath.fromId("mock-simple:1"))
                    .display()
                    .assertClosed()
                    .assertSuccess()
                    .activityState()
                        .rootActivity()
                            .assertRealizationState(ActivityRealizationStateType.COMPLETE)
                            .assertResultStatus(OperationResultStatusType.SUCCESS)
//                            .assertNoTaskRef()
                            .workStateExtension()
                                .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 3)
                            .end()
                        .end()
                    .end()
                .end()
                .subtaskForPath(ActivityPath.fromId("composition:1"))
                    .display()
                    .subtaskForPath(ActivityPath.fromId("composition:1", "mock-simple:1"))
                        .display()
                        .assertClosed()
                    .end()
                    .subtaskForPath(ActivityPath.fromId("composition:1", "mock-simple:2"))
                        .display()
                        .assertSuspended()
                        .getOid();

        // @formatter:on

        displayDumpable("recorder after run 3", recorder);
        expectedRecords.add("#1"); // success after 2 failures
        expectedRecords.add("#2.1"); // immediate success
        expectedRecords.add("#2.2"); // 1st failure
        assertThat(recorder.getExecutions()).as("recorder after run 3")
                .containsExactlyElementsOf(expectedRecords);

        dumpProgressAndPerformanceInfo(root.getOid(), result);

        // ------------------------------------------------------------------------------------ run 4

        when("run 4");

        activityManager.clearFailedActivityState(root.getOid(), result);
        taskManager.resumeTask(oidOfSubtask22, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, 10000, 500);

        then("run 4");

        root.refresh(result);
        displayValue("Task tree", TaskDebugUtil.dumpTaskTree(root, result));

        String oidOfSubtask3 = assertTaskTree(root.getOid(), "after run 4")
                .display()
                .assertExecutionStatus(TaskExecutionStateType.RUNNING)
                .assertSchedulingState(TaskSchedulingStateType.WAITING)
                .subtaskForPath(ActivityPath.fromId("mock-simple:2"))
                    .display()
                    .assertSuspended()
                    .getOid();

        displayDumpable("recorder after run 4", recorder);
        expectedRecords.add("#2.2"); // success after 1 failure
        expectedRecords.add("#3"); // 1st failure
        assertThat(recorder.getExecutions()).as("recorder after run 4")
                .containsExactlyElementsOf(expectedRecords);

        // ------------------------------------------------------------------------------------ run 5

        when("run 5");

        activityManager.clearFailedActivityState(root.getOid(), result);
        taskManager.resumeTask(oidOfSubtask3, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, 10000, 500);

        then("run 5");

        root.refresh(result);
        displayValue("Task tree", TaskDebugUtil.dumpTaskTree(root, result));

        // @formatter:off
        assertTaskTree(root.getOid(), "after run 5")
                .display()
                .assertSuccess()
                .assertExecutionStatus(TaskExecutionStateType.CLOSED)
                .activityState()
                    .assertTreeRealizationComplete();
        // @formatter:on

        displayDumpable("recorder after run 5", recorder);
        expectedRecords.add("#3"); // success after 1 failure
        assertThat(recorder.getExecutions()).as("recorder after run 5")
                .containsExactlyElementsOf(expectedRecords);

        // @formatter:off
        assertProgress( root.getOid(),"after")
                .display()
                .assertComplete()
                .assertNoBucketInformation()
                .assertNoItemsInformation()
                .assertChildren(3)
                .child("mock-simple:1")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertItems(1, null)
                    .assertNoChildren()
                .end()
                .child("composition:1") // 1 user
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                    .assertChildren(2)
                    .child("mock-simple:1")
                        .assertComplete()
                        .assertNoBucketInformation()
                        .assertItems(1, null)
                    .end()
                    .child("mock-simple:2")
                        .assertComplete()
                        .assertNoBucketInformation()
                        .assertItems(1, null)
                    .end()
                .end()
                .child("mock-simple:2")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertItems(1, null)
                    .assertNoChildren()
                .end();

        assertPerformance( root.getOid(),"after")
                .display()
                .assertNotApplicable()
                .assertChildren(3)
                .child("mock-simple:1")
                    .assertItemsProcessed(3)
                    .assertErrors(2)
                    .assertProgress(1)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end()
                .child("composition:1")
                    .assertNotApplicable()
                    .assertChildren(2)
                    .child("mock-simple:1")
                        .assertItemsProcessed(1)
                        .assertErrors(0)
                        .assertProgress(1)
                        .assertHasWallClockTime()
                        .assertHasThroughput()
                    .end()
                    .child("mock-simple:2")
                        .assertItemsProcessed(2)
                        .assertErrors(1)
                        .assertProgress(1)
                        .assertHasWallClockTime()
                        .assertHasThroughput()
                    .end()
                .end()
                .child("mock-simple:2")
                    .assertItemsProcessed(2)
                    .assertErrors(1)
                    .assertProgress(1)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end();
        // @formatter:on
    }

    @Test
    public void test220MockCompositeWithSubtasks() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task root = taskAdd(TASK_220_MOCK_COMPOSITE_WITH_SUBTASKS, result);

        when();

        waitForTaskClose(root.getOid(), result, 10000, 200);

        then();

        displayDumpable("recorder", recorder);

        root.refresh(result);

        assertTaskTree(root.getOid(), "after")
                .display("root")
                .assertSuccess()
                .assertClosed()
                .activityState()
                    .rootActivity()
                        .assertComplete()
                        .assertSuccess()
                        .assertChildren(2)
                        .child("opening")
                            .assertComplete()
                            .assertSuccess()
                            .assertDelegationWorkStateWithTaskRef()
                        .end()
                        .child("closing")
                            .assertComplete()
                            .assertSuccess()
                            .assertDelegationWorkStateWithTaskRef()
                        .end()
                    .end()
                .end()
                .subtaskForPath(ActivityPath.fromId("opening"))
                    .display()
                    .assertClosed()
                    .assertSuccess()
                    .activityState()
                        .rootActivity()
                            .progress()
                                .assertUncommitted(1, 0, 0)
                                .assertNoCommitted()
                            .end()
                            .itemProcessingStatistics()
                                .assertTotalCounts(1, 0, 0)
                                .assertLastSuccessObjectName("id1:opening")
                                .assertExecutions(1)
                            .end()
                        .end()
                    .end()
                .end()
                .subtaskForPath(ActivityPath.fromId("closing"))
                    .display()
                    .assertClosed()
                    .assertSuccess()
                    .activityState()
                        .rootActivity()
                            .progress()
                                .assertUncommitted(1, 0, 0)
                                .assertNoCommitted()
                            .end()
                            .itemProcessingStatistics()
                                .assertTotalCounts(1, 0, 0)
                                .assertLastSuccessObjectName("id1:closing")
                                .assertExecutions(1)
                            .end()
                        .end()
                    .end()
                .end();

        OperationStatsType stats = root.getStoredOperationStatsOrClone();
        displayValue("statistics", TaskOperationStatsUtil.format(stats));

        assertProgress(root.getOid(), "after") // This is derived from the subtask
                .display()
                .assertComplete()
                .assertNoBucketInformation()
                .assertNoItemsInformation()
                .child("opening")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertItems(1, null)
                .end()
                .child("closing")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertItems(1, null)
                .end();

        assertPerformance(root.getOid(), "after") // This is derived from the subtask
                .display()
                .assertNotApplicable()
                .assertChildren(2)
                .child("opening")
                    .assertItemsProcessed(1)
                    .assertErrors(0)
                    .assertProgress(1)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end()
                .child("closing")
                    .assertItemsProcessed(1)
                    .assertErrors(0)
                    .assertProgress(1)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end();
    }

    @Test
    public void test300WorkersSimple() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task root = taskAdd(TASK_300_WORKERS_SIMPLE, result);

        when();

        waitForTaskClose(root.getOid(), result, 10000, 200);

        then();

        root.refresh(result);

        assertTaskTree(root.getOid(), "after")
                .display("root")
                .assertSuccess()
                .subtask(0)
                    .display("child");

        OperationStatsType stats = root.getStoredOperationStatsOrClone();
        displayValue("statistics", TaskOperationStatsUtil.format(stats));

        displayDumpable("recorder", recorder);

        assertProgress(root.getOid(), "after")
                .display()
                .assertComplete()
                .assertBuckets(11, 11)
                .assertItems(100, null);
        assertPerformance(root.getOid(), "after")
                .display()
                .assertItemsProcessed(100)
                .assertErrors(0)
                .assertProgress(100)
                .assertHasWallClockTime();
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

    private void dumpProgressAndPerformanceInfo(String oid, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        ActivityProgressInformation progressInfo = activityManager.getProgressInformation(oid, result);
        displayDumpable("progress information", progressInfo);

        dumpPerformanceInfo(oid, result);
    }

    private void dumpPerformanceInfo(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        TreeNode<ActivityPerformanceInformation> performanceInfo =
                activityManager.getPerformanceInformation(oid, result);
        displayDumpable("performance information", performanceInfo);
    }

    @NotNull
    private TaskType getObjectable(Task task1) {
        return task1.getUpdatedTaskObject().asObjectable();
    }
}
