/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.ValueMetadataComputer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataMappingType;

/**
 * Evaluated or to-be-evaluated metadata mapping.
 */
public class MetadataMappingImpl<V extends PrismValue, D extends ItemDefinition> extends AbstractMappingImpl<V, D, MetadataMappingType> {

    MetadataMappingImpl(MetadataMappingBuilder<V, D> builder) {
        super(builder);
    }

    private MetadataMappingImpl(MetadataMappingImpl<V, D> prototype) {
        super(prototype);
    }

    ValueMetadataComputer createValueMetadataComputer() {
        return null;
    }

    @Override
    public MetadataMappingImpl<V, D> clone() {
        return new MetadataMappingImpl<>(this);
    }
}
