/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

import java.lang.reflect.Method;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Mixin supporting work with {@link TestMonitor} at the method-scope level
 * (one test report for each test method).
 * Details of setting of {@link TestMonitor} is up to the class, methods from
 * {@link PerformanceTestCommonMixin} must be implemented.
 */
public interface PerformanceTestMethodMixin extends PerformanceTestCommonMixin {

    @BeforeMethod
    default void initTestMonitor() {
        createTestMonitor();
    }

    @AfterMethod
    default void dumpReport(Method method) {
        dumpReport(getClass().getSimpleName() + "#" + method.getName());
    }
}
