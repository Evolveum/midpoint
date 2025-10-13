/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.sync;

import static com.evolveum.midpoint.model.intest.sync.AbstractSynchronizationStoryTest.Color.*;

import java.util.Map;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.test.TestTask;

/**
 * Uses reconciliation with individual sub-activities run in separate subtasks.
 *
 * Also, it takes a little longer than standard test because of the overhead.
 *
 * NOTE: The utility of this test is questionable, as the synchronization story test is about the correctness
 * of the synchronization algorithms themselves, not about the distribution features of the reconciliation activity.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSyncStoryUsingReconciliationWithSubtasks extends TestSyncStoryUsingReconciliation {

    private static final TestTask TASK_RECONCILE_DUMMY = new TestTask(
            TEST_DIR, "task-reconcile-dummy-with-subtasks.xml", "7b801396-a602-48b4-8b39-26d7a48f6501");
    private static final TestTask TASK_RECONCILE_DUMMY_GREEN = new TestTask(
            TEST_DIR, "task-reconcile-dummy-green-with-subtasks.xml", "0ed7212e-368f-4928-9799-2960b87c8840");
    private static final TestTask TASK_RECONCILE_DUMMY_BLUE = new TestTask(
            TEST_DIR, "task-reconcile-dummy-blue-with-subtasks.xml", "ed388ce5-bc16-4489-9764-8b9abec547fb");

    @Override
    protected Map<Color, TestTask> getTaskMap() {
        return Map.of(
                DEFAULT, TASK_RECONCILE_DUMMY,
                GREEN, TASK_RECONCILE_DUMMY_GREEN,
                BLUE, TASK_RECONCILE_DUMMY_BLUE);
    }
}
