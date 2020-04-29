/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.TabWithContainerWrapper;
import com.evolveum.midpoint.schrodinger.page.configuration.SystemPage;

/**
 * @author skublik
 */

public class DeploymentInformationTab extends TabWithContainerWrapper<DeploymentInformationTab, SystemPage> {
    public DeploymentInformationTab(SystemPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
