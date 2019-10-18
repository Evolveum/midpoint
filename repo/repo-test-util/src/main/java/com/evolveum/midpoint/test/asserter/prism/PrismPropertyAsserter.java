/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;

import java.util.Arrays;
import java.util.HashSet;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author semancik
 *
 */
public class PrismPropertyAsserter<T, RA> extends PrismItemAsserter<PrismProperty<T>, RA> {

    @SuppressWarnings("unused")
    public PrismPropertyAsserter(PrismProperty<T> property) {
        super(property);
    }

    @SuppressWarnings("unused")
    public PrismPropertyAsserter(PrismProperty<T> property, String detail) {
        super(property, detail);
    }

    public PrismPropertyAsserter(PrismProperty<T> property, RA returnAsserter, String detail) {
        super(property, returnAsserter, detail);
    }

    @Override
    public PrismPropertyAsserter<T,RA> assertSize(int expected) {
        super.assertSize(expected);
        return this;
    }

    @Override
    public PrismPropertyAsserter<T,RA> assertNullOrNoValues() {
        super.assertNullOrNoValues();
        return this;
    }

    @Override
    public PrismPropertyAsserter<T,RA> assertComplete() {
        super.assertComplete();
        return this;
    }

    @Override
    public PrismPropertyAsserter<T,RA> assertIncomplete() {
        super.assertIncomplete();
        return this;
    }

    public PrismPropertyValueAsserter<T,PrismPropertyAsserter<T,RA>> singleValue() {
        assertSize(1);
        PrismPropertyValue<T> pval = getItem().getValue();
        PrismPropertyValueAsserter<T,PrismPropertyAsserter<T,RA>> asserter = new PrismPropertyValueAsserter<>(pval, this, "single value in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    // TODO

    protected String desc() {
        return getDetails();
    }

    // TODO generalize for any items
    @SafeVarargs
    public final PrismPropertyAsserter<T,RA> assertRealValues(T... expectedRealValues) {
        if (expectedRealValues.length > 0) {
            assertSize(expectedRealValues.length);
            assertEquals("Wrong real values", new HashSet<>(Arrays.asList(expectedRealValues)),
                    new HashSet<>(getItem().getRealValues()));
        } else {
            assertNullOrNoValues();
        }
        return this;
    }
}
