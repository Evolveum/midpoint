package com.evolveum.midpoint.schrodinger;

import com.codeborne.selenide.Configuration;
import com.evolveum.midpoint.schrodinger.component.LoggedUser;
import com.evolveum.midpoint.schrodinger.page.LoginPage;
import org.apache.commons.lang3.Validate;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPoint {

    private static EnvironmentConfiguration environment;

    public static long TIMEOUT_DEFAULT = 2000;
    public static long TIMEOUT_MEDIUM = 6000;
    public static long TIMEOUT_LONG = 60000;

    public MidPoint(EnvironmentConfiguration environment) {
        Validate.notNull(environment, "Environment configuration must not be null");

        this.environment = environment;

        init();
    }

    private void init() {
        environment.validate();
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\matus\\chromedriver\\chromedriver.exe"); // TODO workaround, find proper way how to resolve
        System.setProperty("selenide.browser", environment.getDriver().name().toLowerCase());
        System.setProperty("selenide.baseUrl", environment.getBaseUrl());

        Configuration.timeout = 6000L;
    }

    public LoginPage login() {
        return new LoginPage();
    }

    public static FluentWait waitWithIgnoreMissingElement() {

        FluentWait wait = new FluentWait(environment.getDriver())
                .withTimeout(TIMEOUT_MEDIUM, MILLISECONDS)
                .pollingEvery(100, MILLISECONDS)
                .ignoring(NoSuchElementException.class).ignoring(org.openqa.selenium.TimeoutException.class);

        return wait;
    }


    public MidPoint logout() {
        new LoggedUser().logout();

        return this;
    }
}
