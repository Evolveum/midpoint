/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.visualizer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ModelContextVisualization implements Serializable {

    private List<Visualization> primary;

    private List<Visualization> secondary;

    public ModelContextVisualization() {
        this(null, null);
    }

    public ModelContextVisualization(List<Visualization> primary, List<Visualization> secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    @NotNull
    public List<Visualization> getPrimary() {
        if (primary == null) {
            primary = new ArrayList<>();
        }
        return primary;
    }

    @NotNull
    public List<Visualization> getSecondary() {
        if (secondary == null) {
            secondary = new ArrayList<>();
        }
        return secondary;
    }
}
