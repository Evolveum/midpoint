/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusNormalizedDataType;

public class FocusNormalizedDataAsserter<RA> extends PrismContainerValueAsserter<FocusNormalizedDataType, RA> {

    FocusNormalizedDataAsserter(PrismContainerValue<FocusNormalizedDataType> prismValue, RA returnAsserter, String detail) {
        super(prismValue, returnAsserter, detail);
    }

    public FocusNormalizedDataAsserter<RA> assertNormalizedItem(String name, Object... expected) {
        //noinspection unchecked
        assertThat((Collection<Object>) getNormValues(name))
                .as(() -> "Normalized value of '" + name + "'")
                .containsExactlyInAnyOrder(expected);
        return this;
    }

    private Collection<?> getNormValues(String name) {
        return
                getPrismValue()
                        .findItem(new ItemName(name))
                        .getRealValues();
    }
}
