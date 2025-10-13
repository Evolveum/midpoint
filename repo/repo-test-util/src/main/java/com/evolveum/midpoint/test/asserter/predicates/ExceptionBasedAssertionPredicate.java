/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter.predicates;

import com.evolveum.midpoint.util.CheckedConsumer;

import org.jetbrains.annotations.NotNull;

/**
 * Throws an exception if predicate fails.
 *
 * Experimental.
 */
public class ExceptionBasedAssertionPredicate<T> implements AssertionPredicate<T> {

    @NotNull private final CheckedConsumer<T> checkingConsumer;

    @SuppressWarnings("WeakerAccess") // useful for general public
    public ExceptionBasedAssertionPredicate(@NotNull CheckedConsumer<T> checkingConsumer) {
        this.checkingConsumer = checkingConsumer;
    }

    @Override
    public AssertionPredicateEvaluation evaluate(T value) {
        try {
            checkingConsumer.accept(value);
            return AssertionPredicateEvaluation.success();
        } catch (Throwable t) {
            return AssertionPredicateEvaluation.failure(t.getMessage()); // TODO
        }
    }
}
