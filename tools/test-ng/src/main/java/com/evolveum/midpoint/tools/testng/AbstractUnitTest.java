/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Base test class providing basic {@link MidpointTestMixin} implementation.
 * Can be extended by any unit test class that otherwise doesn't extend anything.
 */
public abstract class AbstractUnitTest implements MidpointTestMixin {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @BeforeClass
    public void displayTestClassTitle() {
        displayTestTitle("Initializing TEST CLASS: " + getClass().getName());
    }

    @AfterClass
    public void displayTestClassFooter() {
        displayTestFooter("Finishing with TEST CLASS: " + getClass().getName());
    }

    @BeforeMethod
    public void startTestContext(ITestResult testResult) {
        SimpleMidpointTestContext context = SimpleMidpointTestContext.create(testResult);
        displayTestTitle(context.getTestName());
    }

    @AfterMethod
    public void finishTestContext(ITestResult testResult) {
        SimpleMidpointTestContext context = SimpleMidpointTestContext.get();
        SimpleMidpointTestContext.destroy();
        displayTestFooter(context.getTestName(), testResult);
    }

    @Override
    @Nullable
    public MidpointTestContext getTestContext() {
        return SimpleMidpointTestContext.get();
    }

    @Override
    public Logger logger() {
        return logger;
    }
}
