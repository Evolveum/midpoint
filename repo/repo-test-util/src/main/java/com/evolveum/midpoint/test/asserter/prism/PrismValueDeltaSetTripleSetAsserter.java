/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;

import java.util.Collection;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author semancik
 */
public class PrismValueDeltaSetTripleSetAsserter<V extends PrismValue,RA> extends DeltaSetTripleSetAsserter<V,RA> {

    public PrismValueDeltaSetTripleSetAsserter(Collection<V> set) {
        super(set);
    }

    public PrismValueDeltaSetTripleSetAsserter(Collection<V> set, String detail) {
        super(set,detail);
    }

    public PrismValueDeltaSetTripleSetAsserter(Collection<V> set, RA returnAsserter, String detail) {
        super(set,returnAsserter, detail);
    }

    public static <V extends PrismValue> PrismValueDeltaSetTripleSetAsserter<V,Void> forSPrismValueet(Collection<V> set) {
        return new PrismValueDeltaSetTripleSetAsserter<>(set);
    }

    @Override
    public PrismValueDeltaSetTripleSetAsserter<V,RA> assertSize(int expected) {
        super.assertSize(expected);
        return this;
    }

    @Override
    public PrismValueDeltaSetTripleSetAsserter<V,RA> assertNone() {
        super.assertNone();
        return this;
    }

    @Override
    public PrismValueDeltaSetTripleSetAsserter<V,RA> assertNull() {
        super.assertNull();
        return this;
    }

    public <T> PrismPropertyValueAsserter<T, PrismValueDeltaSetTripleSetAsserter<V,RA>> singlePropertyValue(Class<T> type) {
        assertSize(1);
        V val = getSet().iterator().next();
        if (!(val instanceof PrismPropertyValue)) {
            fail("Expected that a single value of set will be a property value, but it was "+val+", in " + desc());
        }
        PrismPropertyValueAsserter<T, PrismValueDeltaSetTripleSetAsserter<V,RA>> vAsserter = new PrismPropertyValueAsserter<>((PrismPropertyValue<T>)val, this, "single property value in " + desc());
        copySetupTo(vAsserter);
        return vAsserter;
    }

    public <T> PrismValueDeltaSetTripleSetAsserter<V,RA> assertSinglePropertyValue(T expectedValue) {
        assertSize(1);
        V val = getSet().iterator().next();
        if (!(val instanceof PrismPropertyValue)) {
            fail("Expected that a single value of set will be a property value, but it was "+val+", in " + desc());
        }
        T value = ((PrismPropertyValue<T>)val).getValue();
        assertEquals("Wrong property value in "+desc(), expectedValue, value);
        return this;
    }

    @Override
    public PrismValueDeltaSetTripleSetAsserter<V,RA> display() {
        super.display();
        return this;
    }

    @Override
    public PrismValueDeltaSetTripleSetAsserter<V,RA> display(String message) {
        super.display(message);
        return this;
    }
}
