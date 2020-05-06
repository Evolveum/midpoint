/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import java.io.File;
import java.util.ArrayList;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.AssignmentHolderBasicTab;
import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;
import com.evolveum.midpoint.schrodinger.component.InducementsTab;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.table.DirectIndirectAssignmentTable;
import com.evolveum.midpoint.schrodinger.page.AbstractRolePage;
import com.evolveum.midpoint.schrodinger.page.archetype.ArchetypePage;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.page.resource.AccountPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Utils;

import com.evolveum.midpoint.testing.schrodinger.scenarios.ScenariosCommons;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;

/**
 * @author skublik
 */

public class M5AccountsAssignmentsAndRoles extends AbstractLabTest {

    private static final Logger LOG = LoggerFactory.getLogger(M5AccountsAssignmentsAndRoles.class);

    private static final File INCOGNITO_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-incognito.xml");
    private static final File INTERNAL_EMPLOYEE_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-internal-employee.xml");
    private static final File SECRET_I_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-secret-i.xml");
    private static final File SECRET_II_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-secret-ii.xml");
    private static final File TOP_SECRET_I_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-top-secret-i.xml");
    private static final File CSV_1_RESOURCE_FILE_5_5 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-1-document-access-5-5.xml");
    private static final File CSV_2_RESOURCE_FILE_5_5 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-2-canteen-5-5.xml");
    private static final File CSV_3_RESOURCE_FILE_5_5 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-3-ldap-5-5.xml");
    private static final File ARCHETYPE_EMPLOYEE_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-employee.xml");
    private static final String ARCHETYPE_EMPLOYEE_NAME = "Employee";
    private static final String ARCHETYPE_EMPLOYEE_LABEL = "Employee";
    private static final File ARCHETYPE_EXTERNAL_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-external.xml");
    private static final File SYSTEM_CONFIGURATION_FILE_5_7 = new File(LAB_OBJECTS_DIRECTORY + "systemConfiguration/system-configuration-5-7.xml");

