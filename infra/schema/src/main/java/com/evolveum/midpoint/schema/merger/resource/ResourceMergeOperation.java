/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.resource;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType.F_ABSTRACT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType.F_TEMPLATE;

import java.util.Map;

import com.evolveum.midpoint.prism.OriginMarker;
import com.evolveum.midpoint.prism.impl.GenericItemMerger;
import com.evolveum.midpoint.schema.OriginMarkerMixin;
import com.evolveum.midpoint.schema.merger.*;

import org.jetbrains.annotations.NotNull;

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
                new GenericItemMerger(
                        OriginMarkerMixin.forOid(source.getOid(), ResourceType.COMPLEX_TYPE),
                        createPathMap(Map.of(
                                // Resource name: we don't want to copy super-resource (e.g. template) name to the specific
                                // resource object. Originally here was "required" but we no longer require the name - e.g.
                                // when new resource is being created.
                                F_NAME, IgnoreSourceItemMerger.INSTANCE,
                                // Neither we want to propagate "abstract" and "template" flags
                                F_ABSTRACT, IgnoreSourceItemMerger.INSTANCE,
                                F_TEMPLATE, IgnoreSourceItemMerger.INSTANCE
                        ))));
    }
}
