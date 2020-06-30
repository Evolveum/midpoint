/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.builtin;

import javax.annotation.PostConstruct;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * TODO
 */
abstract class BaseBuiltinMetadataMapping implements BuiltinMetadataMapping {

    @NotNull private final ItemPath targetPath;
//    private ItemDefinition<?> targetDefinition;

//    @Autowired private PrismContext prismContext;
    @Autowired private BuiltinMetadataMappingsRegistry registry;

    BaseBuiltinMetadataMapping(@NotNull ItemPath targetItem) {
        this.targetPath = targetItem;
    }

    @PostConstruct
    void register() {
        registry.registerBuiltinMapping(this);
//        targetDefinition = Objects.requireNonNull(
//                prismContext.getSchemaRegistry().getValueMetadataDefinition().findItemDefinition(targetPath),
//                () -> "No definition for metadata item " + targetPath);
    }

    @Override
    @NotNull
    public ItemPath getTargetPath() {
        return targetPath;
    }

    void addPropertyRealValue(PrismContainerValue<?> outputMetadata, Object value) throws SchemaException {
        if (value != null) {
            PrismProperty property = outputMetadata.findOrCreateProperty(targetPath);
            //noinspection unchecked
            property.addRealValue(value);
        }
    }
}
