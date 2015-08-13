package com.evolveum.midpoint.testing.selenide.tests;

import org.openqa.selenium.By;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

/**
 * Created by Kate on 09.08.2015.
 */
@ContextConfiguration(locations = {"classpath:spring-module.xml"})
public class BaseTest extends AbstractTestNGSpringContextTests {
    private static final String PARAM_SITE_URL = "site.url";
    private static final String PARAM_USER_LOGIN = "user.login";
    private static final String PARAM_USER_PASSWORD = "user.password";

    public String siteUrl;
    public String userLogin;
    public String userPassword;


    public BaseTest(){
        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
    }

    @BeforeClass(alwaysRun = true)
    public void beforeClass(ITestContext context) {
        siteUrl = context.getCurrentXmlTest().getParameter(PARAM_SITE_URL);
        userLogin = context.getCurrentXmlTest().getParameter(PARAM_USER_LOGIN);
        userPassword = context.getCurrentXmlTest().getParameter(PARAM_USER_PASSWORD);
    }

    public void login(String siteUrl, String username, String password) {
        //opens login page
        open(siteUrl);
        //enter login value
        $(By.name("username")).shouldBe(visible).setValue(username);
        //enter password value
        $(By.name("password")).shouldBe(visible).setValue(password);
        //click Sign in button
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.row div.col-md-offset-2.col-md-8.col-lg-offset-4.col-lg-4 div.panel.panel-default div.panel-body form#id6.form-horizontal input.btn.btn-primary.pull-right")).shouldBe(enabled).click();
    }


}
