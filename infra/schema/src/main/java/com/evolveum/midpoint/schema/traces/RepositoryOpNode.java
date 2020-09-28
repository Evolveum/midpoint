/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryOperationTraceType;

/**
 * General repository op (raw/cached, read/update, ...).
 */
public class RepositoryOpNode extends OpNode {

    private final RepositoryOperationTraceType trace;

    public RepositoryOpNode(PrismContext prismContext, OperationResultType result,
            OpResultInfo info, OpNode parent, TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
        trace = getTrace(RepositoryOperationTraceType.class);
    }

    public RepositoryOperationTraceType getTrace() {
        return trace;
    }
}
