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
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;
import org.testng.Assert;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar.
 */
public class ResourceWizardPage extends BasicPage {

    public void clickOnWizardTab(String tabName){
        $(By.linkText(tabName))
                .shouldBe(Condition.visible)
                .click();
    }

    public ConfigurationWizardStep selectConfigurationStep() {
        clickOnWizardTab("Configuration");
        SelenideElement tabElement = $(Schrodinger.byDataId("configuration")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new ConfigurationWizardStep(this, tabElement);
    }

    public SchemaWizardStep selectSchemaStep() {
        clickOnWizardTab("Schema");
        SelenideElement tabElement = $(Schrodinger.byDataId("tabPanel")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);
        return new SchemaWizardStep(this, tabElement);
    }

    public boolean isReadonlyMode() {
        return $(By.className("wizard"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S)
                .exists();
    }

    public ResourceWizardPage assertReadonlyMode() {
        assertion.assertTrue(isReadonlyMode());
        return this;
    }
}
