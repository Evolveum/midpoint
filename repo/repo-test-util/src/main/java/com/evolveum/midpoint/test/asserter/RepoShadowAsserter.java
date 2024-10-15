/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowReferenceAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts the raw repo shadows.
 *
 * TODO reconsider if it should extend {@link ShadowAsserter}
 */
@Experimental
public class RepoShadowAsserter<RA> extends ShadowAsserter<RA> {

    private final Collection<? extends QName> cachedAttributes;

    RepoShadowAsserter(ShadowType shadow, Collection<? extends QName> cachedAttributes, String details) {
        super(shadow.asPrismObject(), details);
        this.cachedAttributes = cachedAttributes;
    }

    public static RepoShadowAsserter<Void> forRepoShadow(
            RawRepoShadow shadow,
            Collection<? extends QName> cachedAttributes) {
        return forRepoShadow(shadow, cachedAttributes, "");
    }

    public static RepoShadowAsserter<Void> forRepoShadow(
            RawRepoShadow shadow,
            Collection<? extends QName> cachedAttributes,
            String details) {
        return new RepoShadowAsserter<>(shadow.getBean(), cachedAttributes, "");
    }

    public static RepoShadowAsserter<Void> forRepoShadow(
            PrismObject<ShadowType> shadow, Collection<? extends QName> cachedAttributes) {
        return new RepoShadowAsserter<>(shadow.asObjectable(), cachedAttributes, "");
    }

    public static RepoShadowAsserter<Void> forRepoShadow(
            ShadowType shadow, Collection<? extends QName> cachedAttributes) {
        return new RepoShadowAsserter<>(shadow, cachedAttributes, "");
    }

    public final RepoShadowAsserter<RA> assertCachedRefValues(String attrName, String... expectedOids) {
        return assertCachedRefValues(
                ItemName.from(MidPointConstants.NS_RI, attrName),
                expectedOids);
    }

    /** Attribute is present iff it is among {@link #cachedAttributes}. */
    public final RepoShadowAsserter<RA> assertCachedRefValues(QName attrName, String... expectedOidsIfCached) {
        PrismContainer<ShadowReferenceAttributesType> container = getObject().findContainer(ShadowType.F_REFERENCE_ATTRIBUTES);
        PrismReference reference = container != null ? container.findReference(ItemName.fromQName(attrName)) : null;
        Collection<String> actualOids = new ArrayList<>();
        if (reference != null) {
            for (PrismReferenceValue value : reference.getValues()) {
                actualOids.add(
                        MiscUtil.assertNonNull(
                                value.getOid(),
                                "No OID in reference attribute '%s' value: %s", attrName, value));
            }
        }
        Collection<String> expectedValues = isCached(attrName) ? List.of(expectedOidsIfCached) : List.of();
        assertThat(actualOids)
                .as("OIDs in reference attribute '" + attrName + "' in " + getObject())
                .containsExactlyInAnyOrderElementsOf(expectedValues);
        return this;
    }

    /** Attribute is present iff it is among {@link #cachedAttributes}. */
    @SafeVarargs
    public final <T> RepoShadowAsserter<RA> assertCachedOrigValues(String attrName, T... expectedValues) {
        return assertCachedOrigValues(
                ItemName.from(MidPointConstants.NS_RI, attrName),
                expectedValues);
    }

    /** Attribute is present iff it is among {@link #cachedAttributes}. */
    @SafeVarargs
    public final <T> RepoShadowAsserter<RA> assertCachedOrigValues(QName attrName, T... expectedValues) {
        return assertCachedOrigOrNormValues(attrName, RepoShadowAsserter::extractOrigValues, expectedValues);
    }

    /** Attribute is present iff it is among {@link #cachedAttributes}. */
    @SafeVarargs
    public final <T> RepoShadowAsserter<RA> assertCachedNormValues(String attrName, T... expectedValues) {
        return assertCachedNormValues(
                ItemName.from(MidPointConstants.NS_RI, attrName),
                expectedValues);
    }

