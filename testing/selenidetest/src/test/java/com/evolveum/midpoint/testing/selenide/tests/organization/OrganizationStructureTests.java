package com.evolveum.midpoint.testing.selenide.tests.organization;

import com.evolveum.midpoint.testing.selenide.tests.AbstractSelenideTest;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import java.util.HashMap;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.close;

/**
 * Created by Kate on 17.08.2015.
 */
public class OrganizationStructureTests extends AbstractSelenideTest {
    public static final String ORG_FILE_PATH = "../../samples/org/org-monkey-island-simple.xml";
    public static final String ASSIGN_ORG_UNIT_LINKTEXT = "Assign";
    public static final String USER_NAME = "OrgTestUser";
    public static final String ORG_UNIT_NAME = "F0002";
    public static final String ORG_UNIT_DISPLAY_NAME = "Ministry of Defense";
    public static final String PARENT_ORG_UNIT_DISPLAY_NAME = "Governor Office";

    /**
     *  Import organization structure from org-monkey-island-simple.xml
     *  sample file. Check if organization tree was created in MP
     */
    @Test(priority = 0)
    public void test001importOrganizationStructureFromFileTest(){
        close();
        login();

        checkLoginIsPerformed();
        //import organization structure xml file
        importObjectFromFile(ORG_FILE_PATH);

        //click Org. structure menu
        $(By.partialLinkText("Org. structure")).shouldBe(visible).click();

        //click Organization tree menu item
        $(By.partialLinkText("Organization tree")).shouldBe(visible).click();

        //check if organization structure was created in midPoint
        $(byText("Governor Office")).shouldBe(visible);
        $(byText("Projects")).shouldBe(visible);
    }

    @Test(priority = 1, dependsOnMethods = {"test001importOrganizationStructureFromFileTest"})
    public void test002assignOrgUnitTest(){
        close();
        login();
        //create test user
        createUser(USER_NAME, new HashMap<String, String>());
        //open user's Edit page
        openUsersEditPage(USER_NAME);
        //assign F0002 org unit (Ministry of Defense) to the user
        assignObjectToFocusObject(ASSIGN_ORG_UNIT_LINKTEXT, "OrgType", ORG_UNIT_NAME);
        //open user's Edit page
        openUsersEditPage(USER_NAME);
        openAssignmentsTab();
        //check if assigned org. unit is displayed in the Assignments section
        $(byText(ORG_UNIT_DISPLAY_NAME)).shouldBe(visible);
        //click Org. structure menu
        $(By.partialLinkText("Org. structure")).shouldBe(visible).click();
        //click Organization tree menu item
        $(By.partialLinkText("Organization tree")).shouldBe(visible).click();
        //click on Ministry of Defense
        $(By.partialLinkText(PARENT_ORG_UNIT_DISPLAY_NAME)).shouldBe(visible).click();
        $(By.partialLinkText(ORG_UNIT_DISPLAY_NAME)).shouldBe(visible).click();
        //check if user was found in the organization
        $(byText(USER_NAME)).shouldBe(visible);

    }

    @Test(priority = 2, dependsOnMethods = {"test001importOrganizationStructureFromFileTest", "test002assignOrgUnitTest"})
    public void test003unassignOrgUnitTest(){
        close();
        login();
        //open user's Edit page
        openUsersEditPage(USER_NAME);
        openAssignmentsTab();
        //select checkbox for org. unit in Assignments section
        $(byAttribute("class","box assignment object-org-box-thin")).shouldHave(text(ORG_UNIT_DISPLAY_NAME)).find(By.tagName("input")).setSelected(true);
        //click on the menu icon next to Assignments section
        $(byAttribute("about", "assignments")).find(byAttribute("about", "dropdownMenu")).click();
        //click Assign menu item with the specified linkText
        $(By.linkText("Unassign")).shouldBe(visible).click();
        //click Yes button in the opened Confirm delete window
        $(By.partialLinkText("Yes")).shouldBe(visible).click();
        //click Save button
        $(By.linkText("Save")).shouldBe(visible).click();
        checkOperationStatusOk("Save (GUI)");
        //open user's Edit page
        openUsersEditPage(USER_NAME);
        //check if there is no assignments in the Assignments section any more
        openAssignmentsTab();
        assert !($(byText(ORG_UNIT_DISPLAY_NAME)).exists());
        //click Org. structure menu
        $(By.partialLinkText("Org. structure")).shouldBe(visible).click();
        //click Organization tree menu item
        $(By.partialLinkText("Organization tree")).shouldBe(visible).click();
        //click on Ministry of Defense
        $(By.partialLinkText(PARENT_ORG_UNIT_DISPLAY_NAME)).shouldBe(visible).click();
        $(By.partialLinkText(ORG_UNIT_DISPLAY_NAME)).shouldBe(visible).click();
        //search for the user in the opened organization
        $(byText(USER_NAME)).shouldNotBe(visible);

    }



}
