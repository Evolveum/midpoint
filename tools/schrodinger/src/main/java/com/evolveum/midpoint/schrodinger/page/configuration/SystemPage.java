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
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SystemPage extends AssignmentHolderDetailsPage {

    public SystemTab systemTab() {
        return new SystemTab(this, getTabSelenideElement("pageSystemConfiguration.system.title"));
    }

    public ObjectPolicyTab objectPolicyTab() {
        return new ObjectPolicyTab(this, getTabSelenideElement("pageSystemConfiguration.objectPolicy.title"));
    }

    public NotificationsTab notificationsTab() {
        return new NotificationsTab(this, getTabSelenideElement("pageSystemConfiguration.notifications.title"));
    }

    public LoggingTab loggingTab() {
        return new LoggingTab(this, getTabSelenideElement("pageSystemConfiguration.logging.title"));
    }

    public ProfilingTab profilingTab() {
        return new ProfilingTab(this, getTabSelenideElement("pageSystemConfiguration.profiling.title"));
    }

    public AdminGuiTab adminGuiTab() {
        return new AdminGuiTab(this, getTabSelenideElement("pageSystemConfiguration.adminGui.title"));
    }

    public DeploymentInformationTab deploymentInformationTab() {
        return new DeploymentInformationTab(this, getTabSelenideElement("pageSystemConfiguration.deploymentInformation.title"));
    }

    public InfrastructureTab infrastructureTab() {
        return new InfrastructureTab(this, getTabSelenideElement("pageSystemConfiguration.infrastructure.title"));
    }

    public RoleManagementTab roleManagementTab(){
        return new RoleManagementTab(this, getTabSelenideElement("pageSystemConfiguration.roleManagement.title"));
    }

}
