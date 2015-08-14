package com.evolveum.midpoint.testing.selenide.tests;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;
import org.springframework.stereotype.Component;
import org.testng.annotations.Test;

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
    private String siteUrl;
    private String adminUserLogin;
    private String adminUserPassword;



    /**
     *close browser window
     */
    @Test
    public void closeTest(){
        close();
    }
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
//        $(By.xpath("/html/body/div[4]/div/form[1]/span/a")).shouldHave(text("Search")).click();
//        /html/body/div[4]/div/div[4]/form/span/a
    }
    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getAdminUserLogin() {
        return adminUserLogin;
    }

    public void setAdminUserLogin(String adminUserLogin) {
        this.adminUserLogin = adminUserLogin;
    }

    public String getAdminUserPassword() {
        return adminUserPassword;
    }

    public void setAdminUserPassword(String adminUserPassword) {
        this.adminUserPassword = adminUserPassword;
    }

}
