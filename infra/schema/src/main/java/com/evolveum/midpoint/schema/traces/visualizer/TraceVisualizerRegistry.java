/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.visualizer;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationKindType;

/**
 * TODO rework
 */
@Experimental
public class TraceVisualizerRegistry {

    private final DefaultVisualizer defaultVisualizer;

    private Map<OperationKindType, Visualizer> visualizers = new HashMap<>();

    public TraceVisualizerRegistry(PrismContext prismContext) {
        defaultVisualizer = new DefaultVisualizer(prismContext);
        visualizers.put(OperationKindType.CLOCKWORK_EXECUTION, new ClockworkExecutionVisualizer(prismContext));
        visualizers.put(OperationKindType.CLOCKWORK_CLICK, new ClockworkClickVisualizer(prismContext));
        visualizers.put(OperationKindType.MAPPING_EVALUATION, new MappingEvaluationVisualizer(prismContext));
        visualizers.put(OperationKindType.FOCUS_CHANGE_EXECUTION, new ChangeExecutionVisualizer(prismContext));
        visualizers.put(OperationKindType.PROJECTION_CHANGE_EXECUTION, new ChangeExecutionVisualizer(prismContext));
        visualizers.put(OperationKindType.FOCUS_LOAD, new FocusLoadVisualizer(prismContext));
        visualizers.put(OperationKindType.FULL_PROJECTION_LOAD, new FullProjectionLoadVisualizer(prismContext));
    }

    Visualizer getVisualizer(OperationKindType operationKind) {
        Visualizer visualizer = visualizers.get(operationKind);
        if (visualizer != null) {
            return visualizer;
        } else {
            return defaultVisualizer;
        }
    }
}
