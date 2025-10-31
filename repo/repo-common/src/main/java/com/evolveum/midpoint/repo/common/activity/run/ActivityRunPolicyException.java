/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import com.evolveum.midpoint.repo.common.activity.ActivityPolicyBasedAbortException;
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
            ActivityPolicyBasedAbortException cause) {
        super(message, operationResultStatus, activityRunResultStatus, cause);
    }
}
