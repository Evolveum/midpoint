/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Base test class for tests integrated with Spring providing {@link UnitTestMixin} implementation.
 * Can be extended by any unit test class that would otherwise extend
 * {@link AbstractTestNGSpringContextTests}.
 */
public abstract class AbstractSpringTest extends AbstractTestNGSpringContextTests
        implements UnitTestMixin {

    private static final ThreadLocal<ITestResult> TEST_CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * Hides parent's logger, but that one is from commons-logging and we don't want that.
     */
    protected final Trace logger = TraceManager.getTrace(getClass());

    @BeforeMethod
    public void startTestContext(ITestResult testResult) {
        Class<?> testClass = testResult.getMethod().getTestClass().getRealClass();
        String testMethodName = testResult.getMethod().getMethodName();
        TestUtil.displayTestTitle(testClass.getSimpleName() + "." + testMethodName);

        TEST_CONTEXT_THREAD_LOCAL.set(testResult);
    }

    @AfterMethod
    public void finishTestContext(ITestResult testResult) {
        TEST_CONTEXT_THREAD_LOCAL.remove();

        displayDefaultTestFooter(testResult);
    }

    @Override
    public String getTestNameShort() {
        return TEST_CONTEXT_THREAD_LOCAL.get().getMethod().getMethodName();
    }
}
