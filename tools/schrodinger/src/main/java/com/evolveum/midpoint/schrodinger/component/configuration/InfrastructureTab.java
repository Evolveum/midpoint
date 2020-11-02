/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.configuration;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.component.TabWithContainerWrapper;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.page.configuration.SystemPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class InfrastructureTab extends TabWithContainerWrapper<SystemPage> {

    public InfrastructureTab(SystemPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public PrismForm<InfrastructureTab> form() {

        SelenideElement element = $(Schrodinger.byDataId("panel"));
        return new PrismForm<InfrastructureTab>(this, element);
    }

}
