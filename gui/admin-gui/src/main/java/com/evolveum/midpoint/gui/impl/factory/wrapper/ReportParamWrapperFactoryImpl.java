/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ReportParamWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<ReportParameterType> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return super.match(def) && QNameUtil.match(def.getTypeName(), ReportParameterType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 10;
    }

    @Override
    protected List<? extends ItemDefinition> getItemDefinitions(
            PrismContainerWrapper<ReportParameterType> parent, PrismContainerValue<ReportParameterType> value) {
        List<ItemDefinition> defs = new ArrayList<>();
        if (parent != null
                && parent.getItem() != null) {
            for (Item<?, ?> item : parent.getItem().getValue().getItems()) {
                defs.add(item.getDefinition());
            }
        }
        return defs;
    }

    @Override
    public PrismContainerWrapper<ReportParameterType> createWrapper(PrismContainerValueWrapper<?> parent, ItemDefinition<?> def, WrapperContext context) throws SchemaException {
        PrismContainerWrapper<ReportParameterType> container = super.createWrapper(parent, def, context);
        container.setReadOnly(true);
        return container;
    }

    @Override
    protected ItemWrapper<?, ?> createChildWrapper(ItemDefinition<?> def, PrismContainerValueWrapper<?> containerValueWrapper, WrapperContext context) throws SchemaException {
        ItemWrapper<?, ?> wrapper = super.createChildWrapper(def, containerValueWrapper, context);
        wrapper.setReadOnly(true);
        return wrapper;
    }
}
