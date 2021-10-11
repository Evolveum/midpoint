/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.LayerRefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.ResourceAttributeDefinitionPanel;
import com.evolveum.midpoint.gui.impl.prism.ResourceAttributeWrapper;
import com.evolveum.midpoint.gui.impl.prism.ResourceAttributeWrapperImpl;
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

    @Autowired private GuiComponentRegistry registry;

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
            PrismPropertyValue<T> value, ValueStatus status, WrapperContext context) throws SchemaException {
        PrismPropertyValueWrapper<T> valueWrapper = new PrismPropertyValueWrapper<>(parent, value, status);
        return valueWrapper;
    }

    @PostConstruct
    @Override
    public void register() {
        registry.addToRegistry(this);
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
    protected ResourceAttributeWrapper<T> createWrapper(PrismContainerValueWrapper<?> parent,
            ResourceAttribute<T> childContainer, ItemStatus status, WrapperContext ctx) {
        registry.registerWrapperPanel(new QName("ResourceAttributeDefinition"), ResourceAttributeDefinitionPanel.class);
        ResourceAttributeWrapper<T> propertyWrapper = new ResourceAttributeWrapperImpl<>(parent, childContainer, status);
        return propertyWrapper;
    }
}
