/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author semancik
 */
public class PrismPropertyValueAsserter<T, RA> extends PrismValueAsserter<PrismPropertyValue<T>, RA> {

    public PrismPropertyValueAsserter(PrismPropertyValue<T> prismValue) {
        super(prismValue);
    }

    public PrismPropertyValueAsserter(PrismPropertyValue<T> prismValue, String detail) {
        super(prismValue, detail);
    }

    public PrismPropertyValueAsserter(PrismPropertyValue<T> prismValue, RA returnAsserter, String detail) {
        super(prismValue, returnAsserter, detail);
    }

    public PrismPropertyValueAsserter<T,RA> assertValue(T expectedValue) {
        assertEquals("Wrong property value in "+desc(), expectedValue, getPrismValue().getValue());
        return this;
    }

    public PrismPropertyValueAsserter<T,RA> assertPolyStringValue(String expectedOrigValue) {
        assertEquals("Wrong property value in "+desc(), expectedOrigValue, ((PolyString)getPrismValue().getValue()).getOrig());
        return this;
    }

    public ProtectedStringAsserter<PrismPropertyValueAsserter<T,RA>> protectedString() {
        ProtectedStringAsserter<PrismPropertyValueAsserter<T,RA>> asserter = new ProtectedStringAsserter<PrismPropertyValueAsserter<T,RA>>((ProtectedStringType)getPrismValue().getValue(), this, desc());
        copySetupTo(asserter);
        return asserter;
    }

    // TODO

    protected String desc() {
        return getDetails();
    }

}
