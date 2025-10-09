/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocalAutoassignSpecificationType;

public class FocalAutoassignSpecificationConfigItem extends ConfigurationItem<FocalAutoassignSpecificationType> {

    @SuppressWarnings({ "unused", "WeakerAccess" }) // called dynamically
    public FocalAutoassignSpecificationConfigItem(@NotNull ConfigurationItem<FocalAutoassignSpecificationType> original) {
        super(original);
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
