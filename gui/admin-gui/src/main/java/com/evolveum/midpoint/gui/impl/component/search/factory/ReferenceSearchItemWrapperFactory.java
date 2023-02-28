/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.ReferenceSearchItemWrapper;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;


public class ReferenceSearchItemWrapperFactory extends AbstractSearchItemWrapperFactory<ObjectReferenceType, ReferenceSearchItemWrapper> {

    @Override
    protected ReferenceSearchItemWrapper createSearchWrapper(SearchItemContext ctx) {
        return new ReferenceSearchItemWrapper((PrismReferenceDefinition)ctx.getItemDef(), ctx.getParameterTargetType(), ctx.getContainerClassType());
    }

    @Override
    public boolean match(SearchItemContext ctx) {
        return ctx.getItemDef() instanceof PrismReferenceDefinition;
    }
}
