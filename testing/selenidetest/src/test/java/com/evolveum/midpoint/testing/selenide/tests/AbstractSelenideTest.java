package com.evolveum.midpoint.testing.selenide.tests;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public static final String USER_NAME_FIELD_NAME = "Name";

    public static final String DESCRIPTION_FIELD_NAME = "Description";
    public static final String FULL_NAME_FIELD_NAME = "Full name";
    public static final String GIVEN_NAME_FIELD_NAME = "Given name";
    public static final String FAMILY_NAME_FIELD_NAME = "Family name";
    public static final String ADDITIONAL_NAME_FIELD_NAME = "Additional Name";
    public static final String NICKNAME_FIELD_NAME = "Nickname";
    public static final String HONORIFIC_PREFIX_FIELD_NAME = "Honorific Prefix";
    public static final String HONORIFIC_SUFFIX_FIELD_NAME = "Honorific Suffix";
    public static final String TITLE_FIELD_NAME = "Title";
    public static final String EMAIL_ADDRESS_FIELD_NAME = "Email Address";
    public static final String TELEPHONE_NUMBER_FIELD_NAME = "Telephone Number";
    public static final String EMPLOYEE_NUMBER_FIELD_NAME = "Employee Number";
    public static final String COST_CENTER_FIELD_NAME = "Cost Center";
    public static final String ORGANIZATION_FIELD_NAME = "Organization";
    public static final String ORGANIZATIONAL_UNIT_FIELD_NAME = "Organizational Unit";
    public static final String PASSWORD1_FIELD_NAME = "password1";
    public static final String PASSWORD2_FIELD_NAME = "password2";

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
    public static final String ASSIGN_ROLE_LINKTEXT = "Assign";
    public static final String ASSIGN_DEFAULT_OBJECT_TYPE = "RoleType";

    public static final String ASSIGNMENT_TAB_NAME = "Assignments";
    public static final String INDUCEMENT_TAB_NAME = "Inducements";

    Logger LOGGER = LoggerFactory.getLogger(AbstractSelenideTest.class);

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
        userAttributes.put(EMAIL_ADDRESS_FIELD_NAME, EMAIL_ADDRESS_FIELD_VALUE);
        userAttributes.put(TELEPHONE_NUMBER_FIELD_NAME, TELEPHONE_NUMBER_FIELD_VALUE);
        userAttributes.put(EMPLOYEE_NUMBER_FIELD_NAME, EMPLOYEE_NUMBER_FIELD_VALUE);
        userAttributes.put(COST_CENTER_FIELD_NAME, COST_CENTER_FIELD_VALUE);
        userAttributes.put(ORGANIZATION_FIELD_NAME, ORGANIZATION_FIELD_VALUE);
        userAttributes.put(ORGANIZATIONAL_UNIT_FIELD_NAME, ORGANIZATIONAL_UNIT_FIELD_VALUE);
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
            if (attributeFieldName.equals(DESCRIPTION_FIELD_NAME)){
                findAttributeValueFiledByDisplayName(attributeFieldName, "textarea")
                        .shouldHave(text(objectAttributes.get(attributeFieldName)));
            } else {
                findAttributeValueFiledByDisplayName(attributeFieldName, "input")
                        .shouldHave(value(objectAttributes.get(attributeFieldName)));
            }
        }
    }

    /**
     * Log out from MP
     */
    protected void logout(){
        //click user's name in the upper right corner
        $(byAttribute("class", "dropdown user user-menu")).shouldBe(visible).click();
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
                $(byAttribute("about", PASSWORD2_FIELD_NAME)).shouldBe(visible).setValue(fieldValues.get(PASSWORD2_FIELD_NAME));
                fieldValues.remove(PASSWORD2_FIELD_NAME);
            }
            if (fieldValues.containsKey(PASSWORD1_FIELD_NAME)){
                $(byAttribute("about", PASSWORD1_FIELD_NAME)).shouldBe(visible).setValue(fieldValues.get(PASSWORD1_FIELD_NAME));
                fieldValues.remove(PASSWORD1_FIELD_NAME);
            }
            if (fieldValues.size() > 0) {
                Set<String> fieldNameSet = fieldValues.keySet();
                for (String fieldName : fieldNameSet) {
                    if (fieldName.equals(DESCRIPTION_FIELD_NAME)){
                        findAttributeValueFiledByDisplayName(fieldName, "textarea")
                                .shouldBe(visible).setValue(fieldValues.get(fieldName));
                    } else {
                        findAttributeValueFiledByDisplayName(fieldName, "input")
                                .shouldBe(visible).setValue(fieldValues.get(fieldName));
                    }
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
        $(By.partialLinkText("New user")).shouldBe(visible).click();

        //set value to Name field
        findAttributeValueFiledByDisplayName(USER_NAME_FIELD_NAME, "input")
                .shouldBe(visible).setValue(userName);
        //fill in user's attributes with values if userFields map is not empty
        setFieldValues(userFields);
        //click Save button
        $(By.linkText("Save")).shouldBe(visible).click();

    }

     public void createRole(Map<String, String> roleFields){
        if (!$(By.partialLinkText("New role")).isDisplayed())
            $(By.partialLinkText("Roles")).shouldBe(visible).click();

        $(By.partialLinkText("New role")).shouldBe(visible).click();
        setFieldValues(roleFields);
        //click Save button
        $(By.linkText("Save")).shouldBe(visible).click();

    }

     public void createService(Map<String, String> servviceFields){
        if (!$(By.partialLinkText("New service")).isDisplayed())
            $(By.partialLinkText("Services")).shouldBe(visible).click();

        $(By.partialLinkText("New service")).shouldBe(visible).click();
        setFieldValues(servviceFields);
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

    public void assignObjectToFocusObject(String linkText, String objectName) {
        assignObjectToFocusObject(linkText, "", objectName);
    }

    public void assignObjectToFocusObject(String linkText, String objectType, String objectName) {
        assignObjectToFocusObject(linkText, objectType, objectName, ASSIGNMENT_TAB_NAME);
    }
    /**
     * Prerequirement: user's Edit page is to be opened
     * Assign the specified roleName role to user
     * @param linkText          the text of the menu item from the Assignments section menu
     * @param objectName        the name of the object to be assigned
     */
    public void assignObjectToFocusObject(String linkText, String objectType, String objectName, String tabName){
        if (tabName != null && tabName.equals(INDUCEMENT_TAB_NAME)){
            openInducementsTab();
        } else {
            openAssignmentsTab();
        }
        $(byAttribute("about", "dropdownMenu")).click();
        //click Assign menu item with the specified linkText
        $(By.linkText(linkText)).shouldBe(visible).click();

        if (objectType != null && !(objectType.trim().isEmpty())){
            $(byAttribute("class", "form-inline search-form")).find(By.tagName("select")).shouldBe(visible).click();
            $(byText(objectType)).shouldBe(visible).click();
            $(byAttribute("class", "form-inline search-form")).find(By.tagName("select")).find(byAttribute("selected", "selected")).shouldHave(text(objectType));
        }
        //search for object by objectName in the opened Select object(s) window
        searchForElement(objectName, "searchText");
        //select checkbox for the found object
        SelenideElement element = $(byAttribute("class", "wicket-modal"));
        element.find(byAttribute("about", "table")).find(By.tagName("tbody")).find(byAttribute("type", "checkbox")).shouldBe(visible).setSelected(true);
        //click Assign button
        $(By.linkText("Add")).shouldBe(visible).click();
        $(By.linkText("Add")).should(disappear);

        //click Save button
        $(By.linkText("Save")).click();
    }

    protected void openAssignmentsTab(){
        $(byAttribute("class", "tab2")).shouldBe(visible).click();
    }

    protected void openInducementsTab(){
        $(byAttribute("class", "tab5")).shouldBe(visible).click();
    }

    protected void openProjectionsTab(){
        $(byAttribute("class", "tab1")).shouldBe(visible).click();
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
        $(byAttribute("about", "searchItemButton")).shouldBe(visible).click();
        $(byAttribute("class", "popover-content")).find(byAttribute("class", "form-control input-sm")).shouldBe(visible).setValue(searchText);
        $(byAttribute("class", "popover-content")).find(byAttribute("class", "btn btn-sm btn-success")).shouldBe(visible).click();
        $(byAttribute("class", "popover-content")).find(byAttribute("class", "btn btn-sm btn-success")).shouldBe(disappear);
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
        $(byText("Import file (Gui)")).shouldBe(visible);

    }



    /**
     * Edit Object Policies in the Configuration -> Basic
     * @param objectType
     * @param objectTemplate
     * @param propertyConstraintList
     */
    public void editObjectPolicy(String objectType, String objectTemplate, List<String> propertyConstraintList){
        if (!$(By.partialLinkText("System")).isDisplayed()) {
            $(By.partialLinkText("Configuration")).shouldBe(visible).click(); // clicked in previous step
        }
        $(By.partialLinkText("System")).shouldBe(visible).click();
        //click on the Edit button in the Object Policies row
        //Note: this Edit button click affects only modifying of the first row
        //of Object Policies
        $(byText("Object policies")).parent().find(By.tagName("button"))
                .shouldBe(visible).click();
        //select Object Type value from drop-down list
        $(byText("Object type")).parent().parent().find(By.tagName("select")).shouldBe(visible).selectOption(objectType);
        //select Object Template value from drop-down list
        $(byText("Object template")).parent().parent().find(By.tagName("select")).shouldBe(visible).selectOption(objectTemplate);
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
        checkOperationStatusOk("Update system configuration (GUI)");
    }


    protected void switchToInnerFrame(){
        SelenideElement element = $(byAttribute("class", "wicket-modal"));
        String modalWindowId = element.getAttribute("id");
        switchTo().innerFrame(modalWindowId);
    }

    protected void checkOperationStatusOk(String message) {
        $(byText(message)).shouldBe(visible);
        $(byAttribute("class", "icon fa fa-check")).shouldBe(visible);
    }

    protected void checkLoginIsPerformed(){
        $(By.partialLinkText("Home")).click();
        $(byText("My work items")).shouldBe(visible);
    }

    protected SelenideElement findAttributeValueFiledByDisplayName(String displayName, String fieldTagName){
        return $(byText(displayName)).parent().parent().find(By.tagName(fieldTagName));
    }

    protected SelenideElement getFeedbackPanel(){
        return $(byAttribute("class", "feedbackContainer"));
    }

}
