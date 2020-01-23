/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Collection;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;

/**
 * @author semancik
 *
 */
public class DeltaSetTripleSetAsserter<T,RA> extends AbstractAsserter<RA> {

    private Collection<T> set;

    public DeltaSetTripleSetAsserter(Collection<T> set) {
        super();
        this.set = set;
    }

    public DeltaSetTripleSetAsserter(Collection<T> set, String detail) {
        super(detail);
        this.set = set;
    }

    public DeltaSetTripleSetAsserter(Collection<T> set, RA returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.set = set;
    }

    public static <T> DeltaSetTripleSetAsserter<T,Void> forSet(Collection<T> set) {
        return new DeltaSetTripleSetAsserter<>(set);
    }

    public Collection<T> getSet() {
        return set;
    }

    public DeltaSetTripleSetAsserter<T,RA> assertSize(int expected) {
        assertEquals("Wrong number of values in " + desc(), expected, set.size());
        return this;
    }

    public DeltaSetTripleSetAsserter<T,RA> assertNone() {
        assertSize(0);
        return this;
    }

    public DeltaSetTripleSetAsserter<T,RA> assertNull() {
        AssertJUnit.assertNull("Expected null, but found non-null set in " + desc(), set);
        return this;
    }

    // TODO

    protected String desc() {
        return descWithDetails(set);
    }

    public DeltaSetTripleSetAsserter<T,RA> display() {
        display(desc());
        return this;
    }

    public DeltaSetTripleSetAsserter<T,RA> display(String message) {
        IntegrationTestTools.display(message, set);
        return this;
    }
}
