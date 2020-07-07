/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component.common;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import com.evolveum.midpoint.schrodinger.component.Component;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TabPanel<T> extends Component<T> {

    public TabPanel(T parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public SelenideElement clickTab(String resourceKey) {
        SelenideElement link = getParentElement().$(Schrodinger.bySchrodingerDataResourceKey(resourceKey)).parent();

        return verifyAndFetchActiveTab(link);
    }

    public SelenideElement clickTabWithName(String tabName) {
        SelenideElement link = getParentElement().$(By.linkText(tabName));

        return verifyAndFetchActiveTab(link);
    }

    public String getTabBadgeText(String resourceKey) {
        SelenideElement element = getParentElement().$(Schrodinger.bySchrodingerDataResourceKey(resourceKey));
        element.shouldBe(Condition.visible);

        SelenideElement badge = element.$(Schrodinger.byDataId("small", "count"));
        badge.shouldBe(Condition.visible);

        return badge.getValue();
    }

    private SelenideElement verifyAndFetchActiveTab(SelenideElement link) {
        link.shouldBe(Condition.visible);

        link.click();

        SelenideElement li = link.parent();
        li.shouldHave(Condition.cssClass("active"));

        return li.parent().parent().$(By.cssSelector(".tab-pane.active"));
    }

    public SelenideElement getActiveTab() {
        return getParentElement().$(By.cssSelector(".tab-pane.active"));
    }
}
