/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ResourceWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author skublik
 */
@Component
public class ResourceWrapperFactoryImpl extends PrismObjectWrapperFactoryImpl<ResourceType> {

    @Override
    public PrismObjectWrapper<ResourceType> createObjectWrapper(PrismObject<ResourceType> object, ItemStatus status) {
        return new ResourceWrapper(object, status);
    }

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismObjectDefinition && QNameUtil.match(def.getTypeName(), ResourceType.COMPLEX_TYPE);
    }

    @Override
    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return 99;
    }

    @Override
    protected void addItemWrapper(ItemDefinition<?> def, PrismContainerValueWrapper<?> containerValueWrapper, WrapperContext context, List<ItemWrapper<?, ?>> wrappers) throws SchemaException {
        if (ResourceType.F_DESCRIPTION.equivalent(def.getItemName())) {
            def.toMutable().setEmphasized(true);
        }
        super.addItemWrapper(def, containerValueWrapper, context, wrappers);
    }
}
