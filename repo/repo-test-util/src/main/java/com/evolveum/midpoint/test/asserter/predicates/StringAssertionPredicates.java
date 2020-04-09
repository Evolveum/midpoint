/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
