package com.evolveum.midpoint.testing.selenide.tests;

import org.openqa.selenium.By;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.close;

/**
 * Created by Kate on 19.08.2015.
 */
public class ObjectTemplateTests extends AbstractSelenideTest {
    public static final String OBJECT_TEMPLATE_FILE_PATH = "../../samples/objects/object-template-default.xml";
    public static final String USER_NAME_VALUE = "UserTemplateTest";
    public static final String USER_GIVEN_NAME_VALUE = "TestGivenName";
    public static final String USER_FAMILY_NAME_VALUE = "TestFamilyName";

    @Test(priority = 0)
    public void test001supplyUserAttributesByObjectTemplateTest(){
        close();
        login();
        checkLoginIsPerformed();
        //import object template from file object-template-default.xml
        importObjectFromFile(OBJECT_TEMPLATE_FILE_PATH);
        //update SystemConfiguration, insert reference on the default template
        editObjectPolicy("UserType", "Default User Template 3", new ArrayList<String>());
        //create map with field names which are to be filled in and their values
        Map<String, String> userAttributes = new HashMap<>();
        userAttributes.put(GIVEN_NAME_FIELD_NAME, USER_GIVEN_NAME_VALUE);
        userAttributes.put(FAMILY_NAME_FIELD_NAME, USER_FAMILY_NAME_VALUE);
        //create user with Name, Given Name and Family Name attributes
        createUser(USER_NAME_VALUE, userAttributes);
        //check if Full name and Nickname attributes are filled with values
        openUsersEditPage(USER_NAME_VALUE);
        //check if Full Name and Nickname fields were filled in according to user template rules
        Map<String, String> userAttributesToCheck = new HashMap<>();
        userAttributesToCheck.put(FULL_NAME_FIELD_NAME, USER_GIVEN_NAME_VALUE + " " + USER_FAMILY_NAME_VALUE);
        userAttributesToCheck.put(NICKNAME_FIELD_NAME, "nick_" + USER_GIVEN_NAME_VALUE);
        checkObjectAttributesValues(userAttributesToCheck);
    }

}
