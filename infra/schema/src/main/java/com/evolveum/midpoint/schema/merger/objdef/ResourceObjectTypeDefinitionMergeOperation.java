/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.objdef;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType.F_ABSTRACT;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.merger.BaseMergeOperation;
import com.evolveum.midpoint.schema.merger.GenericItemMerger;
import com.evolveum.midpoint.schema.merger.IgnoreSourceItemMerger;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * Merges {@link ResourceObjectTypeDefinitionType} objects.
 */
public class ResourceObjectTypeDefinitionMergeOperation extends BaseMergeOperation<ResourceObjectTypeDefinitionType> {

    public ResourceObjectTypeDefinitionMergeOperation(
            @NotNull ResourceObjectTypeDefinitionType target,
            @NotNull ResourceObjectTypeDefinitionType source) {

        super(target,
                source,
                new GenericItemMerger(createPathMap(Map.of(
                        F_ABSTRACT, IgnoreSourceItemMerger.INSTANCE // otherwise everything would be abstract
                ))));
    }
}
