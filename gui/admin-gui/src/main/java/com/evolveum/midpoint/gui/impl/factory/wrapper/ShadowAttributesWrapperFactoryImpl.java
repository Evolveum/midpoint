/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributesContainerDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

@Component
public class ShadowAttributesWrapperFactoryImpl<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof ShadowAttributesContainerDefinition && ShadowAttributesType.class.isAssignableFrom(((ShadowAttributesContainerDefinition) def).getCompileTimeClass());
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE - 100;
    }


    @Override
    public void registerWrapperPanel(PrismContainerWrapper<C> wrapper) {
        getRegistry().registerWrapperPanel(ShadowAttributesType.COMPLEX_TYPE, PrismContainerPanel.class);

    }
}
