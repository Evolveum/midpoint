/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttribute;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.processor.ShadowAttributesContainer;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.test.asserter.prism.PrismPropertyAsserter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @author semancik
 */
public class ShadowAttributesAsserter<R> extends AbstractAsserter<ShadowAsserter<R>> {

    private PrismContainer<ShadowAttributesType> attributesContainer;
    private ShadowAsserter<R> shadowAsserter;

    public ShadowAttributesAsserter(ShadowAsserter<R> shadowAsserter) {
        super();
        this.shadowAsserter = shadowAsserter;
    }

    public ShadowAttributesAsserter(ShadowAsserter<R> shadowAsserter, String details) {
        super(details);
        this.shadowAsserter = shadowAsserter;
    }

    private PrismObject<ShadowType> getShadow() {
        return shadowAsserter.getObject();
    }

    private PrismContainer<ShadowAttributesType> getAttributesContainer() {
        if (attributesContainer == null) {
            attributesContainer = getShadow().findContainer(ShadowType.F_ATTRIBUTES);
        }
        return attributesContainer;
    }

    private PrismContainerValue<ShadowAttributesType> getAttributes() {
        return getAttributesContainer().getValue();
    }

    public ShadowAttributesAsserter<R> assertResourceAttributeContainer() {
        assertTrue("Wrong type of attribute container in "+desc()+", expected ResourceAttributeContainer but was " + getAttributesContainer().getClass(), getAttributesContainer() instanceof ShadowAttributesContainer);
        return this;
    }

    public ShadowAttributesAsserter<R> assertSize(int expected) {
        assertEquals("Wrong number of attributes in "+desc(), expected, getAttributes().size());
        return this;
    }

    public ShadowAttributesAsserter<R> assertSizeAtLeast(int expected) {
        assertThat(getAttributes().size())
                .as("number of attributes in " + desc())
                .isGreaterThanOrEqualTo(expected);
        return this;
    }

    public ShadowAttributesAsserter<R> assertSizeCachingAware(int expected) {
        if (InternalsConfig.isShadowCachingOnByDefault()) {
            return assertSizeAtLeast(expected);
        } else {
            return assertSize(expected);
        }
    }

    // TODO better name
    public ShadowAttributesAsserter<R> assertAttributesCachingAware(QName... expectedAttributes) {
        if (InternalsConfig.isShadowCachingOnByDefault()) {
            return assertAttributesPresent(expectedAttributes);
        } else {
            return assertAttributesExactly(expectedAttributes);
        }
    }

    public ShadowAttributesAsserter<R> assertAttributesPresent(QName... expectedAttributes) {
        for (QName expectedAttribute: expectedAttributes) {
            PrismProperty<Object> attr = getAttributes().findProperty(ItemName.fromQName(expectedAttribute));
            if (attr == null) {
                fail("Expected attribute "+expectedAttribute+" in "+desc()+" but there was none. Attributes present: "+presentAttributeNames());
            }
        }
        return this;
    }

    public ShadowAttributesAsserter<R> assertAttributesExactly(QName... expectedAttributes) {
        for (QName expectedAttribute: expectedAttributes) {
            PrismProperty<Object> attr = getAttributes().findProperty(ItemName.fromQName(expectedAttribute));
            if (attr == null) {
                fail("Expected attribute "+expectedAttribute+" in "+desc()+" but there was none. Attributes present: "+presentAttributeNames());
            }
        }
        for (Item<?, ?> existingAttr : getAttributes().getItems()) {
            if (!QNameUtil.contains(expectedAttributes, existingAttr.getElementName())) {
                fail("Unexpected attribute "+existingAttr.getElementName()+" in "+desc()+". Expected attributes: "+QNameUtil.prettyPrint(expectedAttributes));
            }
        }
        return this;
    }

