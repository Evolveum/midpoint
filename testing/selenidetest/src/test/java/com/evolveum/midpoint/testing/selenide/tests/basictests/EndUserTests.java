package com.evolveum.midpoint.testing.selenide.tests.basictests;

import com.evolveum.midpoint.testing.selenide.tests.AbstractSelenideTest;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.close;

/**
 * Created by Kate on 22.08.2015.
 */
public class EndUserTests extends AbstractSelenideTest{
    public static final String ENDUSER_ROLE_NAME = "End user";
    public static final String END_USER_NAME = "EndUser";
    public static final String NEW_PASSWORD_VALUE = "new_password";

    /**
     * Create user with assigned End user role.
     */
    @Test(priority = 0)
    public void test001createEndUserTest() {
        close();
        //log in to midPoint
        login();
        //check if welcome message appears after user logged in
        $(byText("welcome to midPoint")).shouldBe(visible);
        //create user, set password fields for him
        Map<String, String> userAttributes = new HashMap<>();
        userAttributes.put(PASSWORD1_FIELD_NAME, PASSWORD1_FIELD_VALUE);
        userAttributes.put(PASSWORD2_FIELD_NAME, PASSWORD2_FIELD_VALUE);
        createUser(END_USER_NAME, userAttributes);
        //search for the created user in users list
        searchForElement(END_USER_NAME);
        //click on the found user link
        $(By.linkText(END_USER_NAME)).shouldBe(visible).click();

        //assign End user role to user
        assignObjectToUser(ASSIGN_ROLE_LINKTEXT, ENDUSER_ROLE_NAME);

        //search for the user in users list
        searchForElement(END_USER_NAME);
        //click on the found user link
        $(By.linkText(END_USER_NAME)).shouldBe(visible).click();

        //check if assigned role is displayed in the Assignments section
        $(By.linkText(ENDUSER_ROLE_NAME));

    }

    @Test(dependsOnMethods = {"test001createEndUserTest"}, priority = 1)
    public void test002loginAsEnduserTest() {
        close();
        login(END_USER_NAME, PASSWORD1_FIELD_VALUE);
        //check if welcome message appears after user logged in
        $(byText("welcome to midPoint")).shouldBe(visible);
        //check if End user role s displayed in My Assignments section
        $(byText(ENDUSER_ROLE_NAME)).shouldBe(visible);

    }

    @Test(dependsOnMethods = {"test002loginAsEnduserTest"}, priority = 2)
    public void test003changePasswordAndLoginTest() {
        //click user's name in the upper right corner
        $(By.className("caret")).shouldBe(visible).click();
        //select Reset passwords menu item
        $(byText("Reset passwords")).shouldBe(visible).click();
        //set new password value
        $(byAttribute("about", "password2")).shouldBe(visible).setValue(NEW_PASSWORD_VALUE);
        $(byAttribute("about", "password1")).shouldBe(visible).setValue(NEW_PASSWORD_VALUE);
        //select MidPoint account
        $(By.name("accounts:table:body:rows:1:cells:1:cell:check")).shouldBe(visible).click();
        //click Save button
        $(By.linkText("Save")).shouldBe(visible).click();
        //check if Success message appears
        $(byText("Success")).shouldBe(visible);
        //log out
        logout(END_USER_NAME);
        //log in with new password
        login(END_USER_NAME, NEW_PASSWORD_VALUE);
        //check if welcome message appears after user logged in
        $(byText("welcome to midPoint")).shouldBe(visible);
        //check if End user role s displayed in My Assignments section
        $(byText(ENDUSER_ROLE_NAME)).shouldBe(visible);

    }

}
