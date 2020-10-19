/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata.builtin;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.mapping.metadata.ConsolidationMetadataComputation;
import com.evolveum.midpoint.model.common.mapping.metadata.TransformationalMetadataComputation;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * TODO
 */
public interface BuiltinMetadataMapping {

    void applyForTransformation(@NotNull TransformationalMetadataComputation computation) throws SchemaException;

    void applyForConsolidation(@NotNull ConsolidationMetadataComputation computation) throws SchemaException;

    @NotNull
    ItemPath getTargetPath();
}
