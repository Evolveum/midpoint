/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.operations;

import static com.evolveum.midpoint.schema.traces.operations.ResolutionUtil.resolveAssignmentReferenceNames;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.*;
import com.evolveum.midpoint.schema.util.AssignmentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentEvaluationTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

public class AssignmentEvaluationOpNode extends OpNode {

    private final AssignmentEvaluationTraceType trace;
    private final AssignmentType assignment;

    public AssignmentEvaluationOpNode(PrismContext prismContext,
            OperationResultType result,
            OpResultInfo info, OpNode parent,
            TraceInfo traceInfo) {
        super(prismContext, result, info, parent, traceInfo);
        trace = getTrace(AssignmentEvaluationTraceType.class);
        assignment = getAssignmentAny();
    }

    private AssignmentType getAssignmentAny() {
        if (trace != null) {
            if (trace.getAssignmentNew() != null) {
                return trace.getAssignmentNew();
            } else {
                return trace.getAssignmentOld();
            }
        } else {
            return null;
        }
    }

    public AssignmentEvaluationTraceType getTrace() {
        return trace;
    }

    public AssignmentType getAssignment() {
        return assignment;
    }

    public String getAssignmentInfo() {
        if (assignment != null) {
            return "→ " + AssignmentUtil.getAssignmentInfo(assignment);
        } else {
            return new TemplateExpander()
                    .expandTemplate(this, "→ ${c:assignmentTargetName}");
        }
    }

    @Override
    public void resolveReferenceTargetNames(OpNodeTreeBuilder.NameResolver nameResolver) {
        super.resolveReferenceTargetNames(nameResolver);
        resolveAssignmentReferenceNames(assignment, nameResolver);
    }

    public String getModeInfo() {
        String mode = getParameter("primaryAssignmentMode").toLowerCase();
        if ("plus".equals(mode) || "minus".equals(mode)) {
            return " (" + mode + ")";
        } else {
            return "";
        }
    }
}
