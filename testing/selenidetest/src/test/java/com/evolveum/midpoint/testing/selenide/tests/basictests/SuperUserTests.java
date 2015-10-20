package com.evolveum.midpoint.testing.selenide.tests.basictests;

import com.evolveum.midpoint.testing.selenide.tests.AbstractSelenideTest;
import org.openqa.selenium.By;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;

import java.util.HashMap;

import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.close;

/**
 * Created by Kate on 09.08.2015.
 */
@Component
public class SuperUserTests extends AbstractSelenideTest {
    public static final String SUPER_ROLE_NAME = "Superuser";
    public static final String SUPER_USER_NAME = "SuperUser";
    public static final String ASSIGN_ROLE_LINKTEXT = "Assign Role";

    /**
     * Create user with assigned Superuser role.
     */
    @Test(priority = 0)
    public void test001createSuperUserTest() {
        close();
        //log in to midPoint
        login();
        //check if welcome message appears after user logged in
        $(byText("welcome to midPoint")).shouldBe(visible);
        //create user
        createUser(SUPER_USER_NAME, new HashMap<String, String>());
        //search for the created user in users list
        searchForElement(SUPER_USER_NAME);
        //click on the found user link
        $(By.linkText(SUPER_USER_NAME)).shouldBe(visible).click();

        //click on the menu icon in the User details section
        $(byText("Details")).parent().parent().find(byAttribute("about", "dropdownMenu"))
                .shouldBe(visible).click();
        //click on Show empty fields menu item
        $(By.linkText("Show empty fields")).shouldBe(visible).click();
        //set Password fields with value
        $(By.name(AbstractSelenideTest.PASSWORD1_FIELD_NAME))
                .shouldBe(visible).setValue(PASSWORD1_FIELD_VALUE);
        $(By.name(AbstractSelenideTest.PASSWORD2_FIELD_NAME))
                .shouldBe(visible).setValue(PASSWORD1_FIELD_VALUE);

        //assign Superuser role to user
        assignObjectToUser(ASSIGN_ROLE_LINKTEXT, SUPER_ROLE_NAME);

        //search for the user in users list
        searchForElement(SUPER_USER_NAME);
        //click on the found user link
        $(By.linkText(SUPER_USER_NAME)).shouldBe(visible).click();

        //check if assigned role is displayed in the Assignments section
        $(By.linkText(SUPER_ROLE_NAME)).shouldBe(visible);

        close();
    }

    @Test(dependsOnMethods = {"test001createSuperUserTest"}, priority = 1)
    public void test002loginAsSuperuserTest() {
        close();
        login(SUPER_USER_NAME, PASSWORD1_FIELD_VALUE);
        //check if welcome message appears after user logged in
        $(byText("welcome to midPoint")).shouldBe(visible);

    }

    @Test(dependsOnMethods = {"test001createSuperUserTest"}, priority = 2)
    public void test003disableSuperuserAndLoginTest() {
        close();
        login();
        //check if welcome message appears after user logged in
        $(byText("welcome to midPoint")).shouldBe(visible);
        //open Users list page
        openListUsersPage();
        //search for the super user in users list
        searchForElement(SUPER_USER_NAME);
        //check if super user was found during the search
        $(By.linkText(SUPER_USER_NAME)).shouldBe(visible);
        //select checkbox next to the found user
        $(byAttribute("about", "table")).find(By.tagName("tbody")).find(By.tagName("input")).shouldBe(visible).click();
        //click on the menu icon in the upper right corner of the users list
        $(byAttribute("about", "dropdownMenu")).shouldBe(visible).click();
        //click on Disable menu item
        $(By.linkText("Disable")).shouldBe(visible).click();
        //check if success operation messages are shown
        $(byText("Success")).shouldBe(visible);

        //log out
        logout();

        //try to log in to the system with disabled super user
        login(SUPER_USER_NAME, PASSWORD1_FIELD_VALUE);

        //check if error message is shown for disabled user
        $(byText("User is disabled.")).shouldBe(visible);
    }

    @Test(dependsOnMethods = {"test001createSuperUserTest"}, priority = 3)
    public void test004enableSuperuserAndLoginTest() {
        close();
        login();
        //check if welcome message appears after user logged in
        $(byText("welcome to midPoint")).shouldBe(visible);
        //open Users list page
        openListUsersPage();
        //search for the super user in users list
        searchForElement(SUPER_USER_NAME);
        //check if super user was found during the search
        $(By.linkText(SUPER_USER_NAME)).shouldBe(visible);
        //select checkbox next to the found user
        $(byAttribute("about", "table")).find(By.tagName("tbody")).find(By.tagName("input")).shouldBe(visible).click();
        //click on the menu icon in the upper right corner of the users list
        $(byAttribute("about", "dropdownMenu")).shouldBe(visible).click();
        //click on Disable menu item
        $(By.linkText("Enable")).shouldBe(visible).click();
        //check if success operation messages are shown
        $(byText("Success")).shouldBe(visible);

        //log out
        logout();

        //log in to the system after super user was enabled
        login(SUPER_USER_NAME, PASSWORD1_FIELD_VALUE);

        //check if welcome message appears after user logged in
        $(byText("welcome to midPoint")).shouldBe(visible);
    }

}




