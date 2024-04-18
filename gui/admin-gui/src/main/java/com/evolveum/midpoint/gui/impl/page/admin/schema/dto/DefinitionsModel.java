/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.dto;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.schema.PrismSchema;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.prism.wrapper.SchemaPropertyWrapperImpl;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.EnumerationTypeDefinition;

public class DefinitionsModel<T extends DefinitionDto> implements IModel<List<T>> {

    private final IModel<SchemaPropertyWrapperImpl> schemaType;

    public DefinitionsModel(IModel<SchemaPropertyWrapperImpl> wrapperModel) {
        this.schemaType = wrapperModel;
    }

    @Override
    public List<T> getObject() {
        try {

            PrismSchema prismSchema = schemaType.getObject().getParsedSchema();
            if (prismSchema == null) {
                return null;
            }
            Collection<Definition> definitions = prismSchema.getDefinitions();

            return definitions.stream()
                    .map(this::createDefinitionDto)
                    .toList();
        } catch (Exception e) {
            //TODO error handling
        }
            return null;
    }

    @Override
    public void detach() {
        IModel.super.detach();
    }

        private T createDefinitionDto(Definition def) {
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
