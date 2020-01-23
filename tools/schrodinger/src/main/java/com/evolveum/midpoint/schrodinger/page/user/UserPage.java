/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.user;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.component.common.SummaryPanel;
import com.evolveum.midpoint.schrodinger.component.user.*;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.page.PreviewPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;
import static com.evolveum.midpoint.schrodinger.util.Utils.setOptionChecked;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UserPage extends AssignmentHolderDetailsPage {

    public UserPage checkForce() {
        setOptionChecked("executeOptions:force", true);
        return this;
    }

    public UserPage checkReconcile() {
        setOptionChecked("executeOptions:reconcileContainer:container:check", true);
        return this;
    }

    public UserPage checkExecuteAfterAllApprovals() {
        setOptionChecked("executeOptions:executeAfterAllApprovals", true);
        return this;
    }

    public UserPage checkKeepDisplayingResults() {
        setOptionChecked("executeOptions:keepDisplayingResultsContainer:container:check", true);
        return this;
    }

    public UserPage uncheckForce() {
        setOptionChecked("executeOptions:force", false);
        return this;
    }

    public UserPage uncheckReconcile() {
        setOptionChecked("executeOptions:reconcileLabel:reconcile", false);
        return this;
    }

    public UserPage uncheckExecuteAfterAllApprovals() {
        setOptionChecked("executeOptions:executeAfterAllApprovals", false);
        return this;
    }

    public UserPage uncheckKeepDisplayingResults() {
        setOptionChecked("executeOptions:keepDisplayingResultsContainer:keepDisplayingResults", false);
        return this;
    }

    public PreviewPage clickPreviewChanges() {
        $(Schrodinger.byDataId("previewChanges")).click();
        return new PreviewPage();
    }


    public UserProjectionsTab selectTabProjections() {
        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.projections");

        return new UserProjectionsTab(this, element);
    }

    public UserPersonasTab selectTabPersonas() {
        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.personas");

        return new UserPersonasTab(this, element);
    }

    public UserTasksTab selectTabTasks() {
        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.tasks");

        return new UserTasksTab(this, element);
    }

    public UserHistoryTab selectTabHistory() {
        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.objectHistory");

        return new UserHistoryTab(this, element);
    }

    public UserDelegationsTab selectTabDelegations() {
        SelenideElement element = findTabPanel().clickTab("FocusType.delegations");

        return new UserDelegationsTab(this, element);
    }

    public UserDelegatedToMeTab selectTabDelegatedToMe() {
        SelenideElement element = findTabPanel().clickTab("FocusType.delegatedToMe");

        return new UserDelegatedToMeTab(this, element);
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

    @Override
    public AssignmentHolderBasicTab<UserPage> selectTabBasic(){
        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.basic")
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new AssignmentHolderBasicTab<UserPage>(this, element);
    }

    @Override
    public AssignmentsTab<UserPage> selectTabAssignments(){
        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.assignments");

        return new AssignmentsTab<UserPage>(this, element);
    }

}
