/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityExecutionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

// TODO implement
public class ActivityExecution {

    @NotNull private final CurrentActivityState<?> currentState;

    private int executionAttempt = 0;

    private final List<ActivityExecutionType> activityExecutions = new ArrayList<>();

    public ActivityExecution(@NotNull CurrentActivityState<?> currentState) {
        this.currentState = currentState;
    }

    public synchronized void incrementExecutionAttemptAndRecordHistory(OperationResult result)
            throws ActivityRunException, ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException {

        executionAttempt++;

        ActivityExecutionType execution = new ActivityExecutionType();
//        execution.setPolicies();
        execution.setResultStatus(OperationResultStatusType.FATAL_ERROR);
//        execution.setRunStartTimestamp();
//        execution.setRunEndTimestamp();

        activityExecutions.add(execution);

        currentState.setItemRealValues(ActivityStateType.F_EXECUTION_ATTEMPT, executionAttempt);
        currentState.setItemRealValues(ActivityStateType.F_ACTIVITY_EXECUTION, activityExecutions.toArray());

        currentState.flushPendingTaskModifications(result);
    }
}
