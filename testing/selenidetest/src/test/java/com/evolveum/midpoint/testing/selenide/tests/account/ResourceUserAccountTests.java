package com.evolveum.midpoint.testing.selenide.tests.account;

/**
 * Created by Kate on 09.08.2015.
 */

import com.evolveum.midpoint.testing.selenide.tests.BaseTest;
import com.evolveum.midpoint.testing.selenide.tests.LoginTest;
import com.evolveum.midpoint.testing.selenide.tests.Util;
import com.evolveum.midpoint.testing.selenide.tests.user.SimpleUserTests;
import com.evolveum.midpoint.testing.selenide.tests.user.UserUtil;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.close;

@Component
public class ResourceUserAccountTests extends BaseTest {

    @Autowired
    LoginTest loginTest;

    @Autowired
    SimpleUserTests simpleUserTests;

    @Autowired
    UserUtil userUtil;

    @Autowired
    Util util;

    public static final String USER_NAME = "UserWithOpendjAccount";
    public static final String OPENDJ_RESOURCE_NAME = "Localhost OpenDJ (no extension schema)";
    public static final String OPENDJ_RESOURCE_PATH = "../../samples/resources/opendj/opendj-localhost-resource-sync-no-extension-advanced.xml";
    public static final String USER_ADMINISTRATOR_ROLE_NAME = "User Administrator";
    public static final String AUTHORIZATION_ROLES_XML_PATH = "../../samples/roles/authorization-roles.xml";
    public static final String END_USER_ROLE_NAME = "End user";
    public static final String ACCOUNT_SURNAME_VALUE = "Surname";
    public static final String ACCOUNT_PASSWORD_VALUE = "Common name";
    public static final String ACCOUNT_COMMON_NAME_VALUE = "Common name";
    public static final String UPDATED_VALUE = "_updated";

    /**
     * Import OpenDJ resource test (file "opendj-localhost-resource-sync-no-extension-advanced.xml" is used)
     */
    @Test(priority = 0)
    public void importResourceTest(){
        close();
        loginTest.login();

        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small"))
                .shouldHave(text("welcome to midPoint"));

        //import resource xml file
        util.importObjectFromFile(OPENDJ_RESOURCE_PATH);
        //open Resources -> List Resources
        $(By.xpath("/html/body/div[3]/div/div[2]/ul[1]/li[4]/a")).shouldHave(text("Resources")).click();
        $(By.linkText("List resources")).click();

        //search for resource in resources list
        util.searchForElement(OPENDJ_RESOURCE_NAME, "/html/body/div[4]/div/form[1]/span/a");
        $(By.xpath("/html/body/div[4]/div/form[2]/div[2]/table/tbody/tr/td[2]/div/a/span"))
                .shouldBe(visible);

    }

    /**
     * Check resource connection on the
     * Resource details page
     */
    @Test(priority = 1, dependsOnMethods = {"importResourceTest"})
    public void checkResourceConnectionTest(){
        //open Resources -> List Resources
        $(By.xpath("/html/body/div[3]/div/div[2]/ul[1]/li[4]/a")).shouldHave(text("Resources")).click();
        $(By.linkText("List resources")).click();

        //search for resource in resources list
        util.searchForElement(ResourceUserAccountTests.OPENDJ_RESOURCE_NAME, "/html/body/div[4]/div/form[1]/span/a");
        $(By.xpath("/html/body/div[4]/div/form[2]/div[2]/table/tbody/tr/td[2]/div/a/span")).click();
        //click on resource link
        $(By.linkText(ResourceUserAccountTests.OPENDJ_RESOURCE_NAME)).click();

        //click Test connection button
        $(By.xpath("/html/body/div[4]/div/form/div[4]/a[1]")).should(appear).click();

        //check if all statuses are succeeded
        $(By.xpath("/html/body/div[4]/div/form/div[2]/div[2]/div/table/tbody/tr[1]/td[2]/i")).shouldHave(hasAttribute("title", "Success"));
        $(By.xpath("/html/body/div[4]/div/form/div[2]/div[2]/div/table/tbody/tr[2]/td[2]/i")).shouldHave(hasAttribute("title", "Success"));
        $(By.xpath("/html/body/div[4]/div/form/div[2]/div[2]/div/table/tbody/tr[3]/td[2]/i")).shouldHave(hasAttribute("title", "Success"));
        $(By.xpath("/html/body/div[4]/div/form/div[2]/div[2]/div/table/tbody/tr[4]/td[2]/i")).shouldHave(hasAttribute("title", "Success"));
        $(By.xpath("/html/body/div[4]/div/form/div[2]/div[2]/div/table/tbody/tr[5]/td[2]/i")).shouldHave(hasAttribute("title", "Success"));

    }

