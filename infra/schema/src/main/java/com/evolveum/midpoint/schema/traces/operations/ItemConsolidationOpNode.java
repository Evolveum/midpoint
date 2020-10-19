/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.operations;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.schema.traces.OpResultInfo;
import com.evolveum.midpoint.schema.traces.TraceInfo;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemConsolidationTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;

/**
 *
 */
public class ItemConsolidationOpNode extends OpNode {

    private final ItemConsolidationTraceType trace;

    public ItemConsolidationOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
        trace = getTrace(ItemConsolidationTraceType.class);
    }

    protected void postProcess() {
        setDisabled(trace != null && getResults(trace).isEmpty());
    }

    public ItemConsolidationTraceType getTrace() {
        return trace;
    }

    public String getItemConsolidationInfo() {
        StringBuilder sb = new StringBuilder();
        ItemConsolidationTraceType trace = getTrace(ItemConsolidationTraceType.class);
        if (trace != null) {
            sb.append(trace.getItemPath());
            List<String> results = getResults(trace);
            if (results.isEmpty()) {
                sb.append(" ⇒ no delta");
            } else {
                sb.append(" ⇒ ").append(String.join(", ", results));
            }
        } else {
            sb.append(getParameter("itemPath"));
        }
        return sb.toString();
    }

    @NotNull
    private List<String> getResults(ItemConsolidationTraceType trace) {
        List<String> results = new ArrayList<>();
        for (ItemDeltaType itemDeltaBean : trace.getResultingDelta()) {
            int values = itemDeltaBean.getValue().size();
            if (itemDeltaBean.getModificationType() == ModificationTypeType.ADD && values > 0) {
                results.add(values + " add");
            }
            if (itemDeltaBean.getModificationType() == ModificationTypeType.DELETE && values > 0) {
                results.add(values + " delete");
            }
            if (itemDeltaBean.getModificationType() == ModificationTypeType.REPLACE) {
                results.add(values + " replace");
            }
        }
        return results;
    }
}
