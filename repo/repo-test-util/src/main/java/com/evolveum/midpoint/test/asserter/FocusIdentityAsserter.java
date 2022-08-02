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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityItemsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IdentityItemsType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TemporaryUserType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FocusIdentityAsserter<RA> extends PrismContainerValueAsserter<FocusIdentityType, RA> {

    FocusIdentityAsserter(PrismContainerValue<FocusIdentityType> prismValue, RA returnAsserter, String detail) {
        super(prismValue, returnAsserter, detail);
    }

    public FocusIdentityAsserter<RA> assertOriginalItem(ItemPath path, Object... expected)
            throws SchemaException {
        //noinspection unchecked
        assertThat((Collection<Object>) getOrigValues(path))
                .as(() -> "Normalized value of '" + path + "'")
                .containsExactlyInAnyOrder(expected);
        return this;
    }

    public FocusIdentityAsserter<RA> assertNormalizedItem(String name, Object... expected)
            throws SchemaException {
        //noinspection unchecked
        assertThat((Collection<Object>) getNormValues(name))
                .as(() -> "Normalized value of '" + name + "'")
                .containsExactlyInAnyOrder(expected);
        return this;
    }

    private Collection<?> getOrigValues(ItemPath path) throws SchemaException {
        return
                getRealValues(getData()
                        .asPrismContainerValue()
                        .findItem(path));
    }

    private @NotNull Collection<?> getRealValues(Item<?, ?> item) {
        return item != null ? item.getRealValues() : List.of();
    }

    private Collection<?> getNormValues(String name) throws SchemaException {
        return
                getNorm()
                        .asPrismContainerValue()
                        .findItem(new ItemName(name))
                        .getRealValues();
    }

    @SuppressWarnings("SameParameterValue")
    private <T> T getValue(IdentityItemsType items, String name, Class<T> type) {
        Item<?, ?> item = items.asPrismContainerValue()
                .findItem(new ItemName(name));
        return JavaTypeConverter.convert(type, item.getRealValue());
    }

    private @NotNull FocusIdentityType getIdentity() throws SchemaException {
        return getRealValueRequired(FocusIdentityType.class);
    }

    private @NotNull TemporaryUserType getData() throws SchemaException {
        return MiscUtil.requireNonNull(
                getIdentity().getData(),
                () -> new AssertionError("No 'data' in " + getDetails()));
    }

    private @NotNull IdentityItemsType getNorm() throws SchemaException {
        return MiscUtil.requireNonNull(
                getItems().getNormalized(),
                () -> new AssertionError("No 'items/normalized' in " + getDetails()));
    }

    private @NotNull FocusIdentityItemsType getItems() throws SchemaException {
        return MiscUtil.requireNonNull(
                getIdentity().getItems(),
                () -> new AssertionError("No 'items' in " + getDetails()));
    }
}
