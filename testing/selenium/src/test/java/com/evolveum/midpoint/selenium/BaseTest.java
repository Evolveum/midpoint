/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.selenium;


import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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

    private static final Trace LOGGER = TraceManager.getTrace(BaseTest.class);
    private static String SITE_URL;
    protected WebDriver driver;

    public String getSiteUrl() {
        return SITE_URL;
    }

    @BeforeClass(alwaysRun = true)
    public void beforeClass(ITestContext context) {
        SITE_URL = context.getCurrentXmlTest().getParameter("site.url");

        LOGGER.info("Site url: {0}", new Object[]{SITE_URL});

        driver = new FirefoxDriver();
        driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() {
        driver.quit();
    }

    protected void performLogin(WebDriver driver) {
        performLogin(driver, "administrator", "5ecr3t");
    }

    protected void performLogin(WebDriver driver, String username, String password) {
        driver.get(SITE_URL + "/login");

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
