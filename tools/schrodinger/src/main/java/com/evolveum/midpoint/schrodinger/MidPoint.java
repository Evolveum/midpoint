/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger;

import com.codeborne.selenide.Configuration;
import com.evolveum.midpoint.schrodinger.component.LoggedUser;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.page.login.SamlSelectPage;
import org.apache.commons.lang3.Validate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPoint {

    private static EnvironmentConfiguration environment;

    public static final long TIMEOUT_DEFAULT_2_S = 2000;
    public static final long TIMEOUT_MEDIUM_6_S = 6000;
    public static final long TIMEOUT_LONG_1_M = 60000;
    public static final long TIMEOUT_EXTRA_LONG_1_M = 180000;

    private static final String SCHRODINGER_PROPERTIES = "../../testing/schrodingertest/src/test/resources/configuration/schrodinger.properties";

    private String username;
    private String password;

    private String baseUrl;
    private String webDriver;
    private String webdriverLocation;
    private boolean headless;

    public MidPoint(EnvironmentConfiguration environment) throws IOException {
        Validate.notNull(environment, "Environment configuration must not be null");

        MidPoint.environment = environment;

        init();
    }

    private void init() throws IOException {
        fetchProperties();
        environment.baseUrl(baseUrl);
        environment.validate();

        System.setProperty(webDriver, webdriverLocation);
        System.setProperty("selenide.browser", environment.getDriver().name().toLowerCase());
        System.setProperty("selenide.baseUrl", environment.getBaseUrl());

        Configuration.headless = headless;
        Configuration.timeout = 6000L;
    }

    public FormLoginPage formLogin() {
        return new FormLoginPage();
    }

    public SamlSelectPage samlSelect() {
        return new SamlSelectPage();
    }

    public MidPoint logout() {
        new LoggedUser().logout();

        return this;
    }

    private void fetchProperties() throws IOException {

        Properties schrodingerProperties = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(SCHRODINGER_PROPERTIES);
            schrodingerProperties.load(input);

            webDriver = schrodingerProperties.getProperty("webdriver");
            webdriverLocation = schrodingerProperties.getProperty("webdriverLocation");
            username = schrodingerProperties.getProperty("username");
            password = schrodingerProperties.getProperty("password");
            baseUrl = schrodingerProperties.getProperty("base_url");

            headless = Boolean.parseBoolean(schrodingerProperties.getProperty("headlessStart"));
        } catch (IOException e) {
            throw new IOException("An exception was thrown during Schrodinger initialization " + e.getLocalizedMessage());
        }
    }

    public String getPassword() {

        return this.password;
    }

    public String getUsername() {

        return this.username;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
