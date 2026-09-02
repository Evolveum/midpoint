/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util.cases;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

public class ManualCaseUtils {

    public static OperationResultStatus translateOutcomeToStatus(String outcome) {
        return translateOutcomeToStatus(outcome, false);
    }

    /** When {@code closed}, a missing outcome is treated as a successful operation. */
    public static OperationResultStatus translateOutcomeToStatus(String outcome, boolean closed) {
        if (closed && outcome == null) {
            return OperationResultStatus.SUCCESS;
        }
        if (outcome == null) {
            return null;
        }
        for (OperationResultStatusType statusType : OperationResultStatusType.values()) {
            if (outcome.equals(statusType.value())) {
                return OperationResultStatus.parseStatusType(statusType);
            }
        }
        // This is a hack... FIXME
        if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_APPROVE)) {
            return OperationResultStatus.SUCCESS;
        } else if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_REJECT)) {
            return OperationResultStatus.FATAL_ERROR;
        } else if (QNameUtil.matchUri(outcome, SchemaConstants.MODEL_APPROVAL_OUTCOME_SKIP)) {
            // Better make this "unknown" than non-applicable. Non-applicable can be misinterpreted.
            return OperationResultStatus.UNKNOWN;
        }
        return OperationResultStatus.UNKNOWN;
    }
}
