/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.component;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.evolveum.midpoint.schrodinger.component.DateTimePanel;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schrodinger.component.common.search.Search;
import com.evolveum.midpoint.schrodinger.component.user.UsersPageTable;
import com.evolveum.midpoint.schrodinger.page.role.RolesPageTable;
import com.evolveum.midpoint.schrodinger.page.service.ServicesPageTable;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

/**
 * Created by honchar
 */
public class SearchPanelTest extends AbstractSchrodingerTest {

    private static final String COMPONENT_RESOURCES_DIRECTORY = "./src/test/resources/component/";
    private static final String COMPONENT_OBJECTS_DIRECTORY = COMPONENT_RESOURCES_DIRECTORY + "objects/";
    private static final String COMPONENT_USERS_DIRECTORY = COMPONENT_OBJECTS_DIRECTORY + "users/";
    private static final String COMPONENT_ROLES_DIRECTORY = COMPONENT_OBJECTS_DIRECTORY + "roles/";
    private static final String COMPONENT_ORGS_DIRECTORY = COMPONENT_OBJECTS_DIRECTORY + "orgs/";

    private static final File SEARCH_BY_NAME_USER_FILE = new File(COMPONENT_USERS_DIRECTORY + "searchByNameUser.xml");
    private static final File SEARCH_BY_GIVEN_NAME_USER_FILE = new File(COMPONENT_USERS_DIRECTORY + "searchByGivenNameUser.xml");
    private static final File SEARCH_BY_FAMILY_NAME_USER_FILE = new File(COMPONENT_USERS_DIRECTORY + "searchByFamilyNameUser.xml");
    private static final File SEARCH_BY_ROLE_MEMBERSHIP_NAME_USER_FILE = new File(COMPONENT_USERS_DIRECTORY + "user-role-membership-name-search.xml");
    private static final File SEARCH_BY_ROLE_MEMBERSHIP_OID_USER_FILE = new File(COMPONENT_USERS_DIRECTORY + "user-role-membership-oid-search.xml");
    private static final File SEARCH_BY_ROLE_MEMBERSHIP_TYPE_USER_FILE = new File(COMPONENT_USERS_DIRECTORY + "user-role-membership-type-search.xml");
    private static final File SEARCH_BY_ROLE_MEMBERSHIP_RELATION_USER_FILE = new File(COMPONENT_USERS_DIRECTORY + "user-role-membership-relation-search.xml");
    private static final File REQUESTABLE_ROLE_FILE = new File(COMPONENT_ROLES_DIRECTORY + "requestableRole.xml");
    private static final File DISABLED_ROLE_FILE = new File(COMPONENT_ROLES_DIRECTORY + "disabledRole.xml");
    private static final File SEARCH_BY_ROLE_MEMBERSHIP_NAME_ROLE_FILE = new File(COMPONENT_ROLES_DIRECTORY + "role-membership-search-by-name.xml");
    private static final File SEARCH_BY_ROLE_MEMBERSHIP_OID_ROLE_FILE = new File(COMPONENT_ROLES_DIRECTORY + "role-membership-search-by-oid.xml");
    private static final File SEARCH_BY_ROLE_MEMBERSHIP_TYPE_ORG_FILE = new File(COMPONENT_ORGS_DIRECTORY + "org-membership-search-by-type.xml");
    private static final File SEARCH_BY_ROLE_MEMBERSHIP_RELATIONS_ROLE_FILE = new File(COMPONENT_ROLES_DIRECTORY + "role-membership-search-by-relation.xml");
    private static final File SYSTEM_CONFIG_WITH_CONFIGURED_USER_SEARCH = new File("./src/test/resources/configuration/objects/systemconfig/system-configuration-with-configured-user-search.xml");
    private static final File USER_WITH_EMPLOYEE_NUMBER_FILE = new File(COMPONENT_USERS_DIRECTORY + "user-with-employee-number.xml");
    private static final File USER_WITH_EMAIL_ADDRESS_FILE = new File(COMPONENT_USERS_DIRECTORY + "user-with-email-address.xml");

