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
