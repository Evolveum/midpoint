/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.TabWithContainerWrapper;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.common.TabPanel;
import com.evolveum.midpoint.schrodinger.page.resource.EditResourceConfigurationPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by matus on 3/28/2018.
 */
public class ResourceConfigurationTab extends TabWithContainerWrapper<EditResourceConfigurationPage> {
    public ResourceConfigurationTab(EditResourceConfigurationPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public PrismForm<ResourceConfigurationTab> form() {
        SelenideElement element = getParentElement().$(Schrodinger.byElementAttributeValue("div", "class", "tab-content"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new PrismForm<>(this, element);
    }
}
