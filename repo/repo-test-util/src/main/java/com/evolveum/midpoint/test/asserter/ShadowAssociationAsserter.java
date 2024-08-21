/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.util.Collection;
import java.util.function.Predicate;

import com.evolveum.midpoint.util.MiscUtil;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Asserts about (non-raw) association values.
 *
 * @author semancik
 */
public class ShadowAssociationAsserter<R> extends AbstractAsserter<R> {

    private final Collection<ShadowAssociationValue> values;

    ShadowAssociationAsserter(Collection<ShadowAssociationValue> values, R returnAsserter, String details) {
        super(returnAsserter, details);
        this.values = values;
    }

    public ShadowAssociationAsserter<R> assertSize(int expected) {
        assertEquals("Wrong number of values in "+desc(), expected, values.size());
        return this;
    }

    public ShadowAssociationAsserter<R> assertShadowOids(String... expectedShadowOids) {
        for (String expectedShadowOid: expectedShadowOids) {
            var association = findByShadowOid(expectedShadowOid);
            if (association == null) {
                fail(String.format(
                        "Expected association shadow OID %s in %s but there was none. Association present: %s",
                        expectedShadowOid, desc(), prettyPrintShadowOids()));
            }
        }
        for (var existingAssociation : values) {
            var actualObjectOid = existingAssociation.getSingleObjectRefRequired().getOid();
            if (!ArrayUtils.contains(expectedShadowOids, actualObjectOid)) {
                fail(String.format(
                        "Unexpected association shadow OID %s in %s. Expected shadow OIDs: %s",
                        actualObjectOid, desc(), ArrayUtils.toString(expectedShadowOids)));
            }
        }
        return this;
    }

//    public ShadowReferenceAsserter<ShadowAssociationAsserter<R>> singleShadowRef() {
//        assertSize(1);
//        PrismReferenceValue refVal = values.iterator().next().getShadowRef().asReferenceValue();
//        ShadowReferenceAsserter<ShadowAssociationAsserter<R>> asserter =
//                new ShadowReferenceAsserter<>(refVal, null, this, "shadowRef in "+desc());
//        copySetupTo(asserter);
//        return asserter;
//    }

    public ShadowAssociationValueAsserter<ShadowAssociationAsserter<R>> singleValue() {
        var value = MiscUtil.extractSingletonRequired(values);
        ShadowAssociationValueAsserter<ShadowAssociationAsserter<R>> asserter =
                new ShadowAssociationValueAsserter<>(value, this, "association value in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    public ShadowAssociationValueAsserter<ShadowAssociationAsserter<R>> singleValueSatisfying(
            Predicate<ShadowAssociationValue> predicate) {
        var value = MiscUtil.extractSingletonRequired(values, predicate);
        ShadowAssociationValueAsserter<ShadowAssociationAsserter<R>> asserter =
                new ShadowAssociationValueAsserter<>(value, this, "association value in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    /** Looks by default objectRef OID. */
    public ShadowAssociationValueAsserter<ShadowAssociationAsserter<R>> forShadowOid(String shadowOid) {
        var value = findByShadowOid(shadowOid);
        assertThat(value).as("association value with shadow OID " + shadowOid).isNotNull();
        ShadowAssociationValueAsserter<ShadowAssociationAsserter<R>> asserter =
                new ShadowAssociationValueAsserter<>(value, this, "association value in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    private @Nullable ShadowAssociationValue findByShadowOid(String shadowOid) {
        for (var value : values) {
            if (shadowOid.equals(value.getSingleObjectRefRequired().getOid())) {
                return value;
            }
        }
        return null;
    }

    private String prettyPrintShadowOids() {
        StringBuilder sb = new StringBuilder();
        var iterator = values.iterator();
        while (iterator.hasNext()) {
            sb.append(PrettyPrinter.prettyPrint(iterator.next().getSingleObjectRefRequired().getOid()));
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public ShadowAssociationAsserter<R> assertAny() {
        assertNotNull("No associations in "+desc(), values);
        assertFalse("No associations in "+desc(), values.isEmpty());
        return this;
    }

    public <T> ShadowAssociationAsserter<R> assertNone() {
        assertTrue("Unexpected association values in "+desc()+": "+ values, values.isEmpty());
        return this;
    }

    protected String desc() {
        return getDetails();
    }

    public @NotNull ObjectReferenceType getSingleTargetRef() {
        assertSize(1);
        var ref = values.iterator().next().getSingleObjectRefRequired();
        assertThat(ref).as("target ref in " + desc()).isNotNull();
        return ref;
    }
}
