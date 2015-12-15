package com.evolveum.midpoint.testing.selenide.tests.account;

/**
 * Created by Kate on 09.08.2015.
 */

import com.evolveum.midpoint.testing.selenide.tests.AbstractSelenideTest;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;

import java.util.HashMap;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.close;
import static com.codeborne.selenide.Selenide.switchTo;

@Component
public class ResourceUserAccountTests extends AbstractSelenideTest {

    public static final String USER_NAME = "UserWithOpendjAccount";
    public static final String OPENDJ_RESOURCE_NAME = "Localhost OpenDJ (no extension schema)";
    public static final String OPENDJ_RESOURCE_PATH = "../../samples/resources/opendj/opendj-localhost-resource-sync-no-extension-advanced.xml";
    public static final String USER_ADMINISTRATOR_ROLE_NAME = "User Administrator";
    public static final String AUTHORIZATION_ROLES_XML_PATH = "../../samples/roles/authorization-roles.xml";
    public static final String END_USER_ROLE_NAME = "End user";
    public static final String ACCOUNT_SURNAME_VALUE = "Surname";
    public static final String ACCOUNT_PASSWORD_VALUE = "Common name";
    public static final String ACCOUNT_COMMON_NAME_VALUE = "Common name";

    /**
     * Import OpenDJ resource test (file "opendj-localhost-resource-sync-no-extension-advanced.xml" is used)
     */
    @Test(priority = 0)
    public void test001importResourceTest(){
        close();
        login();

        //check if welcome message appears after user logged in
        $(byText("welcome to midPoint")).shouldBe(visible);

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
        //open Resources -> List Resources
//        $(By.partialLinkText("Resources")).shouldBe(visible).click(); // clicked in previous test
        $(By.partialLinkText("List resources")).click();

        //search for resource in resources list
        searchForElement(OPENDJ_RESOURCE_NAME);
        //click on resource link
        $(By.partialLinkText(OPENDJ_RESOURCE_NAME)).click();

        //click Test connection button
        $(By.linkText("Test connection")).click();

        //check if all statuses are succeeded
        $(By.xpath("/html/body/div/div/section[2]/form/div[2]/div[2]/div/div[2]/table/tbody/tr[1]/td[2]/i")).shouldHave(hasAttribute("title", "Success"));
        $(By.xpath("/html/body/div/div/section[2]/form/div[2]/div[2]/div/div[2]/table/tbody/tr[2]/td[2]/i")).shouldHave(hasAttribute("title", "Success"));
        $(By.xpath("/html/body/div/div/section[2]/form/div[2]/div[2]/div/div[2]/table/tbody/tr[3]/td[2]/i")).shouldHave(hasAttribute("title", "Success"));
        $(By.xpath("/html/body/div/div/section[2]/form/div[2]/div[2]/div/div[2]/table/tbody/tr[4]/td[2]/i")).shouldHave(hasAttribute("title", "Success"));
        $(By.xpath("/html/body/div/div/section[2]/form/div[2]/div[2]/div/div[2]/table/tbody/tr[5]/td[2]/i")).shouldHave(hasAttribute("title", "Success"));

    }

