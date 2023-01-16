/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.simulation;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationDefinitionType.F_SUPER;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.merger.BaseMergeOperation;
import com.evolveum.midpoint.schema.merger.GenericItemMerger;
import com.evolveum.midpoint.schema.merger.IgnoreSourceItemMerger;
import com.evolveum.midpoint.schema.merger.OriginMarker;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationDefinitionType;

/**
 * Merges {@link SimulationDefinitionType} objects.
 */
public class SimulationDefinitionMergeOperation extends BaseMergeOperation<SimulationDefinitionType> {

    public SimulationDefinitionMergeOperation(
            @NotNull SimulationDefinitionType target,
            @NotNull SimulationDefinitionType source,
            @Nullable OriginMarker originMarker) {

        super(target,
                source,
                new GenericItemMerger(
                        originMarker,
                        createPathMap(Map.of(
                                F_SUPER, IgnoreSourceItemMerger.INSTANCE
                        ))));
    }
}
