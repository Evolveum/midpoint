/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.TabWithContainerWrapper;
import com.evolveum.midpoint.schrodinger.page.configuration.SystemPage;

public class GlobalProjectionPolicyTab extends TabWithContainerWrapper<SystemPage> {
    public GlobalProjectionPolicyTab(SystemPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }
}
