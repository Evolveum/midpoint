package com.evolveum.midpoint.testing.selenide.tests.basictests;

import com.evolveum.midpoint.testing.selenide.tests.AbstractSelenideTest;
import org.openqa.selenium.By;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * Create user with assigned Superuser role.
     */
    @Test(priority = 0)
    public void test001createSuperUserTest() {
        close();
        login();
        checkLoginIsPerformed();
        Map<String, String> userAttributes = new HashMap<String, String>();
        userAttributes.put(PASSWORD1_FIELD_NAME, PASSWORD1_FIELD_VALUE);
        userAttributes.put(PASSWORD2_FIELD_NAME, PASSWORD2_FIELD_VALUE);
        createUser(SUPER_USER_NAME, userAttributes);
        //open user's Edit page
        openUsersEditPage(SUPER_USER_NAME);

        //assign Superuser role to user
        assignObjectToFocusObject(ASSIGN_ROLE_LINKTEXT, SUPER_ROLE_NAME);

        openUsersEditPage(SUPER_USER_NAME);
        openAssignmentsTab();
        //check if assigned role is displayed in the Assignments section
        $(By.linkText(SUPER_ROLE_NAME)).shouldBe(visible);

        close();
    }

    @Test(dependsOnMethods = {"test001createSuperUserTest"}, priority = 1)
    public void test002loginAsSuperuserTest() {
        close();
        login(SUPER_USER_NAME, PASSWORD1_FIELD_VALUE);
        checkLoginIsPerformed();

    }

    @Test(dependsOnMethods = {"test001createSuperUserTest"}, priority = 2)
    public void test003disableSuperuserAndLoginTest() {
        close();
        login();
        checkLoginIsPerformed();
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
        $(byText("Disable users (Gui)")).shouldBe(visible);

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
        checkLoginIsPerformed();
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
        $(byText("Enable users (Gui)")).shouldBe(visible);

        //log out
        logout();

        //log in to the system after super user was enabled
        login(SUPER_USER_NAME, PASSWORD1_FIELD_VALUE);

        checkLoginIsPerformed();
    }

}




