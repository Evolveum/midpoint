/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

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
        assertThat(information).isInstanceOf(LegacyTaskInformation.class);
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
        assertThat(information).isInstanceOf(LegacyTaskInformation.class);
        assertThat(information.getProgressDescriptionShort()).as("progress description").isEqualTo("0");
    }
}
