/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component.configuration;

import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.TabWithTableAndPrismView;
import com.evolveum.midpoint.schrodinger.component.common.PrismFormWithActionButtons;
import com.evolveum.midpoint.schrodinger.page.configuration.SystemPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * @author skublik
 */

public class ObjectPolicyTab extends TabWithTableAndPrismView<SystemPage> {

    public ObjectPolicyTab(SystemPage parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public PrismFormWithActionButtons<ObjectPolicyTab> clickAddObjectPolicy() {
        SelenideElement plusButton = $(Schrodinger.byElementAttributeValue("i", "class", "fa fa-plus "));
        plusButton.waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        plusButton.waitWhile(Condition.visible, MidPoint.TIMEOUT_LONG_1_M);
        SelenideElement prismElement = $(Schrodinger.byDataId("div", "itemDetails"))
                .waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new PrismFormWithActionButtons(this, prismElement);
    }

    @Override
    protected String getPrismViewPanelId() {
        return "itemDetails";
    }
}
