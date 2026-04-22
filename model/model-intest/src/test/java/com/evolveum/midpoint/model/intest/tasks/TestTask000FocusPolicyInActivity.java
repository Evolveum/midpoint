/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.util.function.Consumer;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

import org.testng.annotations.BeforeMethod;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.TraceManager;

public class TestTask000FocusPolicyInActivity extends TestThresholds {

    private static final long TIMEOUT = 60000;

    private static final int WORKER_THREADS = 1;

    private static final TestObject<RoleType> ROLE_ADD_10_NOTIFICATION =
            TestObject.file(TEST_DIR, "role-add-10-notification.xml", "79d1d4e7-408c-4255-9386-5697892691df");

    private static final TestObject<RoleType> ROLE_MODIFY_5_COST_CENTER =
            TestObject.file(TEST_DIR, "role-modify-cost-center-5-notification.xml", "719eb6bc-6a89-4baf-a125-2482b9502ad1");

    private static final TestObject<RoleType> ROLE_MODIFY_5_FULL_NAME =
            TestObject.file(TEST_DIR, "role-modify-full-name-5-notification.xml", "6b76ce6f-0ca0-4e86-9209-8d734851348d");

    private static final TestTask TASK_IMPORT =
            TestTask.file(TEST_DIR, "task-000-import.xml", "385b2498-bf8b-4e31-a807-71c312cc2e29");

    private static final TestTask TASK_IMPORT_SIMULATE =
            TestTask.file(TEST_DIR, "task-000-import-simulate.xml", "4ba70403-424c-410d-a3b0-e3b7719063dc");

    private static final TestTask TASK_IMPORT_SIMULATE_EXECUTE =
            TestTask.file(TEST_DIR, "task-000-import-simulate-execute.xml", "c865c889-238b-47f2-b05b-445d1c21259f");

    private static final TestTask TASK_RECONCILIATION =
            TestTask.file(TEST_DIR, "task-000-reconciliation.xml", "385b2498-bf8b-4e31-a807-71c312cc2e29");

    private static final TestTask TASK_RECONCILIATION_SIMULATE =
            TestTask.file(TEST_DIR, "task-000-reconciliation-simulate.xml", "7534f9eb-6139-4db5-a1d5-8e699a057e8a");

    private static final TestTask TASK_RECONCILIATION_SIMULATE_EXECUTE =
            TestTask.file(TEST_DIR, "task-000-reconciliation-simulate-execute.xml", "53734bf9-7068-4ee6-8804-2be3f4fe31ee");

    private static final String DUMMY_NOTIFICATION_TRANSPORT = "dummy:policyRuleNotifier";

    String roleAddNotificationId;

