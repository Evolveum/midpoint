/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.metadata;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.metadata.ValueMetadataAdapter;
import com.evolveum.midpoint.prism.metadata.ValueMetadataFactory;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.jetbrains.annotations.NotNull;

@Experimental
public class MidpointValueMetadataFactory implements ValueMetadataFactory {

    @NotNull private final PrismContext prismContext;

    public MidpointValueMetadataFactory(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    @Override
    public @NotNull ValueMetadata createEmpty() {
        try {
            return ValueMetadataAdapter.holding(
                    prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(ValueMetadataType.class)
                            .instantiate(PrismConstants.VALUE_METADATA_CONTAINER_NAME));
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception while creating value metadata container: " + e.getMessage(), e);
        }
    }

    public static ValueMetadata createFrom(@NotNull PrismContainer<?> container) {
        return ValueMetadataAdapter.holding(container);
    }
}
