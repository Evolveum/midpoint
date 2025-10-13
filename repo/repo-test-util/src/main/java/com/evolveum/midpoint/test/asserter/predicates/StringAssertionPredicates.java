/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter.predicates;

/**
 * Methods for construction of string assertion predicates.
 *
 * Experimental.
 */
public class StringAssertionPredicates {

    public static AssertionPredicate<String> startsWith(String prefix) {
        return new GenericAssertionPredicate<>(
                value -> value.startsWith(prefix),
                value -> "'" + value + "' does not start with '" + prefix + "'");
    }
}
