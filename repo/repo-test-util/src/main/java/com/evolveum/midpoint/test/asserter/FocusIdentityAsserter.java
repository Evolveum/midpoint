/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FocusIdentityAsserter<RA> extends PrismContainerValueAsserter<FocusIdentityType, RA> {

    FocusIdentityAsserter(PrismContainerValue<FocusIdentityType> prismValue, RA returnAsserter, String detail) {
        super(prismValue, returnAsserter, detail);
    }

    public FocusIdentityAsserter<RA> assertDataItem(ItemPath path, Object... expected)
            throws SchemaException {
        //noinspection unchecked
        assertThat((Collection<Object>) getDataValues(path))
                .as(() -> "Original value of '" + path + "'")
                .containsExactlyInAnyOrder(expected);
        return this;
    }

    private Collection<?> getDataValues(ItemPath path) throws SchemaException {
        return
                getRealValues(getData()
                        .asPrismContainerValue()
                        .findItem(path));
    }

    private @NotNull Collection<?> getRealValues(Item<?, ?> item) {
        return item != null ? item.getRealValues() : List.of();
    }

    private @NotNull FocusIdentityType getIdentity() throws SchemaException {
        return getRealValueRequired(FocusIdentityType.class);
    }

    private @NotNull FocusType getData() throws SchemaException {
        return MiscUtil.requireNonNull(
                getIdentity().getData(),
                () -> new AssertionError("No 'data' in " + getDetails()));
    }
}
