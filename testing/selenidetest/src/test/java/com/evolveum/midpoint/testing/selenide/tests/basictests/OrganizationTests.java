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
    public static final String ORGANIZATION_NAME_FIELD = "name:textWrapper:text";
    public static final String ORGANIZATION_DISPLAY_NAME_FIELD = "displayName:textWrapper:text";
    public static final String ORGANIZATION_DESCRIPTION_FIELD = "description:textWrapper:text";
    public static final String ORGANIZATION_ORG_TYPE_FIELD = "orgType:repeater:0:textWrapper:text";
    public static final String ORGANIZATION_IDENTIFIER_FIELD = "identifier:textWrapper:text";
    public static final String ORGANIZATION_COST_CENTER_FIELD = "costCenter:textWrapper:text";
    public static final String ORGANIZATION_LOCALITY_FIELD = "locality:textWrapper:text";
    public static final String ORGANIZATION_MAIL_DOMAIN_FIELD = "mailDomain:repeater:0:textWrapper:text";
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
        //check if Success message appears
        $(byText("Success")).shouldBe(visible);
        //open created organization tab
        $(By.linkText(ORGANIZATION_DISPLAY_NAME_VALUE)).shouldBe(visible).click();
        //click on menu icon in the Org. hierarchy row
        $(byAttribute("about", "treeMenu")).find(byAttribute("class", "dropdown-toggle")).shouldBe(visible).click();
        //Click Edit root menu item
        $(By.linkText("Edit root")).shouldBe(visible).click();
        //check role attributes are filled in with correct values
        checkObjectAttributesValues(organizationFieldsMap);
    }

    @Test(priority = 1, dependsOnMethods = {"test001createOrganisationTest"})
    public void test002createSubOrganizationTest(){
        //create sub organization
        subOrganizationFieldsMap.put(ORGANIZATION_NAME_FIELD, SUB_ORGANIZATION_NAME_VALUE);
        subOrganizationFieldsMap.put(ORGANIZATION_DISPLAY_NAME_FIELD, SUB_ORGANIZATION_DISPLAY_NAME_VALUE);
        createOrganization(subOrganizationFieldsMap, ORGANIZATION_NAME_VALUE);
        //check if Success message appears
        $(byText("Success")).shouldBe(visible);
        //check if sub organization appeared in the Children org. units section
        $(byAttribute("about", "childUnitTable")).find(By.linkText(SUB_ORGANIZATION_DISPLAY_NAME_VALUE));
    }

    @Test(priority = 2, dependsOnMethods = {"test001createOrganisationTest"})
    public void test003updateOrganizationTest(){
        //fill in updated organization fields map
        updatedOrganizationFieldsMap.put(ORGANIZATION_NAME_FIELD, ORGANIZATION_NAME_VALUE + UPDATED_VALUE);
        updatedOrganizationFieldsMap.put(ORGANIZATION_DISPLAY_NAME_FIELD, ORGANIZATION_DISPLAY_NAME_VALUE + UPDATED_VALUE);
        updatedOrganizationFieldsMap.put(ORGANIZATION_DESCRIPTION_FIELD, ORGANIZATION_DESCRIPTION_VALUE + UPDATED_VALUE);
        updatedOrganizationFieldsMap.put(ORGANIZATION_ORG_TYPE_FIELD, ORGANIZATION_ORG_TYPE_VALUE + UPDATED_VALUE);
        updatedOrganizationFieldsMap.put(ORGANIZATION_IDENTIFIER_FIELD, ORGANIZATION_IDENTIFIER_VALUE + UPDATED_VALUE);
        updatedOrganizationFieldsMap.put(ORGANIZATION_COST_CENTER_FIELD, ORGANIZATION_COST_CENTER_VALUE + UPDATED_VALUE);
        updatedOrganizationFieldsMap.put(ORGANIZATION_LOCALITY_FIELD, ORGANIZATION_LOCALITY_VALUE + UPDATED_VALUE);
        updatedOrganizationFieldsMap.put(ORGANIZATION_MAIL_DOMAIN_FIELD, ORGANIZATION_MAIL_DOMAIN_VALUE + UPDATED_VALUE);
        //click Users menu
        $(By.partialLinkText("Users")).shouldBe(visible).click();
        //click Organization tree menu item
        $(By.partialLinkText("Organization tree")).shouldBe(visible).click();
        //open created organization tab
        $(By.linkText(ORGANIZATION_DISPLAY_NAME_VALUE)).shouldBe(visible).click();
        //click on menu icon in the Org. hierarchy row
        $(byAttribute("about", "treeMenu")).find(byAttribute("class", "dropdown-toggle")).shouldBe(visible).click();
        //Click Edit root menu item
        $(By.linkText("Edit root")).shouldBe(visible).click();
        //fill in fields with updated values
        setFieldValues(updatedOrganizationFieldsMap);
        //click Save button
        $(By.linkText("Save")).shouldBe(visible).click();
        //open created organization tab
        $(By.linkText(ORGANIZATION_DISPLAY_NAME_VALUE + UPDATED_VALUE)).shouldBe(visible).click();

    }

    @Test (priority = 3, dependsOnMethods = {"test001createOrganisationTest", "test003updateOrganizationTest"})
    public void test004deleteOrganizationTest(){
        //click Users menu
        $(By.partialLinkText("Users")).shouldBe(visible).click();
        //click Organization tree menu item
        $(By.partialLinkText("Organization tree")).shouldBe(visible).click();
        //open created organization tab
        $(By.linkText(ORGANIZATION_DISPLAY_NAME_VALUE + UPDATED_VALUE)).shouldBe(visible).click();
        //click on menu icon in the Org. hierarchy row
        $(byAttribute("about", "treeMenu")).find(byAttribute("class", "dropdown-toggle")).shouldBe(visible).click();
        //Click Edit root menu item
        $(By.linkText("Delete root")).shouldBe(visible).click();
        //Click Yes button in the Confirm delete window
        $(By.linkText("Yes")).shouldBe(visible).click();
        //check Confirm delete window disappears
        $(byText("Confirm delete")).should(disappear);
        //check if Success message appears
        $(byText("Success")).shouldBe(visible);
        //open created organization tab
        $(By.linkText(ORGANIZATION_DISPLAY_NAME_VALUE + UPDATED_VALUE)).shouldNot(exist);
    }


    public void createOrganization(Map<String, String> organizationFieldsMap, String parentOrgName){
        //click Users menu
        $(By.partialLinkText("Users")).shouldBe(visible).click();
        //click New organization menu
        $(By.partialLinkText("New organization")).shouldBe(visible).click();
        setFieldValues(organizationFieldsMap);
        if (parentOrgName != null && !parentOrgName.isEmpty()){
            //click Edit button for Parent org. units field
            $(byText("Edit")).shouldBe(visible).click();
            //click on the parent organization name in the opened Choose object window
            $(By.linkText(ORGANIZATION_NAME_VALUE)).shouldBe(visible).click();
            //wait till Choose object window close
            $(byText("Choose object")).should(disappear);
        }
        //click Save button
        $(By.linkText("Save")).shouldBe(visible).click();
    }
}
