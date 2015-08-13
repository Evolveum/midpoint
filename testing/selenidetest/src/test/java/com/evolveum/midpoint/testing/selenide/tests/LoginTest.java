 package com.evolveum.midpoint.testing.selenide.tests;

import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
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
        open(util.getSiteUrl());
        //perform login
        login(util.getSiteUrl(), util.getAdminUserLogin(), util.getAdminUserPassword());

        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small")).shouldHave(text("welcome to midPoint"));

        close();
    }

    /**
     * Log in to system with incorrect username
     */
    @Test
    public void loginWithIncorrectUsernameTest(){
        open(util.getSiteUrl());
        //perform login
        login(util.getSiteUrl(), "incorrectUserName", util.getAdminUserPassword());

        //check if error message appears
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div/div/span")).shouldHave(text("Invalid username and/or password."));

        close();
    }

    /**
     * Log in to system with incorrect password
     */
    @Test
    public void loginWithIncorrectPasswordTest(){
        open(util.getSiteUrl());
        //perform login
        login(util.getSiteUrl(), util.getAdminUserLogin(), "incorrectPassword");

        //check if error message appears
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div/div/span")).shouldHave(text("Invalid username and/or password."));

        close();
    }


    /**
     * open browser window with the specified siteUrl
     */
    @Test
    public void loginAndStay(){
        open(util.getSiteUrl());
        //perform login
        login(util.getSiteUrl(), util.getAdminUserLogin(), util.getAdminUserPassword());

        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small")).shouldHave(text("welcome to midPoint"));
    }

    /**
     * Log in to MidPoint as administrator
     */
    public void loginAsAdmin(){
        login(util.getSiteUrl(), util.getAdminUserLogin(), util.getAdminUserPassword());
    }

    public void login(String siteUrl, String username, String password) {
        //enter login value
        $(By.name("username")).shouldBe(visible).setValue(username);
        //enter password value
        $(By.name("password")).shouldBe(visible).setValue(password);
        //click Sign in button
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.row div.col-md-offset-2.col-md-8.col-lg-offset-4.col-lg-4 div.panel.panel-default div.panel-body form#id6.form-horizontal input.btn.btn-primary.pull-right")).shouldBe(enabled).click();
    }

}
