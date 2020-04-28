/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceAccountsTab;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceShadowTable;
import com.evolveum.midpoint.schrodinger.page.resource.AccountPage;
import com.evolveum.midpoint.schrodinger.page.resource.ViewResourcePage;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author skublik
 */

public class AbstractLabTest extends AbstractSchrodingerTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractLabTest.class);

    protected static final String LAB_DIRECTORY = "./src/test/resources/labs/";
    protected static final String LAB_OBJECTS_DIRECTORY = LAB_DIRECTORY + "objects/";
    protected static final String LAB_SOURCES_DIRECTORY = LAB_DIRECTORY + "sources/";

    protected static final File EXTENSION_SCHEMA_FILE = new File(LAB_DIRECTORY +"schema/extension-example.xsd");
    protected static final File CSV_1_SIMPLE_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-1-document-access-simple.xml");
    protected static final File CSV_1_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-1-document-access.xml");
    protected static final File CSV_1_SOURCE_FILE = new File(LAB_SOURCES_DIRECTORY + "csv-1.csv");
    protected static final File CSV_1_SOURCE_FILE_7_3 = new File(LAB_SOURCES_DIRECTORY + "csv-1-7-3.csv");
    protected static final File NUMERIC_PIN_FIRST_NONZERO_POLICY_FILE = new File(LAB_OBJECTS_DIRECTORY + "valuePolicies/numeric-pin-first-nonzero-policy.xml");
    protected static final File CSV_2_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-2-canteen.xml");
    protected static final File CSV_2_SOURCE_FILE = new File(LAB_SOURCES_DIRECTORY + "csv-2.csv");
    protected static final File CSV_3_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-3-ldap.xml");
    protected static final File CSV_3_SOURCE_FILE = new File(LAB_SOURCES_DIRECTORY + "csv-3.csv");
    protected static final File HR_NO_EXTENSION_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-hr-noextension.xml");
    protected static final File HR_SOURCE_FILE = new File(LAB_SOURCES_DIRECTORY + "source.csv");
    protected static final File HR_SOURCE_FILE_7_4_PART_1 = new File(LAB_SOURCES_DIRECTORY + "source-7-4-part-1.csv");
    protected static final File HR_SOURCE_FILE_7_4_PART_2 = new File(LAB_SOURCES_DIRECTORY + "source-7-4-part-2.csv");
    protected static final File HR_SOURCE_FILE_7_4_PART_3 = new File(LAB_SOURCES_DIRECTORY + "source-7-4-part-3.csv");
    protected static final File HR_SOURCE_FILE_7_4_PART_4 = new File(LAB_SOURCES_DIRECTORY + "source-7-4-part-4.csv");
    protected static final File HR_SOURCE_FILE_10_1 = new File(LAB_SOURCES_DIRECTORY + "source-10-1.csv");
    protected static final File HR_SOURCE_FILE_10_2_PART1 = new File(LAB_SOURCES_DIRECTORY + "source-10-2-part1.csv");
    protected static final File HR_SOURCE_FILE_10_2_PART2 = new File(LAB_SOURCES_DIRECTORY + "source-10-2-part2.csv");
    protected static final File HR_SOURCE_FILE_10_2_PART3 = new File(LAB_SOURCES_DIRECTORY + "source-10-2-part3.csv");


    protected static final String DIRECTORY_CURRENT_TEST = "labTests";
    protected static final String EXTENSION_SCHEMA_NAME = "extension-example.xsd";
    protected static final String CSV_1_FILE_SOURCE_NAME = "csv-1.csv";
    protected static final String CSV_1_RESOURCE_NAME = "CSV-1 (Document Access)";
    protected static final String CSV_1_RESOURCE_OID = "10000000-9999-9999-0000-a000ff000002";
    protected static final String CSV_2_FILE_SOURCE_NAME = "csv-2.csv";
    protected static final String CSV_2_RESOURCE_NAME = "CSV-2 (Canteen Ordering System)";
    protected static final String CSV_2_RESOURCE_OID = "10000000-9999-9999-0000-a000ff000003";
    protected static final String CSV_3_FILE_SOURCE_NAME = "csv-3.csv";
    protected static final String CSV_3_RESOURCE_NAME = "CSV-3 (LDAP)";
    protected static final String CSV_3_RESOURCE_OID = "10000000-9999-9999-0000-a000ff000004";
    protected static final String HR_FILE_SOURCE_NAME = "source.csv";
    protected static final String HR_RESOURCE_NAME = "ExAmPLE, Inc. HR Source";

    protected static final String PASSWORD_ATTRIBUTE_RESOURCE_KEY = "User password attribute name";
    protected static final String UNIQUE_ATTRIBUTE_RESOURCE_KEY = "Unique attribute name";

    protected static final String CSV_1_UNIQUE_ATTRIBUTE_NAME = "login";
    protected static final String CSV_1_PASSWORD_ATTRIBUTE_NAME = "password";
    protected static final String CSV_1_ACCOUNT_OBJECT_CLASS_LINK = "AccountObjectClass (Default Account)";
    protected static final String ARCHETYPE_EMPLOYEE_PLURAL_LABEL = "Employees";

    protected static final List<String> CSV_1_RESOURCE_ATTRIBUTES = Arrays.asList("login", "lname", "groups", "enumber", "phone", "dep", "fname", "dis");

    protected static File csv1TargetFile;
    protected static File csv2TargetFile;
    protected static File csv3TargetFile;
    protected static File hrTargetFile;

    @AfterClass
    @Override
    public void afterClass() {
        LOG.info("Finished tests from class {}", getClass().getName());

        Selenide.clearBrowserCookies();
        Selenide.clearBrowserLocalStorage();
        Selenide.close();
    }

    public UserPage showUser(String userName){
        UserPage user = showUserInTable(userName).clickByName(userName);
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return user;
    }

    public AssignmentHolderObjectListTable<ListUsersPage, UserPage> showUserInTable(String userName) {
        return basicPage.listUsers()
                .table()
                    .search()
                        .byName()
                            .inputValue(userName)
                        .updateSearch()
                    .and();
    }

    public AccountPage showShadow(String resourceName, String searchedItem, String itemValue){
        return showShadow(resourceName, searchedItem, itemValue, null, false);
    }

    public AccountPage showShadow(String resourceName, String searchedItem, String itemValue, String intent, boolean useRepository){
        return getShadowTable(resourceName, searchedItem, itemValue, intent, useRepository)
                .clickByName(itemValue);
    }

    public boolean existShadow(String resourceName, String searchedItem, String itemValue){
        return existShadow(resourceName, searchedItem, itemValue, null, false);
    }

    public boolean existShadow(String resourceName, String searchedItem, String itemValue, String intent,  boolean useRepository){
        ResourceShadowTable table = getShadowTable(resourceName, searchedItem, itemValue, intent, useRepository);
        return table.containsText(itemValue);
    }
    public ResourceShadowTable getShadowTable(String resourceName, String searchedItem, String itemValue) {
        return getShadowTable(resourceName, searchedItem, itemValue, null, false);
    }

    public ResourceShadowTable getShadowTable(String resourceName, String searchedItem, String itemValue, String intent, boolean useRepository) {
        ResourceAccountsTab<ViewResourcePage> tab = basicPage.listResources()
                .table()
                    .search()
                        .byName()
                            .inputValue(resourceName)
                        .updateSearch()
                        .and()
                    .clickByName(resourceName)
                        .clickAccountsTab();
        if (useRepository) {
            tab.clickSearchInRepository();
        } else {
            tab.clickSearchInResource();
        }
        Selenide.sleep(1000);
        if (intent != null && !intent.isBlank()) {
            tab.setIntent(intent);
            Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        }
        return tab.table()
                .search()
                    .resetBasicSearch()
                    .byItem(searchedItem)
                        .inputValueWithEnter(itemValue)
                    .and()
                .and();
    }

    protected TaskPage showTask(String name) {
        return basicPage.listTasks()
                .table()
                    .search()
                        .byName()
                            .inputValue(name)
                            .updateSearch()
                    .and()
                    .clickByName(name);
    }
}
