/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter.predicates;

/**
 * Predicate to be used in assertions.
 *
 * Experimental.
 */
public interface AssertionPredicate<T> {

    AssertionPredicateEvaluation evaluate(T value);

}
