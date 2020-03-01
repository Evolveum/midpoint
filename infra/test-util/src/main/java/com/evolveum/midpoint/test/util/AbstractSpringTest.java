/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.evolveum.midpoint.tools.testng.UnitTestMixin;
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
        displayTestTitle(testClass.getSimpleName() + "." + testMethodName);

        TEST_CONTEXT_THREAD_LOCAL.set(testResult);
    }

    @AfterMethod
    public void finishTestContext(ITestResult testResult) {
        TEST_CONTEXT_THREAD_LOCAL.remove();

        displayDefaultTestFooter(testResult);
    }

    @Override
    public Trace logger() {
        return logger;
    }

    @NotNull
    public String contextName() {
        ITestResult context = TEST_CONTEXT_THREAD_LOCAL.get();
        return context != null
                ? getClass().getSimpleName() + "." + context.getMethod().getMethodName()
                : getClass().getSimpleName();
    }

    @Override
    public String getTestNameShort() {
        return TEST_CONTEXT_THREAD_LOCAL.get().getMethod().getMethodName();
    }

    /**
     * This method null all fields which are not static, final or primitive type.
     * <p>
     * All this is just to make GC work during DirtiesContext after every test class,
     * because test class instances are not GCed immediately.
     * If they hold autowired fields like sessionFactory (for example
     * through SqlRepositoryService impl), their memory footprint is getting big.
     * <p>
     * Note that this does not work for components injected through constructor into
     * final fields - if we need this cleanup, make the field non-final.
     */
    @AfterClass(alwaysRun = true)
    protected void clearClassFields() throws Exception {
        logger.trace("Clearing all fields for test class {}", getClass().getName());
        clearClassFields(this, getClass());
    }

    private void clearClassFields(Object object, Class<?> forClass) throws Exception {
        if (forClass.getSuperclass() != null) {
            clearClassFields(object, forClass.getSuperclass());
        }

        for (Field field : forClass.getDeclaredFields()) {
            if (Modifier.isFinal(field.getModifiers())
                    || Modifier.isStatic(field.getModifiers())
                    || field.getType().isPrimitive()) {
                continue;
            }

            nullField(object, field);
        }
    }

    private void nullField(Object obj, Field field) throws Exception {
        logger.info("Setting {} to null on {}.", field.getName(), obj.getClass().getSimpleName());
        boolean accessible = field.isAccessible();
//        boolean accessible = field.canAccess(obj); // TODO: after ditching JDK 8
        if (!accessible) {
            field.setAccessible(true);
        }
        field.set(obj, null);
        field.setAccessible(accessible);
    }
}
