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
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertTrue;

/**
 * @author lazyman
 */
public class LoginLogoutTest extends BaseTest {

    private static final Trace LOGGER = TraceManager.getTrace(LoginLogoutTest.class);

    @Test
    public void launchSite() {
        logTestMethodStart(LOGGER, "launchSite");
        driver.get(getSiteUrl());

        //check if there are login form elements
        driver.findElement(By.id("userName"));
        driver.findElement(By.id("userPass"));
        driver.findElement(By.cssSelector("input.button"));

        assertTrue(driver.getCurrentUrl().startsWith(getSiteUrl() + "/login"));

        logTestMethodFinish(LOGGER, "launchSite");
    }

    @Test
    public void login() {
        logTestMethodStart(LOGGER, "login");

        performLogin(driver);

        //todo asserts

        logTestMethodFinish(LOGGER, "login");
    }

    @Test
    public void loginFail() {
        logTestMethodStart(LOGGER, "loginFail");

        performLogin(driver, "administrator", "badPassword");

        //todo asserts

        logTestMethodFinish(LOGGER, "loginFail");
    }

    @Test
    public void logout() {
        logTestMethodStart(LOGGER, "logout");

        performLogin(driver);
        performLogout(driver);

        //todo asserts

        logTestMethodFinish(LOGGER, "logout");
    }
}
