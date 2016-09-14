package com.evolveum.midpoint.testing.selenide.tests.basictests;

import static com.codeborne.selenide.Condition.*;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.testing.selenide.tests.AbstractSelenideTest;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selectors.byValue;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.close;

/**
 * Created by Kate on 22.08.2015.
 */
public class ServiceTests extends AbstractSelenideTest {
    //service fields
    public static final String SERVICE_NAME_FIELD = "Name";
    public static final String SERVICE_DISPLAY_NAME_FIELD = "Display Name";
    public static final String SERVICE_DESCRIPTION_FIELD = "Description";
    public static final String SERVICE_TYPE_FIELD = "Type";
    public static final String SERVICE_IDENTIFIER_FIELD = "Identifier";
    public static final String SERVICE_RISK_LEVEL_FIELD = "Risk Level";
    //service values
    public static final String SERVICE_NAME_VALUE = "TestService";
    public static final String SERVICE_DISPLAY_NAME_VALUE = "ServiceDisplayName";
    public static final String SERVICE_DESCRIPTION_VALUE = "ServiceDescription";
    public static final String SERVICE_TYPE_VALUE = "ServiceType";
    public static final String SERVICE_IDENTIFIER_VALUE = "ServiceIdentifier";
    public static final String SERVICE_RISK_LEVEL_VALUE = "ServiceRiskLevel";

    public static final String SERVICE_TEST_USER = "ServiceTestUser";

    private Map<String, String> serviceAttributes = new HashMap<>();

    @Test(priority = 0)
    public void test001createServiceTest(){
        close();
        login();
        checkLoginIsPerformed();
        //set new service attributes
        serviceAttributes.put(SERVICE_NAME_FIELD, SERVICE_NAME_VALUE);
        serviceAttributes.put(SERVICE_DISPLAY_NAME_FIELD, SERVICE_DISPLAY_NAME_VALUE);
        serviceAttributes.put(SERVICE_DESCRIPTION_FIELD, SERVICE_DESCRIPTION_VALUE);
        serviceAttributes.put(SERVICE_TYPE_FIELD, SERVICE_TYPE_VALUE);
        serviceAttributes.put(SERVICE_IDENTIFIER_FIELD, SERVICE_IDENTIFIER_VALUE);
        serviceAttributes.put(SERVICE_RISK_LEVEL_FIELD, SERVICE_RISK_LEVEL_VALUE);
        createService(serviceAttributes);
        //check  message appears
        checkOperationStatusOk("Save (GUI)");
        //search for newly created service
        searchForElement(SERVICE_NAME_VALUE);
        //click on the found service
        $(By.linkText(SERVICE_NAME_VALUE)).shouldBe(visible).click();
        //check service attributes are filled in with correct values
        checkObjectAttributesValues(serviceAttributes);

    }

    @Test(priority = 1, dependsOnMethods = {"test001createServiceTest"})
    public void test002updateServiceTest(){
        close();
        login();
        checkLoginIsPerformed();
        if (!$(By.partialLinkText("List services")).isDisplayed()) {
            $(By.partialLinkText("Services")).shouldBe(visible).click();
        }
        //click List services menu item
        $(By.partialLinkText("List services")).shouldBe(visible).click();
        //search for newly created service
        searchForElement(SERVICE_NAME_VALUE);
        //click on the found service
        $(By.linkText(SERVICE_NAME_VALUE)).shouldBe(visible).click();
        //update service attributes values
        Set<String> attributeFielldNames = serviceAttributes.keySet();
        Map<String, String> serviceAttributesUpdated = new HashMap<>();
        for (String attributeFieldName : attributeFielldNames){
            serviceAttributesUpdated.put(attributeFieldName, serviceAttributes.get(attributeFieldName) + UPDATED_VALUE);
        }
        setFieldValues(serviceAttributesUpdated);
        //click Save button
        $(By.linkText("Save")).shouldBe(visible).click();
        checkOperationStatusOk("Save (GUI)");
        //search for newly created service
        searchForElement(SERVICE_NAME_VALUE + UPDATED_VALUE);
        //click on the found service
        $(By.linkText(SERVICE_NAME_VALUE + UPDATED_VALUE)).shouldBe(visible).click();
        //check service attributes are filled in with correct values
        checkObjectAttributesValues(serviceAttributesUpdated);
    }

