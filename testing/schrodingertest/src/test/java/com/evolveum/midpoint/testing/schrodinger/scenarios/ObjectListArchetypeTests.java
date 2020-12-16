/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.configuration.AdminGuiTab;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

/**
 * Created by honchar
 */
public class ObjectListArchetypeTests extends AbstractSchrodingerTest {

    private static final File EMPLOYEE_ARCHETYPE_FILE = new File("src/test/resources/configuration/objects/archetypes/archetype-employee.xml");
    private static final String ARCHETYPE_OBJECT_NAME = "Employee";
    private static final String ARCHETYPE_PLURAL_LABEL = "Employees";
    private static final String ARCHETYPE_ICON_CSS_STYLE = ".fa.fa-male";
    private static final String EMPLOYEE_USER_NAME_VALUE = "TestEmployee";

    public static final String OBJECT_LIST_ARCHETYPE_TESTS_GROUP = "ObjectListArchetypeTests";

    @Override
    protected List<File> getObjectListToImport() {
        return Collections.singletonList(EMPLOYEE_ARCHETYPE_FILE);
    }

    @Test(priority = 1, groups = OBJECT_LIST_ARCHETYPE_TESTS_GROUP)
    public void configureArchetypeObjectListView(){
        AdminGuiTab adminGuiTab = basicPage.adminGui();
        adminGuiTab
                .addNewObjectCollection(ARCHETYPE_PLURAL_LABEL, "User", "Archetype", ARCHETYPE_PLURAL_LABEL)
                    .feedback()
                        .isSuccess();
    }



    @Test(priority = 2, dependsOnMethods ={"configureArchetypeObjectListView"}, groups = OBJECT_LIST_ARCHETYPE_TESTS_GROUP)
    public void actualizeArchetypeConfiguration() {
        basicPage.loggedUser().logout();
        midPoint.formLogin()
                .loginWithReloadLoginPage(getUsername(), getPassword());

        //check archetype pluralLabel
        ListUsersPage collectionListPage = basicPage.listUsers(ARCHETYPE_PLURAL_LABEL);

        //check the icon class next to the Employee  menu item
        Assert.assertTrue("fa fa-male"
                .equals(basicPage.getAdministrationMenuItemIconClass("PageAdmin.menu.top.users", ARCHETYPE_PLURAL_LABEL)),
                "Employee icon css style doesn't match to .fa.fa-male");

        Assert.assertTrue(collectionListPage
                .table()
                .buttonToolBarExists(),
                "Button toolbar is absent");

        //check new employee button exists on the toolbar
        collectionListPage
                .table()
                .getToolbarButton(ARCHETYPE_ICON_CSS_STYLE)
                .shouldBe(Condition.visible)
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S);

    }

    @Test(priority = 3, dependsOnMethods ={"actualizeArchetypeConfiguration"}, groups = OBJECT_LIST_ARCHETYPE_TESTS_GROUP)
    public void createNewEmployeeUser(){
        ListUsersPage collectionListPage = basicPage.listUsers(ARCHETYPE_PLURAL_LABEL);

        collectionListPage
                .table()
                    .newObjectButtonClickPerformed(ARCHETYPE_ICON_CSS_STYLE)
                        .selectTabBasic()
                            .form()
                                .addAttributeValue("name", EMPLOYEE_USER_NAME_VALUE)
                            .and()
                        .and()
                    .clickSave()
                .feedback()
                .isSuccess();

        basicPage.listUsers(ARCHETYPE_PLURAL_LABEL)
                .table()
                    .search()
                        .byName()
                            .inputValue(EMPLOYEE_USER_NAME_VALUE)
                                .updateSearch()
                            .and()
                        .clickByName(EMPLOYEE_USER_NAME_VALUE);

    }

    @Test(priority = 4, dependsOnMethods ={"actualizeArchetypeConfiguration"})
    public void checkNewObjectButtonWithDropdown(){
        Selenide.sleep(5000);
        ListUsersPage userListPage = basicPage.listUsers();
        Assert.assertEquals(userListPage
                .table()
                .countDropdownButtonChildrenButtons(".fa.fa-plus"), 2);
        userListPage = basicPage.listUsers();
        userListPage
                .table()
                    .newObjectCollectionButtonClickPerformed(".fa.fa-plus", ARCHETYPE_ICON_CSS_STYLE);
    }

}

