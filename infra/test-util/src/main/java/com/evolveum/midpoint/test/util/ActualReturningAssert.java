/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.util;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.InstanceOfAssertFactory;

/**
 * Obtained from https://github.com/assertj/assertj/issues/1931#issuecomment-1145274790, with many thanks.
 */
public class ActualReturningAssert<ACTUAL> extends AbstractAssert<ActualReturningAssert<ACTUAL>, ACTUAL> {

    private ActualReturningAssert(ACTUAL actual) {
        super(actual, ActualReturningAssert.class);
    }

    public ACTUAL getActual() {
        return actual;
    }

    /**
     * Allows returning the <em>actual</em> element of an Assertion.
     *
     * <p>Example:
     * <pre>String singleEntry = Assertions.assertThat(myStringList)
     *   .singleElement(as(actualReturning(String.class)))
     *   .getActual();</pre>
     *
     * @param <ACTUAL>  type of the actual element
     * @param actualClass  class of the actual element
     * @return AssertFactory decorator for use in <code>asInstanceOf(...)</code>
     *     that provides access to the actual element
     */
    public static <ACTUAL> InstanceOfAssertFactory<ACTUAL, ActualReturningAssert<ACTUAL>>
    actualReturning(Class<ACTUAL> actualClass) {
        return new InstanceOfAssertFactory<>(actualClass, ActualReturningAssert::new);
    }
}
