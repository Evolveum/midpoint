package com.evolveum.midpoint.testing.selenide.tests.account;

/**
 * Created by Kate on 09.08.2015.
 */

import com.evolveum.midpoint.testing.selenide.tests.AbstractSelenideTest;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.close;
import static com.codeborne.selenide.Selenide.switchTo;

@Component
public class ResourceUserAccountTests extends AbstractSelenideTest {

    public static final String USER_NAME = "UserWithOpendjAccount";
    public static final String OPENDJ_RESOURCE_NAME = "Localhost OpenDJ (no extension schema) test";
    public static final String OPENDJ_RESOURCE_PATH = "../../samples/resources/opendj/opendj-localhost-resource-sync-no-extension-advanced-test.xml";
    public static final String USER_ADMINISTRATOR_ROLE_NAME = "User Administrator";
    public static final String AUTHORIZATION_ROLES_XML_PATH = "../../samples/roles/authorization-roles.xml";
    public static final String END_USER_ROLE_NAME = "End user";
    public static final String ACCOUNT_SURNAME_VALUE = "sn value";
    public static final String ACCOUNT_SURNAME_FIELD = "Surname";
    public static final String ACCOUNT_PASSWORD_VALUE = "password";
    public static final String ACCOUNT_COMMON_NAME_VALUE = "cm value";
    public static final String ACCOUNT_COMMON_NAME_FIELD = "Common Name";

    /**
     * Import OpenDJ resource test (file "opendj-localhost-resource-sync-no-extension-advanced.xml" is used)
     */
    @Test(priority = 0)
    public void test001importResourceTest(){
        close();
        login();
        checkLoginIsPerformed();
        //import resource xml file
        importObjectFromFile(OPENDJ_RESOURCE_PATH);

        //open Resources -> List Resources
        if (!$(By.partialLinkText("List resources")).isDisplayed())
            $(By.partialLinkText("Resources")).shouldBe(visible).click(); // clicked in previous test
        $(By.partialLinkText("List resources")).click();

        //search for resource in resources list
        searchForElement(OPENDJ_RESOURCE_NAME);
        $(By.partialLinkText(OPENDJ_RESOURCE_NAME)).shouldBe(visible);
    }

    /**
     * Check resource connection on the
     * Resource details page
     */
    @Test(priority = 1, dependsOnMethods = {"test001importResourceTest"})
    public void test002checkResourceConnectionTest(){
        close();
        login();
        checkLoginIsPerformed();
        $(By.partialLinkText("Resources")).shouldBe(visible).click();
        $(By.partialLinkText("List resources")).click();

        //search for resource in resources list
        searchForElement(OPENDJ_RESOURCE_NAME);
        //click on resource link
        $(By.linkText(OPENDJ_RESOURCE_NAME)).click();

        //click Test connection button
        $(By.linkText("Test connection")).shouldBe(visible).click();

        //check if all statuses are succeeded
        $(byText("Test connection result(s)")).shouldBe(visible);
        assertEquals(4, $$(byAttribute("class", "feedback-message box box-solid  box-success")).size());
    }

    /**
     * Create user, then create account for the user with
     * resource imported in the previous test
     */
    @Test (priority = 2, dependsOnMethods = {"test001importResourceTest"})
    public void test003createAccountTest() {
        close();
        login();
        checkLoginIsPerformed();
        //create user with filled user name only
        createUser(USER_NAME, new HashMap<String, String>());

        //open user's Edit page
        openUsersEditPage(USER_NAME);
        openProjectionsTab();
        //click on the menu icon in the Projection section
        $(byAttribute("about", "dropdownMenu")).shouldBe(visible).click();
        //click on the Add projection menu item
        $(By.linkText("Add projection")).shouldBe(visible).click();

        searchForElement(OPENDJ_RESOURCE_NAME);
        $(byAttribute("about", "table")).find(By.tagName("tbody")).find(By.tagName("input")).shouldBe(visible).setSelected(true);
        $(By.linkText("Add")).shouldBe(enabled).click();

        $(By.linkText(OPENDJ_RESOURCE_NAME)).shouldBe(visible).click();

        //Fill in account fields: Common name, Surname, first and second password fields
        Map<String, String> fieldsMap = new HashMap<String, String>();
        fieldsMap.put(ACCOUNT_COMMON_NAME_FIELD, ACCOUNT_COMMON_NAME_VALUE);
        fieldsMap.put(ACCOUNT_SURNAME_FIELD, ACCOUNT_SURNAME_VALUE);
        fieldsMap.put(PASSWORD1_FIELD_NAME, ACCOUNT_PASSWORD_VALUE);
        fieldsMap.put(PASSWORD2_FIELD_NAME, ACCOUNT_PASSWORD_VALUE);
        setFieldValues(fieldsMap);
        //click Save button
        $(By.linkText("Save")).shouldBe(visible).click();
        checkOperationStatusOk("Save (GUI)");

        //search for user in users list
        openUsersEditPage(USER_NAME);
        openProjectionsTab();
        //check if the created account is displayed in the Accounts section
        $(By.linkText(OPENDJ_RESOURCE_NAME)).shouldBe(visible);
    }

    /**
     *  update account attributes (Common Name, Surname),
     *  check if the appropriate user's attributes were
     *  also updated
     */
    @Test (priority = 4, dependsOnMethods = {"test003createAccountTest"})
    public void test004updateAccountAttributesTest(){
        close();
        login();
        checkLoginIsPerformed();
        //open user's Edit page
        openUsersEditPage(USER_NAME);
        openProjectionsTab();

        //click on the account link to expand its fields
        $(By.linkText(OPENDJ_RESOURCE_NAME)).shouldBe(visible).click();

        Map<String, String> fieldsMap = new HashMap<String, String>();
        fieldsMap.put(ACCOUNT_COMMON_NAME_FIELD, ACCOUNT_COMMON_NAME_VALUE + UPDATED_VALUE);
        fieldsMap.put(ACCOUNT_SURNAME_FIELD, ACCOUNT_SURNAME_VALUE + UPDATED_VALUE);
        setFieldValues(fieldsMap);
        //click Save button
        $(By.linkText("Save")).shouldBe(visible).click();
        checkOperationStatusOk("Save (GUI)");
        //search for user in users list
        searchForElement(USER_NAME);

        //check if users attributes were updated
        $(byText(ACCOUNT_SURNAME_VALUE + UPDATED_VALUE)).shouldBe(visible);
        $(byText(ACCOUNT_COMMON_NAME_VALUE + UPDATED_VALUE)).shouldBe(visible);

        //open user's Edit page
        $(By.linkText(USER_NAME)).shouldBe(visible).click();
        openProjectionsTab();
        //click on the account link to expand its fields
        $(By.linkText(OPENDJ_RESOURCE_NAME)).shouldBe(visible).click();
        //check if account's attributes were updated
        checkObjectAttributesValues(fieldsMap);
    }



}
