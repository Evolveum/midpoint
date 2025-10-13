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

/**
 *
 */
public class ProjectorProjectionOpNode extends ProjectorComponentOpNode {

    public ProjectorProjectionOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
    }

    public String getInfo() {
        if (trace != null && trace.getResourceShadowDiscriminator() != null) {
            return FormattingUtil.getDiscriminatorDescription(trace.getResourceShadowDiscriminator());
        } else {
            return getParameter("resourceName");
        }
    }
}
