/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import static com.evolveum.midpoint.model.intest.sync.AbstractSynchronizationStoryTest.Color.*;

import java.util.Map;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.test.TestTask;

/**
 * Uses multithreaded reconciliation task.
 *
 * NOTE: The utility of this test is questionable, as the synchronization story test is about the correctness
 * of the synchronization algorithms themselves, not about the distribution features of the reconciliation activity.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSyncStoryUsingReconciliationMultithreaded extends TestSyncStoryUsingReconciliation {

    private static final TestTask TASK_RECONCILE_DUMMY = new TestTask(
            TEST_DIR, "task-reconcile-dummy-multithreaded.xml", "74d4297d-cdeb-43e6-a7f9-0af38d36de12");
    private static final TestTask TASK_RECONCILE_DUMMY_GREEN = new TestTask(
            TEST_DIR, "task-reconcile-dummy-green-multithreaded.xml", "36a53692-3324-443e-a683-3c23dd48a276");
    private static final TestTask TASK_RECONCILE_DUMMY_BLUE = new TestTask(
            TEST_DIR, "task-reconcile-dummy-blue-multithreaded.xml", "6aea358b-2043-42fa-b2db-1cfdccb26725");

    @Override
    protected Map<Color, TestTask> getTaskMap() {
        return Map.of(
                DEFAULT, TASK_RECONCILE_DUMMY,
                GREEN, TASK_RECONCILE_DUMMY_GREEN,
                BLUE, TASK_RECONCILE_DUMMY_BLUE);
    }
}
