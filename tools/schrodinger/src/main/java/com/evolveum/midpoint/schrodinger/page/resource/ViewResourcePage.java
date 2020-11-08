/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.resource;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceAccountsTab;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceConfigurationTab;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

public class ViewResourcePage extends BasicPage {

    public ResourceConfigurationTab clickEditResourceConfiguration() {

        $(Schrodinger.byDataResourceKey("a", "pageResource.button.configurationEdit")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

      SelenideElement element=  $(By.cssSelector(".tab0.active"))
              .waitUntil(Condition.visible, MidPoint.TIMEOUT_LONG_1_M);

        return new ResourceConfigurationTab(new EditResourceConfigurationPage(), element);
    }

    public ResourceWizardPage clickShowUsingWizard() {

        $(Schrodinger.byDataResourceKey("a", "pageResource.button.wizardShow")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();
        $(Schrodinger.byElementAttributeValue("form", "class", "form-horizontal"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new ResourceWizardPage();
    }

    public ResourceAccountsTab<ViewResourcePage> clickAccountsTab() {

        $(Schrodinger.byDataResourceKey("schrodinger", "PageResource.tab.content.account")).parent()
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_MEDIUM_6_S).click();
        $(By.className("resource-content-selection")).waitUntil(Condition.visible, MidPoint.TIMEOUT_MEDIUM_6_S);
        SelenideElement tabContent = $(By.cssSelector(".tab-pane.active"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);

        return new ResourceAccountsTab<>(this, tabContent);
    }

    public ViewResourcePage refreshSchema() {
        $(Schrodinger.byDataResourceKey("a", "pageResource.button.refreshSchema")).waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        return this;
    }

}
