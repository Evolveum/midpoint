/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import com.evolveum.midpoint.schema.util.task.ActivityBasedTaskInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.LegacyTaskInformation;
import com.evolveum.midpoint.schema.util.task.TaskInformation;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Tests miscellaneous kinds of tasks that do not deserve their own test class.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMiscTasks extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/misc");

    private static final TestResource<TaskType> TASK_DELETE_REPORT_DATA =
            new TestResource<>(TEST_DIR, "task-delete-report-data.xml", "d3351dff-4c72-4985-8a9c-f8d46ffb328f");
    private static final TestResource<TaskType> TASK_DELETE_MISSING_QUERY_LEGACY =
            new TestResource<>(TEST_DIR, "task-delete-missing-query-legacy.xml", "c637b877-efe8-43ae-b87f-738bff9062fb");
    private static final TestResource<TaskType> TASK_DELETE_MISSING_TYPE =
            new TestResource<>(TEST_DIR, "task-delete-missing-type.xml", "889d1313-2a7f-4112-a996-2b84f1f000a7");
    private static final TestResource<TaskType> TASK_DELETE_SELECTED_USERS =
            new TestResource<>(TEST_DIR, "task-delete-selected-users.xml", "623f261c-4c63-445b-a714-dcde118f227c");

    /**
     * MID-7277
     */
    @Test
    public void test100DeleteReportData() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ReportDataType reportData = new ReportDataType(PrismContext.get())
                .name("waste data");
        repoAddObject(reportData.asPrismObject(), result);

        when();

        addTask(TASK_DELETE_REPORT_DATA, result);
        waitForTaskCloseOrSuspend(TASK_DELETE_REPORT_DATA.oid, 10000);

        then();

        TaskType taskAfter = assertTask(TASK_DELETE_REPORT_DATA.oid, "after")
                .display()
                .assertSuccess()
                .assertClosed()
                .assertProgress(1)
                .getObjectable();

        TaskInformation information = TaskInformation.createForTask(taskAfter, taskAfter);
        assertThat(information).isInstanceOf(ActivityBasedTaskInformation.class);
        assertThat(information.getProgressDescriptionShort()).as("progress description").isEqualTo("100.0%");

        assertNoRepoObject(ReportDataType.class, reportData.getOid());
    }

    /**
     * MID-7277
     */
    @Test
    public void test110DeleteReportDataAgain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        suspendAndDeleteTasks(TASK_DELETE_REPORT_DATA.oid);
        addTask(TASK_DELETE_REPORT_DATA, result);
        waitForTaskCloseOrSuspend(TASK_DELETE_REPORT_DATA.oid, 10000);

        then();

        TaskType taskAfter = assertTask(TASK_DELETE_REPORT_DATA.oid, "after")
                .display()
                .assertSuccess()
                .assertClosed()
                .assertProgress(0)
                .getObjectable();

        TaskInformation information = TaskInformation.createForTask(taskAfter, taskAfter);
        assertThat(information).isInstanceOf(ActivityBasedTaskInformation.class);
        assertThat(information.getProgressDescriptionShort()).as("progress description").isEqualTo("0");
    }

    /**
     * Checks that deletion activity does not allow running without type and/or query.
     */
    @Test
    public void test120DeleteWithoutQueryOrType() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("no query");
        addTask(TASK_DELETE_MISSING_QUERY_LEGACY, result);
        waitForTaskCloseOrSuspend(TASK_DELETE_MISSING_QUERY_LEGACY.oid, 10000);

        then("no query");
        assertTask(TASK_DELETE_MISSING_QUERY_LEGACY.oid, "after")
                .display()
                .assertFatalError()
                .assertResultMessageContains("Object query must be specified");

        when("no type");
        addTask(TASK_DELETE_MISSING_TYPE, result);
        waitForTaskCloseOrSuspend(TASK_DELETE_MISSING_TYPE.oid, 10000);

        then("no type");
        assertTask(TASK_DELETE_MISSING_TYPE.oid, "after")
                .display()
                .assertFatalError()
                .assertResultMessageContains("Object type must be specified");
    }

    /**
     * Checks that deletion activity really deletes something, and avoids deleting indestructible objects.
     *
     * - user-to-delete-1-normal will be deleted,
     * - user-to-delete-2-indestructible will be kept.
     */
    @Test
    public void test130DeleteObjects() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        UserType user1 = new UserType(PrismContext.get())
                .name("user-to-delete-1-normal");
        repoAddObject(user1.asPrismObject(), result);

        UserType user2 = new UserType(PrismContext.get())
                .name("user-to-delete-2-indestructible")
                .indestructible(true);
        repoAddObject(user2.asPrismObject(), result);

        when();
        addTask(TASK_DELETE_SELECTED_USERS, result);
        waitForTaskCloseOrSuspend(TASK_DELETE_SELECTED_USERS.oid, 10000);

        then();
        // @formatter:off
        assertTask(TASK_DELETE_SELECTED_USERS.oid, "after")
                .display()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .assertCommitted(1, 0, 1)
                    .end()
                    .itemProcessingStatistics()
                        .assertTotalCounts(1, 0, 1);
        // @formatter:on

        assertNoRepoObject(UserType.class, user1.getOid());
        assertUser(user2.getOid(), "after")
                .display()
                .assertName("user-to-delete-2-indestructible");
    }
}
