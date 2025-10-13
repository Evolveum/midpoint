/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.traces.visualizer;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.util.annotation.Experimental;

@Experimental
public class ClockworkExecutionVisualizer extends BaseVisualizer {

    ClockworkExecutionVisualizer(PrismContext prismContext) {
        super(prismContext);
    }

    @Override
    public void visualize(StringBuilder sb, OpNode node, int indent) {
        indent(sb, node, indent);
        sb.append(node.getOperationNameFormatted())
                .append(" with focus '")
                .append(node.getFocusName())
                .append("'");
        appendInvocationIdAndDuration(sb, node);
        sb.append("\n");
    }
}
