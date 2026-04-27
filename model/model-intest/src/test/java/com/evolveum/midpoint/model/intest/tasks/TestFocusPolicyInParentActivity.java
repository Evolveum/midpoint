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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

public class TestFocusPolicyInParentActivity extends TestFocusPolicies {

    private static final Trace LOGGER = TraceManager.getTrace(TestFocusPolicyInParentActivity.class);

    private static final TestTask TASK_IMPORT =
            TestTask.file(TEST_DIR, "task-100-import.xml", "cb9319a6-add7-4841-bfb1-f3f3f3a4a435");

    private static final TestTask TASK_IMPORT_SIMULATE =
            TestTask.file(TEST_DIR, "task-100-import-simulate.xml", "0bc80ef5-9ad9-4343-b4be-1a588070c6a9");

    private static final TestTask TASK_IMPORT_SIMULATE_EXECUTE =
            TestTask.file(TEST_DIR, "task-100-import-simulate-execute.xml", "d3326c20-6607-4cd4-a1be-f55189fbc71a");

    private static final TestTask TASK_RECONCILIATION =
            TestTask.file(TEST_DIR, "task-100-reconciliation.xml", "ee5791ed-b3af-43b2-bc5e-644d4b020100");

    private static final TestTask TASK_RECONCILIATION_SIMULATE =
            TestTask.file(TEST_DIR, "task-100-reconciliation-simulate.xml", "74094e36-c3fb-4ed6-8a72-73ea23626fa2");

    private static final TestTask TASK_RECONCILIATION_SIMULATE_EXECUTE =
            TestTask.file(TEST_DIR, "task-100-reconciliation-simulate-execute.xml", "4a0e5fe6-b512-4fb0-a750-7bec5d6e0fbb");

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportAdd10Simulate() {
        return transplantRolePolicyForSimulateOrExecuteTask(ROLE_ADD_10_NOTIFICATION);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportAdd10SimulateExecute() {
        return transplantRolePolicyForSimulateExecuteTask(ROLE_ADD_10_NOTIFICATION);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportAdd10Execute() {
        return transplantRolePolicyForSimulateOrExecuteTask(ROLE_ADD_10_NOTIFICATION);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyCostCenter5Execute() {
        return transplantRolePolicyForSimulateOrExecuteTask(ROLE_MODIFY_5_COST_CENTER_NOTIFICATION);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyCostCenter5SimulateExecute() {
        return transplantRolePolicyForSimulateExecuteTask(ROLE_MODIFY_5_COST_CENTER_NOTIFICATION);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyCostCenter5Simulate() {
        return transplantRolePolicyForSimulateOrExecuteTask(ROLE_MODIFY_5_COST_CENTER_NOTIFICATION);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesImportModifyFullName5SimulateExecute() {
        return transplantRolePolicyForSimulateOrExecuteTask(ROLE_MODIFY_5_FULL_NAME_NOTIFICATION);
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesReconModifyFullName5SimulateExecute() {
        return task ->
                transplantRolePolicy(
                        ROLE_MODIFY_5_FULL_NAME_NOTIFICATION,
                        new PolicyActionsType().suspendTask(new SuspendTaskPolicyActionType()),
                        task,
                        ActivityPath.empty()
                );
    }

    @Override
    protected Consumer<PrismObject<TaskType>> customizePoliciesReconcileDelete5Simulate() {
        return task ->
                transplantRolePolicy(
                        ROLE_DELETE_5_NOTIFICATION,
                        new PolicyActionsType().suspendTask(new SuspendTaskPolicyActionType()),
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

    // todo check usage and move to parent class if possible

    /**
     * Transplant for simulate OR execute task. If task have simulate-execute,
     * please use {@link #transplantRolePolicyForSimulateExecuteTask(TestObject)}.
     */
    private Consumer<PrismObject<TaskType>> transplantRolePolicyForSimulateOrExecuteTask(TestObject<RoleType> source) {
        return task ->
                transplantRolePolicy(
                        source,
                        new PolicyActionsType().suspendTask(new SuspendTaskPolicyActionType()),
                        task,
                        ActivityPath.empty());
    }

    // todo check usage and move to parent class if possible

    /**
     * Transplant role policy for simulate-execute task. If task has simulate OR execute ONLY
     * please use {@link #transplantRolePolicyForSimulateOrExecuteTask(TestObject)}.
     */
    private Consumer<PrismObject<TaskType>> transplantRolePolicyForSimulateExecuteTask(TestObject<RoleType> source) {
        return task -> {
            transplantRolePolicy(
                    source,
                    new PolicyActionsType().suspendTask(new SuspendTaskPolicyActionType()),
                    task,
                    ActivityPath.fromId("simulate"));

            transplantRolePolicy(
                    source,
                    new PolicyActionsType().suspendTask(new SuspendTaskPolicyActionType()),
                    task,
                    ActivityPath.fromId("execute"));
        };
    }

    @Override
    void assertTest100Task(TestObject<TaskType> importTask) throws Exception {
        // todo assert notifications

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
        // todo assert notifications

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
