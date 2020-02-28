/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

import org.testng.ITestResult;

/**
 * Mixin with various utility methods - typically delegating to {@link TestUtil}.
 */
public interface UnitTestMixin {

    /**
     * Returns short test name - typically just a method name (without class).
     */
    String getTestNameShort();

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
        TestUtil.displayWhen(getTestNameShort(), description);
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
        TestUtil.displayThen(getTestNameShort(), description);
    }

    // TODO introduce "expect" as well? sometimes we use when/then combined section
    // in such a case instead of "given - when/then" we should have "given - expect"

    // TODO inline after merge to master
    default void displayWhen() {
        when();
    }

    // TODO inline after merge to master
    default void displayWhen(String description) {
        when(description);
    }

    // TODO inline after merge to master
    default void displayThen() {
        then();
    }

    // TODO inline after merge to master
    default void displayThen(String description) {
        then(description);
    }

    default void displayDefaultTestFooter(ITestResult testResult) {
        long testMsDuration = testResult.getEndMillis() - testResult.getStartMillis();
        TestUtil.displayFooter(testResult.getMethod().getMethodName() + " FINISHED in " + testMsDuration + " ms");
    }
}
