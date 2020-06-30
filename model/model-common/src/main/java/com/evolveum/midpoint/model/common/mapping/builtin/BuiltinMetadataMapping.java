/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.builtin;

import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * TODO
 */
public interface BuiltinMetadataMapping {

    void apply(List<PrismValue> valuesTuple, PrismContainerValue<ValueMetadataType> outputMetadata,
            MappingImpl<?, ?> dataMapping, OperationResult result) throws SchemaException;

    @NotNull
    ItemPath getTargetPath();
}
