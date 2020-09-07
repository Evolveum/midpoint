/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.common.Paging;
import com.evolveum.midpoint.schrodinger.component.common.Popover;
import com.evolveum.midpoint.schrodinger.component.common.Search;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;

import com.evolveum.midpoint.testing.schrodinger.scenarios.ScenariosCommons;

import org.openqa.selenium.Keys;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static com.codeborne.selenide.Selenide.screenshot;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UsersTest extends AbstractSchrodingerTest {

    private static final File LOOKUP_TABLE_SUBTYPES = new File("src/test/resources/configuration/objects/lookuptable/subtypes.xml");
    private static final File OT_FOR_LOOKUP_TABLE_SUBTYPES = new File("src/test/resources/configuration/objects/objecttemplate/object-template-for-lookup-table-subtypes.xml");
    private static final File SYSTEM_CONFIG_WITH_LOOKUP_TABLE = new File("src/test/resources/configuration/objects/systemconfig/system-configuration-with-lookup-table.xml");

    @BeforeClass
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
        importObject(LOOKUP_TABLE_SUBTYPES, true);
        importObject(OT_FOR_LOOKUP_TABLE_SUBTYPES, true);
        importObject(SYSTEM_CONFIG_WITH_LOOKUP_TABLE, true);

    }

    @Test
    public void test001UserTablePaging() {
        ListUsersPage users = basicPage.listUsers();

        screenshot("listUsers");

        for (int i = 0; i < 21; i++) {
            addUser("john" + i);
            Selenide.sleep(5000);
        }

        Paging paging = users
                .table()
                .paging();

        paging.pageSize(5);
        Selenide.sleep(3000);

        screenshot("paging");

        paging.next();
        paging.last();
        paging.previous();
        paging.first();
        paging.actualPagePlusOne();
        paging.actualPagePlusTwo();
        paging.actualPageMinusTwo();
        paging.actualPageMinusOne();
    }

    @Test
    public void test002SearchWithLookupTable() {

        UserPage user = basicPage.newUser();
        user.selectTabBasic()
                .form()
                .addAttributeValue("name", "searchUser")
                .addAttributeValue("title", "PhD.")
                .and()
                .and()
                .clickSave();


        ListUsersPage users = basicPage.listUsers();

        Assert.assertTrue(
            users
                .table()
                    .search()
                        .byItemName("title")
                            .inputValue("PhD.")
                    .updateSearch()
                    .and()
                .currentTableContains("searchUser")
        );

        Assert.assertTrue(
                users
                .table()
                    .search()
                        .byItemName("title")
                            .inputValue("PhD")
                    .updateSearch()
                    .and()
                .currentTableContains("searchUser")
        );

        Assert.assertFalse(
            users
                .table()
                    .search()
                        .byItemName("title")
                            .inputValue("Ing.")
                    .updateSearch()
                    .and()
                .currentTableContains("searchUser")
        );

        Assert.assertFalse(
            users
                .table()
                    .search()
                        .byItemName("title")
                            .inputValue("Ing")
                    .updateSearch()
                    .and()
                .currentTableContains("searchUser")
        );

    }

    private void addUser(String name) {
        UserPage user = basicPage.newUser();
        user.selectTabBasic()
                .form()
                .addAttributeValue("name", name)
                .and()
                .and()
                .clickSave();
    }
}
