/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME;

import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.test.DummyResourceContoller;

/**
 * @author semancik
 *
 */
public class DummyAccountAsserter<R> extends DummyObjectAsserter<DummyAccount,R> {

    public DummyAccountAsserter(DummyAccount dummyAccount, String dummyResourceName) {
        super(dummyAccount, dummyResourceName);
    }

    public DummyAccountAsserter(DummyAccount dummyAccount, String dummyResourceName, String details) {
        super(dummyAccount, dummyResourceName, details);
    }

    public DummyAccountAsserter(DummyAccount dummyAccount, String dummyResourceName, R returnAsserter, String details) {
        super(dummyAccount, dummyResourceName, returnAsserter, details);
    }

    @Override
    public DummyAccountAsserter<R> assertName(String expected) {
        super.assertName(expected);
        return this;
    }

    @Override
    public DummyAccountAsserter<R> assertId(String expected) {
        super.assertId(expected);
        return this;
    }

    @Override
    public <T> DummyObjectAsserter<DummyAccount, R> assertBinaryAttribute(String attributeName, byte[] expectedAttributeValue) {
        super.assertBinaryAttribute(attributeName, expectedAttributeValue);
        return this;
    }

    @Override
    public <T> DummyAccountAsserter<R> assertAttribute(String attrName, T... expected) {
        super.assertAttribute(attrName, expected);
        return this;
    }

    @Override
    public <T> DummyAccountAsserter<R> assertNoAttribute(String attrName) {
        super.assertNoAttribute(attrName);
        return this;
    }

    @Override
    public DummyAccountAsserter<R> assertEnabled() {
        super.assertEnabled();
        return this;
    }

    @Override
    public DummyAccountAsserter<R> assertDisabled() {
        super.assertDisabled();
        return this;
    }

    @Override
    public DummyAccountAsserter<R> assertLastModifier(String expected) {
        super.assertLastModifier(expected);
        return this;
    }

    public DummyAccountAsserter<R> assertFullName(String expected) {
        assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, expected);
        return this;
    }

    public DummyAccountAsserter<R> assertDescription(String expected) {
        assertAttribute(DummyAccount.ATTR_DESCRIPTION_NAME, expected);
        return this;
    }

    public DummyAccountAsserter<R> assertLocation(String expected) {
        assertAttribute(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, expected);
        return this;
    }

    public DummyAccountAsserter<R> assertPassword(String expected) {
        assertEquals("Wrong password in "+desc(), expected, getDummyObjectAssertExists().getPassword());
        return this;
    }

    public DummyAccountAsserter<R> display() {
        super.display();
        return this;
    }

    public DummyAccountAsserter<R> display(String message) {
        super.display(message);
        return this;
    }
}
