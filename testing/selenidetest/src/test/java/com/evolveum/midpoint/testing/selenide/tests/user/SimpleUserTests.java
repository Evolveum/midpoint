package com.evolveum.midpoint.testing.selenide.tests.user;

import com.evolveum.midpoint.testing.selenide.tests.BaseTest;
import com.evolveum.midpoint.testing.selenide.tests.LoginTest;
import com.evolveum.midpoint.testing.selenide.tests.Util;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.close;

/**
 * Created by Kate on 14.08.2015.
 */
@Component
public class SimpleUserTests extends BaseTest {

    @Autowired
    UserUtil userUtil;

    @Autowired
    LoginTest loginTest;

    @Autowired
    Util util;

    //test data
    public static final String UPDATED_STRING = "_updated";
    public static final String USER_DESCRIPTION = "description";
    public static final String USER_FULL_NAME = "full name";
    public static final String USER_GIVEN_NAME = "given name";
    public static final String USER_FAMILY_NAME = "family name";
    public static final String USER_NICKNAME = "nickname";
    public static final String SIMPLE_USER_NAME = "SimpleUserName";


    /**
     * Create user test.
     */
    @Test (priority = 1)
    public void createUserWithUserNameOnlyTest() {
        close();
        loginTest.login();

        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small"))
                .shouldHave(text("welcome to midPoint"));

        //create user with filled user name only
        userUtil.createUser(SIMPLE_USER_NAME);

        //check if Success message appears after user saving
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).shouldHave(text("Success"));

        //search for the created in users list
        util.searchForElement(SIMPLE_USER_NAME, "/html/body/div[4]/div/div[4]/form/span/a");
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr/td[3]/div/a/span"))
                .shouldBe(visible).click();

