package com.evolveum.midpoint.testing.selenium.tests;


import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.google.common.base.Function;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.*;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
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
    private static final String MIDPOINT_SAMPLES_FOLDER_PATH = "midpoint.samples.folder.path";

    private static final Trace LOGGER = TraceManager.getTrace(BaseTest.class);
    private String siteUrl;
    protected String userLogin;
    protected String userPassword;
    protected String samplesFolderPath;
    protected WebDriver driver;
    private WebDriverWait waitDriver;


    public String getSiteUrl() {
        return siteUrl;
    }

    @BeforeClass(alwaysRun = true)
    public void beforeClass(ITestContext context) {
        siteUrl = context.getCurrentXmlTest().getParameter(PARAM_SITE_URL);
        userLogin = context.getCurrentXmlTest().getParameter(PARAM_USER_LOGIN);
        userPassword = context.getCurrentXmlTest().getParameter(PARAM_USER_PASSWORD);
        userPassword = context.getCurrentXmlTest().getParameter(PARAM_USER_PASSWORD);
        samplesFolderPath = context.getCurrentXmlTest().getParameter(MIDPOINT_SAMPLES_FOLDER_PATH);

        int wait = getTimeoutParameter(context, PARAM_TIMEOUT_WAIT, 5);
        int page = getTimeoutParameter(context, PARAM_TIMEOUT_PAGE, 5);
        int script = getTimeoutParameter(context, PARAM_TIMEOUT_SCRIPT,5);
        LOGGER.info("Site url: '{}'. Timeouts: implicit wait({}), page load ({}), script({})",
                new Object[]{siteUrl, wait, page, script});

        driver = new FirefoxDriver();

        WebDriver.Timeouts timeouts = driver.manage().timeouts();
        timeouts.implicitlyWait(wait, TimeUnit.SECONDS);
        timeouts.pageLoadTimeout(page, TimeUnit.SECONDS);
        timeouts.setScriptTimeout(script, TimeUnit.SECONDS);

        waitDriver  = new WebDriverWait(driver, 10);
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

    /**
     * Set parameter string to the system's clipboard.
     */
    public static void setClipboardData(String string) {
        //StringSelection is a class that can be used for copy and paste operations.
        StringSelection stringSelection = new StringSelection(string);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

    /**
     * Upload file from local machine
     * @param fileLocation
     */
    public void uploadFile(String fileLocation) {
        try {
            //Setting clipboard with file location
            setClipboardData(fileLocation);

            //native key strokes for CTRL, V and ENTER keys
            Robot robot = new Robot();

            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_V);

            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            Thread.sleep(3000);
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);


        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    public void implicitWait(long time){
        driver.manage().timeouts().implicitlyWait(time, TimeUnit.SECONDS);
    }

    public WebElement fluentWait(final By locator) {
        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver)
                .withTimeout(30, TimeUnit.SECONDS)
                .pollingEvery(5, TimeUnit.SECONDS)
                .ignoring(NoSuchElementException.class);

        WebElement element = wait.until(
                new Function<WebDriver, WebElement>() {
                    public WebElement apply(WebDriver driver) {
                        return driver.findElement(locator);
                    }
                }
        );
        return element;
    }

    /**
     * Returns WebElement after it becomes clickable
     */
    public WebElement waitToBeClickable(By by) {
        WebElement element = waitDriver.until(ExpectedConditions.elementToBeClickable(by));
        return element;
    }

    /**
     * Returns WebElement after the text was entered to it
     */
    public boolean waitForTextPresented(By by, String text) {
        return waitDriver.until(ExpectedConditions.textToBePresentInElementLocated(by, text));
    }

    protected void logTestMethodStart(Trace LOGGER, String method) {
        LOGGER.info("===[" + method + " START]===");
    }

    protected void logTestMethodFinish(Trace LOGGER, String method) {
        LOGGER.info("===[" + method + " FINISH]===");
    }
}
