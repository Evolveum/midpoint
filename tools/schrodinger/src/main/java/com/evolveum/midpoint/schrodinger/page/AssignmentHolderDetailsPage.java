/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page;

import static com.codeborne.selenide.Selenide.$;

import static com.evolveum.midpoint.schrodinger.util.Utils.getModalWindowSelenideElement;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.component.common.TabPanel;
import com.evolveum.midpoint.schrodinger.component.modal.ObjectBrowserModal;
import com.evolveum.midpoint.schrodinger.page.user.ProgressPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.testng.Assert;

/**
 * Created by honchar
 */
public abstract class AssignmentHolderDetailsPage<P extends AssignmentHolderDetailsPage> extends BasicPage {

    public BasicPage clickBack() {
        $(Schrodinger.byDataResourceKey("pageAdminFocus.button.back"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
        return new BasicPage();
    }

    public ProgressPage clickSave() {
        $(Schrodinger.byDataId("save")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return new ProgressPage();
    }

    public PreviewPage clickPreview() {
        getPreviewButton().waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        feedback().isSuccess();
        return new PreviewPage();
    }

    public AssignmentHolderDetailsPage assertPreviewButtonIsNotVisible() {
        assertion.assertFalse(getPreviewButton().is(Condition.visible), "Preview button shouldn't be visible.");
        return this;
    }

    private SelenideElement getPreviewButton() {
        return $(Schrodinger.byDataId("previewChanges"));
    }

    public TabPanel getTabPanel() {
        SelenideElement tabPanelElement = $(Schrodinger.byDataId("div", "tabPanel"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        tabPanelElement.waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new TabPanel<>(this, tabPanelElement);
    }

    public AssignmentHolderBasicTab<P> selectTabBasic() {
        return new AssignmentHolderBasicTab<>((P) this, getTabSelenideElement("pageAdminFocus.basic"));
    }

    public AssignmentsTab<P> selectTabAssignments() {
        return new AssignmentsTab<>((P) this, getTabSelenideElement("pageAdminFocus.assignments"));
    }

    protected SelenideElement getTabSelenideElement(String tabTitleKey) {
        return getTabPanel().clickTab(tabTitleKey)
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
    }

    public ObjectBrowserModal<AssignmentHolderDetailsPage<P>> changeArchetype() {
        if ($(Schrodinger.byDataResourceKey("PageAdminObjectDetails.button.changeArchetype")).exists()) {
            $(Schrodinger.byDataResourceKey("PageAdminObjectDetails.button.changeArchetype"))
                    .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
            return new ObjectBrowserModal<AssignmentHolderDetailsPage<P>>(this, getModalWindowSelenideElement());
        }
        return null;
    }
}
