/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.visualizer;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

public interface VisualizationDeltaItem extends VisualizationItem, Serializable {

    @NotNull List<VisualizationItemValue> getOldValues();
    @NotNull List<VisualizationItemValue> getAddedValues();
    @NotNull List<VisualizationItemValue> getDeletedValues();
    @NotNull List<VisualizationItemValue> getUnchangedValues();

    /**
     * Item delta (if applicable). It should contain the original path (not a relative one).
     */
    ItemDelta<?, ?> getSourceDelta();
}
