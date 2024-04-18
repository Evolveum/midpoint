/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.dto;

import java.util.Collection;

import com.evolveum.midpoint.prism.EnumerationTypeDefinition;

public class EnumerationTypeDefinitionDto extends DefinitionDto {

    Collection<EnumerationTypeDefinition.ValueDefinition> values;

    public EnumerationTypeDefinitionDto(EnumerationTypeDefinition definition) {
        super(definition);

        values = definition.getValues();
    }

    public Collection<EnumerationTypeDefinition.ValueDefinition> getValues() {
        return values;
    }
}
