/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.component.password.ProtectedStringPropertyPanel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ProtectedStringTypeWrapperImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by honchar
 */
@Component
public class ProtectedStringWrapperFactory extends PrismPropertyWrapperFactoryImpl<ProtectedStringType>{

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(ProtectedStringType.COMPLEX_TYPE, def.getTypeName()) ;
    }

    @PostConstruct
    @Override
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return 1010;
    }

    @Override
    protected PrismPropertyWrapper<ProtectedStringType> createWrapperInternal(PrismContainerValueWrapper<?> parent, PrismProperty<ProtectedStringType> item,
                                                                      ItemStatus status, WrapperContext ctx) {
        return new ProtectedStringTypeWrapperImpl(parent, item, status);
    }

    @Override
    public void registerWrapperPanel(PrismPropertyWrapper<ProtectedStringType> wrapper) {
        getRegistry().registerWrapperPanel(wrapper.getTypeName(), ProtectedStringPropertyPanel.class);
    }
}
