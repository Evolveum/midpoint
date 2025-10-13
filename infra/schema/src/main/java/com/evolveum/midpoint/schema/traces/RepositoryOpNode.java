/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.traces;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryGetObjectTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryOperationTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositorySearchObjectsTraceType;

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

    /**
     * Returns
     *
     * 1. true if the get/search operation is read-only;
     * 2. false if it's not;
     * 3. null if the flag is not applicable or not available.
     */
    public Boolean isReadOnly() {
        if (trace != null) {
            return isReadOnlyFromTrace();
        } else {
            return isReadOnlyFromOpResult();
        }
    }

    private Boolean isReadOnlyFromTrace() {
        if (trace instanceof RepositoryGetObjectTraceType) {
            return isReadOnlyFromOptions(((RepositoryGetObjectTraceType) trace).getOptions());
        } else if (trace instanceof RepositorySearchObjectsTraceType) {
            return isReadOnlyFromOptions(((RepositorySearchObjectsTraceType) trace).getOptions());
        } else {
            return null;
        }
    }

    /** Only approximate for now. */
    private boolean isReadOnlyFromOptions(String options) {
        return options != null &&
                options.contains("readOnly") &&
                !options.contains("readOnly=false");
    }

    /** Only approximate for now. */
    private Boolean isReadOnlyFromOpResult() {
        String operation = result.getOperation();
        if (operation == null || !operation.contains("getObject") && !operation.contains("searchObject")) {
            return null;
        } else {
            return TraceUtil.getParametersAsStringList(result, "options").stream()
                    .anyMatch(o -> o.contains("readOnly") && !o.contains("readOnly=false"));
        }
    }
}
