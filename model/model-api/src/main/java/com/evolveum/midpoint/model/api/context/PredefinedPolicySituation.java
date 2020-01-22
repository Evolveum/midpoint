/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;

/**
 * @author semancik
 */
public enum PredefinedPolicySituation {

    EXCLUSION_VIOLATION(SchemaConstants.MODEL_POLICY_SITUATION_EXCLUSION_VIOLATION, PolicyConstraintKindType.EXCLUSION),

    UNDERASSIGNED(SchemaConstants.MODEL_POLICY_SITUATION_UNDERASSIGNED, PolicyConstraintKindType.MIN_ASSIGNEES_VIOLATION),

    OVERASSIGNED(SchemaConstants.MODEL_POLICY_SITUATION_OVERASSIGNED, PolicyConstraintKindType.MAX_ASSIGNEES_VIOLATION),

    MODIFIED(SchemaConstants.MODEL_POLICY_SITUATION_MODIFIED, PolicyConstraintKindType.OBJECT_MODIFICATION),

    ASSIGNMENT_MODIFIED(SchemaConstants.MODEL_POLICY_SITUATION_ASSIGNMENT_MODIFIED, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION),

    HAS_ASSIGNMENT(SchemaConstants.MODEL_POLICY_SITUATION_HAS_ASSIGNMENT, PolicyConstraintKindType.HAS_ASSIGNMENT),

    HAS_NO_ASSIGNMENT(SchemaConstants.MODEL_POLICY_SITUATION_HAS_NO_ASSIGNMENT, PolicyConstraintKindType.HAS_NO_ASSIGNMENT),

    OBJECT_STATE(SchemaConstants.MODEL_POLICY_SITUATION_OBJECT_STATE, PolicyConstraintKindType.OBJECT_STATE),

    ASSIGNMENT_STATE(SchemaConstants.MODEL_POLICY_SITUATION_ASSIGNMENT_STATE, PolicyConstraintKindType.ASSIGNMENT_STATE),

    OBJECT_TIME_VALIDITY(SchemaConstants.MODEL_POLICY_SITUATION_TIME_VALIDITY, PolicyConstraintKindType.OBJECT_TIME_VALIDITY),

    ASSIGNMENT_TIME_VALIDITY(SchemaConstants.MODEL_POLICY_SITUATION_TIME_VALIDITY, PolicyConstraintKindType.ASSIGNMENT_TIME_VALIDITY);

    private String url;
    private PolicyConstraintKindType constraintKind;

    PredefinedPolicySituation(String url, PolicyConstraintKindType constraintKind) {
        this.url = url;
        this.constraintKind = constraintKind;
    }

    public String getUrl() {
        return url;
    }

    public PolicyConstraintKindType getConstraintKind() {
        return constraintKind;
    }

    public static PredefinedPolicySituation get(PolicyConstraintKindType constraintKind) {
        for (PredefinedPolicySituation val: PredefinedPolicySituation.values()) {
            if (val.getConstraintKind() == constraintKind) {
                return val;
            }
        }
        return null;
    }

}
