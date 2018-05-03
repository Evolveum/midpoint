package com.evolveum.midpoint.schrodinger;

import com.codeborne.selenide.Configuration;
import com.evolveum.midpoint.schrodinger.component.LoggedUser;
import com.evolveum.midpoint.schrodinger.page.LoginPage;
import org.apache.commons.lang3.Validate;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPoint {

    private EnvironmentConfiguration environment;

    public static long TIMEOUT_DEFAULT = 2000;

    public MidPoint(EnvironmentConfiguration environment) {
        Validate.notNull(environment, "Environment configuration must not be null");

        this.environment = environment;

        init();
    }

    private void init() {
        environment.validate();
      //  System.setProperty("webdriver.chrome.driver","C:\\Users\\matus\\chromedriver\\chromedriver.exe"); // TODO workaround, find proper way how to resolve
        System.setProperty("selenide.browser", environment.getDriver().name().toLowerCase());
        System.setProperty("selenide.baseUrl", environment.getBaseUrl());

        Configuration.timeout = 6000L;
    }

    public LoginPage login() {
        return new LoginPage();
    }

    public MidPoint logout() {
        new LoggedUser().logout();

        return this;
    }
}
