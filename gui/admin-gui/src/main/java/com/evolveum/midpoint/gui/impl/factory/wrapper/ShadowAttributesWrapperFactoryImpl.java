/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerWrapperImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import org.springframework.stereotype.Component;

@Component
public class ShadowAttributesWrapperFactoryImpl<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof ResourceAttributeContainerDefinition && ShadowAttributesType.class.isAssignableFrom(((ResourceAttributeContainerDefinition) def).getCompileTimeClass());
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE - 100;
    }

    @Override
    protected PrismContainerWrapper<C> createWrapper(PrismContainerValueWrapper<?> parent,
            PrismContainer<C> childContainer, ItemStatus status, WrapperContext ctx) {
        getRegistry().registerWrapperPanel(ShadowAttributesType.COMPLEX_TYPE, PrismContainerPanel.class);
        return new PrismContainerWrapperImpl<>(parent, childContainer, status);
    }
}
