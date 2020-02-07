/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.Map;
import java.util.Map.Entry;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author semancik
 */
public class PolyStringAsserter<RA> extends AbstractAsserter<RA> {

    private PolyString polystring;

    public PolyStringAsserter(PolyString polystring) {
        super();
        this.polystring = polystring;
    }

    public PolyStringAsserter(PolyString polystring, String detail) {
        super(detail);
        this.polystring = polystring;
    }

    public PolyStringAsserter(PolyString polystring, RA returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.polystring = polystring;
    }

    public static <O extends ObjectType> PolyStringAsserter<Void> forPolyString(PolyString polystring) {
        return new PolyStringAsserter<>(polystring);
    }

    public PolyString getPolyString() {
        return polystring;
    }

    public PolyStringAsserter<RA> assertOrig(String expected) {
        assertEquals("Wrong orig in "+desc(), expected, polystring.getOrig());
        return this;
    }

    public PolyStringAsserter<RA> assertNorm(String expected) {
        assertEquals("Wrong norm in "+desc(), expected, polystring.getNorm());
        return this;
    }

    public PolyStringAsserter<RA> assertLangs(String... expectedParams) {
        if (polystring.getLang() == null) {
            if (expectedParams.length == 0) {
                return this;
            } else {
                fail("No langs in "+desc());
            }
        }
        Map<String, String> expectedLangs = MiscUtil.paramsToMap(expectedParams);
        for (Entry<String, String> expectedLangEntry : expectedLangs.entrySet()) {
            String realLangValue = polystring.getLang().get(expectedLangEntry.getKey());
            assertEquals("Wrong lang "+expectedLangEntry.getKey()+" in "+desc(), expectedLangEntry.getValue(), realLangValue);
        }
        for (Entry<String, String> realLangEntry : polystring.getLang().entrySet()) {
            String expectedLangValue = expectedLangs.get(realLangEntry.getKey());
            assertEquals("Wrong lang "+realLangEntry.getKey()+" in "+desc(), expectedLangValue, realLangEntry.getValue());
        }
        return this;
    }

    public PolyStringAsserter<RA> assertNoLangs() {
        assertNull("Unexpected langs in "+desc(), polystring.getLang());
        return this;
    }

    protected String desc() {
        return descWithDetails(polystring);
    }

    public PolyStringAsserter<RA> display() {
        display(desc());
        return this;
    }

    public PolyStringAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, polystring);
        return this;
    }
}
