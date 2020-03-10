/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter.predicates;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * Generic assertion predicate defined using function that generates the failure description (or null if test passes).
 *
 * Experimental.
 */
public class SimplifiedGenericAssertionPredicate<T> implements AssertionPredicate<T> {

    @NotNull private final Function<T, String> failureDescriptionGenerator;

    @SuppressWarnings("WeakerAccess")   // useful for general public
    public SimplifiedGenericAssertionPredicate(@NotNull Function<T, String> failureDescriptionGenerator) {
        this.failureDescriptionGenerator = failureDescriptionGenerator;
    }

    @Override
    public AssertionPredicateEvaluation evaluate(T value) {
        String failureDescription = failureDescriptionGenerator.apply(value);
        if (failureDescription != null) {
            return AssertionPredicateEvaluation.failure(failureDescription);
        } else {
            return AssertionPredicateEvaluation.success();
        }
    }
}
