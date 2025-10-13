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
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

public class FocusChangeExecutionOpNode extends AbstractChangeExecutionOpNode {

    public FocusChangeExecutionOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
    }

    @Override
    protected void postProcess() {
        initialize();
        if (getTrace() != null) {
            setDisabled(isDeltaEmpty());
        } else {
            setDisabled(children.isEmpty());
        }
    }

    public String getInfo() {
        initialize();
        ObjectDeltaType objectDelta = getObjectDelta();

        if (objectDelta == null) {
            if (children.isEmpty()) {
                return "none";
            } else {
                return ""; // TODO some info from OperationResult
            }
        }

        return getDeltaInfo(objectDelta);
    }

}
