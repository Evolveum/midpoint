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
import com.evolveum.midpoint.schrodinger.component.task.OperationStatisticsTab;
import com.evolveum.midpoint.schrodinger.page.task.ListTasksPage;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

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
    private static final File RESULTS_CLEANUP_TASK_FILE = new File("./src/test/resources/configuration/objects/tasks/results-clean-up.xml");

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(OPERATION_STATISTICS_CLEANUP_TASK_FILE, ENVIRONMENTAL_PERFORMANCE_CLEANUP_TASK_FILE, RESULTS_CLEANUP_TASK_FILE);
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

        taskForm.assertInputAttributeValueMatches("name", name);
        taskForm.assertInputAttributeValueMatches("handlerUri", handler);
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
        operationStatisticsTab.assertSuccessfullyProcessedCountMatch(30);
        operationStatisticsTab.assertObjectsFailedToBeProcessedCountMatch(3);
        operationStatisticsTab.assertObjectsTotalCountMatch(33);
        taskPage
                .cleanupEnvironmentalPerformanceInfo()
                .clickYes()
                .feedback()
                .assertSuccess();
        operationStatisticsTab = taskPage
                .selectTabOperationStatistics();
        operationStatisticsTab.assertSuccessfullyProcessedIsNull();
        operationStatisticsTab.assertObjectsFailedToBeProcessedIsNull();
        operationStatisticsTab.assertObjectsTotalIsNull();

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
        taskPage
                .selectTabEnvironmentalPerformance()
                    .getStatisticsPanel()
                        .assertMappingsEvaluationContainingObjectValueMatch("ManRes")
                        .assertMappingsEvaluationInvocationsCountValueMatch("4")
                        .assertMappingsEvaluationMaxValueMatch("1")
                        .assertMappingsEvaluationTotalTimeValueMatch("2");
        taskPage
                .cleanupEnvironmentalPerformanceInfo()
                .clickYes()
                .feedback()
                .assertSuccess();
        taskPage
                .selectTabEnvironmentalPerformance()
                    .getStatisticsPanel()
                        .assertMappingsEvaluationContainingObjectValueMatch(null)
                        .assertMappingsEvaluationInvocationsCountValueMatch(null)
                        .assertMappingsEvaluationMaxValueMatch(null)
                        .assertMappingsEvaluationTotalTimeValueMatch(null);
    }

    @Test
    public void test004cleanupTaskResults() {
        TaskPage taskPage = basicPage.listTasks()
                .table()
                    .search()
                        .byName()
                        .inputValue("ResultCleanupTest")
                        .updateSearch()
                    .and()
                    .clickByName("ResultCleanupTest");
        String tokenValue = "1000000000000003831";
        taskPage
                .selectTabResult()
                    .getResultsTable()
                        .assertTableContainsText(tokenValue)
                        .and()
                    .assertOperationValueByTokenMatch(tokenValue, "run")
                    .assertStatusValueByTokenMatch(tokenValue, "IN_PROGRESS")
                    .assertTimestampValueByTokenMatch(tokenValue, "1/1/20, 12:00:00 AM")
                    .assertMessageValueByTokenMatch(tokenValue, "Test results");
        taskPage
                .cleanupResults()
                    .clickYes()
                        .feedback()
                        .assertSuccess();
        taskPage
                .selectTabResult()
                    .getResultsTable()
                        .assertTableDoesntContainText(tokenValue)
                        .and()
                    .assertOperationValueByTokenMatch(tokenValue, null)
                    .assertStatusValueByTokenMatch(tokenValue, null)
                    .assertTimestampValueByTokenMatch(tokenValue, null)
                    .assertMessageValueByTokenMatch(tokenValue, null);

    }
}
