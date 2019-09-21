/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.model.impl.lens.AssignmentPathVariables;
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

    private final AssignmentPathVariables assignmentPathVariables;
    private final String sourceDescription;

    public AssignedFocusMappingEvaluationRequest(@NotNull MappingType mapping, @NotNull ObjectType originObject,
            AssignmentPathVariables assignmentPathVariables, String sourceDescription) {
        super(mapping, originObject);
        this.assignmentPathVariables = assignmentPathVariables;
        this.sourceDescription = sourceDescription;
    }

    public AssignmentPathVariables getAssignmentPathVariables() {
        return assignmentPathVariables;
    }

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
        sb.append("'").append(mapping.getName()).append("' in ").append(sourceDescription);
    }
}