        close();
    }

    /**
     * Updating user fields
     */
    @Test(dependsOnMethods = {"createUserWithUserNameOnlyTest"}, priority = 3)
    public void editUserTest() {
        close();
        loginTest.login();
        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small"))
                .shouldHave(text("welcome to midPoint"));

        //open user's Edit page
        userUtil.openUsersEditPage(SIMPLE_USER_NAME);

        //click on the menu icon in the User details section
        $(By.xpath("/html/body/div[4]/div/form/div[3]/div[1]/div/div/div/div[2]/div[2]/ul/li/a")).shouldBe(visible).click();
        //click on Show empty fields menu item
        $(By.linkText("Show empty fields")).shouldBe(visible).click();

        //update Name field
        $(By.name("userForm:body:containers:0:container:properties:0:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).clear();
        $(By.name("userForm:body:containers:0:container:properties:0:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).setValue(SIMPLE_USER_NAME + "_updated");
        //update Description field
        $(By.name("userForm:body:containers:0:container:properties:1:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).setValue(USER_DESCRIPTION + UPDATED_STRING);
        //update Full Name field
        $(By.name("userForm:body:containers:0:container:properties:2:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).setValue(USER_FULL_NAME + UPDATED_STRING);
        //update Given Name field
        $(By.name("userForm:body:containers:0:container:properties:3:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).setValue(USER_GIVEN_NAME + UPDATED_STRING);
        //update Family Name field
        $(By.name("userForm:body:containers:0:container:properties:4:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).setValue(USER_FAMILY_NAME + UPDATED_STRING);
        //update Nickname field
        $(By.name("userForm:body:containers:0:container:properties:6:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).setValue(USER_NICKNAME + UPDATED_STRING);

        //click Save button
        $(By.xpath("/html/body/div[4]/div/form/div[6]/a[2]")).shouldHave(text("Save")).click();

        //check if Success message appears after user saving
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).shouldHave(text("Success"));

        //search for user in users list
        util.searchForElement(SIMPLE_USER_NAME + UPDATED_STRING, "/html/body/div[4]/div/div[4]/form/span/a");

        //check if updated values are displayed in the users list
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr[1]/td[3]/div"))
                .shouldHave(text(SIMPLE_USER_NAME + UPDATED_STRING));
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr[1]/td[4]/div"))
                .shouldHave(text(USER_GIVEN_NAME + UPDATED_STRING));
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr[1]/td[5]/div"))
                .shouldHave(text(USER_FAMILY_NAME + UPDATED_STRING));
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr[1]/td[6]/div"))
                .shouldHave(text(USER_FULL_NAME + UPDATED_STRING));

        //click on the user link
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr/td[3]/div/a/span"))
                .shouldBe(visible).click();

        //check if updated values are displayed on the user's Edit page
        $(By.name("userForm:body:containers:0:container:properties:0:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).shouldHave(value(SIMPLE_USER_NAME + "_updated"));
        $(By.name("userForm:body:containers:0:container:properties:1:property:values:0:value:valueContainer:input:input"))
                .shouldHave(value(USER_DESCRIPTION + UPDATED_STRING));
        $(By.name("userForm:body:containers:0:container:properties:2:property:values:0:value:valueContainer:input:input"))
                .shouldHave(value(USER_FULL_NAME + UPDATED_STRING));
        $(By.name("userForm:body:containers:0:container:properties:3:property:values:0:value:valueContainer:input:input"))
                .shouldHave(value(USER_GIVEN_NAME + UPDATED_STRING));
        $(By.name("userForm:body:containers:0:container:properties:4:property:values:0:value:valueContainer:input:input"))
                .shouldHave(value(USER_FAMILY_NAME + UPDATED_STRING));
        $(By.name("userForm:body:containers:0:container:properties:6:property:values:0:value:valueContainer:input:input"))
                .shouldHave(value(USER_NICKNAME + UPDATED_STRING));

        close();
    }

    /**
     * Cancelling of user update
     */
    @Test(dependsOnMethods = {"createUserWithUserNameOnlyTest"}, priority = 2)
    public void cancelUserUpdateTest() {
        close();
        loginTest.login();
        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small"))
                .shouldHave(text("welcome to midPoint"));

        //open user's Edit page
        userUtil.openUsersEditPage(SIMPLE_USER_NAME);


        //click on the menu icon in the User details section
        $(By.xpath("/html/body/div[4]/div/form/div[3]/div[1]/div/div/div/div[2]/div[2]/ul/li/a")).shouldBe(visible).click();
        //click on Show empty fields menu item
        $(By.linkText("Show empty fields")).shouldBe(visible).click();

        //update Name field
        $(By.name("userForm:body:containers:0:container:properties:0:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).clear();
        $(By.name("userForm:body:containers:0:container:properties:0:property:values:0:value:valueContainer:input:input"))
                .shouldBe(visible).setValue(SIMPLE_USER_NAME + "_updated");

        //click Save button
        $(By.xpath("/html/body/div[4]/div/form/div[6]/span/a")).shouldHave(text("Back")).click();

        //search for user in users list
        util.searchForElement(SIMPLE_USER_NAME, "/html/body/div[4]/div/div[4]/form/span/a");

        //check if user name wasn't updated
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr[1]/td[3]/div"))
                .shouldHave(text(SIMPLE_USER_NAME));
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr[1]/td[3]/div"))
                .shouldHave(not(text(UPDATED_STRING)));
        close();
    }

    /**
     * Attempt to create user with all empty fields
     */
    @Test(alwaysRun = true, priority = 4)
    public void createUserWithEmptyFieldsTest() {
        close();
        loginTest.login();
        //create user with all empty fields
        userUtil.createUser("");

        //check if error message appears after user saving
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]")).shouldHave(text("No name in new object"));
        close();
    }

    /**
     * Attempt to create user with existing name
     */
    @Test(alwaysRun = true, priority = 5)
    public void createUserWithExistingNameTest() {
        close();
        loginTest.login();
        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small"))
                .shouldHave(text("welcome to midPoint"));

        //create user
        String userName = "Existing name";
        userUtil.createUser(userName);
        //check if Success message appears after user saving
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).shouldHave(text("Success"));
        //try to create user with the same name
        userUtil.createUser(userName);
        //check if error message appears
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).shouldBe(visible)
                .shouldHave(text("Error processing focus"));

        close();
    }

    @Test(alwaysRun = true, priority = 6)
    public void deleteUserTest() {
        close();
        loginTest.login();
        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small"))
                .shouldHave(text("welcome to midPoint"));

        //open Users -> List users
        userUtil.openListUsersPage();

        //search for user in users list
        util.searchForElement(SIMPLE_USER_NAME + UPDATED_STRING, "/html/body/div[4]/div/div[4]/form/span/a");

        //select checkbox next to the found user
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tbody/tr/td[1]/div/input")).shouldBe(visible).click();

        //click on the menu icon in the upper right corner of the users list
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/thead/tr/th[9]/div/span[1]/ul/li/a")).shouldBe(visible).click();
        //click on Delete menu item
        $(By.linkText("Delete")).shouldBe(visible).click();

        //click on Yes button in the opened "Confirm delete" window
        $(By.xpath("/html/body/div[6]/form/div/div[2]/div/div/div/div[2]/div/div/div/div/p[2]/a[1]")).shouldBe(visible).click();

        //check if operation success message appears
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).shouldHave(text("Success"));
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[2]/ul/li/div/span"))
                .shouldHave(text("Delete users (Gui)"));

        //search for user in users list
        util.searchForElement(SIMPLE_USER_NAME + UPDATED_STRING, "/html/body/div[4]/div/div[4]/form/span/a");
        //check the user was not found during user search
        $(By.xpath("/html/body/div[4]/div/form/div[2]/table/tfoot/tr/td")).shouldBe(visible)
                .shouldHave(text("No matching result found."));
        close();
    }


}