    /**
     * Create user, then create account for the user with
     * resource imported in the previous test
     */
    @Test (priority = 2, dependsOnMethods = {"test001importResourceTest"})
    public void test003createAccountTest() {
        close();
        login();
        //create user with filled user name only
        createUser(USER_NAME, new HashMap<String, String>());

        //open user's Edit page
        openUsersEditPage(USER_NAME);

        //click on the menu icon in the Projection section
        $(By.xpath("/html/body/div[1]/div/section[2]/form/div[4]/div/div/div[7]/div[2]/div[1]/div/div[2]/ul/li/a"))
                .shouldBe(visible).click();
        //click on the Add projection menu item
        $(By.linkText("Add projection")).shouldBe(visible).click();

        //switch to the opened modal window
        switchToInnerFrame();
        //search for resource in resources list in the opened Select resource(s) window
        $(byText(OPENDJ_RESOURCE_NAME)).shouldBe(visible).parent().parent().findElementByTagName("input").click();
//        searchForElement(OPENDJ_RESOURCE_NAME);
//        //check if Localhost OpenDJ resource was found
//        $(byText(OPENDJ_RESOURCE_NAME)).shouldBe(visible);
//
//        //select check box in the first row for "Localhost OpenDJ (no extension schema)" resource
//        $(byAttribute("about", "resourcePopupTable")).find(By.tagName("tbody")).find(By.tagName("input"))
//                .shouldBe(visible).click();
//
//        $(byAttribute("about", "resourcePopupTable")).find(By.tagName("tbody")).find(By.tagName("input"))
//                .shouldBe(selected);

        //click Add resource(s) button
        $(By.linkText("Add resource(s)")).shouldBe(enabled).click();

        //switch to main window
        switchTo().defaultContent();

        //Fill in account fields: Common name, Surname, first and second password fields
        $(By.name("tabPanel:panel:shadows:shadowList:0:shadow:body:containers:0:container:properties:3:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).setValue(ACCOUNT_COMMON_NAME_VALUE);
        $(By.name("tabPanel:panel:shadows:shadowList:0:shadow:body:containers:0:container:properties:42:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).setValue(ACCOUNT_SURNAME_VALUE);
        $(By.name("tabPanel:panel:shadows:shadowList:0:shadow:body:containers:5:container:properties:0:property:values:0:value:valueContainer:input:inputContainer:password1"))
                .shouldBe(visible).setValue(ACCOUNT_PASSWORD_VALUE);
        $(By.name("tabPanel:panel:shadows:shadowList:0:shadow:body:containers:5:container:properties:0:property:values:0:value:valueContainer:input:inputContainer:password2"))
                .shouldBe(visible).setValue(ACCOUNT_PASSWORD_VALUE);

        //click Save button
        $(By.linkText("Save")).shouldBe(visible).click();

        //check if Success message appears after user saving
        $(byText("Success")).shouldBe(visible);

        //search for user in users list
        searchForElement(USER_NAME);
        $(By.linkText(USER_NAME)).shouldBe(visible).click();

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
        //open user's Edit page
        openUsersEditPage(USER_NAME);

        //click on the account link to expand its fields
        $(By.linkText(OPENDJ_RESOURCE_NAME)).shouldBe(visible).click();

        //update Common Name field
        $(By.name("tabPanel:panel:shadows:shadowList:0:shadow:body:containers:0:container:properties:3:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).setValue(ACCOUNT_COMMON_NAME_VALUE + UPDATED_VALUE);

        //update Surname field
        $(By.name("tabPanel:panel:shadows:shadowList:0:shadow:body:containers:0:container:properties:42:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).setValue(ACCOUNT_SURNAME_VALUE + UPDATED_VALUE);

        //click Save button
        $(By.linkText("Save")).shouldBe(visible).click();

        //check if Success message appears after user saving
        $(byText("Success")).shouldBe(visible);

        //search for user in users list
        searchForElement(USER_NAME);

        //check if users attributes were updated
        $(By.xpath("/html/body/div[1]/div/section[2]/form/div[2]/div/div[2]/table/tbody/tr/td[5]/div")).shouldHave(text(ACCOUNT_SURNAME_VALUE + UPDATED_VALUE));
        $(By.xpath("/html/body/div[1]/div/section[2]/form/div[2]/div/div[2]/table/tbody/tr/td[6]/div")).shouldHave(text(ACCOUNT_COMMON_NAME_VALUE + UPDATED_VALUE));

        //open user's Edit page
        $(By.linkText(USER_NAME)).shouldBe(visible).click();
        //click on the account link to expand its fields
        $(By.linkText(OPENDJ_RESOURCE_NAME)).shouldBe(visible).click();
        //check if account's attributes were updated
        $(By.name("tabPanel:panel:shadows:shadowList:0:shadow:body:containers:0:container:properties:3:property:values:0:value:valueContainer:input:input"))
                .shouldHave(value(ACCOUNT_COMMON_NAME_VALUE + UPDATED_VALUE));
        $(By.name("tabPanel:panel:shadows:shadowList:0:shadow:body:containers:0:container:properties:42:property:values:0:value:valueContainer:input:input"))
                .shouldHave(value(ACCOUNT_SURNAME_VALUE + UPDATED_VALUE));
    }



}
