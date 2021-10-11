/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.model.impl.lens.AssignmentPathVariables;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingEvaluationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

/**
 * Request to evaluate a mapping held by an assignment (in focusMappings container).
 *
 * TODO reconsider the OO (origin object) parameter. It could be AssignmentHolderType, but this would need additional
 *  class cast check in AssignmentEvaluator. If not strictly needed, let's stay with neutral ObjectType for now.
 */
public class AssignedFocusMappingEvaluationRequest extends FocalMappingEvaluationRequest<MappingType, ObjectType> {

    /**
     * Evaluated assignment this request is part of. Beware: DO NOT CLONE. It is engaged in identity-based lookup.
     */
    @NotNull private final EvaluatedAssignmentImpl<?> evaluatedAssignment;

    /**
     * Mode of the focus mapping (plus, minus, zero), relative to the assignment being evaluated.
     */
    @NotNull private final PlusMinusZero relativeMode;

    private final AssignmentPathVariables assignmentPathVariables;
    private final String sourceDescription;

    public AssignedFocusMappingEvaluationRequest(@NotNull MappingType mapping, @NotNull ObjectType originObject,
            @NotNull EvaluatedAssignmentImpl<?> evaluatedAssignment,
            @NotNull PlusMinusZero relativeMode, AssignmentPathVariables assignmentPathVariables,
            String sourceDescription) {
        super(mapping, MappingKindType.ASSIGNED, originObject);
        this.evaluatedAssignment = evaluatedAssignment;
        this.relativeMode = relativeMode;
        this.assignmentPathVariables = assignmentPathVariables;
        this.sourceDescription = sourceDescription;
    }

    @NotNull
    public EvaluatedAssignmentImpl<?> getEvaluatedAssignment() {
        return evaluatedAssignment;
    }

    @NotNull
    public PlusMinusZero getRelativeMode() {
        return relativeMode;
    }

    @Override
    public AssignmentPathVariables getAssignmentPathVariables() {
        return assignmentPathVariables;
    }

    @SuppressWarnings("unused")
    public String getSourceDescription() {
        return sourceDescription;
    }

    @Override
    public ObjectTemplateMappingEvaluationPhaseType getEvaluationPhase() {
        // We should evaluate these mappings without checking for evaluation phase.
        return null;
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append("assigned mapping ");
        sb.append("'").append(getMappingInfo()).append("' in ").append(sourceDescription);
    }
}
