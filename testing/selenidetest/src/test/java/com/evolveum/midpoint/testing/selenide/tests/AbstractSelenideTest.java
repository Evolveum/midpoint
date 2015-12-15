package com.evolveum.midpoint.testing.selenide.tests;

import com.codeborne.selenide.SelenideElement;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.util.*;

import static com.codeborne.selenide.Selenide.switchTo;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selectors.byValue;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

/**
 * Created by Kate on 13.08.2015.
 */
//@ContextConfiguration(locations = {"classpath:spring-module.xml"})
public class AbstractSelenideTest{
//        extends AbstractTestNGSpringContextTests {
    public static final String SITE_URL = "/midpoint";
    public static final String ADMIN_LOGIN = "administrator";
    public static final String ADMIN_PASSWORD = "5ecr3t";
    //User's attributes' fields' names
    public static final String USER_NAME_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:0:property:values:0:value:valueContainer:input:input";


    public static final String DESCRIPTION_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:1:property:values:0:value:valueContainer:input:input";
    public static final String FULL_NAME_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:2:property:values:0:value:valueContainer:input:input";
    public static final String GIVEN_NAME_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:3:property:values:0:value:valueContainer:input:input";
    public static final String FAMILY_NAME_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:4:property:values:0:value:valueContainer:input:input";
    public static final String ADDITIONAL_NAME_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:5:property:values:0:value:valueContainer:input:input";
    public static final String NICKNAME_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:6:property:values:0:value:valueContainer:input:input";
    public static final String HONORIFIC_PREFIX_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:7:property:values:0:value:valueContainer:input:input";
    public static final String HONORIFIC_SUFFIX_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:8:property:values:0:value:valueContainer:input:input";
    public static final String TITLE_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:9:property:values:0:value:valueContainer:input:input";
    public static final String PREFERRED_LANGUAGE_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:10:property:values:0:value:valueContainer:input:input";
    public static final String LOCALE_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:11:property:values:0:value:valueContainer:input:input";
    public static final String TIMEZONE_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:12:property:values:0:value:valueContainer:input:input";
    public static final String EMAIL_ADDRESS_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:14:property:values:0:value:valueContainer:input:input";
    public static final String TELEPHONE_NUMBER_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:15:property:values:0:value:valueContainer:input:input";
    public static final String EMPLOYEE_NUMBER_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:16:property:values:0:value:valueContainer:input:input";
    public static final String EMPLOYEE_TYPE_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:17:property:values:0:value:valueContainer:input:input";
    public static final String COST_CENTER_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:18:property:values:0:value:valueContainer:input:input";
    public static final String ORGANIZATION_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:19:property:values:0:value:valueContainer:input:input";
    public static final String ORGANIZATIONAL_UNIT_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:20:property:values:0:value:valueContainer:input:input";
    public static final String LOCALITY_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:0:container:properties:21:property:values:0:value:valueContainer:input:input";
    public static final String PASSWORD1_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:7:container:properties:0:property:values:0:value:valueContainer:input:inputContainer:password1";
    public static final String PASSWORD2_FIELD_NAME = "tabPanel:panel:focusForm:body:containers:7:container:properties:0:property:values:0:value:valueContainer:input:inputContainer:password2";

    //User's attributes' fields' values
    public static final String DESCRIPTION_FIELD_VALUE = "test description";
    public static final String FULL_NAME_FIELD_VALUE = "test full name";
    public static final String GIVEN_NAME_FIELD_VALUE = "test given name";
    public static final String FAMILY_NAME_FIELD_VALUE = "test family name";
    public static final String ADDITIONAL_NAME_FIELD_VALUE = "test add name";
    public static final String NICKNAME_FIELD_VALUE = "test nickname";
    public static final String HONORIFIC_PREFIX_FIELD_VALUE = "test prefix";
    public static final String HONORIFIC_SUFFIX_FIELD_VALUE = "test suffix";
    public static final String TITLE_FIELD_VALUE = "test title";
    public static final String PREFERRED_LANGUAGE_FIELD_VALUE = "test language";
    public static final String LOCALE_FIELD_VALUE = "test locale";
    public static final String TIMEZONE_FIELD_VALUE = "test timezone";
    public static final String EMAIL_ADDRESS_FIELD_VALUE = "test@test.com";
    public static final String TELEPHONE_NUMBER_FIELD_VALUE = "123456789";
    public static final String EMPLOYEE_NUMBER_FIELD_VALUE = "1234567";
    public static final String EMPLOYEE_TYPE_FIELD_VALUE = "test employee type";
    public static final String COST_CENTER_FIELD_VALUE = "test cost center";
    public static final String ORGANIZATION_FIELD_VALUE = "test organization";
    public static final String ORGANIZATIONAL_UNIT_FIELD_VALUE = "test organizational unit";
    public static final String LOCALITY_FIELD_VALUE = "test locality";
    public static final String PASSWORD1_FIELD_VALUE = "password";
    public static final String PASSWORD2_FIELD_VALUE = "password";
    public static final String UPDATED_VALUE = "_updated";
    //Assign role link text
    public static final String ASSIGN_ROLE_LINKTEXT = "Assign Role";

