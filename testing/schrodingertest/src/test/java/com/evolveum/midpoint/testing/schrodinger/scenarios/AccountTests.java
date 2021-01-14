/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.evolveum.midpoint.schrodinger.page.configuration.ImportObjectPage;
import com.evolveum.midpoint.schrodinger.page.resource.ListResourcesPage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import java.io.File;
import java.io.IOException;

/**
 * Created by matus on 3/22/2018.
 */
public class AccountTests extends AbstractSchrodingerTest {

    private static File csvTargetFile;

    private static final File CSV_RESOURCE_MEDIUM = new File("./src/test/resources/csv/resource-csv-username.xml");

    protected static final File CSV_INITIAL_SOURCE_FILE = new File("./src/test/resources/csv/midpoint-username.csv");

    protected static final String IMPORT_CSV_RESOURCE_DEPENDENCY= "importCsvResource";
    protected static final String CREATE_MP_USER_DEPENDENCY= "createMidpointUser";
    protected static final String CHANGE_RESOURCE_FILE_PATH_DEPENDENCY= "changeResourceFilePath";
    protected static final String ADD_ACCOUNT_DEPENDENCY= "addAccount";
    protected static final String DISABLE_ACCOUNT_DEPENDENCY= "disableAccount";
    protected static final String ENABLE_ACCOUNT_DEPENDENCY= "enableAccount";
    protected static final String MODIFY_ACCOUNT_PASSWORD_DEPENDENCY= "modifyAccountPassword";
    protected static final String TEST_GROUP_BEFORE_USER_DELETION = "beforeDelete";

    protected static final String CSV_RESOURCE_NAME= "Test CSV: username";

    protected static final String CSV_RESOURCE_ATTR_FILE_PATH= "File path";

    protected static final String TEST_USER_MIKE_NAME= "michelangelo";
    protected static final String TEST_USER_MIKE_LAST_NAME_OLD= "di Lodovico Buonarroti Simoni";
    protected static final String TEST_USER_MIKE_LAST_NAME_NEW= "di Lodovico Buonarroti Simoni Il Divino";

    private static final String DIRECTORY_CURRENT_TEST = "accountTests";
    private static final String FILE_RESOUCE_NAME = "midpoint-accounttests.csv";


