/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.builtin;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Registry for built-in metadata mappings.
 */
@Component
public class BuiltinMetadataMappingsRegistry {

    private final List<BuiltinMetadataMapping> mappings = new ArrayList<>();

    public List<BuiltinMetadataMapping> getMappings() {
        return mappings;
    }

    void registerBuiltinMapping(BaseBuiltinMetadataMapping mapping) {
        mappings.add(mapping);
    }
}
