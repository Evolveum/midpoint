/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.resource;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType.F_ABSTRACT;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.merger.BaseMergeOperation;
import com.evolveum.midpoint.schema.merger.GenericItemMerger;
import com.evolveum.midpoint.schema.merger.IgnoreSourceItemMerger;
import com.evolveum.midpoint.schema.merger.RequiredItemMerger;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Merges {@link ResourceType} objects.
 */
public class ResourceMergeOperation extends BaseMergeOperation<ResourceType> {

    public ResourceMergeOperation(
            @NotNull ResourceType target,
            @NotNull ResourceType source) {

        super(target,
                source,
                new GenericItemMerger(createPathMap(Map.of(
                        F_NAME, RequiredItemMerger.INSTANCE,
                        F_ABSTRACT, IgnoreSourceItemMerger.INSTANCE // otherwise this would propagate to specific resources
                ))));
    }
}
