/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collection;

import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;

/**
 * @author semancik
 *
 */
public class DeltaSetTripleAsserter<T,D extends DeltaSetTriple<T>,RA> extends AbstractAsserter<RA> {

    private D triple;

    public DeltaSetTripleAsserter(D triple) {
        super();
        this.triple = triple;
    }

    public DeltaSetTripleAsserter(D triple, String detail) {
        super(detail);
        this.triple = triple;
    }

    public DeltaSetTripleAsserter(D triple, RA returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.triple = triple;
    }

    public static <T> DeltaSetTripleAsserter<T,DeltaSetTriple<T>,Void> forDeltaSetTriple(DeltaSetTriple<T> triple) {
        return new DeltaSetTripleAsserter<>(triple);
    }

    public D getTriple() {
        return triple;
    }

    public DeltaSetTripleSetAsserter<T,? extends DeltaSetTripleAsserter<T,D,RA>> zeroSet() {
        return createSetAsserter(triple.getZeroSet(), "zero");
    }

    public DeltaSetTripleSetAsserter<T,? extends DeltaSetTripleAsserter<T,D,RA>> plusSet() {
        return createSetAsserter(triple.getPlusSet(), "plus");
    }

    public DeltaSetTripleSetAsserter<T,? extends DeltaSetTripleAsserter<T,D,RA>> minusSet() {
        return createSetAsserter(triple.getMinusSet(), "minus");
    }

    private DeltaSetTripleSetAsserter<T,DeltaSetTripleAsserter<T,D,RA>> createSetAsserter(Collection<T> set, String name) {
        DeltaSetTripleSetAsserter<T,DeltaSetTripleAsserter<T,D,RA>> setAsserter = new DeltaSetTripleSetAsserter<>(set, this, name+" set of "+desc());
        copySetupTo(setAsserter);
        return setAsserter;
    }

    public DeltaSetTripleAsserter<T,D,RA> assertNullZero() {
        assertNullSet(triple.getZeroSet(), "zero");
        return this;
    }

    public DeltaSetTripleAsserter<T,D,RA> assertNullPlus() {
        assertNullSet(triple.getPlusSet(), "plus");
        return this;
    }

    public DeltaSetTripleAsserter<T,D,RA> assertNullMinus() {
        assertNullSet(triple.getMinusSet(), "minus");
        return this;
    }

    private void assertNullSet(Collection<T> set, String name) {
        assertNull("Non-null "+name+" set in "+desc(), set);
    }

    public DeltaSetTripleAsserter<T,D,RA> assertEmptyZero() {
        assertEmptySet(triple.getZeroSet(), "zero");
        return this;
    }

    public DeltaSetTripleAsserter<T,D,RA> assertEmptyPlus() {
        assertEmptySet(triple.getPlusSet(), "plus");
        return this;
    }

    public DeltaSetTripleAsserter<T,D,RA> assertEmptyMinus() {
        assertEmptySet(triple.getMinusSet(), "minus");
        return this;
    }

    private void assertEmptySet(Collection<T> set, String name) {
        assertTrue("Non-empty "+name+" set in "+desc(), set.isEmpty());
    }

    protected String desc() {
        return descWithDetails(triple);
    }

    public DeltaSetTripleAsserter<T,D,RA> display() {
        display(desc());
        return this;
    }

    public DeltaSetTripleAsserter<T,D,RA> display(String message) {
        PrismTestUtil.display(message, triple);
        return this;
    }
}
