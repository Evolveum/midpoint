/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.delta.ChangeType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.visualizer.Visualization;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ObjectVisualization implements Serializable {

    private Visualization visualization;

    private List<ContainerVisualization> containers = new ArrayList<>();

    public ObjectVisualization(@NotNull Visualization visualization) {
        this.visualization = visualization;

        for (Visualization partial : visualization.getPartialVisualizations()) {
            if (partial.getName().getSimpleDescription() != null) {
                containers.add(new ContainerVisualization(partial));
            }
        }
    }

    public List<ContainerVisualization> getContainers() {
        return containers;
    }

    public Visualization getVisualization() {
        return visualization;
    }

    public ChangeType getChangeType() {
        return visualization.getChangeType();
    }
}
