/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
