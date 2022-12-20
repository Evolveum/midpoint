/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.tasks;

import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Tests the thresholds in multi-node scenarios.
 *
 * Note that task-related assertions can be added to this class, although it's not strictly necessary:
 * the basic effects (actions executed on users) are observed in the superclass.
 */
public class TestThresholdsMultiNode extends TestThresholds {

    private static final TestResource<TaskType> TASK_IMPORT_SIMULATE_MULTI = new TestResource<>(TEST_DIR, "task-import-simulate-multi.xml", "aefaec62-5882-476e-b40d-6745387fbc84");
    private static final TestResource<TaskType> TASK_IMPORT_SIMULATE_EXECUTE_MULTI = new TestResource<>(TEST_DIR, "task-import-simulate-execute-multi.xml", "6e1310f0-75c4-42c4-a2dd-e8c15eb8e1ea");
    private static final TestResource<TaskType> TASK_IMPORT_EXECUTE_MULTI = new TestResource<>(TEST_DIR, "task-import-execute-multi.xml", "bb3e8b67-dbb2-4b01-a220-0774eb819791");

    private static final TestResource<TaskType> TASK_RECONCILIATION_SIMULATE_MULTI = new TestResource<>(TEST_DIR, "task-reconciliation-simulate-multi.xml", "5f14f4d7-1fe0-4f83-87f3-9fc8ed468cb1");
    private static final TestResource<TaskType> TASK_RECONCILIATION_SIMULATE_EXECUTE_MULTI = new TestResource<>(TEST_DIR, "task-reconciliation-simulate-execute-multi.xml", "5756a1c4-751c-4cd3-8edc-3d1e357dab83");
    private static final TestResource<TaskType> TASK_RECONCILIATION_EXECUTE_MULTI = new TestResource<>(TEST_DIR, "task-reconciliation-execute-multi.xml", "bc114530-a111-4baf-9888-1a51dd99a558");

    TestResource<TaskType> getSimulateTask() {
        return TASK_IMPORT_SIMULATE_MULTI;
    }

    TestResource<TaskType> getSimulateExecuteTask() {
        return TASK_IMPORT_SIMULATE_EXECUTE_MULTI;
    }

    TestResource<TaskType> getExecuteTask() {
        return TASK_IMPORT_EXECUTE_MULTI;
    }

    TestResource<TaskType> getReconciliationSimulateTask() {
        return TASK_RECONCILIATION_SIMULATE_MULTI;
    }

    TestResource<TaskType> getReconciliationSimulateExecuteTask() {
        return TASK_RECONCILIATION_SIMULATE_EXECUTE_MULTI;
    }

    TestResource<TaskType> getReconciliationExecuteTask() {
        return TASK_RECONCILIATION_EXECUTE_MULTI;
    }

    @Override
    long getTimeout() {
        return 60000;
    }

    @Override
    int getWorkerThreads() {
        return 2;
    }

    @Override
    void assertTest100Task(TestResource<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
        // @formatter:off
        assertTaskTree(importTask.oid, "after")
                .rootActivityState()
                    .display()
                    .previewModePolicyRulesCounters()
                        .display() // it's important the counters are here, not in the workers
                    .end()
                    .progress().display().end()
                    .itemProcessingStatistics().display().end()
                .end()
                .subtask(0)
                    .rootActivityState()
                        .display()
                    .end();
        // @formatter:on
    }

    @Override
    void assertTest100TaskAfterRepeatedExecution(TestResource<TaskType> importTask) {
    }

    @Override
    void assertTest110TaskAfter(TestResource<TaskType> importTask) {
    }

    @Override
    void assertTest120TaskAfter(TestResource<TaskType> importTask) {
    }

    @Override
    void assertTest200TaskAfter(TestResource<TaskType> importTask) {
    }

    @Override
    void assertTest200TaskAfterRepeatedExecution(TestResource<TaskType> importTask) {
    }

    @Override
    void assertTest210TaskAfter(TestResource<TaskType> importTask) {
    }

    @Override
    void assertTest220TaskAfter(TestResource<TaskType> importTask) {
    }

    @Override
    void assertTest300TaskAfter(TestResource<TaskType> importTask) {
    }

    @Override
    void assertTest310TaskAfter(TestResource<TaskType> reconTask) {
    }

    @Override
    void assertTest400TaskAfter(TestResource<TaskType> reconTask) {
    }

    @Override
    void assertTest400TaskAfterRepeatedExecution(TestResource<TaskType> reconTask) {
    }

    @Override
    void assertTest410TaskAfter(TestResource<TaskType> importTask) {
    }

    @Override
    void assertTest420TaskAfter(TestResource<TaskType> importTask) {
    }
}
