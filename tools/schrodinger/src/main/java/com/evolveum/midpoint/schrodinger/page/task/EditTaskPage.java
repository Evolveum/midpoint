/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.task;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.component.common.SummaryPanel;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;


/**
 * Created by matus on 3/21/2018.
 */
public class EditTaskPage extends AssignmentHolderDetailsPage {


    public SummaryPanel<EditTaskPage> summary() {

        SelenideElement summaryBox = $(By.cssSelector("div.info-box-content"));

        return new SummaryPanel(this, summaryBox);
    }

    public EditTaskPage clickResume() {

        $(Schrodinger.byDataResourceKey("a", "pageTaskEdit.button.resume")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this;
    }

    public EditTaskPage clickSuspend() {
        $(Schrodinger.byDataResourceKey("a", "pageTaskEdit.button.suspend")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }

    public boolean isRunNowVisible(){
        return   $(Schrodinger.byDataResourceKey("a", "pageTaskEdit.button.runNow")).is(Condition.visible);
    }

    public EditTaskPage clickRunNow() {
        $(Schrodinger.byDataResourceKey("a", "pageTaskEdit.button.runNow"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return this;
    }

    @Override
    public AssignmentHolderBasicTab<EditTaskPage> selectTabBasic(){
        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.basic")
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new AssignmentHolderBasicTab<EditTaskPage>(this, element);
    }

    @Override
    public AssignmentsTab selectTabAssignments(){
        //TODO implement
        return null;
    }
}
