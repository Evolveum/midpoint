package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.page.LoginPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LoggedUser {

    public LoginPage logout() {
        SelenideElement userMenu =  $(".dropdown.user.user-menu");

        userMenu.$(By.cssSelector(".dropdown-toggle")).click();
        userMenu.$(By.cssSelector(".user-footer"))
                .$(Schrodinger.byElementAttributeValue("input", "type", "submit")).click();

        //todo implement

        return new LoginPage();
    }
}
