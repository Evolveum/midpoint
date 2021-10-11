/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.ValueSelector;

/**
 * @author semancik
 */
public class PrismContainerAsserter<C extends Containerable, RA> extends PrismItemAsserter<PrismContainer<C>, RA> {

    public PrismContainerAsserter(PrismContainer<C> container) {
        super(container);
    }

    public PrismContainerAsserter(PrismContainer<C> container, String detail) {
        super(container, detail);
    }

    public PrismContainerAsserter(PrismContainer<C> container, RA returnAsserter, String detail) {
        super(container, returnAsserter, detail);
    }

    @Override
    public PrismContainerAsserter<C,RA> assertSize(int expected) {
        super.assertSize(expected);
        return this;
    }

    @Override
    public PrismItemAsserter<PrismContainer<C>, RA> assertComplete() {
        super.assertComplete();
        return this;
    }

    @Override
    public PrismItemAsserter<PrismContainer<C>, RA> assertIncomplete() {
        super.assertIncomplete();
        return this;
    }

    public PrismContainerValueAsserter<C, PrismContainerAsserter<C, RA>> value(int index) {
        PrismContainerValueAsserter<C, PrismContainerAsserter<C, RA>> asserter =
                new PrismContainerValueAsserter<>(getItem().getValues().get(index), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public PrismContainerValueAsserter<C, PrismContainerAsserter<C, RA>> value(ValueSelector<PrismContainerValue<C>> selector) {
        PrismContainerValueAsserter<C, PrismContainerAsserter<C, RA>> asserter =
                new PrismContainerValueAsserter<>(getItem().getAnyValue(selector), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    protected String desc() {
        return getDetails();
    }

}
