/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;

public enum WorkflowResult {

    REJECTED, APPROVED, UNKNOWN, OTHER;

    public static WorkflowResult fromNiceWfAnswer(String answer) {
        return fromWfAnswer(answer);
    }

    private static WorkflowResult fromWfAnswer(String answer) {
        if (answer == null) {
            return UNKNOWN;
        } else {
            Boolean booleanValue = ApprovalUtils.approvalBooleanValueNice(answer);
            if (booleanValue == null) {
                return OTHER;
            } else if (booleanValue) {
                return APPROVED;
            } else {
                return REJECTED;
            }
        }
    }

}
