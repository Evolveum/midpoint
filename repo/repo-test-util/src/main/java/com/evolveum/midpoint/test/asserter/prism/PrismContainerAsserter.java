/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;

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

    protected String desc() {
        return getDetails();
    }

}
