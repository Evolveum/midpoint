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
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
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

    @Override
    public PrismObjectWrapper<ResourceType> createObjectWrapper(PrismObject<ResourceType> object, ItemStatus status, WrapperContext context) throws SchemaException {
        if (object.getOid() != null && object.asObjectable().getSuper() != null) {
            long fakeId = -1;
            addFakeNegativeIdForMergedValuesFromTemplate(object.getValue(), fakeId);
        }
        return super.createObjectWrapper(object, status, context);
    }

    /**
     * If we use merged resource from template, then resource can contain values, of multivalued container, without id.
     * GUI can process deltas from that containers, so we set fake id for that values.
    **/
    private void addFakeNegativeIdForMergedValuesFromTemplate(PrismContainerValue<?> parentValue, long fakeId) {
        for (Item<?, ?> item : parentValue.getItems()) {
            if (item.isOperational()) {
                continue;
            }
            if (!(item instanceof PrismContainer)) {
                continue;
            }
            PrismContainer<?> container  = (PrismContainer) item;
            for (PrismContainerValue<?> containerValue : container.getValues()) {
                if (!container.isSingleValue() && containerValue.getId() == null && WebPrismUtil.hasValueMetadata(containerValue)) {
                    containerValue.setId(fakeId);
                    fakeId--;
                }
                addFakeNegativeIdForMergedValuesFromTemplate(containerValue, fakeId);
            }
        }
    }
}
