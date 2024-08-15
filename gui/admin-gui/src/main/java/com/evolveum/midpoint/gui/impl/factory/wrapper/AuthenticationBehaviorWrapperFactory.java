/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.AuthenticationBehaviorWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationBehavioralDataType;

@Component
public class AuthenticationBehaviorWrapperFactory extends OperationalContainerWrapperFactory<AuthenticationBehavioralDataType> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(AuthenticationBehavioralDataType.COMPLEX_TYPE, def.getTypeName());
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 1;
    }

    @Override
    public PrismContainerWrapper<AuthenticationBehavioralDataType> createWrapperInternal(PrismContainerValueWrapper<?> parent,
            PrismContainer<AuthenticationBehavioralDataType> childContainer, ItemStatus status, WrapperContext context) {
        return new AuthenticationBehaviorWrapper(parent, childContainer, status);
    }

    @Override
    protected ItemWrapper<?, ?> createChildWrapper(ItemDefinition<?> def, PrismContainerValueWrapper<?> containerValueWrapper, WrapperContext context) throws SchemaException {
        //TODO nasty hack, fixme. we need to create children for <authentication> container which are all operational
        boolean isCreateOperatonal = context.isCreateOperational();
        context.setCreateOperational(true);
        ItemWrapper<?, ?> child = super.createChildWrapper(def, containerValueWrapper, context);
        context.setCreateOperational(isCreateOperatonal);
        return child;
    }
}
