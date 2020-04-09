/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import static com.codeborne.selenide.Selenide.$;

import javax.xml.namespace.QName;

import com.codeborne.selenide.Condition;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Created by honchar
 * covers LAB 4-1
 */
public class BasicProvisioningTest extends AbstractSchrodingerTest {

    private static final String USER_NAME_ATTRIBUTE = "Name";
    private static final String USER_GIVEN_NAME_ATTRIBUTE = "Given name";
    private static final String USER_FAMILY_NAME_ATTRIBUTE = "Family name";
    private static final String USER_PASSWORD_ATTRIBUTE = "Value";
    private static final String USER_ADMINISTRATIVE_STATUS_ATTRIBUTE = "Administrative status";
    private static final String PASSWORD_IS_SET_LABEL = "password is set";
    private static final QName PASSWORD_FIELD_LABEL = new QName(SchemaConstantsGenerated.NS_COMMON, "value");

    private static final String USER_NAME = "kirk";
    private static final String USER_GIVEN_NAME = "Jim";
    private static final String USER_FAMILY_NAME = "Kirk";
    private static final String USER_PASSWORD = "abc123";
    private static final String USER_ADMINISTRATIVE_STATUS = "Enabled";

    //todo dependsOnGroup
    @Test(groups = { "lab_4_1" })
    public void test001createUserKirk() {
        //we use New user link in this test
        UserPage userPage = basicPage.newUser();
        userPage
                .selectTabBasic()
                .form()
                .addAttributeValue(UserType.F_NAME, USER_NAME)
                .addAttributeValue(UserType.F_FAMILY_NAME, USER_FAMILY_NAME)
                .addAttributeValue(UserType.F_GIVEN_NAME, USER_GIVEN_NAME)
                .setDropDownAttributeValue(ActivationType.F_ADMINISTRATIVE_STATUS, USER_ADMINISTRATIVE_STATUS)
                .setPasswordFieldsValues(PASSWORD_FIELD_LABEL, USER_PASSWORD)
                .and()
                .and()
                .clickSave();

        ListUsersPage usersList = new ListUsersPage();

        usersList
                .table()
                .search()
                .byName()
                .inputValue(USER_NAME)
                .updateSearch()
                .and()
                .clickByName(USER_NAME);

        //check name attribute value
        Assert.assertTrue(userPage
                .selectTabBasic()
                .form()
                .compareInputAttributeValue(USER_NAME_ATTRIBUTE, USER_NAME));

        //check given name attribute value
        Assert.assertTrue(userPage
                .selectTabBasic()
                .form()
                .compareInputAttributeValue(USER_GIVEN_NAME_ATTRIBUTE, USER_GIVEN_NAME));

        //check family name attribute value
        Assert.assertTrue(userPage
                .selectTabBasic()
                .form()
                .compareInputAttributeValue(USER_FAMILY_NAME_ATTRIBUTE, USER_FAMILY_NAME));

        //check password is set label
        Assert.assertTrue($(Schrodinger.byElementValue("span", PASSWORD_IS_SET_LABEL))
                .shouldBe(Condition.visible)
                .exists());

        //check Administrative status value
        Assert.assertTrue(userPage
                .selectTabBasic()
                .form()
                .compareSelectAttributeValue(USER_ADMINISTRATIVE_STATUS_ATTRIBUTE, USER_ADMINISTRATIVE_STATUS));

    }

    @Test(groups = { "lab_4_1" })
    public void test002addProjectionToUserKirk() {
        ListUsersPage users = basicPage.listUsers();
        users
                .table()
                .search()
                .byName()
                .inputValue(USER_NAME)
                .updateSearch()
                .and()
                .clickByName(USER_NAME)
                .selectTabProjections()
                .clickAddProjection()
                .table()
                .selectCheckboxByName(ImportResourceTest.RESOURCE_NAME)
                .and()
                .clickAdd()
                .and()
                .clickSave()
                .feedback()
                .isSuccess();

    }

}
