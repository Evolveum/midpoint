/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger;

import com.evolveum.midpoint.schrodinger.EnvironmentConfiguration;
import com.evolveum.midpoint.schrodinger.MidPoint;

import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.page.configuration.AboutPage;
import org.apache.commons.lang3.ArrayUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.io.IOException;


/**
 * Created by matus on 7/10/2018.
 */
public class TestSuiteConfig extends TestBase {

    private static final Logger LOG = LoggerFactory.getLogger(TestSuiteConfig.class);

    @BeforeSuite
    public void init() throws IOException {

        EnvironmentConfiguration config = new EnvironmentConfiguration();
        midPoint = new MidPoint(config);
    }

  @AfterSuite
    public void cleanUp() {

    }
}
