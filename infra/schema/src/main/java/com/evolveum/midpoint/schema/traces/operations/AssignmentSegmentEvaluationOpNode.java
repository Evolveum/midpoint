/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.operations;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.*;
import com.evolveum.midpoint.schema.util.AssignmentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import static com.evolveum.midpoint.schema.traces.operations.ResolutionUtil.resolveAssignmentReferenceNames;
import static com.evolveum.midpoint.schema.traces.operations.ResolutionUtil.resolveReferenceName;

public class AssignmentSegmentEvaluationOpNode extends OpNode {

    private final AssignmentSegmentEvaluationTraceType trace;
    private final AssignmentPathSegmentType segment;

    public AssignmentSegmentEvaluationOpNode(PrismContext prismContext,
            OperationResultType result,
            OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
        trace = getTrace(AssignmentSegmentEvaluationTraceType.class);
        segment = trace != null ? trace.getSegment() : null;

        fixSegmentTargetName();
    }

    private void fixSegmentTargetName() {
        if (segment != null && segment.getTargetRef() != null && segment.getTargetRef().getTargetName() == null) {
            String targetName = TraceUtil.getContext(result, "segmentTargetName");
            if (targetName != null) {
                segment.getTargetRef().setTargetName(PolyStringType.fromOrig(targetName));
            }
        }
    }

    public AssignmentSegmentEvaluationTraceType getTrace() {
        return trace;
    }

    public AssignmentPathSegmentType getSegment() {
        return segment;
    }

    public String getSegmentLabel() {
        if (segment != null) {
            return AssignmentUtil.getSegmentInfo(segment);
        } else {
            return new TemplateExpander()
                    .expandTemplate(this, "Segment: ${c:segmentSourceName} → ${c:segmentTargetName}");
        }
    }

    @Override
    public void resolveReferenceTargetNames(OpNodeTreeBuilder.NameResolver nameResolver) {
        super.resolveReferenceTargetNames(nameResolver);
        if (segment != null) {
            resolveReferenceName(segment.getTargetRef(), nameResolver);
            resolveAssignmentReferenceNames(segment.getAssignment(), nameResolver);
        }
    }
}
