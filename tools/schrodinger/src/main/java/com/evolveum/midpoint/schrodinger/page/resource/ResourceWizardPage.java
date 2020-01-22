/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.resource;

import com.codeborne.selenide.Condition;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import org.openqa.selenium.By;

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
}
