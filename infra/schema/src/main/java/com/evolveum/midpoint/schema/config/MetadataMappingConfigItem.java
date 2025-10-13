/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataMappingType;

public class MetadataMappingConfigItem
        extends ConfigurationItem<MetadataMappingType>
        implements AbstractMappingConfigItem<MetadataMappingType> {

    private MetadataMappingConfigItem(@NotNull MetadataMappingType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin, null); // TODO provide parent in the future
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
