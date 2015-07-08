package com.evolveum.midpoint.testing.selenium.tests;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;


import org.openqa.selenium.*;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author honchar
 */
public class LoginTest extends BaseTest {
    private static final Trace LOGGER = TraceManager.getTrace(LoginTest.class);

    /**
     * Logging in to midpoint administrator module with credentials administrator/5ecr3t
     */
    @Test
    public void loginTest() {
        logTestMethodStart(LOGGER, "login");

        performLogin(driver);

        Assert.assertEquals("welcome to midPoint", driver.findElement(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small")).getText());

        logTestMethodFinish(LOGGER, "login");
    }

    /**
     * Logging in to midpoint administrator module with correct login value and incorrect
     * password value
     */
    @Test
    public void loginWithIncorrectPasswordTest() {
        logTestMethodStart(LOGGER, "loginWithIncorrectPassword");

        performLogin(driver, userLogin, "incorrectPassword");

        Assert.assertEquals("Invalid username and/or password.", driver.findElement(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div/div/span")).getText());

        logTestMethodFinish(LOGGER, "loginWithIncorrectPassword");
    }

    /**
     * Logging in to midpoint administrator module with incorrect login value and correct
     * password value
     */
    @Test
    public void loginWithIncorrectUsernameTest() {
        logTestMethodStart(LOGGER, "loginWithIncorrectUsername");

        performLogin(driver, "incorrectUsername", userPassword);

        Assert.assertEquals("Invalid username and/or password.", driver.findElement(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div/div/span")).getText());

        logTestMethodFinish(LOGGER, "loginWithIncorrectUsername");
    }
}
