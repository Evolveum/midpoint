/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.testing.schrodinger;

import com.codeborne.selenide.testng.BrowserPerClass;
import com.evolveum.midpoint.schrodinger.EnvironmentConfiguration;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.page.LoginPage;
import com.evolveum.midpoint.schrodinger.page.configuration.AboutPage;
import com.evolveum.midpoint.schrodinger.page.configuration.ImportObjectPage;
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

    //public static final String BASE_URL = "http://localhost:8080/midpoint";

    //public static final String USERNAME = "administrator";
    //public static final String PASSWORD = "5ecr3t";

    public static final String PROPERTY_NAME_MIDPOINT_HOME = "-Dmidpoint.home";

    private static final Logger LOG = LoggerFactory.getLogger(TestBase.class);
    protected static File CSV_TARGET_DIR;

    protected MidPoint midPoint;
    protected BasicPage basicPage;


    @BeforeClass
    public void beforeClass() throws IOException {
        LOG.info("Starting tests in class {}", getClass().getName());

        //config.baseUrl(BASE_URL);

        if (midPoint !=null){

        }else{

        EnvironmentConfiguration config = new EnvironmentConfiguration();
        midPoint = new MidPoint(config);

        }

        LoginPage login = midPoint.login();


        basicPage = login.login(midPoint.getUsername(),midPoint.getPassword());

    }

    @AfterClass
    public void afterClass() {
        LOG.info("Finished tests from class {}", getClass().getName());
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
        String homeDir = aboutPage.getJVMproperty(PROPERTY_NAME_MIDPOINT_HOME);

        if (homeDir != null && !homeDir.isEmpty()) {

            return homeDir;
        } else {

            LOG.error("Midpoint home parameter is empty!");
            throw new ConfigurationException("Midpoint home parameter is empty ,please add the -Dmidpoint.home parameter to the jvm configuration");

        }

    }

    protected File initTestDirectory(String dir) throws ConfigurationException, IOException {

        String home = fetchMidpointHome();
        File parentDir = new File(home, "schrodinger");
        parentDir.mkdir();
        CSV_TARGET_DIR = new File(parentDir, dir);

        if (CSV_TARGET_DIR.mkdir()) {

            return CSV_TARGET_DIR;
        } else {
            if (CSV_TARGET_DIR.exists()) {

                FileUtils.cleanDirectory(CSV_TARGET_DIR);
                return CSV_TARGET_DIR;
            } else {

                throw new IOException("Creation of directory \"" + CSV_TARGET_DIR.getName() + "\" unsuccessful");
            }
        }
    }

}
