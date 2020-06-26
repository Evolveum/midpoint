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
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

/**
 * (Traditional) data mapping.
 */
public class MappingImpl<V extends PrismValue, D extends ItemDefinition> extends AbstractMappingImpl<V, D, MappingType> {

    MappingImpl(MappingBuilder<V, D> builder) {
        super(builder);
    }

    private MappingImpl(MappingImpl<V, D> prototype) {
        super(prototype);
    }

    ValueMetadataComputer createValueMetadataComputer() {
        if (mappingBean.getMetadataMapping().isEmpty()) {
            return null; // temporary (metadata handling can be inherited - later)
        } else {
            return new SimpleValueMetadataComputer(this);
        }
    }

    @Override
    public MappingImpl<V, D> clone() {
        return new MappingImpl<>(this);
    }
}
