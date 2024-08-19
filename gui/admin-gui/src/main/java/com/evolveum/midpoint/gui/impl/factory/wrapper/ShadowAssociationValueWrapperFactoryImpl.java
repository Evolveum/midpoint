/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ShadowAssociationValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributesContainerDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import org.springframework.stereotype.Component;

@Component
public class ShadowAssociationValueWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<ShadowAssociationValueType> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof ShadowAssociationDefinition associationDef
                && ShadowAssociationValueType.class.isAssignableFrom(associationDef.getCompileTimeClass());
    }

    @Override
    public int getOrder() {
        return 900;
    }

    @Override
    protected PrismContainerWrapper<ShadowAssociationValueType> createWrapper(
            PrismContainerValueWrapper<?> parent,
            PrismContainer<ShadowAssociationValueType> childContainer,
            ItemStatus status) {
        return new ShadowAssociationValueWrapper(parent, childContainer, status);
    }

    @Override
    protected boolean shouldCreateEmptyValue(PrismContainer<ShadowAssociationValueType> item, WrapperContext context) {
        return false;
    }
}