    Logger LOGGER = Logger.getLogger(AbstractSelenideTest.class);

    private static final String PARAM_SITE_URL = "site.url";
    public String siteUrl;


    @BeforeClass(alwaysRun = true)
    public void beforeClass(ITestContext context) {
        siteUrl = context.getCurrentXmlTest().getParameter(PARAM_SITE_URL);
    }

    //Login util methods
    /**
     * Log in to MidPoint as administrator
     */
    protected void login(){
        //perform login
        login(siteUrl, ADMIN_LOGIN, ADMIN_PASSWORD);
    }

    protected void login(String username, String password){
        //perform login
        login(siteUrl, username, password);
    }

    protected void login(String siteUrl, String username, String password) {
        System.setProperty("selenide.timeout","12000");
        open(siteUrl);
        //enter login value
        $(By.name("username")).shouldBe(visible).setValue(username);
        //enter password value
        $(By.name("password")).shouldBe(visible).setValue(password);
        //click Sign in button
        $(byValue("Sign in")).shouldBe(visible).click();
    }

    protected Map<String, String> getAllUserAttributesMap(){
        Map<String,String> userAttributes = new HashMap<>();
        userAttributes.put(DESCRIPTION_FIELD_NAME, DESCRIPTION_FIELD_VALUE);
        userAttributes.put(FULL_NAME_FIELD_NAME, FULL_NAME_FIELD_VALUE);
        userAttributes.put(GIVEN_NAME_FIELD_NAME, GIVEN_NAME_FIELD_VALUE);
        userAttributes.put(FAMILY_NAME_FIELD_NAME, FAMILY_NAME_FIELD_VALUE);
        userAttributes.put(ADDITIONAL_NAME_FIELD_NAME, ADDITIONAL_NAME_FIELD_VALUE);
        userAttributes.put(NICKNAME_FIELD_NAME, NICKNAME_FIELD_VALUE);
        userAttributes.put(HONORIFIC_PREFIX_FIELD_NAME, HONORIFIC_PREFIX_FIELD_VALUE);
        userAttributes.put(HONORIFIC_SUFFIX_FIELD_NAME, HONORIFIC_SUFFIX_FIELD_VALUE);
        userAttributes.put(TITLE_FIELD_NAME, TITLE_FIELD_VALUE);
        userAttributes.put(PREFERRED_LANGUAGE_FIELD_NAME, PREFERRED_LANGUAGE_FIELD_VALUE);
        userAttributes.put(LOCALE_FIELD_NAME, LOCALE_FIELD_VALUE);
        userAttributes.put(TIMEZONE_FIELD_NAME, TIMEZONE_FIELD_VALUE);
        userAttributes.put(EMAIL_ADDRESS_FIELD_NAME, EMAIL_ADDRESS_FIELD_VALUE);
        userAttributes.put(TELEPHONE_NUMBER_FIELD_NAME, TELEPHONE_NUMBER_FIELD_VALUE);
        userAttributes.put(EMPLOYEE_NUMBER_FIELD_NAME, EMPLOYEE_NUMBER_FIELD_VALUE);
        userAttributes.put(EMPLOYEE_TYPE_FIELD_NAME, EMPLOYEE_TYPE_FIELD_VALUE);
        userAttributes.put(COST_CENTER_FIELD_NAME, COST_CENTER_FIELD_VALUE);
        userAttributes.put(ORGANIZATION_FIELD_NAME, ORGANIZATION_FIELD_VALUE);
        userAttributes.put(ORGANIZATIONAL_UNIT_FIELD_NAME, ORGANIZATIONAL_UNIT_FIELD_VALUE);
        userAttributes.put(LOCALITY_FIELD_NAME, LOCALITY_FIELD_VALUE);
        userAttributes.put(PASSWORD1_FIELD_NAME, PASSWORD1_FIELD_VALUE);
        userAttributes.put(PASSWORD2_FIELD_NAME, PASSWORD2_FIELD_VALUE);
        //TODO: Activation, Photo, Parent Organization, Tenant
        return userAttributes;
    }

