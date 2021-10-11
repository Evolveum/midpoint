/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class ExtensionAsserter<O extends ObjectType, OA extends PrismObjectAsserter<O,RA>, RA> extends PrismContainerValueAsserter<ExtensionType,OA> {

    private OA objectAsserter;

    public ExtensionAsserter(OA objectAsserter) {
        super((PrismContainerValue<ExtensionType>) objectAsserter.getObject().getExtensionContainerValue());
        this.objectAsserter = objectAsserter;
    }

    public ExtensionAsserter(OA objectAsserter, String details) {
        super((PrismContainerValue<ExtensionType>) objectAsserter.getObject().getExtensionContainerValue(), details);
        this.objectAsserter = objectAsserter;
    }

    private PrismObject<O> getObject() {
        return objectAsserter.getObject();
    }

    @Override
    public ExtensionAsserter<O,OA,RA> assertSize(int expected) {
        super.assertSize(expected);
        return this;
    }

    @Override
    public ExtensionAsserter<O,OA,RA> assertItems(QName... expectedItems) {
        super.assertItems(expectedItems);
        return this;
    }

    @Override
    public ExtensionAsserter<O,OA,RA> assertAny() {
        super.assertAny();
        return this;
    }

    @Override
    public <T> ExtensionAsserter<O,OA,RA> assertPropertyValuesEqual(QName propName, T... expectedValues) {
        super.assertPropertyValuesEqual(propName, expectedValues);
        return this;
    }

    @Override
    public <T> ExtensionAsserter<O,OA,RA> assertPropertyValuesEqualRaw(QName attrName, T... expectedValues) {
        super.assertPropertyValuesEqualRaw(attrName, expectedValues);
        return this;
    }

    @Override
    public <T> ExtensionAsserter<O,OA,RA> assertNoItem(QName itemName) {
        super.assertNoItem(itemName);
        return this;
    }

    @Override
    public ExtensionAsserter<O,OA,RA> assertTimestampBetween(QName propertyName, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) {
        super.assertTimestampBetween(propertyName, startTs, endTs);
        return this;
    }

    @Override
    public <CC extends Containerable> PrismContainerValueAsserter<CC,ExtensionAsserter<O,OA,RA>> containerSingle(QName subcontainerQName) {
        return (PrismContainerValueAsserter<CC, ExtensionAsserter<O, OA, RA>>) super.containerSingle(subcontainerQName);
    }


    protected String desc() {
        return "extension of " + descWithDetails(getObject());
    }

    @Override
    public OA end() {
        return objectAsserter;
    }

}
