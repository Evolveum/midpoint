/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs.advanced;

import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.util.Utils;

import com.evolveum.midpoint.testing.schrodinger.scenarios.ScenariosCommons;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
/**
 * @author honchar
 */
public class M1ArchetypeAndObjectCollections extends AbstractAdvancedLabTest {
    protected static final String LAB_OBJECTS_DIRECTORY = LAB_ADVANCED_DIRECTORY + "M1/";
    private static final File OBJECT_COLLECTION_ACTIVE_EMP_FILE = new File(LAB_OBJECTS_DIRECTORY + "objectcollections/objectCollection-active-employees.xml");
    private static final File OBJECT_COLLECTION_INACTIVE_EMP_FILE = new File(LAB_OBJECTS_DIRECTORY + "objectcollections/objectCollection-inactive-employees.xml");
    private static final File OBJECT_COLLECTION_FORMER_EMP_FILE = new File(LAB_OBJECTS_DIRECTORY + "objectcollections/objectCollection-former-employees.xml");
    private static final File ARCHETYPE_EMPLOYEE_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-employee.xml");
    private static final File ARCHETYPE_EXTERNAL_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-external.xml");
    private static final File KIRK_USER_FILE = new File(LAB_OBJECTS_DIRECTORY + "users/kirk-user.xml");
    private static final File SECRET_I_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-secret-i.xml");
    private static final File SECRET_II_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-secret-ii.xml");
    private static final File INTERNAL_EMPLOYEE_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-internal-employee.xml");
    private static final File CSV_1_SIMPLE_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-1-document-access-simple.xml");
    private static final File CSV_2_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-2-canteen.xml");
    private static final File CSV_3_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-3-ldap.xml");
    private static final File SYSTEM_CONFIGURATION_FILE_INIT = new File(LAB_OBJECTS_DIRECTORY + "systemconfiguration/system-configuration-advanced-labs-init.xml");
    private static final File HR_NO_EXTENSION_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-hr-noextension.xml");
    private static final File ORG_EXAMPLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "org/org-example.xml");

    @BeforeClass(alwaysRun = true, dependsOnMethods = { "springTestContextPrepareTestInstance" })
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
        csv1TargetFile = new File(getTestTargetDir(), CSV_1_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_1_SOURCE_FILE, csv1TargetFile);
        csv2TargetFile = new File(getTestTargetDir(), CSV_2_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_2_SOURCE_FILE, csv2TargetFile);
        csv3TargetFile = new File(getTestTargetDir(), CSV_3_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_3_SOURCE_FILE, csv3TargetFile);

        hrTargetFile = new File(getTestTargetDir(), HR_FILE_SOURCE_NAME);
        FileUtils.copyFile(HR_SOURCE_FILE, hrTargetFile);
    }

//    @Override
//    protected List<File> getObjectListToImport(){
//        return Arrays.asList(OBJECT_COLLECTION_ACTIVE_EMP_FILE, OBJECT_COLLECTION_INACTIVE_EMP_FILE, OBJECT_COLLECTION_FORMER_EMP_FILE,
//                KIRK_USER_FILE, SECRET_I_ROLE_FILE, SECRET_II_ROLE_FILE, INTERNAL_EMPLOYEE_ROLE_FILE,
//                CSV_1_SIMPLE_RESOURCE_FILE, CSV_2_RESOURCE_FILE, CSV_3_RESOURCE_FILE, SYSTEM_CONFIGURATION_FILE_INIT);
//    }

    @Test(groups={"advancedM1"})
    public void mod01test01environmentInitialization() {
        changeResourceAttribute(CSV_1_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv1TargetFile.getAbsolutePath(), true);
        changeResourceAttribute(CSV_2_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv2TargetFile.getAbsolutePath(), true);
        changeResourceAttribute(CSV_3_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv3TargetFile.getAbsolutePath(), true);

        Utils.addAsignments(showUser("kirk").selectTabAssignments(), "Internal Employee");
        showUser("kirk")
                        .selectTabProjections()
                        .assertProjectionExist("jkirk")
                        .assertProjectionExist("cn=Jim Kirk,ou=ExAmPLE,dc=example,dc=com")
                        .assertProjectionExist("kirk");

        Utils.addAsignments(showUser("kirk").selectTabAssignments(), "Secret Projects I", "Secret Projects II");
        //TODO check CSV-1 groups
        showUser("kirk")
                .selectTabProjections();
        Utils.removeAssignments(showUser("kirk").selectTabAssignments(), "Secret Projects I", "Secret Projects II");
        //TODO check CSV-1 groups
        Utils.removeAllAssignments(showUser("kirk").selectTabAssignments());
        showUser("kirk")
                        .selectTabProjections()
                            .assertProjectionDisabled("jkirk")
                            .assertProjectionDisabled("cn=Jim Kirk,ou=ExAmPLE,dc=example,dc=com")
                            .assertProjectionDisabled("kirk");
        Utils.addAsignments(showUser("kirk").selectTabAssignments(), "Internal Employee");
        showUser("kirk")
                        .selectTabProjections()
                            .assertProjectionEnabled("jkirk")
                            .assertProjectionEnabled("cn=Jim Kirk,ou=ExAmPLE,dc=example,dc=com")
                            .assertProjectionEnabled("kirk");
    }

    @Test(groups={"advancedM1"})
    public void mod01test02ArchetypeAndObjectCollection() {
        basicPage.loggedUser().logout();
        FormLoginPage loginPage = midPoint.formLogin();
        loginPage.loginWithReloadLoginPage(getUsername(), getPassword());

        basicPage.listUsers()
                .newObjectCollection("New employee")
                    .selectTabBasic()
                        .form()
                            .addAttributeValue("Name", "janeway")
                            .addAttributeValue("Given name", "Kathryn")
                            .addAttributeValue("Family name", "Janeway")
                            .selectOption("Administrative status", "Enabled")
                            .addAttributeValue("Password", "qwerty12345XXXX")
                            .and()
                        .and()
                    .clickSave()
                        .feedback()
                            .assertSuccess();

        basicPage
                .listUsers()
                    .table()
                        .assertIconColumnExistsByNameColumnValue("janeway", "fa fa-user", "darkgreen");

        showUser("janeway")
                .summary()
                    .assertSummaryTagWithTextExists("Employee");

        basicPage
                .listUsers("Employees")
                    .table()
                        .assertTableObjectsCountEquals(1)
                        .assertCurrentTableContains("janeway");
    }

    @Test(groups={"advancedM1"})
    public void mod01test03EnvironmentExamination() {
        changeResourceAttribute(HR_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, hrTargetFile.getAbsolutePath(), true);

        getShadowTable(HR_RESOURCE_NAME, "name", "001212")
                .selectCheckboxByName("001212")
                .clickImport();
        showUser("X001212")
            .assertGivenName("John")
            .assertFamilyName("Smith")
            .assertFullName("John Smith")
            .selectTabBasic()
                .form()
                    .assertPropertySelectValue("isManager", "True")
                    .assertPropertyInputValue("empStatus", "A")
                    .and()
                .and()
                .selectTabAssignments()
                    .assertAssignmentsWithRelationExist("Member", "Active Employees", "Inactive Employees",
                            "Former Employees", "Internal Employee")
                    .and()
                .selectTabProjections()
                    .assertProjectionExist("jsmith")
                    .assertProjectionExist("cn=John Smith,ou=0300,ou=ExAmPLE,dc=example,dc=com")
                    .assertProjectionExist("smith");


    }
}
