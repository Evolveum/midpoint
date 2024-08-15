/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.AuthenticationAttemptValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismPropertyValue;

import com.evolveum.midpoint.web.component.prism.ValueStatus;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.AuthenticationAttemptWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationAttemptDataType;

@Component
public class AuthenticationAttemptWrapperFactory extends PrismPropertyWrapperFactoryImpl<AuthenticationAttemptDataType>{

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(AuthenticationAttemptDataType.COMPLEX_TYPE, def.getTypeName()) ;
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 10;
    }

    @Override
    protected AuthenticationAttemptWrapper createWrapperInternal(PrismContainerValueWrapper<?> parent, PrismProperty<AuthenticationAttemptDataType> item,
                                                                      ItemStatus status, WrapperContext ctx) {
        return new AuthenticationAttemptWrapper(parent, item, status);
    }

    @Override
    public PrismPropertyValueWrapper<AuthenticationAttemptDataType> createValueWrapper(PrismPropertyWrapper<AuthenticationAttemptDataType> parent, PrismPropertyValue<AuthenticationAttemptDataType> value, ValueStatus status, WrapperContext context) {
        return new AuthenticationAttemptValueWrapper(parent, value, status);
    }
}
