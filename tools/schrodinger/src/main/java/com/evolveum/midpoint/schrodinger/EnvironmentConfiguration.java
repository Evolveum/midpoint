/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger;

import org.apache.commons.lang3.Validate;

/**
 * Created by Viliam Repan (lazyman).
 */
public class EnvironmentConfiguration {

    private WebDriver driver = WebDriver.CHROME;

    private String driverLocation;

    private boolean headless;

    private String baseUrl;

    public EnvironmentConfiguration driver(final WebDriver driver) {
        Validate.notNull(driver, "Web driver must not be null");

        this.driver = driver;
        return this;
    }

    public EnvironmentConfiguration baseUrl(final String baseUrl) {
        Validate.notNull(driver, "Base url must not be null");

        this.baseUrl = baseUrl;
        return this;
    }

    public EnvironmentConfiguration driverLocation(final String driverLocation) {
        this.driverLocation = driverLocation;
        return this;
    }

    public EnvironmentConfiguration headless(final boolean headless) {
        this.headless = headless;
        return this;
    }

    public WebDriver getDriver() {
        return driver;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getDriverLocation() {
        return driverLocation;
    }

    public boolean isHeadless() {
        return headless;
    }

    public void validate() {
        Validate.notNull(baseUrl, "Base url must not be null");
    }
}
