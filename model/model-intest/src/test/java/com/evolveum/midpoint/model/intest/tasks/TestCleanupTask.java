/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * TODO finish this test (objects, asserts ...)
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCleanupTask extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/tasks");

    private static final TestResource<TaskType> TASK_CLEANUP_LEGACY = new TestResource<>(TEST_DIR, "task-cleanup-legacy.xml", "0726d8b4-641e-4a01-9878-a11cabace465");
    private static final TestResource<TaskType> TASK_CLEANUP = new TestResource<>(TEST_DIR, "task-cleanup.xml", "08f630d0-0459-49c7-9c70-a813ba2e9da6");

    @Test
    public void test100RunLegacy() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        addTask(TASK_CLEANUP_LEGACY, result);
        waitForTaskCloseOrSuspend(TASK_CLEANUP_LEGACY.oid);

        then();
        assertTask(TASK_CLEANUP_LEGACY.oid, "after")
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .display();
    }

    @Test
    public void test200RunNew() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        addTask(TASK_CLEANUP, result);
        waitForTaskCloseOrSuspend(TASK_CLEANUP.oid);

        then();
        assertTask(TASK_CLEANUP.oid, "after")
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .display();
    }
}
