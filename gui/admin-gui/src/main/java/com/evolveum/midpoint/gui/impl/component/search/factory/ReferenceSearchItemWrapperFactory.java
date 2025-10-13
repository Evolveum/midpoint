/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.ReferenceSearchItemWrapper;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;


public class ReferenceSearchItemWrapperFactory extends AbstractSearchItemWrapperFactory<ObjectReferenceType, ReferenceSearchItemWrapper> {

    @Override
    protected ReferenceSearchItemWrapper createSearchWrapper(SearchItemContext ctx) {
        return new ReferenceSearchItemWrapper(
                (PrismReferenceDefinition)ctx.getItemDef(),
                ctx.getPath(),
                ctx.getParameterTargetType(),
                ctx.getContainerClassType());
    }

    @Override
    public boolean match(SearchItemContext ctx) {
        return ctx.getItemDef() instanceof PrismReferenceDefinition;
    }
}
