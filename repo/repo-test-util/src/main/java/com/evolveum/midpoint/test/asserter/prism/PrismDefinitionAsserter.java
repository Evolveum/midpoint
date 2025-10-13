/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;

/**
 * @author semancik
 */
public abstract class PrismDefinitionAsserter<RA> extends AbstractAsserter<RA> {

    private Definition definition;

    public PrismDefinitionAsserter(Definition definition) {
        super();
        this.definition = definition;
    }

    public PrismDefinitionAsserter(Definition definition, String detail) {
        super(detail);
        this.definition = definition;
    }

    public PrismDefinitionAsserter(Definition definition, RA returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.definition = definition;
    }

    public Definition getDefinition() {
        return definition;
    }

    protected String desc() {
        return descWithDetails(definition);
    }

    public PrismDefinitionAsserter<RA> display() {
        display(desc());
        return this;
    }

    public PrismDefinitionAsserter<RA> display(String message) {
        PrismTestUtil.display(message, definition);
        return this;
    }
}
