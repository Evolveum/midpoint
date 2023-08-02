/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AutoassignSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocalAutoassignSpecificationType;

public class AutoassignSpecificationConfigItem extends ConfigurationItem<AutoassignSpecificationType> {

    @SuppressWarnings("unused") // called dynamically
    public AutoassignSpecificationConfigItem(@NotNull ConfigurationItem<AutoassignSpecificationType> original) {
        super(original);
    }

    public static AutoassignSpecificationConfigItem embedded(@NotNull AutoassignSpecificationType value) {
        return ConfigurationItem.embedded(value)
                .as(AutoassignSpecificationConfigItem.class);
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
}
