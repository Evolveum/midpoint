/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.VariableBindingDefSearchItemWrapper;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

import org.jetbrains.annotations.NotNull;

public class VariableBindingDefSearchItemWrapperFactory extends AbstractSearchItemWrapperFactory<VariableBindingDefinitionType, VariableBindingDefSearchItemWrapper> {

    @Override
    protected VariableBindingDefSearchItemWrapper createSearchWrapper(@NotNull SearchItemContext ctx) {
        return new VariableBindingDefSearchItemWrapper(ctx.getPath());
    }

    @Override
    public boolean match(@NotNull SearchItemContext ctx) {
        return QNameUtil.match(VariableBindingDefinitionType.COMPLEX_TYPE, ctx.getValueTypeName());
    }
}
