/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.visualizer;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusLoadedTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericTraceVisualizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

@Experimental
public class FocusLoadVisualizer extends BaseVisualizer {

    FocusLoadVisualizer(PrismContext prismContext) {
        super(prismContext);
    }

    @Override
    public void visualize(StringBuilder sb, OpNode node, int indent) {
        GenericTraceVisualizationType generic = defaultIfNull(node.getGenericVisualization(), GenericTraceVisualizationType.ONE_LINE);

        String nullPrefix = indent(sb, node, indent);
        sb.append("Focus loaded");
        appendInvocationIdAndDuration(sb, node);
        sb.append("\n");
        FocusLoadedTraceType trace = node.getTrace(FocusLoadedTraceType.class);
        if (trace != null && generic != GenericTraceVisualizationType.ONE_LINE) {
            ObjectReferenceType focusLoadedRef = trace.getFocusLoadedRef();
            PrismObject<?> focus = focusLoadedRef != null ? focusLoadedRef.asReferenceValue().getObject() : null;
            if (focus != null) {
                String prefix = nullPrefix + " < ";
                PrismObject<?> objectToDisplay = removeOperationalItemsIfNeeded(focus, generic);
                appendWithPrefix(sb, prefix, objectToDisplay.debugDump());
            }
        }
    }
}
