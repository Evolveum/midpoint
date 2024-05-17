/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.path.ItemName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FrameworkNameResolver {

    /** Resolves framework-specific name (like `__NAME__`) to midPoint name (like `ri:dn`), if available. */
    @Nullable ItemName resolveFrameworkName(@NotNull String frameworkName);

    /**
     * Creates a simple resolver based on a resource object definition. Faster implementations may exist.
     * We assume that association definitions need not be covered by this method.
     */
    static FrameworkNameResolver simple(@NotNull ResourceObjectDefinition definition) {
        return frameworkName -> findInObjectDefinition(definition, frameworkName);
    }

    static @Nullable ItemName findInObjectDefinition(@NotNull ResourceObjectDefinition definition, String frameworkName) {
        for (ShadowSimpleAttributeDefinition<?> attributeDefinition : definition.getSimpleAttributeDefinitions()) {
            if (frameworkName.equals(attributeDefinition.getFrameworkAttributeName())) {
                return attributeDefinition.getItemName();
            }
        }
        return null;
    }
}
