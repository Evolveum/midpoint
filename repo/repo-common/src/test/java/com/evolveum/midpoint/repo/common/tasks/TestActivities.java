/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.tasks;

import static com.evolveum.midpoint.repo.common.tasks.handlers.CommonMockActivityHelper.EXECUTION_COUNT_NAME;

import static com.evolveum.midpoint.schema.util.task.ActivityProgressInformationBuilder.InformationSource.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityTaskExecutionStateType.NOT_RUNNING;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.activity.handlers.NoOpActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.activity.run.reports.ActivityReportUtil;
import com.evolveum.midpoint.repo.common.activity.run.reports.SimpleReportReader;
import com.evolveum.midpoint.repo.common.activity.run.buckets.BucketingConfigurationOverrides;
import com.evolveum.midpoint.schema.statistics.ActionsExecutedInformationUtil;
import com.evolveum.midpoint.schema.util.task.*;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper;
import com.evolveum.midpoint.task.api.TaskDebugUtil;
import com.evolveum.midpoint.test.asserter.ActivityProgressInformationAsserter;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

import org.apache.commons.collections4.ListUtils;
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
 * Tests basic features of the activity framework:
 *
 * 1. running simple mock activity ({@link #test100RunSimpleLegacyTask()}, {@link #test120RunSimpleTask()}),
 * 2. running mock semi-composite activity ({@link #test110RunCompositeLegacyTask()}, {@link #test130RunCompositeTask()}),
 * 3. running custom composite activity ({@link #test140RunCustomCompositeTask()}),
 * 4. running mock iterative activity, including bucketing ({@link #test150RunMockIterativeTask()}, {@link #test155RunBucketedMockIterativeTask()}),
 * 5. running mock search-based activity, including bucketing ({@link #test160RunMockSearchBasedTask()}, {@link #test170RunBucketedTask()}),
 * 6. running tree of bucketed activities ({@link #test180RunBucketedTree()}),
 * 7. delegation of processing to separate task(s) - for simple activity ({@link #test200Subtask()}) or children
 * of a semi-composite one ({@link #test220MockCompositeWithSubtasks()}),
 * 8. distribution of a processing to worker tasks ({@link #test300WorkersSimple()}, {@link #test310WorkersScavengerFrozen()}).
 *
 * Specifically, the following is checked as well:
 *
 * 1. suspension and resuming for composite activities ({@link #test190SuspendingComposite()}), even those with
 * subtasks ({@link #test210SuspendingCompositeWithSubtasks()}),
 * 2. extraction of progress and performance information - see {@link ActivityProgressInformation}
 * and {@link ActivityPerformanceInformation}.
 *
 * Activities used here are the mock ones: in {@link com.evolveum.midpoint.repo.common.tasks.handlers} package.
 * As a special case, {@link NoOpActivityHandler} is tested briefly.
 */

@ContextConfiguration(locations = "classpath:ctx-repo-common-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestActivities extends AbstractRepoCommonTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/activities");

    private static final TestResource<TaskType> TASK_100_MOCK_SIMPLE_LEGACY = new TestResource<>(TEST_DIR, "task-100-mock-simple-legacy.xml", "7523433a-a537-4943-96e9-58b6c57566e8");
    private static final TestResource<TaskType> TASK_110_MOCK_COMPOSITE_LEGACY = new TestResource<>(TEST_DIR, "task-110-mock-composite-legacy.xml", "b5fd4ecf-2163-4079-99ec-d56e8a96ca94");
    private static final TestResource<TaskType> TASK_115_NO_OP_LEGACY = new TestResource<>(TEST_DIR, "task-115-no-op-legacy.xml", "2e577670-e422-47d9-a915-92e8ddfee087");
    private static final TestResource<TaskType> TASK_120_MOCK_SIMPLE = new TestResource<>(TEST_DIR, "task-120-mock-simple.xml", "6a1a58fa-ce09-495d-893f-3093cdcc00b6");
    private static final TestResource<TaskType> TASK_130_MOCK_COMPOSITE = new TestResource<>(TEST_DIR, "task-130-mock-composite.xml", "14a41fca-a664-450c-bc5d-d4ce35045346");
    private static final TestResource<TaskType> TASK_135_NO_OP = new TestResource<>(TEST_DIR, "task-135-no-op.xml", "d1c750b0-eddc-445f-b907-d19c8ed754b5");
    private static final TestResource<TaskType> TASK_140_CUSTOM_COMPOSITE = new TestResource<>(TEST_DIR, "task-140-custom-composite.xml", "65866e01-73cd-4249-9b7b-03ebc4413bd0");
    private static final TestResource<TaskType> TASK_150_MOCK_ITERATIVE = new TestResource<>(TEST_DIR, "task-150-mock-iterative.xml", "c21785e9-1c67-492f-bc79-0c51f74561a1");
    private static final TestResource<TaskType> TASK_155_MOCK_ITERATIVE_BUCKETED = new TestResource<>(TEST_DIR, "task-155-mock-iterative-bucketed.xml", "02a94071-2eff-4ca0-aa63-3fdf9d540064");
    private static final TestResource<TaskType> TASK_160_MOCK_SEARCH_ITERATIVE = new TestResource<>(TEST_DIR, "task-160-mock-search-iterative.xml", "9d8384b3-a007-44e2-a9f7-084a64bdc285");
    private static final TestResource<TaskType> TASK_170_MOCK_BUCKETED = new TestResource<>(TEST_DIR, "task-170-mock-bucketed.xml", "04e257d1-bb25-4675-8e00-f248f164fbc3");
    private static final TestResource<TaskType> TASK_180_BUCKETED_TREE = new TestResource<>(TEST_DIR, "task-180-bucketed-tree.xml", "ac3220c5-6ded-4b94-894e-9ed39c05db66");
    private static final TestResource<TaskType> TASK_185_BUCKETED_TREE_ANALYSIS = new TestResource<>(TEST_DIR, "task-185-bucketed-tree-analysis.xml", "12f07ab1-41c3-4dba-bf47-3d2a032fa555");
    private static final TestResource<TaskType> TASK_190_SUSPENDING_COMPOSITE = new TestResource<>(TEST_DIR, "task-190-suspending-composite.xml", "1e7cf975-7253-4991-a707-661d3c52f203");
    private static final TestResource<TaskType> TASK_200_SUBTASK = new TestResource<>(TEST_DIR, "task-200-subtask.xml", "ee60863e-ff77-4edc-9e4e-2e1ea7853478");
    private static final TestResource<TaskType> TASK_210_SUSPENDING_COMPOSITE_WITH_SUBTASKS = new TestResource<>(TEST_DIR, "task-210-suspending-composite-with-subtasks.xml", "cd36ca66-cd49-44cf-9eb2-36928acbe1fd");
    private static final TestResource<TaskType> TASK_220_MOCK_COMPOSITE_WITH_SUBTASKS = new TestResource<>(TEST_DIR, "task-220-mock-composite-with-subtasks.xml", "");
    private static final TestResource<TaskType> TASK_300_WORKERS_SIMPLE = new TestResource<>(TEST_DIR, "task-300-workers-simple.xml", "5cfa521a-a174-4254-a5cb-199189fe42d5");
    private static final TestResource<TaskType> TASK_310_WORKERS_SCAVENGING = new TestResource<>(TEST_DIR, "task-310-workers-scavenging.xml", "1e956013-5997-47bd-8885-4da2340dddfc");
    private static final TestResource<TaskType> TASK_400_LONG_RUNNING = new TestResource<>(TEST_DIR, "task-400-long-running.xml", "f179b67d-a4b2-4bd0-af8a-7f814d9f069c");

    @Autowired private MockRecorder recorder;
    @Autowired private CommonTaskBeans beans;

    private static final int ROLES = 100;
    private static final String ROLE_NAME_PATTERN = "r%02d";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        createRoles(initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
        BucketingConfigurationOverrides.setFreeBucketWaitIntervalOverride(100L);
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

    /**
     * Mock-simple activity configured in a legacy way.
     */
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
        // @formatter:off
        assertTask(task1, "after")
                .display()
                .assertClosed()
                .assertSuccess()
                .assertProgress(1)
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete()
                        .assertNoSynchronizationStatistics()
                        .assertNoActionsExecutedInformation()
                        .assertPersistenceSingleRealization();
        // @formatter:on

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

    /**
     * Mock-composite activity configured in a legacy way.
     */
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
        // @formatter:off
        assertTask(task1, "after")
                .display()
                .assertClosed()
                .assertSuccess()
                .assertProgress(2)
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete()
                        .assertNoSynchronizationStatistics()
                        .assertNoActionsExecutedInformation()
                        .assertPersistenceSingleRealization()
                        .child("opening")
                            .assertPersistenceSingleRealization()
                        .end()
                        .child("closing")
                            .assertPersistencePerpetual()
                        .end()
                    .end()
                .end();
        // @formatter:on

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("executions").containsExactly("id1:opening", "id1:closing");

        // @formatter:off
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
                    .assertNoThroughput();
        // @formatter:on
    }

    /**
     * NoOp activity configured in a legacy way.
     */
    @Test
    public void test115RunNoOpLegacyTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        Task task1 = taskAdd(TASK_115_NO_OP_LEGACY, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        assertNoOpTaskAfter(task1, true);
    }

    private void assertNoOpTaskAfter(Task task1, boolean legacy) throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        assertTask(task1, "after")
                .display()
                .assertClosed()
                .assertSuccess()
                .assertProgress(5)
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete()
                        .assertNoSynchronizationStatistics()
                        .assertNoActionsExecutedInformation()
                        .assertPersistenceSingleRealization()
                    .end()
                .end();

        Consumer<ActivityProgressInformationAsserter<?>> progressChecker =
                (asserter) -> asserter
                        .display()
                        .assertComplete()
                        .assertBuckets(1, 1)
                        .assertItems(5, 5);

        if (legacy) {
            assertProgress(task1.getOid(), TREE_OVERVIEW_ONLY, "after")
                    .display()
                    .assertComplete()
                    .assertNoBucketInformation() // this is not filled in the overview by default
                    .assertNoItemsInformation(); // this is not filled in the overview by default
        } else {
            progressChecker.accept(assertProgress(task1.getOid(), TREE_OVERVIEW_ONLY, "after"));
        }
        progressChecker.accept(assertProgress(task1.getOid(), TREE_OVERVIEW_PREFERRED, "after"));
        progressChecker.accept(assertProgress(task1.getOid(), FULL_STATE_PREFERRED, "after"));
        progressChecker.accept(assertProgress(task1.getOid(), FULL_STATE_ONLY, "after"));

        assertPerformance(task1.getOid(), "after")
                .display()
                .assertItemsProcessed(5)
                .assertErrors(0)
                .assertProgress(5)
                .assertHasWallClockTime()
                .assertHasThroughput();
        // @formatter:on
    }

    /**
     * Mock-simple activity in modern way.
     *
     * This test runs the task twice to check the activity state purger
     * (it cleans up the state before second run).
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
                .assertProgress(1)
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete()
                        .assertNoSynchronizationStatistics()
                        .assertNoActionsExecutedInformation();

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
                .assertProgress(1)
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete();

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("executions").containsExactly("msg1", "msg1");

        dumpProgressAndPerformanceInfo(task1.getOid(), result);

    }

    /**
     * Mock-composite activity in a modern way.
     */
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
                .assertProgress(2)
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete()
                        .assertNoSynchronizationStatistics()
                        .assertNoActionsExecutedInformation();

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("executions").containsExactly("id1:opening", "id1:closing");
    }

    /**
     * NoOp activity configured in a legacy way.
     */
    @Test
    public void test135RunNoOpTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        Task task1 = taskAdd(TASK_135_NO_OP, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        assertNoOpTaskAfter(task1, false);
    }

    /**
     * Custom composite activity.
     *
     * This test runs the task twice to check the activity state purger
     * (it cleans up the state before second run).
     */
    @Test
    public void test140RunCustomCompositeTask() throws Exception {

        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();
        Task root = taskAdd(TASK_140_CUSTOM_COMPOSITE, result);
        List<String> expectedExecutions1 = List.of("A:opening", "A:closing", "Hello", "B:opening", "B:closing", "C:closing");
        List<String> expectedExecutions2 = ListUtils.union(expectedExecutions1, expectedExecutions1);

        execute140RunCustomCompositeTaskOnce(root, "run 1", 1, expectedExecutions1);
        restartTask(root.getOid(), result);

        execute140RunCustomCompositeTaskOnce(root, "run 2", 2, expectedExecutions2);
    }

    private void execute140RunCustomCompositeTaskOnce(Task root, String label, int runNumber,
            List<String> expectedExecutions) throws CommonException {

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
                .assertProgress(3 + runNumber*3L) // there are 3 "closing" activities with persistent statistics
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete()
                        .assertNoSynchronizationStatistics()
                        .assertNoActionsExecutedInformation();

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
                        .assertItems(runNumber, null)
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
                        .assertItems(runNumber, null)
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
                        .assertItems(runNumber, null)
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
                        .assertItemsProcessed(runNumber) // state (incl. statistics) is kept
                        .assertErrors(0)
                        .assertProgress(runNumber)
                        .assertHasWallClockTime()
                        .assertNoThroughput()
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
                        .assertItemsProcessed(runNumber) // state (incl. statistics) is kept
                        .assertErrors(0)
                        .assertProgress(runNumber)
                        .assertHasWallClockTime()
                        .assertNoThroughput()
                    .end()
                .end()
                .child("mock-composite:3")
                    .assertNotApplicable()
                    .assertChildren(1)
                    .child("closing")
                        .assertItemsProcessed(runNumber) // state (incl. statistics) is kept
                        .assertErrors(0)
                        .assertProgress(runNumber)
                        .assertHasWallClockTime()
                        .assertNoThroughput()
                    .end()
                .end();
        // @formatter:on
    }

    /**
     * Runs mock-iterative activity.
     */
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

        displayDumpable("recorder", recorder);

        task1.refresh(result);
        // @formatter:off
        assertTask(task1, "after")
                .display()
                .assertSuccess()
                .assertClosed()
                .assertProgress(5)
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete()
                        .assertSuccess()
                        .progress()
                            .assertCommitted(5, 0, 0) // maybe in the future we may move these to committed on activity close
                            .assertNoUncommitted()
                        .end()
                        .itemProcessingStatistics()
                            .assertTotalCounts(5, 0, 0)
                            .assertLastSuccessObjectName("5")
                            .assertRuns(1)
                        .end()
                        .synchronizationStatistics()
                            .display()
                            .assertTransitions(1)
                            .assertTransition(UNMATCHED, UNLINKED, LINKED, null, 5, 0, 0)
                        .end()
                        .actionsExecuted()
                            .part(ActionsExecutedInformationUtil.Part.ALL)
                                .display()
                                .assertCount(ChangeTypeType.ADD, UserType.COMPLEX_TYPE, 5, 0)
                                .assertCount(ChangeTypeType.MODIFY, UserType.COMPLEX_TYPE, 5, 0)
                                .assertLastSuccessName(ChangeTypeType.ADD, UserType.COMPLEX_TYPE, "5")
                                .assertLastSuccessName(ChangeTypeType.MODIFY, UserType.COMPLEX_TYPE, "5")
                            .end()
                            .part(ActionsExecutedInformationUtil.Part.RESULTING)
                                .display()
                                .assertCount(ChangeTypeType.ADD, UserType.COMPLEX_TYPE, 5, 0)
                                .assertLastSuccessName(ChangeTypeType.ADD, UserType.COMPLEX_TYPE, "5")
                            .end();
        // @formatter:on

        OperationStatsType stats = task1.getStoredOperationStatsOrClone();
        displayValue("task statistics", TaskOperationStatsUtil.format(stats));

        assertThat(recorder.getExecutions()).as("recorder")
                .containsExactly("Item: 1", "Item: 2", "Item: 3", "Item: 4", "Item: 5");

        assertProgress(task1.getOid(), "after")
                .display()
                .assertComplete()
                .assertItems(5, 5);
        assertPerformance(task1.getOid(), "after")
                .display()
                .assertItemsProcessed(5)
                .assertErrors(0)
                .assertProgress(5)
                .assertHasWallClockTime();
    }

    /**
     * Runs mock-iterative activity with buckets.
     */
    @Test
    public void test155RunBucketedMockIterativeTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_155_MOCK_ITERATIVE_BUCKETED, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        displayDumpable("recorder", recorder);

        task1.refresh(result);
        // @formatter:off
        assertTask(task1, "after")
                .display()
                .assertSuccess()
                .assertClosed()
                .assertProgress(12)
                .activityState()
                    .assertTreeRealizationComplete()
                    .rootActivity()
                        .assertComplete()
                        .assertSuccess()
                        .progress()
                            .assertCommitted(12, 0, 0) // maybe in the future we may move these to committed on activity close
                            .assertNoUncommitted()
                        .end()
                        .itemProcessingStatistics()
                            .assertTotalCounts(12, 0, 0)
                            .assertLastSuccessObjectName("12")
                            .assertRuns(1)
                        .end()
                        .synchronizationStatistics()
                            .display()
                            .assertTransitions(1)
                            .assertTransition(UNMATCHED, UNLINKED, LINKED, null, 12, 0, 0)
                        .end()
                        .actionsExecuted()
                            .part(ActionsExecutedInformationUtil.Part.ALL)
                                .display()
                                .assertCount(ChangeTypeType.ADD, UserType.COMPLEX_TYPE, 12, 0)
                                .assertCount(ChangeTypeType.MODIFY, UserType.COMPLEX_TYPE, 12, 0)
                                .assertLastSuccessName(ChangeTypeType.ADD, UserType.COMPLEX_TYPE, "12")
                                .assertLastSuccessName(ChangeTypeType.MODIFY, UserType.COMPLEX_TYPE, "12")
                            .end()
                            .part(ActionsExecutedInformationUtil.Part.RESULTING)
                                .display()
                                .assertCount(ChangeTypeType.ADD, UserType.COMPLEX_TYPE, 12, 0)
                                .assertLastSuccessName(ChangeTypeType.ADD, UserType.COMPLEX_TYPE, "12")
                            .end();
        // @formatter:on

        OperationStatsType stats = task1.getStoredOperationStatsOrClone();
        displayValue("task statistics", TaskOperationStatsUtil.format(stats));

        assertThat(recorder.getExecutions()).as("recorder")
                .containsExactly("Item: 1", "Item: 2", "Item: 3", "Item: 4", "Item: 5", "Item: 6",
                        "Item: 7", "Item: 8", "Item: 9", "Item: 10", "Item: 11", "Item: 12");

        assertProgress(task1.getOid(), "after")
                .display()
                .assertComplete()
                .assertItems(12, 12)
                .assertBuckets(4, 4);
        assertPerformance(task1.getOid(), "after")
                .display()
                .assertItemsProcessed(12)
                .assertErrors(0)
                .assertProgress(12)
                .assertHasWallClockTime();
    }

    /**
     * Runs mock search-based activity.
     */
    @Test
    public void test160RunMockSearchBasedTask() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_160_MOCK_SEARCH_ITERATIVE, result);

        when();

        waitForTaskClose(task1.getOid(), result, 10000, 200);

        then();

        task1.refresh(result);
        // @formatter:off
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
                            .assertRuns(1)
                        .end()
                        .synchronizationStatistics()
                            .display()
                            .assertTransitions(1)
                            .assertTransition(null, UNLINKED, LINKED, null, 100, 0, 0)
                        .end()
                        .actionsExecuted()
                            .part(ActionsExecutedInformationUtil.Part.ALL)
                                .display()
                                .assertCount(ChangeTypeType.MODIFY, RoleType.COMPLEX_TYPE, 200, 0)
                            .end()
                            .part(ActionsExecutedInformationUtil.Part.RESULTING)
                                .display()
                                .assertCount(ChangeTypeType.MODIFY, RoleType.COMPLEX_TYPE, 100, 0)
                            .end()
                        .end();
        // @formatter:on

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
                .assertItems(100, 100);
        assertPerformance(task1.getOid(), "after")
                .display()
                .assertItemsProcessed(100)
                .assertErrors(0)
                .assertProgress(100)
                .assertHasWallClockTime();
    }

    /**
     * Runs mock search-based activity with buckets.
     */
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
                            .assertRuns(1)
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
                .assertItems(100, 100);
        assertPerformance(task1.getOid(), "after")
                .display()
                .assertItemsProcessed(100)
                .assertErrors(0)
                .assertProgress(100)
                .assertHasWallClockTime();
    }

    /**
     * Runs a tree of bucketed activities, to check that multiple bucketed activities do not conflict with each other.
     */
    @Test
    public void test180RunBucketedTree() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_180_BUCKETED_TREE, result);

        when();

        waitForTaskCloseOrSuspend(task1.getOid(), 40000, 2000);

        then();

        task1.refresh(result);
        // @formatter:off
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
                                .assertRuns(1)
                            .end()
                            .synchronizationStatistics()
                                .display()
                                .assertTransitions(0)
                            .end()
                            .actionsExecuted()
                                .part(ActionsExecutedInformationUtil.Part.ALL)
                                    .display()
                                    .assertEmpty()
                                .end()
                                .part(ActionsExecutedInformationUtil.Part.RESULTING)
                                    .display()
                                    .assertEmpty()
                                .end()
                            .end()
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
                                .assertRuns(1)
                            .end()
                            .synchronizationStatistics()
                                .display()
                            .end()
                            .actionsExecuted()
                                .part(ActionsExecutedInformationUtil.Part.ALL)
                                    .display()
                                    .assertCount(ChangeTypeType.MODIFY, UserType.COMPLEX_TYPE, 2, 0)
                                    .assertLastSuccessName(ChangeTypeType.MODIFY, UserType.COMPLEX_TYPE, "administrator")
                                .end()
                                .part(ActionsExecutedInformationUtil.Part.RESULTING)
                                    .display()
                                    .assertCount(ChangeTypeType.MODIFY, UserType.COMPLEX_TYPE, 1, 0)
                                    .assertLastSuccessName(ChangeTypeType.MODIFY, UserType.COMPLEX_TYPE, "administrator")
                                .end()
                            .end()
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
                                    .assertRuns(1)
                                .end()
                                .synchronizationStatistics()
                                    .display()
                                    .assertTransitions(1)
                                    .assertTransition(null, UNLINKED, LINKED, null, 10, 0, 0)
                                .end()
                                .actionsExecuted()
                                    .part(ActionsExecutedInformationUtil.Part.ALL)
                                        .display()
                                        .assertCount(ChangeTypeType.MODIFY, RoleType.COMPLEX_TYPE, 20, 0)
                                    .end()
                                    .part(ActionsExecutedInformationUtil.Part.RESULTING)
                                        .display()
                                        .assertCount(ChangeTypeType.MODIFY, RoleType.COMPLEX_TYPE, 10, 0)
                                    .end()
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
                                    .assertRuns(1)
                                .end()
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
                                .assertRuns(1)
                            .end()
                            .assertBucketManagementStatisticsOperations(3)
                        .end()
                    .end()
                .end()
                .rootActivityStateOverview()
                    .display()
                    .assertComplete()
                    .assertSuccess()
                    .assertProgressHidden()
                    .assertSingleTask(TASK_180_BUCKETED_TREE.oid, NOT_RUNNING)
                    .assertChildren(4)
                    .child("first")
                        .assertComplete()
                        .assertSuccess()
                        .assertProgressHidden()
                        .assertSingleTask(TASK_180_BUCKETED_TREE.oid, NOT_RUNNING)
                    .end()
                    .child("second")
                        .assertComplete()
                        .assertSuccess()
                        .assertProgressHidden()
                        .assertSingleTask(TASK_180_BUCKETED_TREE.oid, NOT_RUNNING)
                    .end()
                    .child("composition:1")
                        .assertComplete()
                        .assertSuccess()
                        .assertProgressHidden()
                        .assertSingleTask(TASK_180_BUCKETED_TREE.oid, NOT_RUNNING)
                        .assertChildren(2)
                        .child("third-A")
                            .assertComplete()
                            .assertSuccess()
                            .assertProgressHidden()
                            .assertSingleTask(TASK_180_BUCKETED_TREE.oid, NOT_RUNNING)
                        .end()
                        .child("third-B")
                            .assertComplete()
                            .assertSuccess()
                            .assertProgressHidden()
                            .assertSingleTask(TASK_180_BUCKETED_TREE.oid, NOT_RUNNING)
                        .end()
                    .end()
                    .child("fourth")
                        .assertComplete()
                        .assertSuccess()
                        .assertProgressHidden()
                        .assertSingleTask(TASK_180_BUCKETED_TREE.oid, NOT_RUNNING)
                    .end();
        // @formatter:on

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
                    .assertItems(0, 0)
                    .assertNoChildren()
                .end()
                .child("second") // 1 user
                    .assertComplete()
                    .assertBuckets(1, 1)
                    .assertItems(1, 1)
                .end()
                .child("composition:1")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                    .assertChildren(2)
                    .child("third-A")
                        .assertComplete()
                        .assertBuckets(11, 11)
                        .assertItems(10, 10)
                    .end()
                    .child("third-B")
                        .assertComplete()
                        .assertBuckets(1, 1)
                        .assertItems(1, 1)
                    .end()
                .end()
                .child("fourth")
                    .assertComplete()
                    .assertBuckets(101, 101)
                    .assertItems(100, 100)
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

    /**
     * Runs a tree of bucketed activities in the "bucket analysis" mode.
     */
    @Test
    public void test185RunBucketedTreeAnalysis() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task task1 = taskAdd(TASK_185_BUCKETED_TREE_ANALYSIS, result);

        when();

        waitForTaskCloseOrSuspend(task1.getOid(), 40000, 2000);

        then();

        task1.refresh(result);
        // @formatter:off
        assertTask(task1, "after")
                .display()
                .assertSuccess()
                .assertClosed()
                .activityState()
                    .assertTreeRealizationComplete();
        // @formatter:on

        OperationStatsType stats = task1.getStoredOperationStatsOrClone();
        displayValue("statistics", TaskOperationStatsUtil.format(stats));

        displayDumpable("recorder", recorder);
        assertThat(recorder.getExecutions()).as("recorder").isEmpty();

        task1.setResult(null);
        displayValue("task after (XML)", prismContext.xmlSerializer().serialize(task1.getRawTaskObjectClone()));

        // @formatter:off
        assertProgress( task1.getOid(),"after")
                .display()
                .assertComplete()
                .assertNoBucketInformation()
                .assertNoItemsInformation()
                .assertChildren(3)
                .child("first") // 0 configs
                    .assertComplete()
                    .assertBuckets(1, 1)
                    .assertItems(0, 0) // we determine expected total also in bucket analysis mode
                    .assertNoChildren()
                .end()
                .child("second")
                    .assertComplete()
                    .assertBuckets(11, 11)
                    .assertItems(0, 10) // we determine expected total also in bucket analysis mode
                .end()
                .child("third")
                    .assertComplete()
                    .assertBuckets(101, 101)
                    .assertItems(0, 100) // we determine expected total also in bucket analysis mode
                .end();

        assertPerformance( task1.getOid(),"after")
                .display();
        // @formatter:on

        // TODO improve this code
        String secondOid = ActivityReportUtil.getReportDataOid(task1.getWorkState(), ActivityPath.fromId("second"),
                ActivityReportsType.F_BUCKETS, taskManager.getNodeId());
        assertThat(secondOid).as("second buckets report OID").isNotNull();
        try (var reader = SimpleReportReader.createForLocalReportData(
                secondOid, List.of("content-from", "content-to", "size"), beans, result)) {
            List<List<String>> rows = reader.getRows();
            displayValue("rows of bucket analysis report", rows);
            assertThat(rows).as("rows").hasSize(11);
            assertThat(rows.get(0)).as("row 0").containsExactly("", "r10", "0");
            assertThat(rows.get(1)).as("row 1").containsExactly("r10", "r11", "1");
            assertThat(rows.get(2)).as("row 2").containsExactly("r11", "r12", "1");
            assertThat(rows.get(10)).as("row 10").containsExactly("r19", "", "1");
        }
    }

    /**
     * Checks suspension and resuming for composite activities.
     */
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
                .assertExecutionState(TaskExecutionStateType.SUSPENDED)
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
                                .assertCommitted(0, 1, 0)
                                .assertNoUncommitted()
                            .end()
                            .itemProcessingStatistics()
                                .assertTotalCounts(0, 1, 0)
                                .assertLastFailureObjectName("#1")
                                .assertRuns(1)
                            .end()
                        .end()
                        .child("composition:1")
                            .assertResultStatus(null)
                            .assertChildren(0)
                        .end()
                        .child("mock-simple:2")
                            .assertResultStatus(null)
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

        restartTask(task1.getOid(), result);
        waitForTaskCloseOrSuspend(task1.getOid(), 10000, 200);

        then("run 2");

        task1.refresh(result);

        // @formatter:off
        assertTask(task1.getOid(), "after run 2")
                .display()
                //.assertFatalError() // TODO
                .assertExecutionState(TaskExecutionStateType.SUSPENDED)
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
                                .assertCommitted(0, 2, 0)
                                .assertNoUncommitted()
                            .end()
                            .itemProcessingStatistics()
                                .assertTotalCounts(0, 2, 0)
                                .assertLastFailureObjectName("#1")
                                .assertRuns(2)
                            .end()
                        .end()
                        .child("composition:1")
                            .assertResultStatus(null) // TODO
                            .assertChildren(0)
                        .end()
                        .child("mock-simple:2")
                            .assertResultStatus(null) // TODO
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
                    .assertItems(2, null)
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
                    .assertProgress(2)
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

        restartTask(task1.getOid(), result);
        waitForTaskCloseOrSuspend(task1.getOid(), 10000, 200);

        then("run 3");

        task1.refresh(result);

        // @formatter:off
        assertTask(task1.getOid(), "after run 3")
                .display()
                .assertFatalError()
                .assertExecutionState(TaskExecutionStateType.SUSPENDED)
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
                                .assertCommitted(1, 2, 0)
                                .assertNoUncommitted()
                            .end()
                            .itemProcessingStatistics()
                                .assertTotalCounts(1, 2, 0)
                                .assertLastSuccessObjectName("#1")
                                .assertLastFailureObjectName("#1")
                                .assertRuns(3)
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
                                    .assertCommitted(0, 1, 0)
                                    .assertNoUncommitted()
                                .end()
                                .itemProcessingStatistics()
                                    .assertTotalCounts(0, 1, 0)
                                    .assertLastFailureObjectName("#2.2")
                                    .assertRuns(1)
                                .end()
                            .end()
                        .end()
                        .child("mock-simple:2")
                            .assertNotStarted()
                            .assertResultStatus(null) // TODO
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

        restartTask(task1.getOid(), result);
        waitForTaskCloseOrSuspend(task1.getOid(), 10000, 200);

        then("run 4");

        task1.refresh(result);

        // @formatter:off
        assertTask(task1.getOid(), "after run 4")
                .display()
                .assertFatalError()
                .assertExecutionState(TaskExecutionStateType.SUSPENDED)
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

        restartTask(task1.getOid(), result);
        waitForTaskCloseOrSuspend(task1.getOid(), 10000, 200);

        then("run 5");

        task1.refresh(result);

        // @formatter:off
        assertTask(task1.getOid(), "after run 5")
                .display()
                .assertSuccess()
                .assertExecutionState(TaskExecutionStateType.CLOSED)
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
                    .assertItems(3, null)
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
                        .assertItems(2, null)
                    .end()
                .end()
                .child("mock-simple:2")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertItems(2, null)
                    .assertNoChildren()
                .end();

        assertPerformance(task1.getOid(),"after")
                .display()
                .assertNotApplicable()
                .assertChildren(3)
                .child("mock-simple:1")
                    .assertItemsProcessed(3)
                    .assertErrors(2)
                    .assertProgress(3)
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
                        .assertProgress(2)
                        .assertHasWallClockTime()
                        .assertHasThroughput()
                    .end()
                .end()
                .child("mock-simple:2")
                    .assertItemsProcessed(2)
                    .assertErrors(1)
                    .assertProgress(2)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end();
        // @formatter:on
    }

    /**
     * Checks delegation of processing to separate task.
     */
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

        // @formatter:off
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
                        .assertRole(TaskRoleType.DELEGATE)
                        .assertLocalRoot(ActivityPath.empty())
                        .rootActivity()
                            .assertComplete()
                            .assertSuccess()
                            .progress()
                                .assertCommitted(1, 0, 0)
                                .assertNoUncommitted()
                            .end()
                            .itemProcessingStatistics()
                                .assertTotalCounts(1, 0, 0)
                                .assertRuns(1)
                            .end()
                            .workStateExtension()
                                .assertPropertyValuesEqual(EXECUTION_COUNT_NAME, 1)
                            .end();
        // @formatter:on

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

    /**
     * Checks suspension of composite activity that contains delegation to subtasks.
     */
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

        String oidOfSubtask1 = assertTaskTree(root.getOid(), "after run 1")
                .subtask(0)
                .getOid();

        // @formatter:off
        assertTaskTree(root.getOid(), "after run 1")
                .display()
                .assertExecutionState(TaskExecutionStateType.RUNNING)
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
                .rootActivityStateOverview()
                    .assertRealizationInProgress()
                    .assertStatusInProgress()
                    .assertProgressHidden()
                    .assertSingleTask(TASK_210_SUSPENDING_COMPOSITE_WITH_SUBTASKS.oid, NOT_RUNNING)
                    .child("mock-simple:1")
                        .assertRealizationInProgress()
                        .assertFatalError()
                        .assertSingleTask(oidOfSubtask1, NOT_RUNNING)
                        .assertItemsProgress(null, 1)
                    .end()
                .end()
                .subtask(0)
                    .display()
                    .assertExecutionState(TaskExecutionStateType.SUSPENDED)
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
        Consumer<ActivityProgressInformationAsserter<?>> progressChecker =
                (asserter) -> asserter
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

        progressChecker.accept(assertProgress(root.getOid(), FULL_STATE_ONLY, "after"));
        progressChecker.accept(assertProgress(root.getOid(), FULL_STATE_PREFERRED, "after"));
        progressChecker.accept(assertProgress(root.getOid(), TREE_OVERVIEW_PREFERRED, "after"));
        // Tree overview only would miss data from the root task

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

        taskManager.resumeTask(oidOfSubtask1, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, 10000, 500);

        then("run 2");

        root.refresh(result);
        displayValue("Task tree", TaskDebugUtil.dumpTaskTree(root, result));

        // @formatter:off
        assertTaskTree(root.getOid(), "after run 2")
                .display()
                // state?
                .assertExecutionState(TaskExecutionStateType.RUNNING)
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
                .rootActivityStateOverview()
                    .assertRealizationInProgress()
                    .assertStatusInProgress()
                    .assertProgressHidden()
                    .assertSingleTask(TASK_210_SUSPENDING_COMPOSITE_WITH_SUBTASKS.oid, NOT_RUNNING)
                    .child("mock-simple:1")
                        .assertRealizationInProgress()
                        .assertFatalError()
                        .assertSingleTask(oidOfSubtask1, NOT_RUNNING)
                        .assertItemsProgress(null, 2)
                    .end()
                .end()
                .subtask(0)
                    .display()
                    .assertExecutionState(TaskExecutionStateType.SUSPENDED)
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
                    .assertItems(2, null)
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
                    .assertProgress(2)
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

        taskManager.resumeTask(oidOfSubtask1, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, 10000, 500);

        then("run 3");

        root.refresh(result);
        displayValue("Task tree", TaskDebugUtil.dumpTaskTree(root, result));

        Holder<String> oidOfSubtask2Holder = new Holder<>();
        Holder<String> oidOfSubtask21Holder = new Holder<>();
        Holder<String> oidOfSubtask22Holder = new Holder<>();

        // @formatter:off
        assertTaskTree(root.getOid(), "after run 3")
                .display()
                .subtaskForPath(ActivityPath.fromId("composition:1"))
                    .sendOid(oidOfSubtask2Holder)
                    .subtaskForPath(ActivityPath.fromId("composition:1", "mock-simple:1"))
                        .sendOid(oidOfSubtask21Holder)
                    .end()
                    .subtaskForPath(ActivityPath.fromId("composition:1", "mock-simple:2"))
                        .sendOid(oidOfSubtask22Holder)
                    .end()
                .end()
                .assertExecutionState(TaskExecutionStateType.RUNNING)
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
                .rootActivityStateOverview()
                    .assertRealizationInProgress()
                    .assertStatusInProgress()
                    .assertProgressHidden()
                    .assertSingleTask(TASK_210_SUSPENDING_COMPOSITE_WITH_SUBTASKS.oid, NOT_RUNNING)
                    .child("mock-simple:1")
                        .assertComplete()
                        .assertSuccess()
                        .assertSingleTask(oidOfSubtask1, NOT_RUNNING)
                        .assertItemsProgress(null, 3)
                    .end()
                    .child("composition:1")
                        .assertRealizationInProgress()
                        .assertStatusInProgress()
                        .assertSingleTask(oidOfSubtask2Holder.getValue(), NOT_RUNNING)
                        .assertProgressVisible()
                        .assertNoItemsProgress()
                        .child("mock-simple:1")
                            .assertComplete()
                            .assertSuccess()
                            .assertSingleTask(oidOfSubtask21Holder.getValue(), NOT_RUNNING)
                            .assertItemsProgress(null, 1)
                        .end()
                        .child("mock-simple:2")
                            .assertRealizationInProgress()
                            .assertFatalError()
                            .assertSingleTask(oidOfSubtask22Holder.getValue(), NOT_RUNNING)
                            .assertItemsProgress(null, 1)
                        .end()
                        .child("mock-simple:3")
                            .assertNotStarted()
                            .assertNoTask()
                        .end()
                    .end()
                    // TODO mock-simple:2
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

        String oidOfSubtask22 = oidOfSubtask22Holder.getValue();

        displayDumpable("recorder after run 3", recorder);
        expectedRecords.add("#1"); // success after 2 failures
        expectedRecords.add("#2.1"); // immediate success
        expectedRecords.add("#2.2"); // 1st failure
        assertThat(recorder.getExecutions()).as("recorder after run 3")
                .containsExactlyElementsOf(expectedRecords);

        dumpProgressAndPerformanceInfo(root.getOid(), result);

        Consumer<ActivityProgressInformationAsserter<?>> progressChecker3 =
                (asserter) -> asserter
                        .display()
                        .assertInProgress()
                        .assertNoBucketInformation()
                        .assertNoItemsInformation()
                        .assertChildren(3)
                        .child("mock-simple:1")
                            .assertComplete()
                            .assertNoBucketInformation()
                            .assertItems(3, null)
                            .assertNoChildren()
                        .end()
                        .child("composition:1")
                            .assertInProgress()
                            .assertNoBucketInformation()
                            .assertNoItemsInformation()
                            .assertChildren(3)
                            .child("mock-simple:1")
                                .assertComplete()
                                .assertNoBucketInformation()
                                .assertItems(1, null)
                                .assertNoChildren()
                            .end()
                            .child("mock-simple:2")
                                .assertInProgress()
                                .assertNoBucketInformation()
                                .assertItems(1, null)
                                .assertNoChildren()
                            .end()
                            .child("mock-simple:3")
                                .assertNotStarted()
                                .assertNoBucketInformation()
                                .assertNoItemsInformation()
                                .assertNoChildren()
                            .end()
                        .end()
                        .child("mock-simple:2")
                            .assertNotStarted()
                            .assertNoBucketInformation()
                            .assertNoItemsInformation()
                            .assertNoChildren()
                        .end();

        progressChecker3.accept(assertProgress(root.getOid(), FULL_STATE_ONLY, "after run 3 (full only)"));
        progressChecker3.accept(assertProgress(root.getOid(), FULL_STATE_PREFERRED, "after run 3 (full preferred)"));
        progressChecker3.accept(assertProgress(root.getOid(), TREE_OVERVIEW_PREFERRED, "after run 3 (tree preferred"));

        // ------------------------------------------------------------------------------------ run 4

        when("run 4");

        taskManager.resumeTask(oidOfSubtask22, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, 10000, 500);

        then("run 4");

        root.refresh(result);
        displayValue("Task tree", TaskDebugUtil.dumpTaskTree(root, result));

        String oidOfSubtask3 = assertTaskTree(root.getOid(), "after run 4")
                .display()
                .assertExecutionState(TaskExecutionStateType.RUNNING)
                .assertSchedulingState(TaskSchedulingStateType.WAITING)
                .subtaskForPath(ActivityPath.fromId("mock-simple:2"))
                    .display()
                    .assertSuspended()
                    .getOid();

        displayDumpable("recorder after run 4", recorder);
        expectedRecords.add("#2.2"); // success after 1 failure
        expectedRecords.add("#2.3");
        expectedRecords.add("#3"); // 1st failure
        assertThat(recorder.getExecutions()).as("recorder after run 4")
                .containsExactlyElementsOf(expectedRecords);

        // ------------------------------------------------------------------------------------ run 5

        when("run 5");

        taskManager.resumeTask(oidOfSubtask3, result);
        waitForTaskTreeCloseCheckingSuspensionWithError(root.getOid(), result, 10000, 500);

        then("run 5");

        root.refresh(result);
        displayValue("Task tree", TaskDebugUtil.dumpTaskTree(root, result));

        // @formatter:off
        assertTaskTree(root.getOid(), "after run 5")
                .display()
                .assertSuccess()
                .assertExecutionState(TaskExecutionStateType.CLOSED)
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
                    .assertItems(3, null)
                    .assertNoChildren()
                .end()
                .child("composition:1") // 1 user
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertNoItemsInformation()
                    .assertChildren(3)
                    .child("mock-simple:1")
                        .assertComplete()
                        .assertNoBucketInformation()
                        .assertItems(1, null)
                    .end()
                    .child("mock-simple:2")
                        .assertComplete()
                        .assertNoBucketInformation()
                        .assertItems(2, null)
                    .end()
                    .child("mock-simple:3")
                        .assertComplete()
                        .assertNoBucketInformation()
                        .assertItems(1, null)
                    .end()
                .end()
                .child("mock-simple:2")
                    .assertComplete()
                    .assertNoBucketInformation()
                    .assertItems(2, null)
                    .assertNoChildren()
                .end();

        assertPerformance( root.getOid(),"after")
                .display()
                .assertNotApplicable()
                .assertChildren(3)
                .child("mock-simple:1")
                    .assertItemsProcessed(3)
                    .assertErrors(2)
                    .assertProgress(3)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end()
                .child("composition:1")
                    .assertNotApplicable()
                    .assertChildren(3)
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
                        .assertProgress(2)
                        .assertHasWallClockTime()
                        .assertHasThroughput()
                    .end()
                    .child("mock-simple:3")
                        .assertItemsProcessed(1)
                        .assertErrors(0)
                        .assertProgress(1)
                        .assertHasWallClockTime()
                        .assertHasThroughput()
                    .end()
                .end()
                .child("mock-simple:2")
                    .assertItemsProcessed(2)
                    .assertErrors(1)
                    .assertProgress(2)
                    .assertHasWallClockTime()
                    .assertHasThroughput()
                .end();
        // @formatter:on
    }

    /**
     * Delegation of embedded activities (i.e. children of semi-composite activity).
     */
    @Test
    public void test220MockCompositeWithSubtasks() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task root = taskAdd(TASK_220_MOCK_COMPOSITE_WITH_SUBTASKS, result);

        when();

        waitForTaskClose(root.getOid(), result, 10000000, 200);

        then();

        displayDumpable("recorder", recorder);

        root.refresh(result);

        // @formatter:off
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
                                .assertCommitted(1, 0, 0)
                                .assertNoUncommitted()
                            .end()
                            .itemProcessingStatistics()
                                .assertTotalCounts(1, 0, 0)
                                .assertLastSuccessObjectName("id1:opening")
                                .assertRuns(1)
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
                            .assertPersistencePerpetual()
                            .progress()
                                .assertCommitted(1, 0, 0)
                                .assertNoUncommitted()
                            .end()
                            .itemProcessingStatistics()
                                .assertTotalCounts(1, 0, 0)
                                .assertLastSuccessObjectName("id1:closing")
                                .assertRuns(0) // because of perpetual persistence of state
                            .end()
                        .end()
                    .end()
                .end();
        // @formatter:on

        OperationStatsType stats = root.getStoredOperationStatsOrClone();
        displayValue("statistics", TaskOperationStatsUtil.format(stats));

        // @formatter:off
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
                    .assertNoThroughput()
                .end();
        // @formatter:on
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

        assertThat(recorder.getRealizationStartTimestamps())
                .as("realization start timestamps")
                .hasSize(1);
    }

    /**
     * When the scavenger (or any worker) is not finished, the realization state
     * in the distributed activity must be "in progress".
     *
     * Here we start 4 workers, one of which (the scavenger in this case) takes too long to finish.
     *
     * We check that the state is "in progress".
     */
    @Test
    public void test310WorkersScavengerFrozen() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        recorder.reset();

        Task root = taskAdd(TASK_310_WORKERS_SCAVENGING, result);

        when();

        // We wait until all non-scavengers are done.
        waitForTaskTreeCloseOrCondition(root.getOid(), result, 10000, 200,
                tasksClosedPredicate(3));
        stabilize();

        then();

        root.refresh(result);

        assertTaskTree(root.getOid(), "after")
                .display("root")
                .activityState()
                    .assertTreeRealizationInProgress()
                .end()
                .rootActivityStateOverview()
                    .display()
                    .assertRealizationInProgress()
                    .assertStatusInProgress()
                .end();
    }


    /**
     * The performance information of running tasks should be measured in a reasonable way.
     */
    @Test
    public void test400RunningTaskPerformance() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        Task root = taskAdd(TASK_400_LONG_RUNNING, result);
        waitForTaskStart(root.getOid(), result, 5000, 100);

        when();

        MiscUtil.sleepCatchingInterruptedException(2000);
        assertTask(root.getOid(), "during")
                .assertExecutionState(TaskExecutionStateType.RUNNING)
                .display();

        assertPerformance(root.getOid(), "during")
                .display()
                .assertApplicable();

        then();

        boolean suspended = suspendTask(root.getOid(), 10000);
        assertThat(suspended).as("task was suspended").isTrue();

        assertPerformance(root.getOid(), "after")
                .display()
                .assertApplicable();
    }

    private void dumpProgressAndPerformanceInfo(String oid, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        ActivityProgressInformation progressInfo = activityManager.getProgressInformationFromTaskTree(oid, result);
        displayDumpable("progress information", progressInfo);

        dumpPerformanceInfo(oid, result);
    }

    private void dumpPerformanceInfo(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        TreeNode<ActivityPerformanceInformation> performanceInfo =
                activityManager.getPerformanceInformation(oid, result);
        displayDumpable("performance information", performanceInfo);
    }
}
