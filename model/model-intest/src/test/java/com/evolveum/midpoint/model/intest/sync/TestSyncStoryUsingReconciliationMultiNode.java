/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import com.evolveum.midpoint.test.TestTask;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import java.util.Map;

import static com.evolveum.midpoint.model.intest.sync.AbstractSynchronizationStoryTest.Color.*;

/**
 * Uses multiple worker tasks.
 *
 * Shouldn't be run under H2 because of too much contention.
 * Also, it takes a little longer than standard reconciliation test because of the overhead.
 *
 * NOTE: The utility of this test is questionable, as the synchronization story test is about the correctness
 * of the synchronization algorithms themselves, not about the distribution features of the reconciliation activity.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSyncStoryUsingReconciliationMultiNode extends TestSyncStoryUsingReconciliation {

    private static final TestTask TASK_RECONCILE_DUMMY = new TestTask(
            TEST_DIR, "task-reconcile-dummy-multinode.xml", "c36de89d-5ee2-469c-935b-ff34560d4e77");
    private static final TestTask TASK_RECONCILE_DUMMY_GREEN = new TestTask(
            TEST_DIR, "task-reconcile-dummy-green-multinode.xml", "87100587-1f5c-468f-8e89-8731650833fd");
    private static final TestTask TASK_RECONCILE_DUMMY_BLUE = new TestTask(
            TEST_DIR, "task-reconcile-dummy-blue-multinode.xml", "fa8abd8d-c379-46c1-aec8-38afb1a2d469");

    @Override
    protected Map<Color, TestTask> getTaskMap() {
        return Map.of(
                DEFAULT, TASK_RECONCILE_DUMMY,
                GREEN, TASK_RECONCILE_DUMMY_GREEN,
                BLUE, TASK_RECONCILE_DUMMY_BLUE);
    }
}
