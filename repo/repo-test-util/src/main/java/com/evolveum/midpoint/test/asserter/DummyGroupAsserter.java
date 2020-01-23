/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import java.util.Collection;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.test.DummyResourceContoller;

/**
 * @author semancik
 *
 */
public class DummyGroupAsserter<R> extends DummyObjectAsserter<DummyGroup,R> {

    public DummyGroupAsserter(DummyGroup dummyGroup, String dummyResourceName) {
        super(dummyGroup, dummyResourceName);
    }

    public DummyGroupAsserter(DummyGroup dummyGroup, String dummyResourceName, String details) {
        super(dummyGroup, dummyResourceName, details);
    }

    public DummyGroupAsserter(DummyGroup dummyGroup, String dummyResourceName, R returnAsserter, String details) {
        super(dummyGroup, dummyResourceName, returnAsserter, details);
    }

    @Override
    public DummyGroupAsserter<R> assertName(String expected) {
        super.assertName(expected);
        return this;
    }

    @Override
    public <T> DummyGroupAsserter<R> assertAttribute(String attrName, T... expected) {
        super.assertAttribute(attrName, expected);
        return this;
    }

    @Override
    public <T> DummyGroupAsserter<R> assertNoAttribute(String attrName) {
        super.assertNoAttribute(attrName);
        return this;
    }

    @Override
    public DummyGroupAsserter<R> assertEnabled() {
        super.assertEnabled();
        return this;
    }

    @Override
    public DummyGroupAsserter<R> assertLastModifier(String expected) {
        super.assertLastModifier(expected);
        return this;
    }

    public DummyGroupAsserter<R> assertDescription(String expected) {
        assertAttribute(DummyResourceContoller.DUMMY_GROUP_ATTRIBUTE_DESCRIPTION, expected);
        return this;
    }

    public DummyGroupAsserter<R> assertMembers(String... expectedMembers) {
        Collection<String> groupMembers = getDummyObjectAssertExists().getMembers();
        PrismAsserts.assertEqualsCollectionUnordered("Wrong members of "+desc(), groupMembers, expectedMembers);
        return this;
    }

    public DummyGroupAsserter<R> assertNoMembers() {
        Collection<String> groupMembers = getDummyObjectAssertExists().getMembers();
        if (groupMembers != null && !groupMembers.isEmpty()) {
            fail("Unexpected members in "+desc()+": "+groupMembers);
        }
        return this;
    }

    public DummyGroupAsserter<R> assertMember(String expectedMember) {
        Collection<String> groupMembers = getDummyObjectAssertExists().getMembers();
        if (!groupMembers.contains(expectedMember)) {
            fail("Member "+expectedMember+" not found in "+desc());
        }
        return this;
    }

    public DummyGroupAsserter<R> assertNotMember(String expectedMember) {
        Collection<String> groupMembers = getDummyObjectAssertExists().getMembers();
        if (groupMembers.contains(expectedMember)) {
            fail("Member "+expectedMember+" found, but not expecting it, in "+desc());
        }
        return this;
    }

    public DummyGroupAsserter<R> display() {
        super.display();
        return this;
    }

    public DummyGroupAsserter<R> display(String message) {
        super.display(message);
        return this;
    }
}
