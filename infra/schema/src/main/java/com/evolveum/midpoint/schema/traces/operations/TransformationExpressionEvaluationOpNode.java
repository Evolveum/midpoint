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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueTransformationExpressionEvaluationTraceType;

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

}
