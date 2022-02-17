/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
