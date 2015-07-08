package com.evolveum.midpoint.testing.selenium.tests.user;

import com.evolveum.midpoint.testing.selenium.tests.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by honchar
 */
public class DeleteUserTest extends BaseTest{

    @Test
    public void deleteUserTest() throws Exception {
        //log in to system as administrator
        performLogin(driver, userLogin, userPassword);

        //click Users menu item in the top vertical menu
        driver.findElement(By.cssSelector("html.no-js body div.navbar.navbar-default.navbar-fixed-top div div.navbar-collapse.collapse ul.nav.navbar-nav li.dropdown a.dropdown-toggle")).click();

        //click List Users menu item
        driver.findElement(By.cssSelector("li > a > span")).click();


        //Search for a TestUser
        driver.findElement(By.name("basicSearch:searchText")).clear();
        driver.findElement(By.name("basicSearch:searchText")).sendKeys("TestUser");

        //Select TestUser checkbox
        driver.findElement(By.name("table:table:body:rows:1:cells:1:cell:check")).click();

        //Click Settings drop-down menu
        driver.findElement(By.xpath("/html/body/div[4]/div/form/div[2]/table/thead/tr/th[9]/div/span[1]/ul/li/a")).click();
        //Click Delete menu item
        driver.findElement(By.xpath("/html/body/div[4]/div/form/div[2]/table/thead/tr/th[9]/div/span[1]/ul/li/ul/li[6]/a")).click();
        //Click Yes in the confirmation window
        driver.findElement(By.xpath("/html/body/div[6]/form/div/div[2]/div/div/div/div[2]/div/div/div/div/p[2]/a[1]")).click();

        //Check is the message appears
        Assert.assertEquals("Success", driver.findElement(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).getText());
        Assert.assertEquals("Delete users (Gui)", driver.findElement(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[2]/ul/li/div/span")).getText());
    }

}
