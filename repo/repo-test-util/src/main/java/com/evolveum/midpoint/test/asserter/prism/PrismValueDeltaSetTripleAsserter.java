/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;

import java.util.Collection;

/**
 * @author semancik
 */
public class PrismValueDeltaSetTripleAsserter<V extends PrismValue,RA> extends DeltaSetTripleAsserter<V,PrismValueDeltaSetTriple<V>,RA> {

    public PrismValueDeltaSetTripleAsserter(PrismValueDeltaSetTriple<V> triple) {
        super(triple);
    }

    public PrismValueDeltaSetTripleAsserter(PrismValueDeltaSetTriple<V> triple, String detail) {
        super(triple, detail);
    }

    public PrismValueDeltaSetTripleAsserter(PrismValueDeltaSetTriple<V> triple, RA returnAsserter, String detail) {
        super(triple, returnAsserter, detail);
    }

    public static <V extends PrismValue> PrismValueDeltaSetTripleAsserter<V,Void> forPrismValueDeltaSetTriple(PrismValueDeltaSetTriple<V> triple) {
        return new PrismValueDeltaSetTripleAsserter<>(triple);
    }

    @Override
    public PrismValueDeltaSetTripleSetAsserter<V,PrismValueDeltaSetTripleAsserter<V,RA>> zeroSet() {
        return createSetAsserter(getTriple().getZeroSet(), "zero");
    }

    @Override
    public PrismValueDeltaSetTripleSetAsserter<V,PrismValueDeltaSetTripleAsserter<V,RA>> plusSet() {
        return createSetAsserter(getTriple().getPlusSet(), "plus");
    }

    @Override
    public PrismValueDeltaSetTripleSetAsserter<V,PrismValueDeltaSetTripleAsserter<V,RA>> minusSet() {
        return createSetAsserter(getTriple().getMinusSet(), "minus");
    }

    private PrismValueDeltaSetTripleSetAsserter<V,PrismValueDeltaSetTripleAsserter<V,RA>> createSetAsserter(Collection<V> set, String name) {
        PrismValueDeltaSetTripleSetAsserter<V,PrismValueDeltaSetTripleAsserter<V,RA>> setAsserter = new PrismValueDeltaSetTripleSetAsserter<>(set, this, name+" set of "+desc());
        copySetupTo(setAsserter);
        return setAsserter;
    }

    @Override
    public PrismValueDeltaSetTripleAsserter<V,RA> assertNullZero() {
        super.assertNullZero();
        return this;
    }

    @Override
    public PrismValueDeltaSetTripleAsserter<V,RA> assertNullPlus() {
        super.assertNullPlus();
        return this;
    }

    @Override
    public PrismValueDeltaSetTripleAsserter<V,RA> assertNullMinus() {
        super.assertNullMinus();
        return this;
    }

    @Override
    public PrismValueDeltaSetTripleAsserter<V,RA> assertEmptyZero() {
        super.assertEmptyZero();
        return this;
    }

    @Override
    public PrismValueDeltaSetTripleAsserter<V,RA> assertEmptyPlus() {
        super.assertEmptyPlus();
        return this;
    }

    @Override
    public PrismValueDeltaSetTripleAsserter<V,RA> assertEmptyMinus() {
        super.assertEmptyMinus();
        return this;
    }

    @Override
    public PrismValueDeltaSetTripleAsserter<V,RA> display() {
        super.display();
        return this;
    }

    @Override
    public PrismValueDeltaSetTripleAsserter<V,RA> display(String message) {
        super.display(message);
        return this;
    }
}
