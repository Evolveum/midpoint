/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.mapping.metadata;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public abstract class DefaultValueMetadataProcessing {

    private static final Map<ItemName, DefaultValueMetadataProcessing> ITEM_DEFAULTS;
    private static final DefaultValueMetadataProcessing DISABLED = new DefaultValueMetadataProcessing() {
        @Override
        boolean isEnabledFor(ItemPath dataPath, ItemDefinition<?> dataDefinition) {
            return false;
        }
    };

    static {
        ITEM_DEFAULTS = ImmutableMap.<ItemName, DefaultValueMetadataProcessing>builder()
                .put(ValueMetadataType.F_PROVENANCE, defaultProvenanceProcessing())
                .build();
    }



    public static DefaultValueMetadataProcessing forMetadataItem(ItemName name) {
        var maybe = ITEM_DEFAULTS.get(name);
        if (maybe != null) {
            return maybe;
        }
        return DISABLED;
    }

    /**
     * Returns true if value metadata processing should be enabled for item by default.
     *
     * @param dataPath item path of data
     * @param dataDefinition definition of data
     * @return
     */
    abstract boolean isEnabledFor(ItemPath dataPath, ItemDefinition<?> dataDefinition);

    private static DefaultValueMetadataProcessing defaultProvenanceProcessing() {
        return new DefaultValueMetadataProcessing() {
            @Override
            boolean isEnabledFor(ItemPath dataPath, ItemDefinition<?> dataDefinition) {
                return dataDefinition.isMultiValue();
            }
        };
    };

}
