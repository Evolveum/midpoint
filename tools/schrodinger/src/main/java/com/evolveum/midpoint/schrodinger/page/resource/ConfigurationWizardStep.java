/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.common.TabPanel;

/**
 * Created by honchar.
 */
public class ConfigurationWizardStep extends Component<ResourceWizardPage> {
        public ConfigurationWizardStep(ResourceWizardPage parent, SelenideElement parentElement) {
            super(parent, parentElement);
        }

        public TabPanel<ConfigurationWizardStep> getTabPanel() {
            return new TabPanel<>(this, getParentElement());
        }

        public ConfigurationStepConfigurationTab selectConfigurationTab() {
            return new ConfigurationStepConfigurationTab(this, getTabPanel().clickTabWithName("Configuration"));
        }

        public ConfigurationStepConnectorPoolTab selectConnectorPoolTab() {
            return new ConfigurationStepConnectorPoolTab(this, getTabPanel().clickTabWithName("Connector pool"));
        }

        public ConfigurationStepResultsHandlersTab selectResultsHandlerTab() {
            return new ConfigurationStepResultsHandlersTab(this, getTabPanel().clickTabWithName("Results handlers"));
        }

        public ConfigurationStepTimeoutsTab selectTimeoutsTab() {
            return new ConfigurationStepTimeoutsTab(this, getTabPanel().clickTabWithName("Timeouts"));
        }
}
