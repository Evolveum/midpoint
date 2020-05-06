/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by Kate Honchar
 */
@Component
public class ApprovalSchemaWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<ApprovalSchemaType> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return ApprovalSchemaType.COMPLEX_TYPE .equals(def.getTypeName());
    }

    @Override
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return 1000;
    }

    @Override
    protected PrismContainerValue<ApprovalSchemaType> createNewValue(PrismContainer<ApprovalSchemaType> item) {
        throw new UnsupportedOperationException("New approval schema value should not be created while creating wrappers.");
    }


    @Override
    protected boolean shouldCreateEmptyValue(PrismContainer<ApprovalSchemaType> item, WrapperContext context) {
        return false;
    }

}
