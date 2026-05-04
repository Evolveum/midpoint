/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.util.function.Consumer;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyUtils;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * This test class is similar to {@link TestThresholdsSingleThread}, however
 * role assigned to task contains policy that just creates notifications.
 *
 * Clockwork policy that suspends task is "transplanted" from role to current (main) activity.
 */
public class TestFocusPolicyInActivity extends TestFocusPolicies {

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

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportAdd10Simulate() {
        return transplantRolePolicyForSimulateOrExecuteTask(ROLE_ADD_10);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportAdd10SimulateExecute() {
        return transplantRolePolicyForSimulateExecuteTask(ROLE_ADD_10);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportAdd10Execute() {
        return transplantRolePolicyForSimulateOrExecuteTask(ROLE_ADD_10);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyCostCenter5Execute() {
        return transplantRolePolicyForSimulateOrExecuteTask(ROLE_MODIFY_COST_CENTER_5);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyCostCenter5SimulateExecute() {
        return transplantRolePolicyForSimulateExecuteTask(ROLE_MODIFY_COST_CENTER_5);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyCostCenter5Simulate() {
        return transplantRolePolicyForSimulateOrExecuteTask(ROLE_MODIFY_COST_CENTER_5);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyFullName5SimulateExecute() {
        return transplantRolePolicyForSimulateOrExecuteTask(ROLE_MODIFY_FULL_NAME_5);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesReconModifyFullName5SimulateExecute() {
        return task ->
                transplantRolePolicy(
                        ROLE_MODIFY_FULL_NAME_5,
                        createPolicyActionsReplacement(),
                        task,
                        ActivityPath.empty()
                );
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesReconcileDelete5Simulate() {
        return task ->
                transplantRolePolicy(
                        ROLE_DELETE_5,
                        createPolicyActionsReplacement(),
                        task,
                        ActivityPath.empty()
                );
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

    @Override
    void assertTest100Task(TestObject<TaskType> importTask) throws Exception {
        // there are 9 notifications, because for the 10th time, activity policy rules are not being evaluated,
        // because task is suspended from clockwork policy rule (before activity policies evaluation).
        assertNotifications(DUMMY_ACTIVITY_POLICY_NOTIFIER, "Execution time 0s", 9);
//        assertNotifications(DUMMY_POLICY_NOTIFIER, "add-10", 5); // todo fix this [viliam]

        PrismObject<TaskType> task = getTask(importTask.oid);
        var suspendPolicyIdentifier = ActivityPolicyUtils.buildPolicyIdentifier(task, ActivityPath.empty(), "add-10");

        // @formatter:off
        assertTaskTree(importTask.oid, "after")
            .assertSuspended()
            .assertFatalError()
            .rootActivityState()
                .display()
                .previewModePolicyRulesCounters()
                    .display()
                    .assertCounterMinMax(ruleAddNotificationId, USER_ADD_ALLOWED + 1, USER_ADD_ALLOWED + getThreads())
                    .assertCounterMinMax(suspendPolicyIdentifier, USER_ADD_ALLOWED + 1, USER_ADD_ALLOWED + getThreads())
                    .assertCounterCount(2)
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
    void assertTest100TaskAfterRepeatedExecution(TestObject<TaskType> importTask) throws Exception {
        // todo assert notifications, counters

        PrismObject<TaskType> task = getTask(importTask.oid);
        var suspendPolicyIdentifier = ActivityPolicyUtils.buildPolicyIdentifier(task, ActivityPath.empty(), "add-10");

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
                    .assertCounter(ruleAddNotificationId, USER_ADD_ALLOWED + 2 )
                    .assertCounter(suspendPolicyIdentifier, USER_ADD_ALLOWED + 2)
                    .assertCounterCount(2)
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
        // todo assert notifications, counters
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
        // todo assert notifications, counters
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
        // todo assert notifications, counters
        // @formatter:off
        assertTaskTree(importTask.oid, "task after")
            .assertSuspended()
            .assertFatalError()
            .rootActivityState()
                .display()
                .previewModePolicyRulesCounters()
                    .display()
                    .assertCounterMinMax(ruleModifyCostCenterNotificationId, USER_MODIFY_ALLOWED + 1, USER_MODIFY_ALLOWED + getThreads())
                    .end()
                .progress()
                    .display()
                    .assertSuccessCount(USER_MODIFY_ALLOWED, ACCOUNTS, true) // this is quite a broad range :)
                    .assertFailureCount(1, getThreads(), true)
                    .end()
                .itemProcessingStatistics()
                    .assertTotalCounts(USER_MODIFY_ALLOWED * 4, 1, 0)
                    .end();
        // @formatter:on
    }

    @Override
    void assertTest200TaskAfterRepeatedExecution(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        // todo assert notifications, counters
        // @formatter:off
        assertTaskTree(importTask.oid, "task after repeated execution")
            .assertSuspended()
            .assertFatalError()
            .rootActivityState()
                .display()
                .previewModePolicyRulesCounters()
                    .display()
                    .assertCounter(ruleModifyCostCenterNotificationId, USER_MODIFY_ALLOWED + 2)
                    .end()
                .itemProcessingStatistics()
                    .display()
                    .assertTotalCounts(USER_MODIFY_ALLOWED*4, 2, 0)
                    .end();
        // @formatter:on
    }

    @Override
    void assertTest210TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        // todo assert notifications, counters
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
    void assertTest220TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        // todo assert notifications, counters
        // @formatter:off
        assertTaskTree(importTask.oid, "after")
            .assertSuspended()
            .assertFatalError()
            .rootActivityState()
                .assertInProgressLocal()
                .assertFatalError()
                .progress()
                    .display()
                    .assertFailureCount(1, getThreads(), true)
                    .end();
        // @formatter:on
    }

    @Override
    void assertTest300TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        // todo assert notifications, counters
        // @formatter:off
        assertTaskTree(importTask.oid, "after")
            .assertClosed()
            .assertSuccess()
            .activityState(SIMULATE)
                .assertComplete()
                .assertSuccess()
                .previewModePolicyRulesCounters()
                    .display()
                    .assertCounter(ruleModifyFullNameNotificationId, 4)
                    .end()
                .itemProcessingStatistics()
                    .display()
                    .assertTotalCounts(ACCOUNTS, 0, 0)
                    .end()
                .end()
            .activityState(EXECUTE)
                .assertComplete()
                .assertSuccess()
                .fullExecutionModePolicyRulesCounters()
                    .display()
                    .assertCounter(ruleModifyFullNameNotificationId, 4)
                    .end()
                .itemProcessingStatistics()
                    .display()
                    .assertTotalCounts(ACCOUNTS, 0, 0)
                    .end()
                .end();
        // @formatter:on
    }

    @Override
    void assertTest310TaskAfter(TestObject<TaskType> reconTask) throws SchemaException, ObjectNotFoundException {
        // todo assert notifications, counters
        // @formatter:off
        assertTaskTree(reconTask.oid, "after")
            .assertClosed()
            .assertSuccess()
            .rootActivityState()
                .previewModePolicyRulesCounters()
                    .display()
                    .assertCounter(ruleModifyFullNameNotificationId, 4)
                    .end()
                .fullExecutionModePolicyRulesCounters()
                    .display()
                    .assertCounter(ruleModifyFullNameNotificationId, 4)
                    .end()
                .child(ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_PREVIEW_ID)
                    .assertComplete()
                    .assertSuccess()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(ACCOUNTS, 0, 0)
                        .end()
                    .end()
                .child(ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_ID)
                    .assertComplete()
                    .assertSuccess()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(ACCOUNTS, 0, 0)
                        .end()
                    .end()
                .end();
        // @formatter:on
    }

    @Override
    void assertTest400TaskAfter(TestObject<TaskType> reconTask) throws SchemaException, ObjectNotFoundException {
        // todo assert notifications, counters
        // @formatter:off
        assertTaskTree(reconTask.oid, "after")
                .display()
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .display()
                    .previewModePolicyRulesCounters()
                        .display()
                        .assertCounterMinMax(ruleDeleteNotificationId, USER_DELETE_ALLOWED + 1, USER_DELETE_ALLOWED + getThreads())
                        .end();
        // @formatter:on
    }

    @Override
    void assertTest400TaskAfterRepeatedExecution(TestObject<TaskType> reconTask) throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        assertTaskTree(reconTask.oid, "after repeated execution")
            .assertSuspended()
            .assertFatalError()
            .rootActivityState()
                .display()
                .previewModePolicyRulesCounters()
                    .display()
                    .assertCounter(ruleDeleteNotificationId, USER_DELETE_ALLOWED + 2)
                    .end();
        // @formatter:on
    }

    @Override
    void assertTest410TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        assertTaskTree(importTask.oid, "after")
            .display()
            .assertSuspended()
            .assertFatalError()
            .rootActivityState()
                .assertInProgressLocal()
                .assertFatalError()
                .previewModePolicyRulesCounters()
                    .display()
                    .end()
                .child(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_PREVIEW_ID)
                    .assertInProgressLocal()
                    .assertFatalError()
                    .progress()
                        .display()
                        .assertSuccessCount(USER_DELETE_ALLOWED, true)
                        .assertFailureCount(1, getThreads(), true)
                        .end()
                    .end()
                .child(ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_ID)
                    .display()
                    .assertRealizationState(null) // this should not even start
                    .end()
                .child(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_ID)
                    .display()
                    .assertRealizationState(null) // this should not even start
                    .end()
                .end();
        // @formatter:on
    }

    @Override
    void assertTest420TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        assertTaskTree(importTask.oid, "after")
            .display()
            .assertSuspended()
            .assertFatalError()
            .rootActivityState()
                .fullExecutionModePolicyRulesCounters()
                    .display()
                    .end()
                .end()
            .activityState(ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_PATH)
                .assertInProgressLocal()
                .assertFatalError()
                .progress()
                    .display()
                    .assertSuccessCount(USER_DELETE_ALLOWED, true)
                    .assertFailureCount(1, getThreads(), true)
                    .end();
        // @formatter:on
    }
}
