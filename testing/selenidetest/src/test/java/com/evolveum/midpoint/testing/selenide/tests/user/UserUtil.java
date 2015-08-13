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
    private String simpleTestUserName;
    private String superUserName;

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

        //check if Success message appears after user saving
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).shouldHave(text("Success"));
    }


    public void openListUsersPage(){
        //click Users menu
        $(By.cssSelector("html.no-js body div.navbar.navbar-default.navbar-fixed-top div div.navbar-collapse.collapse ul.nav.navbar-nav li.dropdown a.dropdown-toggle")).shouldHave(text("Users")).click();

        //click New user menu item
        $(By.linkText("List users")).click();

        //check if Users page is opened
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1")).shouldHave(text("Users in midPoint"));

    }
    public void setSimpleTestUserName(String simpleTestUserName) {
        this.simpleTestUserName = simpleTestUserName;
    }

    public String getSimpleTestUserName() {
        return simpleTestUserName;
    }

    public String getSuperUserName() {
        return superUserName;
    }

    public void setSuperUserName(String superUserName) {
        this.superUserName = superUserName;
    }
}
