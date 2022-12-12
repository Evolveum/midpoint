/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.DeadShadowSearchItemWrapper;
import com.evolveum.midpoint.web.component.search.SearchValue;
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
        return ShadowType.F_DEAD.equivalent(ctx.getPath());
    }
}
