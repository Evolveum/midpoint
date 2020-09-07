/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.operations;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.schema.traces.OpResultInfo;
import com.evolveum.midpoint.schema.traces.TraceInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FullShadowLoadedTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

public class FullProjectionLoadOpNode extends OpNode {

    private final FullShadowLoadedTraceType trace;

    public FullProjectionLoadOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
        trace = getTrace(FullShadowLoadedTraceType.class);
    }

    public String getInfo() {
        if (trace != null) {
            return getResourceName() + ": " + getShadowName();
        } else {
            return "";
        }
    }

    public String getShadowName() {
        if (trace != null && trace.getShadowLoadedRef() != null) {
            return PolyString.getOrig(trace.getShadowLoadedRef().getTargetName());
        } else {
            return null;
        }
    }

    public String getResourceName() {
        return trace != null ? PolyString.getOrig(trace.getResourceName()) : null;
    }
}
