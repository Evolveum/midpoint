/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schrodinger.component.ProjectionsTab;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.common.PrismFormWithActionButtons;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTableWithPrismView;
import com.evolveum.midpoint.schrodinger.page.configuration.AboutPage;
import com.evolveum.midpoint.schrodinger.page.resource.AccountPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;

import com.evolveum.midpoint.testing.schrodinger.scenarios.ScenariosCommons;


import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;

/**
 * @author skublik
 */

public class ProvisioningToResources extends AbstractLabTest {

    private static final Logger LOG = LoggerFactory.getLogger(ProvisioningToResources.class);

    private static final File CSV_1_RESOURCE_FILE_4_2 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-1-document-access-4-2.xml");
    private static final File CSV_3_RESOURCE_FILE_4_2 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-3-ldap-4-2.xml");
    private static final File CSV_1_RESOURCE_FILE_4_3 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-1-document-access-4-3.xml");
    private static final File CSV_3_RESOURCE_FILE_4_4 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-3-ldap-4-4.xml");


    @Test
    public void test0401BasicProvisioningToMultipleResources() {
        importObject(CSV_1_RESOURCE_FILE,true);

        changeResourceAttribute(CSV_1_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv1TargetFile.getAbsolutePath(), true);

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

        Assert.assertTrue(existShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk"));

        showUser("kirk")
                .selectTabBasic()
                    .form()
                        .addAttributeValue(UserType.F_GIVEN_NAME, "Jim Tiberius")
                    .and()
                .and()
                .clickSave()
                    .feedback()
                        .isSuccess();

        AccountPage shadow = showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk");
        Selenide.sleep(2000);
        PrismForm<AccountPage> accountForm = shadow.form();
        Selenide.sleep(1000);
        Assert.assertTrue(accountForm.compareInputAttributeValue("fname", "Jim Tiberius"));

        showUser("kirk")
            .selectTabBasic()
                .form()
                    .setDropDownAttributeValue(ActivationType.F_ADMINISTRATIVE_STATUS, "Disabled")
                .and()
            .and()
            .clickSave()
                .feedback()
                    .isSuccess();

        showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk");
        Assert.assertTrue(accountForm.compareSelectAttributeValue("administrativeStatus", "Disabled"));
        showUserInTable("kirk")
                .selectAll()
                .and()
                .table()
                    .clickActionDropDown()
                        .clickEnable()
                            .clickYes();

        showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk");
        Assert.assertTrue(accountForm.compareSelectAttributeValue("administrativeStatus", "Enabled"));

        changeAdministrativeStatusViaProjectionTab("kirk", "jkirk", "Disabled", CSV_1_RESOURCE_NAME);
        changeAdministrativeStatusViaProjectionTab("kirk", "jkirk", "Enabled", CSV_1_RESOURCE_NAME);

        showUser("kirk")
                .selectTabProjections()
                    .clickAddProjection()
                        .table()
                            .search()
                                .byName()
                                    .inputValue(CSV_2_RESOURCE_NAME)
                                .updateSearch()
                            .and()
                            .selectCheckboxByName(CSV_2_RESOURCE_NAME)
                        .and()
                    .clickAdd()
                    .clickAddProjection()
                        .table()
                            .search()
                                .byName()
                                    .inputValue(CSV_3_RESOURCE_NAME)
                                .updateSearch()
                            .and()
                            .selectCheckboxByName(CSV_3_RESOURCE_NAME)
                        .and()
                    .clickAdd()
                    .and()
                .clickSave()
                    .feedback()
                        .isSuccess();

        Assert.assertTrue(existShadow(CSV_2_RESOURCE_NAME, "Login", "kirk"));

        Assert.assertTrue(basicPage.listResources()
                .table()
                    .search()
                        .byName()
                            .inputValue(CSV_3_RESOURCE_NAME)
                        .updateSearch()
                    .and()
                    .clickByName(CSV_3_RESOURCE_NAME)
                        .clickAccountsTab()
                            .clickSearchInResource()
                                .table()
                                    .search()
                                        .byItem("Distinguished Name")
                                            .inputValue("cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com")
                                        .updateSearch()
                                        .and()
                                    .containsText("cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com"));

        showUser("kirk")
                .selectTabProjections()
                    .table()
                        .search()
                            .byItem("Resource")
                                .inputRefOid("10000000-9999-9999-0000-a000ff000003")
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

        Assert.assertFalse(existShadow(CSV_2_RESOURCE_NAME, "Login", "kirk"));
    }

    @Test(dependsOnMethods = {"test0401BasicProvisioningToMultipleResources"})
    public void test0402AddingMappings() {

        importObject(CSV_1_RESOURCE_FILE_4_2,true);

        changeResourceAttribute(CSV_1_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv1TargetFile.getAbsolutePath(), true);

        importObject(CSV_3_RESOURCE_FILE_4_2,true);

        changeResourceAttribute(CSV_3_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv3TargetFile.getAbsolutePath(), true);

        showUser("kirk")
                .selectTabBasic()
                    .form()
                        .showEmptyAttributes("Properties")
                        .addAttributeValue(UserType.F_DESCRIPTION, "This user is created by midPoint")
                        .addAttributeValue("telephoneNumber","123 / 555 - 1010")
                        .and()
                    .and()
                .clickSave()
                    .feedback()
                        .isSuccess();

        AccountPage shadow = showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk");
        Selenide.sleep(2000);
        PrismForm<AccountPage> accountForm = shadow.form();
        Selenide.sleep(1000);
        Assert.assertTrue(accountForm.compareInputAttributeValue("phone", "123555-1010"));

        showShadow(CSV_3_RESOURCE_NAME, "Distinguished Name", "cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com");
        Selenide.sleep(2000);

        Assert.assertTrue(accountForm.compareInputAttributeValue("telephoneNumber", "123 / 555 - 1010"));
        Assert.assertTrue(accountForm.compareInputAttributeValue("description", "This user is created by midPoint"));

    }

    @Test(dependsOnMethods = {"test0402AddingMappings"})
    public void test0403ModifyingExistingMappings() {
        importObject(CSV_1_RESOURCE_FILE_4_3,true);

        changeResourceAttribute(CSV_1_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv1TargetFile.getAbsolutePath(), true);

        UserPage user = basicPage.newUser();
        user.selectTabBasic()
                .form()
                    .addAttributeValue(UserType.F_NAME, "picard")
                    .addAttributeValue(UserType.F_GIVEN_NAME, "Jean-Luc")
                    .addAttributeValue(UserType.F_FAMILY_NAME, "Picard")
                    .setDropDownAttributeValue(ActivationType.F_ADMINISTRATIVE_STATUS, "Enabled")
                    .setPasswordFieldsValues(new QName(SchemaConstantsGenerated.NS_COMMON, "value"), "abc123")
                    .and()
                .and()
             .clickSave()
                .feedback()
                    .isSuccess();

        showUser("picard")
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

        AccountPage shadow = showShadow(CSV_1_RESOURCE_NAME, "Login", "jpicard");
        Selenide.sleep(2000);
        PrismForm<AccountPage> accountForm = shadow.form();
        Selenide.sleep(1000);
        Assert.assertTrue(accountForm.compareInputAttributeValue("lname", "PICARD"));

        showUser("kirk")
                .checkReconcile()
                .clickSave()
                    .feedback()
                        .isSuccess();

        showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk");
        Selenide.sleep(2000);
        Assert.assertTrue(accountForm.compareInputAttributeValue("lname", "KIRK"));

    }

    @Test(dependsOnMethods = {"test0403ModifyingExistingMappings"})
    public void test0404AddingANewAttribute() {
        ((PrismFormWithActionButtons<AbstractTableWithPrismView<ProjectionsTab<UserPage>>>)
                ((AbstractTableWithPrismView)showUser("kirk")
                        .selectTabProjections()
                            .table()
                                .search()
                                    .byItem("Name")
                                        .inputValue("jim tiberius kirk")
                                        .updateSearch()
                                    .and())
                                .clickByName("cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com")
                        .showEmptyAttributes("Attributes")
                        .addAttributeValue("manager", "xxx"))
                        .and()
                    .and()
                .and()
                .clickSave()
                    .feedback()
                        .isSuccess();
        AccountPage shadow = showShadow(CSV_3_RESOURCE_NAME, "Distinguished Name", "cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com");
        Assert.assertTrue(shadow.form().compareInputAttributeValue("manager", "xxx"));

        importObject(CSV_3_RESOURCE_FILE_4_4,true);

        changeResourceAttribute(CSV_3_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv3TargetFile.getAbsolutePath(), true);
    }

    private void changeAdministrativeStatusViaProjectionTab(String userName, String accountName, String status, String resourceName) {
        ((PrismFormWithActionButtons<AbstractTableWithPrismView<ProjectionsTab<UserPage>>>)
                ((AbstractTableWithPrismView)showUser(userName)
                .selectTabProjections()
                    .table()
                        .search()
                            .byItem("Name")
                                .inputValue(accountName)
                            .updateSearch()
                        .and())
                        .clickByName(accountName)
                            .setDropDownAttributeValue(ActivationType.F_ADMINISTRATIVE_STATUS, status))
                            .and()
                        .and()
                    .and()
                .clickSave()
                    .feedback()
                    .isSuccess();
        AccountPage shadow = showShadow(resourceName, "Login", accountName);
        Assert.assertTrue(shadow.form().compareSelectAttributeValue("administrativeStatus", status));
    }
}