    @Test(priority = 1, groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void createMidpointUser() throws IOException {

        initTestDirectory(DIRECTORY_CURRENT_TEST);

        csvTargetFile = new File(testTargetDir, FILE_RESOUCE_NAME);
        FileUtils.copyFile(CSV_INITIAL_SOURCE_FILE, csvTargetFile);

        UserPage user = basicPage.newUser();

        user.selectTabBasic()
                    .form()
                        .addAttributeValue("name", TEST_USER_MIKE_NAME)
                        .addAttributeValue(UserType.F_GIVEN_NAME, "Michelangelo")
                        .addAttributeValue(UserType.F_FAMILY_NAME, "di Lodovico Buonarroti Simoni")
                        .addProtectedAttributeValue("value","5ecr3tPassword")
                        .and()
                    .and()
                .checkKeepDisplayingResults()
                    .clickSave()
                    .feedback()
                    .assertSuccess();
    }

    @Test(priority = 2, groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void importCsvResource(){
        ImportObjectPage importPage = basicPage.importObject();

        importPage
                .getObjectsFromFile()
                .chooseFile(CSV_RESOURCE_MEDIUM)
                .checkOverwriteExistingObject()
                .clickImportFileButton()
                    .feedback()
                    .assertSuccess();
    }


    @Test (priority = 3, dependsOnMethods = {IMPORT_CSV_RESOURCE_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void changeResourceFilePath(){
        ListResourcesPage listResourcesPage = basicPage.listResources();

        Assert.assertTrue(listResourcesPage
                .table()
                .clickByName(CSV_RESOURCE_NAME)
                    .clickEditResourceConfiguration()
                        .form()
                        .changeAttributeValue(CSV_RESOURCE_ATTR_FILE_PATH, ScenariosCommons.CSV_SOURCE_OLDVALUE, csvTargetFile.getAbsolutePath())
                        .changeAttributeValue(CSV_RESOURCE_ATTR_UNIQUE,"","username")
                    .and()
                .and()
                .clickSaveAndTestConnection()
                .isTestSuccess()
        );
        refreshResourceSchema(CSV_RESOURCE_NAME);
    }

    @Test(priority = 4, dependsOnMethods = {CREATE_MP_USER_DEPENDENCY,CHANGE_RESOURCE_FILE_PATH_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void addAccount() {
        ListUsersPage users = basicPage.listUsers();
        users
                .table()
                    .search()
                    .byName()
                    .inputValue(TEST_USER_MIKE_NAME)
                    .updateSearch()
                .and()
                .clickByName(TEST_USER_MIKE_NAME)
                    .selectTabProjections()
                    .clickAddProjection()
                            .table()
                            .selectCheckboxByName(CSV_RESOURCE_NAME)
                        .and()
                        .clickAdd()
                    .and()
                    .checkKeepDisplayingResults()
                        .clickSave()
                        .feedback()
                        .assertSuccess();
    }

    @Test (priority = 5, dependsOnMethods = {ADD_ACCOUNT_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void modifyAccountAttribute(){
        ListUsersPage users = basicPage.listUsers();
                users
                    .table()
                        .search()
                        .byName()
                        .inputValue(TEST_USER_MIKE_NAME)
                        .updateSearch()
                    .and()
                    .clickByName(TEST_USER_MIKE_NAME)
                        .selectTabProjections()
                            .table()
                            .clickByName(TEST_USER_MIKE_NAME)
                                .changeAttributeValue("lastname",TEST_USER_MIKE_LAST_NAME_OLD,TEST_USER_MIKE_LAST_NAME_NEW)
                            .and()
                        .and()
                    .and()
                    .checkKeepDisplayingResults()
                        .clickSave()
                        .feedback()
            ;
    }

    @Test (priority = 6, dependsOnMethods = {ADD_ACCOUNT_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void modifyAccountPassword(){
        ListUsersPage users = basicPage.listUsers();
            users
                .table()
                    .search()
                    .byName()
                    .inputValue(TEST_USER_MIKE_NAME)
                    .updateSearch()
                .and()
                .clickByName(TEST_USER_MIKE_NAME)
                    .selectTabProjections()
                        .table()
                        .clickByName(TEST_USER_MIKE_NAME)
                            .showEmptyAttributes("Password")
                            .addProtectedAttributeValue("value","5ecr3t")
                        .and()
                    .and()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                    .feedback()
                    .isSuccess();
    }

    @Test (priority = 7, dependsOnMethods = {ADD_ACCOUNT_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void disableAccount(){
        ListUsersPage users = basicPage.listUsers();
            users
                .table()
                    .search()
                    .byName()
                    .inputValue(TEST_USER_MIKE_NAME)
                    .updateSearch()
                .and()
                .clickByName(TEST_USER_MIKE_NAME)
                    .selectTabProjections()
                        .table()
                        .clickByName(TEST_USER_MIKE_NAME)
                            .selectOption("administrativeStatus","Disabled")
                        .and()
                    .and()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                    .feedback()
                    .isSuccess();
    }

    @Test (priority = 8, dependsOnMethods = {ADD_ACCOUNT_DEPENDENCY, DISABLE_ACCOUNT_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void enableAccount(){
        ListUsersPage users = basicPage.listUsers();
            users
                .table()
                    .search()
                    .byName()
                    .inputValue(TEST_USER_MIKE_NAME)
                    .updateSearch()
                .and()
                .clickByName(TEST_USER_MIKE_NAME)
                    .selectTabProjections()
                        .table()
                        .clickByName(TEST_USER_MIKE_NAME)
                            .selectOption("administrativeStatus","Enabled")
                        .and()
                    .and()
                .and()
                .checkKeepDisplayingResults()
                .clickSave()
                    .feedback()
                    .isSuccess();
    }

    @Test(priority = 9, dependsOnMethods = {ENABLE_ACCOUNT_DEPENDENCY},groups = TEST_GROUP_BEFORE_USER_DELETION)
    public void deleteAccount(){
        ListUsersPage users = basicPage.listUsers();
                users
                    .table()
                        .search()
                        .byName()
                        .inputValue(TEST_USER_MIKE_NAME)
                        .updateSearch()
                    .and()
                    .clickByName(TEST_USER_MIKE_NAME)
                        .selectTabProjections()
                            .table()
                            .selectCheckboxByName(TEST_USER_MIKE_NAME)
                        .and()
                            .clickHeaderActionDropDown()
                            .delete()
                                .clickYes()
                        .and()
                    .and()
                    .checkKeepDisplayingResults()
                    .clickSave()
                        .feedback()
                        .isSuccess();
    }
}
