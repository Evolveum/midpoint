/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.metadata;

import com.evolveum.midpoint.prism.ValueMetadata;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

/**
 * Provides empty value metadata.
 */
@Experimental
public interface ValueMetadataFactory {

    @NotNull
    ValueMetadata createEmpty();

}
