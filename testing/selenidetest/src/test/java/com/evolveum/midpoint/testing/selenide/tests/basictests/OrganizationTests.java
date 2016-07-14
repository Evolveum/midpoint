package com.evolveum.midpoint.testing.selenide.tests.basictests;

import com.evolveum.midpoint.testing.selenide.tests.AbstractSelenideTest;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.close;
import static com.codeborne.selenide.Condition.*;

/**
 * Created by Kate on 24.08.2015.
 */
public class OrganizationTests extends AbstractSelenideTest {
    //Organization fields' names
    public static final String ORGANIZATION_NAME_FIELD = "Name";
    public static final String ORGANIZATION_DISPLAY_NAME_FIELD = "Display Name";
    public static final String ORGANIZATION_DESCRIPTION_FIELD = "Description";
    public static final String ORGANIZATION_ORG_TYPE_FIELD = "Type";
    public static final String ORGANIZATION_IDENTIFIER_FIELD = "Identifier";
    public static final String ORGANIZATION_COST_CENTER_FIELD = "Cost Center";
    public static final String ORGANIZATION_LOCALITY_FIELD = "Locality";
    public static final String ORGANIZATION_MAIL_DOMAIN_FIELD = "Mail Domain";
    //Organization fields' values
    public static final String ORGANIZATION_NAME_VALUE = "TestOrganization";
    public static final String SUB_ORGANIZATION_NAME_VALUE = "TestSubOrganization";
    public static final String ORGANIZATION_DISPLAY_NAME_VALUE = "TestOrgDisplayName";
    public static final String SUB_ORGANIZATION_DISPLAY_NAME_VALUE = "TestSubOrgDisplayName";
    public static final String ORGANIZATION_DESCRIPTION_VALUE = "TestDescription";
    public static final String ORGANIZATION_ORG_TYPE_VALUE = "TestOrgType";
    public static final String ORGANIZATION_IDENTIFIER_VALUE = "TestIdentifier";
    public static final String ORGANIZATION_COST_CENTER_VALUE = "TestCostCenter";
    public static final String ORGANIZATION_LOCALITY_VALUE= "TestLocality";
    public static final String ORGANIZATION_MAIL_DOMAIN_VALUE = "TestMailDomain";

    private Map<String, String> organizationFieldsMap = new HashMap<>();
    private Map<String, String> subOrganizationFieldsMap = new HashMap<>();
    private Map<String, String> updatedOrganizationFieldsMap = new HashMap<>();

    @Test(priority = 0)
    public void test001createOrganisationTest(){
        close();
        login();
        checkLoginIsPerformed();
        //fill in organization fields
        organizationFieldsMap.put(ORGANIZATION_NAME_FIELD, ORGANIZATION_NAME_VALUE);
        organizationFieldsMap.put(ORGANIZATION_DISPLAY_NAME_FIELD, ORGANIZATION_DISPLAY_NAME_VALUE);
        organizationFieldsMap.put(ORGANIZATION_DESCRIPTION_FIELD, ORGANIZATION_DESCRIPTION_VALUE);
        organizationFieldsMap.put(ORGANIZATION_ORG_TYPE_FIELD, ORGANIZATION_ORG_TYPE_VALUE);
        organizationFieldsMap.put(ORGANIZATION_IDENTIFIER_FIELD, ORGANIZATION_IDENTIFIER_VALUE);
        organizationFieldsMap.put(ORGANIZATION_COST_CENTER_FIELD, ORGANIZATION_COST_CENTER_VALUE);
        organizationFieldsMap.put(ORGANIZATION_LOCALITY_FIELD, ORGANIZATION_LOCALITY_VALUE);
        organizationFieldsMap.put(ORGANIZATION_MAIL_DOMAIN_FIELD, ORGANIZATION_MAIL_DOMAIN_VALUE);
        //create organization
        createOrganization(organizationFieldsMap, "");
        checkOperationStatusOk("Save (GUI)");
        //open created organization tab
        $(By.linkText(ORGANIZATION_DISPLAY_NAME_VALUE)).shouldBe(visible).click();
        $(byAttribute("class", "box-body org-tree-container")).shouldBe(visible).find(byAttribute("class", "cog")).shouldBe(visible).click();
        $(By.linkText("Edit")).shouldBe(visible).click();
        //check role attributes are filled in with correct values
        checkObjectAttributesValues(organizationFieldsMap);
    }

    @Test(priority = 1, dependsOnMethods = {"test001createOrganisationTest"})
    public void test002createSubOrganizationTest(){
        close();
        login();
        checkLoginIsPerformed();
        //create sub organization
        subOrganizationFieldsMap.put(ORGANIZATION_NAME_FIELD, SUB_ORGANIZATION_NAME_VALUE);
        subOrganizationFieldsMap.put(ORGANIZATION_DISPLAY_NAME_FIELD, SUB_ORGANIZATION_DISPLAY_NAME_VALUE);
        createOrganization(subOrganizationFieldsMap, ORGANIZATION_DISPLAY_NAME_VALUE);
        //check if Success message appears
        checkOperationStatusOk("Save (GUI)");
        //check if sub organization appeared in the Children org. units section
        $(byAttribute("about", "childUnitTable")).find(By.linkText(SUB_ORGANIZATION_DISPLAY_NAME_VALUE));
    }

