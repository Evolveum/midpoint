/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger;

import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.screenshot;

/**
 * Created by Viliam Repan (lazyman).
 */
public class UserTest extends TestBase {

    @Test
    public void createUser() {

        //@formatter:off
        UserPage user = basicPage.newUser();
        user.selectTabBasic()
                .form()
                    .addAttributeValue("name", "jdoe222323")
                    .addAttributeValue(UserType.F_GIVEN_NAME, "john")
                    .addAttributeValue(UserType.F_FAMILY_NAME, "doe")
                    .and()
                .and()
            .clickSave();

//        user.selectTabProjections().and()
//            .selectTabPersonas().and()
//            .selectTabAssignments().and()
//            .selectTabTasks().and()
//            .selectTabDelegations().and()
//            .selectTabDelegatedToMe().and()
        //@formatter:on

        screenshot("create");

        ListUsersPage users = user.listUsers();

        // todo validation
    }
}
