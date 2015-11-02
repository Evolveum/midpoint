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
    public static final String ORGANIZATION_NAME_FIELD = "tabPanel:panel:focusForm:body:containers:0:container:properties:0:property:values:0:value:valueContainer:input:input";
    public static final String ORGANIZATION_DISPLAY_NAME_FIELD = "tabPanel:panel:focusForm:body:containers:0:container:properties:1:property:values:0:value:valueContainer:input:input";
    public static final String ORGANIZATION_DESCRIPTION_FIELD = "tabPanel:panel:focusForm:body:containers:0:container:properties:2:property:values:0:value:valueContainer:input:input";
    public static final String ORGANIZATION_ORG_TYPE_FIELD = "tabPanel:panel:focusForm:body:containers:0:container:properties:4:property:values:0:value:valueContainer:input:input";
    public static final String ORGANIZATION_IDENTIFIER_FIELD = "tabPanel:panel:focusForm:body:containers:0:container:properties:3:property:values:0:value:valueContainer:input:input";
    public static final String ORGANIZATION_COST_CENTER_FIELD = "tabPanel:panel:focusForm:body:containers:0:container:properties:6:property:values:0:value:valueContainer:input:input";
    public static final String ORGANIZATION_LOCALITY_FIELD = "tabPanel:panel:focusForm:body:containers:0:container:properties:7:property:values:0:value:valueContainer:input:input";
    public static final String ORGANIZATION_MAIL_DOMAIN_FIELD = "tabPanel:panel:focusForm:body:containers:0:container:properties:8:property:values:0:value:valueContainer:input:input";
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
        // TODO: display order, risk level, ...
        //click Org. structure menu
        $(By.partialLinkText("Org. structure")).shouldBe(visible).click();
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
        createOrganization(subOrganizationFieldsMap, ORGANIZATION_DISPLAY_NAME_VALUE);
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
        //click Org. structure  menu
        if (!$(By.partialLinkText("Organization tree")).isDisplayed())
            $(By.partialLinkText("Org. structure")).shouldBe(visible).click();
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


    public void createOrganization(Map<String, String> organizationFieldsMap, String parentOrgDisplayName){
        // must go over Organization tree
        if (parentOrgDisplayName != null && !parentOrgDisplayName.isEmpty()){
            // click Organization tree
            $(By.partialLinkText("Organization tree")).shouldBe(visible).click();
            // click to organization
            $(By.linkText(parentOrgDisplayName)).shouldBe(visible).click();

            //click on the menu icon in the Organisation section
            $(By.xpath("/html/body/div[1]/div/section[2]/div[2]/div/div/div[4]/div[3]/div/div/form/div[2]/div[2]/table/thead/tr/th[6]/div/span[1]/ul/li/a"))
                    .shouldBe(visible).click();
            //click Add org. unit (root is selected)
            $(By.linkText("Add org. unit")).shouldBe(visible).click();

            setFieldValues(organizationFieldsMap);
            //click Save button
            $(By.linkText("Save")).shouldBe(visible).click();
        }
        // create root org
        else {
            //click New organization menu
            $(By.partialLinkText("New organization")).shouldBe(visible).click();
            setFieldValues(organizationFieldsMap);
            //click Save button
            $(By.linkText("Save")).shouldBe(visible).click();
        }
    }
}
