/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.configuration.AdminGuiTab;
import com.evolveum.midpoint.schrodinger.component.modal.ObjectBrowserModal;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.testing.schrodinger.TestBase;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar
 */
public class ObjectListArchetypeTests extends TestBase {

    private static final File EMPLOYEE_ARCHETYPE_FILE = new File("src/test/resources/configuration/objects/archetypes/archetype-employee.xml");
    private static final String ARCHETYPE_OBJECT_NAME = "Employee";
    private static final String ARCHETYPE_PLURAL_LABEL = "Employees";
    private static final String ARCHETYPE_ICON_CSS_STYLE = "fa fa-male";
    private static final String EMPLOYEE_USER_NAME_VALUE = "TestEmployee";

    private static final String COLLECTION_REF_ATTRIBUTE_NAME = "Collection ref";
    private static final String OBJECT_COLLECTION_VIEWS_HEADER = "Object collection views";
    private static final String OBJECT_COLLECTION_VIEW_HEADER = "Object collection view";
    private static final String NEW_GUI_OBJECT_LIST_VIEW_HEADER = "New gui object list view";
    private static final String NEW_OBJECT_LIST_VIEW_CONTAINER_KEY = "GuiObjectListViewType.details";
    private static final String NEW_OBJECT_LIST_VIEW_CONTAINER_NEW_VALUE_KEY = "GuiObjectListViewType.details.newValue";
    private static final String COLLECTION_HEADER = "Collection";
    public static final String OBJECT_LIST_ARCHETYPE_TESTS_GROUP = "bjectListArchetypeTests";

    @Test(priority = 0, groups = OBJECT_LIST_ARCHETYPE_TESTS_GROUP)
    public void importEmployeeArchetype() {
        importObject(EMPLOYEE_ARCHETYPE_FILE, true);
    }

    @Test(priority = 1, dependsOnMethods ={"importEmployeeArchetype"}, groups = OBJECT_LIST_ARCHETYPE_TESTS_GROUP)
    public void configureArchetypeObjectListView(){
        AdminGuiTab adminGuiTab = basicPage.adminGui();
        PrismForm<AdminGuiTab> prismForm = adminGuiTab.form();
        prismForm
                .expandContainerPropertiesPanel(OBJECT_COLLECTION_VIEWS_HEADER)
                .addNewContainerValue(OBJECT_COLLECTION_VIEW_HEADER, NEW_GUI_OBJECT_LIST_VIEW_HEADER)
                .collapseAllChildrenContainers(OBJECT_COLLECTION_VIEW_HEADER)
                .expandContainerPropertiesPanel(NEW_OBJECT_LIST_VIEW_CONTAINER_NEW_VALUE_KEY)
                .expandContainerPropertiesPanel(COLLECTION_HEADER);

        //set UserType
        SelenideElement newGuiObjectListViewPropertiesPanel = prismForm.getPrismPropertiesPanel(NEW_OBJECT_LIST_VIEW_CONTAINER_NEW_VALUE_KEY);
        newGuiObjectListViewPropertiesPanel
                .find(Schrodinger.byDataResourceKey("Type"))
                .waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)
                .find(By.tagName("select"))
                .sendKeys("User");

        //set archetypeRef
        newGuiObjectListViewPropertiesPanel
                .find(Schrodinger.byElementValue("span", COLLECTION_REF_ATTRIBUTE_NAME))
                .parent()
                .parent()
                .parent()
                .$(Schrodinger.byDataId("edit"))
                .click();

        SelenideElement modalWindow = $(By.className("wicket-modal"))
                .waitUntil(Condition.appear, MidPoint.TIMEOUT_DEFAULT_2_S);

        ObjectBrowserModal objectBrowserModal = new ObjectBrowserModal<>(prismForm, modalWindow);
        objectBrowserModal
                .selectType("Archetype")
                .table()
                    .search()
                        .byName()
                        .inputValue(ARCHETYPE_OBJECT_NAME)
                        .updateSearch();
        objectBrowserModal
                .table()
                    .clickByName(ARCHETYPE_OBJECT_NAME);

        Assert.assertTrue(prismForm
                .compareInputAttributeValue(COLLECTION_REF_ATTRIBUTE_NAME, ARCHETYPE_OBJECT_NAME + ": ArchetypeType"));

        adminGuiTab
                .getParent()
                .save()
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
        Assert.assertTrue(ARCHETYPE_ICON_CSS_STYLE
                .equals(basicPage.getAdministrationMenuItemIconClass("PageAdmin.menu.top.users", ARCHETYPE_PLURAL_LABEL)));

        Assert.assertTrue(collectionListPage
                .table()
                .buttonToolBarExists());

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
        ListUsersPage userListPage = basicPage.listUsers();
        Assert.assertTrue(userListPage
                            .table()
                                .getToolbarButton("fa fa-plus")
                                .exists());

        SelenideElement newObjectButton = userListPage
                .table()
                    .getToolbarButton("fa fa-plus");

        newObjectButton.waitUntil(Condition.appears, MidPoint.TIMEOUT_DEFAULT_2_S).click();

        newObjectButton
                    .$(Schrodinger.byElementAttributeValue("i", "class", ARCHETYPE_ICON_CSS_STYLE))
                    .exists();

        newObjectButton
                .$(Schrodinger.byElementAttributeValue("i", "class", "fa fa-user")) //standard user icon
                .exists();
    }

}

