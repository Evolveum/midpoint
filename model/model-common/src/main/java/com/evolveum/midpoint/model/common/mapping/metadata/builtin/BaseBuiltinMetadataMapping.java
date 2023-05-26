/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata.builtin;

import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.prism.PrismValue;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.List;

/**
 * TODO
 */
abstract class BaseBuiltinMetadataMapping implements BuiltinMetadataMapping {

    @NotNull private final ItemPath targetPath;

    @Autowired PrismContext prismContext;
    @Autowired BuiltinMetadataMappingsRegistry registry;

    BaseBuiltinMetadataMapping(@NotNull ItemPath targetItem) {
        this.targetPath = targetItem;
    }

    @PostConstruct
    public void register() {
        registry.registerBuiltinMapping(this);
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

    String dumpInput(List<PrismValue> inputValues) {
        if (inputValues.isEmpty()) {
            return "  (no values)";
        } else {
            StringBuilder sb = new StringBuilder();
            for (PrismValue inputValue : inputValues) {
                if (inputValue != null) {
                    sb.append("  - ").append(inputValue.toString()).append(" with metadata:\n");
                    sb.append(inputValue.getValueMetadata().debugDump(2)).append("\n");
                }
            }
            return sb.toString();
        }
    }
}
