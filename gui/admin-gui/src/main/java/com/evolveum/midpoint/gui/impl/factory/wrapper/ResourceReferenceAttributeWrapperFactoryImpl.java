/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.stereotype.Component;

/**
 * @author skublik
 */
@Component
public class ResourceReferenceAttributeWrapperFactoryImpl<R extends Referencable> extends PrismReferenceWrapperFactory<R> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceReferenceAttributeWrapperFactoryImpl.class);

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof ShadowReferenceAttributeDefinition;
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    protected PrismReferenceWrapper<R> createWrapperInternal(PrismContainerValueWrapper<?> parent, PrismReference item, ItemStatus status, WrapperContext ctx) {
        PrismReferenceWrapper<R> wrapper = super.createWrapperInternal(parent, item, status, ctx);
        ShadowReferenceAttributeDefinition def = (ShadowReferenceAttributeDefinition) item.getDefinition();
        wrapper.setFilter(def.createTargetObjectsFilter());
        return wrapper;
    }
}
