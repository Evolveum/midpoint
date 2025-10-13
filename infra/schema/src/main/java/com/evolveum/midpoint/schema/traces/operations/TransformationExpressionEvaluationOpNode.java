/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.traces.operations;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.schema.traces.OpResultInfo;
import com.evolveum.midpoint.schema.traces.TraceInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueTransformationExpressionEvaluationTraceType;

import org.apache.commons.lang3.StringUtils;

/**
 *
 */
public class TransformationExpressionEvaluationOpNode extends OpNode {

    private final ValueTransformationExpressionEvaluationTraceType trace;

    public TransformationExpressionEvaluationOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
        trace = getTrace(ValueTransformationExpressionEvaluationTraceType.class);
    }

    public ValueTransformationExpressionEvaluationTraceType getTrace() {
        return trace;
    }

    public String getContextDescription() {
        if (trace != null && trace.getLocalContextDescription() != null) {
            return trace.getLocalContextDescription();
        }
        String context = getContext("context");
        if (StringUtils.isNotEmpty(context)) {
            return context;
        }
        if (trace != null && trace.getContextDescription() != null) {
            return trace.getContextDescription();
        }
        return "";
    }
}