    @Test(priority = 2, dependsOnMethods = {"test001createOrganisationTest", "test002createSubOrganizationTest"} )
    public void test003updateOrganizationTest(){
        close();
        login();
        checkLoginIsPerformed();
        //fill in updated organization fields map
        updatedOrganizationFieldsMap.put(ORGANIZATION_NAME_FIELD, ORGANIZATION_NAME_VALUE + UPDATED_VALUE);
        updatedOrganizationFieldsMap.put(ORGANIZATION_DISPLAY_NAME_FIELD, ORGANIZATION_DISPLAY_NAME_VALUE + UPDATED_VALUE);
        updatedOrganizationFieldsMap.put(ORGANIZATION_DESCRIPTION_FIELD, ORGANIZATION_DESCRIPTION_VALUE + UPDATED_VALUE);
        updatedOrganizationFieldsMap.put(ORGANIZATION_ORG_TYPE_FIELD, ORGANIZATION_ORG_TYPE_VALUE + UPDATED_VALUE);
        updatedOrganizationFieldsMap.put(ORGANIZATION_IDENTIFIER_FIELD, ORGANIZATION_IDENTIFIER_VALUE + UPDATED_VALUE);
        updatedOrganizationFieldsMap.put(ORGANIZATION_COST_CENTER_FIELD, ORGANIZATION_COST_CENTER_VALUE + UPDATED_VALUE);
        updatedOrganizationFieldsMap.put(ORGANIZATION_LOCALITY_FIELD, ORGANIZATION_LOCALITY_VALUE + UPDATED_VALUE);
        updatedOrganizationFieldsMap.put(ORGANIZATION_MAIL_DOMAIN_FIELD, ORGANIZATION_MAIL_DOMAIN_VALUE + UPDATED_VALUE);
        //click Org. structure  menu
        if (!$(By.partialLinkText("Organization tree")).isDisplayed())
            $(By.partialLinkText("Org. structure")).shouldBe(visible).click();
        //click Organization tree menu item
        $(By.partialLinkText("Organization tree")).shouldBe(visible).click();
        //open created organization tab
        $(By.linkText(ORGANIZATION_DISPLAY_NAME_VALUE)).shouldBe(visible).click();
        //click on menu icon in the Org. hierarchy row
        $(byAttribute("class", "box-body org-tree-container")).shouldBe(visible).find(byAttribute("class", "cog")).hover();
        $(byAttribute("class", "box-body org-tree-container")).shouldBe(visible).find(byAttribute("class", "cog")).shouldBe(visible).click();
        //Click Edit  menu item
        $(By.linkText("Edit")).shouldBe(visible).click();
        //fill in fields with updated values
        setFieldValues(updatedOrganizationFieldsMap);
        //click Save button
        $(By.linkText("Save")).shouldBe(visible).click();
        //open created organization tab
        $(By.linkText(ORGANIZATION_DISPLAY_NAME_VALUE + UPDATED_VALUE)).shouldBe(visible).click();

    }

    @Test (priority = 3, dependsOnMethods = {"test001createOrganisationTest", "test003updateOrganizationTest"})
    public void test004deleteOrganizationTest(){
        close();
        login();
        checkLoginIsPerformed();
        //click Organization tree menu item
        if (!$(By.partialLinkText("Organization tree")).isDisplayed())
            $(By.partialLinkText("Org. structure")).shouldBe(visible).click();
        $(By.partialLinkText("Organization tree")).shouldBe(visible).click();
        //open created organization tab
        $(By.partialLinkText(ORGANIZATION_DISPLAY_NAME_VALUE)).shouldBe(visible).click();
        //click on menu icon in the Org. hierarchy row
        $(byAttribute("class", "box-body org-tree-container")).shouldBe(visible).find(byAttribute("class", "cog")).hover();
        $(byAttribute("class", "box-body org-tree-container")).shouldBe(visible).find(byAttribute("class", "cog")).shouldBe(visible).click();

        //Click Delete menu item
        $(By.linkText("Delete")).shouldBe(visible).click();
        //Click Yes button in the Confirm delete window
        $(By.linkText("Yes")).shouldBe(visible).click();
        //check Confirm delete window disappears
        $(byText("Confirm delete")).should(disappear);
        //check if Success message appears
        checkOperationStatusOk("Delete object (Gui)");
        //open created organization tab
        $(By.linkText(ORGANIZATION_DISPLAY_NAME_VALUE + UPDATED_VALUE)).shouldNot(exist);
    }


    public void createOrganization(Map<String, String> organizationFieldsMap, String parentOrgDisplayName){
        // must go over Organization tree
        if (parentOrgDisplayName != null && !parentOrgDisplayName.isEmpty()){
            // click Organization tree
            if (!$(By.partialLinkText("Organization tree")).isDisplayed()) {
                $(By.partialLinkText("Org. structure")).shouldBe(visible).click();
            }
            $(By.partialLinkText("Organization tree")).shouldBe(visible).click();
            // click to organization
            $(By.linkText(parentOrgDisplayName)).shouldBe(visible).click();

            //click on the menu icon in the Organisation section
            $(byAttribute("class", "box-body org-tree-container")).shouldBe(visible).find(byAttribute("class", "cog")).shouldBe(visible).click();
            //click Add org. unit (root is selected)
            $(By.linkText("Create child")).shouldBe(visible).click();

            setFieldValues(organizationFieldsMap);
            //click Save button
            $(By.linkText("Save")).shouldBe(visible).click();
        }
        // create root org
        else {
            if (!$(By.partialLinkText("New organization")).isDisplayed()) {
                $(By.partialLinkText("Org. structure")).shouldBe(visible).click();
            }
            $(By.partialLinkText("New organization")).shouldBe(visible).click();
            setFieldValues(organizationFieldsMap);
            //click Save button
            $(By.linkText("Save")).shouldBe(visible).click();
        }
    }
}
