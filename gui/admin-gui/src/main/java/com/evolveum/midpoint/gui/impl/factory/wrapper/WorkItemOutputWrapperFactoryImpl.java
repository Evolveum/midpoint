/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by honchar
 */
@Component
public class WorkItemOutputWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<AbstractWorkItemOutputType> {

    @Autowired
    private GuiComponentRegistry registry;

    @Override
    public boolean match(ItemDefinition<?> def) {
        return AbstractWorkItemOutputType.COMPLEX_TYPE.equals(def.getTypeName());
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
    protected PrismContainerValue<AbstractWorkItemOutputType> createNewValue(PrismContainer<AbstractWorkItemOutputType> item) {
        throw new UnsupportedOperationException("New work item output value should not be created while creating wrappers.");
    }


    @Override
    protected boolean shouldCreateEmptyValue(PrismContainer<AbstractWorkItemOutputType> item, WrapperContext context) {
        return false;
    }

    @Override
    public PrismContainerValueWrapper<AbstractWorkItemOutputType> createValueWrapper(PrismContainerWrapper<AbstractWorkItemOutputType> parent,
                   PrismContainerValue<AbstractWorkItemOutputType> value, ValueStatus status, WrapperContext context) throws SchemaException {
        context.setCreateIfEmpty(false);
        return super.createValueWrapper(parent, value, status, context);
    }


}
