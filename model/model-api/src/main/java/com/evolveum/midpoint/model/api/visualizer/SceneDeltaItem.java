/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.visualizer;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

/**
 * @author mederly
 */
public interface SceneDeltaItem extends SceneItem, Serializable {

    @NotNull List<? extends SceneItemValue> getOldValues();
    @NotNull List<? extends SceneItemValue> getAddedValues();
    @NotNull List<? extends SceneItemValue> getDeletedValues();
    @NotNull List<? extends SceneItemValue> getUnchangedValues();

    /**
     * Item delta (if applicable). It should contain the original path (not a relative one).
     */
    ItemDelta<?, ?> getSourceDelta();
}