    // TODO: change to ShadowAttributeAsserter later
    public <T> PrismPropertyAsserter<T,ShadowAttributesAsserter<R>> simpleAttribute(String attrName) {
        PrismProperty<T> attribute = findSimpleAttribute(attrName);
        PrismPropertyAsserter<T,ShadowAttributesAsserter<R>> asserter = new PrismPropertyAsserter<>(attribute, this, "attribute "+attrName+" in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    // TODO: change to ShadowAttributeAsserter later
    public <T> PrismPropertyAsserter<T,ShadowAttributesAsserter<R>> simpleAttribute(QName attrName) {
        PrismProperty<T> attribute = findSimpleAttribute(attrName);
        PrismPropertyAsserter<T,ShadowAttributesAsserter<R>> asserter = new PrismPropertyAsserter<>(attribute, this, "attribute "+attrName+" in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    // assumes the shadow is not raw;
    // accepts also that the attribute may not exist (it is treated as zero-values attribute)
    public ShadowReferenceAttributeAsserter<ShadowAttributesAsserter<R>> referenceAttribute(QName attrName) {
        var attribute = (ShadowReferenceAttribute) findReferenceAttribute(attrName);
        var asserter = new ShadowReferenceAttributeAsserter<>(
                attribute != null ? attribute.getAttributeValues() : List.of(),
                this, "attribute "+attrName+" in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    public ShadowAttributesAsserter<R> assertAny() {
        assertNotNull("No attributes container in "+desc(), getAttributesContainer());
        PrismContainerValue<ShadowAttributesType> containerValue = getAttributesContainer().getValue();
        assertNotNull("No attributes container avlue in "+desc(), containerValue);
        assertFalse("No attributes in "+desc(), containerValue.isEmpty());
        return this;
    }

    private String presentAttributeNames() {
        StringBuilder sb = new StringBuilder();
        Iterator<Item<?, ?>> iterator = getAttributes().getItems().iterator();
        while (iterator.hasNext()) {
            sb.append(PrettyPrinter.prettyPrint(iterator.next().getElementName()));
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public <T> PrismPropertyAsserter<T, ShadowAttributesAsserter<R>> primaryIdentifier() {
        Collection<ShadowSimpleAttribute<?>> primaryIdentifiers = ShadowUtil.getPrimaryIdentifiers(getShadow());
        assertFalse("No primary identifier in "+desc(), CollectionUtils.isEmpty(primaryIdentifiers));
        assertEquals("Wrong # of primary identifiers in "+desc(), 1, primaryIdentifiers.size());
        return simpleAttribute(primaryIdentifiers.iterator().next().getElementName());
    }

    public ShadowAttributesAsserter<R> assertHasPrimaryIdentifier() {
        Collection<ShadowSimpleAttribute<?>> primaryIdentifiers = ShadowUtil.getPrimaryIdentifiers(getShadow());
        assertFalse("No primary identifiers in "+desc(), CollectionUtils.isEmpty(primaryIdentifiers));
        return this;
    }

    public ShadowAttributesAsserter<R> assertNoPrimaryIdentifier() {
        Collection<ShadowSimpleAttribute<?>> primaryIdentifiers = ShadowUtil.getPrimaryIdentifiers(getShadow());
        assertTrue("Unexpected primary identifiers in "+desc()+": "+primaryIdentifiers, CollectionUtils.isEmpty(primaryIdentifiers));
        return this;
    }

    public <T> PrismPropertyAsserter<T, ShadowAttributesAsserter<R>> secondaryIdentifier() {
        Collection<ShadowSimpleAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(getShadow());
        assertFalse("No secondary identifier in "+desc(), CollectionUtils.isEmpty(secondaryIdentifiers));
        assertEquals("Wrong # of secondary identifiers in "+desc(), 1, secondaryIdentifiers.size());
        return simpleAttribute(secondaryIdentifiers.iterator().next().getElementName());
    }

    public ShadowAttributesAsserter<R> assertHasSecondaryIdentifier() {
        Collection<ShadowSimpleAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(getShadow());
        assertFalse("No secondary identifiers in "+desc(), CollectionUtils.isEmpty(secondaryIdentifiers));
        return this;
    }

    public ShadowAttributesAsserter<R> assertNoSecondaryIdentifier() {
        Collection<ShadowSimpleAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(getShadow());
        assertTrue("Unexpected secondary identifiers in "+desc()+": "+secondaryIdentifiers, CollectionUtils.isEmpty(secondaryIdentifiers));
        return this;
    }

    public <T> ShadowAttributesAsserter<R> assertValue(QName attrName, T... expectedValues) {
        PrismProperty<T> property = findSimpleAttribute(attrName);
        assertNotNull("No attribute "+attrName+" in "+desc(), property);
        PrismAsserts.assertPropertyValueDesc(property, desc(), expectedValues);
        return this;
    }

    public ShadowAsserter<ShadowAttributesAsserter<R>> singleReferenceValueShadow(QName attrName) {
        var reference = (ShadowReferenceAttribute) findReferenceAttribute(attrName);
        assertNotNull("No reference attribute " + attrName + " in " + desc(), reference);
        var value = MiscUtil.extractSingleton(reference.getAttributeValues());
        assertNotNull("No reference attribute " + attrName + " value in " + desc(), value);
        AbstractShadow shadow = value.getShadow();
        assertNotNull("No reference attribute " + attrName + " value shadow in " + desc(), value);
        var asserter = new ShadowAsserter<>(shadow, this, "reference value object in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    public <T> ShadowAttributesAsserter<R> assertValueRaw(QName attrName, T... expectedValues) {
        PrismProperty<T> property = findSimpleAttribute(attrName);
        assertNotNull("No attribute "+attrName+" in "+desc(), property);
        RawType[] expectedRaw = rawize(attrName, expectedValues);
        PrismAsserts.assertPropertyValueDesc(property, desc(), (T[])expectedRaw);
        return this;
    }

    private <T> RawType[] rawize(QName attrName, T[] expectedValues) {
        RawType[] raws = new RawType[expectedValues.length];
        for(int i = 0; i < expectedValues.length; i++) {
            raws[i] = new RawType(PrismContext.get().itemFactory().createPropertyValue(expectedValues[i]), attrName);
        }
        return raws;
    }

    public <T> T getSimpleAttributeValue(QName attrName) {
        PrismProperty<T> property = findSimpleAttribute(attrName);
        assertNotNull("No attribute "+attrName+" in "+desc(), property);
        return property.getRealValue();
    }

    public ShadowAttributesAsserter<R> assertNoAttribute(QName attrName) {
        var attribute = findAttribute(attrName);
        assertNull("Unexpected attribute " + attrName + " in " + desc() + ": " + attribute, attribute);
        return this;
    }

    public <T> ShadowAttributesAsserter<R> assertNoSimpleAttribute(QName attrName) {
        PrismProperty<T> property = findSimpleAttribute(attrName);
        assertNull("Unexpected attribute "+attrName+" in "+desc()+": "+property, property);
        return this;
    }

    public <T> ShadowAttributesAsserter<R> assertNoSimpleAttributeIfNotCached(QName attrName) {
        if (!InternalsConfig.isShadowCachingOnByDefault()) {
            PrismProperty<T> property = findSimpleAttribute(attrName);
            assertNull("Unexpected attribute "+attrName+" in "+desc()+": "+property, property);
        }
        return this;
    }

    private <T> PrismProperty<T> findSimpleAttribute(QName attrName) {
        return getAttributes().findProperty(ItemName.fromQName(attrName));
    }

    private PrismReference findReferenceAttribute(QName attrName) {
        return getAttributes().findReference(ItemName.fromQName(attrName));
    }

    private Item<?, ?> findAttribute(QName attrName) {
        return getAttributes().findItem(ItemName.fromQName(attrName));
    }

    private <T> PrismProperty<T> findSimpleAttribute(String attrName) {
        return getAttributes().findProperty(ItemName.from(null, attrName));
    }

    protected String desc() {
        return descWithDetails(getShadow());
    }

    @Override
    public ShadowAsserter<R> end() {
        return shadowAsserter;
    }

}
