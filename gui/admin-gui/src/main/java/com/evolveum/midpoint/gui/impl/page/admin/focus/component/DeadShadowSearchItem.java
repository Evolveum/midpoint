/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.focus.component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.PropertySearchItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

public class DeadShadowSearchItem extends PropertySearchItem<Boolean> {

    public DeadShadowSearchItem(Search search, @NotNull SearchItemDefinition definition) {
        super(search, definition);
        setFixed(true);
    }

    @Override
    public ObjectFilter transformToFilter() {
        DisplayableValue<Boolean> selectedValue = getValue();
        if (selectedValue == null) {
            return null;
        }
        Boolean value = selectedValue.getValue();
        if (BooleanUtils.isTrue(value)) {
            return null; // let the default behavior to take their chance
        }

        return PrismContext.get().queryFor(ShadowType.class)
                .not()
                .item(ShadowType.F_DEAD)
                .eq(true)
                .buildFilter();
    }

    @Override
    protected boolean canRemoveSearchItem() {
        return false;
    }
}
