/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.component.common.TabPanel;
import com.evolveum.midpoint.schrodinger.page.user.ProgressPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar
 */
public abstract class AssignmentHolderDetailsPage extends BasicPage {

    public BasicPage clickBack() {
        $(Schrodinger.byDataResourceKey("pageAdminFocus.button.back"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
        return new BasicPage();
    }

    public ProgressPage clickSave() {
        $(Schrodinger.byDataId("save")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return new ProgressPage();
    }

    public PreviewPage clickPreview() {
        $(Schrodinger.byDataId("previewChanges")).waitUntil(Condition.visible, MidPoint.TIMEOUT_MEDIUM_6_S).click();
        return new PreviewPage();
    }

    protected TabPanel findTabPanel() {
        SelenideElement tabPanelElement = $(Schrodinger.byDataId("div", "tabPanel"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new TabPanel<>(this, tabPanelElement);
    }

    public abstract <P extends AssignmentHolderDetailsPage> AssignmentHolderBasicTab<P> selectTabBasic();
//        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.basic")
//                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
//
//        return new AssignmentHolderBasicTab<>(getParentComponent(), element);
//    }

    public abstract <P extends AssignmentHolderDetailsPage> AssignmentsTab<P> selectTabAssignments();
//        SelenideElement element = findTabPanel().clickTab("pageAdminFocus.assignments");
//
//        return new AssignmentsTab<>(getParentComponent(), element);
//    }

}
