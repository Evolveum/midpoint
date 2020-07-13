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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

public class ValueMetadataAsserter<RA extends AbstractAsserter> extends PrismContainerValueAsserter<ValueMetadataType, RA> {

    public ValueMetadataAsserter(PrismContainerValue<ValueMetadataType> valueMetadata, RA parentAsserter, String details) {
        super(valueMetadata, parentAsserter, details);
    }

    @Override
    public ValueMetadataAsserter<RA> assertSize(int expected) {
        return (ValueMetadataAsserter<RA>) super.assertSize(expected);
    }

    @Override
    public ValueMetadataAsserter<RA> assertItems(QName... expectedItems) {
        return (ValueMetadataAsserter<RA>) super.assertItems(expectedItems);
    }

    @Override
    public ValueMetadataAsserter<RA> assertAny() {
        return (ValueMetadataAsserter<RA>) super.assertAny();
    }

    @Override
    public <T> ValueMetadataAsserter<RA> assertPropertyValuesEqual(ItemPath path, T... expectedValues) {
        return (ValueMetadataAsserter<RA>) super.assertPropertyValuesEqual(path, expectedValues);
    }

    @Override
    public <T> ValueMetadataAsserter<RA> assertPropertyValuesEqualRaw(ItemPath path, T... expectedValues) {
        return (ValueMetadataAsserter<RA>) super.assertPropertyValuesEqualRaw(path, expectedValues);
    }

    @Override
    public <T> ValueMetadataAsserter<RA> assertNoItem(QName itemName) {
        return (ValueMetadataAsserter<RA>) super.assertNoItem(itemName);
    }

    public ProvenanceMetadataAsserter<ValueMetadataAsserter<RA>> provenance() {
        return new ProvenanceMetadataAsserter<>(getPrismValue().asContainerable().getProvenance(),
                this, "provenance in " + getDetails());
    }

    @Override
    public <CC extends Containerable> PrismContainerValueAsserter<CC, ValueMetadataAsserter<RA>> containerSingle(QName subcontainerQName) {
        return (PrismContainerValueAsserter<CC, ValueMetadataAsserter<RA>>) super.containerSingle(subcontainerQName);
    }


    protected String desc() {
        // TODO handling of details
        return "metadata of " + getDetails();
    }

    @Override
    public ValueMetadataAsserter<RA> display() {
        return (ValueMetadataAsserter<RA>) super.display();
    }
}
