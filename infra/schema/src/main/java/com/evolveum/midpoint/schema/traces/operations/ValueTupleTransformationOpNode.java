/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.operations;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.schema.traces.OpResultInfo;
import com.evolveum.midpoint.schema.traces.TraceInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueTransformationTraceType;

/**
 *
 */
public class ValueTupleTransformationOpNode extends OpNode {

    private final ValueTransformationTraceType trace;

    public ValueTupleTransformationOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
        trace = getTrace(ValueTransformationTraceType.class);
    }

    public ValueTransformationTraceType getTrace() {
        return trace;
    }

    public String getValueTupleTransformationDescription() {
        if (trace != null) {
            StringBuilder sb = new StringBuilder();
            if (trace.getLocalContextDescription() != null) {
                sb.append("for ").append(trace.getLocalContextDescription()).append(" ");
            }
            sb.append("(");
            if (trace.getInputOrigin() != null) {
                sb.append(trace.getInputOrigin()).append(" → ");
            }
            sb.append(trace.getDestination());
            sb.append(")");
            if (Boolean.FALSE.equals(trace.isConditionResult())) {
                sb.append(" [cond: false]");
            }
            return sb.toString();
        } else {
            return null;
        }
    }
}