    @Test(groups={"M5"}, dependsOnGroups={"M4"})
    public void mod05test01UsingRBAC() {
        importObject(INTERNAL_EMPLOYEE_ROLE_FILE,true);
        importObject(INCOGNITO_ROLE_FILE,true);
        importObject(SECRET_I_ROLE_FILE,true);
        importObject(SECRET_II_ROLE_FILE,true);
        importObject(TOP_SECRET_I_ROLE_FILE,true);

        Utils.addAsignments(showUser("kirk").selectTabAssignments(), "Secret Projects I", "Secret Projects II");
        Assert.assertTrue(
            showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk")
                .form()
                    .compareInputAttributeValues("groups", "Lucky Numbers",
                            "Teleportation", "Time Travel", "Presidential Candidates Motivation"));

        Utils.removeAssignments(showUser("kirk").selectTabAssignments(), "Secret Projects I");

        Assert.assertTrue(
                showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk")
                    .form()
                        .compareInputAttributeValues("groups", "Lucky Numbers",
                                "Presidential Candidates Motivation"));

        Utils.removeAssignments(showUser("kirk").selectTabAssignments(), "Secret Projects II");

        Assert.assertFalse(existShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk"));

        Utils.addAsignments(showUser("kirk").selectTabAssignments(), "Internal Employee");

        Assert.assertTrue(existShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk"));
        Assert.assertTrue(existShadow(CSV_2_RESOURCE_NAME, "Login", "jkirk"));
        Assert.assertTrue(existShadow(CSV_3_RESOURCE_NAME, "Distinguished Name",
                "cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com"));

    }

    @Test(dependsOnMethods = {"mod05test01UsingRBAC"}, groups={"M5"}, dependsOnGroups={"M4"})
    public void mod05test02SegregationOfDuties() {
        showUser("kirk").selectTabAssignments()
                .clickAddAssignemnt()
                    .table()
                        .search()
                            .byName()
                                .inputValue("Incognito")
                                .updateSearch()
                            .and()
                        .selectCheckboxByName("Incognito")
                    .and()
                .clickAdd()
                .and()
            .clickSave()
                .feedback()
                    .isError();
    }

    @Test(dependsOnMethods = {"mod05test02SegregationOfDuties"}, groups={"M5"}, dependsOnGroups={"M4"})
    public void mod05test04CreatingRoles() {
        InducementsTab<AbstractRolePage> tab = basicPage.newRole()
                .selectTabBasic()
                    .form()
                        .addAttributeValue(RoleType.F_NAME, "Too Many Secrets")
                        .addAttributeValue(RoleType.F_DISPLAY_NAME, "Too Many Secrets")
                    .and()
                .and()
                .selectTabInducements();
        Utils.addAsignments(tab, "Secret Projects I", "Secret Projects II", "Top Secret Projects I");

        Utils.addAsignments(showUser("kirk").selectTabAssignments(), "Too Many Secrets");
        Assert.assertTrue(existShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk"));

        DirectIndirectAssignmentTable<AssignmentsTab<UserPage>> table = showUser("kirk").selectTabAssignments()
                .selectTypeAllDirectIndirect();
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);
        Assert.assertTrue(table.containsIndirectAssignments("Secret Projects I",
                "Secret Projects II", "Top Secret Projects I", CSV_1_RESOURCE_NAME, CSV_2_RESOURCE_NAME,
                CSV_3_RESOURCE_NAME));

        Utils.removeAssignments(showUser("kirk").selectTabAssignments(), "Too Many Secrets");
    }

    @Test(dependsOnMethods = {"mod05test04CreatingRoles"}, groups={"M5"}, dependsOnGroups={"M4"})
    public void mod05test05DisableOnUnassign() {
        importObject(CSV_1_RESOURCE_FILE_5_5,true);
        changeResourceAttribute(CSV_1_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv1TargetFile.getAbsolutePath(), true);

        importObject(CSV_2_RESOURCE_FILE_5_5,true);
        changeResourceAttribute(CSV_2_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv2TargetFile.getAbsolutePath(), true);

        importObject(CSV_3_RESOURCE_FILE_5_5,true);
        changeResourceAttribute(CSV_3_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv3TargetFile.getAbsolutePath(), true);

        Utils.removeAssignments(showUser("kirk").selectTabAssignments(), "Internal Employee");

        AccountPage shadow = showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk");
        Selenide.sleep(2000);
        PrismForm<AccountPage> accountForm = shadow.form();
        Assert.assertTrue(accountForm.compareSelectAttributeValue("administrativeStatus", "Disabled"));
        Assert.assertTrue(accountForm.showEmptyAttributes("Attributes")
                .compareInputAttributeValues("groups", new ArrayList<String>()));

        showShadow(CSV_2_RESOURCE_NAME, "Login", "jkirk");
        Assert.assertTrue(accountForm.compareSelectAttributeValue("administrativeStatus", "Disabled"));

        showShadow(CSV_3_RESOURCE_NAME, "Distinguished Name", "cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com");
        Assert.assertTrue(accountForm.compareSelectAttributeValue("administrativeStatus", "Disabled"));

        Utils.addAsignments(showUser("kirk").selectTabAssignments(), "Internal Employee");
        showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk");
        Assert.assertTrue(accountForm.compareSelectAttributeValue("administrativeStatus", "Enabled"));
        Assert.assertTrue(
                accountForm.compareInputAttributeValues("groups", "Internal Employees",
                        "Essential Documents"));

        showShadow(CSV_2_RESOURCE_NAME, "Login", "jkirk");
        Assert.assertTrue(accountForm.compareSelectAttributeValue("administrativeStatus", "Enabled"));

        showShadow(CSV_3_RESOURCE_NAME, "Distinguished Name", "cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com");
        Assert.assertTrue(accountForm.compareSelectAttributeValue("administrativeStatus", "Enabled"));
    }

    @Test(dependsOnMethods = {"mod05test05DisableOnUnassign"}, groups={"M5"}, dependsOnGroups={"M4"})
    public void mod05test06InactiveAssignment() {
        Utils.addAsignments(showUser("kirk").selectTabAssignments(), "Too Many Secrets");
        AccountPage shadow = showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk");
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        PrismForm<AccountPage> accountForm = shadow.form();
        Assert.assertTrue(
                accountForm.compareInputAttributeValues("groups", "Internal Employees",
                        "Essential Documents", "Lucky Numbers", "Teleportation",
                        "Time Travel", "Presidential Candidates Motivation",
                        "Area 52 Managers", "Area 52 News Obfuscators", "Abduction Professional Services",
                        "Immortality Training", "Telekinesis In Practice", "IDDQD"));
        Assert.assertTrue(
                accountForm.compareInputAttributeValues("groups", "Lucky Numbers",
                        "Teleportation", "Time Travel", "Presidential Candidates Motivation"));

        Utils.setStatusForAssignment(showUser("kirk").selectTabAssignments(), "Too Many Secrets", "Disabled");

        showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk");
        Assert.assertTrue(accountForm.compareSelectAttributeValue("administrativeStatus", "Enabled"));
        Assert.assertTrue(
                accountForm.compareInputAttributeValues("groups", "Internal Employees", "Essential Documents"));

        showShadow(CSV_2_RESOURCE_NAME, "Login", "jkirk");
        Assert.assertTrue(accountForm.compareSelectAttributeValue("administrativeStatus", "Enabled"));

        showShadow(CSV_3_RESOURCE_NAME, "Distinguished Name", "cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com");
        Assert.assertTrue(accountForm.compareSelectAttributeValue("administrativeStatus", "Enabled"));

        Utils.setStatusForAssignment(showUser("kirk").selectTabAssignments(), "Internal Employee", "Disabled");

        showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk");
        Assert.assertTrue(accountForm.compareSelectAttributeValue("administrativeStatus", "Disabled"));
        Assert.assertTrue(accountForm.showEmptyAttributes("Attributes")
                .compareInputAttributeValues("groups", new ArrayList<String>()));

        showShadow(CSV_2_RESOURCE_NAME, "Login", "jkirk");
        Assert.assertTrue(accountForm.compareSelectAttributeValue("administrativeStatus", "Disabled"));

        showShadow(CSV_3_RESOURCE_NAME, "Distinguished Name", "cn=Jim Tiberius Kirk,ou=ExAmPLE,dc=example,dc=com");
        Assert.assertTrue(accountForm.compareSelectAttributeValue("administrativeStatus", "Disabled"));

        Utils.setStatusForAssignment(showUser("kirk").selectTabAssignments(), "Internal Employee", "Undefined");
        Utils.removeAssignments(showUser("kirk").selectTabAssignments(), "Too Many Secrets");
    }

    @Test(dependsOnMethods = {"mod05test06InactiveAssignment"}, groups={"M5"}, dependsOnGroups={"M4"})
    public void mod05test07ArchetypesIntroduction() {

        importObject(ARCHETYPE_EMPLOYEE_FILE, true);
        importObject(ARCHETYPE_EXTERNAL_FILE, true);

        PrismForm<AssignmentHolderBasicTab<ArchetypePage>> archetypePolicyForm = basicPage.listArchetypes()
                .table()
                    .clickByName(ARCHETYPE_EMPLOYEE_NAME)
                        .selectTabArchetypePolicy()
                            .form();

        Assert.assertTrue(archetypePolicyForm.compareInputAttributeValue("label", ARCHETYPE_EMPLOYEE_LABEL));
        Assert.assertTrue(archetypePolicyForm.compareInputAttributeValue("pluralLabel", ARCHETYPE_EMPLOYEE_PLURAL_LABEL));
        Assert.assertTrue(archetypePolicyForm.compareInputAttributeValue("cssClass", "fa fa-user"));
        Assert.assertTrue(archetypePolicyForm.compareInputAttributeValue("color", "darkgreen"));

        importObject(SYSTEM_CONFIGURATION_FILE_5_7, true);

        basicPage.loggedUser().logoutIfUserIsLogin();
        FormLoginPage login = midPoint.formLogin();
        login.login(getUsername(), getPassword());

        basicPage.listUsers().newUser("New "+ARCHETYPE_EMPLOYEE_LABEL.toLowerCase())
                .selectTabBasic()
                    .form()
                        .addAttributeValue(UserType.F_NAME, "janeway")
                        .addAttributeValue(UserType.F_GIVEN_NAME, "Kathryn")
                        .addAttributeValue(UserType.F_FAMILY_NAME, "Janeway")
                        .setDropDownAttributeValue(ActivationType.F_ADMINISTRATIVE_STATUS, "Enabled")
                        .setPasswordFieldsValues(new QName(SchemaConstantsGenerated.NS_COMMON, "value"), "abc123")
                        .and()
                    .and()
                .clickSave()
                    .feedback()
                        .isSuccess();

        AssignmentsTab<UserPage> assigmentsTab = showUser("janeway")
                .selectTabAssignments();
        Assert.assertFalse(assigmentsTab.table().containsText(ARCHETYPE_EMPLOYEE_NAME));
        Utils.addAsignments(assigmentsTab, "Secret Projects I");
    }
}
