/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingOutputType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

@Experimental
public class OpNodeTreeBuilder {

    @NotNull private final PrismContext prismContext;

    private IdentityHashMap<OperationResultType, OpResultInfo> infoMap = new IdentityHashMap<>();

    public OpNodeTreeBuilder(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public List<OpNode> build(TracingOutputType tracingOutput) {
        List<OpNode> rv = new ArrayList<>();
        addNode(null, rv, tracingOutput.getResult(), new TraceInfo(tracingOutput));
        return rv;

    }

    private void addNode(OpNode parent, List<OpNode> rv, OperationResultType result, TraceInfo traceInfo) {
        OpResultInfo info = OpResultInfo.create(result, infoMap);
        OpNode newNode = new OpNode(prismContext, result, info, parent, traceInfo);
        rv.add(newNode);
        for (OperationResultType child : result.getPartialResults()) {
            addNode(newNode, newNode.getChildren(), child, traceInfo);
        }
    }
}
