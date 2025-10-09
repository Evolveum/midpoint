/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.DeadShadowSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.Arrays;

public class DeadShadowSearchItemWrapperFactory extends AbstractSearchItemWrapperFactory<Boolean, DeadShadowSearchItemWrapper> {
    @Override
    protected DeadShadowSearchItemWrapper createSearchWrapper(SearchItemContext ctx) {
        DeadShadowSearchItemWrapper deadWrapper = new DeadShadowSearchItemWrapper(Arrays.asList(new SearchValue<>(true), new SearchValue<>(false)));
        deadWrapper.setValue(new SearchValue(false));
        return deadWrapper;
    }

    @Override
    public boolean match(SearchItemContext ctx) {
        return ShadowType.F_DEAD.equivalent(ctx.getPath()) && ctx.isVisible();
    }
}
