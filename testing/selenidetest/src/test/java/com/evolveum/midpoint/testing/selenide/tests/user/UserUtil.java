package com.evolveum.midpoint.testing.selenide.tests.user;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;
import org.springframework.stereotype.Component;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Kate on 12.08.2015.
 */
@Component
public class UserUtil {
    private String testUserName;

    /**
     * Looks for the user in the user list with userName value
     * @return
     */
    public SelenideElement searchForUser(String userName){
        //search for user in users list
        $(By.name("basicSearch:searchText")).shouldBe(visible).setValue(userName);
        $(By.xpath("/html/body/div[4]/div/div[4]/form/span/a")).shouldHave(text("Search")).click();
        //check if user is found during users search
        return $(By.linkText(userName));
    }

    public void openListUsersPage(){
        //click Users menu
        $(By.cssSelector("html.no-js body div.navbar.navbar-default.navbar-fixed-top div div.navbar-collapse.collapse ul.nav.navbar-nav li.dropdown a.dropdown-toggle")).shouldHave(text("Users")).click();

        //click New user menu item
        $(By.linkText("List users")).click();

        //check if Users page is opened
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1")).shouldHave(text("Users in midPoint"));

    }
    public void setTestUserName(String testUserName) {
        this.testUserName = testUserName;
    }

    public String getTestUserName() {
        return testUserName;
    }
}
