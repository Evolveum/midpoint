/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.*;

public class ComplexTypeDefinitionDto extends DefinitionDto<MutableComplexTypeDefinition> implements Serializable {

    public static final String F_DEFINITIONS = "definitionDtos";

//    private List<? extends ItemDefinitionDto> definitionDtos;

    public ComplexTypeDefinitionDto(ComplexTypeDefinition ctd) {
        super(ctd.toMutable());

//        definitionDtos = ctd.getDefinitions().stream()
//                .map(this::createDefinitionDto)
//                .collect(Collectors.toList());

    }

//    private ItemDefinitionDto createDefinitionDto(ItemDefinition<?> def) {
////        if (def instanceof ComplexTypeDefinition) {
////            return new ComplexTypeDefinitionDto((ComplexTypeDefinition) def);
////        } if (def instanceof EnumerationTypeDefinition) {
////            return new EnumerationTypeDefinitionDto((EnumerationTypeDefinition) def);
////        }
////        else
//            return new ItemDefinitionDto(def.toMutable());
//    }

//    public List<DefinitionDto> getDefinitionDtos() {
//        return definitionDtos;
//    }

//    @Override
//    public ComplexTypeDefinition getEditedDefinition() {
//        ComplexTypeDefinition originalDefinition = getOriginalDefinition();
//        MutableComplexTypeDefinition mutableDefinition = originalDefinition.clone().toMutable();
//        mutableDefinition.setDisplayName(getDisplayName());
//        mutableDefinition.setDisplayOrder(getDisplayOrder());
////        mutableDefinition.getDefinitions().clear();
////        for (ItemDefinitionDto dto : definitionDtos) {
////            ItemDefinition<?> itemDEf = mutableDefinition.findItemDefinition(dto.getOriginalDefinition().getItemName());
////            itemDEf.toMutable().setDisplayName(dto.getDisplayName());
////            itemDEf.toMutable().setDisplayOrder(dto.getDisplayOrder());
////        }
//        return mutableDefinition;
//    }
}
