/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.traces.operations;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.FormattingUtil;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.schema.traces.OpResultInfo;
import com.evolveum.midpoint.schema.traces.TraceInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectConstructionEvaluationTraceType;

public class ResourceObjectConstructionEvaluationOpNode extends OpNode {

    private final ResourceObjectConstructionEvaluationTraceType trace;

    public ResourceObjectConstructionEvaluationOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
        trace = getTrace(ResourceObjectConstructionEvaluationTraceType.class);
    }

    public ResourceObjectConstructionEvaluationTraceType getTrace() {
        return trace;
    }

    public String getInfo() {
        if (trace != null) {
            return FormattingUtil.getDiscriminatorDescription(trace.getResourceShadowDiscriminator());
        } else {
            return getParameter("resourceShadowDiscriminator");
        }
    }
}
