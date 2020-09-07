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
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.jetbrains.annotations.NotNull;

abstract public class AbstractChangeExecutionOpNode extends OpNode {

    private boolean initialized;

    private ModelExecuteDeltaTraceType trace;
    private ObjectDeltaOperationType objectDeltaOperation;

    public AbstractChangeExecutionOpNode(PrismContext prismContext,
            OperationResultType result,
            OpResultInfo info, OpNode parent,
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

    public ObjectDeltaType getObjectDelta() {
        initialize();
        return objectDeltaOperation != null ? objectDeltaOperation.getObjectDelta() : null;
    }

    void initialize() {
        if (!initialized) {
            trace = getTraceDownwards(ModelExecuteDeltaTraceType.class, 1);
            LensObjectDeltaOperationType lensObjectDeltaOperation = trace != null ? trace.getDelta() : null;
            objectDeltaOperation = lensObjectDeltaOperation != null ? lensObjectDeltaOperation.getObjectDeltaOperation() : null;
            initialized = true;
        }
    }

    public boolean isDeltaEmpty() {
        ObjectDeltaType delta = objectDeltaOperation != null ? objectDeltaOperation.getObjectDelta() : null;
        return delta == null || delta.getChangeType() == ChangeTypeType.MODIFY && delta.getItemDelta().isEmpty();
    }

    @NotNull
    public String getDeltaInfo(ObjectDeltaType objectDelta) {
        StringBuilder sb = new StringBuilder();
        if (objectDelta.getChangeType() == ChangeTypeType.ADD) {
            sb.append("add");
        } else if (objectDelta.getChangeType() == ChangeTypeType.MODIFY) {
            sb.append(objectDelta.getItemDelta().size()).append(" mod(s)");
        } else if (objectDelta.getChangeType() == ChangeTypeType.DELETE) {
            sb.append("delete");
        } else {
            sb.append("?");
        }
        return sb.toString();
    }
}
