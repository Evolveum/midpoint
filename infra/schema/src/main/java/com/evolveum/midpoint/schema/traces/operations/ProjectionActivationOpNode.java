/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.traces.operations;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.schema.traces.OpResultInfo;
import com.evolveum.midpoint.schema.traces.TraceInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

/**
 *
 */
public class ProjectionActivationOpNode extends OpNode {

    public ProjectionActivationOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
    }

    public String getDecisionInfo() {
        String decision = getDecision();
        if (StringUtils.isNotEmpty(decision)) {
            return "⇒ " + decision;
        } else {
            return "";
        }
    }

    @NotNull
    public String getDecision() {
        return getReturn("decision").toLowerCase();
    }
}
