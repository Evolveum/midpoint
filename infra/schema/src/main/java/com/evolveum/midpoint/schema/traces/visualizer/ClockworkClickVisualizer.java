/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.visualizer;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.util.annotation.Experimental;

@Experimental
public class ClockworkClickVisualizer extends BaseVisualizer {

    ClockworkClickVisualizer(PrismContext prismContext) {
        super(prismContext);
    }

    @Override
    public void visualize(StringBuilder sb, OpNode node, int indent) {
        indent(sb, node, indent);
        sb.append(node.getOperationNameFormatted())
                .append(": state ").append(node.getClockworkState())
                .append(", execution wave ").append(node.getExecutionWave())
                .append(", projection wave ").append(node.getProjectionWave());
        appendInvocationIdAndDuration(sb, node);
        sb.append("\n");
    }

}
