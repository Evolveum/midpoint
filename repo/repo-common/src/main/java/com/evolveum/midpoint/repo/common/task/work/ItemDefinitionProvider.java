/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.work;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Provides definitions of an item (for bucketing purposes).
 */
@FunctionalInterface
public interface ItemDefinitionProvider {

    @Nullable ItemDefinition<?> getItemDefinition(@NotNull ItemPath itemPath);

    @NotNull static ItemDefinitionProvider forObjectClassAttributes(
            ObjectClassComplexTypeDefinition objectClass) {
        return itemPath -> {
            if (itemPath.startsWithName(ShadowType.F_ATTRIBUTES)) {
                return objectClass.findAttributeDefinition(itemPath.rest().asSingleName());
            } else {
                return null;
            }
        };
    }
}
