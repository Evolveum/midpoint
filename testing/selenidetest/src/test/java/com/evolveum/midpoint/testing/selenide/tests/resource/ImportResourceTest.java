package com.evolveum.midpoint.testing.selenide.tests.resource;

import com.evolveum.midpoint.testing.selenide.tests.BaseTest;
import com.evolveum.midpoint.testing.selenide.tests.Util;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;

import java.io.File;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Kate on 09.08.2015.
 */
@Component
public class ImportResourceTest extends BaseTest {

    @Autowired
    ResourceUtil resourceUtil;

    @Autowired
    Util util;

    /**
     * Import OpenDJ resource test (file "opendj-localhost-resource-sync-no-extension-advanced.xml" is used)
     */
    @Test
    public void importResourceTest(){

        //click Configuration menu
        $(By.xpath("/html/body/div[3]/div/div[2]/ul[1]/li[8]/a"))
                .shouldHave(text("Configuration")).click();

        //click Import object menu item
        $(By.linkText("Import object")).click();

        //select Overwrite existing object check box
        $(By.name("importOptions:overwriteExistingObject")).setSelected(true);

        //Specify the file to be uploaded
        File test = new File(resourceUtil.getTestResourcePath());
        $(By.name("input:inputFile:fileInput")).uploadFile(test);

        //click Import object button
        $(By.xpath("/html/body/div[4]/div/form/div[6]/a")).shouldHave(text("Import object")).click();

        //check if Success message appears after resource importing
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).shouldHave(text("Success"));

        //open Resources -> List Resources
        $(By.xpath("/html/body/div[3]/div/div[2]/ul[1]/li[4]/a")).shouldHave(text("Resources")).click();
        $(By.linkText("List resources")).click();

        //search for resource in resources list
        util.searchForElement(resourceUtil.getTestResourceName(), "/html/body/div[4]/div/form[1]/span/a");
        $(By.xpath("/html/body/div[4]/div/form[2]/div[2]/table/tbody/tr/td[2]/div/a/span"))
                .shouldBe(visible);

    }




}
