package com.evolveum.midpoint.testing.schrodinger;

import com.evolveum.midpoint.schrodinger.EnvironmentConfiguration;
import com.evolveum.midpoint.schrodinger.MidPoint;

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
