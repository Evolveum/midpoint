/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class JenkinsTest extends NinjaSpringTest {

    private static final Trace LOGGER = TraceManager.getTrace(JenkinsTest.class);

    @BeforeClass(alwaysRun = true, dependsOnMethods = "springTestContextBeforeTestClass")
    protected void springTestContextPrepareTestInstance() throws Exception {
        LOGGER.info("springTestContextPrepareTestInstance started");
        super.springTestContextPrepareTestInstance();
        LOGGER.info("springTestContextPrepareTestInstance finished");
    }

    @BeforeClass(alwaysRun = true, dependsOnMethods = "springTestContextPrepareTestInstance")
    protected void custom() throws Exception {
        LOGGER.info("custom started");
        super.springTestContextPrepareTestInstance();
        LOGGER.info("custom finished");
    }

    @BeforeClass
    @Override
    public void beforeClass() throws Exception {
        LOGGER.info("beforeClass started");
        super.beforeClass();
        LOGGER.info("beforeClass finished");
    }

    @BeforeClass

    @Test
    public void emptyTest() throws Exception {

    }
}
