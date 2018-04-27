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

package schrodinger;

import com.codeborne.selenide.testng.BrowserPerClass;
import com.evolveum.midpoint.schrodinger.EnvironmentConfiguration;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.page.LoginPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

import java.lang.reflect.Method;

/**
 * Created by Viliam Repan (lazyman).
 */
@Listeners({BrowserPerClass.class})
public abstract class TestBase {

    public static final String BASE_URL = "http://localhost:8080/midpoint";

    public static final String USERNAME = "administrator";
    public static final String PASSWORD = "5ecr3t";

    private static final Logger LOG = LoggerFactory.getLogger(TestBase.class);

    protected MidPoint midPoint;
    protected BasicPage basicPage;

    @BeforeClass
    public void beforeClass() {
        LOG.info("Starting tests in class {}", getClass().getName());

        EnvironmentConfiguration config = new EnvironmentConfiguration();
        config.baseUrl(BASE_URL);

        midPoint = new MidPoint(config);

        LoginPage login = midPoint.login();
        basicPage = login.login(USERNAME, PASSWORD);
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
}
