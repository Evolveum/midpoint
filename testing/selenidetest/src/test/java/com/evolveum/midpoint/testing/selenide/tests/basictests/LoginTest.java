 package com.evolveum.midpoint.testing.selenide.tests.basictests;

import com.evolveum.midpoint.testing.selenide.tests.AbstractSelenideTest;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;

import java.util.HashMap;

import static com.codeborne.selenide.Selectors.by;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byAttribute;

/**
 * Created by Kate on 07.08.2015.
 */
//@Component
public class LoginTest extends AbstractSelenideTest {

    private static String USER_WITHOUT_PASSWORD = "UserWithoutPassword";
    /**
     * Log in to system as administrator/5ecr3t
     */
    @Test
    public void test001loginWithCorrectCredentialsTest(){
        //perform login
        login(ADMIN_LOGIN, ADMIN_PASSWORD);

        checkLoginIsPerformed();
        close();
    }

    /**
     * Log in to system with incorrect username
     */
    @Test
    public void test002loginWithIncorrectUsernameTest(){
        //perform login
        login("incorrectUserName", ADMIN_PASSWORD);

        //check if error message appears
        $(byText("Invalid username and/or password.")).shouldBe(visible);

        close();
    }

    /**
     * Log in to system without username
     */
    @Test
    public void test003loginWithoutUsernameTest(){
        //perform login
        login("", ADMIN_PASSWORD);

        $(byText("Invalid username and/or password.")).shouldBe(visible);
        close();
    }

    /**
     * Log in to system with incorrect password
     */
    @Test
    public void test004loginWithIncorrectPasswordTest(){
        //perform login
        login(ADMIN_LOGIN, "incorrectPassword");

        //check if error message appears
        $(byText("Invalid username and/or password.")).shouldBe(visible);

        close();
    }

    /**
     * Log in to system without password
     */
    @Test
    public void test005loginWithoutPasswordTest(){
        //perform login
        login(ADMIN_LOGIN, "");

        $(byText("Couldn't authenticate user, reason: couldn't encode password.")).shouldBe(visible);
        close();
    }

    /**
     * Log in to system without password with user who don't have password
     */
    @Test (priority = 0)
    public void test006loginWithoutPasswordWithUserWhoDontHavePasswordTest() {
        close();
        login();

        //create user with filled user name only
        createUser(USER_WITHOUT_PASSWORD, new HashMap<String, String>());

        checkOperationStatusOk("Save (GUI)");

        //search for the created user in users list
        searchForElement(USER_WITHOUT_PASSWORD);
        //click on the found user link
        $(By.linkText(USER_WITHOUT_PASSWORD)).shouldBe(visible).click();

        //assign End user role to user
        assignObjectToFocusObject(ASSIGN_ROLE_LINKTEXT, EndUserTests.ENDUSER_ROLE_NAME);
        checkOperationStatusOk("Save (GUI)");
        logout();
        close();

        //perform login
        login(USER_WITHOUT_PASSWORD, "");

        //check if error message appears
        $(byText("Couldn't authenticate user, reason: couldn't encode password.")).shouldBe(visible);
    }



}
