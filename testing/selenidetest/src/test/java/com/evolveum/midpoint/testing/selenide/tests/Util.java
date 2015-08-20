package com.evolveum.midpoint.testing.selenide.tests;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;

import java.io.File;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.close;
import static com.codeborne.selenide.Selenide.open;

/**
 * Created by Kate on 13.08.2015.
 */
@Component
public class Util {
    public static final String SITE_URL = "/midpoint";
    public static final String ADMIN_LOGIN = "administrator";
    public static final String ADMIN_PASSWORD = "5ecr3t";

    /**
     * Looks for the element with specified searchText
     * and returns the first element from the search results
     * @param searchText
     * @return
     */
    public void searchForElement(String searchText, String searchButtonXpath){
        //search for element in search form
        $(By.name("basicSearch:searchText")).shouldBe(visible).setValue(searchText);
        $(By.xpath(searchButtonXpath)).shouldHave(text("Search")).click();
    }

    public void importObjectFromFile(String filePath){
        //click Configuration menu
        $(By.xpath("/html/body/div[3]/div/div[2]/ul[1]/li[8]/a"))
                .shouldHave(text("Configuration")).click();

        //click Import object menu item
        $(By.linkText("Import object")).click();

        //select Overwrite existing object check box
        $(By.name("importOptions:overwriteExistingObject")).setSelected(true);

        //Specify the file to be uploaded
        File test = new File(filePath);
        $(By.name("input:inputFile:fileInput")).uploadFile(test);

        //click Import object button
        $(By.xpath("/html/body/div[4]/div/form/div[6]/a")).shouldHave(text("Import object")).click();

        //check if Success message appears after resource importing
        $(By.xpath("/html/body/div[4]/div/div[2]/div[1]/ul/li/div/div[1]/div[1]/span")).shouldHave(text("Success"));

    }

    public void logout(){
        //click administrator menu in the upper right corner of the window
        $(By.xpath("/html/body/div[3]/div/div[2]/ul[2]/li/a"))
                .shouldBe(visible).click();
        //click on Log out menu item
        $(By.linkText("Log out")).shouldBe(visible).click();

    }
}
