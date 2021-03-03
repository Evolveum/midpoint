/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs.fundamental;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schrodinger.MidPoint;

import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;

import com.evolveum.midpoint.schrodinger.page.self.ProfilePage;
import com.evolveum.midpoint.schrodinger.util.Utils;

import com.evolveum.midpoint.testing.schrodinger.labs.AbstractLabTest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;

/**
 * @author skublik
 */

public class M12Authorizations extends AbstractLabTest {

    private static final Logger LOG = LoggerFactory.getLogger(M12Authorizations.class);
    protected static final String LAB_OBJECTS_DIRECTORY = LAB_FUNDAMENTAL_DIRECTORY + "M12/";

    private static final File ROLE_BASIC_USER_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-basic-user.xml");
    private static final File ROLE_BASIC_USER_FILE_12_1 = new File(LAB_OBJECTS_DIRECTORY + "roles/role-basic-user-12-1.xml");

    @BeforeClass(alwaysRun = true, dependsOnMethods = { "springTestContextPrepareTestInstance" })
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
    }

    @Test
    public void mod12test01BasicUserAuthorization() {
        addObjectFromFile(ROLE_BASIC_USER_FILE);
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        showUser("X000005").selectTabBasic()
                .form()
                    .setPasswordFieldsValues(new QName(SchemaConstantsGenerated.NS_COMMON, "value"), "qwerty12345XXXX")
                    .and()
                .and()
            .clickSave()
                .feedback()
                    .isSuccess();

        basicPage.loggedUser().logoutIfUserIsLogin();
        FormLoginPage login = midPoint.formLogin();
        login.login("X000005", "qwerty12345XXXX")
            .feedback()
                .isError();

        login.login(getUsername(), getPassword());

        Utils.addAsignments(showUser("X000005").selectTabAssignments(), "Basic user");

        basicPage.loggedUser().logoutIfUserIsLogin();
        login.login("X000005", "qwerty12345XXXX");

        ProfilePage profile = basicPage.profile();
        profile.selectTabProjections()
            .table()
                .assertTableContainsLinksTextPartially(""); //TODO projections names

        profile.selectTabAssignments()
                .table()
                    .assertTableContainsLinksTextPartially("Basic user", ""); //TODO roles names

        basicPage.credentials(); //TODO implement credentials page

        basicPage.loggedUser().logoutIfUserIsLogin();
        login.login(getUsername(), getPassword());

        addObjectFromFile(ROLE_BASIC_USER_FILE_12_1);
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);

        basicPage.loggedUser().logoutIfUserIsLogin();
        login.login("X000005", "qwerty12345ZZZZ");

    }
}
