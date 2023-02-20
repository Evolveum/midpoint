/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.visualizer;

import java.io.Serializable;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;

public interface Visualization extends Serializable, DebugDumpable {

    Name getName();

    ChangeType getChangeType();

    @NotNull List<? extends Visualization> getPartialVisualizations();

    @NotNull List<? extends VisualizationItem> getItems();

    boolean isOperational();

    Visualization getOwner();

    /**
     * Visualization root path, relative to the owning visualization root path.
     */
    ItemPath getSourceRelPath();

    ItemPath getSourceAbsPath();

    /**
     * Source container value where more details can be found.
     * (For visualizations that display object or value add.)
     */
    PrismContainerValue<?> getSourceValue();

    PrismContainerDefinition<?> getSourceDefinition();

    /**
     * Source object delta where more details can be found.
     * (For visualization that display an object delta.)
     */
    ObjectDelta<?> getSourceDelta();

    boolean isEmpty();

    boolean isBroken();
}
