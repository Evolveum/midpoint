/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

public class MappingConfigItem extends ConfigurationItem<MappingType> {

    @SuppressWarnings("unused") // called dynamically
    public MappingConfigItem(@NotNull ConfigurationItem<MappingType> original) {
        super(original);
    }

    public MappingConfigItem(@NotNull MappingType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static MappingConfigItem embedded(@NotNull MappingType bean) {
        return of(bean, ConfigurationItemOrigin.embedded(bean));
    }

    public static MappingConfigItem of(@NotNull MappingType bean, @NotNull ConfigurationItemOrigin origin) {
        return new MappingConfigItem(bean, origin);
    }

    public static MappingConfigItem of(
            @NotNull MappingType bean,
            @NotNull OriginProvider<? super MappingType> originProvider) {
        return new MappingConfigItem(bean, originProvider.origin(bean));
    }

}
