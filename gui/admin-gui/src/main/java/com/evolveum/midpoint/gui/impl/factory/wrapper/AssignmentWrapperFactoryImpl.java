/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * @author katka
 *
 */
@Component
public class AssignmentWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<AssignmentType> {

    @Autowired private GuiComponentRegistry registry;

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismContainerDefinition && def.isMultiValue();
    }

    @Override
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return 1000;
    }

    @Override
    protected PrismContainerValue<AssignmentType> createNewValue(PrismContainer<AssignmentType> item) {
        throw new UnsupportedOperationException("New assignment value should not be created while creating wrappers.");
    }


    @Override
    protected boolean shouldCreateEmptyValue(PrismContainer<AssignmentType> item, WrapperContext context) {
        return false;
    }

}
