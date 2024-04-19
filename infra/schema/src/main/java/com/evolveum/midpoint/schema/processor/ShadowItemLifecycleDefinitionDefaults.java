/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.PrismLifecycleDefinition;

/** Declares the default values for lifecycle properties for shadow items definitions. */
public interface ShadowItemLifecycleDefinitionDefaults extends PrismLifecycleDefinition {

    @Override
    default boolean isDeprecated() {
        return false;
    }

    @Override
    default boolean isRemoved() {
        return false;
    }

    @Override
    default String getRemovedSince() {
        return null;
    }

    @Override
    default boolean isExperimental() {
        return false;
    }

    @Override
    default String getPlannedRemoval() {
        return null;
    }

    @Override
    default String getDeprecatedSince() {
        return null;
    }
}
