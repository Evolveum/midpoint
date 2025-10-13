/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.merger.simulation;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationDefinitionType.F_SUPER;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.merger.BaseMergeOperation;
import com.evolveum.midpoint.prism.impl.GenericItemMerger;
import com.evolveum.midpoint.schema.merger.IgnoreSourceItemMerger;
import com.evolveum.midpoint.prism.OriginMarker;
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
