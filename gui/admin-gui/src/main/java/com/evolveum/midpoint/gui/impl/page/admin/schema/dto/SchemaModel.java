/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.dto;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.SchemaPropertyWrapperImpl;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaExtensionType;

import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.page.admin.schema.component.PrismSchemaModel;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.EnumerationTypeDefinition;

public class SchemaModel<T extends DefinitionDto> implements IModel<List<T>> {

    private final IModel<PrismObjectWrapper<SchemaExtensionType>> schemaType;

//    private final PrismSchemaModel schemaExtensionType;

//    public SchemaModel(IModel<SchemaDefinitionType> schemaType) {
//        this.schemaType = schemaType;
//    }

    public SchemaModel(IModel<PrismObjectWrapper<SchemaExtensionType>> wrapperModel) {
        this.schemaType = wrapperModel;
    }

//    private String namespace;
//    private List<T> cachedDtos;

    @Override
    public List<T> getObject() {

//        SchemaDefinitionType def = schemaExtensionType.getObject().getDefinition();
//        Element schemaElement = def.getSchema();
//
//        if (cachedDtos != null) {
//            schemaDefinitionType.setSchema(schemaNew.serializeToXsd().getDocumentElement());
//            PrismSchemaImpl schemaNew = new PrismSchemaImpl(namespace);
//            cachedDtos.forEach(cahcedDto -> schemaNew.add(cahcedDto.getOriginalDefinition()));

//            SchemaDefinitionType schemaDefinitionType = new SchemaDefinitionType();
//            try {
//                schemaDefinitionType.setSchema(DOMUtil.getFirstChildElement(schemaNew.serializeToXsd()));
//            } catch (SchemaException e) {
//                throw new RuntimeException(e);
//            }


//            SchemaExtensionType extType = schemaExtensionType.getObject();
//            extType.setDefinition(schemaDefinitionType);
//            schemaExtensionType.setObject(extType);
////
//            return cachedDtos;
//        }

//        SchemaDefinitionType def = schemaType.getObject();
//        Element schemaElement = def.getSchema();

//        QName extensionForType = schemaType.getObject();
//        QName extensionForType = UserType.COMPLEX_TYPE;
        try {
//            PrismSchema prismSchema = PrismSchemaImpl.parse(schemaElement, true, "schema for " + extensionForType, PrismContext.get());
//            this.namespace = prismSchema.getNamespace();
            PrismPropertyWrapper<SchemaDefinitionType> property = schemaType.getObject().findProperty(SchemaExtensionType.F_DEFINITION);
            PrismPropertyValueWrapper<SchemaDefinitionType> valueWrapper = property.getValue();
            SchemaPropertyWrapperImpl schemaPropertyWrapper = (SchemaPropertyWrapperImpl) valueWrapper;
            Collection<Definition> definitions = schemaPropertyWrapper.getParsedSchema().getDefinitions();

            List<T> dtos = definitions.stream()
                    .map(this::createDeinitionDto)
                    .toList();
//            this.cachedDtos = dtos;
//            return cachedDtos;
            return dtos;
        } catch (Exception e) {

        }
            return null;
    }

//    @Override
//    public void setObject(List<T> object) {
//
//        Element schemaElement = schemaExtensionType.getObject().getDefinition().getSchema();
//        PrismSchema prismSchema;
//        try {
//             prismSchema = PrismSchemaImpl.parse(schemaElement, true, "schema for ", PrismContext.get());
//        } catch (SchemaException e) {
//            throw new RuntimeException(e);
//        }
//
//
//        PrismSchemaImpl schemaNew = new PrismSchemaImpl(prismSchema.getNamespace());
////        object.forEach(def -> schemaNew.add(def.getEditedDefinition()));
//
//
//        SchemaDefinitionType schemaDefinitionType = new SchemaDefinitionType();
//        try {
//            schemaDefinitionType.setSchema(schemaNew.serializeToXsd().getDocumentElement());
//        } catch (SchemaException e) {
//            throw new RuntimeException(e);
//        }
////        schemaExtensionType.setObject(schemaDefinitionType);
//
//
//        IModel.super.setObject(object);
//    }

    @Override
    public void detach() {
        IModel.super.detach();
    }

        private T createDeinitionDto(Definition def) {
            if (def instanceof ComplexTypeDefinition) {
                return (T) new ComplexTypeDefinitionDto((ComplexTypeDefinition) def);
            } if (def instanceof EnumerationTypeDefinition) {
                return (T) new EnumerationTypeDefinitionDto((EnumerationTypeDefinition) def);
            } else {
                throw new UnsupportedOperationException("Unsupported definition type: " + def.getClass());
//            return (T) new ItemDefinitionDto(def);
            }
        }

    }
