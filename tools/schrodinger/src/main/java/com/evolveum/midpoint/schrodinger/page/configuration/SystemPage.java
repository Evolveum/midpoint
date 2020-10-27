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
public class SystemPage extends BasicPage {

    public SystemPage cancel() {
        //todo implement
        return this;
    }

    public SystemPage save() {
        $(Schrodinger.byDataId("save")).click();
        return this;
    }

    public SystemTab systemTab() {
        SelenideElement element = getTabPanel().clickTab("pageSystemConfiguration.system.title");
        return new SystemTab(this, element);
    }

    public ObjectPolicyTab objectPolicyTab() {
        SelenideElement element = getTabPanel().clickTab("pageSystemConfiguration.objectPolicy.title");
        return new ObjectPolicyTab(this, element);
    }

    public NotificationsTab notificationsTab() {
        SelenideElement element = getTabPanel().clickTab("pageSystemConfiguration.notifications.title");
        return new NotificationsTab(this, element);
    }

    public LoggingTab loggingTab() {
        SelenideElement element = getTabPanel().clickTab("pageSystemConfiguration.logging.title");
        return new LoggingTab(this, element);
    }

    public ProfilingTab profilingTab() {
        SelenideElement element = getTabPanel().clickTab("pageSystemConfiguration.profiling.title");
        return new ProfilingTab(this, element);
    }

    public AdminGuiTab adminGuiTab() {
        SelenideElement element = getTabPanel().clickTab("pageSystemConfiguration.adminGui.title")
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new AdminGuiTab(this, element);
    }

    public DeploymentInformationTab deploymentInformationTab() {
        SelenideElement element = getTabPanel().clickTab("pageSystemConfiguration.deploymentInformation.title");
        return new DeploymentInformationTab(this, element);
    }

    public InfrastructureTab infrastructureTab() {
        SelenideElement element = getTabPanel().clickTab("pageSystemConfiguration.infrastructure.title");
        return new InfrastructureTab(this, element);
    }

    public RoleManagementTab roleManagementTab(){
        SelenideElement element = getTabPanel().clickTab("pageSystemConfiguration.roleManagement.title");
        return new RoleManagementTab(this, element);
    }

    protected TabPanel getTabPanel() {
        SelenideElement tabPanelElement = $(Schrodinger.byDataId("div", "tabPanel"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new TabPanel<>(this, tabPanelElement);
    }
}
