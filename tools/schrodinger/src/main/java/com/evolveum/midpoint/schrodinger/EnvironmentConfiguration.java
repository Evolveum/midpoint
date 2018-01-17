package com.evolveum.midpoint.schrodinger;

import org.apache.commons.lang3.Validate;

/**
 * Created by Viliam Repan (lazyman).
 */
public class EnvironmentConfiguration {

    private WebDriver driver = WebDriver.CHROME;

    private String baseUrl;

    public EnvironmentConfiguration driver(final WebDriver driver) {
        Validate.notNull(driver, "Web driver must not be null");

        this.driver = driver;
        return this;
    }

    public EnvironmentConfiguration baseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public WebDriver getDriver() {
        return driver;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void validate() {
        Validate.notNull(baseUrl, "Base url must not be null");
    }
}
