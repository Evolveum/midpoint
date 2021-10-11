/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
 * @author katka
 *
 */
@Component
public class MetadataWrapperFactory extends PrismContainerWrapperFactoryImpl<MetadataType>{


    @Autowired private GuiComponentRegistry registry;

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(MetadataType.COMPLEX_TYPE, def.getTypeName());
    }

    @PostConstruct
    @Override
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return 10;
    }

    protected void addItemWrapper(ItemDefinition<?> def, PrismContainerValueWrapper<?> containerValueWrapper,
            WrapperContext context, List<ItemWrapper<?,?,?,?>> wrappers) throws SchemaException {

        ItemWrapperFactory<?, ?, ?> factory = registry.findWrapperFactory(def);

        context.setCreateOperational(true);
        ItemWrapper<?,?,?,?> wrapper = factory.createWrapper(containerValueWrapper, def, context);
        wrapper.setReadOnly(true);
        wrappers.add(wrapper);
        context.setCreateOperational(false);
    }

    @Override
    protected boolean canCreateWrapper(ItemDefinition<?> def, ItemStatus status, WrapperContext context, boolean isEmptyValue) {
        return true;
    }
}
