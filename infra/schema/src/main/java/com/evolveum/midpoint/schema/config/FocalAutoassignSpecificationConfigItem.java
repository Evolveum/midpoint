/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocalAutoassignSpecificationType;

public class FocalAutoassignSpecificationConfigItem extends ConfigurationItem<FocalAutoassignSpecificationType> {

    @SuppressWarnings("unused") // called dynamically
    public FocalAutoassignSpecificationConfigItem(@NotNull ConfigurationItem<FocalAutoassignSpecificationType> original) {
        super(original);
    }

    public static FocalAutoassignSpecificationConfigItem embedded(@NotNull FocalAutoassignSpecificationType value) {
        return ConfigurationItem.embedded(value)
                .as(FocalAutoassignSpecificationConfigItem.class);
    }

    public @Nullable ObjectSelectorConfigItem getSelector() {
        var bean = value().getSelector();
        if (bean != null) {
            return new ObjectSelectorConfigItem(
                    child(bean, FocalAutoassignSpecificationType.F_SELECTOR));
        } else {
            return null;
        }
    }

    public @NotNull List<AutoAssignMappingConfigItem> getMappings() {
        return value().getMapping().stream()
                .map(val ->
                        new AutoAssignMappingConfigItem(
                                childWithOrWithoutId(val, FocalAutoassignSpecificationType.F_MAPPING)))
                .toList();
    }
}
