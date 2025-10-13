/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.mapping.metadata.builtin;

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
