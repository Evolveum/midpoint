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

import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeValue;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import javax.xml.namespace.QName;

/**
 * Asserts about (non-raw) reference attribute values.
 */
public class ShadowReferenceAttributeAsserter<R> extends AbstractAsserter<R> {

    private final Collection<ShadowReferenceAttributeValue> values;

    ShadowReferenceAttributeAsserter(Collection<ShadowReferenceAttributeValue> values, R returnAsserter, String details) {
        super(returnAsserter, details);
        this.values = values;
    }

    public ShadowReferenceAttributeAsserter<R> assertSize(int expected) {
        assertEquals("Wrong number of values in "+desc(), expected, values.size());
        return this;
    }

    public ShadowReferenceAttributeAsserter<R> assertShadowOids(String... expectedShadowOids) {
        for (String expectedShadowOid : expectedShadowOids) {
            var refAttrValue = findByShadowOid(expectedShadowOid);
            if (refAttrValue == null) {
                fail(String.format(
                        "Expected referenced shadow OID %s in %s but there was none. Values present: %s",
                        expectedShadowOid, desc(), prettyPrintShadowOids()));
            }
        }
        for (var existingValue : values) {
            if (!ArrayUtils.contains(expectedShadowOids, existingValue.asObjectReferenceType().getOid())) {
                fail(String.format(
                        "Unexpected referenced shadow OID %s in %s. Expected shadow OIDs: %s",
                        existingValue.asObjectReferenceType().getOid(), desc(), ArrayUtils.toString(expectedShadowOids)));
            }
        }
        return this;
    }

    public ShadowReferenceAsserter<ShadowReferenceAttributeAsserter<R>> singleShadowRef() {
        assertSize(1);
        PrismReferenceValue refVal = values.iterator().next().asObjectReferenceType().asReferenceValue();
        ShadowReferenceAsserter<ShadowReferenceAttributeAsserter<R>> asserter =
                new ShadowReferenceAsserter<>(refVal, null, this, "shadowRef in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    public ShadowReferenceAttributeValueAsserter<ShadowReferenceAttributeAsserter<R>> forShadowOid(String shadowOid) {
        var value = findByShadowOid(shadowOid);
        assertThat(value).as("reference attribute value with shadow OID " + shadowOid).isNotNull();
        ShadowReferenceAttributeValueAsserter<ShadowReferenceAttributeAsserter<R>> asserter =
                new ShadowReferenceAttributeValueAsserter<>(value, this, "ref attr value in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    private @Nullable ShadowReferenceAttributeValue findByShadowOid(String shadowOid) {
        for (var value : values) {
            if (shadowOid.equals(value.asObjectReferenceType().getOid())) {
                return value;
            }
        }
        return null;
    }

    public @NotNull ShadowReferenceAttributeValueAsserter<ShadowReferenceAttributeAsserter<R>> forPrimaryIdentifierValue(
            @NotNull String value) {
        return matching(primaryIdentifierValuePredicate(value), "primary identifier value '" + value + "'");
    }

    public @NotNull ShadowReferenceAttributeValueAsserter<ShadowReferenceAttributeAsserter<R>> forAttributeValue(
            @NotNull QName attrName, @NotNull Object value) {
        return matching(primaryAttributeValuePredicate(attrName, value), "attribute '%s' value '%s'".formatted(attrName, value));
    }

    private @NotNull ShadowReferenceAttributeValueAsserter<ShadowReferenceAttributeAsserter<R>> matching(
            @NotNull Predicate<ShadowReferenceAttributeValue> predicate, String predicateDescription) {
        var refAttrValue = find(predicate);
        assertThat(refAttrValue).as("reference attribute with " + predicateDescription).isNotNull();
        ShadowReferenceAttributeValueAsserter<ShadowReferenceAttributeAsserter<R>> asserter =
                new ShadowReferenceAttributeValueAsserter<>(refAttrValue, this, "ref attr value in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    private @Nullable ShadowReferenceAttributeValue findByPrimaryIdentifierValue(@NotNull String uid) {
        return find(primaryIdentifierValuePredicate(uid));
    }

    private static @NotNull Predicate<ShadowReferenceAttributeValue> primaryIdentifierValuePredicate(@NotNull String uid) {
        return value -> {
            try {
                return uid.equals(value.getShadowRequired().getPrimaryIdentifierValueFromAttributes());
            } catch (SchemaException e) {
                throw new AssertionError(e);
            }
        };
    }

    private static @NotNull Predicate<ShadowReferenceAttributeValue> primaryAttributeValuePredicate(
            @NotNull QName attrName, @NotNull Object value) {
        return refValue -> refValue.getShadowRequired().getAttributeRealValues(attrName).contains(value);
    }

    private @Nullable ShadowReferenceAttributeValue find(@NotNull Predicate<ShadowReferenceAttributeValue> predicate) {
        return values.stream()
                .filter(predicate)
                .findFirst().orElse(null);
    }

    private String prettyPrintShadowOids() {
        StringBuilder sb = new StringBuilder();
        var iterator = values.iterator();
        while (iterator.hasNext()) {
            sb.append(PrettyPrinter.prettyPrint(iterator.next().asObjectReferenceType().getOid()));
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public ShadowReferenceAttributeAsserter<R> assertAny() {
        assertNotNull("No reference attr values in "+desc(), values);
        assertFalse("No reference attr values in "+desc(), values.isEmpty());
        return this;
    }

    public <T> ShadowReferenceAttributeAsserter<R> assertNone() {
        assertTrue("Unexpected reference attr values in "+desc()+": "+ values, values.isEmpty());
        return this;
    }

    protected String desc() {
        return getDetails();
    }

    public @NotNull ObjectReferenceType getSingleTargetRef() {
        assertSize(1);
        var ref = values.iterator().next().asObjectReferenceType();
        assertThat(ref).as("target ref in " + desc()).isNotNull();
        return ref;
    }
}
