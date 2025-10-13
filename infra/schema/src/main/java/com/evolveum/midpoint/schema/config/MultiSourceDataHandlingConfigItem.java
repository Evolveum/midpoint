/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MultiSourceDataHandlingType;

public class MultiSourceDataHandlingConfigItem extends ConfigurationItem<MultiSourceDataHandlingType> {

    @SuppressWarnings("unused") // called dynamically
    public MultiSourceDataHandlingConfigItem(@NotNull ConfigurationItem<MultiSourceDataHandlingType> original) {
        super(original);
    }

    private MultiSourceDataHandlingConfigItem(
            @NotNull MultiSourceDataHandlingType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin, null); // TODO provide parent in the future
    }

    public static MultiSourceDataHandlingConfigItem of(
            @NotNull MultiSourceDataHandlingType bean,
            @NotNull OriginProvider<? super MultiSourceDataHandlingType> originProvider) {
        return new MultiSourceDataHandlingConfigItem(bean, originProvider.origin(bean));
    }

    @Override
    public @NotNull String localDescription() {
        return "object template multi-source data handling definition";
    }

    public @Nullable ObjectTemplateMappingConfigItem getDefaultAuthoritativeSource() {
        return child(
                value().getDefaultAuthoritativeSource(),
                ObjectTemplateMappingConfigItem.class,
                MultiSourceDataHandlingType.F_DEFAULT_AUTHORITATIVE_SOURCE);
    }
}
