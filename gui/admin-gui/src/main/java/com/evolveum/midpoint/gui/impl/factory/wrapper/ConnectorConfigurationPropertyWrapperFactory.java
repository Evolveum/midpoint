/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyWrapperImpl;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;

/**
 * @author lskublik
 */
@Component
public class ConnectorConfigurationPropertyWrapperFactory<T> extends PrismPropertyWrapperFactoryImpl<T> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (def instanceof PrismPropertyDefinition
                && parent != null
                && parent.getDefinition().getItemName().equivalent(ItemPath.create("configurationProperties"))) {
            return true;
        }
        return false;
    }

    @PostConstruct
    @Override
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE - 1;
    }

    @Override
    protected PrismPropertyWrapper<T> createWrapperInternal(PrismContainerValueWrapper<?> parent, PrismProperty<T> item,
            ItemStatus status, WrapperContext wrapperContext) {
        if (wrapperContext.getConnectorConfigurationSuggestions() != null) {
            wrapperContext.getConnectorConfigurationSuggestions().getDiscoveredProperties().forEach(suggestion -> {
                PrismPropertyDefinition<?> suggestionDef = suggestion.getDefinition();
                if (suggestionDef.getItemName().equals(item.getDefinition().getItemName())) {
                    if (suggestionDef.getAllowedValues() != null && !suggestionDef.getAllowedValues().isEmpty()) {
                        item.getDefinition().toMutable().setAllowedValues((Collection<? extends DisplayableValue<T>>) suggestionDef.getAllowedValues());
                    }
                    if (suggestionDef.getSuggestedValues() != null && !suggestionDef.getSuggestedValues().isEmpty()) {
                        item.getDefinition().toMutable().setSuggestedValues((Collection<? extends DisplayableValue<T>>) suggestionDef.getSuggestedValues());
                    }
                }
            });
        }
        PrismPropertyWrapper<T> propertyWrapper = new PrismPropertyWrapperImpl<>(parent, item, status);
        propertyWrapper.setPredefinedValuesOid(getPredefinedValuesOid(item));
        return propertyWrapper;
    }
}
