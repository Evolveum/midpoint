/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import com.evolveum.midpoint.repo.common.activity.ActivityPolicyViolationException;
import com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus;
import com.evolveum.midpoint.schema.result.OperationResultStatus;

/**
 * Exception indicating that an activity run was stopped due to policy rule execution.
 *
 * It was introduced to provide more specific {@code throws} clauses in some parts of the code.
 */
public class ActivityRunPolicyException extends ActivityRunException {

    public ActivityRunPolicyException(
            String message,
            OperationResultStatus operationResultStatus,
            ActivityRunResultStatus activityRunResultStatus,
            ActivityPolicyViolationException cause) {
        super(message, operationResultStatus, activityRunResultStatus, cause);
    }
}
