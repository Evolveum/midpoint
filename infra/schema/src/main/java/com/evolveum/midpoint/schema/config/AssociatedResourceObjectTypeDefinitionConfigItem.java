/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociatedResourceObjectTypeDefinitionType;

public class AssociatedResourceObjectTypeDefinitionConfigItem
        extends AbstractResourceObjectTypeDefinitionConfigItem<AssociatedResourceObjectTypeDefinitionType> {

    @SuppressWarnings("unused") // called dynamically
    public AssociatedResourceObjectTypeDefinitionConfigItem(
            @NotNull ConfigurationItem<AssociatedResourceObjectTypeDefinitionType> original) {
        super(original);
    }

    @Override
    public @NotNull String localDescription() {
        return "associated " + super.localDescription();
    }
}
