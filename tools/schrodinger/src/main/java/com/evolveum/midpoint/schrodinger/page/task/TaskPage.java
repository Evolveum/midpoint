/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.task;

import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.component.task.TaskBasicTab;

import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;

import org.openqa.selenium.By;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.SummaryPanel;
import com.evolveum.midpoint.schrodinger.page.PreviewPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TaskPage extends AssignmentHolderDetailsPage {

    public PreviewPage clickPreviewChanges() {
        $(Schrodinger.byDataId("previewChanges")).click();
        return new PreviewPage();
    }

    public SummaryPanel<TaskPage> summary() {

        SelenideElement summaryBox = $(By.cssSelector("div.info-box-content"));

        return new SummaryPanel(this, summaryBox);
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
        return   $(Schrodinger.byDataResourceKey("span", "pageTaskEdit.button.runNow")).is(Condition.visible);
    }

    @Override
    public <P extends AssignmentHolderDetailsPage> AssignmentHolderBasicTab<P> selectTabBasic() {
        SelenideElement element = findTabPanel().clickTab("pageTask.basic.title")
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        return (AssignmentHolderBasicTab<P>) new TaskBasicTab(this, element);
    }

    @Override
    public <P extends AssignmentHolderDetailsPage> AssignmentsTab<P> selectTabAssignments() {
        return null;
    }

    public AssignmentHolderBasicTab<TaskPage> selectScheduleTab(){
        SelenideElement element = findTabPanel().clickTab("pageTask.schedule.title")
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new AssignmentHolderBasicTab<TaskPage>(this, element);
    }
}
