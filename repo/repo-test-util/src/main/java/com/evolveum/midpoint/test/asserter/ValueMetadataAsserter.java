/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.ValueSelector;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import static com.evolveum.midpoint.schema.util.ProvenanceMetadataUtil.hasOrigin;

public class ValueMetadataAsserter<RA extends AbstractAsserter> extends PrismContainerAsserter<ValueMetadataType, RA> {

    public ValueMetadataAsserter(PrismContainer<ValueMetadataType> valueMetadata, RA parentAsserter, String details) {
        super(valueMetadata, parentAsserter, details);
    }

    @Override
    public ValueMetadataAsserter<RA> assertSize(int expected) {
        return (ValueMetadataAsserter<RA>) super.assertSize(expected);
    }

    @Override
    public ValueMetadataValueAsserter<ValueMetadataAsserter<RA>> singleValue() {
        assertSize(1);
        return value(0);
    }

    public ValueMetadataValueAsserter<ValueMetadataAsserter<RA>> value(int index) {
        ValueMetadataValueAsserter<ValueMetadataAsserter<RA>> asserter =
                new ValueMetadataValueAsserter<>(getItem().getValues().get(index), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ValueMetadataValueAsserter<ValueMetadataAsserter<RA>> value(ValueSelector<PrismContainerValue<ValueMetadataType>> selector) {
        ValueMetadataValueAsserter<ValueMetadataAsserter<RA>> asserter =
                new ValueMetadataValueAsserter<>(getItem().getAnyValue(selector), this, getDetails());
        copySetupTo(asserter);
        return asserter;
    }

    public ValueMetadataValueAsserter<ValueMetadataAsserter<RA>> valueForOrigin(String originOid) {
        return value(pcv -> hasOrigin(pcv.asContainerable(), originOid));
    }

    protected String desc() {
        // TODO handling of details
        return "metadata of " + getDetails();
    }

    @Override
    public ValueMetadataAsserter<RA> assertHasDefinition() {
        super.assertHasDefinition();
        return this;
    }

    @Override
    public ValueMetadataAsserter<RA> display() {
        return (ValueMetadataAsserter<RA>) super.display();
    }
}
