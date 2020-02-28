/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger;

import java.io.IOException;

import com.codeborne.selenide.Configuration;
import org.apache.commons.lang3.Validate;

import com.evolveum.midpoint.schrodinger.component.LoggedUser;
import com.evolveum.midpoint.schrodinger.page.login.FormLoginPage;
import com.evolveum.midpoint.schrodinger.page.login.SamlSelectPage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPoint {

    private static EnvironmentConfiguration configuration;

    public static final long TIMEOUT_DEFAULT_2_S = 2000;
    public static final long TIMEOUT_MEDIUM_6_S = 6000;
    public static final long TIMEOUT_LONG_1_M = 60000;
    public static final long TIMEOUT_EXTRA_LONG_1_M = 180000;

    private String baseUrl;

    public MidPoint(EnvironmentConfiguration configuration) throws IOException {
        Validate.notNull(configuration, "Environment configuration must not be null");

        this.configuration = configuration;

        init();
    }

    private void init() throws IOException {
        configuration.baseUrl(baseUrl);
        configuration.validate();

        System.setProperty(configuration.getDriver().getDriver(), configuration.getDriverLocation());
        System.setProperty("selenide.browser", configuration.getDriver().name().toLowerCase());
        System.setProperty("selenide.baseUrl", configuration.getBaseUrl());

        Configuration.headless = configuration.isHeadless();
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
}
