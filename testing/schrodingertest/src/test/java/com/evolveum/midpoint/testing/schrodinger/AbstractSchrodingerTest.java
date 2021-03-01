/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.ex.ElementNotFound;
import com.codeborne.selenide.testng.BrowserPerClass;
import com.codeborne.selenide.testng.annotations.Report;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.client.api.ObjectAddService;
import com.evolveum.midpoint.client.api.exception.CommonException;
import com.evolveum.midpoint.client.impl.prism.RestPrismObjectAddService;
import com.evolveum.midpoint.client.impl.prism.RestPrismService;
import com.evolveum.midpoint.client.impl.prism.RestPrismServiceBuilder;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismContextFactory;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schrodinger.EnvironmentConfiguration;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.WebDriver;
import com.evolveum.midpoint.schrodinger.component.TabWithContainerWrapper;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.common.FeedbackBox;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceAccountsTab;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceConfigurationTab;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceShadowTable;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.page.configuration.AboutPage;
import com.evolveum.midpoint.schrodinger.page.configuration.ImportObjectPage;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.page.resource.AccountPage;
import com.evolveum.midpoint.schrodinger.page.resource.ListResourcesPage;
import com.evolveum.midpoint.schrodinger.page.resource.ViewResourcePage;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.testing.schrodinger.reports.SchrodingerTextReport;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.boot.MidPointSpringApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("default")
@SpringBootTest(classes = MidPointSpringApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = { "server.port=8180", "midpoint.schrodinger=true" })
@Listeners({ BrowserPerClass.class, SchrodingerTextReport.class })
@Report
public abstract class AbstractSchrodingerTest extends AbstractTestNGSpringContextTests {

    public static final String PROPERTY_NAME_MIDPOINT_HOME = "-Dmidpoint.home";
    public static final String PROPERTY_NAME_USER_HOME = "user.home";
    public static final String PROPERTY_NAME_FILE_SEPARATOR = "file.separator";
    private static final File SYSTEM_CONFIG_INITIAL = new File("src/test/resources/configuration/objects/systemconfig/system-configuration-initial.xml");

    protected static final String CSV_RESOURCE_ATTR_UNIQUE = "Unique attribute name";