    /**
     * Create user, then create account for the user with
     * resource imported in the previous test
     */
    @Test (priority = 2, dependsOnMethods = {"importResourceTest"})
    public void createAccountTest() {
        //create user with filled user name only
        userUtil.createUser(USER_NAME);

        //open user's Edit page
        userUtil.openUsersEditPage(USER_NAME);

        //click on the menu icon in the Accounts section
        $(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[1]/div/div[2]/ul/li/a")).shouldBe(visible).click();
        //click on the Add account menu item
        $(By.linkText("Add account")).shouldBe(visible).click();

        //search for resource in resources list in the opened Select resource(s) window
        util.searchForElement(OPENDJ_RESOURCE_NAME,
                "/html/body/div[6]/form/div/div[2]/div/div/div/div[2]/div/div/div/div/div/div[1]/form/span/a");
        $(By.xpath("/html/body/div[6]/form/div/div[2]/div/div/div/div[2]/div/div/div/div/div/div[2]/div/table/tbody/tr/td[2]/div"))
                .shouldHave(text(OPENDJ_RESOURCE_NAME));

        //select check box in the first row for "Localhost OpenDJ (no extension schema)" resource
        $(By.name("resourcePopup:content:table:table:body:rows:4:cells:1:cell:check")).shouldBe(visible).click();
        //click Add resource(s) button
        $(By.linkText("Add resource(s)")).shouldBe(enabled).click();

        //Fill in account fields: Common name, Surname, first and second password fields
        $(By.name("accounts:accountList:0:account:body:containers:0:container:properties:3:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).setValue(ACCOUNT_COMMON_NAME_VALUE);
        $(By.name("accounts:accountList:0:account:body:containers:0:container:properties:42:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).setValue(ACCOUNT_SURNAME_VALUE);
        $(By.name("accounts:accountList:0:account:body:containers:5:container:properties:0:property:values:0:value:valueContainer:input:password2"))
                .shouldBe(visible).setValue(ACCOUNT_PASSWORD_VALUE);
        $(By.name("accounts:accountList:0:account:body:containers:5:container:properties:0:property:values:0:value:valueContainer:input:password1"))
                .shouldBe(visible).setValue(ACCOUNT_PASSWORD_VALUE);

        //click Save button
        $(By.xpath("/html/body/div[4]/div/form/div[6]/a[2]")).shouldHave(text("Save")).click();

        //check if Success message appears after user saving
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).shouldHave(text("Success"));

        //search for user in users list
        util.searchForElement(USER_NAME, "/html/body/div[4]/div/div[4]/form/span/a");
        $(By.linkText(USER_NAME)).shouldBe(visible).click();

        //check if the created account is displayed in the Accounts section
        $(By.linkText(OPENDJ_RESOURCE_NAME)).shouldBe(visible);
    }

    /**
     *  update account attributes (Common Name, Surname),
     *  check if the appropriate user's attributes were
     *  also updated
     */
    @Test (priority = 4, dependsOnMethods = {"createAccountTest"})
    public void updateAccountAttributesTest(){
        close();
        loginTest.login();
        //open user's Edit page
        userUtil.openUsersEditPage(USER_NAME);

        //click on the account link to expand its fields
        $(By.linkText(OPENDJ_RESOURCE_NAME)).shouldBe(visible).click();

        //update Common Name field
        $(By.name("accounts:accountList:0:account:body:containers:0:container:properties:3:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).setValue(ACCOUNT_COMMON_NAME_VALUE + UPDATED_VALUE);

        //update Surname field
        $(By.name("accounts:accountList:0:account:body:containers:0:container:properties:42:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).setValue(ACCOUNT_SURNAME_VALUE + UPDATED_VALUE);

        //click Save button
        $(By.xpath("/html/body/div[4]/div/form/div[6]/a[2]")).shouldHave(text("Save")).click();

        //check if Success message appears after user saving
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).shouldHave(text("Success"));

        //search for user in users list
        util.searchForElement(USER_NAME, "/html/body/div[4]/div/div[4]/form/span/a");

        //check if users attributes were updated
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr/td[5]/div")).shouldHave(text(ACCOUNT_SURNAME_VALUE + UPDATED_VALUE));
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr/td[6]/div")).shouldHave(text(ACCOUNT_COMMON_NAME_VALUE + UPDATED_VALUE));

        //open user's Edit page
        $(By.linkText(USER_NAME)).shouldBe(visible).click();
        //click on the account link to expand its fields
        $(By.linkText(OPENDJ_RESOURCE_NAME)).shouldBe(visible).click();
        //check if account's attributes were updated
        $(By.name("accounts:accountList:0:account:body:containers:0:container:properties:3:property:values:0:value:valueContainer:input:input"))
                .shouldHave(value(ACCOUNT_COMMON_NAME_VALUE + UPDATED_VALUE));
        $(By.name("accounts:accountList:0:account:body:containers:0:container:properties:42:property:values:0:value:valueContainer:input:input"))
                .shouldHave(value(ACCOUNT_SURNAME_VALUE + UPDATED_VALUE));
    }



}
