/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper.schema;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.factory.wrapper.AssignmentHolderWrapperFactoryImpl;
import com.evolveum.midpoint.gui.impl.factory.wrapper.PrismObjectWrapperFactoryImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyWrapperImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;

import org.springframework.stereotype.Component;

@Component
public class SchemaWrapperFactory extends AssignmentHolderWrapperFactoryImpl {

    @Override
    public boolean match(ItemDefinition<?> def) {
        if (!super.match(def) || def == null) {
            return false;
        }
        return QNameUtil.match(def.getTypeName(), SchemaType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return 97;
    }

    @Override
    public PrismContainerValueWrapper<AssignmentHolderType> createValueWrapper(PrismContainerWrapper<AssignmentHolderType> parent, PrismContainerValue<AssignmentHolderType> value, ValueStatus status, WrapperContext context) throws SchemaException {
        PrismContainerValueWrapper<AssignmentHolderType> objectValueWrapper = super.createValueWrapper(parent, value, status, context);

        PrismPropertyWrapper<Object> namespace = objectValueWrapper.findProperty(
                ItemPath.create(WebPrismUtil.PRISM_SCHEMA, PrismSchemaType.F_NAMESPACE));
        if (namespace != null) {
            objectValueWrapper.addItem(namespace);
            ((PrismPropertyWrapperImpl)namespace).setMuteDeltaCreate(true);
        }

        PrismPropertyWrapper<Object> defaultPrefix = objectValueWrapper.findProperty(
                ItemPath.create(WebPrismUtil.PRISM_SCHEMA, PrismSchemaType.F_DEFAULT_PREFIX));
        if (defaultPrefix != null) {
            objectValueWrapper.addItem(defaultPrefix);
            ((PrismPropertyWrapperImpl)defaultPrefix).setMuteDeltaCreate(true);
        }


        return objectValueWrapper;
    }
}
