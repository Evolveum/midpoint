/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.self;

import com.codeborne.selenide.Condition;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.TabPanel;
import com.evolveum.midpoint.schrodinger.component.self.PasswordTab;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CredentialsPage extends BasicPage {

    public PasswordTab passwordTab() {
        TabPanel<CredentialsPage> tabPanel = new TabPanel<>(this,
                $(Schrodinger.byDataId("tabPanel")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
        return new PasswordTab(this,
                tabPanel.clickTab("PageSelfCredentials.tabs.password").waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }
}
