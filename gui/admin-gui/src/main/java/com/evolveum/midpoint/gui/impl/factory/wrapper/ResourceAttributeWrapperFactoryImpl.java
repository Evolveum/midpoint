/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ResourceAttributeWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ResourceAttributeDefinitionPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ResourceAttributeWrapperImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author skublik
 *
 */
@Component
public class ResourceAttributeWrapperFactoryImpl<T> extends ItemWrapperFactoryImpl<ResourceAttributeWrapper<T>, PrismPropertyValue<T>, ResourceAttribute<T>, PrismPropertyValueWrapper<T>> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof ResourceAttributeDefinition;
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE-2;
    }

    @Override
    public PrismPropertyValueWrapper<T> createValueWrapper(ResourceAttributeWrapper<T> parent,
            PrismPropertyValue<T> value, ValueStatus status, WrapperContext context) {
        return new PrismPropertyValueWrapper<>(parent, value, status);
    }

    @PostConstruct
    @Override
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected void setupWrapper(ResourceAttributeWrapper<T> wrapper) {

    }

    @Override
    protected PrismPropertyValue<T> createNewValue(ResourceAttribute<T> item) throws SchemaException {
        PrismPropertyValue<T> newValue = getPrismContext().itemFactory().createPropertyValue();
        item.add(newValue);
        return newValue;
    }

    @Override
    protected ResourceAttributeWrapper<T> createWrapperInternal(PrismContainerValueWrapper<?> parent,
            ResourceAttribute<T> childContainer, ItemStatus status, WrapperContext ctx) {
        return new ResourceAttributeWrapperImpl<>(parent, childContainer, status);
    }

    @Override
    public void registerWrapperPanel(ResourceAttributeWrapper<T> wrapper) {
        getRegistry().registerWrapperPanel(new QName("ResourceAttributeDefinition"), ResourceAttributeDefinitionPanel.class);
    }

    @Override
    protected boolean canCreateWrapper(ItemDefinition<?> def, ItemStatus status, WrapperContext context, boolean isEmptyValue) {
        if (!super.canCreateWrapper(def, status, context, isEmptyValue)) {
            return false;
        }

        return isEmptyValue ? context.isCreateIfEmpty() : true;
    }

}
