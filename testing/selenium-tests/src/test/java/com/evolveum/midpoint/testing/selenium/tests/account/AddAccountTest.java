package com.evolveum.midpoint.testing.selenium.tests.account;

import com.evolveum.midpoint.testing.selenium.tests.BaseTest;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;

/**
 * Created by honchar
 */
public class AddAccountTest extends BaseTest {
    private static final Trace LOGGER = TraceManager.getTrace(AddAccountTest.class);

    @Test
    public void addAccountTest() {
        logTestMethodStart(LOGGER, "addAccountTest");
        //log in to system as administrator
        performLogin(driver, userLogin, userPassword);

        //click Users menu item in the top vertical menu
        driver.findElement(By.cssSelector("html.no-js body div.navbar.navbar-default.navbar-fixed-top div div.navbar-collapse.collapse ul.nav.navbar-nav li.dropdown a.dropdown-toggle")).click();

        //click List Users menu item
        driver.findElement(By.cssSelector("li > a > span")).click();

        //Search for a TestUserName
        driver.findElement(By.name("basicSearch:searchText")).clear();
        driver.findElement(By.name("basicSearch:searchText")).sendKeys("TestUserName");
        driver.findElement(By.xpath("/html/body/div[4]/div/div[4]/form/span/a")).click();

        //click on the users link to open Edit user page
        waitToBeClickable(By.partialLinkText("TestUserName")).click();
        implicitWait(5);

        //click on the menu icon in the  right upper corner of the users list
        fluentWait(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[1]/div/div[2]/ul/li/a/b")).click();

        //click on the Add account menu item
        driver.findElement(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[1]/div[1]/div[2]/ul/li/ul/li[1]/a")).click();

        //select resource check box in the opened Select resource(s) window
        fluentWait(By.xpath("/html/body/div[6]/form/div/div[2]/div/div/div/div[2]/div/div/div/div/div/div[2]/div/table/tbody/tr/td[1]/div/input")).click();

        //Click Add resource(s) button
        driver.findElement(By.xpath("/html/body/div[6]/form/div/div[2]/div/div/div/div[2]/div/div/div/div/div/p/a")).click();

        //Fill in confirm password
        fluentWait(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[1]/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div[2]/div/div[1]/div[1]/input[2]")).clear();
        driver.findElement(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[1]/div[2]/div/div/div/div[3]/div[5]/div/div[2]/div/div[2]/div/div[1]/div[1]/input[2]")).sendKeys(userPassword);
        implicitWait(5);


        //Fill in mandatory ConnId Name field
        fluentWait(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[1]/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div[2]/div/div[1]/div[1]/input")).clear();
        driver.findElement(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[1]/div[2]/div/div/div/div[3]/div[1]/div/div[1]/div/div[2]/div/div[1]/div[1]/input")).sendKeys("Connid name");

        //Fill in user name
        fluentWait(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[1]/div[2]/div/div/div/div[3]/div[1]/div/div[5]/div/div[2]/div/div[1]/div[1]/input")).clear();
        driver.findElement(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[1]/div[2]/div/div/div/div[3]/div[1]/div/div[5]/div/div[2]/div/div[1]/div[1]/input")).sendKeys(userLogin);

        //Fill in password
        fluentWait(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[1]/div[2]/div/div/div/div[3]/div[5]/div/div[2]/div/div[2]/div/div[1]/div[1]/input[1]")).clear();
        fluentWait(By.xpath("/html/body/div[4]/div/form/div[3]/div[2]/div[1]/div[2]/div/div/div/div[3]/div[5]/div/div[2]/div/div[2]/div/div[1]/div[1]/input[1]")).sendKeys(userPassword);
        implicitWait(5);

        //Click Save button
        implicitWait(5);
        driver.findElement(By.xpath("/html/body/div[4]/div/form/div[6]/a[2]")).click();

        //Check is the message appears
        Assert.assertEquals("Success", fluentWait(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).getText());
        Assert.assertEquals("Save user (Gui)", driver.findElement(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[2]/ul/li/div/span")).getText());

        logTestMethodFinish(LOGGER, "addAccountTest");

    }
}
