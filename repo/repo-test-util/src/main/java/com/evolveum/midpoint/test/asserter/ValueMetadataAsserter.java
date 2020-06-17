/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

public class ValueMetadataAsserter<O extends ObjectType, PA extends AbstractAsserter<RA>, RA> extends PrismContainerValueAsserter<ValueMetadataType, PA> {

    private final PA parentAsserter;

    public ValueMetadataAsserter(PA parentAsserter, PrismContainerValue<ValueMetadataType> valueMetadata) {
        super(valueMetadata);
        this.parentAsserter = parentAsserter;
    }

    public ValueMetadataAsserter(PA parentAsserter, PrismContainerValue<ValueMetadataType> valueMetadata, String details) {
        super(valueMetadata, details);
        this.parentAsserter = parentAsserter;
    }

    @Override
    public ValueMetadataAsserter<O, PA,RA> assertSize(int expected) {
        return (ValueMetadataAsserter<O, PA, RA>) super.assertSize(expected);
    }

    @Override
    public ValueMetadataAsserter<O, PA,RA> assertItems(QName... expectedItems) {
        return (ValueMetadataAsserter<O, PA, RA>) super.assertItems(expectedItems);
    }

    @Override
    public ValueMetadataAsserter<O, PA,RA> assertAny() {
        return (ValueMetadataAsserter<O, PA, RA>) super.assertAny();
    }

    @Override
    public <T> ValueMetadataAsserter<O, PA,RA> assertPropertyValuesEqual(ItemPath path, T... expectedValues) {
        return (ValueMetadataAsserter<O, PA, RA>) super.assertPropertyValuesEqual(path, expectedValues);
    }

    @Override
    public <T> ValueMetadataAsserter<O, PA,RA> assertPropertyValuesEqualRaw(ItemPath path, T... expectedValues) {
        return (ValueMetadataAsserter<O, PA, RA>) super.assertPropertyValuesEqualRaw(path, expectedValues);
    }

    @Override
    public <T> ValueMetadataAsserter<O, PA,RA> assertNoItem(QName itemName) {
        return (ValueMetadataAsserter<O, PA, RA>) super.assertNoItem(itemName);
    }

    @Override
    public <CC extends Containerable> PrismContainerValueAsserter<CC, ValueMetadataAsserter<O, PA,RA>> containerSingle(QName subcontainerQName) {
        return (PrismContainerValueAsserter<CC, ValueMetadataAsserter<O, PA, RA>>) super.containerSingle(subcontainerQName);
    }


    protected String desc() {
        // TODO handling of details
        return "metadata of " + getDetails() + " of " + parentAsserter.desc();
    }

    @Override
    public PA end() {
        return parentAsserter;
    }

    @Override
    public ValueMetadataAsserter<O, PA,RA> display() {
        return (ValueMetadataAsserter<O, PA, RA>) super.display();
    }
}
