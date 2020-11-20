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
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingEvaluationTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

import static com.evolveum.midpoint.prism.delta.DeltaSetTripleUtil.isEmpty;

/**
 *
 */
public class MappingEvaluationOpNode extends AbstractMappingEvaluationOpNode {

    public MappingEvaluationOpNode(PrismContext prismContext,
            OperationResultType result,
            OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
    }

    protected void postProcess() {
        setDisabled(trace != null && hasNoOutput());
    }

    private boolean hasNoOutput() {
        return trace != null && isEmpty(trace.getOutput());
    }

    public String getMappingInfo() {
        MappingEvaluationTraceType trace = getTrace(MappingEvaluationTraceType.class);
        if (trace != null) {
            String outputs = getOutputsAsString();
            return getMappingNameOrSignature() + " ⇒ " + (!outputs.isEmpty() ? outputs : "no outputs");
        } else {
            return context;
        }
    }
}
