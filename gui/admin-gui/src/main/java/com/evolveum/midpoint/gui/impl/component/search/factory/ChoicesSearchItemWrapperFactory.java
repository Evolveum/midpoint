/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.ChoicesSearchItemWrapper;

import com.evolveum.midpoint.util.DOMUtil;

import com.evolveum.midpoint.util.DisplayableValue;

import com.evolveum.midpoint.gui.impl.component.search.SearchValue;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ChoicesSearchItemWrapperFactory<T extends Serializable> extends AbstractSearchItemWrapperFactory<T, ChoicesSearchItemWrapper<T>> {


    @Override
    protected ChoicesSearchItemWrapper<T> createSearchWrapper(SearchItemContext ctx) {
        if (DOMUtil.XSD_BOOLEAN.equals(ctx.getValueTypeName())) {
            List<DisplayableValue<Boolean>> list = new ArrayList<>();
            list.add(new SearchValue<>(Boolean.TRUE, "Boolean.TRUE"));
            list.add(new SearchValue<>(Boolean.FALSE, "Boolean.FALSE"));
            return new ChoicesSearchItemWrapper(ctx.getPath(), list);
        }
        return new ChoicesSearchItemWrapper(ctx.getPath(), ctx.getAvailableValues());
    }

    @Override
    public boolean match(@NotNull SearchItemContext ctx) {
        return CollectionUtils.isNotEmpty(ctx.getAvailableValues()) || DOMUtil.XSD_BOOLEAN.equals(ctx.getValueTypeName());
    }
}
