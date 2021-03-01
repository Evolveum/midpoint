/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.configuration;

import com.codeborne.selenide.Condition;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;
import org.testng.Assert;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class BulkActionsPage extends BasicPage {

    public BulkActionsPage insertOneLineTextIntoEditor(String text){
        $(Schrodinger.byElementAttributeValue("textarea", "class", "ace_text-input"))
                .waitUntil(Condition.exist, MidPoint.TIMEOUT_DEFAULT_2_S)

//        $(Schrodinger.byElementAttributeValue("textarea", "class", "ace_text-input"))
                .sendKeys(text);
        return this;
    }

    public BulkActionsPage startButtonClick(){
        $(Schrodinger.byDataId("a", "start"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .click();
        return this;
    }

    public boolean isAceEditorVisible(){
        return $(By.className("aceEditor")).exists();
    }

    public BulkActionsPage assertAceEditorVisible() {
        assertion.assertTrue(isAceEditorVisible(), "Ace editor should be visible.");
        return this;
    }
}
