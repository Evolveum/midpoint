/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter.predicate;

/**
 * Evaluation of a predicate used within an assertion.
 *
 * Experimental.
 */
public class AssertionPredicateEvaluation {

    private final boolean success;
    private final String failureDescription;

    private AssertionPredicateEvaluation(boolean success, String failureDescription) {
        this.success = success;
        this.failureDescription = failureDescription;
    }

    public static AssertionPredicateEvaluation success() {
        return new AssertionPredicateEvaluation(true, null);
    }

    public static AssertionPredicateEvaluation failure(String description) {
        return new AssertionPredicateEvaluation(false, description);
    }

    public boolean hasFailed() {
        return !success;
    }

    public String getFailureDescription() {
        return failureDescription;
    }
}
