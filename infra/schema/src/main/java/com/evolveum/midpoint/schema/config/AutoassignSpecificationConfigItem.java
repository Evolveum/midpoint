/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AutoassignSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocalAutoassignSpecificationType;

import static com.evolveum.midpoint.schema.config.ConfigurationItem.configItem;

public class AutoassignSpecificationConfigItem extends ConfigurationItem<AutoassignSpecificationType> {

    @SuppressWarnings("unused") // called dynamically
    public AutoassignSpecificationConfigItem(@NotNull ConfigurationItem<AutoassignSpecificationType> original) {
        super(original);
    }

    public static AutoassignSpecificationConfigItem embedded(@NotNull AutoassignSpecificationType value) {
        return configItem(value, ConfigurationItemOrigin.embedded(value), AutoassignSpecificationConfigItem.class);
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(value().isEnabled());
    }

    public @Nullable FocalAutoassignSpecificationConfigItem getFocus() {
        FocalAutoassignSpecificationType bean = value().getFocus();
        if (bean != null) {
            return new FocalAutoassignSpecificationConfigItem(
                    child(bean, AutoassignSpecificationType.F_FOCUS));
        } else {
            return null;
        }
    }

    @Override
    public @NotNull String localDescription() {
        return "auto-assignment mapping";
    }
}
