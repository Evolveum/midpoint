/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.operations;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.schema.traces.OpResultInfo;
import com.evolveum.midpoint.schema.traces.TraceInfo;
import com.evolveum.midpoint.schema.traces.TraceUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class MappingEvaluationOpNode extends OpNode {

    private final MappingEvaluationTraceType trace;
    private final AbstractMappingType mappingBean;
    private final String context;

    public MappingEvaluationOpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
        trace = getTrace(MappingEvaluationTraceType.class);
        mappingBean = trace != null ? trace.getMapping() : null;
        context = TraceUtil.getContext(result, "context");
    }

    public MappingEvaluationTraceType getTrace() {
        return trace;
    }

    public AbstractMappingType getMappingBean() {
        return mappingBean;
    }

    public String getContext() {
        return context;
    }

    public String getMappingInfo() {
        MappingEvaluationTraceType trace = getTrace(MappingEvaluationTraceType.class);
        if (trace != null) {
            String outputs = getOutputsAsString();
            return getMappingNameOrSignature() + " ⇒ " + (!outputs.isEmpty() ? outputs : "no outputs");
        } else {
            return context;
        }
    }

    public String getOutputsAsString() {
        return String.join(", ", getOutputs());
    }

    public List<String> getOutputs() {
        List<String> outputs = new ArrayList<>();
        if (trace.getOutput() != null) {
            if (trace.getOutput().getPlus().size() > 0) {
                outputs.add(trace.getOutput().getPlus().size() + " plus");
            }
            if (trace.getOutput().getMinus().size() > 0) {
                outputs.add(trace.getOutput().getMinus().size() + " minus");
            }
            if (trace.getOutput().getZero().size() > 0) {
                outputs.add(trace.getOutput().getZero().size() + " zero");
            }
        }
        return outputs;
    }

    public String getMappingNameOrSignature() {
        if (mappingBean != null) {
            if (mappingBean.getName() != null) {
                return mappingBean.getName();
            } else {
                return getMappingSignature();
            }
        } else {
            return "";
        }
    }

    public String getMappingSignature() {
        StringBuilder sb = new StringBuilder();
        if (mappingBean != null) {
            String sources = mappingBean.getSource().stream()
                    .map(source -> stringifyPath(source.getPath()))
                    .collect(Collectors.joining(", "));
            if (mappingBean.getTarget() != null && mappingBean.getTarget().getPath() != null) {
                String sourcesPlusSpace = sources.isEmpty() ? "" : sources + " ";
                sb.append(sourcesPlusSpace).append("→ ").append(stringifyPath(mappingBean.getTarget().getPath()));
            } else {
                sb.append(context).append(sources.isEmpty() ? "" : " <- " + sources);
            }
        }
        return sb.toString();
    }

    private String stringifyPath(ItemPathType pathBean) {
        if (pathBean != null) {
            ItemPath path = pathBean.getItemPath();
            return path.stripVariableSegment().toString();
        } else {
            return "(no path)";
        }
    }
}