    /**
     * check if attribute which is found by name (key value)
     * has the appropriate value
     * @param objectAttributes
     */
    protected void checkObjectAttributesValues(Map<String, String> objectAttributes){
        Set<String> attributeFieldNames = objectAttributes.keySet();
        for (String attributeFieldName : attributeFieldNames){
            if ($(By.name(attributeFieldName)).getTagName().equals("input")) {
                $(By.name(attributeFieldName)).shouldBe(visible).shouldHave(value(objectAttributes.get(attributeFieldName)));
            } else {
                $(By.name(attributeFieldName)).shouldBe(visible).shouldHave(text(objectAttributes.get(attributeFieldName)));
            }
        }
    }

    /**
     * Log out from MP
     */
    protected void logout(){
        //click user's name in the upper right corner
        $(By.className("dropdown-toggle")).shouldBe(visible).click();
        //click on Log out menu item
        $(By.partialLinkText("Log out")).shouldBe(visible).click();
    }

    /**
     * Fill in form fields with values
     * @param fieldValues map contains field names and field values
     */
    protected void setFieldValues(Map<String, String> fieldValues){
        if (fieldValues != null && fieldValues.size() > 0){

            if (fieldValues.containsKey(PASSWORD2_FIELD_NAME)){
                $(By.name(PASSWORD2_FIELD_NAME)).shouldBe(visible).setValue(fieldValues.get(PASSWORD2_FIELD_NAME));
                fieldValues.remove(PASSWORD2_FIELD_NAME);
            }
            if (fieldValues.containsKey(PASSWORD1_FIELD_NAME)){
                $(By.name(PASSWORD1_FIELD_NAME)).shouldBe(visible).setValue(fieldValues.get(PASSWORD1_FIELD_NAME));
                fieldValues.remove(PASSWORD1_FIELD_NAME);
            }
            if (fieldValues.size() > 0) {
                Set<String> fieldNameSet = fieldValues.keySet();
                for (String fieldName : fieldNameSet) {
                    $(By.name(fieldName)).shouldBe(visible).setValue(fieldValues.get(fieldName));
                }
            }
        }

    }

    //User's util methods
    /**
     * Creates user with userName value
     * @param userName
     */
    public void createUser(String userName, Map<String, String> userFields){
        //click Users menu
        if (!$(By.partialLinkText("New user")).isDisplayed())
            $(By.partialLinkText("Users")).shouldBe(visible).click();

        //click New user menu item
        $(By.partialLinkText("New user")).click();

        //set value to Name field
        $(By.name(USER_NAME_FIELD_NAME)).shouldBe(visible).setValue(userName);
        //fill in user's attributes with values if userFields map is not empty
        setFieldValues(userFields);
        //click Save button
        $(By.linkText("Save")).shouldBe(visible).click();

    }

    /**
     * Open Users -> List users
     */
    public void openListUsersPage(){
        //click Users menu
        if (!$(By.partialLinkText("List users")).isDisplayed())
            $(By.partialLinkText("Users")).shouldBe(visible).click();

        //click List users menu item
        $(By.partialLinkText("List users")).shouldBe(visible).click();
    }

    /**
     * Prerequirement: user's Edit page is to be opened
     * Assign the specified roleName role to user
     * @param linkText          the text of the menu item from the Assignments section menu
     * @param objectName        the name of the object to be assigned
     */
    public void assignObjectToUser(String linkText, String objectName){
        //click on the menu icon next to Assignments section
        $(byAttribute("about", "assignmentsContainer")).find(byAttribute("about", "dropdownMenu")).click();
        //click Assign menu item with the specified linkText
        $(By.linkText(linkText)).shouldBe(visible).click();

        //switch to the opened modal window
        switchToInnerFrame();

        //search for object by objectName in the opened Select object(s) window
        searchForElement(objectName, "searchText");
        //select checkbox for the found object
        $(byAttribute("about", "table")).find(By.tagName("tbody")).find(By.tagName("input")).shouldBe(visible).click();
        //click Assign button
        $(By.linkText("Assign")).shouldBe(visible).click();
        $(By.linkText("Assign")).should(disappear);

        //switch to main window
        switchTo().defaultContent();
        //click Save button
        $(By.linkText("Save")).click();

        //check if Success message appears after user saving
        $(byText("Success")).shouldBe(visible);
    }

