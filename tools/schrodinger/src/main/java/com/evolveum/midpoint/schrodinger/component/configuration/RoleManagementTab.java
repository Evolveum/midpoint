/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.TabWithContainerWrapper;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.page.configuration.SystemPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by honchar
 */
public class RoleManagementTab extends TabWithContainerWrapper<SystemPage> {

    public RoleManagementTab(SystemPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
