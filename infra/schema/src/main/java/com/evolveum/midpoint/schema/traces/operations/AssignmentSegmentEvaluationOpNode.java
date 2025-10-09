/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.traces.operations;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.*;
import com.evolveum.midpoint.schema.util.AssignmentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PlusMinusZeroType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import static com.evolveum.midpoint.schema.traces.operations.ResolutionUtil.resolveAssignmentReferenceNames;
import static com.evolveum.midpoint.schema.traces.operations.ResolutionUtil.resolveReferenceName;

public class AssignmentSegmentEvaluationOpNode extends OpNode {

    private final AssignmentSegmentEvaluationTraceType trace;
    private final AssignmentPathSegmentType segment;
    private final PlusMinusZeroType mode;

    public AssignmentSegmentEvaluationOpNode(PrismContext prismContext,
            OperationResultType result,
            OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
        trace = getTrace(AssignmentSegmentEvaluationTraceType.class);
        segment = trace != null ? trace.getSegment() : null;
        mode = trace != null ? trace.getMode() : null;

        fixSegmentTargetName();
    }

    protected void postProcess() {
        if (segment != null && Boolean.FALSE.equals(segment.isMatchingOrder())) {
            // TODO what about assignments carrying target policy rules?
            setDisabled(true);
        }
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
            return AssignmentUtil.getSegmentInfo(segment) + getModeInfoSuffix();
        } else {
            return new TemplateExpander()
                    .expandTemplate(this, "Segment: ${c:segmentSourceName} → ${c:segmentTargetName}");
        }
    }

    private String getModeInfoSuffix() {
        if (mode == PlusMinusZeroType.PLUS) {
            return " (+)";
        } else if (mode == PlusMinusZeroType.MINUS) {
            return " (-)";
        } else {
            return "";
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
