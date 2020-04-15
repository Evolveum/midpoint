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
import java.util.Properties;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.testng.BrowserPerClass;

import com.evolveum.midpoint.schrodinger.component.AssignmentsTab;

import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTableWithPrismView;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;

import com.evolveum.midpoint.schrodinger.EnvironmentConfiguration;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.WebDriver;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceConfigurationTab;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.page.configuration.AboutPage;
import com.evolveum.midpoint.schrodinger.page.configuration.ImportObjectPage;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.page.resource.ListResourcesPage;
import com.evolveum.midpoint.schrodinger.page.resource.ViewResourcePage;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.web.boot.MidPointSpringApplication;

/**
 * Created by Viliam Repan (lazyman).
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("default")
@SpringBootTest(classes = MidPointSpringApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = { "server.port=8180", "midpoint.schrodinger=true" })//, "webdriverLocation=234234234" })
@Listeners({ BrowserPerClass.class })
public abstract class AbstractSchrodingerTest extends AbstractIntegrationTest {

    public static final String PROPERTY_NAME_MIDPOINT_HOME = "-Dmidpoint.home";
    public static final String PROPERTY_NAME_USER_HOME = "user.home";
    public static final String PROPERTY_NAME_FILE_SEPARATOR = "file.separator";

    protected static final String CSV_RESOURCE_ATTR_UNIQUE = "Unique attribute name";

    protected static final String SCHRODINGER_PROPERTIES = "./src/test/resources/configuration/schrodinger.properties";

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSchrodingerTest.class);

    protected static File csvTargetDir;

    protected EnvironmentConfiguration configuration;

    protected String username;

    protected String password;

    protected MidPoint midPoint;

    protected BasicPage basicPage;

    public EnvironmentConfiguration getConfiguration() {
        return configuration;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @BeforeClass
    public void beforeClass() throws IOException {
        LOG.info("Starting tests in class {}", getClass().getName());

        if (midPoint == null) {
            Properties props = new Properties();
            InputStream is = new FileInputStream(new File(SCHRODINGER_PROPERTIES));
            props.load(is);

            configuration = buildEnvironmentConfiguration(props);
            midPoint = new MidPoint(configuration);

            username = props.getProperty("username");
            password = props.getProperty("password");
        }

        FormLoginPage login = midPoint.formLogin();

        basicPage = login.loginIfUserIsNotLog(username, password);
    }

    protected EnvironmentConfiguration buildEnvironmentConfiguration(Properties props) {
        EnvironmentConfiguration config = new EnvironmentConfiguration();
        config.driver(WebDriver.valueOf(props.getProperty("webdriver")));

        String webdriverLocation = System.getProperty("webdriverLocation");
        if (webdriverLocation == null) {
            webdriverLocation = props.getProperty("webdriverLocation");
        }
        config.driverLocation(webdriverLocation);

        String headlessStart = System.getProperty("headlessStart");
        if (headlessStart == null) {
            headlessStart = props.getProperty("headlessStart");
        }

        config.headless(Boolean.parseBoolean(headlessStart));

        String baseUrl = System.getProperty("base_url");
        if (baseUrl == null) {
            baseUrl = props.getProperty("base_url");
        }
        config.baseUrl(baseUrl);

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

    protected void importObject(File source, Boolean overrideExistingObject) {
        ImportObjectPage importPage = basicPage.importObject();

        if (overrideExistingObject) {
            importPage
                    .checkOverwriteExistingObject();
        }

        Assert.assertTrue(
                importPage
                        .getObjectsFromFile()
                        .chooseFile(source)
                        .clickImportFileButton()
                        .feedback()
                        .isSuccess());
    }

    protected void importObject(File source) {
        importObject(source, false);
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

        String home = fetchMidpointHome();
        File parentDir = new File(home, "schrodinger");
        parentDir.mkdir();
        csvTargetDir = new File(parentDir, dir);

        if (csvTargetDir.mkdir()) {

            return csvTargetDir;
        } else {
            if (csvTargetDir.exists()) {

                FileUtils.cleanDirectory(csvTargetDir);
                return csvTargetDir;
            } else {

                throw new IOException("Creation of directory \"" + csvTargetDir.getAbsolutePath() + "\" unsuccessful");
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

            Assert.assertTrue(resourceConfigurationTab
                    .form()
                    .changeAttributeValue(attributeName, oldValue, newValue)
                    .and()
                    .and()
                    .clickSaveAndTestConnection()
                    .isTestSuccess()
            );
        } else {
            Assert.assertTrue(
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
                            .isTestFailure()
            );
        }

    }
}
