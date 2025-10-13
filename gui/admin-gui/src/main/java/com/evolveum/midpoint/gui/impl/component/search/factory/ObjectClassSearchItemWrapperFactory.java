/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.ObjectClassSearchItemWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

public class ObjectClassSearchItemWrapperFactory extends AbstractSearchItemWrapperFactory<QName, ObjectClassSearchItemWrapper> {

    @Override
    protected ObjectClassSearchItemWrapper createSearchWrapper(SearchItemContext ctx) {
        return new ObjectClassSearchItemWrapper();
    }

    @Override
    public boolean match(SearchItemContext ctx) {
        return ShadowType.F_OBJECT_CLASS.equivalent(ctx.getPath());
    }
}
