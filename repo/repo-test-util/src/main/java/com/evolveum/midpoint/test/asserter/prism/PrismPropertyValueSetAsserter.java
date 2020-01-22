/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismPropertyValue;

/**
 * @author semancik
 */
public class PrismPropertyValueSetAsserter<T,RA>
        extends PrismValueSetAsserter<PrismPropertyValue<T>, PrismPropertyValueAsserter<T, PrismPropertyValueSetAsserter<T,RA>>, RA> {

    public PrismPropertyValueSetAsserter(Collection<PrismPropertyValue<T>> valueSet) {
        super(valueSet);
    }

    public PrismPropertyValueSetAsserter(Collection<PrismPropertyValue<T>> valueSet, String detail) {
        super(valueSet, detail);
    }

    public PrismPropertyValueSetAsserter(Collection<PrismPropertyValue<T>> valueSet, RA returnAsserter, String detail) {
        super(valueSet, returnAsserter, detail);
    }

    public PrismPropertyValueSetAsserter<T,RA> assertSize(int expected) {
        super.assertSize(expected);
        return this;
    }

    public PrismPropertyValueSetAsserter<T,RA> assertNone() {
        super.assertNone();
        return this;
    }

    @Override
    protected PrismPropertyValueAsserter<T, PrismPropertyValueSetAsserter<T,RA>> createValueAsserter(PrismPropertyValue<T> pval, String detail) {
        return new PrismPropertyValueAsserter<>(pval, this, detail);
    }

    protected String desc() {
        return getDetails();
    }

}
