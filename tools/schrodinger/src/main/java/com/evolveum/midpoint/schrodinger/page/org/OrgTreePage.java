/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.org;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.TabPanel;
import com.evolveum.midpoint.schrodinger.component.org.OrgRootTab;
import com.evolveum.midpoint.schrodinger.component.user.UserTasksTab;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class OrgTreePage extends BasicPage {

    public OrgRootTab selectTabWithRootOrg(String rootOrgName) {
        SelenideElement element = getTabPanel().clickTabWithName(rootOrgName);

        return new OrgRootTab(this, element);
    }

    private TabPanel getTabPanel() {
        SelenideElement tabPanelElement = $(Schrodinger.byDataId("div", "tabs"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new TabPanel<>(this, tabPanelElement);
    }

}
