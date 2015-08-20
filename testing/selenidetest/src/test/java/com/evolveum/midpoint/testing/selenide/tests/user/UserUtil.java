package com.evolveum.midpoint.testing.selenide.tests.user;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.testing.selenide.tests.Util;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Kate on 12.08.2015.
 */
@Component
public class UserUtil {

    @Autowired
    Util util;

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

    /**
     * Open Users -> List users
     */
    public void openListUsersPage(){
        //click Users menu
        $(By.cssSelector("html.no-js body div.navbar.navbar-default.navbar-fixed-top div div.navbar-collapse.collapse ul.nav.navbar-nav li.dropdown a.dropdown-toggle")).shouldHave(text("Users")).click();

        //click List users menu item
        $(By.linkText("List users")).click();

        //check if Users page is opened
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1")).shouldHave(text("Users in midPoint"));

    }

    /**
     * Prerequirement: user's Edit page is to be opened
     * Assign the specified roleName role to user
     * @param linkText          the text of the menu item from the Assignments section menu
     * @param objectName        the name of the object to be assigned
     * @param searchButtonXpath xpath of the Search button from Search panel
     * @param checkboxXpath     xpath of the checkbox which is to be selected
     */
    public void assignObjectToUser(String linkText, String objectName, String searchButtonXpath, String checkboxXpath){
        //click on the menu icon next to Assignments section
        $(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[2]/div[1]/div[2]/ul/li/a")).shouldBe(visible).click();
        //click Assign menu item with the specified linkText
        $(By.linkText(linkText)).shouldBe(visible).click();
        //search for object by objectName in the opened Select object(s) window
        util.searchForElement(objectName, searchButtonXpath);
        //select checkbox for the found object
        $(By.xpath(checkboxXpath)).shouldBe(visible).click();
        //click Assign button
        $(By.xpath("/html/body/div[6]/form/div/div[2]/div/div/div/div[2]/div/div/div/div/div/p/a"))
                .shouldBe(visible).click();

        //click Save button
        $(By.xpath("/html/body/div[4]/div/form/div[6]/a[2]")).shouldHave(text("Save")).click();

        //check if Success message appears after user saving
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).shouldHave(text("Success"));
    }

    /**
     * opens Edit page for the specified user with userName
     * @param userName
     */
    public void openUsersEditPage(String userName){
        //open Users page
        openListUsersPage();

        //search for user in users list
        util.searchForElement(userName, "/html/body/div[4]/div/div[4]/form/span/a");
        //click on the found user link
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr/td[3]/div/a/span"))
                .shouldBe(visible).click();

    }

}
