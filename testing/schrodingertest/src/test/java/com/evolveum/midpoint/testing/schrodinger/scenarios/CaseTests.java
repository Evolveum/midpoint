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

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import com.evolveum.midpoint.schrodinger.component.cases.ChildrenCaseTable;
import com.evolveum.midpoint.schrodinger.component.cases.WorkitemDetailsPanel;
import com.evolveum.midpoint.schrodinger.component.common.search.Search;
import com.evolveum.midpoint.schrodinger.component.common.search.TextInputSearchItemPanel;

import org.testng.annotations.Test;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.modal.ForwardWorkitemModal;
import com.evolveum.midpoint.schrodinger.page.cases.*;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.ConstantsUtil;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

/**
 * Created by honchar.
 */
public class CaseTests extends AbstractSchrodingerTest {

    public static final File ROLE_WITH_ADMIN_APPROVER_XML = new File("./src/test/resources/role-with-admin-approver.xml");
    public static final String CASE_CREATION_TEST_USER_NAME = "caseCreationTestUser";
    public static final String CASE_CREATION_TEST_ROLE_NAME = "Role with admin approver";
    public static final String REQUEST_CASE_NAME = "Approving and executing change of user \"";
    public static final String ASSIGNING_ROLE_CASE_NAME = "Assigning role \"Role with admin approver\" to user \"";

    public static final String REJECT_WORKITEM_TEST_USER_NAME = "rejectWorkitemTestUser";
    public static final String CLAIM_WORKITEM_TEST_USER_NAME = "claimWorkitemTestUser";
    public static final String FORWARD_WORKITEM_TEST_USER_NAME = "forwardWorkitemTestUser";
    public static final String FORWARD_WORKITEM_TO_USER_NAME = "forwardToUser";

    @Override
    protected List<File> getObjectListToImport(){
        return Collections.singletonList(ROLE_WITH_ADMIN_APPROVER_XML);
    }

