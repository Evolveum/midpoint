/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.page;

import com.evolveum.midpoint.schrodinger.page.configuration.QueryPlaygroundPage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by Kate Honchar.
 */
public class QueryPlaygroundPageTest extends AbstractSchrodingerTest {

    @Test //covers MID-5346
    public void test001useInObjectListOptionTest() {
        UserPage user = basicPage.newUser();

        Assert.assertTrue(
                user.selectTabBasic()
                        .form()
                        .addAttributeValue("name", "a_start")
                        .and()
                        .and()
                        .clickSave()
                        .feedback()
                        .isSuccess()
        );

        user = basicPage.newUser();
        Assert.assertTrue(
                user.selectTabBasic()
                        .form()
                        .addAttributeValue("name", "b_start")
                        .and()
                        .and()
                        .clickSave()
                        .feedback()
                        .isSuccess()
        );

        QueryPlaygroundPage queryPlaygroundPage = basicPage.queryPlayground();
        queryPlaygroundPage
                .setQuerySampleValue(QueryPlaygroundPage.QueryPlaygroundSample.FIRST_10_USERS_WITH_FIRST_A)
                .useInObjectListButtonClick();

        ListUsersPage usersPage = basicPage.listUsers();
        Assert.assertTrue(
                usersPage
                        .table()
                        .containsLinkTextPartially("a_start")
        );

        Assert.assertFalse(
                usersPage
                        .table()
                        .containsLinkTextPartially("b_start")
        );


    }
}
