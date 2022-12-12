/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
