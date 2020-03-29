/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.visualizer;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Experimental
public class ChangeExecutionVisualizer extends BaseVisualizer {

    ChangeExecutionVisualizer(PrismContext prismContext) {
        super(prismContext);
    }

    @Override
    public void visualize(StringBuilder sb, OpNode node, int indent) {
        GenericTraceVisualizationType generic = defaultIfNull(node.getGenericVisualization(), GenericTraceVisualizationType.ONE_LINE);

        String label;
        boolean isProjection;
        if (node.getKind() == OperationKindType.FOCUS_CHANGE_EXECUTION) {
            label = "Focus change execution";
            isProjection = false;
        } else {
            label = "Projection change execution";
            isProjection = true;
        }
        ModelExecuteDeltaTraceType trace = node.getTraceDownwards(ModelExecuteDeltaTraceType.class);
        if (trace == null || trace.getDelta() == null || trace.getDelta().getObjectDeltaOperation() == null) {
            indent(sb, node, indent);
            sb.append(label);
            appendInvocationIdAndDuration(sb, node);
            sb.append("\n");
            return;
        }

        ObjectDeltaOperationType odo = trace.getDelta().getObjectDeltaOperation();
        String type = odo.getObjectDelta() != null ? String.valueOf(odo.getObjectDelta().getChangeType()) : "?";
        String nullPrefix = indent(sb, node, indent);
        sb.append(label).append(": ").append(type).append(" of ").append(odo.getObjectName());
        if (isProjection) {
            sb.append(" on ").append(odo.getResourceName());
        }
        appendInvocationIdAndDuration(sb, node);
        sb.append("\n");

        if (generic != GenericTraceVisualizationType.ONE_LINE) {
            String prefix = nullPrefix + " > ";
            String prefixResult = nullPrefix + " = ";
            appendWithPrefix(sb, prefix, dumpDelta(odo.getObjectDelta(), generic));
            appendWithPrefix(sb, prefixResult, showResultStatus(odo.getExecutionResult()));
        }
    }

    private String showResultStatus(OperationResultType result) {
        return result != null ? String.valueOf(result.getStatus()) : null;
    }
}
