package com.evolveum.midpoint.testing.schrodinger.labs;

import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.testing.schrodinger.TestBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar
 * covers LAB 4-1
 */
public class BasicProvisioningTest  extends TestBase {

    private static final String USER_NAME_ATTRIBUTE = "Name";
    private static final String USER_GIVEN_NAME_ATTRIBUTE = "Given name";
    private static final String USER_FAMILY_NAME_ATTRIBUTE = "Family name";
    private static final String USER_PASSWORD_ATTRIBUTE = "Value";
    private static final String USER_ADMINISTRATIVE_STATUS_ATTRIBUTE = "Administrative status";

    private static final String USER_NAME = "kirk";
    private static final String USER_GIVEN_NAME = "Jim";
    private static final String USER_FAMILY_NAME = "Kirk";
    private static final String USER_PASSWORD = "abc123";
    private static final String USER_ADMINISTRATIVE_STATUS = "enabled";

    //todo dependsOnGroup
    @Test(groups={"lab_4_1"})
    public void test001createUserKirk(){
        //we use New user link in this test
        UserPage userPage = basicPage.newUser();
        userPage
                .selectTabBasic()
                    .form()
                        .addAttributeValue(UserType.F_NAME, USER_NAME)
                        .addAttributeValue(UserType.F_FAMILY_NAME, USER_FAMILY_NAME)
                        .addAttributeValue(UserType.F_GIVEN_NAME, USER_GIVEN_NAME)
                //TODO set password and enable status
//                        .addAttributeValue(ActivationType.F_ADMINISTRATIVE_STATUS, USER_ADMINISTRATIVE_STATUS)
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
                .compareAttibuteValue(USER_NAME_ATTRIBUTE, USER_NAME));

        //check given name attribute value
        Assert.assertTrue(userPage
                .selectTabBasic()
                .form()
                .compareAttibuteValue(USER_GIVEN_NAME_ATTRIBUTE, USER_GIVEN_NAME));

        //check family name attribute value
        Assert.assertTrue(userPage
                .selectTabBasic()
                .form()
                .compareAttibuteValue(USER_FAMILY_NAME_ATTRIBUTE, USER_FAMILY_NAME));

        //TODO check status, and password is set
        //check administrative status attribute value
//        Assert.assertTrue(userPage
//                .selectTabBasic()
//                .form()
//                .compareAttibuteValue(Schrodinger.qnameToString(ActivationType.F_ADMINISTRATIVE_STATUS), USER_ADMINISTRATIVE_STATUS));

    }
}
