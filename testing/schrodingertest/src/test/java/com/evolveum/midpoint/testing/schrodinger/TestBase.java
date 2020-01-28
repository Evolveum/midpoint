/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.schrodinger;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.testng.BrowserPerClass;
import com.evolveum.midpoint.schrodinger.EnvironmentConfiguration;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceConfigurationTab;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.page.configuration.AboutPage;
import com.evolveum.midpoint.schrodinger.page.configuration.ImportObjectPage;
import com.evolveum.midpoint.schrodinger.page.resource.ListResourcesPage;
import com.evolveum.midpoint.schrodinger.page.resource.ViewResourcePage;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Created by Viliam Repan (lazyman).
 */
@Listeners({BrowserPerClass.class})
public abstract class TestBase {

    public static final String PROPERTY_NAME_MIDPOINT_HOME = "-Dmidpoint.home";
    public static final String PROPERTY_NAME_USER_HOME = "user.home";
    public static final String PROPERTY_NAME_FILE_SEPARATOR = "file.separator";

    protected static final String CSV_RESOURCE_ATTR_UNIQUE= "Unique attribute name";

    private static final Logger LOG = LoggerFactory.getLogger(TestBase.class);
    protected static File csvTargetDir;

    protected MidPoint midPoint;
    protected BasicPage basicPage;


    @BeforeClass
    public void beforeClass() throws IOException {
        LOG.info("Starting tests in class {}", getClass().getName());


        if (midPoint !=null){

        }else{

        EnvironmentConfiguration config = new EnvironmentConfiguration();
        midPoint = new MidPoint(config);

        }

        FormLoginPage login = midPoint.formLogin();


        basicPage = login.loginWithReloadLoginPage(midPoint.getUsername(),midPoint.getPassword());


    }

    @AfterClass
    public void afterClass() {
        LOG.info("Finished tests from class {}", getClass().getName());

        Selenide.clearBrowserCookies();
        Selenide.clearBrowserLocalStorage();
        Selenide.close();

        midPoint.formLogin()
                .loginWithReloadLoginPage(midPoint.getUsername(),midPoint.getPassword());
    System.out.println("After: Login name "+ midPoint.getUsername()+ " pass " +midPoint.getPassword());
        AboutPage aboutPage = basicPage.aboutPage();
                aboutPage
                        .clickSwitchToFactoryDefaults()
                        .clickYes();
    }

    @BeforeMethod
    public void beforeMethod(Method method) {
        LOG.info("Starting test {}.{}", method.getDeclaringClass().getName(), method.getName());
    }

    @AfterMethod
    public void afterMethod(Method method) {
        LOG.info("Finished test {}.{}", method.getDeclaringClass().getName(), method.getName());
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
                        .clickImport()
                        .feedback()
                        .isSuccess());
    }

    protected void importObject(File source) {
        importObject(source, false);
    }


    protected String fetchMidpointHome() throws ConfigurationException {

        AboutPage aboutPage = basicPage.aboutPage();
        String mpHomeDir = aboutPage.getJVMproperty(PROPERTY_NAME_MIDPOINT_HOME);

        if (mpHomeDir != null && !mpHomeDir.isEmpty() && !PROPERTY_NAME_MIDPOINT_HOME.equalsIgnoreCase(mpHomeDir)) {

            return mpHomeDir;
        } else {

            mpHomeDir = new StringBuilder(aboutPage.getSystemProperty(PROPERTY_NAME_USER_HOME))
                    .append(aboutPage.getSystemProperty(PROPERTY_NAME_FILE_SEPARATOR)).append("midpoint").toString();

            LOG.info("Midpoint home parameter is empty! Using defaults: "+ mpHomeDir);



        }
        return mpHomeDir;
    }

    protected File initTestDirectory(String dir) throws ConfigurationException, IOException {

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
    public ViewResourcePage refreshResourceSchema(String resourceName){

        ListResourcesPage listResourcesPage = basicPage.listResources();
        ViewResourcePage resourcePage= listResourcesPage
                                            .table()
                                                .clickByName(resourceName)
                                                .refreshSchema();
        return resourcePage;
    }

    public void changeResourceAttribute(String resourceName,String attributeName, String newValue){
        changeResourceAttribute(resourceName ,attributeName ,null ,newValue ,true);
    }

    public void changeResourceAttribute(String resourceName,String attributeName, String newValue, Boolean shouldBeSuccess){
        changeResourceAttribute(resourceName ,attributeName ,null ,newValue ,shouldBeSuccess);
    }

    public void changeResourceAttribute(String resourceName,String attributeName,String oldValue, String newValue, Boolean shouldBeSuccess){
        ListResourcesPage listResourcesPage = basicPage.listResources();

        if(shouldBeSuccess){
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
            Selenide.sleep(60000);
            Selenide.screenshot("afterMinuteSleep");

            Assert.assertTrue(resourceConfigurationTab
                                    .form()
                                    .changeAttributeValue(attributeName,oldValue, newValue)
                                .and()
                            .and()
                                .clickSaveAndTestConnection()
                                .isTestSuccess()
            );
          }else{
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
                                    .changeAttributeValue(attributeName,oldValue, newValue)
                                .and()
                            .and()
                                .clickSaveAndTestConnection()
                                .isTestFailure()
            );
        }

    }

}
