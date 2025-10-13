/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.metadata;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Objects;

public abstract class DefaultValueMetadataProcessing {

    private static final Map<ItemName, DefaultValueMetadataProcessing> ITEM_DEFAULTS;
    private static final DefaultValueMetadataProcessing DISABLED = new DefaultValueMetadataProcessing() {
        @Override
        public boolean isEnabledFor(ItemPath dataPath, ItemDefinition<?> dataDefinition) {
            return false;
        }
    };
    private static final DefaultValueMetadataProcessing ASSIGNMENTS_ONLY = new DefaultValueMetadataProcessing() {

        @Override
        public boolean isEnabledFor(ItemPath dataPath, ItemDefinition<?> dataDefinition) {
            return AssignmentType.COMPLEX_TYPE.equals(dataDefinition.getTypeName());
        }
    };


    private static final DefaultValueMetadataProcessing MULTIVALUE = new DefaultValueMetadataProcessing() {

        @Override
        public boolean isEnabledFor(ItemPath dataPath, ItemDefinition<?> dataDefinition) {
            return dataDefinition.isMultiValue();
        }
    };

    private static DefaultValueMetadataProcessing defaultProvenance = MULTIVALUE;

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

    public static void setDisableDefaultMultivalueProvenance(Boolean disableMultivalue) {
        var disabled = Objects.requireNonNullElse(disableMultivalue, false);
        // If multivalue provenance is disabled, we use assignment only, otherwise we use multivalue
        defaultProvenance = disabled ? ASSIGNMENTS_ONLY : MULTIVALUE;
    }

    /**
     * Returns true if value metadata processing should be enabled for item by default.
     *
     * @param dataPath item path of data
     * @param dataDefinition definition of data
     * @return
     */
    public abstract boolean isEnabledFor(ItemPath dataPath, ItemDefinition<?> dataDefinition);

    private static DefaultValueMetadataProcessing defaultProvenanceProcessing() {
        return new DefaultValueMetadataProcessing() {
            @Override
            public boolean isEnabledFor(ItemPath dataPath, ItemDefinition<?> dataDefinition) {
                return defaultProvenance.isEnabledFor(dataPath, dataDefinition);
            }
        };
    };

}
