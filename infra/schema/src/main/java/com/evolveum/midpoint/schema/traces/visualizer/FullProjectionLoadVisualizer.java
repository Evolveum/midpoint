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
import com.evolveum.midpoint.xml.ns._public.common.common_3.FullShadowLoadedTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericTraceVisualizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

@Experimental
public class FullProjectionLoadVisualizer extends BaseVisualizer {

    FullProjectionLoadVisualizer(PrismContext prismContext) {
        super(prismContext);
    }

    @Override
    public void visualize(StringBuilder sb, OpNode node, int indent) {
        GenericTraceVisualizationType generic = defaultIfNull(node.getGenericVisualization(), GenericTraceVisualizationType.ONE_LINE);

        String nullPrefix = indent(sb, node, indent);
        FullShadowLoadedTraceType trace = node.getTrace(FullShadowLoadedTraceType.class);
        if (trace != null) {
            ObjectReferenceType shadowLoadedRef = trace.getShadowLoadedRef();
            PrismObject<?> shadow = shadowLoadedRef != null ? shadowLoadedRef.asReferenceValue().getObject() : null;

            if (shadow != null) {
                sb.append("Projection (").append(shadow.asObjectable().getName()).append(") loaded from ").append(trace.getResourceName());
                appendInvocationIdAndDuration(sb, node);
                sb.append("\n");

                if (generic != GenericTraceVisualizationType.ONE_LINE) {
                    String prefix = nullPrefix + " < ";
                    PrismObject<?> objectToDisplay = removeShadowAuxiliaryItemsIfNeeded(shadow, generic);
                    appendWithPrefix(sb, prefix, objectToDisplay.debugDump());
                }
            } else {
                sb.append("Projection loaded from ").append(trace.getResourceName());
                appendInvocationIdAndDuration(sb, node);
                sb.append("\n");
            }
        } else {
            sb.append("Projection loaded");
            appendInvocationIdAndDuration(sb, node);
            sb.append("\n");
        }
    }
}
