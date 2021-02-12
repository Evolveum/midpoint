/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.component.common.Paging;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;

import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.codeborne.selenide.Selenide.screenshot;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UsersTest extends AbstractSchrodingerTest {

    private static final File LOOKUP_TABLE_SUBTYPES = new File("src/test/resources/configuration/objects/lookuptable/subtypes.xml");
    private static final File OT_FOR_LOOKUP_TABLE_SUBTYPES = new File("src/test/resources/configuration/objects/objecttemplate/object-template-for-lookup-table-subtypes.xml");
    private static final File SYSTEM_CONFIG_WITH_LOOKUP_TABLE = new File("src/test/resources/configuration/objects/systemconfig/system-configuration-with-lookup-table.xml");
    private static final File MULTIPLE_USERS = new File("src/test/resources/configuration/objects/users/jack-users.xml");

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(LOOKUP_TABLE_SUBTYPES, OT_FOR_LOOKUP_TABLE_SUBTYPES, SYSTEM_CONFIG_WITH_LOOKUP_TABLE, MULTIPLE_USERS);
    }

    @Test
    public void test001UserTablePaging() {
        ListUsersPage users = basicPage.listUsers();

        Paging paging = users
                .table()
                .paging();

        paging.pageSize(5);
        Selenide.sleep(3000);

        paging
                .next()
                .last()
                .previous()
                .first()
                .actualPagePlusOne()
                .actualPagePlusTwo()
                .actualPageMinusTwo()
                .actualPageMinusOne();
    }

    @Test
    public void test002SearchWithLookupTable() {

        Map<String, String> attr = new HashMap<>();
        attr.put("name", "searchUser");
        attr.put("title", "PhD.");
        createUser(attr);

        ListUsersPage users = basicPage.listUsers();

        users
                .table()
                    .search()
                        .textInputPanelByItemName("title")
                            .inputValue("PhD.")
                    .updateSearch()
                    .and()
                .assertCurrentTableContains("searchUser");

        users
                .table()
                    .search()
                        .textInputPanelByItemName("title")
                            .inputValue("PhD")
                    .updateSearch()
                    .and()
                .assertCurrentTableContains("searchUser");

        users
                .table()
                    .search()
                        .textInputPanelByItemName("title")
                            .inputValue("Ing.")
                    .updateSearch()
                    .and()
                .assertCurrentTableDoesntContain("searchUser");

        users
                .table()
                    .search()
                        .textInputPanelByItemName("title")
                            .inputValue("Ing")
                    .updateSearch()
                    .and()
                .assertCurrentTableDoesntContain("searchUser");

    }
}
