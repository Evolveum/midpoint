/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceConfigurationTab;
import com.evolveum.midpoint.schrodinger.page.resource.AccountPage;
import com.evolveum.midpoint.schrodinger.page.resource.ListResourcesPage;
import com.evolveum.midpoint.schrodinger.page.resource.SchemaStepSchemaTab;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;

import com.evolveum.midpoint.testing.schrodinger.scenarios.ScenariosCommons;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;

/**
 * @author skublik
 */

public class M3ResourcesAttributesAndMappingsTest extends AbstractLabTest {

    private static final String LAB_OBJECTS_DIRECTORY = LAB_DIRECTORY + "M3/";
    private static final File CSV_1_SIMPLE_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-1-document-access-simple.xml");
    private static final File NUMERIC_PIN_FIRST_NONZERO_POLICY_FILE = new File(LAB_OBJECTS_DIRECTORY + "valuepolicies/numeric-pin-first-nonzero-policy.xml");
    private static final File CSV_2_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-2-canteen.xml");
    private static final File CSV_3_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-3-ldap.xml");

    @BeforeClass(alwaysRun = true, dependsOnMethods = { "springTestContextPrepareTestInstance" })
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
    }

    @Test(groups={"M3"})
    public void mod03test01ViewingResources() throws Exception {
        csv1TargetFile = new File(getTestTargetDir(), CSV_1_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_1_SOURCE_FILE, csv1TargetFile);

        importObject(CSV_1_SIMPLE_RESOURCE_FILE, true);

        changeResourceAttribute(CSV_1_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv1TargetFile.getAbsolutePath(), true);

        ListResourcesPage resourcesList = basicPage.listResources();

        PrismForm<ResourceConfigurationTab> configTab = resourcesList
                .table()
                .clickByName(CSV_1_RESOURCE_NAME)
                    .clickEditResourceConfiguration()
                    .form();
        // Unique attribute name should be login
        configTab.assertPropertyInputValue(UNIQUE_ATTRIBUTE_NAME, CSV_1_UNIQUE_ATTRIBUTE_NAME);

        // Password attribute name should be password
        configTab.assertPropertyInputValue(PASSWORD_ATTRIBUTE_NAME, CSV_1_PASSWORD_ATTRIBUTE_NAME);

        SchemaStepSchemaTab schemaStepSchemaTab = basicPage.listResources()
                .table()
                    .clickByName(CSV_1_RESOURCE_NAME)
                        .clickShowUsingWizard()
                        .assertReadonlyMode()
                            .selectConfigurationStep()
                            .and()
                            .selectSchemaStep()
                                .selectSchemaTab()
                                .assertObjectClassPresent(CSV_1_ACCOUNT_OBJECT_CLASS_LINK)
                                .clickObjectClass(CSV_1_ACCOUNT_OBJECT_CLASS_LINK);
        //check resource attributes are present
        CSV_1_RESOURCE_ATTRIBUTES.forEach(attr ->
                schemaStepSchemaTab.getAttributesTable().assertTableContainsText(attr));

        importObject(NUMERIC_PIN_FIRST_NONZERO_POLICY_FILE, true);

        csv2TargetFile = new File(getTestTargetDir(), CSV_2_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_2_SOURCE_FILE, csv2TargetFile);

        importObject(CSV_2_RESOURCE_FILE, true);

        changeResourceAttribute(CSV_2_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv2TargetFile.getAbsolutePath(), true);

        csv3TargetFile = new File(getTestTargetDir(), CSV_3_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_3_SOURCE_FILE, csv3TargetFile);

        importObject(CSV_3_RESOURCE_FILE, true);

        changeResourceAttribute(CSV_3_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv3TargetFile.getAbsolutePath(), true);
    }

    @Test(dependsOnMethods = {"mod03test01ViewingResources"}, groups={"M3"})
    public void mod03test02BasicProvisioning() {
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

        assertShadowExists(CSV_1_RESOURCE_NAME, "Login", "kirk");

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

        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        accountForm.assertPropertyInputValue("fname", "Jim T.");

        showUser("kirk")
                    .selectTabProjections()
                        .table()
                            .search()
                                .textInputPanelByItemName("Name")
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

        assertShadowDoesntExist(CSV_1_RESOURCE_NAME, "Login", "kirk");

    }
}
