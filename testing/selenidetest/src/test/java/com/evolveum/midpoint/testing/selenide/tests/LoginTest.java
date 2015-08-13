 package com.evolveum.midpoint.testing.selenide.tests;

import org.openqa.selenium.By;
import org.testng.annotations.Test;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.Condition.*;

/**
 * Created by Kate on 07.08.2015.
 */
public class LoginTest extends BaseTest{

    /**
     * Log in to system as administrator/5ecr3t
     */
    @Test
    public void loginWithCorrectCredentialsTest(){
        //perform login
        login(siteUrl, userLogin, userPassword);

        //check if welcome message appears after user logged in
        $(By.cssSelector("html.no-js body div.mp-main-container div.row.mainContainer div.page-header h1 small")).shouldHave(text("welcome to midPoint"));
    }
}
