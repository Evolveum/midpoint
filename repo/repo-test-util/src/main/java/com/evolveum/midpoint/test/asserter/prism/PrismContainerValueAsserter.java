/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author semancik
 */
public class PrismContainerValueAsserter<C extends Containerable, RA> extends PrismValueAsserter<PrismContainerValue<C>, RA> {

    public PrismContainerValueAsserter(PrismContainerValue<C> prismValue) {
        super(prismValue);
    }

    public PrismContainerValueAsserter(PrismContainerValue<C> prismValue, String detail) {
        super(prismValue, detail);
    }

    public PrismContainerValueAsserter(PrismContainerValue<C> prismValue, RA returnAsserter, String detail) {
        super(prismValue, returnAsserter, detail);
    }

    public PrismContainerValueAsserter<C,RA> assertSize(int expected) {
        assertEquals("Wrong number of items in "+desc(), expected, getPrismValue().size());
        return this;
    }

    public PrismContainerValueAsserter<C,RA> assertItems(QName... expectedItems) {
        for (QName expectedItem: expectedItems) {
            Item<PrismValue,ItemDefinition> item = getPrismValue().findItem(ItemName.fromQName(expectedItem));
            if (item == null) {
                fail("Expected item "+expectedItem+" in "+desc()+" but there was none. Items present: "+presentItemNames());
            }
        }
        for (Item<?, ?> existingItem : getPrismValue().getItems()) {
            if (!QNameUtil.contains(expectedItems, existingItem.getElementName())) {
                fail("Unexpected item "+existingItem.getElementName()+" in "+desc()+". Expected items: "+QNameUtil.prettyPrint(expectedItems));
            }
        }
        return this;
    }

    public PrismContainerValueAsserter<C,RA> assertAny() {
        assertNotNull("No container value in "+desc(), getPrismValue());
        assertFalse("No items in "+desc(), getPrismValue().isEmpty());
        return this;
    }

