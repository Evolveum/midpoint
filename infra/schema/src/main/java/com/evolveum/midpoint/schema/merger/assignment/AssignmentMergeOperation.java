/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.merger.assignment;

import java.util.Map;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.merger.BaseMergeOperation;
import com.evolveum.midpoint.prism.impl.GenericItemMerger;
import com.evolveum.midpoint.prism.OriginMarker;

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
