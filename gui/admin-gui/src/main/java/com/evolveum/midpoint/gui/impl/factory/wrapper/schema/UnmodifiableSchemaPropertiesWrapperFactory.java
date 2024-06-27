/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper.schema;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
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
public class UnmodifiableSchemaPropertiesWrapperFactory<T>
        extends PrismPropertyWrapperFactoryImpl<T> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (!super.match(def, parent)) {
            return false;
        }

        if (parent == null || parent.getCompileTimeClass() == null) {
            return false;
        }

        if (DefinitionType.class.isAssignableFrom(parent.getCompileTimeClass())
                && (def.getItemName().equivalent(DefinitionType.F_NAME))) {
            return true;
        }

        if (PrismSchemaType.class.isAssignableFrom(parent.getCompileTimeClass())
                && def.getItemName().equivalent(PrismSchemaType.F_NAMESPACE)) {
            return true;
        }

        if (EnumerationTypeDefinitionType.class.isAssignableFrom(parent.getCompileTimeClass())
                && def.getItemName().equivalent(EnumerationTypeDefinitionType.F_BASE_TYPE)) {
            return true;
        }

        if (EnumerationValueTypeDefinitionType.class.isAssignableFrom(parent.getCompileTimeClass())
                && (def.getItemName().equivalent(EnumerationValueTypeDefinitionType.F_VALUE)
                || def.getItemName().equivalent(EnumerationValueTypeDefinitionType.F_CONSTANT_NAME))) {
            return true;
        }

        if (PrismItemDefinitionType.class.isAssignableFrom(parent.getCompileTimeClass())
                && def.getItemName().equivalent(PrismItemDefinitionType.F_TYPE)) {
            return true;
        }

        if (ComplexTypeDefinitionType.class.isAssignableFrom(parent.getCompileTimeClass())
                && def.getItemName().equivalent(ComplexTypeDefinitionType.F_EXTENSION)) {
            return true;
        }

        if (parent.getRealValue() instanceof PrismItemDefinitionType itemDef) {
            if (def.getItemName().equivalent(PrismItemDefinitionType.F_REQUIRED)) {
                return !Boolean.TRUE.equals(itemDef.getRequired());
            }
        }

        if (parent.getRealValue() instanceof PrismItemDefinitionType itemDef) {
            if (def.getItemName().equivalent(PrismItemDefinitionType.F_MULTIVALUE)) {
                return Boolean.TRUE.equals(itemDef.getMultivalue());
            }
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

        if (!ValueStatus.ADDED.equals(itemWrapper.getParent().getStatus())
                && !ItemStatus.ADDED.equals(itemWrapper.getStatus())) {
            return true;
        }

        return false;
    }

}
