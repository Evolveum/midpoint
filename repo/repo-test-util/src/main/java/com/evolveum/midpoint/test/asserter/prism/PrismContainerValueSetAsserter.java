/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;

import java.util.Collection;

/**
 * @author semancik
 */
public class PrismContainerValueSetAsserter<C extends Containerable,RA>
        extends PrismValueSetAsserter<PrismContainerValue<C>, PrismContainerValueAsserter<C, PrismContainerValueSetAsserter<C,RA>>, RA> {

    public PrismContainerValueSetAsserter(Collection<PrismContainerValue<C>> valueSet) {
        super(valueSet);
    }

    public PrismContainerValueSetAsserter(Collection<PrismContainerValue<C>> valueSet, String detail) {
        super(valueSet, detail);
    }

    public PrismContainerValueSetAsserter(Collection<PrismContainerValue<C>> valueSet, RA returnAsserter, String detail) {
        super(valueSet, returnAsserter, detail);
    }

    public PrismContainerValueSetAsserter<C,RA> assertSize(int expected) {
        super.assertSize(expected);
        return this;
    }

    public PrismContainerValueSetAsserter<C,RA> assertNone() {
        super.assertNone();
        return this;
    }

    @Override
    protected PrismContainerValueAsserter<C, PrismContainerValueSetAsserter<C,RA>> createValueAsserter(PrismContainerValue<C> pval, String detail) {
        return new PrismContainerValueAsserter<>(pval, this, detail);
    }

    protected String desc() {
        return getDetails();
    }

}
