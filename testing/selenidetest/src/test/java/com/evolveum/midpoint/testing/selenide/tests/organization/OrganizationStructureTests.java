package com.evolveum.midpoint.testing.selenide.tests.organization;

import com.evolveum.midpoint.testing.selenide.tests.BaseTest;
import com.evolveum.midpoint.testing.selenide.tests.LoginTest;
import com.evolveum.midpoint.testing.selenide.tests.Util;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.close;

/**
 * Created by Kate on 17.08.2015.
 */
public class OrganizationStructureTests extends BaseTest{
    @Autowired
    LoginTest loginTest;

    @Autowired
    Util util;

    public static final String ORG_FILE_PATH = "../../samples/org/org-monkey-island-simple.xml";

    /**
     *  Import organization structure from org-monkey-island-simple.xml
     *  sample file. Check if organization tree was created in MP
     */
    @Test
    public void importOrganizationStructureFromFile(){
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


}
