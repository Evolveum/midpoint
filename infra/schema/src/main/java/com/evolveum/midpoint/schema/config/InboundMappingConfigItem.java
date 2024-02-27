/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingType;

public class InboundMappingConfigItem
        extends ConfigurationItem<InboundMappingType>
        implements AbstractMappingConfigItem<InboundMappingType> {

    @SuppressWarnings("unused") // called dynamically
    public InboundMappingConfigItem(@NotNull ConfigurationItem<? extends InboundMappingType> original) {
        super(original);
    }
}
