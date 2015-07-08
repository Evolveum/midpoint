package com.evolveum.midpoint.testing.selenium.tests.user;

import com.evolveum.midpoint.testing.selenium.tests.BaseTest;
import com.evolveum.midpoint.testing.selenium.tests.LoginTest;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by honchar
 */
public class CreateUserTest extends BaseTest {
    private static final Trace LOGGER = TraceManager.getTrace(LoginTest.class);

    /**
     * Creating new user in the midPoint administrator module
     */
    @Test
    public void addNewUserTest(){
        //log in to system asadministrator
        performLogin(driver, userLogin, userPassword);

        //click Users menu item in the top vertical menu
        driver.findElement(By.cssSelector("html.no-js body div.navbar.navbar-default.navbar-fixed-top div div.navbar-collapse.collapse ul.nav.navbar-nav li.dropdown a.dropdown-toggle")).click();

        //click New user menu item
        driver.findElement(By.xpath("//li[2]/a/span")).click();

        //Clear and fill in the Name mandatory field
        driver.findElement(By.name("userForm:body:containers:0:container:properties:0:property:values:0:value:valueContainer:input:input")).clear();
        driver.findElement(By.name("userForm:body:containers:0:container:properties:0:property:values:0:value:valueContainer:input:input")).sendKeys("TestUserName");

        //Click on Save button
        driver.findElement(By.xpath("/html/body/div[4]/div/form/div[5]/a[2]")).click();

        //Check is the message appears
        Assert.assertEquals("Success", driver.findElement(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).getText());
        Assert.assertEquals("Save user (Gui)", driver.findElement(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[2]/ul/li/div/span")).getText());
    }

    /**
     * Attemp to create user without mandatory field Name
     */
    @Test
    public void createUserWithoutNameTest() {
        //click Users menu item in the top vertical menu
        driver.findElement(By.cssSelector("html.no-js body div.navbar.navbar-default.navbar-fixed-top div div.navbar-collapse.collapse ul.nav.navbar-nav li.dropdown a.dropdown-toggle")).click();

        //click New user menu item
        driver.findElement(By.xpath("//li[2]/a/span")).click();

        //Clear Name mandatory field
        driver.findElement(By.name("userForm:body:containers:0:container:properties:0:property:values:0:value:valueContainer:input:input")).clear();

        //Click on Save button
        driver.findElement(By.xpath("/html/body/div[4]/div/form/div[5]/a[2]")).click();

        String errorMessage = "No name in new object null as produced by template null in iteration 0, we cannot process an object without a name: No name in new object null as produced by template null in iteration 0, we cannot process an object without a name";
        String messageXpath = "/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span";
        Assert.assertEquals(errorMessage, driver.findElement(By.xpath(messageXpath)).getText());

    }
}