    private String presentItemNames() {
        StringBuilder sb = new StringBuilder();
        Iterator<Item<?, ?>> iterator = getPrismValue().getItems().iterator();
        while (iterator.hasNext()) {
            sb.append(PrettyPrinter.prettyPrint(iterator.next().getElementName()));
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private <T> PrismProperty<T> findProperty(QName attrName) {
        return getPrismValue().findProperty(ItemName.fromQName(attrName));
    }

    private <CC extends Containerable> PrismContainer<CC> findContainer(QName attrName) {
        return getPrismValue().findContainer(ItemName.fromQName(attrName));
    }

    private <T> Item<PrismValue,ItemDefinition> findItem(QName itemName) {
        return getPrismValue().findItem(ItemName.fromQName(itemName));
    }

    public <T> PrismContainerValueAsserter<C,RA> assertPropertyEquals(QName propName, T expected) {
        PrismProperty<T> prop = findProperty(propName);
        if (prop == null && expected == null) {
            return this;
        }
        assertNotNull("No property "+propName.getLocalPart()+" in "+desc(), prop);
        T realValue = prop.getRealValue();
        assertNotNull("No value in "+propName.getLocalPart()+" in "+desc(), realValue);
        assertEquals("Wrong "+propName.getLocalPart()+" in "+desc(), expected, realValue);
        return this;
    }

    public <T> PrismContainerValueAsserter<C,RA> assertPropertyValuesEqual(QName propName, T... expectedValues) {
        PrismProperty<T> property = findProperty(propName);
        assertNotNull("No property "+propName+" in "+desc(), property);
        PrismAsserts.assertPropertyValueDesc(property, desc(), expectedValues);
        return this;
    }

    public <T> PrismContainerValueAsserter<C,RA> assertPropertyValuesEqualRaw(QName attrName, T... expectedValues) {
        PrismProperty<T> property = findProperty(attrName);
        assertNotNull("No attribute "+attrName+" in "+desc(), property);
        RawType[] expectedRaw = rawize(attrName, getPrismContext(), expectedValues);
        PrismAsserts.assertPropertyValueDesc(property, desc(), (T[])expectedRaw);
        return this;
    }

    private <T> RawType[] rawize(QName attrName, PrismContext prismContext, T[] expectedValues) {
        RawType[] raws = new RawType[expectedValues.length];
        for(int i = 0; i < expectedValues.length; i++) {
            raws[i] = new RawType(prismContext.itemFactory().createPropertyValue(expectedValues[i]), attrName, prismContext);
        }
        return raws;
    }

    public <T> PrismContainerValueAsserter<C,RA> assertRefEquals(QName refName, String expectedOid) {
        PrismReference ref = getPrismValue().findReference(refName);
        if (ref == null && expectedOid == null) {
            return this;
        }
        assertNotNull("No reference "+refName.getLocalPart()+" in "+desc(), ref);
        List<PrismReferenceValue> refVals = ref.getValues();
        if (refVals == null || refVals.isEmpty()) {
            fail("No values in reference "+refName.getLocalPart()+" in "+desc());
        }
        if (refVals.size() > 1) {
            fail("Too many values in reference "+refName.getLocalPart()+" in "+desc());
        }
        PrismReferenceValue refVal = refVals.get(0);
        assertNotNull("null value in "+refName.getLocalPart()+" in "+desc(), refVal);
        assertEquals("Wrong "+refName.getLocalPart()+" in "+desc(), expectedOid, refVal.getOid());
        return this;
    }

    public <T> PrismContainerValueAsserter<C,RA> assertNoItem(QName itemName) {
        Item<PrismValue,ItemDefinition> item = findItem(itemName);
        assertNull("Unexpected item "+itemName+" in "+desc()+": "+item, item);
        return this;
    }

    public PrismContainerValueAsserter<C,RA> assertTimestampBetween(QName propertyName, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) {
        PrismProperty<XMLGregorianCalendar> property = findProperty(propertyName);
        assertNotNull("No property "+propertyName+" in "+desc(), property);
        XMLGregorianCalendar timestamp = property.getRealValue();
        assertNotNull("No value of property "+propertyName+" in "+desc(), timestamp);
        TestUtil.assertBetween("Wrong value of property "+propertyName+" in "+desc(), startTs, endTs, timestamp);
        return this;
    }

    public <CC extends Containerable> PrismContainerValueAsserter<CC,? extends PrismContainerValueAsserter<C,RA>> containerSingle(QName subcontainerQName) {
        PrismContainer<CC> container = findContainer(subcontainerQName);
        assertNotNull("No container "+subcontainerQName+" in "+desc(), container);
        PrismContainerValue<CC> pval = container.getValue();
        PrismContainerValueAsserter<CC,PrismContainerValueAsserter<C,RA>> asserter = new PrismContainerValueAsserter<>(pval, this, subcontainerQName.getLocalPart() + " in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    public <CC extends Containerable> PrismContainerAsserter<CC,? extends PrismContainerValueAsserter<C,RA>> container(QName subcontainerQName) {
        PrismContainer<CC> container = findContainer(subcontainerQName);
        assertNotNull("No container "+subcontainerQName+" in "+desc(), container);
        PrismContainerAsserter<CC,PrismContainerValueAsserter<C,RA>> asserter = new PrismContainerAsserter<>(container, this, subcontainerQName.getLocalPart() + " in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    public <T> PrismPropertyAsserter<T,? extends PrismContainerValueAsserter<C,RA>> property(QName propertyName) {
        PrismProperty<T> property = findProperty(propertyName);
        assertNotNull("No property "+propertyName+" in "+desc(), property);
        PrismPropertyAsserter<T,? extends PrismContainerValueAsserter<C,RA>> asserter = new PrismPropertyAsserter<>(property, this, propertyName.getLocalPart() + " in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    // TODO

    protected String desc() {
        return getDetails();
    }

}
