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


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author lazyman
 */
public class BaseTests {

    private static String SITE_URL;
    private WebDriver driver;

    @BeforeSuite(alwaysRun = true)
    public void setupBeforeSuite(ITestContext context) {
        SITE_URL = context.getCurrentXmlTest().getParameter("site.url");

        driver = new FirefoxDriver();
        driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
    }

    @AfterSuite(alwaysRun = true)
    public void setupAfterSuite() {
        driver.quit();
    }

    @Test
    public void launchSite() {
        driver.get(SITE_URL);

        //todo asserts
    }

    @Test
    public void login() {
        driver.get(SITE_URL + "/login");

        driver.findElement(By.id("userName")).sendKeys("administrator");
        driver.findElement(By.id("userPass")).sendKeys("5ecr3t");

        driver.findElement(By.cssSelector("input.button")).click();

        //todo asserts
    }

    @Test
    public void loginFail() {
        driver.get(SITE_URL + "/login");

        driver.findElement(By.id("userName")).sendKeys("administrator");
        driver.findElement(By.id("userPass")).sendKeys("badpassword");

        driver.findElement(By.cssSelector("input.button")).click();

        //todo asserts
    }

    @Test
    public void logout() {
        driver.findElement(By.linkText("Logout")).click();

        //todo asserts
    }
}
