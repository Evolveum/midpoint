/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.jetbrains.annotations.NotNull;

/**
 * Builder for (traditional) data mappings.
 */
public class MappingBuilder<V extends PrismValue, D extends ItemDefinition<?>>
    extends AbstractMappingBuilder<V, D, MappingType, MappingBuilder<V, D>> {

    @Override
    @NotNull
    public MappingImpl<V, D> build() {
        return new MappingImpl<>(this);
    }
}
