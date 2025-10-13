/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