    @Test //covers mid-5813
    public void test100openCasesAndCheckMenuEnabled() {
        //check All cases page is opened, menu item is active, other case menu items are inactive
        basicPage.listAllCases()
                .assertMenuItemActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_CASES, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_MY_CASES_MENU_ITEM_LABEL_TEXT, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_APPROVALS_MENU_ITEM_LABEL_TEXT, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_MANUAL_CASES_MENU_ITEM_LABEL_TEXT, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_REQUESTS_MENU_ITEM_LABEL_TEXT, false))
                .listMyCases()
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_CASES, false))
                .assertMenuItemActive(getCaseMenuItemElement(ConstantsUtil.MENU_MY_CASES_MENU_ITEM_LABEL_TEXT, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_APPROVALS_MENU_ITEM_LABEL_TEXT, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_MANUAL_CASES_MENU_ITEM_LABEL_TEXT, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_REQUESTS_MENU_ITEM_LABEL_TEXT, false))
                .listAllManualCases()
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_CASES, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_MY_CASES_MENU_ITEM_LABEL_TEXT, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_APPROVALS_MENU_ITEM_LABEL_TEXT, false))
                .assertMenuItemActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_MANUAL_CASES_MENU_ITEM_LABEL_TEXT, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_REQUESTS_MENU_ITEM_LABEL_TEXT, false))
                .listAllRequests()
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_CASES, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_MY_CASES_MENU_ITEM_LABEL_TEXT, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_APPROVALS_MENU_ITEM_LABEL_TEXT, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_MANUAL_CASES_MENU_ITEM_LABEL_TEXT, false))
                .assertMenuItemActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_REQUESTS_MENU_ITEM_LABEL_TEXT, false))
                .listAllApprovals()
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_CASES, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_MY_CASES_MENU_ITEM_LABEL_TEXT, false))
                .assertMenuItemActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_APPROVALS_MENU_ITEM_LABEL_TEXT, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_MANUAL_CASES_MENU_ITEM_LABEL_TEXT, false))
                .assertMenuItemDoesntActive(getCaseMenuItemElement(ConstantsUtil.MENU_ALL_REQUESTS_MENU_ITEM_LABEL_TEXT, false));
    }

     @Test
     public void test110isCaseCreated(){
         createUserAndAssignRoleWithApprovement(CASE_CREATION_TEST_USER_NAME);

         AllCasesPage allCasesPage = basicPage.listAllCases();
         allCasesPage
                 .table()
                    .search()
                    .byName()
                    .inputValue(REQUEST_CASE_NAME + CASE_CREATION_TEST_USER_NAME)
                    .updateSearch()
                 .and()
                 .assertTableContainsLinkTextPartially(REQUEST_CASE_NAME + CASE_CREATION_TEST_USER_NAME);

     }

    @Test (dependsOnMethods = {"test110isCaseCreated"})
    public void test120approveCaseAction() {
        AllRequestsPage allRequestsPage = basicPage.listAllRequests();
        ChildrenCaseTable childrenCaseTable = allRequestsPage
                .table()
                .search()
                .byName()
                .inputValue(REQUEST_CASE_NAME + CASE_CREATION_TEST_USER_NAME)
                .updateSearch()
                .and()
                .clickByPartialName(REQUEST_CASE_NAME + CASE_CREATION_TEST_USER_NAME)
                .selectTabChildren()
                .table();
        childrenCaseTable.search()
                .byName()
                .inputValue(ASSIGNING_ROLE_CASE_NAME + CASE_CREATION_TEST_USER_NAME)
                .updateSearch();
        childrenCaseTable
                .clickByPartialName(ASSIGNING_ROLE_CASE_NAME + CASE_CREATION_TEST_USER_NAME)
                .selectTabWorkitems()
                .table()
                .clickByName(ASSIGNING_ROLE_CASE_NAME + CASE_CREATION_TEST_USER_NAME)
                .approveButtonClick();

        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);

        allRequestsPage = basicPage.listAllRequests();
        CasePage casePage = allRequestsPage
                .table()
                .search()
                .byName()
                .inputValue(REQUEST_CASE_NAME + CASE_CREATION_TEST_USER_NAME)
                .updateSearch()
                .and()
                .clickByPartialName(REQUEST_CASE_NAME + CASE_CREATION_TEST_USER_NAME);
        casePage
                        .selectTabChildren()
                        .table()
                        .assertCurrentTableContains("div", "closed")
                        .and()
                    .selectTabOperationRequest()
                        .assertChangesAreApplied();
    }

    @Test
    public void test130rejectCaseAction() {
        createUserAndAssignRoleWithApprovement(REJECT_WORKITEM_TEST_USER_NAME);

        AllRequestsPage allRequestsPage = basicPage.listAllRequests();
        ChildrenCaseTable childrenCaseTable = allRequestsPage
                .table()
                .search()
                .byName()
                .inputValue(REQUEST_CASE_NAME + REJECT_WORKITEM_TEST_USER_NAME)
                .updateSearch()
                .and()
                .clickByPartialName(REQUEST_CASE_NAME + REJECT_WORKITEM_TEST_USER_NAME)
                .selectTabChildren()
                .table();
        childrenCaseTable.search()
                .byName()
                .inputValue(ASSIGNING_ROLE_CASE_NAME + REJECT_WORKITEM_TEST_USER_NAME)
                .updateSearch();
        childrenCaseTable.clickByPartialName(ASSIGNING_ROLE_CASE_NAME + REJECT_WORKITEM_TEST_USER_NAME)
                .selectTabWorkitems()
                .table()
                .clickByName(ASSIGNING_ROLE_CASE_NAME + REJECT_WORKITEM_TEST_USER_NAME)
                .rejectButtonClick();

        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        allRequestsPage = basicPage.listAllRequests();
        CasePage casePage = allRequestsPage
                .table()
                .search()
                .byName()
                .inputValue(REQUEST_CASE_NAME + REJECT_WORKITEM_TEST_USER_NAME)
                .updateSearch()
                .and()
                .clickByPartialName(REQUEST_CASE_NAME + REJECT_WORKITEM_TEST_USER_NAME);

        casePage
                        .selectTabChildren()
                        .table()
                        .assertCurrentTableContains("div", "closed")
                        .and()
                .selectTabOperationRequest()
                .assertChangesAreRejected();
    }

    @Test
    public void test140forwardCaseAction() {
        createUserAndAssignRoleWithApprovement(FORWARD_WORKITEM_TEST_USER_NAME);

        UserPage user = basicPage.newUser();
        user.selectTabBasic()
                .form()
                .addAttributeValue("name", FORWARD_WORKITEM_TO_USER_NAME)
                .and()
                .and()
                .clickSave();

        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);

        AllRequestsPage allRequestsPage = basicPage.listAllRequests();
        ChildrenCaseTable childrenCaseTable = allRequestsPage
                .table()
                .search()
                .byName()
                .inputValue(REQUEST_CASE_NAME + FORWARD_WORKITEM_TEST_USER_NAME)
                .updateSearch()
                .and()
                .clickByPartialName(REQUEST_CASE_NAME + FORWARD_WORKITEM_TEST_USER_NAME)
                .selectTabChildren()
                .table();
        childrenCaseTable.search()
                .byName()
                .inputValue(ASSIGNING_ROLE_CASE_NAME + FORWARD_WORKITEM_TEST_USER_NAME)
                .updateSearch();
        WorkitemDetailsPanel<CasePage> workitemDetailsPanel = childrenCaseTable
                .clickByPartialName(ASSIGNING_ROLE_CASE_NAME + FORWARD_WORKITEM_TEST_USER_NAME)
                .selectTabWorkitems()
                .table()
                .clickByName(ASSIGNING_ROLE_CASE_NAME + FORWARD_WORKITEM_TEST_USER_NAME);

        ForwardWorkitemModal forwardWorkitemModal = workitemDetailsPanel.forwardButtonClick();
        TextInputSearchItemPanel<Search<ForwardWorkitemModal>> nameSearchField = forwardWorkitemModal
                .table()
                .search()
                .byName();
        nameSearchField
                .inputValue(FORWARD_WORKITEM_TO_USER_NAME)
                .updateSearch();
        forwardWorkitemModal
                .table()
                .clickByName(FORWARD_WORKITEM_TO_USER_NAME);

        allRequestsPage
                .feedback()
                .assertSuccess();

        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);

        basicPage.listAllRequests()
                .table()
                    .search()
                        .byName()
                        .inputValue(REQUEST_CASE_NAME + FORWARD_WORKITEM_TEST_USER_NAME)
                        .updateSearch()
                    .and()
                    .clickByPartialName(REQUEST_CASE_NAME + FORWARD_WORKITEM_TEST_USER_NAME)
                        .selectTabChildren()
                            .table()
                            .clickByPartialName(ASSIGNING_ROLE_CASE_NAME + FORWARD_WORKITEM_TEST_USER_NAME)
                                .selectTabWorkitems()
                                    .table()
                                    .clickByName(ASSIGNING_ROLE_CASE_NAME + FORWARD_WORKITEM_TEST_USER_NAME)
                                    .assertApproverElementValueMatches(FORWARD_WORKITEM_TO_USER_NAME);

    }

    private SelenideElement getCaseMenuItemElement(String menuIdentifier, boolean checkByLabelText){
        SelenideElement casesMenuItemElement;
        if (!checkByLabelText) {
            casesMenuItemElement = basicPage.getMenuItemElement(ConstantsUtil.ADMINISTRATION_MENU_ITEMS_SECTION_KEY,
                    ConstantsUtil.MENU_TOP_CASES, menuIdentifier);
        } else {
            casesMenuItemElement = basicPage.getMenuItemElementByMenuLabelText(ConstantsUtil.ADMINISTRATION_MENU_ITEMS_SECTION_KEY,
                    ConstantsUtil.MENU_TOP_CASES, menuIdentifier);
        }
        return casesMenuItemElement;
    }

    private void createUserAndAssignRoleWithApprovement(String userName){
        UserPage user = basicPage.newUser();
        user.selectTabBasic()
                .form()
                .addAttributeValue("name", userName)
                .and()
                .and()
                .clickSave();

        ListUsersPage users = basicPage.listUsers();
        users
                .table()
                .search()
                .byName()
                .inputValue(userName)
                .updateSearch()
                .and()
                .clickByName(userName)
                .selectTabAssignments()
                .clickAddAssignemnt()
                .selectType(ConstantsUtil.ASSIGNMENT_TYPE_SELECTOR_ROLE)
                .table()
                .search()
                .byName()
                .inputValue(CASE_CREATION_TEST_ROLE_NAME)
                .updateSearch()
                .and()
                .selectCheckboxByName(CASE_CREATION_TEST_ROLE_NAME)
                .and()
                .clickAdd()
                .and()
                .clickSave()
                .feedback()
                .isInfo();
    }
}
