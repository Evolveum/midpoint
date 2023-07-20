/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataMappingType;

public class MetadataMappingConfigItem extends ConfigurationItem<MetadataMappingType> {

    public MetadataMappingConfigItem(@NotNull MetadataMappingType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static MetadataMappingConfigItem embedded(@NotNull MetadataMappingType bean) {
        return of(bean, ConfigurationItemOrigin.embedded(bean));
    }

    public static MetadataMappingConfigItem of(@NotNull MetadataMappingType bean, @NotNull ConfigurationItemOrigin origin) {
        return new MetadataMappingConfigItem(bean, origin);
    }

    public static MetadataMappingConfigItem of(
            @NotNull MetadataMappingType bean,
            @NotNull OriginProvider<? super MetadataMappingType> originProvider) {
        return new MetadataMappingConfigItem(bean, originProvider.origin(bean));
    }
}