    /**
     * opens Edit page for the specified user with userName
     * @param userName
     */
    public void openUsersEditPage(String userName){
        //open Users page
        openListUsersPage();

        //search for user in users list
       searchForElement(userName);
        //click on the found user link
        $(By.linkText(userName)).shouldBe(visible).click();

    }

    /**
     * Looks for the element with specified searchText
     * @param searchText
     * @return
     */
    public void searchForElement(String searchText){
        //search for element in search form
        searchForElement(searchText, "searchText");
    }

    /**
     * Looks for the element with specified searchText in specified name
     * @param searchText
     * @param aboutTagValue
     * @return
     */
    public void searchForElement(String searchText, String aboutTagValue){
        //search for element in search form
        $(byAttribute("about", aboutTagValue)).shouldBe(visible).setValue(searchText);
        $(By.linkText("Search")).shouldBe(visible).click();
    }

    public void importObjectFromFile(String filePath){
        //click Configuration menu
        if (!$(By.partialLinkText("Import object")).isDisplayed())
            $(By.partialLinkText("Configuration")).shouldBe(visible).click();

        //click Import object menu item
        $(By.partialLinkText("Import object")).click();

        //select Overwrite existing object check box
        $(By.name("importOptions:overwriteExistingObject")).setSelected(true);

        //Specify the file to be uploaded
        File test = new File(filePath);
        $(By.name("input:inputFile:fileInput")).uploadFile(test);

        //click Import object button
//        $(By.linkText("Import object")).shouldBe(visible).click();
        $(byAttribute("about", "importFileButton")).shouldBe(visible).click();

        //check if Success message appears after resource importing
        $(byText("Success")).shouldBe(visible);

    }



    /**
     * Edit Object Policies in the Configuration -> Basic
     * @param objectType
     * @param objectTemplate
     * @param propertyConstraintList
     */
    public void editObjectPolicy(String objectType, String objectTemplate, List<String> propertyConstraintList){
        //click Configuration menu
//        $(By.partialLinkText("Configuration")).shouldBe(visible).click(); // clicked in previous step
        //click Basic menu item
        $(By.partialLinkText("System")).click();
        //click on the Edit button in the Object Policies row
        //Note: this Edit button click affects only modifying of the first row
        //of Object Policies
        $(byAttribute("placeholder", "Insert object policy")).parent().find(By.tagName("button"))
                .shouldBe(visible).click();
        //select Object Type value from drop-down list
        $(By.name("tabPanel:panel:objectPolicyEditor:templateConfigModal:content:mainForm:type:selectWrapper:select"))
                .shouldBe(visible).selectOption(objectType);
        //select Object Template value from drop-down list
        $(By.name("tabPanel:panel:objectPolicyEditor:templateConfigModal:content:mainForm:objectTemplate:selectWrapper:select"))
                .shouldBe(visible).selectOption(objectTemplate);
        if (propertyConstraintList != null && propertyConstraintList.size() > 0){
            for (int i = 0; i < propertyConstraintList.size(); i++){
                $(By.name("tabPanel:panel:mainForm:objectPolicyEditor:templateConfigModal:content:mainForm:repeater:" + i + ":textWrapper:oidBound"))
                        .shouldBe(visible).click();
                $(By.name("tabPanel:panel:mainForm:objectPolicyEditor:templateConfigModal:content:mainForm:repeater:" + i + ":textWrapper:property"))
                        .shouldBe(enabled).setValue(propertyConstraintList.get(i));
            }
        }
        //click Save button in the Edit Object Policy window
        $(byAttribute("about", "objectPolicySaveButton")).shouldBe(visible).click();
        //wait till Edit Object Policy window disappears
        $(byText("Edit Object Policy ")).shouldBe(disappear);
        //click Save button on the Configuration for midPoint page
        $(By.linkText("Save")).shouldBe(enabled).click();
        //check if Success message appears
        $(byText("Success")).shouldBe(visible);
    }


    protected void switchToInnerFrame(){
        SelenideElement element = $(byAttribute("class", "wicket_modal"));
        String modalWindowId = element.getAttribute("id");
        switchTo().innerFrame(modalWindowId);
    }
}
