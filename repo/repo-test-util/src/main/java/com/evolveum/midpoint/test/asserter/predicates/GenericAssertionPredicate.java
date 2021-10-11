/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter.predicates;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Generic assertion predicate defined using Java Predicate and a function that generates the failure description.
 *
 * Experimental.
 */
public class GenericAssertionPredicate<T> implements AssertionPredicate<T> {

    @NotNull private final Predicate<T> predicate;
    @NotNull private final Function<T, String> failureDescriptionGenerator;

    @SuppressWarnings("WeakerAccess")   // useful for general public
    public GenericAssertionPredicate(@NotNull Predicate<T> predicate, @NotNull Function<T, String> failureDescriptionGenerator) {
        this.predicate = predicate;
        this.failureDescriptionGenerator = failureDescriptionGenerator;
    }

    @Override
    public AssertionPredicateEvaluation evaluate(T value) {
        if (predicate.test(value)) {
            return AssertionPredicateEvaluation.success();
        } else {
            return AssertionPredicateEvaluation.failure(failureDescriptionGenerator.apply(value));
        }
    }
}
