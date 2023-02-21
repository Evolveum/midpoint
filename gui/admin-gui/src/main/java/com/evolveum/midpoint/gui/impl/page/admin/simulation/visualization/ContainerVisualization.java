/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization;

import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.util.LocalizableMessage;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ContainerVisualization implements Serializable {

    private Visualization visualization;

    public ContainerVisualization(@NotNull Visualization visualization) {
        this.visualization = visualization;
    }

    public Visualization getVisualization() {

        return visualization;
    }

    public ChangeType getChangeType() {
        return visualization.getChangeType();
    }
}
