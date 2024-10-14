/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.evolveum.midpoint.schema.internals.InternalsConfig;

import org.jetbrains.annotations.Nullable;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.evolveum.midpoint.tools.testng.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Base test class for tests integrated with Spring providing {@link MidpointTestMixin} implementation.
 * Can be extended by any unit test class that would otherwise extend
 * {@link AbstractTestNGSpringContextTests}.
 */
public abstract class AbstractSpringTest extends AbstractTestNGSpringContextTests
        implements MidpointTestMixin {

    /**
     * Hides parent's logger, but that one is from commons-logging and we don't want that.
     */
    protected final Trace logger = TraceManager.getTrace(getClass());

    public AbstractSpringTest() {
        InternalsConfig.shadowCachingDefaultDefault = InternalsConfig.ShadowCachingDefault.FULL_BUT_USING_FRESH;
    }

    // region perf-test support
    private TestMonitor testMonitor;

    /** Called only by tests that need it, implements performance mixin interface. */
    public TestMonitor createTestMonitor() {
        testMonitor = new TestMonitor();
        return testMonitor;
    }

    /** Called only by tests that need it, implements performance mixin interface. */
    public void destroyTestMonitor() {
        testMonitor = null;
    }

    /** Called only by tests that need it, implements performance mixin interface. */
    public TestMonitor testMonitor() {
        return testMonitor;
    }

    // see the comment in PerformanceTestMethodMixin for explanation
    @BeforeMethod
    public void initTestMethodMonitor() {
        if (this instanceof PerformanceTestMethodMixin) {
            createTestMonitor();
        }
    }

    // see the comment in PerformanceTestMethodMixin for explanation
    @AfterMethod
    public void dumpMethodReport(Method method) {
        if (this instanceof PerformanceTestMethodMixin performanceTestMethodMixin) {
            performanceTestMethodMixin.dumpReport(getClass().getSimpleName() + "#" + method.getName());
        }
    }

    // see the comment in PerformanceTestClassMixin for explanation
    @BeforeClass
    public void initTestClassMonitor() {
        if (this instanceof PerformanceTestClassMixin) {
            createTestMonitor();
        }
    }

    // see the comment in PerformanceTestClassMixin for explanation
    @AfterClass
    public void dumpClassReport() {
        if (this instanceof PerformanceTestClassMixin performanceTestClassMixin) {
            performanceTestClassMixin.dumpReport(getClass().getSimpleName());
        }
    }
    // endregion

    @BeforeClass
    public void displayTestClassTitle() {
        displayTestTitle("Starting TEST CLASS: " + getClass().getName());
    }

    @AfterClass
    public void displayTestClassFooter() {
        displayTestFooter("Finishing with TEST CLASS: " + getClass().getName());
    }

    @BeforeMethod
    public void startTestContext(ITestResult testResult) throws Exception {
        SimpleMidpointTestContext context = SimpleMidpointTestContext.create(testResult);
        displayTestTitle(context.getTestName());
    }

    @AfterMethod
    public void finishTestContext(ITestResult testResult) {
        SimpleMidpointTestContext context = SimpleMidpointTestContext.get();
        displayTestFooter(context.getTestName(), testResult);
        SimpleMidpointTestContext.destroy();
    }

    @Override
    public Trace logger() {
        return logger;
    }

    @Override
    @Nullable
    public MidpointTestContext getTestContext() {
        return SimpleMidpointTestContext.get();
    }

    /**
     * This method null all fields which are not static, final or primitive type.
     *
     * All this is just to make GC work during DirtiesContext after every test class,
     * because test class instances are not GCed immediately.
     * If they hold autowired fields like sessionFactory (for example
     * through SqlRepositoryService impl), their memory footprint is getting big.
     * This can manifest as failed test initialization because of OOM in modules like model-intest.
     * Strangely, this will not fail the Jenkins build (but makes it much slower).
     *
     * Note that this does not work for components injected through constructor into
     * final fields - if we need this cleanup, make the field non-final.
     */
    @AfterClass(alwaysRun = true, dependsOnMethods = "dumpClassReport")
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
        boolean accessible = field.canAccess(obj);
        if (!accessible) {
            field.setAccessible(true);
        }
        field.set(obj, null);
        field.setAccessible(accessible);
    }
}
