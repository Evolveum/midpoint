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

/**
 *
 */
public class FocusChangeExecutionOpNode extends OpNode {

    private boolean initialized;

    private ModelExecuteDeltaTraceType trace;
    private ObjectDeltaOperationType objectDeltaOperation;

    public FocusChangeExecutionOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
    }

    public ModelExecuteDeltaTraceType getTrace() {
        initialize();
        return trace;
    }

    public ObjectDeltaOperationType getObjectDeltaOperation() {
        initialize();
        return objectDeltaOperation;
    }

    private void initialize() {
        if (!initialized) {
            trace = getTraceDownwards(ModelExecuteDeltaTraceType.class, 1);
            LensObjectDeltaOperationType lensObjectDeltaOperation = trace != null ? trace.getDelta() : null;
            objectDeltaOperation = lensObjectDeltaOperation != null ? lensObjectDeltaOperation.getObjectDeltaOperation() : null;
            initialized = true;
        }
    }

}
