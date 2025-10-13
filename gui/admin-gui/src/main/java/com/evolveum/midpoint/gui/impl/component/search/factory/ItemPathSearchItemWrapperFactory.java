/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.ItemPathSearchItemWrapper;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class ItemPathSearchItemWrapperFactory extends AbstractSearchItemWrapperFactory<ItemPathType, ItemPathSearchItemWrapper> {

    @Override
    protected ItemPathSearchItemWrapper createSearchWrapper(SearchItemContext ctx) {
        return new ItemPathSearchItemWrapper(ctx.getPath());
    }

    @Override
    public boolean match(SearchItemContext ctx) {
        return QNameUtil.match(ItemPathType.COMPLEX_TYPE, ctx.getValueTypeName());
    }
}
