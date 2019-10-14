/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.page.cases.*;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.ConstantsUtil;
import com.evolveum.midpoint.testing.schrodinger.TestBase;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by honchar.
 */
public class CaseTests extends TestBase {

    @Test //covers mid-5813
    public void test100openCasesAndCheckMenuEnabled() {
        //check All cases page is opened, menu item is active, other case menu items are inactive
        AllCasesPage allCasesPage = basicPage.listAllCases();

        Assert.assertTrue(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_CASES, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_MY_CASES_MENU_ITEM_LABEL_TEXT, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_APPROVALS_MENU_ITEM_LABEL_TEXT, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_MANUAL_CASES_MENU_ITEM_LABEL_TEXT, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_REQUESTS_MENU_ITEM_LABEL_TEXT, false));

        //check My cases page is opened, menu item is active, other case menu items are inactive
        MyCasesPage myCasesPage = basicPage.listMyCases();

        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_CASES, false));
        Assert.assertTrue(isCaseMenuItemActive(ConstantsUtil.MENU_MY_CASES_MENU_ITEM_LABEL_TEXT, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_APPROVALS_MENU_ITEM_LABEL_TEXT, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_MANUAL_CASES_MENU_ITEM_LABEL_TEXT, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_REQUESTS_MENU_ITEM_LABEL_TEXT, false));

        //check All manual cases page is opened, menu item is active, other case menu items are inactive
        AllManualCasesPage allManualCasesPage = basicPage.listAllManualCases();

        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_CASES, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_MY_CASES_MENU_ITEM_LABEL_TEXT, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_APPROVALS_MENU_ITEM_LABEL_TEXT, false));
        Assert.assertTrue(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_MANUAL_CASES_MENU_ITEM_LABEL_TEXT, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_REQUESTS_MENU_ITEM_LABEL_TEXT, false));

        //check All requests page is opened, menu item is active, other case menu items are inactive
        AllRequestsPage allRequestsPage = basicPage.listAllRequests();

        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_CASES, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_MY_CASES_MENU_ITEM_LABEL_TEXT, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_APPROVALS_MENU_ITEM_LABEL_TEXT, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_MANUAL_CASES_MENU_ITEM_LABEL_TEXT, false));
        Assert.assertTrue(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_REQUESTS_MENU_ITEM_LABEL_TEXT, false));

        //check All approvals page is opened, menu item is active, other case menu items are inactive
        AllApprovalsPage allApprovalsPage = basicPage.listAllApprovals();

        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_CASES, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_MY_CASES_MENU_ITEM_LABEL_TEXT, false));
        Assert.assertTrue(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_APPROVALS_MENU_ITEM_LABEL_TEXT, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_MANUAL_CASES_MENU_ITEM_LABEL_TEXT, false));
        Assert.assertFalse(isCaseMenuItemActive(ConstantsUtil.MENU_ALL_REQUESTS_MENU_ITEM_LABEL_TEXT, false));
     }

     @Test
     public void isCaseCreated(){
         importObject(ConstantsUtil.ROLE_WITH_ADMIN_APPROVER_XML,true);

         UserPage user = basicPage.newUser();
         user.selectTabBasic()
                    .form()
                        .addAttributeValue("name", ConstantsUtil.CASE_CREATION_TEST_USER_NAME)
                        .and()
                    .and()
                 .clickSave();

         ListUsersPage users = basicPage.listUsers();
         users
                 .table()
                    .search()
                    .byName()
                    .inputValue(ConstantsUtil.CASE_CREATION_TEST_USER_NAME)
                    .updateSearch()
                 .and()
                    .clickByName(ConstantsUtil.CASE_CREATION_TEST_USER_NAME)
                    .selectTabAssignments()
                        .clickAddAssignemnt()
                            .selectType(ConstantsUtil.ASSIGNMENT_TYPE_SELECTOR_ROLE)
                            .table()
                            .search()
                            .byName()
                            .inputValue(ConstantsUtil.CASE_CREATION_TEST_ROLE_NAME)
                            .updateSearch()
                        .and()
                        .selectCheckboxByName(ConstantsUtil.CASE_CREATION_TEST_ROLE_NAME)
                    .and()
                    .clickAdd()
                 .and()
                 .clickSave()
                 .feedback()
                 .isInfo();

         AllCasesPage allCasesPage = basicPage.listAllCases();
         allCasesPage
                 .table()
                    .search()
                    .byName()
                    .inputValue(ConstantsUtil.CASE_CREATION_TEST_CASE_NAME)
                    .updateSearch()
                 .and()
                 .clickByName(ConstantsUtil.CASE_CREATION_TEST_CASE_NAME);

     }

    private boolean isCaseMenuItemActive(String menuIdentifier, boolean checkByLabelText){
        SelenideElement casesMenuItemElement;
        if (!checkByLabelText) {
            casesMenuItemElement = basicPage.getMenuItemElement(ConstantsUtil.ADMINISTRATION_MENU_ITEMS_SECTION_KEY,
                    ConstantsUtil.MENU_TOP_CASES, menuIdentifier);
        } else {
            casesMenuItemElement = basicPage.getMenuItemElementByMenuLabelText(ConstantsUtil.ADMINISTRATION_MENU_ITEMS_SECTION_KEY,
                    ConstantsUtil.MENU_TOP_CASES, menuIdentifier);
        }
        SelenideElement casesMenuLi = casesMenuItemElement.parent().parent();
        return casesMenuLi.has(Condition.cssClass("active"));
    }
}
