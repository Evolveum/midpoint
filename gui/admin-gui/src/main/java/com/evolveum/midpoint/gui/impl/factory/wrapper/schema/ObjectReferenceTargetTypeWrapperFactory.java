/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper.schema;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.factory.wrapper.PrismPropertyWrapperFactoryImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.*;

import org.springframework.stereotype.Component;

/**
 * @author skublik
 */
@Component
public class ObjectReferenceTargetTypeWrapperFactory<T>
        extends PrismPropertyWrapperFactoryImpl<T> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (parent == null || parent.getCompileTimeClass() == null) {
            return false;
        }

        if (PrismReferenceDefinitionType.class.isAssignableFrom(parent.getCompileTimeClass())
                && def.getItemName().equivalent(PrismReferenceDefinitionType.F_OBJECT_REFERENCE_TARGET_TYPE)) {
            return true;
        }

        return false;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    protected boolean determineReadOnly(PrismPropertyWrapper<T> itemWrapper, WrapperContext context) {
        if (super.determineReadOnly(itemWrapper, context)) {
            return true;
        }

        if (!ValueStatus.ADDED.equals(itemWrapper.getParent().getStatus())) {
            return true;
        }

        return false;
    }

}
