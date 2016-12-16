package com.evolveum.midpoint.testing.selenide.tests.basictests;

import com.evolveum.midpoint.testing.selenide.tests.AbstractSelenideTest;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.close;

/**
 * Created by Kate on 22.08.2015.
 */
public class EndUserTests extends AbstractSelenideTest{
    public static final String ENDUSER_ROLE_NAME = "End user";
    public static final String END_USER_NAME = "EndUser";
    public static final String NEW_PASSWORD_VALUE = "new_password";

    /**
     * Create user with assigned End user role.
     */
    @Test(priority = 0)
    public void test001createEndUserTest() {
        close();
        //log in to midPoint
        login();

        checkLoginIsPerformed();
        //create user, set password fields for him
        Map<String, String> userAttributes = new HashMap<>();
        userAttributes.put(PASSWORD1_FIELD_NAME, PASSWORD1_FIELD_VALUE);
        userAttributes.put(PASSWORD2_FIELD_NAME, PASSWORD2_FIELD_VALUE);
        createUser(END_USER_NAME, userAttributes);
        //search for the created user in users list
        searchForElement(END_USER_NAME);
        //click on the found user link
        $(By.linkText(END_USER_NAME)).shouldBe(visible).click();

        //assign End user role to user
        assignObjectToFocusObject(ASSIGN_ROLE_LINKTEXT, ENDUSER_ROLE_NAME);

        //search for the user in users list
        searchForElement(END_USER_NAME);
        //click on the found user link
        $(By.linkText(END_USER_NAME)).shouldBe(visible).click();

        //check if assigned role is displayed on the Assignments tab
        openAssignmentsTab();
        $(By.linkText(ENDUSER_ROLE_NAME));

    }

    @Test(dependsOnMethods = {"test001createEndUserTest"}, priority = 1)
    public void test002loginAsEnduserTest() {
        close();
        login(END_USER_NAME, PASSWORD1_FIELD_VALUE);
        checkLoginIsPerformed();
        //click to Profile
        $(By.partialLinkText("Profile")).shouldBe(visible).click();
        //check if End user role s displayed in My Assignments section
        openAssignmentsTab();
        $(byText(ENDUSER_ROLE_NAME)).shouldBe(visible);

    }

    @Test(dependsOnMethods = {"test002loginAsEnduserTest"}, priority = 2)
    public void test003changePasswordAndLoginTest() {
        close();
        login(END_USER_NAME, PASSWORD1_FIELD_VALUE);
        checkLoginIsPerformed();
        //select Credentials menu item
        $(byText("Credentials")).shouldBe(visible).click();
        //set new password value
        $(byAttribute("name", "tabPanel:panel:oldPassword")).shouldBe(visible).setValue(PASSWORD1_FIELD_VALUE);
        $(byAttribute("about", "password2")).shouldBe(visible).setValue(NEW_PASSWORD_VALUE);
        $(byAttribute("about", "password1")).shouldBe(visible).setValue(NEW_PASSWORD_VALUE);
        //click Save button
        $(By.linkText("Save")).shouldBe(visible).click();
        //check if Success message appears
        $(byText("Change password (GUI)")).shouldBe(visible);
        //log out
        logout();
        //log in with new password
        login(END_USER_NAME, NEW_PASSWORD_VALUE);
        checkLoginIsPerformed();
        //click to Profile
        $(By.partialLinkText("Profile")).shouldBe(visible).click();
        //check if End user role s displayed in My Assignments section
        openAssignmentsTab();
        $(byText(ENDUSER_ROLE_NAME)).shouldBe(visible);

    }

}
