package com.evolveum.midpoint.testing.selenide.tests.organization;

import com.evolveum.midpoint.testing.selenide.tests.AbstractSelenideTest;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import java.util.HashMap;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.close;

/**
 * Created by Kate on 17.08.2015.
 */
public class OrganizationStructureTests extends AbstractSelenideTest {
    public static final String ORG_FILE_PATH = "../../samples/org/org-monkey-island-simple.xml";
    public static final String ASSIGN_ORG_UNIT_LINKTEXT = "Assign org. unit";
    public static final String USER_NAME = "OrgTestUser";
    public static final String ORG_UNIT_NAME = "F0002";



    /**
     *  Import organization structure from org-monkey-island-simple.xml
     *  sample file. Check if organization tree was created in MP
     */
    @Test(priority = 0)
    public void test001importOrganizationStructureFromFileTest(){
        close();
        login();

        //check if welcome message appears after user logged in
        $(byText("welcome to midPoint")).shouldBe(visible);

        //import organization structure xml file
        importObjectFromFile(ORG_FILE_PATH);

        //click Users menu
        $(By.cssSelector("html.no-js body div.navbar.navbar-default.navbar-fixed-top div div.navbar-collapse.collapse ul.nav.navbar-nav li.dropdown a.dropdown-toggle")).shouldHave(text("Users")).click();

        //click Organization tree menu item
        $(By.linkText("Organization tree")).click();

        //check if organization structure was created in midPoint
        $(By.xpath("/html/body/div[4]/div/div[3]/ul/li[1]")).shouldHave(text("Governor Office"));
        $(By.xpath("/html/body/div[4]/div/div[3]/div/div/div[4]/div[2]/div[2]/div/table/tbody/tr[1]/td/div/div/span/a/span")).shouldHave(text("Governor Office"));
        $(By.xpath("/html/body/div[4]/div/div[3]/ul/li[2]/a")).shouldHave(text("Projects"));
    }

    @Test(priority = 1, dependsOnMethods = {"test001importOrganizationStructureFromFileTest"})
    public void test002assignOrgUnitTest(){
        //create test user
        createUser(USER_NAME, new HashMap<String, String>());
        //open user's Edit page
        openUsersEditPage(USER_NAME);
        //assign F0002 org unit (Ministry of Defense) to the user
        assignObjectToUser(ASSIGN_ORG_UNIT_LINKTEXT, ORG_UNIT_NAME);
        //open user's Edit page
        openUsersEditPage(USER_NAME);
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
        searchForElement(USER_NAME);
        //check if user was found in the organization
        $(By.xpath("/html/body/div[4]/div/div[3]/div/div/div[4]/div[3]/form/div[4]/div[2]/table/tbody/tr/td[3]/div/a/span"))
                .shouldHave(text(USER_NAME));

    }

    @Test(priority = 2, dependsOnMethods = {"test001importOrganizationStructureFromFileTest", "test002assignOrgUnitTest"})
    public void test003unassignOrgUnitTest(){
        //open user's Edit page
        openUsersEditPage(USER_NAME);
        //select checkbox for org. unit in Assignments section
        $(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[2]/div[2]/div/div[1]/div[1]/input"))
                .shouldBe(visible).click();
        //click on the menu icon next to Assignments section
        $(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[2]/div[1]/div[2]/ul/li/a")).shouldBe(visible).click();
        //click Assign menu item with the specified linkText
        $(By.linkText("Unassign")).shouldBe(visible).click();
        //click Yes button in the opened Confirm delete window
        $(By.xpath("/html/body/div[6]/form/div/div[2]/div/div/div/div[2]/div/div/div/div/p[2]/a[1]"))
                .shouldBe(visible).click();
        //click Save button
        $(By.xpath("/html/body/div[4]/div/form/div[6]/a[2]")).shouldHave(text("Save")).click();
        //check if Success message appears after user saving
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).shouldHave(text("Success"));
        //open user's Edit page
        openUsersEditPage(USER_NAME);
        //check if there is no assignments in the Assignments section any more
        assert !($(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[2]/div[2]/div/div[1]/div[1]/input")).exists());
        //click Users menu
        $(By.cssSelector("html.no-js body div.navbar.navbar-default.navbar-fixed-top div div.navbar-collapse.collapse ul.nav.navbar-nav li.dropdown a.dropdown-toggle")).shouldHave(text("Users")).click();
        //click Organization tree menu item
        $(By.linkText("Organization tree")).click();
        //click on Ministry of Defense
        $(By.xpath("/html/body/div[4]/div/div[3]/div/div/div[4]/div[2]/div[2]/div/table/tbody/tr[2]/td/div/div/div/div/span/a/span"))
                .shouldBe(visible).click();
        //search for the user in the opened organization
        searchForElement(USER_NAME);
        //check if user was not found in the organization, No matching result found message is shown
        $(By.xpath("/html/body/div[4]/div/div[3]/div/div/div[4]/div[3]/form/div[4]/div[2]/table/tfoot/tr/td"))
                .shouldHave(text("No matching result found."));

    }



}
