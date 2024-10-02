/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class MappingNameWrapperFactory extends PrismPropertyWrapperFactoryImpl<String> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (!super.match(def, parent)) {
            return false;
        }

        if (!def.getItemName().equivalent(MappingType.F_NAME)) {
            return false;
        }

        if (parent == null || !MappingType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            return false;
        }

        return true;
    }

    @Override
    public PrismPropertyWrapper<String> createWrapper(PrismContainerValueWrapper<?> parent, Item childItem, ItemStatus status, WrapperContext context) throws SchemaException {
        PrismPropertyWrapper<String> wrapper = super.createWrapper(parent, childItem, status, context);
        if (parent.getStatus() == ValueStatus.NOT_CHANGED
                && wrapper.getStatus() == ItemStatus.NOT_CHANGED
                && childItem.getDefinition().isSingleValue()
                && !childItem.getValues().isEmpty()
                && childItem.getValue() != null
                && StringUtils.isNotEmpty(childItem.getValue().getRealValue())) {
            wrapper.setReadOnly(true);
        }
        return wrapper;
    }

    @Override
    public int getOrder() {
        return 1000;
    }
}