    @Test(priority = 2, dependsOnMethods = {"test001createServiceTest"})
    public void test003assignInducementForRoleTest(){
        close();
        login();
        checkLoginIsPerformed();
        if (!$(By.partialLinkText("List services")).isDisplayed()) {
            $(By.partialLinkText("Services")).shouldBe(visible).click();
        }
        //click List services menu item
        $(By.partialLinkText("List services")).shouldBe(visible).click();
        //search for newly created service
        searchForElement(SERVICE_NAME_VALUE + UPDATED_VALUE);
        //click on the found service
        $(By.linkText(SERVICE_NAME_VALUE + UPDATED_VALUE)).shouldBe(visible).click();

        assignObjectToFocusObject(ASSIGN_ROLE_LINKTEXT, null, EndUserTests.ENDUSER_ROLE_NAME, INDUCEMENT_TAB_NAME);

        checkOperationStatusOk("Save (GUI)");
        searchForElement(SERVICE_NAME_VALUE + UPDATED_VALUE);
        $(By.linkText(SERVICE_NAME_VALUE + UPDATED_VALUE)).shouldBe(visible).click();
        openInducementsTab();
        $(By.linkText(EndUserTests.ENDUSER_ROLE_NAME)).shouldBe(visible);
    }


    @Test(priority = 3, dependsOnMethods = {"test001createServiceTest"})
    public void test004assignServiceToUserTest(){
        close();
        login();
        checkLoginIsPerformed();

        Map<String, String> usersMap = new HashMap<>();
        createUser(SERVICE_TEST_USER, usersMap);
        //search for the created user in users list
        searchForElement(SERVICE_TEST_USER);
        //click on the found user link
        $(By.linkText(SERVICE_TEST_USER)).shouldBe(visible).click();

        //assign End user role to user
        assignObjectToFocusObject(ASSIGN_ROLE_LINKTEXT, "ServiceType", SERVICE_NAME_VALUE + UPDATED_VALUE, null);

        //search for the user in users list
        searchForElement(SERVICE_TEST_USER);
        //click on the found user link
        $(By.linkText(SERVICE_TEST_USER)).shouldBe(visible).click();

        //check if assigned role is displayed on the Assignments tab
        openAssignmentsTab();
        $(byAttribute("about", "dropdownMenu")).click();
        $(By.linkText("Show all assignments")).shouldBe(visible).click();

        SelenideElement element = $(byAttribute("class", "wicket-modal"));
        element.find(By.linkText(SERVICE_DISPLAY_NAME_VALUE + UPDATED_VALUE)).shouldBe(visible);
        element.find(By.linkText(EndUserTests.ENDUSER_ROLE_NAME)).shouldBe(visible);
    }

    @Test (priority = 4, dependsOnMethods = {"test001createServiceTest", "test002updateServiceTest"})
    public void test005deleteServiceTest(){
        close();
        login();
        checkLoginIsPerformed();
        if (!$(By.partialLinkText("List services")).isDisplayed()) {
            $(By.partialLinkText("Services")).shouldBe(visible).click();
        }
        //click List services menu item
        $(By.partialLinkText("List services")).shouldBe(visible).click();
        //search for created service
        searchForElement(SERVICE_NAME_VALUE + UPDATED_VALUE);
        //select found service checkbox
        $(By.tagName("tbody")).find(byAttribute("type", "checkbox")).shouldBe(visible).setSelected(true);
        //click on the menu icon in the upper right corner of the services list
        $(byAttribute("class", "cog")).shouldBe(visible).click();
        //click Delete menu item
        $(By.linkText("Delete")).shouldBe(visible).click();
        //click Yes button in the Confirm delete window
        $(By.linkText("Yes")).shouldBe(visible).click();
        checkOperationStatusOk("Delete services (GUI)");
    }

}
