/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
