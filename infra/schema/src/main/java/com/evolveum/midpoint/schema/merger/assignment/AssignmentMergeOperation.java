/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.assignment;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType.F_ABSTRACT;

import java.util.Map;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.merger.BaseMergeOperation;
import com.evolveum.midpoint.schema.merger.GenericItemMerger;
import com.evolveum.midpoint.schema.merger.IgnoreSourceItemMerger;
import com.evolveum.midpoint.schema.merger.OriginMarker;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * Merges {@link AssignmentType} objects.
 */
public class AssignmentMergeOperation extends BaseMergeOperation<AssignmentType> {

    public AssignmentMergeOperation(
            @NotNull AssignmentType target,
            @NotNull AssignmentType source,
            @Nullable OriginMarker originMarker) {

        super(target,
                source,
                new GenericItemMerger(
                        originMarker,
                        createPathMap(Map.of())));
    }
}
