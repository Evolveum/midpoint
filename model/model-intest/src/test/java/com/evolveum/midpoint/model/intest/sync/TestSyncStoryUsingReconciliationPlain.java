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

@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSyncStoryUsingReconciliationPlain extends TestSyncStoryUsingReconciliation {

    private static final TestTask TASK_RECONCILE_DUMMY = new TestTask(
            TEST_DIR, "task-reconcile-dummy.xml", "1e24908a-4d0f-43f2-ae45-11fa9d37245d");
    private static final TestTask TASK_RECONCILE_DUMMY_GREEN = new TestTask(
            TEST_DIR, "task-reconcile-dummy-green.xml", "b8b06b85-0916-4988-8907-bc6b10734f63");
    private static final TestTask TASK_RECONCILE_DUMMY_BLUE = new TestTask(
            TEST_DIR, "task-reconcile-dummy-blue.xml", "e10c08c6-0395-4164-bc96-f88aa7bc9c1d");

    @Override
    protected Map<Color, TestTask> getTaskMap() {
        return Map.of(
                DEFAULT, TASK_RECONCILE_DUMMY,
                GREEN, TASK_RECONCILE_DUMMY_GREEN,
                BLUE, TASK_RECONCILE_DUMMY_BLUE);
    }
}
