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
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingEvaluationTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

/**
 *
 */
public class MappingTimeValidityEvaluationOpNode extends AbstractMappingEvaluationOpNode {

    public MappingTimeValidityEvaluationOpNode(PrismContext prismContext,
            OperationResultType result,
            OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
    }

    public String getMappingInfo() {
        MappingEvaluationTraceType trace = getTrace(MappingEvaluationTraceType.class);
        if (trace != null) {
            return getMappingNameOrSignature() + " ⇒ " + getTimeValidityInfo();
        } else {
            return context;
        }
    }
}
