/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.page;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.task.EnvironmentalPerformanceTab;
import com.evolveum.midpoint.schrodinger.component.task.OperationStatisticsTab;
import com.evolveum.midpoint.schrodinger.page.task.ListTasksPage;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author skublik
 */

public class TaskPageTest extends AbstractSchrodingerTest {

    private static final File OPERATION_STATISTICS_CLEANUP_TASK_FILE = new File("./src/test/resources/configuration/objects/tasks/operation-statistics-clean-up.xml");
    private static final File ENVIRONMENTAL_PERFORMANCE_CLEANUP_TASK_FILE = new File("./src/test/resources/configuration/objects/tasks/environmental-performance-clean-up.xml");

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(OPERATION_STATISTICS_CLEANUP_TASK_FILE, ENVIRONMENTAL_PERFORMANCE_CLEANUP_TASK_FILE);
    }

    @Test
    public void test001createNewTask() {

        String name = "NewTest";
        String handler = "Recompute task";
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);
        TaskPage task = basicPage.newTask();
        task.setHandlerUriForNewTask(handler);
        task.selectTabBasic()
                .form()
                    .addAttributeValue("name", name)
                    .selectOption("recurrence","Single")
                    .selectOption("objectType","User")
                    .and()
                .and()
             .clickSave()
                .feedback()
                    .isSuccess();

        ListTasksPage tasksPage = basicPage.listTasks();
        PrismForm<AssignmentHolderBasicTab<TaskPage>> taskForm = tasksPage
                .table()
                    .search()
                        .byName()
                            .inputValue(name)
                            .updateSearch()
                        .and()
                    .clickByName(name)
                        .selectTabBasic()
                            .form();

        Assert.assertTrue(taskForm.compareInputAttributeValue("name", name));
        Assert.assertTrue(taskForm.compareInputAttributeValue("handlerUri", handler));
    }

    @Test
    public void test002cleanupOperationStatistics() {
        TaskPage taskPage = basicPage.listTasks()
                .table()
                    .search()
                        .byName()
                        .inputValue("OperationStatisticsCleanupTest")
                        .updateSearch()
                    .and()
                    .clickByName("OperationStatisticsCleanupTest");
        OperationStatisticsTab operationStatisticsTab = taskPage
                .selectTabOperationStatistics();
        Assert.assertEquals(operationStatisticsTab.getSuccessfullyProcessed(), Integer.valueOf(30),
                "The count of successfully processed objects doesn't match");
        Assert.assertEquals(operationStatisticsTab.getObjectsFailedToBeProcessed(), Integer.valueOf(3),
                "The count of failed objects doesn't match");
        Assert.assertEquals(operationStatisticsTab.getObjectsTotalCount(), Integer.valueOf(33),
                "The total count of processed objects doesn't match");
        Assert.assertTrue(taskPage
                .cleanupEnvironmentalPerformanceInfo()
                .clickYes()
                .feedback()
                .isSuccess());
        operationStatisticsTab = taskPage
                .selectTabOperationStatistics();
        Assert.assertNull(operationStatisticsTab.getSuccessfullyProcessed(),
                "The count of successfully processed objects after cleanup is not null");
        Assert.assertNull(operationStatisticsTab.getObjectsFailedToBeProcessed(),
                "The count of failed objects after cleanup is not null");
        Assert.assertNull(operationStatisticsTab.getObjectsTotalCount(),
                "The total count of processed objects after cleanup is not null");

    }

    @Test
    public void test003cleanupEnvironmentalPerformance() {
        TaskPage taskPage = basicPage.listTasks()
                .table()
                    .search()
                        .byName()
                        .inputValue("EnvironmentalPerformanceCleanupTest")
                        .updateSearch()
                    .and()
                    .clickByName("EnvironmentalPerformanceCleanupTest");
        EnvironmentalPerformanceTab environmentalPerformanceTab = taskPage
                .selectTabEnvironmentalPerformance();
        Assert.assertEquals(environmentalPerformanceTab.getStatisticsPanel().getMappingsEvaluationContainingObjectValue(), "ManRes",
                "'Containing object' value doesn't match");
        Assert.assertEquals(environmentalPerformanceTab.getStatisticsPanel().getMappingsEvaluationInvocationsCountValue(), "4",
                "'Invocations count' value doesn't match");
        Assert.assertEquals(environmentalPerformanceTab.getStatisticsPanel().getMappingsEvaluationMaxValue(), "1",
                "'Max' value doesn't match");
        Assert.assertEquals(environmentalPerformanceTab.getStatisticsPanel().getMappingsEvaluationTotalTimeValue(), "2",
                "'Total time' value doesn't match");
        Assert.assertTrue(taskPage
                .cleanupEnvironmentalPerformanceInfo()
                .clickYes()
                .feedback()
                .isSuccess());
        environmentalPerformanceTab = taskPage
                .selectTabEnvironmentalPerformance();
        Assert.assertNull(environmentalPerformanceTab.getStatisticsPanel().getMappingsEvaluationContainingObjectValue(),
                "'Containing object' value is not null after cleanup");
        Assert.assertNull(environmentalPerformanceTab.getStatisticsPanel().getMappingsEvaluationInvocationsCountValue(),
                "'Invocations count' value is not null after cleanup");
        Assert.assertNull(environmentalPerformanceTab.getStatisticsPanel().getMappingsEvaluationMaxValue(),
                "'Max' value is not null after cleanup");
        Assert.assertNull(environmentalPerformanceTab.getStatisticsPanel().getMappingsEvaluationTotalTimeValue(),
                "'Total time' value is not null after cleanup");
    }
}
