/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger;

/**
 * Created by Viliam Repan (lazyman).
 */
public enum WebDriver {

    CHROME("webdriver.chrome.driver"),

    // todo
    FIREFOX(null),

    HTMLUNIT(null),

    IE(null),

    OPERA(null),

    PHANTOMJS(null);

    private String driver;

    WebDriver(String driver) {
        this.driver = driver;
    }

    public String getDriver() {
        return driver;
    }
}
