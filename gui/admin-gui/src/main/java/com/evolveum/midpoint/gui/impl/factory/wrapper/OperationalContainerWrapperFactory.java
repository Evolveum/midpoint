/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.prism.Containerable;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

/**
 * @author katka
 *
 */
@Component
public class OperationalContainerWrapperFactory<T extends Containerable> extends PrismContainerWrapperFactoryImpl<T>{

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(TriggerType.COMPLEX_TYPE, def.getTypeName()); //QNameUtil.match(MetadataType.COMPLEX_TYPE, def.getTypeName()) ||
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    protected boolean canCreateWrapper(ItemDefinition<?> def, ItemStatus status, WrapperContext context, boolean isEmptyValue) {
        return true;
    }

    @Override
    protected boolean shouldCreateEmptyValue(PrismContainer<T> item, WrapperContext context) {
        return false;
    }
}
