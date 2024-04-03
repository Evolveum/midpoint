/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.dto;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.SchemaPropertyWrapperImpl;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaExtensionType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

public class PrismSchemaModel implements IModel<SchemaPropertyWrapperImpl> {

    private final IModel<PrismObjectWrapper<SchemaExtensionType>> schemaType;

    public PrismSchemaModel(IModel<PrismObjectWrapper<SchemaExtensionType>> schemaType) {
        this.schemaType = schemaType;
    }

    @Override
    public SchemaPropertyWrapperImpl getObject() {
        PrismPropertyWrapper<SchemaDefinitionType> property;
        try {
            property = schemaType.getObject().findProperty(SchemaExtensionType.F_DEFINITION);
            PrismPropertyValueWrapper<SchemaDefinitionType> valueWrapper = property.getValue();
            return (SchemaPropertyWrapperImpl) valueWrapper;
        } catch (SchemaException e) {
            //TODO error handling
            throw new RuntimeException(e);
        }

    }

}
