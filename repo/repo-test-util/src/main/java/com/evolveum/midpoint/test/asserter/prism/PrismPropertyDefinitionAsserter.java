/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;

/**
 * @author Radovan semancik
 */
public class PrismPropertyDefinitionAsserter<T,RA> extends PrismDefinitionAsserter<RA> {

    public PrismPropertyDefinitionAsserter(PrismPropertyDefinition<T> definition) {
        super(definition);
    }

    public PrismPropertyDefinitionAsserter(PrismPropertyDefinition<T> definition, String detail) {
        super(definition, detail);
    }

    public PrismPropertyDefinitionAsserter(PrismPropertyDefinition<T> definition, RA returnAsserter, String detail) {
        super(definition, returnAsserter, detail);
    }

    public static <T> PrismPropertyDefinitionAsserter<T,Void> forPropertyDefinition(PrismPropertyDefinition<T> attrDefinition) {
        return new PrismPropertyDefinitionAsserter<>(attrDefinition);
    }

    public PrismPropertyDefinition<T> getDefinition() {
        return (PrismPropertyDefinition<T>) super.getDefinition();
    }

    protected String desc() {
        return descWithDetails("property definition " + PrettyPrinter.prettyPrint(getDefinition().getItemName()));
    }

    public PrismPropertyDefinitionAsserter<T,RA> display() {
        display(desc());
        return this;
    }

    public PrismPropertyDefinitionAsserter<T,RA> display(String message) {
        PrismTestUtil.display(message, getDefinition());
        return this;
    }

    public PrismPropertyDefinitionAsserter<T,RA> assertDenyAdd() {
        assertAccess("Add", false, getDefinition().canAdd());
        return this;
    }

    public PrismPropertyDefinitionAsserter<T,RA> assertAllowAdd() {
        assertAccess("Add", true, getDefinition().canAdd());
        return this;
    }

    public PrismPropertyDefinitionAsserter<T,RA> assertDenyModify() {
        assertAccess("Modify", false, getDefinition().canModify());
        return this;
    }

    public PrismPropertyDefinitionAsserter<T,RA> assertAllowModify() {
        assertAccess("Modify", true, getDefinition().canModify());
        return this;
    }

    public PrismPropertyDefinitionAsserter<T,RA> assertDenyRead() {
        assertAccess("Read", false, getDefinition().canRead());
        return this;
    }

    public PrismPropertyDefinitionAsserter<T,RA> assertAllowRead() {
        assertAccess("Read", true, getDefinition().canRead());
        return this;
    }
    private void assertAccess(String access, boolean expected, boolean actual) {
        if (expected != actual) {
            fail(access+" access is "+actual+" and we expected "+expected+" in "+desc());
        }
    }
}
