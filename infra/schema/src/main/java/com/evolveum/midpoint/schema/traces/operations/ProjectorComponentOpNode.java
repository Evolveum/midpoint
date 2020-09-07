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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectorComponentTraceType;

/**
 *
 */
public class ProjectorComponentOpNode extends OpNode {

    private final ProjectorComponentTraceType trace;

    public ProjectorComponentOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
        trace = getTrace(ProjectorComponentTraceType.class);
    }

    // temporary implementation
    protected void postProcess() {
        if (trace != null && info.getType() != null) {
            switch (info.getType()) {
                case PROJECTOR_INBOUND:
                case PROJECTOR_TEMPLATE_BEFORE_ASSIGNMENTS:
                case PROJECTOR_TEMPLATE_AFTER_ASSIGNMENTS:
                    setDisabled(getMappingsCount() == 0);
                    break;
                case PROJECTOR_ASSIGNMENTS:
                    setDisabled(getAssignmentEvaluationsCount() == 0);
                    break;
            }
        }
    }

    public ProjectorComponentTraceType getTrace() {
        return trace;
    }
}
