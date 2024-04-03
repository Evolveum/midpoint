/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.PrismSchemaModel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.DefinitionDto;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.SchemaModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.schema.RawPrismSchemaImpl;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaExtensionType;

import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.apache.wicket.model.LoadableDetachableModel;
import org.w3c.dom.Document;

import java.util.Collection;

public class SchemaDetailsModel extends ObjectDetailsModels<SchemaExtensionType> {

    private SchemaModel<DefinitionDto> schemaModel;
//    private PrismSchemaModel prismSchemaModel;

    public SchemaDetailsModel(LoadableDetachableModel<PrismObject<SchemaExtensionType>> prismObjectModel, ModelServiceLocator serviceLocator) {
        super(prismObjectModel, serviceLocator);

        this.schemaModel = new SchemaModel<>(getObjectWrapperModel());
    }

    public <T extends DefinitionDto> SchemaModel<T> getSchemaModel() {
        return (SchemaModel<T>) schemaModel;
    }

//    @Override
//    public Collection<ObjectDelta<? extends ObjectType>> collectDeltas(OperationResult result) throws CommonException {
//
//        Document document = prismSchemaModel.getObject().serializeToXsd();
//        SchemaDefinitionType newDef = (SchemaDefinitionType) getObjectWrapper().findProperty(SchemaExtensionType.F_DEFINITION).getValue().getRealValue();
//        newDef.setSchema(DOMUtil.getFirstChildElement(document));
////        setRealValue(DOMUtil.getFirstChildElement(document));
//        return super.collectDeltas(result);
//    }
}
