/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.component;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.schrodinger.page.role.RolesPageTable;

import org.junit.Test;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

import com.evolveum.midpoint.schrodinger.component.common.Search;
import com.evolveum.midpoint.schrodinger.component.user.UsersPageTable;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

/**
 * Created by honchar
 */
public class SearchPanelTest extends AbstractSchrodingerTest {

    private static final String COMPONENT_RESOURCES_DIRECTORY = "./src/test/resources/component/";
    private static final String COMPONENT_OBJECTS_DIRECTORY = COMPONENT_RESOURCES_DIRECTORY + "objects/";
    private static final String COMPONENT_USERS_DIRECTORY = COMPONENT_OBJECTS_DIRECTORY + "users/";

    private static final File SEARCH_BY_NAME_USER_FILE = new File(COMPONENT_USERS_DIRECTORY + "searchByNameUser.xml");
    private static final File SEARCH_BY_GIVEN_NAME_USER_FILE = new File(COMPONENT_USERS_DIRECTORY + "searchByGivenNameUser.xml");
    private static final File SEARCH_BY_FAMILY_NAME_USER_FILE = new File(COMPONENT_USERS_DIRECTORY + "searchByFamilyNameUser.xml");

    private static final String NAME_ATTRIBUTE = "Name";
    private static final String GIVEN_NAME_ATTRIBUTE = "Given name";
    private static final String FAMILY_NAME_ATTRIBUTE = "Family name";
    private static final String ROLE_TYPE_ATTRIBUTE = "Role type";

    @BeforeClass
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
        importObject(SEARCH_BY_NAME_USER_FILE, true);
        importObject(SEARCH_BY_GIVEN_NAME_USER_FILE, true);
        importObject(SEARCH_BY_FAMILY_NAME_USER_FILE, true);

    }

    @Test
    public void test0010defaultSearchOnListPage() {
        UsersPageTable table = basicPage.listUsers().table();
        Search<UsersPageTable> usersListSearch = (Search<UsersPageTable>) table.search();

        usersListSearch
                .byItemName(NAME_ATTRIBUTE)
                .inputValue("searchByNameUser")
                .updateSearch();
        Assert.assertTrue(table.rowWithTextExists("searchByNameUser"));
        Assert.assertFalse(table.rowWithTextExists("searchByGivenNameUser"));
        Assert.assertFalse(table.rowWithTextExists("searchByFamilyNameUser"));

        usersListSearch
                .byItemName(GIVEN_NAME_ATTRIBUTE)
                .inputValue("searchByGivenNameUser")
                .updateSearch();
        Assert.assertFalse(table.rowWithTextExists("searchByNameUser"));
        Assert.assertTrue(table.rowWithTextExists("searchByGivenNameUser"));
        Assert.assertFalse(table.rowWithTextExists("searchByFamilyNameUser"));

        usersListSearch
                .byItemName(FAMILY_NAME_ATTRIBUTE)
                .inputValue("searchByFamilyNameUser")
                .updateSearch();
        Assert.assertFalse(table.rowWithTextExists("searchByNameUser"));
        Assert.assertFalse(table.rowWithTextExists("searchByGivenNameUser"));
        Assert.assertTrue(table.rowWithTextExists("searchByFamilyNameUser"));
    }

    @Test
    public void test0020addSearchAttributeByAddButtonClick() {
        RolesPageTable table = basicPage.listRoles().table();
        Search<RolesPageTable> search = (Search<RolesPageTable>) table.search();
        search.addSearchItemByAddButtonClick(ROLE_TYPE_ATTRIBUTE);
        Assert.assertNotNull(search.byItemName(ROLE_TYPE_ATTRIBUTE, false));
    }
}
