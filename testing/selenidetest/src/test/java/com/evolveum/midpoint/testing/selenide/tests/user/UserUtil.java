package com.evolveum.midpoint.testing.selenide.tests.user;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;
import org.springframework.stereotype.Component;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Kate on 12.08.2015.
 */
@Component
public class UserUtil {

    /**
     * Creates user with userName value
     * @param userName
     */
    public void createUser(String userName){
        //click Users menu
        $(By.cssSelector("html.no-js body div.navbar.navbar-default.navbar-fixed-top div div.navbar-collapse.collapse ul.nav.navbar-nav li.dropdown a.dropdown-toggle")).shouldHave(text("Users")).click();

        //click New user menu item
        $(By.linkText("New user")).click();

        //set value to Name field
        $(By.name("userForm:body:containers:0:container:properties:0:property:values:0:value:valueContainer:input:input")).shouldBe(visible).setValue(userName);

        //click Save button
        $(By.xpath("/html/body/div[4]/div/form/div[5]/a[2]")).shouldHave(text("Save")).click();

    }


    public void openListUsersPage(){
        //click Users menu
        $(By.cssSelector("html.no-js body div.navbar.navbar-default.navbar-fixed-top div div.navbar-collapse.collapse ul.nav.navbar-nav li.dropdown a.dropdown-toggle")).shouldHave(text("Users")).click();

        //click New user menu item
        $(By.linkText("List users")).click();

        //check if Users page is opened
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1")).shouldHave(text("Users in midPoint"));

    }

}
