/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.test.IntegrationTestTools;
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
        IntegrationTestTools.display(message, definition);
        return this;
    }
}
