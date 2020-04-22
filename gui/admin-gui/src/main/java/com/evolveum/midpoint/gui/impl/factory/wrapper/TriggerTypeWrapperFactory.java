/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.List;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.factory.wrapper.ItemWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author Kate Honchar
 *
 */
//TODO review, dirrefence with MetadataWrapperFactory?
@Component
public class TriggerTypeWrapperFactory extends PrismContainerWrapperFactoryImpl<TriggerType> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(TriggerType.COMPLEX_TYPE, def.getTypeName());
    }

    @PostConstruct
    @Override
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return 10;
    }

    protected void addItemWrapper(ItemDefinition<?> def, PrismContainerValueWrapper<?> containerValueWrapper,
                                  WrapperContext context, List<ItemWrapper<?,?>> wrappers) throws SchemaException {
        ItemWrapperFactory<?, ?, ?> factory = getRegistry().findWrapperFactory(def);
        context.setCreateOperational(true);
        ItemWrapper<?,?> wrapper = factory.createWrapper(containerValueWrapper, def, context);
        wrapper.setReadOnly(true);
        wrappers.add(wrapper);
        context.setCreateOperational(false);
    }

    @Override
    protected boolean canCreateWrapper(ItemDefinition<?> def, ItemStatus status, WrapperContext context, boolean isEmptyValue) {
        return true;
    }

    @Override
    protected boolean shouldCreateEmptyValue(PrismContainer<TriggerType> item, WrapperContext context) {
        return false;
    }


    @Override
    public PrismContainerValueWrapper<TriggerType> createValueWrapper(PrismContainerWrapper<TriggerType> parent,
                                                              PrismContainerValue<TriggerType> value, ValueStatus status, WrapperContext context) throws SchemaException {
        context.setCreateIfEmpty(false);
        return super.createValueWrapper(parent, value, status, context);
    }

}
