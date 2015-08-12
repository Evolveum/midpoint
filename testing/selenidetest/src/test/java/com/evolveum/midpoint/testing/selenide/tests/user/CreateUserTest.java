package com.evolveum.midpoint.testing.selenide.tests.user;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.testing.selenide.tests.BaseTest;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Condition.*;

/**
 * Created by Kate on 09.08.2015.
 */
@Component
public class CreateUserTest extends BaseTest{

    @Autowired
    BaseTest baseTest;

    @Autowired
    UserUtil userUtil;

    /**
     * Create user test. The name of the user is specified in the testng.xml, "test.user.name" parameter
     */
    @Test
    public void createUserTest(){
        //click Users menu
        $(By.cssSelector("html.no-js body div.navbar.navbar-default.navbar-fixed-top div div.navbar-collapse.collapse ul.nav.navbar-nav li.dropdown a.dropdown-toggle")).shouldHave(text("Users")).click();

        //click New user menu item
        $(By.linkText("New user")).click();

        //set value to Name field
        $(By.name("userForm:body:containers:0:container:properties:0:property:values:0:value:valueContainer:input:input")).shouldBe(visible).setValue(userUtil.getTestUserName());

        //click Save button
        $(By.xpath("/html/body/div[4]/div/form/div[5]/a[2]")).shouldHave(text("Save")).click();

        //check if Success message appears after user saving

        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).shouldHave(text("Success"));

        //search for the created in users list
        userUtil.searchForUser(userUtil.getTestUserName()).shouldBe(visible);
    }


}
