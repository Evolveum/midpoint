/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.visualizer;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.prism.path.ItemPath;

public interface VisualizationItem extends Serializable {

    Name getName();
    List<VisualizationItemValue> getNewValues();

    boolean isOperational();

    /**
     * Item path, relative to the visualization root path.
     */
    ItemPath getSourceRelPath();

    boolean isDescriptive();
}
