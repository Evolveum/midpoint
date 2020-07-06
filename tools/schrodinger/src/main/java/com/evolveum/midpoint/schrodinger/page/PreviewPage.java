/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.TabPanel;
import com.evolveum.midpoint.schrodinger.component.prism.show.PreviewChangesTab;
import com.evolveum.midpoint.schrodinger.page.user.ProgressPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PreviewPage extends BasicPage {

    //hack for name
    private static final String TAB_NAME_PREFIX = "\n            ";

    public PreviewChangesTab selectPanelByName(String name) {
        SelenideElement element = findTabPanel().clickTabWithName(TAB_NAME_PREFIX + name);
        return new PreviewChangesTab(this, element);
    }

    protected TabPanel findTabPanel() {
        SelenideElement tabPanelElement = $(Schrodinger.byDataId("tabbedPanel"));
        return new TabPanel<>(this, tabPanelElement);
    }

    public ProgressPage clickSave() {
        $(Schrodinger.byDataId("save")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        return new ProgressPage();
    }




}
