/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPredefinedActivationMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CollectionRefSpecificationType;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaType;

/**
 * @author katka
 *
 */
@Component
public class NoEmptyValueContainerWrapperFactoryImpl<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismContainerDefinition && def.isMultiValue()
                || AbstractWorkItemOutputType.COMPLEX_TYPE.equals(def.getTypeName())
                || ApprovalSchemaType.COMPLEX_TYPE.equals(def.getTypeName())
                || (def.getTypeClass() != null
                    && AbstractPredefinedActivationMappingType.class.isAssignableFrom(def.getTypeClass()));
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
    protected PrismContainerValue<C> createNewValue(PrismContainer<C> item) {
        throw new UnsupportedOperationException("New value for multi-value container should not be created while creating wrappers.");
    }

    @Override
    protected boolean shouldCreateEmptyValue(PrismContainer<C> item, WrapperContext context) {
        return false;
    }

}
