/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.selenium;


import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.fail;

/**
 * @author lazyman
 */
public class BaseTest {

    private static final String PARAM_SITE_URL = "site.url";
    private static final String PARAM_TIMEOUT_PAGE = "timeout.page";
    private static final String PARAM_TIMEOUT_WAIT = "timeout.wait";
    private static final String PARAM_TIMEOUT_SCRIPT = "timeout.script";

    private static final Trace LOGGER = TraceManager.getTrace(BaseTest.class);
    private String siteUrl;
    protected WebDriver driver;

    public String getSiteUrl() {
        return siteUrl;
    }

    @BeforeClass(alwaysRun = true)
    public void beforeClass(ITestContext context) {
        siteUrl = context.getCurrentXmlTest().getParameter(PARAM_SITE_URL);

        int wait = getTimeoutParameter(context, PARAM_TIMEOUT_WAIT, 1);
        int page = getTimeoutParameter(context, PARAM_TIMEOUT_PAGE, 1);
        int script = getTimeoutParameter(context, PARAM_TIMEOUT_SCRIPT, 1);
        LOGGER.info("Site url: '{}'. Timeouts: implicit wait({}), page load ({}), script({})",
                new Object[]{siteUrl, wait, page, script});

        driver = new FirefoxDriver();
        WebDriver.Timeouts timeouts = driver.manage().timeouts();
        timeouts.implicitlyWait(wait, TimeUnit.SECONDS);
        timeouts.pageLoadTimeout(page, TimeUnit.SECONDS);
        timeouts.setScriptTimeout(script, TimeUnit.SECONDS);
    }

    private int getTimeoutParameter(ITestContext context, String param, int defaultValue) {
        String value = context.getCurrentXmlTest().getParameter(param);
        if (StringUtils.isEmpty(value) || !value.matches("[0]*[1-9]+[0-9]*")) {
            return defaultValue;
        }

        return Integer.parseInt(value);
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() {
        driver.quit();
    }

    protected void performLogin(WebDriver driver) {
        performLogin(driver, "administrator", "5ecr3t");
    }

    protected void performLogin(WebDriver driver, String username, String password) {
        driver.get(siteUrl + "/login");

        driver.findElement(By.id("userName")).sendKeys(username);
        driver.findElement(By.id("userPass")).sendKeys(password);

        driver.findElement(By.cssSelector("input.button")).click();
    }

    protected void performLogout(WebDriver driver) {
        WebElement logout = driver.findElement(By.xpath("//div[@id=\"login-box\"]/a[1]"));
        if (logout == null) {
            fail("Couldn't find logout link.");
        }
        logout.click();
    }

    protected void logTestMethodStart(Trace LOGGER, String method) {
        LOGGER.info("===[" + method + " START]===");
    }

    protected void logTestMethodFinish(Trace LOGGER, String method) {
        LOGGER.info("===[" + method + " FINISH]===");
    }
}
