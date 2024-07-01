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
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.*;

import org.springframework.stereotype.Component;

/**
 * @author skublik
 */
@Component
public class UnmodifiableSchemaItemIndexWrapperFactory<T>
        extends PrismPropertyWrapperFactoryImpl<T> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (!super.match(def, parent)) {
            return false;
        }

        if (parent == null || parent.getCompileTimeClass() == null) {
            return false;
        }

        if (parent.getRealValue() instanceof PrismContainerDefinitionType) {
            return def.getItemName().equivalent(PrismItemDefinitionType.F_INDEXED);
        }

        return false;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    protected boolean determineReadOnly(PrismPropertyWrapper<T> itemWrapper, WrapperContext context) {
        return true;
    }

}
