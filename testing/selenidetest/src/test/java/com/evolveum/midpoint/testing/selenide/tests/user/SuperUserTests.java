package com.evolveum.midpoint.testing.selenide.tests.user;

import com.evolveum.midpoint.testing.selenide.tests.BaseTest;
import com.evolveum.midpoint.testing.selenide.tests.LoginTest;
import com.evolveum.midpoint.testing.selenide.tests.Util;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.close;
import static com.codeborne.selenide.Selenide.title;

/**
 * Created by Kate on 09.08.2015.
 */
@Component
public class SuperUserTests extends BaseTest {

    @Autowired
    UserUtil userUtil;

    @Autowired
    LoginTest loginTest;

    @Autowired
    Util util;

    public static final String SUPER_ROLE_NAME = "Superuser";
    public static final String USER_PASSWORD = "password";
    public static final String SUPER_USER_NAME = "SuperUser";
    public static final String ASSIGN_ROLE_LINKTEXT = "Assign role";

    /**
     * Create user with assigned Superuser role.
     */
    @Test(priority = 0)
    public void createSuperUserTest() {
        close();
        //log in to midPoint
        loginTest.login();
        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small"))
                .shouldHave(text("welcome to midPoint"));
        //create user
        userUtil.createUser(SUPER_USER_NAME);
        //search for the created user in users list
        util.searchForElement(SUPER_USER_NAME, "/html/body/div[4]/div/div[4]/form/span/a");
        //click on the found user link
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr/td[3]/div/a/span"))
                .shouldBe(visible).click();

        //click on the menu icon in the User details section
        $(By.xpath("/html/body/div[4]/div/form/div[3]/div[1]/div/div/div/div[2]/div[2]/ul/li/a")).shouldBe(visible).click();
        //click on Show empty fields menu item
        $(By.linkText("Show empty fields")).shouldBe(visible).click();
        //set Password fields with value
        $(By.name("userForm:body:containers:7:container:properties:0:property:values:0:value:valueContainer:input:password2"))
                .shouldBe(visible).setValue(USER_PASSWORD);
        $(By.name("userForm:body:containers:7:container:properties:0:property:values:0:value:valueContainer:input:password1"))
                .shouldBe(visible).setValue(USER_PASSWORD);

        //assign Superuser role to user
        userUtil.assignObjectToUser(ASSIGN_ROLE_LINKTEXT, SUPER_ROLE_NAME,
                "/html/body/div[6]/form/div/div[2]/div/div/div/div[2]/div/div/div/div/div/div[1]/form[2]/span/a",
                "/html/body/div[6]/form/div/div[2]/div/div/div/div[2]/div/div/div/div/div/div[2]/div/table/tbody/tr/td[1]/div/input");

        //search for the user in users list
        util.searchForElement(SUPER_USER_NAME, "/html/body/div[4]/div/div[4]/form/span/a");
        //click on the found user link
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr/td[3]/div/a/span"))
                .shouldBe(visible).click();

        //check if assigned role is displayed in the Assignments section
        $(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[2]/div[2]/div/div[1]/div[1]/a/span")).shouldBe(visible)
                .shouldHave(text("Superuser"));

        close();
    }

    @Test(dependsOnMethods = {"createSuperUserTest"}, priority = 1)
    public void loginAsSuperuserTest() {
        close();
        loginTest.login(SUPER_USER_NAME, USER_PASSWORD);
        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small"))
                .shouldHave(text("welcome to midPoint"));

    }

    @Test(dependsOnMethods = {"createSuperUserTest"}, priority = 2)
    public void disableSuperuserAndLoginTest() {
        close();
        loginTest.login();
        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small"))
                .shouldHave(text("welcome to midPoint"));
        //open Users list page
        userUtil.openListUsersPage();
        //search for the super user in users list
        util.searchForElement(SUPER_USER_NAME, "/html/body/div[4]/div/div[4]/form/span/a");
        //check if super user was found during the search
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr[1]/td[3]/div/a/span"))
                .shouldBe(visible).shouldHave(text(SUPER_USER_NAME));
        //select checkbox next to the found user
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr[1]/td[1]/div/input"))
                .shouldBe(visible).click();
        //click on the menu icon in the upper right corner of the users list
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/thead/tr/th[9]/div/span[1]/ul/li/a")).shouldBe(visible).click();
        //click on Disable menu item
        $(By.linkText("Disable")).shouldBe(visible).click();
        //check if success operation messages are shown
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span"))
                .shouldBe(visible).shouldHave(text("Success"));
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[2]/ul/li/div/span"))
                .shouldBe(visible).shouldHave(text("Disable users (Gui)"));

        //log out
        util.logout();

        //try to log in to the system with disabled super user
        loginTest.login(SUPER_USER_NAME, USER_PASSWORD);

        //check if error message is shown for disabled user
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div/div/span"))
                .shouldBe(visible).shouldHave(text("User is disabled."));
    }

    @Test(dependsOnMethods = {"createSuperUserTest"}, priority = 3)
    public void enableSuperuserAndLoginTest() {
        close();
        loginTest.login();
        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small"))
                .shouldHave(text("welcome to midPoint"));
        //open Users list page
        userUtil.openListUsersPage();
        //search for the super user in users list
        util.searchForElement(SUPER_USER_NAME, "/html/body/div[4]/div/div[4]/form/span/a");
        //check if super user was found during the search
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr[1]/td[3]/div/a/span"))
                .shouldBe(visible).shouldHave(text(SUPER_USER_NAME));
        //select checkbox next to the found user
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr[1]/td[1]/div/input"))
                .shouldBe(visible).click();
        //click on the menu icon in the upper right corner of the users list
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/thead/tr/th[9]/div/span[1]/ul/li/a")).shouldBe(visible).click();
        //click on Disable menu item
        $(By.linkText("Enable")).shouldBe(visible).click();
        //check if success operation messages are shown
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span"))
                .shouldBe(visible).shouldHave(text("Success"));
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[2]/ul/li/div/span"))
                .shouldBe(visible).shouldHave(text("Enable users (Gui)"));

        //log out
        util.logout();

        //log in to the system after super user was enabled
        loginTest.login(SUPER_USER_NAME, USER_PASSWORD);

        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small"))
                .shouldHave(text("welcome to midPoint"));
    }

}




