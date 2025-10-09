/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.mapping.metadata;

import com.evolveum.midpoint.model.common.mapping.AbstractMappingImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.TransformationValueMetadataComputer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataMappingType;

/**
 * Evaluated or to-be-evaluated metadata mapping.
 */
public class MetadataMappingImpl<V extends PrismValue, D extends ItemDefinition<?>> extends
        AbstractMappingImpl<V, D, MetadataMappingType> {

    MetadataMappingImpl(MetadataMappingBuilder<V, D> builder) {
        super(builder);
    }

    private MetadataMappingImpl(MetadataMappingImpl<V, D> prototype) {
        super(prototype);
    }

    protected TransformationValueMetadataComputer createValueMetadataComputer(OperationResult result) {
        // No value metadata computing for value metadata itself.
        return null;
    }

    @Override
    protected boolean determinePushChangesRequested() {
        return false;
    }

    @Override
    public MetadataMappingImpl<V, D> clone() {
        return new MetadataMappingImpl<>(this);
    }
}
