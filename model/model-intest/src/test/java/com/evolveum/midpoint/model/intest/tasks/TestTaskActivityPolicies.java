/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.io.File;
import java.util.function.Consumer;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.ActivityPolicyStateAsserter;
import com.evolveum.midpoint.test.asserter.TaskAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestTaskActivityPolicies extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/thresholds");

    private static final TestObject<TaskType> TASK_SIMPLE_NOOP =
            TestObject.file(TEST_DIR, "task-simple-noop.xml", "54464a8e-7cd3-47e3-9bf6-0d07692a893b");

    private static final TestObject<TaskType> TASK_COMPOSITE_NOOP =
            TestObject.file(TEST_DIR, "task-composite-noop.xml", "50a4d2af-4302-4268-99db-ff49895242d5");

    private static final TestObject<TaskType> TASK_SIMPLE_NOOP_RESTART =
            TestObject.file(TEST_DIR, "task-simple-noop-restart.xml", "0020d7c9-5eac-4ad5-a900-e342ffb775e4");

    private static final TestObject<TaskType> TASK_NON_ITERATIVE_RESTART =
            TestObject.file(TEST_DIR, "task-non-iterative-restart.xml", "d5c0d175-ebda-4506-821d-6205eeae85cf");

    private static final TestObject<TaskType> TASK_EXECUTION_TIME_NOTIFICATION =
            TestObject.file(TEST_DIR, "task-execution-time-notification.xml", "38d0f53c-5e01-44dc-8d6a-439e0153b4d8");

    private static final TestObject<TaskType> TASK_SKIP_RESTART =
            TestObject.file(TEST_DIR, "task-skip-restart.xml", "cceeb264-3f9f-4cfd-9f6b-de51c1e63f01");

    private static final TestObject<TaskType> TASK_LAST_SKIP =
            TestObject.file(TEST_DIR, "task-last-skip.xml", "4a2e5ca8-be35-48eb-a280-089afffb8526");

    private static final TestObject<TaskType> TASK_RESTART =
            TestObject.file(TEST_DIR, "task-restart.xml", "42b2e700-763b-4089-9eb5-092396766a51");

    private static final String DUMMY_NOTIFICATION_TRANSPORT = "activityPolicyRuleNotifier";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        SimpleActivityPolicyRuleNotifierType notifier = new SimpleActivityPolicyRuleNotifierType();
        notifier.getTransport().add("dummy:" + DUMMY_NOTIFICATION_TRANSPORT);
        EventHandlerType handler = new EventHandlerType();
        handler.getSimpleActivityPolicyRuleNotifier().add(notifier);

        modifyObjectAddContainer(
                SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                ItemPath.create(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION, NotificationConfigurationType.F_HANDLER),
                initTask,
                initResult,
                handler);
    }

    @BeforeMethod
    public void beforeMethod() {
        prepareNotifications();
    }

    @Test(enabled = false) // todo fix
    public void test100SingleThread() throws Exception {
        testSimpleSuspend(0);
    }

    @Test(enabled = false)  // todo fix
    public void test150MultipleThreads() throws Exception {
        testSimpleSuspend(2);
    }

    private void testSimpleSuspend(int threads) throws Exception {
        OperationResult testResult = getTestOperationResult();

        TaskAsserter<Void> asserter = submitTestTask(
                TASK_SIMPLE_NOOP,
                t -> rootActivityWorkerThreadsCustomizer(threads).accept(t),
                () -> Thread.sleep(10000));

        assertSimpleNoopTask(asserter, threads);

        when("repeated execution");

        taskManager.resumeTaskTree(TASK_SIMPLE_NOOP.oid, testResult);
        waitForTaskTreeCloseCheckingSuspensionWithError(TASK_SIMPLE_NOOP.oid, testResult, 3000L);

        then("repeated execution");

        assertSimpleNoopTaskAfterResume();

        if (isNativeRepository()) {
            and("there are no simulation results"); // MID-8936
            assertNoRepoObjects(SimulationResultType.class);
        }
    }

    private void assertSimpleNoopTask(TaskAsserter<Void> asserter, int threads)
            throws Exception {

        ActivityPolicyType policy = getTask(TASK_SIMPLE_NOOP.oid).asObjectable().getActivity().getPolicies().getPolicy().get(0);
        String identifier = ActivityPolicyUtils.createIdentifier(ActivityPath.empty(), policy);

        int realThreads = threads > 0 ? threads : 1;

        // @formatter:off
        asserter.assertSuspended()
                .assertFatalError()
                .rootActivityState()
                .display()
                .activityPolicyStates()
                    .assertOnePolicyStateTriggers(identifier, 1)
                    .end()
                .assertInProgressLocal()
                    .progress()
                .assertSuccessCount(6, 6 * realThreads, true);
        // @formatter:on
    }

    private void assertSimpleNoopTaskAfterResume()
            throws Exception {

        TaskType task = getTask(TASK_SIMPLE_NOOP.oid).asObjectable();
        ActivityPolicyType policy = task.getActivity().getPolicies().getPolicy().get(0);
        String identifier = ActivityPolicyUtils.createIdentifier(ActivityPath.empty(), policy);

        TaskAsserter<Void> asserter = assertTask(task, "after resume");

        // todo what to do with such task - run time recorded in the task is more than 2s,
        //  so after resume it will hit policy threshold right away
        // @formatter:off
        asserter.assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .display()
                    .activityPolicyStates()
                        .assertOnePolicyStateTriggers(identifier, 1)
                        .end()
                    .assertInProgressLocal()
                    .progress()
                        .assertSuccessCount(7,0);
        // @formatter:on
    }

    @Test
    public void test170SkipRestart() throws Exception {
        // can use waitFor* methods because the task will be suspended for some time (and restarted)
        TaskAsserter<Void> asserter = submitTestTask(TASK_SKIP_RESTART, () -> Thread.sleep(10000));

        // @formatter:off
        asserter.assertFatalError()
                .activityState(ActivityPath.empty())
                    .display()
                    .assertExecutionAttempts(2)
                    .assertComplete()
                    .assertFatalError()
                    .assertNotRestarting()
                .end()
                .activityState(ActivityPath.fromId("activity to be skipped"))
                    .display()
                    .assertSkipped()
                    .assertFatalError()
                    .assertExecutionAttempts(1)
                    .assertNotRestarting()
                .end()
                .activityState(ActivityPath.fromId("activity to be restarted"))
                    .display()
                    .assertComplete()
                    .assertSuccess()
                    .assertExecutionAttempts(2)
                    .assertNotRestarting()
                    .progress()
                        .assertSuccessCount(0, 6)
                        .display()
                        .end()
                    .itemProcessingStatistics()
                        .display();
        // @formatter:on
    }

    /**
     * Test whether skipped activity (last one) doesn't leave the task in runnable/suspended "unfinished" state.
     */
    @Test
    public void test180LastActivitySkip() throws Exception {
        TaskAsserter<Void> asserter = submitTestTask(TASK_LAST_SKIP, () -> waitForTaskCloseOrSuspend(TASK_LAST_SKIP.oid, 3000));

        // @formatter:off
        asserter.assertFatalError()
                .activityState(ActivityPath.empty())
                    .display()
                    .assertExecutionAttempts(1)
                    .assertComplete()
                    .assertFatalError()
                    .end()
                .activityState(ActivityPath.fromId("activity to be skipped"))
                    .display()
                    .assertSkipped()
                    .assertFatalError()
                    .assertExecutionAttempts(1);
        // @formatter:on
    }

    @Test
    public void test190RestartMultipleTimes() throws Exception {
        TaskAsserter<Void> asserter = submitTestTask(
                TASK_RESTART,
                () -> waitForTaskFinish(
                        TASK_RESTART.oid,
                        0,
                        15000,
                        true,
                        300,
                        checkerBuilder -> checkerBuilder.taskConsumer(t -> {
                            if (t.isRunning() || t.isClosed()) {
                                return;
                            }

                            TaskActivityStateType tas = t.getActivitiesStateOrClone();
                            if (tas == null || tas.getActivity() == null) {
                                // too soon
                                return;
                            }

                            // asserting that during activity restarts, task runs are cleared,
                            // otherwise activity execution time measurement would be wrong
                            // @formatter:off
                            assertTask(t, "during run")
                                    .displayXml()
                                    .asTask()
                                    .assertExecutionState(TaskExecutionStateType.RUNNABLE)
                                    .activityState(ActivityPath.empty())
                                        .assertRestarting(false)
                                        .assertInProgressLocal()
                                        .itemProcessingStatistics()
                                            .assertRuns(1); // After a restart, runs should be cleared
                            // @formatter:on
                        })));

        // @formatter:off
        asserter.assertSuccess()
                .activityState(ActivityPath.empty())
                    .display()
                    .assertComplete()
                    .assertSuccess()
                    .assertExecutionAttempts(3)
                    .progress()
                        .assertSuccessCount(0, 6)
                    .end()
                    .itemProcessingStatistics()
                        .assertRuns(1);
        // @formatter:on
    }

    private TaskAsserter<Void> submitTestTask(
            TestObject<TaskType> object, ThrowableRunnable waitFunction) throws Exception {
        return submitTestTask(object, null, waitFunction);
    }

    private TaskAsserter<Void> submitTestTask(
            TestObject<TaskType> object, Consumer<PrismObject<TaskType>> taskCustomizer, ThrowableRunnable waitFunction) throws Exception {

        Task testTask = getTestTask();
        OperationResult testResult = testTask.getResult();

        object.reset();

        when();

        deleteIfPresent(object, testResult);
        addObject(object, getTestTask(), testResult, t -> {
            if (taskCustomizer != null) {
                taskCustomizer.accept(t);
            }
        });

        waitFunction.run();

        return assertTask(object.oid, "after");
    }

    @Test
    public void test200TestComposite() throws Exception {
        // can use waitFor* methods because the task will be suspended for some time (and restarted)
        TaskAsserter<Void> asserter = submitTestTask(TASK_COMPOSITE_NOOP, () -> Thread.sleep(10000));

        // @formatter:off
        asserter.assertSuspended()
                .assertFatalError()
                .activityState(ActivityPath.fromId("activity to be restarted"))
                    .display()
                    .assertExecutionAttempts(2)
                    .assertComplete()
                    .assertSuccess()
                    .end()
                .activityState(ActivityPath.fromId("activity to be skipped"))
                    .display()
                    .assertSkipped()
                    .assertFatalError()
                    .end()
                .activityState(ActivityPath.fromId("activity to suspend"))
                    .display()
                    .assertExecutionAttempts(1)
                    .assertInProgressLocal()
                    .assertFatalError()
                    .progress()
                        .assertSuccessCount(3, 0);
        // @formatter:on
    }

    @Test
    public void test300TestSimpleRestartThenSkip() throws Exception {
        Task testTask = getTestTask();
        OperationResult testResult = testTask.getResult();

        TestObject<TaskType> object = TASK_SIMPLE_NOOP_RESTART;
        object.reset();

        when();

        deleteIfPresent(object, testResult);
        addObject(object, testTask, testResult);

        //waitForTaskCloseOrSuspend(object.oid, 15000); // we can't use this: the task goes through SUSPENDED state several times
        Thread.sleep(15000);

        then();

        PrismObject<TaskType> task = getTask(object.oid);

        // @formatter:off
        assertTaskTree(object.oid, "after")
                .displayXml()
                .asTask()
                .assertSuspended()
                .assertFatalError()
                .assertTaskRunHistorySize(1)
                .rootActivityState()
                    .assertExecutionAttempts(3)     // 1 initial + 2 restarts
                    .assertFatalError()
                    .activityPolicyStates()
                        .display()
                        .assertPolicyStateCount(2)
                        .activityPolicyState(getActivityIdentifier(task.asObjectable(),"Max. 2s execution"))
                            .assertTriggerCount(1)
                        .end()
                        .activityPolicyState(getActivityIdentifier(task.asObjectable(),"count restarts"))
                            .assertTriggerCount(1);
        // @formatter:on

        checkDummyTransportMessages(DUMMY_NOTIFICATION_TRANSPORT, 3);
    }

    private String getActivityIdentifier(TaskType task, String policyName) {
        ActivityPolicyType policy = ActivityPolicyStateAsserter.forName(task.getActivity(), policyName);
        return ActivityPolicyUtils.createIdentifier(ActivityPath.empty(), policy);
    }

    @Test
    public void test350ExecutionTimeNotification() throws Exception {
        Task testTask = getTestTask();
        OperationResult testResult = testTask.getResult();

        TestObject<TaskType> object = TASK_EXECUTION_TIME_NOTIFICATION;
        object.reset();

        when();

        deleteIfPresent(object, testResult);
        addObject(object, testTask, testResult);

        waitForTaskCloseOrSuspend(object.oid, 6000);

        then();

        PrismObject<TaskType> task = getTask(object.oid);

        // @formatter:off
        assertTaskTree(object.oid, "after")
                .assertSuspended()
                .assertFatalError()
                .assertTaskRunHistorySize(1)
                .rootActivityState()
                    .assertExecutionAttempts(1)
                    .assertFatalError()
                    .activityPolicyStates()
                        .display()
                        .assertPolicyStateCount(3)
                        .activityPolicyState(getActivityIdentifier(task.asObjectable(),"Execution notification"));
        // @formatter:on

        checkDummyTransportMessages(DUMMY_NOTIFICATION_TRANSPORT, 3);
    }

    private interface ThrowableRunnable {

        void run() throws Exception;
    }
}
