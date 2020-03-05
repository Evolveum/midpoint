/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;
import java.util.Set;

import org.testng.AssertJUnit;

import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;

/**
 * @author semancik
 *
 */
public class DummyObjectAsserter<D extends DummyObject,R> extends AbstractAsserter<R> {

    private D dummyObject;
    private String dummyResourceName;

    public DummyObjectAsserter(D dummyObject, String dummyResourceName) {
        super();
        this.dummyObject = dummyObject;
        this.dummyResourceName = dummyResourceName;
    }

    public DummyObjectAsserter(D dummyObject, String dummyResourceName, String details) {
        super(details);
        this.dummyObject = dummyObject;
        this.dummyResourceName = dummyResourceName;
    }

    public DummyObjectAsserter(D dummyObject, String dummyResourceName, R returnAsserter, String details) {
        super(returnAsserter, details);
        this.dummyObject = dummyObject;
        this.dummyResourceName = dummyResourceName;
    }

    public D getDummyObject() {
        return dummyObject;
    }

    protected D getDummyObjectAssertExists() {
        D dummyObject = getDummyObject();
        assertNotNull(desc()+" does not exist", dummyObject);
        return dummyObject;
    }

    public DummyObjectAsserter<D,R> assertName(String expected) {
        assertEquals("Wrong name in "+desc(), expected, getDummyObjectAssertExists().getName());
        return this;
    }

    public DummyObjectAsserter<D,R> assertId(String expected) {
        assertEquals("Wrong id in "+desc(), expected, getDummyObjectAssertExists().getId());
        return this;
    }

    public <T> DummyObjectAsserter<D,R> assertAttribute(String attributeName, T... expectedAttributeValues) {

        Set<Object> values = getDummyObjectAssertExists().getAttributeValues(attributeName, Object.class);
        if ((values == null || values.isEmpty()) && (expectedAttributeValues == null || expectedAttributeValues.length == 0)) {
            return this;
        }
        assertNotNull("No values for attribute "+attributeName+" of "+desc(), values);
        assertEquals("Unexpected number of values for attribute " + attributeName + " of " + desc() +
                ". Expected: " + Arrays.toString(expectedAttributeValues) + ", was: " + values,
                expectedAttributeValues.length, values.size());
        for (Object expectedValue: expectedAttributeValues) {
            if (!values.contains(expectedValue)) {
                AssertJUnit.fail("Value '"+expectedValue+"' expected in attribute "+attributeName+" of " + desc() +
                        " but not found. Values found: "+values);
            }
        }
        return this;
    }

    public <T> DummyObjectAsserter<D,R> assertNoAttribute(String attributeName) {
        Set<Object> values = getDummyObjectAssertExists().getAttributeValues(attributeName, Object.class);
        if ((values != null && !values.isEmpty())) {
            fail("Unexpected values for attribute " + attributeName + " of " + desc() + values);
        }
        return this;
    }

    public DummyObjectAsserter<D,R> assertEnabled() {
        assertTrue(desc() + " is disabled", getDummyObjectAssertExists().isEnabled());
        return this;
    }

    public DummyObjectAsserter<D,R> assertLastModifier(String expected) {
        assertEquals("Wrong lastModifier in " + desc(), expected, getDummyObjectAssertExists().getLastModifier());
        return this;
    }

    protected String desc() {
        if (dummyResourceName == null) {
            return descWithDetails(dummyObject) + " on default dummy resource";
        } else {
            return descWithDetails(dummyObject.toString()) + " on dummy resource '"+dummyResourceName+"'";
        }
    }

    public DummyObjectAsserter<D,R> display() {
        display(desc());
        return this;
    }

    public DummyObjectAsserter<D,R> display(String message) {
        PrismTestUtil.display(message, dummyObject);
        return this;
    }
}
