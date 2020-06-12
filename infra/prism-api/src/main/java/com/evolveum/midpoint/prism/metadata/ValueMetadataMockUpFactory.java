/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.metadata;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.ValueMetadata;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.Optional;

/**
 * Provides mock up value metadata for given prism value.
 */
public interface ValueMetadataMockUpFactory {

    Optional<ValueMetadata> createValueMetadata(PrismValue value) throws SchemaException;

}
