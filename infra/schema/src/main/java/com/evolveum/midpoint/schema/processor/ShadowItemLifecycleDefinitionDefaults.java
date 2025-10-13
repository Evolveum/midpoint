/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
