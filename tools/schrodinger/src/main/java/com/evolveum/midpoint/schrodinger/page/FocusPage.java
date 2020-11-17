/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.ProjectionsTab;
import com.evolveum.midpoint.schrodinger.component.common.SummaryPanel;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

import static com.evolveum.midpoint.schrodinger.util.Utils.setOptionCheckedByName;

/**
 * @author skublik
 */

public class FocusPage extends AssignmentHolderDetailsPage {

    public FocusPage checkForce() {
        setOptionCheckedByName("executeOptions:force", true);
        return this;
    }

    public FocusPage checkReconcile() {
        setOptionCheckedByName("executeOptions:reconcileContainer:container:check", true);
        return this;
    }

    public FocusPage checkExecuteAfterAllApprovals() {
        setOptionCheckedByName("executeOptions:executeAfterAllApprovals", true);
        return this;
    }

    public FocusPage checkKeepDisplayingResults() {
        setOptionCheckedByName("executeOptions:keepDisplayingResultsContainer:container:check", true);
        return this;
    }

    public FocusPage uncheckForce() {
        setOptionCheckedByName("executeOptions:force", false);
        return this;
    }

    public FocusPage uncheckReconcile() {
        setOptionCheckedByName("executeOptions:reconcileLabel:reconcile", false);
        return this;
    }

    public FocusPage uncheckExecuteAfterAllApprovals() {
        setOptionCheckedByName("executeOptions:executeAfterAllApprovals", false);
        return this;
    }

    public FocusPage uncheckKeepDisplayingResults() {
        setOptionCheckedByName("executeOptions:keepDisplayingResultsContainer:keepDisplayingResults", false);
        return this;
    }

    public <F extends FocusPage> ProjectionsTab<F> selectTabProjections() {
        SelenideElement element = getTabPanel().clickTab("pageAdminFocus.projections");
        Selenide.sleep(2000);
        return new ProjectionsTab<F>(this, element);
    }


    public SummaryPanel<UserPage> summary() {

        SelenideElement summaryPanel = $(By.cssSelector("div.info-box-content"));

        return new SummaryPanel(this, summaryPanel);
    }

    public boolean isActivationState(String state) {

        SelenideElement summaryPanel = $(Schrodinger.byDataId("span", "summaryTagLabel")).waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        if (state != null || !(state.isEmpty())) {
            return state.equals(summaryPanel.getText());
        } else {
            return "".equals(summaryPanel.getText());
        }
    }
}
