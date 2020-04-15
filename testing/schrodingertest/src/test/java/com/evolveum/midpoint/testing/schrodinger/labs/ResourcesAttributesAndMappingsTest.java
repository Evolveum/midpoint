/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import com.codeborne.selenide.Condition;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceShadowTable;
import com.evolveum.midpoint.schrodinger.page.resource.AccountPage;
import com.evolveum.midpoint.schrodinger.page.resource.ListResourcesPage;
import com.evolveum.midpoint.schrodinger.page.resource.ResourceWizardPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import com.evolveum.midpoint.testing.schrodinger.scenarios.ScenariosCommons;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;

import static com.codeborne.selenide.Selenide.$;

/**
 * @author skublik
 */

public class ResourcesAttributesAndMappingsTest extends AbstractLabTest {

//    @BeforeClass(alwaysRun = true, dependsOnMethods = { "springTestContextBeforeTestClass" })
//    @Override
//    protected void springTestContextPrepareTestInstance() throws Exception {
//
//        String home = System.getProperty("midpoint.home");
//        File schemaDir = new File(home, "schema");
//
//        if (!schemaDir.mkdir()) {
//            if (schemaDir.exists()) {
//                FileUtils.cleanDirectory(schemaDir);
//            } else {
//                throw new IOException("Creation of directory \"" + schemaDir.getAbsolutePath() + "\" unsuccessful");
//            }
//        }
//        File schemaFile = new File(schemaDir, EXTENSION_SCHEMA_NAME);
//        FileUtils.copyFile(EXTENSION_SCHEMA_FILE, schemaFile);
//
//        super.springTestContextPrepareTestInstance();
//    }

    @BeforeClass(alwaysRun = true, dependsOnMethods = { "springTestContextPrepareTestInstance" })
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
    }

    @Test
    public void test0301ViewingResources() throws Exception {
        initTestDirectory(DIRECTORY_CURRENT_TEST);

        csv1TargetFile = new File(csvTargetDir, CSV_1_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_1_SOURCE_FILE, csv1TargetFile);

        importObject(CSV_1_SIMPLE_RESOURCE_FILE,true);

        changeResourceAttribute(CSV_1_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv1TargetFile.getAbsolutePath(), true);

        ListResourcesPage resourcesList = basicPage.listResources();

        resourcesList
                .table()
                    .clickByName(CSV_1_RESOURCE_NAME)
                        .clickEditResourceConfiguration();

        SelenideElement uniqueAttributeField = $(Schrodinger.byDataResourceKey(UNIQUE_ATTRIBUTE_RESOURCE_KEY))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        // Unique attribute name should be login
        Assert.assertTrue(uniqueAttributeField
                .$(By.tagName("input"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S)
                .getValue()
                .equals(CSV_1_UNIQUE_ATTRIBUTE_NAME));

        SelenideElement passwordAttributeField = $(Schrodinger.byDataResourceKey(PASSWORD_ATTRIBUTE_RESOURCE_KEY))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        // Password attribute name should be password
        Assert.assertTrue(passwordAttributeField
                .$(By.tagName("input"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S)
                .getValue()
                .equals(CSV_1_PASSWORD_ATTRIBUTE_NAME));

        ResourceWizardPage resourceWizard = basicPage.listResources()
                .table()
                    .clickByName(CSV_1_RESOURCE_NAME)
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
        $(By.linkText(CSV_1_ACCOUNT_OBJECT_CLASS_LINK))
                .shouldBe(Condition.visible)
                .click();
        //Attributes table visibility check
        Assert.assertTrue($(Schrodinger.byDataId("attributeTable"))
                .shouldBe(Condition.visible)
                .exists());

        //check resource attributes are present
        CSV_1_RESOURCE_ATTRIBUTES.forEach(attr ->
                Assert.assertTrue($(Schrodinger.byElementValue("div", attr))
                        .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                        .exists()));

        importObject(NUMERIC_PIN_FIRST_NONZERO_POLICY_FILE,true);

        csv2TargetFile = new File(csvTargetDir, CSV_2_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_2_SOURCE_FILE, csv2TargetFile);

        importObject(CSV_2_RESOURCE_FILE,true);

        changeResourceAttribute(CSV_2_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv2TargetFile.getAbsolutePath(), true);

        csv3TargetFile = new File(csvTargetDir, CSV_3_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_3_SOURCE_FILE, csv3TargetFile);

        importObject(CSV_3_RESOURCE_FILE,true);

        changeResourceAttribute(CSV_3_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv3TargetFile.getAbsolutePath(), true);
    }

    @Test(dependsOnMethods = {"test0301ViewingResources"})
    public void test0302BasicProvisioning() {
        UserPage user = basicPage.newUser();
        user.selectTabBasic()
                .form()
                    .addAttributeValue(UserType.F_NAME, "kirk")
                    .addAttributeValue(UserType.F_GIVEN_NAME, "Jim")
                    .addAttributeValue(UserType.F_FAMILY_NAME, "Kirk")
                    .setDropDownAttributeValue(ActivationType.F_ADMINISTRATIVE_STATUS, "Enabled")
                    .setPasswordFieldsValues(new QName(SchemaConstantsGenerated.NS_COMMON, "value"), "abc123")
                    .and()
                .and()
                .clickSave()
                    .feedback()
                        .isSuccess();

        showUser("kirk")
                    .selectTabProjections()
                        .clickAddProjection()
                            .table()
                                .search()
                                    .byName()
                                        .inputValue(CSV_1_RESOURCE_NAME)
                                    .updateSearch()
                                .and()
                                .selectCheckboxByName(CSV_1_RESOURCE_NAME)
                                .and()
                                .clickAdd()
                            .and()
                        .clickSave()
                        .feedback()
                            .isSuccess();

        Assert.assertTrue(existShadow(CSV_1_RESOURCE_NAME, "Login", "kirk"));

        showUser("kirk")
                .selectTabBasic()
                            .form()
                                .addAttributeValue(UserType.F_GIVEN_NAME, "Jim T.")
                                .and()
                            .and()
                        .clickSave()
                        .feedback()
                            .isSuccess();

        PrismForm<AccountPage> accountForm = showShadow(CSV_1_RESOURCE_NAME, "Login", "kirk")
                .form();

        Selenide.sleep(2000);
        Assert.assertTrue(accountForm.compareInputAttributeValue("fname", "Jim T."));

        showUser("kirk")
                    .selectTabProjections()
                        .table()
                            .search()
                                .byItem("Name")
                                    .inputValue("kirk")
                                .updateSearch()
                            .and()
                            .selectAll()
                        .and()
                        .clickHeaderActionDropDown()
                            .delete()
                                .clickYes()
                            .and()
                        .and()
                        .clickSave()
                        .feedback()
                            .isSuccess();

        Assert.assertFalse(existShadow(CSV_1_RESOURCE_NAME, "Login", "kirk"));

    }
}
