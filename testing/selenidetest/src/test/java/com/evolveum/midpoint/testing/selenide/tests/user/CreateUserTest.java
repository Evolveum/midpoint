package com.evolveum.midpoint.testing.selenide.tests.user;

import com.evolveum.midpoint.testing.selenide.tests.BaseTest;
import com.evolveum.midpoint.testing.selenide.tests.Util;
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
public class CreateUserTest extends BaseTest {

    @Autowired
    UserUtil userUtil;

    @Autowired
    Util util;

    /**
     * Create user test.
     */
    @Test
    public void createUserTest(){
        //create user
        userUtil.createUser(userUtil.getSimpleTestUserName());
        //search for the created in users list
        util.searchForElement(userUtil.getSimpleTestUserName(), "/html/body/div[4]/div/div[4]/form/span/a");
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr/td[3]/div/a/span"))
                .shouldBe(visible).click();
    }

    /**
     * Create user with assigned Superuser role.
     */
    @Test
    public void createSuperUserTest(){
        //create user
        userUtil.createUser(userUtil.getSuperUserName());
        //search for the created in users list
        util.searchForElement(userUtil.getSuperUserName(), "/html/body/div[4]/div/div[4]/form/span/a");
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr/td[3]/div/a/span"))
                .shouldBe(visible).click();
        //click on the menu icon next to Assignments section
        $(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[2]/div[1]/div[2]/ul/li/a")).shouldBe(visible).click();
        //click Assign role menu item
        $(By.linkText("Assign role")).shouldBe(visible).click();


    }


}
