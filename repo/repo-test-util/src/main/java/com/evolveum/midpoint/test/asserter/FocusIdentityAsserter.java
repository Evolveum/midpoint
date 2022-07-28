/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityItemsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IdentityItemsType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_C;

import static org.assertj.core.api.Assertions.assertThat;

public class FocusIdentityAsserter<RA> extends PrismContainerValueAsserter<FocusIdentityType, RA> {

    FocusIdentityAsserter(PrismContainerValue<FocusIdentityType> prismValue, RA returnAsserter, String detail) {
        super(prismValue, returnAsserter, detail);
    }

    public FocusIdentityAsserter<RA> assertItem(String name, String expectedOrig, String expectedNorm)
            throws SchemaException {
        assertThat(getOrigStringValue(name))
                .as(() -> "Original value of '" + name + "'")
                .isEqualTo(expectedOrig);
        assertThat(getNormStringValue(name))
                .as(() -> "Normalized value of '" + name + "'")
                .isEqualTo(expectedNorm);
        return this;
    }

    private String getOrigStringValue(String name) throws SchemaException {
        return getValue(getOrig(), name, String.class);
    }

    private String getNormStringValue(String name) throws SchemaException {
        return getValue(getNorm(), name, String.class);
    }

    @SuppressWarnings("SameParameterValue")
    private <T> T getValue(IdentityItemsType items, String name, Class<T> type) {
        Item<?, ?> item = items.asPrismContainerValue()
                .findItem(new ItemName(NS_C, name));
        return JavaTypeConverter.convert(type, item.getRealValue());
    }

    @NotNull
    private FocusIdentityType getIdentity() throws SchemaException {
        return getRealValueRequired(FocusIdentityType.class);
    }

    private IdentityItemsType getOrig() throws SchemaException {
        return MiscUtil.requireNonNull(
                getItems().getOriginal(),
                () -> new AssertionError("No 'items/original' in " + getDetails()));
    }

    private IdentityItemsType getNorm() throws SchemaException {
        return MiscUtil.requireNonNull(
                getItems().getNormalized(),
                () -> new AssertionError("No 'items/normalized' in " + getDetails()));
    }

    private FocusIdentityItemsType getItems() throws SchemaException {
        return MiscUtil.requireNonNull(
                getIdentity().getItems(),
                () -> new AssertionError("No 'items' in " + getDetails()));
    }
}
