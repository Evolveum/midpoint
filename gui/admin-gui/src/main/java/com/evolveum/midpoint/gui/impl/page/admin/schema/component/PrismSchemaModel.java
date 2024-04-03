/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.prism.impl.schema.RawPrismSchemaImpl;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaExtensionType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.w3c.dom.Element;

public class PrismSchemaModel implements IModel<PrismSchema> {


    private PrismSchema parsedSchema;

    private IModel<PrismObjectWrapper<SchemaExtensionType>> modelObject;

    public PrismSchemaModel(IModel<PrismObjectWrapper<SchemaExtensionType>> modelObject) {
//        super(modelObject, "value.realValue");
        this.modelObject = modelObject;
    }

    @Override
    public PrismSchema getObject() {
//        SchemaExtensionType ex = super.getObject();
        if (parsedSchema != null) {
            return parsedSchema;
        }
        SchemaExtensionType ex = null; //modelObject.getObject();

        Element schemaElement = ex.getDefinition().getSchema();
//        PrismSchema prismSchema;
        try {
            parsedSchema = RawPrismSchemaImpl.parse(schemaElement, true, "schema for ", PrismContext.get());
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
//        String namespace = prismSchema.getNamespace();
        return parsedSchema;
    }

//    @Override
//    public void setObject(SchemaExtensionType object) {
//        super.setObject(object);
//    }
}
