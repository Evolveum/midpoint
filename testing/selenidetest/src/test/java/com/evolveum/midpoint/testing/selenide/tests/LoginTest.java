 package com.evolveum.midpoint.testing.selenide.tests;

import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.Condition.*;

/**
 * Created by Kate on 07.08.2015.
 */
@Component
public class LoginTest extends BaseTest{

    @Autowired
    Util util;

    @Autowired
    BaseTest baseTest;

    /**
     * Log in to system as administrator/5ecr3t
     */
    @Test
    public void loginWithCorrectCredentialsTest(){
        open(util.SITE_URL);
        //perform login
        login(util.SITE_URL, util.ADMIN_LOGIN, util.ADMIN_PASSWORD);

        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small")).shouldHave(text("welcome to midPoint"));

        close();
    }

    /**
     * Log in to system with incorrect username
     */
    @Test
    public void loginWithIncorrectUsernameTest(){
        open(util.SITE_URL);
        //perform login
        login(util.SITE_URL, "incorrectUserName", util.ADMIN_PASSWORD);

        //check if error message appears
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div/div/span")).shouldHave(text("Invalid username and/or password."));

        close();
    }

    /**
     * Log in to system with incorrect password
     */
    @Test
    public void loginWithIncorrectPasswordTest(){
        open(util.SITE_URL);
        //perform login
        login(util.SITE_URL, util.ADMIN_LOGIN, "incorrectPassword");

        //check if error message appears
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div/div/span")).shouldHave(text("Invalid username and/or password."));

        close();
    }


    /**
     * Log in to MidPoint as administrator
     */
    public void login(){
        //perform login
        login(util.SITE_URL, util.ADMIN_LOGIN, util.ADMIN_PASSWORD);
    }

    public void login(String username, String password){
        //perform login
        login(util.SITE_URL, username, password);
    }


    public void login(String siteUrl, String username, String password) {
        open(siteUrl);
        //enter login value
        $(By.name("username")).shouldBe(visible).setValue(username);
        //enter password value
        $(By.name("password")).shouldBe(visible).setValue(password);
        //click Sign in button
        $(By.xpath("/html/body/div[4]/div/div[3]/div/div/div/form/input")).shouldBe(enabled).click();

    }

}
