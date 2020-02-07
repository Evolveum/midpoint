/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import org.testng.AssertJUnit;

import javax.xml.namespace.QName;

/**
 * @author semancik
 */
public class ObjectFilterAsserter<RA> extends AbstractAsserter<RA> {

    private ObjectFilter filter;

    public ObjectFilterAsserter(ObjectFilter filter) {
        super();
        this.filter = filter;
    }

    public ObjectFilterAsserter(ObjectFilter filter, String detail) {
        super(detail);
        this.filter = filter;
    }

    public ObjectFilterAsserter(ObjectFilter filter, RA returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.filter = filter;
    }

    public static ObjectFilterAsserter<Void> forDelta(ObjectFilter filter) {
        return new ObjectFilterAsserter<>(filter);
    }

    public ObjectFilter getFilter() {
        return filter;
    }

    public ObjectFilterAsserter<RA> assertNull() {
        AssertJUnit.assertNull("Unexpected " + desc(), filter);
        return this;
    }

    public ObjectFilterAsserter<RA> assertNotNull() {
        AssertJUnit.assertNotNull("Null " + desc(), filter);
        return this;
    }

    public ObjectFilterAsserter<RA> assertClass(Class<? extends ObjectFilter> expectedClass) {
        assertNotNull();
        if (!expectedClass.isAssignableFrom(filter.getClass())) {
            fail("Expected that filter will be of class "+expectedClass.getSimpleName()+", but was "+filter.getClass().getSimpleName()+", in "+desc());
        }
        return this;
    }

    public ObjectFilterAsserter<RA> assertNone() {
        if (!ObjectQueryUtil.isNone(filter)) {
            fail("Expected that filter will be NONE, but was "+filter+", in "+desc());
        }
        return this;
    }

    public ObjectFilterAsserter<RA> assertAll() {
        if (!ObjectQueryUtil.isAll(filter)) {
            fail("Expected that filter will be NONE, but was "+filter+", in "+desc());
        }
        return this;
    }

    public ObjectFilterAsserter<RA> assertEq() {
        assertClass(EqualFilter.class);
        return this;
    }

    public ObjectFilterAsserter<RA> assertEq(ItemPath expectedItemPath, Object expectedValue) {
        assertClass(EqualFilter.class);
        ItemPath actualItemPath = ((EqualFilter) filter).getPath();
        AssertJUnit.assertTrue("Wrong path in eq clause, expected "+expectedItemPath+", but was "+actualItemPath+"; in "+desc(), expectedItemPath.equivalent(actualItemPath));
        PrismValue actualPVal = ((EqualFilter) filter).getSingleValue();
        Object actualValue = actualPVal.getRealValue();
        AssertJUnit.assertTrue("Wrong value in eq clause, expected "+expectedValue+", but was "+actualValue+"; in "+desc(), expectedValue.equals(actualValue));
        return this;
    }

    public ObjectFilterAsserter<RA> assertType() {
        assertClass(TypeFilter.class);
        return this;
    }

    public ObjectFilterAsserter<RA> assertType(QName expectedTypeName) {
        assertClass(TypeFilter.class);
        QName actualType = ((TypeFilter) filter).getType();
        AssertJUnit.assertTrue("Wrong type in type clause, expected "+expectedTypeName+", but was "+actualType+"; in "+desc(), expectedTypeName.equals(actualType));
        return this;
    }

    public ObjectFilterAsserter<ObjectFilterAsserter<RA>> type(QName expectedTypeName) {
        assertType(expectedTypeName);
        ObjectFilter subfilter = ((TypeFilter) this.filter).getFilter();
        ObjectFilterAsserter<ObjectFilterAsserter<RA>> asserter = new ObjectFilterAsserter<>(subfilter, this, "type subfilter in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    // TODO

    protected String desc() {
        return descWithDetails(filter);
    }

    public ObjectFilterAsserter<RA> display() {
        display(desc());
        return this;
    }

    public ObjectFilterAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, filter);
        return this;
    }


}
