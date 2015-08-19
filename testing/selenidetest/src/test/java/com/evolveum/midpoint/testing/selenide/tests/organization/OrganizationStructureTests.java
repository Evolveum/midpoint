package com.evolveum.midpoint.testing.selenide.tests.organization;

import com.evolveum.midpoint.testing.selenide.tests.BaseTest;
import com.evolveum.midpoint.testing.selenide.tests.LoginTest;
import com.evolveum.midpoint.testing.selenide.tests.Util;
import com.evolveum.midpoint.testing.selenide.tests.user.UserUtil;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.close;

/**
 * Created by Kate on 17.08.2015.
 */
public class OrganizationStructureTests extends BaseTest{
    @Autowired
    LoginTest loginTest;

    @Autowired
    UserUtil userUtil;

    @Autowired
    Util util;

    public static final String ORG_FILE_PATH = "../../samples/org/org-monkey-island-simple.xml";
    public static final String ASSIGN_ORG_UNIT_LINKTEXT = "Assign org. unit";
    public static final String USER_NAME = "OrgTestUser";
    public static final String ORG_UNIT_NAME = "F0002";

    /**
     *  Import organization structure from org-monkey-island-simple.xml
     *  sample file. Check if organization tree was created in MP
     */
    @Test(priority = 0)
    public void importOrganizationStructureFromFileTest(){
        close();
        loginTest.login();

        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small"))
                .shouldHave(text("welcome to midPoint"));

        //import organization structure xml file
        util.importObjectFromFile(ORG_FILE_PATH);

        //click Users menu
        $(By.cssSelector("html.no-js body div.navbar.navbar-default.navbar-fixed-top div div.navbar-collapse.collapse ul.nav.navbar-nav li.dropdown a.dropdown-toggle")).shouldHave(text("Users")).click();

        //click Organization tree menu item
        $(By.linkText("Organization tree")).click();

        //check if organization structure was created in midPoint
        $(By.xpath("/html/body/div[4]/div/div[3]/ul/li[1]")).shouldHave(text("Governor Office"));
        $(By.xpath("/html/body/div[4]/div/div[3]/div/div/div[4]/div[2]/div[2]/div/table/tbody/tr[1]/td/div/div/span/a/span")).shouldHave(text("Governor Office"));
        $(By.xpath("/html/body/div[4]/div/div[3]/ul/li[2]/a")).shouldHave(text("Projects"));
    }

    @Test(priority = 1, dependsOnMethods = {"importOrganizationStructureFromFileTest"})
    public void assignOrgUnit(){
        //create test user
        userUtil.createUser(USER_NAME);
        //open user's Edit page
        userUtil.openUsersEditPage(USER_NAME);
        //assign F0002 org unit (Ministry of Defense) to the user
        userUtil.assignObjectToUser(ASSIGN_ORG_UNIT_LINKTEXT, ORG_UNIT_NAME,
                "/html/body/div[6]/form/div/div[2]/div/div/div/div[2]/div/div/div/div/div/div/div/div/div/div/div[1]/form/span/a",
                "/html/body/div[6]/form/div/div[2]/div/div/div/div[2]/div/div/div/div/div/div/div/div/div/div/div[3]/form/div[2]/div[2]/table/tbody/tr/td[1]/div/input");
        //open user's Edit page
        userUtil.openUsersEditPage(USER_NAME);
        //check if assigned org. unit is displayed in the Assignments section
        $(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[2]/div[2]/div/div[1]/div[1]/a/span")).shouldBe(visible)
                .shouldHave(text(" F0002, Ministry of Defense"));
        //click Users menu
        $(By.cssSelector("html.no-js body div.navbar.navbar-default.navbar-fixed-top div div.navbar-collapse.collapse ul.nav.navbar-nav li.dropdown a.dropdown-toggle")).shouldHave(text("Users")).click();
        //click Organization tree menu item
        $(By.linkText("Organization tree")).click();
        //click on Ministry of Defense
        $(By.xpath("/html/body/div[4]/div/div[3]/div/div/div[4]/div[2]/div[2]/div/table/tbody/tr[2]/td/div/div/div/div/span/a/span"))
                .shouldBe(visible).click();
        //search for the user in the opened organization
        util.searchForElement(USER_NAME, "/html/body/div[4]/div/div[3]/div/div/div[4]/div[1]/form/span/a");
        //check if user was found in the organization
        $(By.xpath("/html/body/div[4]/div/div[3]/div/div/div[4]/div[3]/form/div[4]/div[2]/table/tbody/tr/td[3]/div/a/span"))
                .shouldHave(text(USER_NAME));

    }


}
