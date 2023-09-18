/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.icf.dummy.resource.DummyOrg;

public class DummyOrgAsserter<R> extends DummyObjectAsserter<DummyOrg,R> {

    public DummyOrgAsserter(DummyOrg org, String dummyResourceName) {
        super(org, dummyResourceName);
    }

    public DummyOrgAsserter(DummyOrg org, String dummyResourceName, String details) {
        super(org, dummyResourceName, details);
    }

    public DummyOrgAsserter(DummyOrg org, String dummyResourceName, R returnAsserter, String details) {
        super(org, dummyResourceName, returnAsserter, details);
    }

    @Override
    public DummyOrgAsserter<R> assertName(String expected) {
        super.assertName(expected);
        return this;
    }

    @Override
    public <T> DummyOrgAsserter<R> assertAttribute(String attrName, T... expected) {
        super.assertAttribute(attrName, expected);
        return this;
    }

    @Override
    public <T> DummyOrgAsserter<R> assertNoAttribute(String attrName) {
        super.assertNoAttribute(attrName);
        return this;
    }

    @Override
    public DummyOrgAsserter<R> assertEnabled() {
        super.assertEnabled();
        return this;
    }

    @Override
    public DummyOrgAsserter<R> assertLastModifier(String expected) {
        super.assertLastModifier(expected);
        return this;
    }

    public DummyOrgAsserter<R> display() {
        super.display();
        return this;
    }

    public DummyOrgAsserter<R> display(String message) {
        super.display(message);
        return this;
    }
}
