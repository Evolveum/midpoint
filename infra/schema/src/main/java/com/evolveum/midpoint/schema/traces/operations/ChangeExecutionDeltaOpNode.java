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
import com.evolveum.midpoint.xml.ns._public.common.common_3.LensObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteDeltaTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

public class ChangeExecutionDeltaOpNode extends OpNode {

    private final ModelExecuteDeltaTraceType trace;
    private final ObjectDeltaOperationType objectDeltaOperation;

    public ChangeExecutionDeltaOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
        trace = getTrace(ModelExecuteDeltaTraceType.class);
        LensObjectDeltaOperationType lensObjectDeltaOperation = trace != null ? trace.getDelta() : null;
        objectDeltaOperation = lensObjectDeltaOperation != null ? lensObjectDeltaOperation.getObjectDeltaOperation() : null;
    }

    public ModelExecuteDeltaTraceType getTrace() {
        return trace;
    }

    public ObjectDeltaOperationType getObjectDeltaOperation() {
        return objectDeltaOperation;
    }

    public ObjectDeltaType getObjectDelta() {
        return objectDeltaOperation != null ? objectDeltaOperation.getObjectDelta() : null;
    }

}
