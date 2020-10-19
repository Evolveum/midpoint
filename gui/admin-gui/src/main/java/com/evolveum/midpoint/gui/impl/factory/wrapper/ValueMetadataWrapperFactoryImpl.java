/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import java.util.Collections;
import java.util.List;

public class ValueMetadataWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<ValueMetadataType> {

    private GuiComponentRegistry registry;

    ValueMetadataWrapperFactoryImpl(GuiComponentRegistry registry) {
        this.registry = registry;
    }

    public PrismContainerWrapper<ValueMetadataType> createWrapper(PrismContainerValueWrapper<?> parent, Item childItem, ItemStatus status, WrapperContext context) throws SchemaException {
        WrapperContext ctx = context.clone();
        ctx.setMetadata(true);
        return super.createWrapper(parent, childItem, status, ctx);
    }

    @Override
    public PrismContainerValueWrapper<ValueMetadataType> createValueWrapper(PrismContainerWrapper<ValueMetadataType> parent, PrismContainerValue<ValueMetadataType> value, ValueStatus status, WrapperContext context) throws SchemaException {
        WrapperContext ctx = context.clone();
        ctx.setMetadata(true);
        ctx.setCreateOperational(true);
        PrismContainerValueWrapper<ValueMetadataType> v = super.createValueWrapper(parent, value, status, ctx);
        return v;
    }

    @Override
    protected boolean shouldBeExpanded(PrismContainerWrapper<ValueMetadataType> parent, PrismContainerValue<ValueMetadataType> value, WrapperContext context) {
        return true;
    }

    @Override
    protected boolean shouldCreateEmptyValue(PrismContainer<ValueMetadataType> item, WrapperContext context) {
        return false;
    }

//    @Override
//    protected List<? extends ItemDefinition> getItemDefinitions(PrismContainerWrapper<Containerable> parent, PrismContainerValue<Containerable> value) {
//        if (value == null || value.getComplexTypeDefinition() == null) {
//            return Collections.emptyList();
//        }
//        return value.getComplexTypeDefinition().getDefinitions();
//    }

    @Override
    public GuiComponentRegistry getRegistry() {
        return registry;
    }

}
