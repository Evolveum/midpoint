/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;

/**
 * @author semancik
 */
public abstract class PrismValueSetAsserter<V extends PrismValue,VA extends PrismValueAsserter<V,? extends PrismValueSetAsserter<V,VA,RA>>, RA> extends AbstractAsserter<RA> {

    private Collection<V> valueSet;

    public PrismValueSetAsserter(Collection<V> valueSet) {
        super();
        this.valueSet = valueSet;
    }

    public PrismValueSetAsserter(Collection<V> valueSet, String detail) {
        super(detail);
        this.valueSet = valueSet;
    }

    public PrismValueSetAsserter(Collection<V> valueSet, RA returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.valueSet = valueSet;
    }


    public PrismValueSetAsserter<V,VA,RA> assertSize(int expected) {
        assertEquals("Wrong number of values in " + desc(), expected, valueSet.size());
        return this;
    }

    public PrismValueSetAsserter<V,VA,RA> assertNone() {
        assertSize(0);
        return this;
    }

    public VA single() {
        assertSize(1);
        VA valueAsserter = createValueAsserter(valueSet.iterator().next(), "single value in " + desc());
        copySetupTo((AbstractAsserter)valueAsserter);
        return valueAsserter;
    }

    protected abstract VA createValueAsserter(V pval, String detail);

    protected String desc() {
        return getDetails();
    }

}
