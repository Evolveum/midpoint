/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.dto;

import com.evolveum.midpoint.prism.ItemDefinition;

import org.apache.wicket.model.IModel;

import java.util.List;
import java.util.stream.Collectors;

public class ItemDefinitionModel<ID extends ItemDefinitionDto> implements IModel<ID> {

    private IModel<ComplexTypeDefinitionDto> complexTypeDefinitionModel;

    public ItemDefinitionModel(IModel<ComplexTypeDefinitionDto> complexTypeDefinitionModel) {
        this.complexTypeDefinitionModel = complexTypeDefinitionModel;
    }

    @Override
    public ID getObject() {
        return null;
    }

//    @Override
//    public List<ID> getObject() {
//        if (definitionDtos != null) {
//            return definitionDtos;
//        }
//        definitionDtos = complexTypeDefinitionModel.getObject().getOriginalDefinition().getDefinitions().stream()
//                .map(this::createDefinitionDto)
//                .collect(Collectors.toList());
//
//        return definitionDtos;
//    }
//
//    private ID createDefinitionDto(ItemDefinition<?> def) {
////        if (def instanceof ComplexTypeDefinition) {
////            return new ComplexTypeDefinitionDto((ComplexTypeDefinition) def);
////        } if (def instanceof EnumerationTypeDefinition) {
////            return new EnumerationTypeDefinitionDto((EnumerationTypeDefinition) def);
////        }
////        else
//        return (ID) new ItemDefinitionDto(def.toMutable());
//    }
//
//    @Override
//    public void setObject(List<ID> object) {
//        IModel.super.setObject(object);
//    }
}
