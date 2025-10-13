/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.visualizer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ModelContextVisualization implements Serializable, DebugDumpable {

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

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(ModelContextVisualization.class, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "primary", primary, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "secondary", primary, indent + 1);
        return sb.toString();
    }
}
