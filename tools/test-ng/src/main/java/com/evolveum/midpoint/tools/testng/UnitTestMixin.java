/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.testng.ITestResult;

/**
 * Mixin with various utility methods, mostly related to test header/footer/section output/logging.
 */
public interface UnitTestMixin {

    String TEST_LOG_PREFIX = "=====[ ";
    String TEST_LOG_SUFFIX = " ]======================================";
    String TEST_OUT_PREFIX = "\n\n=====[ ";
    String TEST_OUT_SUFFIX = " ]======================================\n";
    String TEST_OUT_FOOTER_PREFIX = "====== ";
    String TEST_OUT_FOOTER_SUFFIX = "\n";
    String TEST_OUT_SECTION_PREFIX = "\n\n----- ";
    String TEST_OUT_SECTION_SUFFIX = " --------------------------------------\n";
    String TEST_LOG_SECTION_PREFIX = "----- ";
    String TEST_LOG_SECTION_SUFFIX = " --------------------------------------";

    /**
     * Context name is "class-simple-name.method" if test method context is available,
     * otherwise it is just simple name of the test class.
     * This also works as "getTestName" including (short) class name when
     * {@link #getTestNameShort()} is too short and/or class name is required.
     * <p>
     * This is particularly useful for code that may run outside of test method.
     */
    @NotNull String contextName();

    /**
     * Returns short test name - typically just a method name (without class).
     * See also {@link #contextName()} if class name is required.
     * This fails if test-method context is not available.
     */
    String getTestNameShort();

    /**
     * Returns test class logger.
     */
    Logger logger();

    default void displayTestTitle(String testName) {
        System.out.println(TEST_OUT_PREFIX + testName + TEST_OUT_SUFFIX);
        logger().info(TEST_LOG_PREFIX + testName + TEST_LOG_SUFFIX);
    }

    default void displayDefaultTestFooter(ITestResult testResult) {
        long testMsDuration = testResult.getEndMillis() - testResult.getStartMillis();
        System.out.println(TEST_OUT_FOOTER_PREFIX + " FINISHED in " + testMsDuration + " ms" + TEST_OUT_FOOTER_SUFFIX);
        logger().info(TEST_LOG_PREFIX + " FINISHED in " + testMsDuration + " ms" + TEST_LOG_SUFFIX);
    }

    /**
     * Displays "when" subsection header with test name.
     * Even better, use {@link #when(String)} and provide human readable description.
     */
    default void when() {
        when(null);
    }

    /**
     * Displays "when" subsection header with test name and provided description (nullable).
     */
    default void when(String description) {
        String testName = getTestNameShort();
        if (description == null) {
            description = "";
        }
        System.out.println(TEST_OUT_SECTION_PREFIX + testName + ": WHEN " + description + TEST_OUT_SECTION_SUFFIX);
        logger().info(TEST_LOG_SECTION_PREFIX + testName + ": WHEN " + description + TEST_LOG_SECTION_SUFFIX);
    }

    /**
     * Displays "then" subsection header with test name.
     * Even better, use {@link #then(String)} and provide human readable description.
     */
    default void then() {
        then(null);
    }

    /**
     * Displays "then" subsection header with test name and provided description (nullable).
     */
    default void then(String description) {
        String testName = getTestNameShort();
        System.out.println(TEST_OUT_SECTION_PREFIX + testName + ": THEN " + description);
        logger().info(TEST_LOG_SECTION_PREFIX + testName + ": THEN " + description);
    }

    // TODO introduce "expect" as well? sometimes we use when/then combined section
    // in such a case instead of "given - when/then" we should have "given - expect"
}
