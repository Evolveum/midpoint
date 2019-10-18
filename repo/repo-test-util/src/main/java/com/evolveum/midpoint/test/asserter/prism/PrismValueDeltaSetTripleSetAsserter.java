/**
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collection;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.ContainerDeltaAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class PrismValueDeltaSetTripleSetAsserter<V extends PrismValue,RA> extends DeltaSetTripleSetAsserter<V,RA> {

    public PrismValueDeltaSetTripleSetAsserter(Collection<V> set) {
        super(set);
    }

    public PrismValueDeltaSetTripleSetAsserter(Collection<V> set, String detail) {
        super(set,detail);
    }

    public PrismValueDeltaSetTripleSetAsserter(Collection<V> set, RA returnAsserter, String detail) {
        super(set,returnAsserter, detail);
    }

    public static <V extends PrismValue> PrismValueDeltaSetTripleSetAsserter<V,Void> forSPrismValueet(Collection<V> set) {
        return new PrismValueDeltaSetTripleSetAsserter<>(set);
    }

    @Override
    public PrismValueDeltaSetTripleSetAsserter<V,RA> assertSize(int expected) {
        super.assertSize(expected);
        return this;
    }

    @Override
    public PrismValueDeltaSetTripleSetAsserter<V,RA> assertNone() {
        super.assertNone();
        return this;
    }

    @Override
    public PrismValueDeltaSetTripleSetAsserter<V,RA> assertNull() {
        super.assertNull();
        return this;
    }

    public <T> PrismPropertyValueAsserter<T, PrismValueDeltaSetTripleSetAsserter<V,RA>> singlePropertyValue(Class<T> type) {
        assertSize(1);
        V val = getSet().iterator().next();
        if (!(val instanceof PrismPropertyValue)) {
            fail("Expected that a single value of set will be a property value, but it was "+val+", in " + desc());
        }
        PrismPropertyValueAsserter<T, PrismValueDeltaSetTripleSetAsserter<V,RA>> vAsserter = new PrismPropertyValueAsserter<>((PrismPropertyValue<T>)val, this, "single property value in " + desc());
        copySetupTo(vAsserter);
        return vAsserter;
    }

    public <T> PrismValueDeltaSetTripleSetAsserter<V,RA> assertSinglePropertyValue(T expectedValue) {
        assertSize(1);
        V val = getSet().iterator().next();
        if (!(val instanceof PrismPropertyValue)) {
            fail("Expected that a single value of set will be a property value, but it was "+val+", in " + desc());
        }
        T value = ((PrismPropertyValue<T>)val).getValue();
        assertEquals("Wrong property value in "+desc(), expectedValue, value);
        return this;
    }

    @Override
    public PrismValueDeltaSetTripleSetAsserter<V,RA> display() {
        super.display();
        return this;
    }

    @Override
    public PrismValueDeltaSetTripleSetAsserter<V,RA> display(String message) {
        super.display(message);
        return this;
    }
}