    protected static final String SCHRODINGER_PROPERTIES = "./src/test/resources/configuration/schrodinger.properties";

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSchrodingerTest.class);

    protected static File testTargetDir;

    protected EnvironmentConfiguration configuration;

    protected String username;

    protected String password;

    protected MidPoint midPoint;

    protected BasicPage basicPage;

    private boolean startMidpoint = true;

    public EnvironmentConfiguration getConfiguration() {
        return configuration;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isStartMidpoint() {
        return startMidpoint;
    }

    private Properties props = null;

    @Autowired protected PrismContext prismContext;

    protected List<File> getObjectListToImport(){
        return new ArrayList<>();
    }

    @BeforeClass(
            alwaysRun = true,
            dependsOnMethods = {"springTestContextBeforeTestClass"}
    )
    protected void springTestContextPrepareTestInstance() throws Exception {
        String startMidpointStr = getConfigurationPropertyValue("startMidpoint");
        if (!StringUtils.isEmpty(startMidpointStr) && startMidpointStr.equals("false")) {
            startMidpoint = false;
        }
        if (startMidpoint) {
            super.springTestContextPrepareTestInstance();
        } else if (prismContext == null) {
            PrismContextFactory pcf = new MidPointPrismContextFactory();
            try {
                prismContext = pcf.createPrismContext();
                prismContext.initialize();
            } catch (SchemaException | SAXException | IOException e) {
                throw new com.evolveum.midpoint.client.api.exception.SchemaException(e);
            }
        }

        getObjectListToImport().forEach(objFile -> addObjectFromFile(objFile, true));



    }


    @BeforeClass(dependsOnMethods = {"springTestContextPrepareTestInstance"})
    public void beforeClass() throws IOException {
        LOG.info("Starting tests in class {}", getClass().getName());

        if (midPoint == null) {
            configuration = buildEnvironmentConfiguration();
            midPoint = new MidPoint(configuration);

            username = getConfigurationPropertyValue("username");
            password = getConfigurationPropertyValue("password");
        }

        FormLoginPage login = midPoint.formLogin();

        basicPage = login.loginIfUserIsNotLog(username, password);
    }

    protected EnvironmentConfiguration buildEnvironmentConfiguration() throws IOException {
        EnvironmentConfiguration config = new EnvironmentConfiguration();
        config.driver(WebDriver.valueOf(getConfigurationPropertyValue("webdriver")));

        config.driverLocation(getConfigurationPropertyValue("webdriverLocation"));
        config.headless(Boolean.parseBoolean(getConfigurationPropertyValue("headlessStart")));

        String urlPropertyName = startMidpoint ? "base_url" :  "base_url_mp_already_started";
        config.baseUrl(getConfigurationPropertyValue(urlPropertyName));

        return config;
    }

    @AfterClass
    public void afterClass() {
        LOG.info("Finished tests from class {}", getClass().getName());

        Selenide.clearBrowserCookies();
        Selenide.clearBrowserLocalStorage();
        Selenide.close();

        midPoint.formLogin().loginWithReloadLoginPage(username, password);

        LOG.info("After: Login name " + username + " pass " + password);

        AboutPage aboutPage = basicPage.aboutPage();
        aboutPage
                .clickSwitchToFactoryDefaults()
                .clickYes();
    }

    /**
     * use this method in case you need to test object importing through Import object page
     * or you need to import object inside the test
     * In case if you need to add an object before the test, use repoAddObjectFromFile(File file, OperationResult operationResult)
     * @param source
     * @param overrideExistingObject
     */
    protected void importObject(File source, boolean overrideExistingObject, boolean ignoreWarning) {
        ImportObjectPage importPage = basicPage.importObject();

        if (overrideExistingObject) {
            importPage
                    .checkOverwriteExistingObject();
        }

        FeedbackBox<? extends BasicPage> feedback = importPage
                .getObjectsFromFile()
                    .chooseFile(source)
                        .clickImportFileButton()
                            .feedback();

        boolean isSuccess = false;
        try {
            isSuccess = feedback.isSuccess();
        } catch (ElementNotFound e) {
            if (!ignoreWarning) {
                throw e;
            }
            // else ignoring exception but isSuccess is still false
        }
        if (!isSuccess && ignoreWarning) {
            isSuccess = feedback.isWarning();
        }
        Assert.assertTrue(isSuccess);
    }

   protected void importObject(File source, boolean overrideExistingObject) {
        importObject(source, overrideExistingObject, false);
    }

    protected void importObject(File source) {
        importObject(source, false, false);
    }

    protected String fetchMidpointHome() {
        AboutPage aboutPage = basicPage.aboutPage();
        String mpHomeDir = aboutPage.getJVMproperty(PROPERTY_NAME_MIDPOINT_HOME);

        if (mpHomeDir != null && !mpHomeDir.isEmpty() && !PROPERTY_NAME_MIDPOINT_HOME.equalsIgnoreCase(mpHomeDir)) {

            return mpHomeDir;
        } else {

            mpHomeDir = aboutPage.getSystemProperty(PROPERTY_NAME_USER_HOME)
                    + aboutPage.getSystemProperty(PROPERTY_NAME_FILE_SEPARATOR)
                    + "midpoint";

            LOG.info("Midpoint home parameter is empty! Using defaults: " + mpHomeDir);

        }
        return mpHomeDir;
    }

    protected File initTestDirectory(String dir) throws IOException {
        return initTestDirectory(dir, true);
    }

    protected File initTestDirectory(String dir, boolean clearExist) throws IOException {

        String home = fetchMidpointHome();
        File parentDir = new File(home, "schrodinger");
        parentDir.mkdir();
        testTargetDir = new File(parentDir, dir);

        if (testTargetDir.mkdir()) {

            return testTargetDir;
        } else {
            if (testTargetDir.exists()) {
                if (clearExist) {
                    FileUtils.cleanDirectory(testTargetDir);
                }
                return testTargetDir;
            } else {

                throw new IOException("Creation of directory \"" + testTargetDir.getAbsolutePath() + "\" unsuccessful");
            }
        }
    }

    // TODO workaround -> factory reset during clean up seems to leave some old cached information breaking the resource until version change
    public ViewResourcePage refreshResourceSchema(String resourceName) {

        ListResourcesPage listResourcesPage = basicPage.listResources();
        ViewResourcePage resourcePage = listResourcesPage
                .table()
                .clickByName(resourceName)
                .refreshSchema();
        return resourcePage;
    }

    public void changeResourceAttribute(String resourceName, String attributeName, String newValue) {
        changeResourceAttribute(resourceName, attributeName, null, newValue, true);
    }

    public void changeResourceAttribute(String resourceName, String attributeName, String newValue, Boolean shouldBeSuccess) {
        changeResourceAttribute(resourceName, attributeName, null, newValue, shouldBeSuccess);
    }

    public void changeResourceAttribute(String resourceName, String attributeName, String oldValue, String newValue, Boolean shouldBeSuccess) {
        ListResourcesPage listResourcesPage = basicPage.listResources();

        if (shouldBeSuccess) {
            ViewResourcePage viewResourcePage = listResourcesPage
                    .table()
                    .search()
                    .byName()
                    .inputValue(resourceName)
                    .updateSearch()
                    .and()
                    .clickByName(resourceName);
            Selenide.screenshot("beforeEditConfiguration");
            ResourceConfigurationTab resourceConfigurationTab = viewResourcePage
                    .clickEditResourceConfiguration();
            Selenide.screenshot("afterEditConfigurationClick");
            Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);
            Selenide.screenshot("afterSixSecondsSleep");

            resourceConfigurationTab
                    .form()
                    .changeAttributeValue(attributeName, oldValue, newValue)
                    .and()
                    .and()
                    .clickSaveAndTestConnection()
                    .assertIsTestSuccess();
        } else {
            listResourcesPage
                            .table()
                            .search()
                            .byName()
                            .inputValue(resourceName)
                            .updateSearch()
                            .and()
                            .clickByName(resourceName)
                            .clickEditResourceConfiguration()
                            .form()
                            .changeAttributeValue(attributeName, oldValue, newValue)
                            .and()
                            .and()
                            .clickSaveAndTestConnection()
                            .assertIsTestFailure();
        }

    }

    protected void addObjectFromFile(File file) {
        addObjectFromFile(file, false);
    }

    protected void addObjectFromFile(File file, boolean overwrite) {
        try {
//            PrismObject object = prismContext.parseObject(file);
            List<PrismObject<?>> objects = prismContext.parserFor(file).parseObjects();
            RestPrismServiceBuilder builder = RestPrismServiceBuilder.create();
            RestPrismService service = builder
                    .baseUrl(getConfigurationPropertyValue(startMidpoint ? "base_url" : "base_url_mp_already_started") + "/ws/rest")
                    .username(getConfigurationPropertyValue("username"))
                    .password(getConfigurationPropertyValue("password"))
                    .build();
            final List<String> options = new ArrayList<>();
            if (overwrite) {
                options.add("overwrite");
            }
            objects.forEach(object -> {
                try {
                    addObjectService(service, object).setOptions(options).post();
                } catch (CommonException e) {
                    LOG.error("Unable to add object, {}", e);
                }
            });
        } catch (CommonException | SchemaException | IOException ex) {
            LOG.error("Unable to add object, {}", ex);
        }
    }

    private RestPrismObjectAddService addObjectService(RestPrismService service, PrismObject object) {
        ObjectAddService<? extends ObjectType> addService = null;
        if (object.isOfType(UserType.class)) {
            addService = service.users().add((UserType) object.asObjectable());
        } else if (object.isOfType(RoleType.class)) {
            addService = service.roles().add((RoleType) object.asObjectable());
        } else if (object.isOfType(OrgType.class)) {
            addService = service.orgs().add((OrgType) object.asObjectable());
        } else if (object.isOfType(ArchetypeType.class)) {
            addService = service.archetypes().add((ArchetypeType) object.asObjectable());
        } else if (object.isOfType(LookupTableType.class)) {
            addService = service.lookupTables().add((LookupTableType) object.asObjectable());
        } else if (object.isOfType(ObjectTemplateType.class)) {
            addService = service.objectTemplates().add((ObjectTemplateType) object.asObjectable());
        } else if (object.isOfType(ResourceType.class)) {
            addService = service.resources().add((ResourceType) object.asObjectable());
        } else if (object.isOfType(SystemConfigurationType.class)) {
            addService = service.systemConfigurations().add((SystemConfigurationType) object.asObjectable());
        } else if (object.isOfType(TaskType.class)) {
            addService = service.tasks().add((TaskType) object.asObjectable());
        } else if (object.isOfType(ValuePolicyType.class)) {
            addService = service.valuePolicies().add((ValuePolicyType) object.asObjectable());
        } else if (object.isOfType(SecurityPolicyType.class)) {
            addService = service.securityPolicies().add((SecurityPolicyType) object.asObjectable());
        }
        return (RestPrismObjectAddService) addService;
    }

    public UserPage showUser(String userName){
        UserPage user = showUserInTable(userName).clickByName(userName);
        Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        return user;
    }

    public AssignmentHolderObjectListTable<ListUsersPage, UserPage> showUserInTable(String userName) {
        return basicPage.listUsers()
                .table()
                .search()
                .byName()
                .inputValue(userName)
                .updateSearch()
                .and();
    }

    public AccountPage showShadow(String resourceName, String searchedItem, String itemValue){
        return showShadow(resourceName, searchedItem, itemValue, null, false);
    }

    public AccountPage showShadow(String resourceName, String searchedItem, String itemValue, String intent, boolean useRepository){
        return getShadowTable(resourceName, searchedItem, itemValue, intent, useRepository)
                .clickByName(itemValue);
    }

    public ResourceShadowTable assertShadowExists(String resourceName, String searchedItem, String itemValue){
        return assertShadowExists(resourceName, searchedItem, itemValue, null, false);
    }

    public ResourceShadowTable assertShadowExists(String resourceName, String searchedItem, String itemValue, String intent,  boolean useRepository){
        ResourceShadowTable table = getShadowTable(resourceName, searchedItem, itemValue, intent, useRepository);
        return (ResourceShadowTable) table.assertTableContainsText(itemValue);
    }

    public ResourceShadowTable assertShadowDoesntExist(String resourceName, String searchedItem, String itemValue){
        return assertShadowDoesntExist(resourceName, searchedItem, itemValue, null, false);
    }

    public ResourceShadowTable assertShadowDoesntExist(String resourceName, String searchedItem, String itemValue, String intent,  boolean useRepository){
        ResourceShadowTable table = getShadowTable(resourceName, searchedItem, itemValue, intent, useRepository);
        return (ResourceShadowTable) table.assertTableDoesntContainText(itemValue);
    }

    public ResourceShadowTable getShadowTable(String resourceName, String searchedItem, String itemValue) {
        return getShadowTable(resourceName, searchedItem, itemValue, null, false);
    }

    public ResourceShadowTable getShadowTable(String resourceName, String searchedItem, String itemValue, String intent, boolean useRepository) {
        ResourceAccountsTab<ViewResourcePage> tab = basicPage.listResources()
                .table()
                .search()
                .byName()
                .inputValue(resourceName)
                .updateSearch()
                .and()
                .clickByName(resourceName)
                .clickAccountsTab();
        if (useRepository) {
            tab.clickSearchInRepository();
        } else {
            tab.clickSearchInResource();
        }
        Selenide.sleep(1000);
        if (intent != null && !intent.isEmpty()) {
            tab.setIntent(intent);
            Selenide.sleep(MidPoint.TIMEOUT_DEFAULT_2_S);
        }
        return tab.table()
                .search()
                .resetBasicSearch()
                .textInputPanelByItemName(searchedItem)
                .inputValue(itemValue)
                .updateSearch()
                .and();
    }

    protected TaskPage showTask(String name, String menuKey) {
        return basicPage.listTasks(menuKey)
                .table()
                .search()
                .byName()
                .inputValue(name)
                .updateSearch()
                .and()
                .clickByName(name);
    }

    protected TaskPage showTask(String name) {
        return showTask(name, "");
    }

    protected void createUser(String newUserName) {
        Map<String, String> attr = new HashMap<>();
        attr.put("Name", newUserName);
        createUser(attr);
    }

    protected void createUser(Map<String, String> newUserAttributesMap) {
        PrismForm<TabWithContainerWrapper<UserPage>> form = basicPage
                .newUser()
                    .selectTabBasic()
                        .form();
        newUserAttributesMap.forEach((key, attr) -> form.addAttributeValue(key, newUserAttributesMap.get(key)));
        form
                    .and()
                .and()
                .clickSave()
                .feedback()
                .assertSuccess();
    }

    private String getConfigurationPropertyValue(String propertyName) throws IOException {
        if (props == null) {
            props = new Properties();
            InputStream is = new FileInputStream(new File(SCHRODINGER_PROPERTIES));
            props.load(is);
        }
        String headlessStart = System.getProperty(propertyName);
        if (headlessStart == null) {
            headlessStart = props.getProperty(propertyName);
        }
        return headlessStart;
    }
}
