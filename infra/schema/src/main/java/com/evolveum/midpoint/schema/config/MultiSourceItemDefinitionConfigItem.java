/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