    @BeforeMethod
    public void beforeMethod() {
        prepareNotifications();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        SimpleActivityPolicyRuleNotifierType activityPolicyNotifier = new SimpleActivityPolicyRuleNotifierType();
        activityPolicyNotifier.getTransport().add(DUMMY_NOTIFICATION_TRANSPORT);

        SimplePolicyRuleNotifierType policyNotifier = new SimplePolicyRuleNotifierType();
        policyNotifier.getTransport().add(DUMMY_NOTIFICATION_TRANSPORT);

        EventHandlerType handler = new EventHandlerType();
        handler.getSimpleActivityPolicyRuleNotifier().add(activityPolicyNotifier);
        handler.getSimplePolicyRuleNotifier().add(policyNotifier);

        modifyObjectAddContainer(
                SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                ItemPath.create(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION, NotificationConfigurationType.F_HANDLER),
                initTask,
                initResult,
                handler);

        repoAdd(ROLE_ADD_10_NOTIFICATION, initResult);
        roleAddNotificationId = determineSingleInducedRuleId(ROLE_ADD_10_NOTIFICATION.oid, initResult);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> getRoleAssignmentImportAddCustomizer() {
        return roleAssignmentCustomizer(ROLE_ADD_10_NOTIFICATION.oid);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> getRoleAssignmentModifyCostCenter() {
        return roleAssignmentCustomizer(ROLE_MODIFY_5_COST_CENTER.oid);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> getRoleAssignmentModifyFullName() {
        return roleAssignmentCustomizer(ROLE_MODIFY_5_FULL_NAME.oid);
    }

    @Override
    int getWorkerThreads() {
        return WORKER_THREADS;
    }

    @Override
    long getTimeout() {
        return TIMEOUT;
    }

    @Override
    TestObject<TaskType> getSimulateTask() {
        return TASK_IMPORT_SIMULATE;
    }

    @Override
    TestObject<TaskType> getSimulateExecuteTask() {
        return TASK_IMPORT_SIMULATE_EXECUTE;
    }

    @Override
    TestObject<TaskType> getExecuteTask() {
        return TASK_IMPORT;
    }

    @Override
    TestObject<TaskType> getReconciliationSimulateTask() {
        return TASK_RECONCILIATION_SIMULATE;
    }

    @Override
    TestObject<TaskType> getReconciliationSimulateExecuteTask() {
        return TASK_RECONCILIATION_SIMULATE_EXECUTE;
    }

    @Override
    TestObject<TaskType> getReconciliationExecuteTask() {
        return TASK_RECONCILIATION;
    }

    private void dumpSimplifiedTaskActivityState(TestObject<TaskType> task) throws Exception {
        try {
            TaskActivityStateType state = getTask(task.oid).asObjectable().getActivityState();
            state.getActivity().setStatistics(null);
            String xml = PrismTestUtil.getPrismContext().xmlSerializer().serializeAnyData(state, TaskType.F_ACTIVITY_STATE);
            System.out.println("Task state >>>>\n" + xml);
            TraceManager.getTrace(TestThresholdsSingleTask.class)
                    .error(
                            "Asserting task after execution\n{}",
                            xml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    void assertTest100Task(TestObject<TaskType> importTask) throws Exception {
        dumpSimplifiedTaskActivityState(importTask);

        PrismObject<TaskType> task = getTask(importTask.oid);
        var suspendPolicyIdentifier = ActivityPolicyUtils.buildPolicyIdentifier(task, ActivityPath.empty(), "add-10-suspend");

        // @formatter:off
        assertTaskTree(importTask.oid, "after")
            .assertSuspended()
            .assertFatalError()
            .rootActivityState()
                .display()
                .previewModePolicyRulesCounters()
                    .display()
                    .assertCounterCount(3)
                    .assertCounterMinMax(roleAddNotificationId, USER_ADD_ALLOWED + 1, USER_ADD_ALLOWED + getThreads())
                    .assertCounterMinMax(suspendPolicyIdentifier, USER_ADD_ALLOWED + 1, USER_ADD_ALLOWED + getThreads())
                .end()
                .progress()
                    .display()
                    .end()
                .itemProcessingStatistics()
                    .display()
                    .end()
            .progress()
                .assertUncommitted(USER_ADD_ALLOWED, 1, 0)
                .end()
                .itemProcessingStatistics()
                .assertTotalCounts(USER_ADD_ALLOWED, 1, 0)
            .end();
        // @formatter:on
    }

    @Override
    void assertTest100TaskAfterRepeatedExecution(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        assertTaskTree(importTask.oid, "after repeated execution")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .display()
                    .previewModePolicyRulesCounters()
                        .display()
                        .end()
                    .progress()
                        .display()
                        .end()
                    .itemProcessingStatistics()
                        .display()
                        .end()
                    .previewModePolicyRulesCounters()
                        .assertCounter(ruleAddId, USER_ADD_ALLOWED + 2)
                        .end()
                    .progress()
                        .assertUncommitted(0, 1, 0) // fails immediately because of persistent counters
                        .end()
                    .itemProcessingStatistics()
                        .assertTotalCounts(USER_ADD_ALLOWED, 2, 0)
                    .end();
        // @formatter:on
    }

    @Override
    void assertTest110TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        assertTaskTree(importTask.oid, "after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .assertInProgressLocal()
                    .assertFatalError()
                    .end()
                .activityState(SIMULATE)
                    .assertInProgressLocal()
                    .assertFatalError()
                    .progress()
                        .display()
                        .assertSuccessCount(USER_ADD_ALLOWED, true)
                        .assertFailureCount(1, getThreads(), true)
                        .end()
                    .end()
                .activityState(EXECUTE)
                    .display()
                    .assertRealizationState(null) // this should not even start
                    .end();
        // @formatter:on
    }

    @Override
    void assertTest120TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        assertTaskTree(importTask.oid, "after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .assertInProgressLocal()
                    .assertFatalError()
                    .progress()
                        .display()
                        .assertSuccessCount(USER_ADD_ALLOWED, true)
                        .assertFailureCount(1, getThreads(), true)
                        .end()
                    .itemProcessingStatistics()
                        .display()
                        .end()
                    .actionsExecuted()
                        .resulting()
                            .display()
                            .assertCount(ChangeTypeType.ADD, UserType.COMPLEX_TYPE, USER_ADD_ALLOWED, 0)
                            .end()
                        .end();
        // @formatter:on
    }

    @Override
    void assertTest200TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {

    }

    @Override
    void assertTest200TaskAfterRepeatedExecution(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {

    }

    @Override
    void assertTest210TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {

    }

    @Override
    void assertTest220TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {

    }

    @Override
    void assertTest300TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {

    }

    @Override
    void assertTest310TaskAfter(TestObject<TaskType> reconTask) throws SchemaException, ObjectNotFoundException {

    }

    @Override
    void assertTest400TaskAfter(TestObject<TaskType> reconTask) throws SchemaException, ObjectNotFoundException {

    }

    @Override
    void assertTest400TaskAfterRepeatedExecution(TestObject<TaskType> reconTask) throws SchemaException, ObjectNotFoundException {

    }

    @Override
    void assertTest410TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {

    }

    @Override
    void assertTest420TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {

    }
}
