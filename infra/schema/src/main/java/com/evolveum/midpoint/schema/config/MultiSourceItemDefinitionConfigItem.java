/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MultiSourceItemDefinitionType;

public class MultiSourceItemDefinitionConfigItem extends ConfigurationItem<MultiSourceItemDefinitionType> {

    @SuppressWarnings("unused") // called dynamically
    public MultiSourceItemDefinitionConfigItem(@NotNull ConfigurationItem<MultiSourceItemDefinitionType> original) {
        super(original);
    }

    public ObjectTemplateMappingConfigItem getSelection() {
        return child(
                value().getSelection(),
                ObjectTemplateMappingConfigItem.class,
                MultiSourceItemDefinitionType.F_SELECTION);
    }
}
