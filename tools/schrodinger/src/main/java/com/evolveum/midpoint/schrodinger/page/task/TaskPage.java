/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.task;

import static com.codeborne.selenide.Selenide.$;

import static com.evolveum.midpoint.schrodinger.util.Utils.getModalWindowSelenideElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.task.*;

import org.openqa.selenium.By;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.component.common.SummaryPanel;
import com.evolveum.midpoint.schrodinger.component.modal.ConfirmationModal;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.page.PreviewPage;
import com.evolveum.midpoint.schrodinger.page.user.ProgressPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TaskPage extends AssignmentHolderDetailsPage<TaskPage> {

    public PreviewPage clickPreviewChanges() {
        $(Schrodinger.byDataId("previewChanges")).click();
        return new PreviewPage();
    }

    public SummaryPanel<TaskPage> summary() {

        SelenideElement summaryBox = $(By.cssSelector("div.info-box-content"));

        return new SummaryPanel(this, summaryBox);
    }

    public ProgressPage clickSaveAndRun() {
        $(Schrodinger.byDataId("saveAndRun")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return new ProgressPage();
    }

    public TaskPage clickResume() {

        $(Schrodinger.byDataResourceKey("span", "pageTaskEdit.button.resume")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this;
    }

    public TaskPage resumeStopRefreshing() {

        $(Schrodinger.byElementAttributeValue("span", "title", "Resume refreshing")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this;
    }

    public TaskPage clickRunNow() {
        $(Schrodinger.byDataResourceKey("span", "pageTaskEdit.button.runNow")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this;
    }

    public TaskPage clickSuspend() {
        $(Schrodinger.byDataResourceKey("span", "pageTaskEdit.button.suspend")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }

    public boolean isRunNowVisible(){
        return $(Schrodinger.byDataResourceKey("span", "pageTaskEdit.button.runNow")).is(Condition.visible);
    }

    public TaskPage downloadReport() {
        $(Schrodinger.byDataResourceKey("PageTask.download.report"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }

    public ConfirmationModal<TaskPage> cleanupEnvironmentalPerformanceInfo() {
        $(By.cssSelector(".fa.fa-area-chart"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return new ConfirmationModal<TaskPage>(this, getModalWindowSelenideElement());
    }

    public ConfirmationModal<TaskPage> cleanupResults() {
        $(Schrodinger.byDataResourceKey("operationalButtonsPanel.cleanupResults"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return new ConfirmationModal<TaskPage>(this, getModalWindowSelenideElement());
    }

    public TaskPage refreshNow() {
        $(Schrodinger.byDataResourceKey("autoRefreshPanel.refreshNow"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }

    public TaskPage resumeRefreshing() {
        $(Schrodinger.byDataResourceKey("autoRefreshPanel.resumeRefreshing"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }

    @Override
    public AssignmentHolderBasicTab<TaskPage> selectTabBasic() {
        return new TaskBasicTab(this, getTabSelenideElement(("pageTask.basic.title")));
    }

    @Override
    public  AssignmentsTab<TaskPage> selectTabAssignments() {
        return null;
    }

    public AssignmentHolderBasicTab<TaskPage> selectScheduleTab(){
        return new AssignmentHolderBasicTab<TaskPage>(this, getTabSelenideElement(("pageTask.schedule.title")));
    }

    public OperationStatisticsTab selectTabOperationStatistics() {
        return new OperationStatisticsTab(this, getTabSelenideElement(("pageTask.operationStats.title")));
    }

    public EnvironmentalPerformanceTab selectTabEnvironmentalPerformance() {
        return new EnvironmentalPerformanceTab(this, getTabSelenideElement(("pageTask.environmentalPerformance.title")));
    }

    public InternalPerformanceTab selectTabInternalPerformance() {
        return new InternalPerformanceTab(this, getTabSelenideElement(("pageTask.internalPerformance.title")));
    }

    public ResultTab selectTabResult() {
        return new ResultTab(this, getTabSelenideElement(("pageTask.result.title")));
    }

     public ErrorsTab selectTabErrors() {
        return new ErrorsTab(this, getTabSelenideElement(("pageTask.errors.title")));
    }

    public TaskPage setHandlerUriForNewTask(String handler) {
        SelenideElement handlerElement = $(Schrodinger.byDataResourceKey("a", "TaskHandlerSelectorPanel.selector.header"));
        selectTabBasic().form().addAttributeValue("handlerUri", handler.substring(0, (handler.length() - 1)));
        $(Schrodinger.byElementAttributeValue("li", "textvalue", handler)).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        handlerElement.waitWhile(Condition.exist, MidPoint.TIMEOUT_MEDIUM_6_S);
        return this;
    }
}
