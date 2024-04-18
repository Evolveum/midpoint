/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.dto;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.*;

public class ComplexTypeDefinitionDto extends DefinitionDto<ComplexTypeDefinition> implements Serializable {

    public static final String F_DEFINITIONS = "definitionDtos";

    private List<? extends ItemDefinitionDto> definitionDtos;

    public ComplexTypeDefinitionDto(ComplexTypeDefinition ctd) {
        super(ctd);
    }

    public List<? extends ItemDefinitionDto> getDefinitionDtos() {
        if (definitionDtos == null) {
            initDefinitionDtos();

        }
        return definitionDtos;
    }

    private void initDefinitionDtos() {
        definitionDtos = getOriginalDefinition().getDefinitions().stream()
            .map(this::createDefinitionDto)
            .collect(Collectors.toList());
    }

    private ItemDefinitionDto createDefinitionDto(ItemDefinition<?> def) {
        return new ItemDefinitionDto(def);
    }


}
