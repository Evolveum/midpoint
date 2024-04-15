/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema;

import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.PrismSchemaModel;

import com.evolveum.midpoint.gui.impl.prism.wrapper.SchemaPropertyWrapperImpl;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.DefinitionDto;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.DefinitionsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaExtensionType;

import org.apache.wicket.model.PropertyModel;

public class SchemaDetailsModel extends ObjectDetailsModels<SchemaExtensionType> {

    private final PrismSchemaModel prismSchemaModel;
    private DefinitionsModel<DefinitionDto> schemaModel;

    public SchemaDetailsModel(LoadableDetachableModel<PrismObject<SchemaExtensionType>> prismObjectModel, ModelServiceLocator serviceLocator) {
        super(prismObjectModel, serviceLocator);

        this.prismSchemaModel = new PrismSchemaModel(getObjectWrapperModel());
        this.schemaModel = new DefinitionsModel<>(prismSchemaModel);
    }

    public <T extends DefinitionDto> DefinitionsModel<T> getSchemaModel() {
        return (DefinitionsModel<T>) schemaModel;
    }

    public IModel<String> getNamespaceModel() {
        return new PropertyModel<>(prismSchemaModel, SchemaPropertyWrapperImpl.F_NAMESPACE);
    }

}
