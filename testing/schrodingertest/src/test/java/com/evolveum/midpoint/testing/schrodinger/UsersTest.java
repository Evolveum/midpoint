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

import static com.codeborne.selenide.Selenide.screenshot;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UsersTest extends AbstractSchrodingerTest {

    @Test
    public void testUserTablePaging() {
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
