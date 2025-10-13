/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.mapping.metadata;

import com.evolveum.midpoint.model.common.mapping.AbstractMappingBuilder;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataMappingType;

public class MetadataMappingBuilder<V extends PrismValue, D extends ItemDefinition<?>>
    extends AbstractMappingBuilder<V, D, MetadataMappingType, MetadataMappingBuilder<V, D>> {

    @Override
    public MetadataMappingImpl<V, D> build() {
        return new MetadataMappingImpl<>(this);
    }
}
