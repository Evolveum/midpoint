package com.evolveum.midpoint.testing.selenium.tests.resource;

import com.evolveum.midpoint.testing.selenium.tests.BaseTest;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

/**
 * Created by honchar
 */
public class AddResourceTest extends BaseTest {
    private static final Trace LOGGER = TraceManager.getTrace(AddResourceTest.class);

    /**
     * Import databasetable resource from the xml file
     */
    @Test
    public void importResourceTest() {
        logTestMethodStart(LOGGER, "importResourceTest");

        performLogin(driver);

        //Click Configuration menu item
        driver.findElement(By.xpath("/html/body/div[3]/div/div[2]/ul[1]/li[9]/a")).click();

        //Click Import object from the drop down menu
        driver.findElement(By.xpath("//li[9]/ul/li[2]/a/span")).click();

        //Select Overwrite existing object check box
        driver.findElement(By.name("importOptions:overwriteExistingObject")).click();

        //Select Get objects from File radio button
        driver.findElement(By.name("importRadioGroup")).click();

        //Click Browse button to select file for upload
        fluentWait(By.xpath("/html/body/div[4]/div/form/div[5]/div/input")).click();

        //upload localhost-dbtable-simple.xml file
        uploadFile(samplesFolderPath + "\\resources\\databasetable\\localhost-dbtable-simple.xml");

        //Click on Import object button
        waitToBeClickable(By.xpath("/html/body/div[4]/div/form/div[6]/a")).click();

        //Check if Success message appears
        Assert.assertEquals("Success", driver.findElement(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).getText());
        Assert.assertEquals("Import file (Gui)", driver.findElement(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[2]/ul/li/div/span")).getText());


        logTestMethodFinish(LOGGER, "importResourceTest");

    }
}
