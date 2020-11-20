/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class ExtensionAsserter<C extends Containerable, RA> extends PrismContainerValueAsserter<ExtensionType, RA> {

    private final C owner;

    public ExtensionAsserter(C owner, RA returnAsserter, String details) {
        super(ObjectTypeUtil.getExtensionContainerValue(owner), returnAsserter, details);
        this.owner = owner;
    }

    @Override
    public ExtensionAsserter<C,RA> assertSize(int expected) {
        super.assertSize(expected);
        return this;
    }

    @Override
    public ExtensionAsserter<C,RA> assertItems(QName... expectedItems) {
        super.assertItems(expectedItems);
        return this;
    }

    @Override
    public ExtensionAsserter<C,RA> assertAny() {
        super.assertAny();
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ExtensionAsserter<C,RA> assertPropertyValuesEqual(ItemPath path, T... expectedValues) {
        super.assertPropertyValuesEqual(path, expectedValues);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ExtensionAsserter<C,RA> assertPropertyValuesEqualRaw(ItemPath path, T... expectedValues) {
        super.assertPropertyValuesEqualRaw(path, expectedValues);
        return this;
    }

    @Override
    public ExtensionAsserter<C,RA> assertNoItem(ItemPath itemName) {
        super.assertNoItem(itemName);
        return this;
    }

    @Override
    public ExtensionAsserter<C,RA> assertTimestampBetween(ItemPath path, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) {
        super.assertTimestampBetween(path, startTs, endTs);
        return this;
    }

    @Override
    public <CC extends Containerable> PrismContainerValueAsserter<CC,ExtensionAsserter<C,RA>> containerSingle(QName subcontainerQName) {
        //noinspection unchecked
        return (PrismContainerValueAsserter<CC, ExtensionAsserter<C, RA>>) super.containerSingle(subcontainerQName);
    }

    protected String desc() {
        return "extension of " + descWithDetails(owner);
    }
}
