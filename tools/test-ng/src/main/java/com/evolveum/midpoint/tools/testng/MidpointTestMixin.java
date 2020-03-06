/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.testng.ITestResult;

/**
 * Mixin with various utility methods, mostly related to test header/footer/section output/logging.
 */
public interface MidpointTestMixin {

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
     * Context name is {@link #getTestName()} if test method context is available,
     * otherwise it is just a simple name of the test class.
     * <p>
     * This is particularly useful for code that may run outside of test method scope
     * or in another thread where context is not available.
     */
    @NotNull
    default String contextName() {
        MidpointTestContext context = getTestContext();
        return context != null
                ? context.getTestName()
                : getClass().getSimpleName();
    }

    /**
     * Returns {@link MidpointTestContext#getTestName()}.
     * This fails if test-method context is not available.
     */
    default String getTestName() {
        return testContext().getTestName();
    }

    /**
     * Returns {@link MidpointTestContext#getTestNameShort()}.
     * This fails if test-method context is not available.
     */
    default String getTestNameShort() {
        return testContext().getTestNameShort();
    }

    /**
     * Returns {@link MidpointTestContext#getTestNameLong()}.
     * This fails if test-method context is not available.
     */
    default String getTestNameLong() {
        return testContext().getTestNameLong();
    }

    /**
     * Returns test class logger.
     */
    Logger logger();

    /**
     * Returns {@link MidpointTestContext} from current test-method context
     * or {@code null} if context is not available - it should not fail.
     * <p>
     * This method should be implemented by supporting classes, but not used in tests in general.
     * It is used for default implementations of various testName*() methods.
     */
    @Nullable MidpointTestContext getTestContext();

    @Deprecated
    @NotNull
    // TODO switch to private after ditching JDK 8, DON'T USE/DON'T OVERRIDE!
    /*private*/ default MidpointTestContext testContext() {
        return Objects.requireNonNull(getTestContext(),
                "Current test-method context MUST NOT be null");
    }

    /**
     * Displays test header with the test name.
     * Not intended for tests classes, used by lifecycle methods in our test superclasses.
     */
    default void displayTestTitle(String testTitle) {
        System.out.println(TEST_OUT_PREFIX + testTitle + TEST_OUT_SUFFIX);
        logger().info(TEST_LOG_PREFIX + testTitle + TEST_LOG_SUFFIX);
    }

    /**
     * Displays test footer with the test name and test duration.
     * Not intended for tests classes, used by lifecycle methods in our test superclasses.
     */
    default void displayTestFooter(String testTitle, ITestResult testResult) {
        long testMsDuration = testResult.getEndMillis() - testResult.getStartMillis();
        System.out.println(TEST_OUT_FOOTER_PREFIX + testTitle + " FINISHED in " + testMsDuration + " ms" + TEST_OUT_FOOTER_SUFFIX);
        logger().info(TEST_LOG_PREFIX + testTitle + " FINISHED in " + testMsDuration + " ms" + TEST_LOG_SUFFIX);
    }

    default void display(String text) {
        System.out.println("\n*** " + text);
        logger().debug("*** {}", text);
    }

    default void display(String title, String value) {
        System.out.println("\n*** " + title + "\n" + value);
        logger().debug("*** {}\n{}", title, value);
    }

    /**
     * Displays "given" subsection header with test name.
     * Even better, use {@link #given(String)} and provide human readable description.
     */
    default void given() {
        given(null);
    }

    /**
     * Displays "given" subsection header with test name and provided description (nullable).
     */
    default void given(String description) {
        String testName = getTestNameShort();
        if (description == null) {
            description = "";
        }
        System.out.println(TEST_OUT_SECTION_PREFIX + testName + ": GIVEN " + description + TEST_OUT_SECTION_SUFFIX);
        logger().info(TEST_LOG_SECTION_PREFIX + testName + ": GIVEN " + description + TEST_LOG_SECTION_SUFFIX);
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
        if (description == null) {
            description = "";
        }
        System.out.println(TEST_OUT_SECTION_PREFIX + testName + ": THEN " + description + TEST_OUT_SECTION_SUFFIX);
        logger().info(TEST_LOG_SECTION_PREFIX + testName + ": THEN " + description + TEST_LOG_SECTION_SUFFIX);
    }

    /**
     * Displays "expect" subsection header with test name.
     * Even better, use {@link #expect(String)} and provide human readable description.
     */
    default void expect() {
        expect(null);
    }

    /**
     * Displays "expect" subsection header with test name and provided description (nullable).
     * This is for tests with simpler given-expect structure.
     * In other words, if "when" and "then" can't be clearly separated, we want "expect".
     */
    default void expect(String description) {
        String testName = getTestNameShort();
        if (description == null) {
            description = "";
        }
        System.out.println(TEST_OUT_SECTION_PREFIX + testName + ": EXPECT " + description + TEST_OUT_SECTION_SUFFIX);
        logger().info(TEST_LOG_SECTION_PREFIX + testName + ": EXPECT " + description + TEST_LOG_SECTION_SUFFIX);
    }
}
