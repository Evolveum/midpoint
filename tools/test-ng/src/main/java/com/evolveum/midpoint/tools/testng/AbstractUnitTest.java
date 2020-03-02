/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Base test class providing basic {@link UnitTestMixin} implementation.
 * Can be extended by any unit test class that otherwise doesn't extend anything.
 */
public abstract class AbstractUnitTest implements UnitTestMixin {

    private static final ThreadLocal<ITestResult> TEST_CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @BeforeMethod
    public void startTestContext(ITestResult testResult) {
        Class<?> testClass = testResult.getMethod().getTestClass().getRealClass();
        String testMethodName = testResult.getMethod().getMethodName();
        displayTestTitle(testClass.getSimpleName() + "." + testMethodName);

        TEST_CONTEXT_THREAD_LOCAL.set(testResult);
    }

    @AfterMethod
    public void finishTestContext(ITestResult testResult) {
        TEST_CONTEXT_THREAD_LOCAL.remove();

        displayDefaultTestFooter(testResult);
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    @NotNull
    public String contextName() {
        ITestResult context = TEST_CONTEXT_THREAD_LOCAL.get();
        return context != null
                ? getTestName(context)
                : getClass().getSimpleName();
    }

    @Override
    public String getTestNameShort() {
        return TEST_CONTEXT_THREAD_LOCAL.get().getMethod().getMethodName();
    }
}