    /** Attribute is present iff it is among {@link #cachedAttributes}. */
    @SafeVarargs
    public final <T> RepoShadowAsserter<RA> assertCachedNormValues(QName attrName, T... expectedValues) {
        return assertCachedOrigOrNormValues(attrName, RepoShadowAsserter::extractNormValues, expectedValues);
    }

    @SafeVarargs
    private <T> RepoShadowAsserter<RA> assertCachedOrigOrNormValues(
            QName attrName, Function<PrismProperty<?>, Collection<?>> extractor, T... expectedIfCached) {
        if (InternalsConfig.isShadowCachingOnByDefault() && expectedIfCached.length == 0) {
            // questionable but some assertions are too pessimistic regarding what's in the cache, hence this hack
            return this;
        }
        PrismContainer<ShadowAttributesType> container = getObject().findContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty<?> property = container != null ? container.findProperty(ItemName.fromQName(attrName)) : null;
        Collection<?> actualValues = extractor.apply(property);
        Collection<T> expectedValues = isCached(attrName) ? List.of(expectedIfCached) : List.of();
        //noinspection unchecked
        assertThat((Collection<T>) actualValues)
                .as("values of " + attrName + " in " + getObject())
                .containsExactlyInAnyOrderElementsOf(expectedValues);
        return this;
    }

    private boolean isCached(QName attrName) {
        return cachedAttributes.contains(attrName);
    }

    private static Collection<?> extractOrigValues(PrismProperty<?> property) {
        if (property != null) {
            List<Object> rv = new ArrayList<>();
            for (Object realValue : property.getRealValues()) {
                if (realValue instanceof PolyString polyString) {
                    rv.add(polyString.getOrig());
                } else {
                    rv.add(realValue);
                }
            }
            return rv;
        } else {
            return List.of();
        }
    }

    private static Collection<?> extractNormValues(PrismProperty<?> property) {
        if (property != null) {
            List<Object> rv = new ArrayList<>();
            for (Object realValue : property.getRealValues()) {
                if (realValue instanceof PolyString polyString) {
                    rv.add(polyString.getNorm());
                } else {
                    rv.add(realValue);
                }
            }
            return rv;
        } else {
            return List.of();
        }
    }

    @Override
    public RepoShadowAsserter<RA> assertName(String expectedOrig) {
        super.assertName(expectedOrig);
        return this;
    }

    public RepoShadowAsserter<RA> display() {
        super.display();
        return this;
    }

    public RepoShadowAsserter<RA> assertHasIndexedPrimaryIdentifierValue() {
        assertThat(getIndexedPrimaryIdentifierValue()).as("indexed primary identifier value").isNotNull();
        return this;
    }

    @Override
    public RepoShadowAsserter<RA> assertObjectClass(QName expected) {
        return (RepoShadowAsserter<RA>) super.assertObjectClass(expected);
    }

    @Override
    public RepoShadowAsserter<RA> assertAuxiliaryObjectClasses(QName... expected) {
        return (RepoShadowAsserter<RA>) super.assertAuxiliaryObjectClasses(expected);
    }

    @Override
    public RepoShadowAsserter<RA> assertKind(ShadowKindType expected) {
        return (RepoShadowAsserter<RA>) super.assertKind(expected);
    }

    @Override
    public RepoShadowAsserter<RA> assertIndexedPrimaryIdentifierValue(String expected) {
        return (RepoShadowAsserter<RA>) super.assertIndexedPrimaryIdentifierValue(expected);
    }

    @Override
    public RepoShadowAsserter<RA> assertAttributes(int expectedNumber) {
        if (InternalsConfig.isShadowCachingOnByDefault()) {
            return (RepoShadowAsserter<RA>) super.assertAttributesAtLeast(expectedNumber);
        } else {
            return (RepoShadowAsserter<RA>) super.assertAttributes(expectedNumber);
        }
    }

    public RawRepoShadow getRawRepoShadow() {
        return RawRepoShadow.of(getObject());
    }
}
