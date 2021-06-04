/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers;

import static com.evolveum.midpoint.repo.common.tasks.handlers.composite.MockComponentActivityExecution.NS_EXT;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.activity.ActivityState;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;

@Component
public class CommonMockActivityHelper {

    private static final ItemName EXECUTION_COUNT_NAME = new ItemName(NS_EXT, "executionCount");

    public void increaseExecutionCount(@NotNull AbstractActivityExecution<?, ?, ?> activityExecution, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        ActivityState<?> workState = activityExecution.getWorkState();

        int count = or0(workState.getPropertyRealValue(EXECUTION_COUNT_NAME, Integer.class));
        workState.setPropertyRealValue(EXECUTION_COUNT_NAME, count + 1);
        workState.flushPendingModifications(result);
    }

    public void failIfNeeded(@NotNull AbstractActivityExecution<?, ?, ?> activityExecution, int initialFailures) {
        int count = activityExecution.getWorkState().getPropertyRealValue(EXECUTION_COUNT_NAME, Integer.class);
        if (count <= initialFailures) {
            throw new SystemException(String.format("Failed execution #%d. Expected initial failures: %d.",
                    count, initialFailures));
        }
    }
}
