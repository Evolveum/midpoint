/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.io.File;

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

    private static final TestObject<TaskType> TASK_EXECUTION_TIME_NOTIFICAITON =
            TestObject.file(TEST_DIR, "task-execution-time-notification.xml", "38d0f53c-5e01-44dc-8d6a-439e0153b4d8");

    private static final TestObject<TaskType> TASK_SKIP_RESTART =
            TestObject.file(TEST_DIR, "task-skip-restart.xml", "cceeb264-3f9f-4cfd-9f6b-de51c1e63f01");

    private static final TestObject<TaskType> TASK_LAST_SKIP =
            TestObject.file(TEST_DIR, "task-last-skip.xml", "4a2e5ca8-be35-48eb-a280-089afffb8526");

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

    @Test
    public void test100SingleThread() throws Exception {
        testTask(TASK_SIMPLE_NOOP, 0);
    }

    @Test
    public void test150MultipleThreads() throws Exception {
        testTask(TASK_SIMPLE_NOOP, 2);
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
                    .end()
                .activityState(ActivityPath.fromId("activity to be skipped"))
                    .display()
                .assertComplete()
                    .assertFatalError()
                    .assertExecutionAttempts(1)
                .end()
                .activityState(ActivityPath.fromId("activity to be restarted"))
                    .display()
                    .assertComplete()
                    .assertSuccess()
                    .assertExecutionAttempts(2)
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
                .assertComplete()
                    .assertFatalError()
                    .assertExecutionAttempts(1);
        // @formatter:on
    }

    private TaskAsserter<Void> submitTestTask(TestObject<TaskType> object, ThrowableRunnable waitFunction) throws Exception {
        Task testTask = getTestTask();
        OperationResult testResult = testTask.getResult();

        object.reset();

        when();

        deleteIfPresent(object, testResult);
        addObject(object, getTestTask(), testResult);

        waitFunction.run();

        return assertTask(object.oid, "after");
    }

    @Test
    public void test200TestComposite() throws Exception {
        // can use waitFor* methods because the task will be suspended for some time (and restarted)
        TaskAsserter<Void> asserter = submitTestTask(TASK_COMPOSITE_NOOP, () -> Thread.sleep(10000));

        // @formatter:off
        asserter.assertSuspended()
                .activityState(ActivityPath.fromId("activity to be skipped"))
                    .assertFatalError();
        // @formatter:on
    }

    private void testTask(TestObject<TaskType> task, int threads) throws Exception {
        given();
        Task testTask = getTestTask();
        OperationResult testResult = testTask.getResult();

        task.reset();

        when();

        deleteIfPresent(task, testResult);
        addObject(task, getTestTask(), testResult, t -> {

            if (threads > 0) {
                rootActivityWorkerThreadsCustomizer(threads).accept(t);
            }
        });
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, testResult, 10000L);

        then();

        assertSimpleTask(task, threads);

        when("repeated execution");

        taskManager.resumeTaskTree(task.oid, testResult);
        waitForTaskTreeCloseCheckingSuspensionWithError(task.oid, testResult, 3000L);

        then("repeated execution");

        assertSimpleTaskRepeatedExecution(task);

        if (isNativeRepository()) {
            and("there are no simulation results"); // MID-8936
            assertNoRepoObjects(SimulationResultType.class);
        }
    }

    private void assertSimpleTask(TestObject<TaskType> testObject, int threads) throws Exception {
        var options = schemaService.getOperationOptionsBuilder()
                .item(TaskType.F_RESULT).retrieve()
                .item(TaskType.F_SUBTASK_REF).retrieve()
                .build();
        PrismObject<TaskType> task = taskManager.getObject(TaskType.class, testObject.oid, options, getTestOperationResult());

        ActivityPolicyType policy = task.asObjectable().getActivity().getPolicies().getPolicy().get(0);
        String identifier = ActivityPolicyUtils.createIdentifier(ActivityPath.empty(), policy);

        int realThreads = threads > 0 ? threads : 1;

        // @formatter:off
        var asserter = assertTaskTree(task.getOid(), "after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                .display()
                .activityPolicyStates()
                    .display()
                    .assertOnePolicyStateTriggers(identifier, 1)
                .end()
                .assertInProgressLocal()
                .progress().assertSuccessCount(6, 6 * realThreads, true).display().end()
                .itemProcessingStatistics().display().end();
        // @formatter:on
    }

    private void assertSimpleTaskRepeatedExecution(TestObject<TaskType> testObject) throws Exception {
        var options = schemaService.getOperationOptionsBuilder()
                .item(TaskType.F_RESULT).retrieve()
                .item(TaskType.F_SUBTASK_REF).retrieve()
                .build();
        PrismObject<TaskType> task = taskManager.getObject(TaskType.class, testObject.oid, options, getTestOperationResult());

        ActivityPolicyType policy = task.asObjectable().getActivity().getPolicies().getPolicy().get(0);
        String identifier = ActivityPolicyUtils.createIdentifier(ActivityPath.empty(), policy);

        // @formatter:off
        var asserter = assertTaskTree(task.getOid(), "after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .display()
                    .activityPolicyStates()
                        .display()
                        .assertOnePolicyStateTriggers(identifier, 1)
                        .end()
                    .assertInProgressLocal()
                    .progress()
                        .assertSuccessCount(0,0)
                        .display()
                        .end()
                    .itemProcessingStatistics()
                        .display()
                        .end();
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

        waitForTaskCloseOrSuspend(object.oid, 15000);

        then();

        PrismObject<TaskType> task = getTask(object.oid);

        // @formatter:off
        assertTaskTree(object.oid, "after")
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

        TestObject<TaskType> object = TASK_EXECUTION_TIME_NOTIFICAITON;
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
                        .assertPolicyStateCount(1)
                        .activityPolicyState(getActivityIdentifier(task.asObjectable(),"Execution notification"));
//                            .assertTriggerCount(3);
        // @formatter:on

        checkDummyTransportMessages(DUMMY_NOTIFICATION_TRANSPORT, 3);
    }

    private interface ThrowableRunnable {

        void run() throws Exception;
    }
}
