/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.page;

import com.codeborne.selenide.Condition;
import com.evolveum.midpoint.schrodinger.page.configuration.BulkActionsPage;
import com.evolveum.midpoint.testing.schrodinger.TestBase;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Kate Honchar
 */
public class BulkActionsTest extends TestBase {

    private static final String PARSING_ERROR_MESSAGE = "Couldn't parse bulk action object";

    @Test
    public void wrongBulkActionXmlExecution(){
        BulkActionsPage bulkActionsPage = basicPage.bulkActions();
        bulkActionsPage
                .insertOneLineTextIntoEditor("<objects></objects>")
                .startButtonClick();

        $(By.linkText(PARSING_ERROR_MESSAGE))
                .shouldBe(Condition.visible);

        Assert.assertTrue(bulkActionsPage.isAceEditorVisible());
    }
}
