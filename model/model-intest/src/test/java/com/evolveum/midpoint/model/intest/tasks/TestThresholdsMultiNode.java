/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.tasks;

import com.evolveum.midpoint.test.TestObject;
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

    private static final TestObject<TaskType> TASK_IMPORT_SIMULATE_MULTI = TestObject.file(TEST_DIR, "task-import-simulate-multi.xml", "aefaec62-5882-476e-b40d-6745387fbc84");
    private static final TestObject<TaskType> TASK_IMPORT_SIMULATE_EXECUTE_MULTI = TestObject.file(TEST_DIR, "task-import-simulate-execute-multi.xml", "6e1310f0-75c4-42c4-a2dd-e8c15eb8e1ea");
    private static final TestObject<TaskType> TASK_IMPORT_EXECUTE_MULTI = TestObject.file(TEST_DIR, "task-import-execute-multi.xml", "bb3e8b67-dbb2-4b01-a220-0774eb819791");

    private static final TestObject<TaskType> TASK_RECONCILIATION_SIMULATE_MULTI = TestObject.file(TEST_DIR, "task-reconciliation-simulate-multi.xml", "5f14f4d7-1fe0-4f83-87f3-9fc8ed468cb1");
    private static final TestObject<TaskType> TASK_RECONCILIATION_SIMULATE_EXECUTE_MULTI = TestObject.file(TEST_DIR, "task-reconciliation-simulate-execute-multi.xml", "5756a1c4-751c-4cd3-8edc-3d1e357dab83");
    private static final TestObject<TaskType> TASK_RECONCILIATION_EXECUTE_MULTI = TestObject.file(TEST_DIR, "task-reconciliation-execute-multi.xml", "bc114530-a111-4baf-9888-1a51dd99a558");

    TestObject<TaskType> getSimulateTask() {
        return TASK_IMPORT_SIMULATE_MULTI;
    }

    TestObject<TaskType> getSimulateExecuteTask() {
        return TASK_IMPORT_SIMULATE_EXECUTE_MULTI;
    }

    TestObject<TaskType> getExecuteTask() {
        return TASK_IMPORT_EXECUTE_MULTI;
    }

    TestObject<TaskType> getReconciliationSimulateTask() {
        return TASK_RECONCILIATION_SIMULATE_MULTI;
    }

    TestObject<TaskType> getReconciliationSimulateExecuteTask() {
        return TASK_RECONCILIATION_SIMULATE_EXECUTE_MULTI;
    }

    TestObject<TaskType> getReconciliationExecuteTask() {
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
    void assertTest100Task(TestObject<TaskType> importTask) throws SchemaException, ObjectNotFoundException {
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
    void assertTest100TaskAfterRepeatedExecution(TestObject<TaskType> importTask) {
    }

    @Override
    void assertTest110TaskAfter(TestObject<TaskType> importTask) {
    }

    @Override
    void assertTest120TaskAfter(TestObject<TaskType> importTask) {
    }

    @Override
    void assertTest200TaskAfter(TestObject<TaskType> importTask) {
    }

    @Override
    void assertTest200TaskAfterRepeatedExecution(TestObject<TaskType> importTask) {
    }

    @Override
    void assertTest210TaskAfter(TestObject<TaskType> importTask) {
    }

    @Override
    void assertTest220TaskAfter(TestObject<TaskType> importTask) {
    }

    @Override
    void assertTest300TaskAfter(TestObject<TaskType> importTask) {
    }

    @Override
    void assertTest310TaskAfter(TestObject<TaskType> reconTask) {
    }

    @Override
    void assertTest400TaskAfter(TestObject<TaskType> reconTask) {
    }

    @Override
    void assertTest400TaskAfterRepeatedExecution(TestObject<TaskType> reconTask) {
    }

    @Override
    void assertTest410TaskAfter(TestObject<TaskType> importTask) {
    }

    @Override
    void assertTest420TaskAfter(TestObject<TaskType> importTask) {
    }

    @Override
    void assertTest520TaskAfter(TestObject<TaskType> reconTask) throws SchemaException, ObjectNotFoundException {
        // todo assert something
        fail("assertions not implemented yet");
    }
}
