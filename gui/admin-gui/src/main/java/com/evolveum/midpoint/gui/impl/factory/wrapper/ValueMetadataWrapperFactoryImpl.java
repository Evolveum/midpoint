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

import java.util.Collections;
import java.util.List;

public class ValueMetadataWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<Containerable> {

    private GuiComponentRegistry registry;

    ValueMetadataWrapperFactoryImpl(GuiComponentRegistry registry) {
        this.registry = registry;
    }

    public PrismContainerWrapper<Containerable> createWrapper(PrismContainerValueWrapper<?> parent, Item childItem, ItemStatus status, WrapperContext context) throws SchemaException {
        WrapperContext ctx = context.clone();
        ctx.setMetadata(true);
        return super.createWrapper(parent, childItem, status, ctx);
    }

    @Override
    public PrismContainerValueWrapper<Containerable> createValueWrapper(PrismContainerWrapper<Containerable> parent, PrismContainerValue<Containerable> value, ValueStatus status, WrapperContext context) throws SchemaException {
        WrapperContext ctx = context.clone();
        ctx.setMetadata(true);
        ctx.setCreateOperational(true);
        PrismContainerValueWrapper<Containerable> v = super.createValueWrapper(parent, value, status, ctx);
        return v;
    }

    @Override
    protected boolean shouldBeExpanded(PrismContainerWrapper<Containerable> parent, PrismContainerValue<Containerable> value, WrapperContext context) {
        return true;
    }

    @Override
    protected boolean shouldCreateEmptyValue(PrismContainer<Containerable> item, WrapperContext context) {
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

    @Override
    protected ItemWrapper<?, ?> createChildWrapper(ItemDefinition<?> def, PrismContainerValueWrapper<?> containerValueWrapper, WrapperContext context) throws SchemaException {
        ItemWrapper<?, ?> child = super.createChildWrapper(def, containerValueWrapper, context);
        //TODO ugly hack. find out something smarter
        if (ItemStatus.ADDED == child.getStatus()) {
            return null;
        }

        return child;
    }
}
