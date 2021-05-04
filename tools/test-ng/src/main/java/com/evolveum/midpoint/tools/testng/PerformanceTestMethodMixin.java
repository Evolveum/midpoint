/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

/**
 * Mixin supporting work with {@link TestMonitor} at the method-scope level
 * (one test report for each test method).
 * Details of setting of {@link TestMonitor} is up to the class, methods from
 * {@link PerformanceTestCommonMixin} must be implemented.
 *
 * [NOTE]
 * ====
 * Actual `@Before/AfterMethod` methods are implemented in `AbstractUnitTest`
 * and `AbstractSpringTest` using `instanceof` check for two reasons:
 *
 * * If mixin interface is on the abstract class the lifecycle methods *are not called at all*
 * in the test subclasses, which really sucks.
 * * To use the same strategy as in {@link PerformanceTestClassMixin} which actually has another
 * good reason to do this (issue that does not affect the method lifecycle that much).
 *
 * So currently this is only marker interface used by lifecycle methods in our two top-level
 * classes (unit/Spring) and everything works fine.
 * ====
 */
public interface PerformanceTestMethodMixin extends PerformanceTestCommonMixin {
}
