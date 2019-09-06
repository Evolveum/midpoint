/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger;

import com.evolveum.midpoint.schrodinger.component.common.Paging;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.screenshot;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UsersTest extends TestBase {

    @Test
    public void testUserTablePaging() {
        ListUsersPage users = basicPage.listUsers();

        screenshot("listUsers");

        Paging paging = users
                .table()
                .paging();

        paging.pageSize(5);

        screenshot("paging");

        paging.next()
                .last()
                .previous()
                .first()
                .actualPagePlusOne()
                .actualPagePlusTwo()
                .actualPageMinusTwo()
                .actualPageMinusOne();
    }
}
