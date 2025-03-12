/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.tasks;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.ActivityStateAsserter;
import com.evolveum.midpoint.test.asserter.TaskAsserter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

public abstract class TestThresholdsSingleTask extends TestThresholds {

    private static final TestObject<TaskType> TASK_IMPORT_SIMULATE_SINGLE = TestObject.file(TEST_DIR, "task-import-simulate-single.xml", "c615aa46-a890-45e6-ab4a-94f14fbd204f");
    private static final TestObject<TaskType> TASK_IMPORT_SIMULATE_EXECUTE_SINGLE = TestObject.file(TEST_DIR, "task-import-simulate-execute-single.xml", "046ee785-2b23-4ceb-ba41-7a183045be24");
    // import execute single is in superclass
    private static final TestObject<TaskType> TASK_RECONCILIATION_SIMULATE_SINGLE = TestObject.file(TEST_DIR, "task-reconciliation-simulate-single.xml", "4f0c53e1-c10e-486f-9552-d2db4bfc1240");
    private static final TestObject<TaskType> TASK_RECONCILIATION_SIMULATE_EXECUTE_SINGLE = TestObject.file(TEST_DIR, "task-reconciliation-simulate-execute-single.xml", "29d2a62c-6c31-42a4-9364-ecfb0dad0825");
    private static final TestObject<TaskType> TASK_RECONCILIATION_EXECUTE_SINGLE = TestObject.file(TEST_DIR, "task-reconciliation-execute-single.xml", "7652ea69-c8bc-4320-a03e-ab37bb0accc7");

    private static final TestObject<TaskType> TASK_RECONCILIATION_EXECUTION_TIME_SINGLE = TestObject.file(TEST_DIR, "task-reconciliation-executionTime-single.xml", "5f8ab40d-3df3-487d-a1fb-1bb72dad963b");

    TestObject<TaskType> getSimulateTask() {
        return TASK_IMPORT_SIMULATE_SINGLE;
    }

    TestObject<TaskType> getSimulateExecuteTask() {
        return TASK_IMPORT_SIMULATE_EXECUTE_SINGLE;
    }

    TestObject<TaskType> getExecuteTask() {
        return TASK_IMPORT_EXECUTE_SINGLE;
    }

    TestObject<TaskType> getReconciliationSimulateTask() {
        return TASK_RECONCILIATION_SIMULATE_SINGLE;
    }

    TestObject<TaskType> getReconciliationSimulateExecuteTask() {
        return TASK_RECONCILIATION_SIMULATE_EXECUTE_SINGLE;
    }

    TestObject<TaskType> getReconciliationExecuteTask() {
        return TASK_RECONCILIATION_EXECUTE_SINGLE;
    }

    @Override
    TestObject<TaskType> getReconciliationWithExecutionTimeTask() {
        return TASK_RECONCILIATION_EXECUTION_TIME_SINGLE;
    }

    @Override
    long getTimeout() {
        return 20000;
    }

    @Override
    void assertTest100Task(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        var asserter = assertTaskTree(importTask.oid, "after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .display()
                    .previewModePolicyRulesCounters()
                        .display()
                        .assertCounterMinMax(ruleAddId, USER_ADD_ALLOWED + 1, USER_ADD_ALLOWED + getThreads())
                    .end()
                    .progress().display().end()
                    .itemProcessingStatistics().display().end();
        // @formatter:on
        additionalTest100TaskAsserts(asserter);
    }

    void additionalTest100TaskAsserts(ActivityStateAsserter<TaskAsserter<Void>> asserter) {
    }

    @Override
    void assertTest100TaskAfterRepeatedExecution(TestObject<TaskType> importTask)
            throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        var asserter2 = assertTaskTree(importTask.oid, "after repeated execution")
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
                    .end();
        // @formatter:on
        additionalTest100RepeatedExecutionAsserts(asserter2);
    }

    void additionalTest100RepeatedExecutionAsserts(ActivityStateAsserter<TaskAsserter<Void>> asserter) {
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

    void assertTest200TaskAfter(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        var asserter = assertTaskTree(importTask.oid, "task after")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .display()
                    .previewModePolicyRulesCounters()
                        .display()
                        .assertCounterMinMax(ruleModifyCostCenterId, USER_MODIFY_ALLOWED + 1, USER_MODIFY_ALLOWED + getThreads())
                    .end()
                    .itemProcessingStatistics().display().end()
                    .progress()
                        .display()
                        .assertSuccessCount(USER_MODIFY_ALLOWED, ACCOUNTS, true) // this is quite a broad range :)
                        .assertFailureCount(1, getThreads(), true)
                    .end();
        // @formatter:on

        additionalTest200Asserts(asserter);
    }

    void assertTest200TaskAfterRepeatedExecution(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        var asserter2 = assertTaskTree(importTask.oid, "task after repeated execution")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .display()
                    .previewModePolicyRulesCounters().display().end()
                    .itemProcessingStatistics().display().end();
        // @formatter:on

        additionalTest200RepeatedExecutionAsserts(asserter2);
    }

    void additionalTest200Asserts(ActivityStateAsserter<TaskAsserter<Void>> asserter) {
    }

    void additionalTest200RepeatedExecutionAsserts(ActivityStateAsserter<TaskAsserter<Void>> asserter) {
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
                        .assertCounter(ruleModifyFullNameId, 4)
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
                        .assertCounter(ruleModifyFullNameId, 4)
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
                        .assertCounter(ruleModifyFullNameId, 4)
                    .end()
                    .fullExecutionModePolicyRulesCounters()
                        .display()
                        .assertCounter(ruleModifyFullNameId, 4)
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
                        .assertCounterMinMax(ruleDeleteId, USER_DELETE_ALLOWED + 1, USER_DELETE_ALLOWED + getThreads())
                    .end();
        // @formatter:on
    }

    @Override
    void assertTest400TaskAfterRepeatedExecution(TestObject<TaskType> reconTask) throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        var asserter = assertTaskTree(reconTask.oid, "after repeated execution")
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                .display()
                .previewModePolicyRulesCounters().display().end();
        // @formatter:on

        additionalTest400RepeatedExecutionAsserts(asserter);
    }

    void additionalTest400RepeatedExecutionAsserts(ActivityStateAsserter<TaskAsserter<Void>> asserter) {
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

    @Override
    void assertTest520TaskAfter(TestObject<TaskType> reconTask) throws SchemaException, ObjectNotFoundException {

    }
}