    private static final String NAME_ATTRIBUTE = "Name";
    private static final String GIVEN_NAME_ATTRIBUTE = "Given name";
    private static final String FAMILY_NAME_ATTRIBUTE = "Family name";
    private static final String REQUESTABLE_ATTRIBUTE = "Requestable";
    private static final String ADMINISTRATIVE_STATUS_ATTRIBUTE = "Administrative status";
    private static final String ROLE_MEMBERSHIP_ATTRIBUTE = "Role membership";
    private static final String REF_SEARCH_FIELD_VALUE = "roleMembershipByNameSearch; Oid:33fd485e-kk21-43g2-94sd-18dgrw6ed4aa; Relation:default; RoleType";

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(SEARCH_BY_NAME_USER_FILE, SEARCH_BY_GIVEN_NAME_USER_FILE, SEARCH_BY_FAMILY_NAME_USER_FILE,
                REQUESTABLE_ROLE_FILE, DISABLED_ROLE_FILE, SEARCH_BY_ROLE_MEMBERSHIP_NAME_USER_FILE, SEARCH_BY_ROLE_MEMBERSHIP_OID_USER_FILE,
                SEARCH_BY_ROLE_MEMBERSHIP_TYPE_USER_FILE, SEARCH_BY_ROLE_MEMBERSHIP_RELATION_USER_FILE, SEARCH_BY_ROLE_MEMBERSHIP_NAME_ROLE_FILE,
                SEARCH_BY_ROLE_MEMBERSHIP_OID_ROLE_FILE, SEARCH_BY_ROLE_MEMBERSHIP_TYPE_ORG_FILE, SEARCH_BY_ROLE_MEMBERSHIP_RELATIONS_ROLE_FILE,
                USER_WITH_EMPLOYEE_NUMBER_FILE, USER_WITH_EMAIL_ADDRESS_FILE);
    }

    @Test
    public void test0010defaultSearchOnListPage() {
        UsersPageTable table = basicPage.listUsers().table();
        Search<UsersPageTable> usersListSearch = (Search<UsersPageTable>) table.search();

        usersListSearch
                .textInputPanelByItemName(NAME_ATTRIBUTE)
                .inputValue("searchByNameUser")
                .updateSearch();
        table.assertTableContainsText("searchByNameUser");
        table.assertTableDoesntContainText("searchByGivenNameUser");
        table.assertTableDoesntContainText("searchByFamilyNameUser");
        usersListSearch.clearTextSearchItemByNameAndUpdate(NAME_ATTRIBUTE);

        usersListSearch
                .textInputPanelByItemName(GIVEN_NAME_ATTRIBUTE)
                .inputValue("searchByGivenNameUser")
                .updateSearch();
        table.assertTableDoesntContainText("searchByNameUser");
        table.assertTableContainsText("searchByGivenNameUser");
        table.assertTableDoesntContainText("searchByFamilyNameUser");
        usersListSearch.clearTextSearchItemByNameAndUpdate(GIVEN_NAME_ATTRIBUTE);

        usersListSearch
                .textInputPanelByItemName(FAMILY_NAME_ATTRIBUTE)
                .inputValue("searchByFamilyNameUser")
                .updateSearch();
        table.assertTableDoesntContainText("searchByNameUser");
        table.assertTableDoesntContainText("searchByGivenNameUser");
        table.assertTableContainsText("searchByFamilyNameUser");
        usersListSearch.clearTextSearchItemByNameAndUpdate(FAMILY_NAME_ATTRIBUTE);
    }

    @Test
    public void test0020addSearchAttributeByAddButtonClick() {
        RolesPageTable table = basicPage.listRoles().table();
        Search<RolesPageTable> search = (Search<RolesPageTable>) table.search();
        search.addSearchItemByAddButtonClick(REQUESTABLE_ATTRIBUTE);
        Assert.assertNotNull(search.textInputPanelByItemName(REQUESTABLE_ATTRIBUTE, false));
    }

    @Test
    public void test0030addSearchAttributeByNameLinkClick() {
        ServicesPageTable table = basicPage.listServices().table();
        Search<ServicesPageTable> search = (Search<ServicesPageTable>) table.search();
        search.addSearchItemByNameLinkClick(ROLE_MEMBERSHIP_ATTRIBUTE);
        Assert.assertNotNull(search.textInputPanelByItemName(ROLE_MEMBERSHIP_ATTRIBUTE, false));
    }

    @Test
    public void test0040booleanAttributeSearch() {
        RolesPageTable table = basicPage.listRoles().table();
        Search<RolesPageTable> search = (Search<RolesPageTable>) table.search();
        search.resetBasicSearch();
        table.assertTableObjectsCountNotEquals(1);
        search.dropDownPanelByItemName(REQUESTABLE_ATTRIBUTE)
                .inputDropDownValue("True")
                .updateSearch();
        table.assertTableObjectsCountEquals(1);
    }

    @Test
    public void test0050enumAttributeSearch() {
        RolesPageTable table = basicPage.listRoles().table();
        Search<RolesPageTable> search = (Search<RolesPageTable>) table.search();
        search.resetBasicSearch();
        table.assertTableObjectsCountNotEquals(1);
        search.dropDownPanelByItemName(ADMINISTRATIVE_STATUS_ATTRIBUTE)
                .inputDropDownValue("disabled")
                .updateSearch();
        table.assertTableObjectsCountEquals(1);
    }

    @Test
    public void test0060referenceAttributeByOidSearch() {
        UsersPageTable table = basicPage.listUsers().table();
        Search<UsersPageTable> search = (Search<UsersPageTable>) table.search();
        search.resetBasicSearch();
        table.assertTableObjectsCountNotEquals(1);
        search.referencePanelByItemName(ROLE_MEMBERSHIP_ATTRIBUTE)
                .inputRefOid("332fd5e-sdf7-4322-9ff3-1848cd6ed4aa")
                .updateSearch();
        table.assertTableObjectsCountEquals(1);
        table.assertTableContainsLinkTextPartially("testUserWithRoleMembershipSearchByOid");
    }

    @Test
    public void test0070referenceAttributeByTypeSearch() {
        UsersPageTable table = basicPage.listUsers().table();
        Search<UsersPageTable> search = (Search<UsersPageTable>) table.search();
        search.resetBasicSearch();
        search.referencePanelByItemName(ROLE_MEMBERSHIP_ATTRIBUTE)
                .inputRefType("Organization")
                .updateSearch();
        table.assertTableObjectsCountEquals(1);
        table.assertTableContainsLinkTextPartially("testUserWithRoleMembershipSearchByType");
    }

    @Test
    public void test0080referenceAttributeByRelationSearch() {
        UsersPageTable table = basicPage.listUsers().table();
        Search<UsersPageTable> search = (Search<UsersPageTable>) table.search();
        search.resetBasicSearch();
        search.referencePanelByItemName(ROLE_MEMBERSHIP_ATTRIBUTE)
                .inputRefRelation("Manager")
                .updateSearch();
        table.assertTableObjectsCountEquals(1);
        table.assertTableContainsLinkTextPartially("testUserWithRoleMembershipSearchByRelation");
    }

    @Test
    public void test0080referenceAttributeByNameSearch() {
        UsersPageTable table = basicPage.listUsers().table();
        Search<UsersPageTable> search = (Search<UsersPageTable>) table.search();
        search.resetBasicSearch();
        search.referencePanelByItemName(ROLE_MEMBERSHIP_ATTRIBUTE)
                .inputRefName("roleMembershipByName", "roleMembershipByNameSearch")
                .updateSearch();
        table.assertTableObjectsCountEquals(1);
        table.assertTableContainsLinkTextPartially("testUserWithRoleMembershipSearchByName");
        Assert.assertTrue(search.referencePanelByItemName(ROLE_MEMBERSHIP_ATTRIBUTE).matchRefSearchFieldValue(REF_SEARCH_FIELD_VALUE));
    }

    @Test
    public void test0090configuredAttributesSearch() {
        addObjectFromFile(SYSTEM_CONFIG_WITH_CONFIGURED_USER_SEARCH);
        basicPage.loggedUser().logout();
        midPoint.formLogin()
                .loginWithReloadLoginPage(getUsername(), getPassword());

        UsersPageTable table = basicPage.listUsers().table();
        Search<UsersPageTable> search = (Search<UsersPageTable>) table.search();
        search.textInputPanelByItemName("By employee number", false)
                .inputValue("544")
                .updateSearch();
        table.assertTableObjectsCountEquals(1);
        table.assertTableContainsLinkTextPartially("searchByEmployeeNumberUser");

        search.clearTextSearchItemByNameAndUpdate("By employee number");
        search.textInputPanelByItemName("By email", false)
                .inputValue("testEmailAddress@test.com")
                .updateSearch();
        table.assertTableObjectsCountEquals(1);
        table.assertTableContainsLinkTextPartially("searchByEmailAddressUser");
    }

    @Test
    public void test0100dateIntervalSearch() {
        SimpleDateFormat formater = new SimpleDateFormat("dd/MM/yyyy");
        basicPage.auditLogViewer()
                .table()
                    .search()
                        .dateIntervalPanelByItemName("Time")
                            .getFromDateTimeFieldPanel()
                                .setDateTimeValue(formater.format(new Date()), "", "", DateTimePanel.AmOrPmChoice.AM);
    }

}
