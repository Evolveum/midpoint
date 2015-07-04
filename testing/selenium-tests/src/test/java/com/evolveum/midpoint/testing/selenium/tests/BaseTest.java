package com.evolveum.midpoint.testing.selenium.tests;


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


/**
 * @author lazyman
 */
public class BaseTest {

    private static final String PARAM_SITE_URL = "site.url";
    private static final String PARAM_TIMEOUT_PAGE = "timeout.page";
    private static final String PARAM_TIMEOUT_WAIT = "timeout.wait";
    private static final String PARAM_TIMEOUT_SCRIPT = "timeout.script";
    private static final String PARAM_USER_LOGIN = "user.login";
    private static final String PARAM_USER_PASSWORD = "user.password";

    private static final Trace LOGGER = TraceManager.getTrace(BaseTest.class);
    private String siteUrl;
    protected String userLogin;
    protected String userPassword;
    protected WebDriver driver;

    public String getSiteUrl() {
        return siteUrl;
    }

    @BeforeClass(alwaysRun = true)
    public void beforeClass(ITestContext context) {
        siteUrl = context.getCurrentXmlTest().getParameter(PARAM_SITE_URL);
        userLogin = context.getCurrentXmlTest().getParameter(PARAM_USER_LOGIN);
        userPassword = context.getCurrentXmlTest().getParameter(PARAM_USER_PASSWORD);

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
        performLogin(driver, userLogin, userPassword);
    }

    protected void performLogin(WebDriver driver, String username, String password) {
        driver.get(siteUrl + "/login");

        driver.findElement(By.name("username")).clear();
        driver.findElement(By.name("username")).sendKeys(username);
        driver.findElement(By.name("password")).clear();
        driver.findElement(By.name("password")).sendKeys(password);

        driver.findElement(By.xpath("//input[@value='Sign in']")).click();
    }

    protected void performLogout(WebDriver driver) {
//todo
    }

    protected void logTestMethodStart(Trace LOGGER, String method) {
        LOGGER.info("===[" + method + " START]===");
    }

    protected void logTestMethodFinish(Trace LOGGER, String method) {
        LOGGER.info("===[" + method + " FINISH]===");
    }
}
