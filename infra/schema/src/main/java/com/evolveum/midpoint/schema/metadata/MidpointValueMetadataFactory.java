/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.metadata;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.ValueMetadata;
import com.evolveum.midpoint.prism.impl.metadata.ValueMetadataAdapter;
import com.evolveum.midpoint.prism.metadata.ValueMetadataFactory;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.jetbrains.annotations.NotNull;

@Experimental
public class MidpointValueMetadataFactory implements ValueMetadataFactory {

    @NotNull private final PrismContext prismContext;

    public MidpointValueMetadataFactory(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    @Override
    @NotNull
    public ValueMetadata createEmpty() {
        return createFrom(
                new ValueMetadataType(prismContext)
                        .asPrismContainerValue());
    }

    public static ValueMetadata createFrom(PrismContainerValue<?> pcv) {
        return ValueMetadataAdapter.holding(pcv);
    }
}
