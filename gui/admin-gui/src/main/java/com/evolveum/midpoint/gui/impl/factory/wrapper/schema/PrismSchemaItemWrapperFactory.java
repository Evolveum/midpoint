/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper.schema;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.wrapper.NoEmptyValueContainerWrapperFactoryImpl;

import com.evolveum.midpoint.prism.*;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.*;

import java.util.List;

/**
 * @author skublik
 */
@Component
public class PrismSchemaItemWrapperFactory
        extends NoEmptyValueContainerWrapperFactoryImpl<PrismItemDefinitionType> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (parent == null || parent.getCompileTimeClass() == null) {
            return false;
        }

        if (ComplexTypeDefinitionType.class.isAssignableFrom(parent.getCompileTimeClass())
                && (def.getItemName().equivalent(ComplexTypeDefinitionType.F_ITEM_DEFINITIONS))) {
            return true;
        }

        return false;
    }

    @Override
    protected List<? extends ItemDefinition> getItemDefinitions(PrismContainerWrapper<PrismItemDefinitionType> parent, PrismContainerValue<PrismItemDefinitionType> value) {
        if (value != null) {
            ComplexTypeDefinition def = PrismContext.get().getSchemaRegistry()
                    .findComplexTypeDefinitionByCompileTimeClass(value.getCompileTimeClass());
            if (def != null) {
                return def.getDefinitions();
            }
        }
        return super.getItemDefinitions(parent, value);
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
