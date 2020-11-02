/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.configuration;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.TabPanel;
import com.evolveum.midpoint.schrodinger.component.configuration.*;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class InternalsConfigurationPage extends BasicPage {

    public ClockTab clockTab() {
        SelenideElement element = findTabPanel().clickTab("PageInternals.tab.clock");
        return new ClockTab(this, element);
    }

    public DebugUtilTab debugUtilTab() {
        SelenideElement element = findTabPanel().clickTab("PageInternals.tab.debugUtil");
        return new DebugUtilTab(this, element);
    }

    public InternalConfigurationTab internalConfigurationTab() {
        SelenideElement element = findTabPanel().clickTab("PageInternals.tab.internalConfig");
        return new InternalConfigurationTab(this, element);
    }

    public TracesTab tracesTab() {
        SelenideElement element = findTabPanel().clickTab("PageInternals.tab.traces");
        return new TracesTab(this, element);
    }

    public CountersTab countersTab() {
        SelenideElement element = findTabPanel().clickTab("PageInternals.tab.counters");
        return new CountersTab(this, element);
    }

    protected TabPanel findTabPanel() {
        SelenideElement tabPanelElement = $(Schrodinger.byDataId("div", "tabPanel"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new TabPanel<>(this, tabPanelElement);
    }
}
