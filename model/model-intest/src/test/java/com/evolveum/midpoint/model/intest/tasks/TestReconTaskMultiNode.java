/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.tasks;

import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

/**
 * Tests basic functionality of reconciliation tasks.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReconTaskMultiNode extends TestReconTask {

    private static final TestResource<TaskType> TASK_RECONCILIATION_MULTINODE = new TestResource<>(TEST_DIR, "task-reconciliation-multinode.xml", "19418f29-096e-4d15-b3df-e579e33ca405");

    @Override
    TestResource<TaskType> getReconciliationTask() {
        return TASK_RECONCILIATION_MULTINODE;
    }
}
