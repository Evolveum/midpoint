/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger.labs;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.page.configuration.ImportObjectPage;
import com.evolveum.midpoint.schrodinger.page.resource.ListResourcesPage;
import com.evolveum.midpoint.schrodinger.page.resource.ResourceWizardPage;
import com.evolveum.midpoint.schrodinger.page.resource.ViewResourcePage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static com.codeborne.selenide.Selenide.$;


/**
 * Created by honchar
 * covers LAB 3-1: Viewing Resources
 */
public class ImportResourceTest extends AbstractSchrodingerTest {

    private static final File CSV_RESOURCE = new File("./src/test/resources/labs/resources/localhost-csvfile-1-document-access.xml");
    public static final String RESOURCE_NAME = "CSV-1 (Document Access)";
    private static final String UNIQUE_ATTRIBUTE_NAME = "login";
    private static final String PASSWORD_ATTRIBUTE_NAME = "password";
    private static final String PASSWORD_ATTRIBUTE_RESOURCE_KEY = "User password attribute name";
    private static final String UNIQUE_ATTRIBUTE_RESOURCE_KEY = "Unique attribute name";
    private static final String RESOURCE_WIZARD_READONLY_LABEL = "Resource is in read-only mode";
    private static final String ACCOUNT_OBJECT_CLASS_LINK = "AccountObjectClass (Default Account)";

    private static final List<String> RESOURCE_ATTRIBUTES = Arrays.asList("login", "lname", "groups", "enumber", "phone", "dep", "fname", "dis");

    @Test(groups={"lab_3_1"})
    public void test001ImportCsvResource() {
        ImportObjectPage importPage = basicPage.importObject();
        //import resource
        Assert.assertTrue(
                importPage
                        .getObjectsFromFile()
                        .chooseFile(CSV_RESOURCE)
                        .checkOverwriteExistingObject()
                        .clickImportFileButton()
                        .feedback()
                        .isSuccess()
        );

        String basePathTofile = "/src/test/resources/labs/resources/csv-1.csv";
        String pathToProject = System.getProperty("user.dir");
        changeResourceAttribute("CSV-1 (Document Access)", "File path", pathToProject + basePathTofile);

        ListResourcesPage listResourcesPage = basicPage.listResources();

        //test connection
        Assert.assertTrue(listResourcesPage
                .testConnectionClick(RESOURCE_NAME)
                .feedback()
                .isSuccess());
    }

    @Test(groups={"lab_3_1"}, dependsOnMethods = {"test001ImportCsvResource"}, priority = 1)
    public void test002ViewResourceDetailsPage(){

        //click Edit configuration on the resource edit page
        navigateToViewResourcePage()
                .clickEditResourceConfiguration();

        SelenideElement uniqueAttributeField = $(Schrodinger.byDataResourceKey(UNIQUE_ATTRIBUTE_RESOURCE_KEY))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        // Unique attribute name should be login
        Assert.assertTrue(uniqueAttributeField
                .$(By.tagName("input"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S)
                .getValue()
                .equals(UNIQUE_ATTRIBUTE_NAME));

        SelenideElement passwordAttributeField = $(Schrodinger.byDataResourceKey(PASSWORD_ATTRIBUTE_RESOURCE_KEY))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        // Password attribute name should be password
        Assert.assertTrue(passwordAttributeField
                .$(By.tagName("input"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S)
                .getValue()
                .equals(PASSWORD_ATTRIBUTE_NAME));

    }

    @Test(groups={"lab_3_1"}, dependsOnMethods = {"test001ImportCsvResource"}, priority = 2)
    public void test003showUsingWizard(){
        ResourceWizardPage resourceWizard = navigateToViewResourcePage()
                .clickShowUsingWizard();

        //wizard should appear
        Assert.assertTrue($(By.className("wizard"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S)
                .exists());

        Assert.assertTrue($(Schrodinger.byDataId("readOnlyNote"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S)
                .exists());

        //Configuration tab
        resourceWizard.clickOnWizardTab("Configuration");
        Assert.assertTrue($(Schrodinger.byDataId("configuration"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S)
                .exists());

        //Schema tab
        resourceWizard.clickOnWizardTab("Schema");
        Assert.assertTrue($(Schrodinger.byElementValue("a", "Schema"))
                .shouldBe(Condition.visible)
                .exists());
        $(By.linkText(ACCOUNT_OBJECT_CLASS_LINK))
                .shouldBe(Condition.visible)
                .click();
        //Attributes table visibility check
        Assert.assertTrue($(Schrodinger.byDataId("attributeTable"))
                .shouldBe(Condition.visible)
                .exists());

        //check resource attributes are present
        RESOURCE_ATTRIBUTES.forEach(attr ->
                Assert.assertTrue($(Schrodinger.byElementValue("div", attr))
                        .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                        .exists()));

    }


    private ViewResourcePage navigateToViewResourcePage(){
        ListResourcesPage resourcesList = basicPage.listResources();

        return resourcesList
                .table()
                .clickByName(RESOURCE_NAME);
    }
}

