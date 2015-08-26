 package com.evolveum.midpoint.testing.selenide.tests;

import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selectors.by;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.Condition.*;

/**
 * Created by Kate on 07.08.2015.
 */
@Component
public class LoginTest extends AbstractSelenideTest {
    /**
     * Log in to system as administrator/5ecr3t
     */
    @Test
    public void test001loginWithCorrectCredentialsTest(){
        open(SITE_URL);
        //perform login
        login(SITE_URL, ADMIN_LOGIN, ADMIN_PASSWORD);

        //check if welcome message appears after user logged in
        $(byText("welcome to midPoint")).shouldBe(visible);

        //check if Superuser role is displayed in My Assignments on Dashboard
        $(byText("Superuser"));

        close();
    }

    /**
     * Log in to system with incorrect username
     */
    @Test
    public void test002loginWithIncorrectUsernameTest(){
        open(SITE_URL);
        //perform login
        login(SITE_URL, "incorrectUserName", ADMIN_PASSWORD);

        //check if error message appears
        $(byText("Invalid username and/or password.")).shouldBe(visible);

        close();
    }

    /**
     * Log in to system without username
     */
    @Test
    public void test003loginWithoutUsernameTest(){
        open(SITE_URL);
        //perform login
        login(SITE_URL, "", ADMIN_PASSWORD);

        //check if error message appears
        $(By.className("messages-error")).find(by("title", "Partial error")).shouldBe(visible);

        close();
    }

    /**
     * Log in to system with incorrect password
     */
    @Test
    public void test004loginWithIncorrectPasswordTest(){
        open(SITE_URL);
        //perform login
        login(SITE_URL, ADMIN_LOGIN, "incorrectPassword");

        //check if error message appears
        $(byText("Invalid username and/or password.")).shouldBe(visible);

        close();
    }

    /**
     * Log in to system without password
     */
    @Test
    public void test005loginWithoutPasswordTest(){
        open(SITE_URL);
        //perform login
        login(SITE_URL, ADMIN_LOGIN, "");

        //check if error message appears
        $(By.className("messages-error")).find(by("title", "Partial error")).shouldBe(visible);

        close();
    }



}
