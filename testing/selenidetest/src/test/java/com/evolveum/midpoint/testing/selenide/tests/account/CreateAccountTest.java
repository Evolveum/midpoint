package com.evolveum.midpoint.testing.selenide.tests.account;

/**
 * Created by Kate on 09.08.2015.
 */

import com.evolveum.midpoint.testing.selenide.tests.BaseTest;
import com.evolveum.midpoint.testing.selenide.tests.resource.ImportResourceTest;
import com.evolveum.midpoint.testing.selenide.tests.resource.ResourceUtil;
import com.evolveum.midpoint.testing.selenide.tests.user.UserUtil;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;

@Component
public class CreateAccountTest extends BaseTest{

    @Autowired
    UserUtil userUtil;

    @Autowired
    ResourceUtil resourceUtil;

    @Autowired
    ImportResourceTest importResourceTest;

    @Autowired
    BaseTest baseTest;

    /**
     * Prerequirement: Test user is to be created (see CreateUserTest.createUserTest())
     */
    @Test
    public void createOpendjAccountForUser(){
        //open Users page
        userUtil.openListUsersPage();

        //search for user in users list
        userUtil.searchForUser(userUtil.getTestUserName()).shouldBe(visible).click();

        //click on the menu icon in the Accounts section
        $(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[1]/div/div[2]/ul/li/a")).shouldBe(visible).click();
        //click on the Add account menu item
        $(By.linkText("Add account")).shouldBe(visible).click();

        //search for OpenDJ resource in resources list in the opened Select resource(s) window
        resourceUtil.searchForOpendjResource(resourceUtil.getTestResourceName(), "/html/body/div[6]/form/div/div[2]/div/div/div/div[2]/div/div/div/div/div/div[2]/div/table/tbody/tr/td[2]/div");

        //select check box in the first row for "Localhost OpenDJ (no extension schema)" resource
        $(By.name("resourcePopup:content:table:table:body:rows:4:cells:1:cell:check")).shouldBe(visible).click();
        //click Add resource(s) button
        $(By.linkText("Add resource(s)")).shouldBe(enabled).click();

        //Fill in account fields: Common name, Surname, first and second password fields
        $(By.name("accounts:accountList:0:account:body:containers:0:container:properties:3:property:values:0:value:valueContainer:input:input")).shouldBe(visible).setValue("Common name");
        $(By.name("accounts:accountList:0:account:body:containers:0:container:properties:42:property:values:0:value:valueContainer:input:input")).shouldBe(visible).setValue("Surname");
        $(By.name("accounts:accountList:0:account:body:containers:5:container:properties:0:property:values:0:value:valueContainer:input:password2")).shouldBe(visible).setValue("password");
        $(By.name("accounts:accountList:0:account:body:containers:5:container:properties:0:property:values:0:value:valueContainer:input:password1")).shouldBe(visible).setValue("password");

        //click Save button
        $(By.xpath("/html/body/div[4]/div/form/div[6]/a[2]")).shouldHave(text("Save")).click();

        //check if Success message appears after user saving
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).shouldHave(text("Success"));

        //search for user in users list
        userUtil.searchForUser(userUtil.getTestUserName()).shouldBe(visible).click();

        //check if the created account is displayed in the Accounts section
        $(By.linkText(resourceUtil.getTestResourceName())).shouldBe(visible);

    }


}
