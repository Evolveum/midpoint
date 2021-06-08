/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers;

import static com.evolveum.midpoint.repo.common.tasks.handlers.composite.MockComponentActivityExecution.NS_EXT;
import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType.F_EXTENSION;

import com.evolveum.midpoint.prism.path.ItemPath;
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
    private static final ItemPath EXECUTION_COUNT_PATH = ItemPath.create(F_EXTENSION, EXECUTION_COUNT_NAME);

    public void increaseExecutionCount(@NotNull AbstractActivityExecution<?, ?, ?> activityExecution, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        ActivityState<?> activityState = activityExecution.getActivityState();

        int count = or0(activityState.getWorkStatePropertyRealValue(EXECUTION_COUNT_PATH, Integer.class));
        activityState.setWorkStatePropertyRealValue(EXECUTION_COUNT_PATH, count + 1);
        activityState.flushPendingModifications(result);
    }

    public void failIfNeeded(@NotNull AbstractActivityExecution<?, ?, ?> activityExecution, int initialFailures) {
        int count = activityExecution.getActivityState().getWorkStatePropertyRealValue(EXECUTION_COUNT_PATH, Integer.class);
        if (count <= initialFailures) {
            throw new SystemException(String.format("Failed execution #%d. Expected initial failures: %d.",
                    count, initialFailures));
        }
    }
}
