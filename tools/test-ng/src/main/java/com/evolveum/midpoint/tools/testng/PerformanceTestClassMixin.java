/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

/**
 * Mixin supporting work with {@link TestMonitor} at the class-scope level
 * (one test report for all the methods in the test class).
 * Details of setting of {@link TestMonitor} is up to the class, methods from
 * {@link PerformanceTestCommonMixin} must be implemented.
 *
 * [NOTE]
 * ====
 * Actual `@Before/AfterClass` methods are implemented in `AbstractUnitTest`
 * and `AbstractSpringTest` using `instanceof` check for two reasons:
 *
 * * If `@AfterClass` is in interface it is executed after all lifecycle methods from the class
 * hierarchy - which may happen after the Spring context is destroyed (for integration tests).
 * * If mixin interface is on the abstract class the lifecycle methods *are not called at all*
 * in the test subclasses, which really sucks.
 *
 * So currently this is only marker interface used by lifecycle methods in our two top-level
 * classes (unit/Spring) and everything works fine.
 * ====
 */
public interface PerformanceTestClassMixin extends PerformanceTestCommonMixin {
}
