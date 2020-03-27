/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.visualizer;

import java.util.List;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.traces.OpNode;

@Experimental
public class TraceTreeVisualizer {

    @NotNull private final TraceVisualizerRegistry registry;
    @NotNull private final List<OpNode> rootNodeList;

    public TraceTreeVisualizer(@NotNull TraceVisualizerRegistry registry, @NotNull List<OpNode> rootNodeList) {
        this.registry = registry;
        this.rootNodeList = rootNodeList;
    }

    public String visualize() {
        StringBuilder sb = new StringBuilder();
        for (OpNode rootNode : rootNodeList) {
            visualize(sb, rootNode, 0);
        }
        return sb.toString();
    }

    private void visualize(StringBuilder sb, OpNode node, int indent) {
        int indentDelta;
        if (node.isVisible()) {
            getVisualizer(node).visualize(sb, node, indent);
            indentDelta = 1;
        } else {
            indentDelta = 0;
        }
        node.getChildren().forEach(child -> visualize(sb, child, indent + indentDelta));
        if (node.isVisible()) {
            getVisualizer(node).visualizeAfter(sb, node, indent);
        }
    }

    private Visualizer getVisualizer(OpNode node) {
        return registry.getVisualizer(node.getResult().getOperationKind());
    }
}
