/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.cases;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.cases.OperationRequestTab;
import com.evolveum.midpoint.schrodinger.component.cases.WorkitemsTab;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Kate Honchar.
 */
public class CasePage extends AssignmentHolderDetailsPage {

    public ChildrenCasesTab selectTabChildren(){
        SelenideElement element = getTabPanel().clickTab("PageCase.childCasesTab");

        return new ChildrenCasesTab(this, element);
    }

    public OperationRequestTab selectTabOperationRequest(){
        SelenideElement element = getTabPanel().clickTab("PageCase.operationRequestTab");

        return new OperationRequestTab(this, element);
    }

    public WorkitemsTab selectTabWorkitems(){
        SelenideElement element = getTabPanel().clickTab("PageCase.workitemsTab");

        return new WorkitemsTab(this, element);
    }

    public TaskPage navigateToTask() {
        if ($(Schrodinger.byDataResourceKey("PageCase.navigateToTask")).exists()) {
            $(Schrodinger.byDataResourceKey("PageCase.navigateToTask"))
                    .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
            $(By.cssSelector(".info-box-icon.summary-panel-task")).waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
            return new TaskPage();
        }
        return null;
    }
}
